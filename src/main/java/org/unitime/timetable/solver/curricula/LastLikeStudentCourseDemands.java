/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * The Apereo Foundation licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
*/
package org.unitime.timetable.solver.curricula;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.solver.curricula.StudentCourseDemands.ProjectionsProvider;


/**
 * @author Tomas Muller
 */
public class LastLikeStudentCourseDemands implements StudentCourseDemands, ProjectionsProvider {
	protected org.hibernate.Session iHibSession;
	protected Hashtable<String, Set<WeightedStudentId>> iDemandsForPemId = new Hashtable<String, Set<WeightedStudentId>>();
	protected Hashtable<Long, Hashtable<String, Set<WeightedStudentId>>> iDemandsForSubjectCourseNbr = new Hashtable<Long, Hashtable<String,Set<WeightedStudentId>>>();
	protected Hashtable<Long, Set<WeightedCourseOffering>> iStudentRequests = null;
	protected Long iSessionId = null;
	protected double iBasePriorityWeight = 0.9;
	protected boolean iUsePriorities = false;
	private Hashtable<Long, Hashtable<String, Double>> iEnrollmentPriorities = new Hashtable<Long, Hashtable<String, Double>>();
	
	public LastLikeStudentCourseDemands(DataProperties properties) {
		iUsePriorities = properties.getPropertyBoolean("LastLikeStudentCourseDemands.UsePriorities", iUsePriorities);
		iBasePriorityWeight = properties.getPropertyDouble("LastLikeStudentCourseDemands.BasePriorityWeight", iBasePriorityWeight);
	}
	
	@Override
	public boolean isMakingUpStudents() { return false; }
	
	@Override
	public boolean canUseStudentClassEnrollmentsAsSolution() { return false; }

	@Override
	public boolean isWeightStudentsToFillUpOffering() { return false; }
	
	@Override
	public void init(org.hibernate.Session hibSession, Progress progress, Session session, Collection<InstructionalOffering> offerings) {
		iHibSession = hibSession;
		iSessionId = session.getUniqueId();
	}
	
	@Override
	public float getProjection(String areaAbbv, String clasfCode, String majorCode) {
		return 1.0f;
	}
	
	protected Hashtable<String, Set<WeightedStudentId>> loadSubject(SubjectArea subject) {
		Hashtable<String, Set<WeightedStudentId>> demandsForCourseNbr = new Hashtable<String, Set<WeightedStudentId>>();
		iDemandsForSubjectCourseNbr.put(subject.getUniqueId(), demandsForCourseNbr);
		for (Object[] d: (List<Object[]>)iHibSession.createQuery(
				"select distinct d.courseNbr, d.coursePermId, s, d.priority " +
				"from LastLikeCourseDemand d inner join d.student s left join fetch s.areaClasfMajors where " +
				"d.subjectArea.uniqueId = :subjectAreaId")
				.setLong("subjectAreaId", subject.getUniqueId()).setCacheable(true).list()) {
			String courseNbr = (String)d[0];
			String coursePermId = (String)d[1];
			Student student = (Student)d[2];
			Integer priority = (Integer)d[3];
			WeightedStudentId studentId = new WeightedStudentId(student, this);
			Set<WeightedStudentId> studentIds = demandsForCourseNbr.get(courseNbr);
			if (studentIds == null) {
				studentIds = new HashSet<WeightedStudentId>();
				demandsForCourseNbr.put(courseNbr, studentIds);
			}
			studentIds.add(studentId);
			
			if (coursePermId!=null) {
			    studentIds = iDemandsForPemId.get(coursePermId);
			    if (studentIds==null) {
                    studentIds = new HashSet<WeightedStudentId>();
                    iDemandsForPemId.put(coursePermId, studentIds);
                }
                studentIds.add(studentId);
			}
			
			if (priority != null) {
				Hashtable<String, Double> priorities = iEnrollmentPriorities.get(student.getUniqueId());
				if (priorities == null) {
					priorities = new Hashtable<String, Double>();
					iEnrollmentPriorities.put(student.getUniqueId(), priorities);
				}
				priorities.put(subject.getUniqueId() + "|" + courseNbr, Math.pow(iBasePriorityWeight, priority));
			}
			if (priority != null && coursePermId != null) {
				Hashtable<String, Double> priorities = iEnrollmentPriorities.get(student.getUniqueId());
				if (priorities == null) {
					priorities = new Hashtable<String, Double>();
					iEnrollmentPriorities.put(student.getUniqueId(), priorities);
				}
				priorities.put(coursePermId, Math.pow(iBasePriorityWeight, priority));
			}
		}
		return demandsForCourseNbr;
	}
	
	@Override
	public Set<WeightedStudentId> getDemands(CourseOffering course) {
		Hashtable<String, Set<WeightedStudentId>> demandsForCourseNbr = iDemandsForSubjectCourseNbr.get(course.getSubjectArea().getUniqueId());
		if (demandsForCourseNbr == null) {
			demandsForCourseNbr = loadSubject(course.getSubjectArea());
		}
		Set<WeightedStudentId> studentIds = null;
		if (course.getPermId() != null)
			studentIds = iDemandsForPemId.get(course.getPermId());
		if (studentIds == null)
			studentIds = demandsForCourseNbr.get(course.getCourseNbr());

		if (course.getDemandOffering() != null && !course.getDemandOffering().equals(course)) {
			if (studentIds == null)
				studentIds = getDemands(course.getDemandOffering());
			else {
				studentIds = new HashSet<WeightedStudentId>(studentIds);
				studentIds.addAll(getDemands(course.getDemandOffering()));
			}
		}
		
		if (studentIds == null)
			studentIds = new HashSet<WeightedStudentId>();
		
		return studentIds;
	}
	
	@Override
	public Set<WeightedCourseOffering> getCourses(Long studentId) {
		if (iStudentRequests == null) {
			iStudentRequests = new Hashtable<Long, Set<WeightedCourseOffering>>();
			String[] checks = new String[] {
					"x.subjectArea.session.uniqueId = :sessionId and co.subjectArea.uniqueId = x.subjectArea.uniqueId and x.coursePermId is not null and co.permId=x.coursePermId",
					"x.subjectArea.session.uniqueId = :sessionId and co.subjectArea.uniqueId = x.subjectArea.uniqueId and x.coursePermId is null and co.courseNbr=x.courseNbr",
					"x.subjectArea.session.uniqueId = :sessionId and co.demandOffering.subjectArea.uniqueId = x.subjectArea.uniqueId and x.coursePermId is not null and co.demandOffering.permId=x.coursePermId",
					"x.subjectArea.session.uniqueId = :sessionId and co.demandOffering.subjectArea.uniqueId = x.subjectArea.uniqueId and x.coursePermId is null and co.demandOffering.courseNbr=x.courseNbr"
			};
			for (String where: checks) {
				for (Object[] o : (List<Object[]>)iHibSession.createQuery(
						"select distinct s, co " +
						"from LastLikeCourseDemand x inner join x.student s left join fetch s.areaClasfMajors, CourseOffering co left outer join co.demandOffering do where " + where)
						.setLong("sessionId", iSessionId)
						.setCacheable(true).list()) {
					Student student = (Student)o[0];
					CourseOffering co = (CourseOffering)o[1];
					Set<WeightedCourseOffering> courses = iStudentRequests.get(student.getUniqueId());
					if (courses == null) {
						courses = new HashSet<WeightedCourseOffering>();
						iStudentRequests.put(student.getUniqueId(), courses);
					}
					courses.add(new WeightedCourseOffering(co, new WeightedStudentId(student, this).getWeight()));
				}
			}
		}
		return iStudentRequests.get(studentId);
	}
	


	@Override
	public Double getEnrollmentPriority(Long studentId, Long courseId) {
		if (!iUsePriorities) return null;
		CourseOffering course = CourseOfferingDAO.getInstance().get(courseId);
		if (course == null) return null;
		if (iDemandsForSubjectCourseNbr.get(course.getSubjectArea().getUniqueId()) == null)
			loadSubject(course.getSubjectArea());
		Hashtable<String, Double> priorities = iEnrollmentPriorities.get(studentId);
		if (priorities == null) return null;
		if (course.getPermId() != null) {
			Double priority = priorities.get(course.getPermId());
			if (priority != null) return priority;
		}
		return priorities.get(course.getSubjectArea().getUniqueId() + "|" + course.getCourseNbr());
	}
}
