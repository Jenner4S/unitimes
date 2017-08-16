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
package org.unitime.timetable.onlinesectioning.status.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cpsolver.studentsct.online.expectations.OverExpectedCriterion;
import org.unitime.commons.NaturalOrderComparator;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.server.DayCode;
import org.unitime.timetable.gwt.server.Query;
import org.unitime.timetable.gwt.server.Query.TermMatcher;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ClassAssignment;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.EnrollmentInfo;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.ClassEvent;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.CourseRequest;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Meeting;
import org.unitime.timetable.model.Reservation;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentAccomodation;
import org.unitime.timetable.model.StudentAreaClassificationMajor;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.StudentGroup;
import org.unitime.timetable.model.dao.CourseOfferingDAO;
import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;
import org.unitime.timetable.onlinesectioning.OnlineSectioningHelper;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.status.FindEnrollmentInfoAction;
import org.unitime.timetable.onlinesectioning.status.SectioningStatusFilterAction;
import org.unitime.timetable.onlinesectioning.status.FindStudentInfoAction.FindStudentInfoMatcher;
import org.unitime.timetable.util.Formats;
import org.unitime.timetable.util.NameFormat;

/**
 * @author Tomas Muller
 */
public class DbFindEnrollmentInfoAction extends FindEnrollmentInfoAction {
	private static final long serialVersionUID = 1L;
	private static StudentSectioningConstants CONSTANTS = Localization.create(StudentSectioningConstants.class);

	@Override
	public List<EnrollmentInfo> execute(final OnlineSectioningServer server, final OnlineSectioningHelper helper) {
		if (iFilter == null) return super.execute(server, helper);
		
		List<EnrollmentInfo> ret = new ArrayList<EnrollmentInfo>();
		AcademicSessionInfo session = server.getAcademicSession();
		if (courseId() == null) {
			Set<Long> students = new HashSet<Long>();
			Set<Long> matchingStudents = new HashSet<Long>();
			
			int gEnrl = 0, gWait = 0, gRes = 0, gUnasg = 0;
			int gtEnrl = 0, gtWait = 0, gtRes = 0, gtUnasg = 0;
			int gConNeed = 0, gtConNeed = 0;
			
			DbFindEnrollmentInfoCourseMatcher m = new DbFindEnrollmentInfoCourseMatcher(iCoursesIcoordinate, iCoursesIcanApprove, iQuery);
			
			Map<CourseOffering, List<CourseRequest>> requests = new HashMap<CourseOffering, List<CourseRequest>>();
			for (CourseRequest cr: (List<CourseRequest>)SectioningStatusFilterAction.getCourseQuery(iFilter, server).select("distinct cr").query(helper.getHibSession()).list()) {
				if (!m.match(cr.getCourseOffering())) continue;
				List<CourseRequest> list = requests.get(cr.getCourseOffering());
				if (list == null) {
					list = new ArrayList<CourseRequest>();
					requests.put(cr.getCourseOffering(), list);
				}
				list.add(cr);
			}
			
			for (Map.Entry<CourseOffering, List<CourseRequest>> entry: requests.entrySet()) {
				CourseOffering course = entry.getKey();
				InstructionalOffering offering = course.getInstructionalOffering();
				
				boolean isConsentToDoCourse = isConsentToDoCourse(course);
				EnrollmentInfo e = new EnrollmentInfo();
				e.setCourseId(course.getUniqueId());
				e.setOfferingId(offering.getUniqueId());
				e.setSubject(course.getSubjectAreaAbbv());
				e.setCourseNbr(course.getCourseNbr());
				e.setTitle(course.getTitle());
				e.setConsent(course.getConsentType() == null ? null : course.getConsentType().getAbbv());

				int match = 0;
				int enrl = 0, wait = 0, res = 0, unasg = 0;
				int tEnrl = 0, tWait = 0, tRes = 0, tUnasg = 0;
				int conNeed = 0, tConNeed = 0;
				
				for (CourseRequest request: (List<CourseRequest>)helper.getHibSession().createQuery(
						"from CourseRequest where courseOffering.uniqueId = :courseId"
						).setLong("courseId", course.getUniqueId()).setCacheable(true).list()) {
					
					DbCourseRequestMatcher crm = new DbCourseRequestMatcher(session, request, isConsentToDoCourse, helper.getStudentNameFormat());
					if (!query().match(crm)) {
						if (!crm.enrollment().isEmpty()) {
							tEnrl ++;
							if (crm.reservation() != null) tRes ++;
							if (request.getCourseOffering().getConsentType() != null && crm.approval() == null) tConNeed ++;
						} else if (crm.canAssign()) {
							tUnasg ++;
							if (request.getCourseDemand().isWaitlist())
								tWait ++;
						}
					}
				}

				Set<Long> addedStudents = new HashSet<Long>();
				for (CourseRequest request: entry.getValue()) {
					Student student = request.getCourseDemand().getStudent();
					
					if (students.add(student.getUniqueId()))
						addedStudents.add(student.getUniqueId());
					
					DbCourseRequestMatcher crm = new DbCourseRequestMatcher(session, request, isConsentToDoCourse, helper.getStudentNameFormat());
					if (query().match(crm)) {
						matchingStudents.add(student.getUniqueId());
						match++;
						if (!crm.enrollment().isEmpty()) {
							enrl ++;
							if (crm.reservation() != null) res ++;
							if (request.getCourseOffering().getConsentType() != null && crm.approval() == null) conNeed ++;
						} else if (crm.canAssign()) {
							unasg ++;
							if (request.getCourseDemand().isWaitlist())
								wait ++;
						}
					}
					
					if (!crm.enrollment().isEmpty()) {
						tEnrl ++;
						if (crm.reservation() != null) tRes ++;
						if (request.getCourseOffering().getConsentType() != null && crm.approval() == null) tConNeed ++;
					} else if (crm.canAssign()) {
						tUnasg ++;
						if (request.getCourseDemand().isWaitlist())
							tWait ++;
					}
				}
				
				if (match == 0) {
					students.removeAll(addedStudents);
					continue;
				}
				
				gEnrl += enrl;
				gWait += wait;
				gUnasg += unasg;
				gRes += res;
				gConNeed += conNeed;
				
				gtEnrl += tEnrl;
				gtWait += tWait;
				gtUnasg += tUnasg;
				gtRes += tRes;
				gtConNeed += tConNeed;
				
				int limit = 0;
				for (InstrOfferingConfig config: offering.getInstrOfferingConfigs()) {
					if (config.isUnlimitedEnrollment()) {
						limit = -1; break;
					} else {
						limit += config.getLimit();
					}
				}

				e.setLimit(course.getReservation() != null ? course.getReservation() : limit);
				e.setProjection(course.getProjectedDemand() != null ? course.getProjectedDemand().intValue() : course.getDemand() != null ? course.getDemand().intValue() : 0);
				int av = (int)Math.max(0, offering.getUnreservedSpace());
				if (e.getLimit() >= 0 && av > e.getLimit() - course.getEnrollment())
					av = e.getLimit() - course.getEnrollment();
				if (av == Integer.MAX_VALUE) av = -1;
				e.setAvailable(av);
				if (av >= 0) {
					int other = 0;
					for (CourseOffering c: offering.getCourseOfferings())
						if (!c.equals(course))
							other += c.getEnrollment();
					e.setOther(Math.min(e.getLimit() - course.getEnrollment() - av, other));
					int lim = 0;
					for (InstrOfferingConfig f: offering.getInstrOfferingConfigs()) {
						if (lim < 0 || f.isUnlimitedEnrollment())
							lim = -1;
						else
							lim += f.getLimit();
					}
					if (lim >= 0 && lim < e.getLimit())
						e.setOther(e.getOther() + e.getLimit() - limit);
				}
				
				e.setEnrollment(enrl);
				e.setReservation(res);
				e.setWaitlist(wait);
				e.setUnassigned(unasg);
				
				e.setTotalEnrollment(tEnrl);
				e.setTotalReservation(tRes);
				e.setTotalWaitlist(tWait);
				e.setTotalUnassigned(tUnasg);
				
				e.setConsentNeeded(conNeed);
				e.setTotalConsentNeeded(tConNeed);

				ret.add(e);
				if (limit() != null && ret.size() >= limit()) break;
			}
			
			final Comparator noc = new NaturalOrderComparator();
			Collections.sort(ret, new Comparator<EnrollmentInfo>() {
				@Override
				public int compare(EnrollmentInfo e1, EnrollmentInfo e2) {
					int cmp = noc.compare(e1.getSubject(), e2.getSubject());
					if (cmp != 0) return cmp;
					cmp = e1.getCourseNbr().compareTo(e2.getCourseNbr());
					if (cmp != 0) return cmp;
					return 0;
				}
			});
			
			EnrollmentInfo t = new EnrollmentInfo();
			t.setSubject(MSG.total());
			t.setCourseNbr("");
			
			t.setLimit(students.size());
			t.setAvailable(matchingStudents.size());
			
			t.setEnrollment(gEnrl);
			t.setReservation(gRes);
			t.setWaitlist(gWait);
			t.setUnassigned(gUnasg);
			
			t.setTotalEnrollment(gtEnrl);
			t.setTotalReservation(gtRes);
			t.setTotalWaitlist(gtWait);
			t.setTotalUnassigned(gtUnasg);
			
			t.setConsentNeeded(gConNeed);
			t.setTotalConsentNeeded(gtConNeed);
			ret.add(t);
		} else {
			final CourseOffering course = CourseOfferingDAO.getInstance().get(courseId(), helper.getHibSession());
			if (course == null) return ret;
			final InstructionalOffering offering = course.getInstructionalOffering();
			if (offering == null) return ret;
			List<CourseRequest> requests = (List<CourseRequest>)helper.getHibSession().createQuery(
					"from CourseRequest where courseOffering.instructionalOffering.uniqueId = :offeringId"
					).setLong("offeringId", offering.getUniqueId()).setCacheable(true).list();
			OverExpectedCriterion overExp = server.getOverExpectedCriterion();
			boolean isConsentToDoCourse = isConsentToDoCourse(course);
			List<Class_> sections = new ArrayList<Class_>();
			for (InstrOfferingConfig config: offering.getInstrOfferingConfigs())
				for (SchedulingSubpart subpart: config.getSchedulingSubparts())
					sections.addAll(subpart.getClasses());
			Collections.sort(sections, new Comparator<Class_>() {
				public int compare(InstrOfferingConfig c1, InstrOfferingConfig c2) {
					int cmp = c1.getName().compareToIgnoreCase(c2.getName());
					if (cmp != 0) return cmp;
					return c1.getUniqueId().compareTo(c2.getUniqueId());
				}
				public boolean isParent(SchedulingSubpart s1, SchedulingSubpart s2) {
					SchedulingSubpart p1 = s1.getParentSubpart();
					if (p1==null) return false;
					if (p1.equals(s2)) return true;
					return isParent(p1, s2);
				}
				public int compare(SchedulingSubpart s1, SchedulingSubpart s2) {
					int cmp = compare(s1.getInstrOfferingConfig(), s2.getInstrOfferingConfig());
					if (cmp != 0) return cmp;
			        if (isParent(s1,s2)) return 1;
			        if (isParent(s2,s1)) return -1;
			        cmp = s1.getItype().compareTo(s2.getItype());
			        if (cmp != 0) return cmp;
			        return s1.getUniqueId().compareTo(s2.getUniqueId());
				}
				public int compare(Class_ s1, Class_ s2) {
					if (s1.getSchedulingSubpart().equals(s2.getSchedulingSubpart())) {
						if (s1.getParentClass() != null) {
							int cmp = compare(s1.getParentClass(), s2.getParentClass());
							if (cmp != 0) return cmp;
						}
						try {
							int cmp = Integer.valueOf(s1.getClassSuffix(course) == null ? "0" : s1.getClassSuffix(course)).compareTo(Integer.valueOf(s2.getClassSuffix(course) == null ? "0" : s2.getClassSuffix(course)));
							if (cmp != 0) return cmp;
						} catch (NumberFormatException e) {}
						int cmp = (s1.getClassSuffix(course) == null ? "" : s1.getClassSuffix(course)).compareTo(s2.getClassSuffix(course) == null ? "" : s2.getClassSuffix(course));
						if (cmp != 0) return cmp;
				        return s1.getUniqueId().compareTo(s2.getUniqueId());
					}
					Class_ x = s1;
					while (x != null) {
						if (isParent(s2.getSchedulingSubpart(), x.getSchedulingSubpart())) {
							Class_ s = s2.getParentClass();
							while (!s.getSchedulingSubpart().equals(x.getSchedulingSubpart())) {
								s = s.getParentClass();
							}
							int cmp = compare(x, s);
							return (cmp == 0 ? x.equals(s1) ? -1 : compare(x.getSchedulingSubpart(), s.getSchedulingSubpart()) : cmp);
						}
						x = x.getParentClass();
					}
					x = s2;
					while (x != null) {
						if (isParent(s1.getSchedulingSubpart(), x.getSchedulingSubpart())) {
							Class_ s = s1.getParentClass();
							while (!s.getSchedulingSubpart().equals(x.getSchedulingSubpart())) {
								s = s.getParentClass();
							}
							int cmp = compare(s, x);
							return (cmp == 0 ? x.equals(s2) ? 1 : compare(x.getSchedulingSubpart(), x.getSchedulingSubpart()) : cmp);
						}
						x = x.getParentClass();
					}
					int cmp = compare(s1.getSchedulingSubpart(), s2.getSchedulingSubpart());
					if (cmp != 0) return cmp;
					try {
						cmp = Integer.valueOf(s1.getClassSuffix(course) == null ? "0" : s1.getClassSuffix(course)).compareTo(Integer.valueOf(s2.getClassSuffix(course) == null ? "0" : s2.getClassSuffix(course)));
						if (cmp != 0) return cmp;
					} catch (NumberFormatException e) {}
					cmp = (s1.getClassSuffix(course) == null ? "" : s1.getClassSuffix(course)).compareTo(s2.getClassSuffix(course) == null ? "" : s2.getClassSuffix(course));
					if (cmp != 0) return cmp;
			        return s1.getUniqueId().compareTo(s2.getUniqueId());
				}
			});
			for (Class_ section: sections) {
				EnrollmentInfo e = new EnrollmentInfo();
				e.setCourseId(course.getUniqueId());
				e.setOfferingId(offering.getUniqueId());
				e.setSubject(course.getSubjectAreaAbbv());
				e.setCourseNbr(course.getCourseNbr());
				e.setTitle(course.getTitle());
				e.setConsent(course.getConsentType() == null ? null : course.getConsentType().getAbbv());
				
				SchedulingSubpart subpart = section.getSchedulingSubpart();
				InstrOfferingConfig config = subpart.getInstrOfferingConfig();
				e.setConfig(config.getName());
				e.setConfigId(config.getUniqueId());
				
				e.setSubpart(subpart.getItype().getAbbv().trim());
				if (subpart.getInstrOfferingConfig().getInstructionalMethod() != null)
					e.setSubpart(e.getSubpart() + " (" + subpart.getInstrOfferingConfig().getInstructionalMethod().getLabel() + ")");
				e.setSubpartId(subpart.getUniqueId());
				e.setClazz(section.getClassSuffix(course));
				if (e.getClazz() == null)
					e.setClazz(section.getSectionNumberString());
				e.setClazzId(section.getUniqueId());
				Class_ parent = section.getParentClass();
				while (parent != null) {
					e.incLevel();
					parent = parent.getParentClass();
				}
				
				int match = 0;
				int enrl = 0, wait = 0, res = 0, unasg = 0;
				int tEnrl = 0, tWait = 0, tRes = 0, tUnasg = 0;
				int conNeed = 0, tConNeed = 0;
				int other = 0;

				for (CourseRequest request: requests) {
					DbCourseRequestMatcher m = new DbCourseRequestMatcher(session, request, isConsentToDoCourse, helper.getStudentNameFormat());
					boolean contains = false;
					for (StudentClassEnrollment x: m.enrollment()) {
						if (x.getClazz().equals(section)) { contains = true; break; }
					}
					if (!contains) continue;
					if (!request.getCourseOffering().equals(course)) {other++; continue; }
					if (query().match(m)) {
						match++;
						enrl ++;
						if (m.reservation() != null) res ++;
						if (course.getConsentType() != null && m.approval() == null) conNeed ++;
					}
					
					tEnrl ++;
					if (m.reservation() != null) tRes ++;
					if (course.getConsentType() != null && m.approval() == null) tConNeed ++;
				}

				for (CourseRequest request: requests) {
					DbCourseRequestMatcher m = new DbCourseRequestMatcher(session, request, isConsentToDoCourse, helper.getStudentNameFormat());
					if (!m.enrollment().isEmpty() || !request.getCourseOffering().equals(course)) continue;
					if (!m.canAssign()) continue;
					
					if (query().match(m)) {
						match++;
						unasg++;
						if (request.getCourseDemand().isWaitlist())
							wait++;
					}
					tUnasg ++;
					if (request.getCourseDemand().isWaitlist())
						tWait ++;
				}
				
				if (match == 0) continue;
				
				e.setLimit(section.getSectioningLimit());
				e.setOther(other);
				e.setAvailable(Math.max(0, section.getUnreservedSectionSpace()));
				if (e.getAvailable() == Integer.MAX_VALUE) e.setAvailable(-1);
				e.setProjection(tEnrl + Math.max(0, (int)Math.round(section.getSectioningInfo() == null ? 0 : section.getSectioningInfo().getNbrExpectedStudents())));
				
				e.setEnrollment(enrl);
				e.setReservation(res);
				e.setWaitlist(wait);
				e.setUnassigned(unasg);
				
				e.setTotalEnrollment(tEnrl);
				e.setTotalReservation(tRes);
				e.setTotalWaitlist(tWait);
				e.setTotalUnassigned(tUnasg);

				e.setConsentNeeded(conNeed);
				e.setTotalConsentNeeded(tConNeed);

				ClassAssignment a = new ClassAssignment();
				a.setClassId(section.getUniqueId());
				a.setSubpart(subpart.getItype().getAbbv().trim());
				if (subpart.getInstrOfferingConfig().getInstructionalMethod() != null)
					a.setSubpart(a.getSubpart() + " (" + subpart.getInstrOfferingConfig().getInstructionalMethod().getLabel() + ")");
				a.setClassNumber(section.getClassSuffix() == null ? section.getSectionNumber(helper.getHibSession()) + section.getSchedulingSubpart().getSchedulingSubpartSuffix(helper.getHibSession()) : section.getClassSuffix());
				a.setSection(section.getClassSuffix(course));
				a.setCancelled(section.isCancelled());
				a.setLimit(new int[] { section.getEnrollment(), section.getSectioningLimit()});
				Assignment assignment = section.getCommittedAssignment();
				if (assignment != null) {
					for (DayCode d : DayCode.toDayCodes(assignment.getDays()))
						a.addDay(d.getIndex());
					a.setStart(assignment.getStartSlot());
					a.setLength(assignment.getSlotPerMtg());
					a.setBreakTime(assignment.getBreakTime());
					a.setDatePattern(assignment.getDatePattern().getName());
				}
				if (assignment != null && !assignment.getRooms().isEmpty()) {
					for (Location rm: assignment.getRooms()) {
						a.addRoom(rm.getUniqueId(), rm.getLabelWithDisplayName());
					}
				}
				if (section.isDisplayInstructor() && !section.getClassInstructors().isEmpty()) {
					for (ClassInstructor instructor: section.getClassInstructors()) {
						a.addInstructor(helper.getInstructorNameFormat().format(instructor.getInstructor()));
						a.addInstructoEmail(instructor.getInstructor().getEmail());
					}
				}
				if (section.getParentClass()!= null)
					a.setParentSection(section.getParentClass().getClassSuffix(course));
				a.setSubpartId(section.getSchedulingSubpart().getUniqueId());
				a.addNote(course.getScheduleBookNote());
				a.addNote(section.getSchedulePrintNote());
				if (section.getSchedulingSubpart().getCredit() != null) {
					a.setCredit(section.getSchedulingSubpart().getCredit().creditAbbv() + "|" + section.getSchedulingSubpart().getCredit().creditText());
				} else if (section.getParentClass() != null && course.getCredit() != null) {
					a.setCredit(course.getCredit().creditAbbv() + "|" + course.getCredit().creditText());
				}
				if (a.getParentSection() == null) {
					String consent = (course.getConsentType() == null ? null : course.getConsentType().getLabel());
					if (consent != null)
						a.setParentSection(consent);
				}
				a.setExpected(overExp.getExpected(section.getSectioningLimit(), section.getSectioningInfo() == null ? 0.0 : section.getSectioningInfo().getNbrExpectedStudents()));
				e.setAssignment(a);
				
				ret.add(e);
			}
		}
		return ret;
	}
	
	public boolean isConsentToDoCourse(CourseOffering course) {
		return iCoursesIcanApprove != null && course.getConsentType() != null && iCoursesIcanApprove.contains(course.getUniqueId());
	}
	
	public static class DbFindEnrollmentInfoCourseMatcher extends FindEnrollmentInfoCourseMatcher {
		private static final long serialVersionUID = 1L;
		
		public DbFindEnrollmentInfoCourseMatcher(Set<Long> coursesIcoordinate, Set<Long> coursesIcanApprove, Query query) {
			super(coursesIcoordinate, coursesIcanApprove, query);
		}
		
		public boolean isConsentToDoCourse(CourseOffering co) {
			return iCoursesIcanApprove != null && co.getConsentType() != null && iCoursesIcanApprove.contains(co.getUniqueId());
		}
		
		public boolean match(CourseOffering co) {
			return co != null && isCourseVisible(co.getUniqueId()) && iQuery.match(new DbCourseInfoMatcher(co, isConsentToDoCourse(co)));
		}
		
	}
	
	public static class DbCourseInfoMatcher implements TermMatcher, Serializable {
		private static final long serialVersionUID = 1L;
		private CourseOffering iCourse;
		private boolean iConsentToDoCourse;
		
		public DbCourseInfoMatcher(CourseOffering course, boolean isConsentToDoCourse) {
			iCourse = course;
			iConsentToDoCourse = isConsentToDoCourse;
		}
		
		public CourseOffering course() { return iCourse; }
		
		public boolean isConsentToDoCourse() { return iConsentToDoCourse; }
		
		@Override
		public boolean match(String attr, String term) {
			if (term.isEmpty()) return true;
			if ("limit".equals(attr)) return true;
			if (attr == null || "name".equals(attr) || "course".equals(attr)) {
				return course().getSubjectAreaAbbv().equalsIgnoreCase(term) || course().getCourseNbr().equalsIgnoreCase(term) || (course().getSubjectAreaAbbv() + " " + course().getCourseNbr()).equalsIgnoreCase(term);
			}
			if ((attr == null && term.length() > 2) || "title".equals(attr)) {
				return (course().getTitle() == null ? "" : course().getTitle()).toLowerCase().contains(term.toLowerCase());
			}
			if (attr == null || "subject".equals(attr)) {
				return course().getSubjectAreaAbbv().equalsIgnoreCase(term);
			}
			if (attr == null || "number".equals(attr)) {
				return course().getCourseNbr().equalsIgnoreCase(term);
			}
			if ("department".equals(attr)) {
				return (course().getSubjectArea().getDepartment().getDeptCode() == null ? course().getSubjectArea().getDepartment().getAbbreviation() : course().getSubjectArea().getDepartment().getDeptCode()).equalsIgnoreCase(term);
				
			}
			if ("consent".equals(attr)) {
				if ("none".equalsIgnoreCase(term) || "No Consent".equalsIgnoreCase(term))
					return course().getConsentType() == null;
				else if ("todo".equalsIgnoreCase(term) || "To Do".equalsIgnoreCase(term))
					return isConsentToDoCourse();
				else
					return course().getConsentType() != null;
			}
			if ("registered".equals(attr)) {
				if ("true".equalsIgnoreCase(term) || "1".equalsIgnoreCase(term))
					return true;
				else
					return false;
			}
			return attr != null; // pass unknown attributes lower
		}
	}
	
	public static class DbCourseRequestMatcher extends DbCourseInfoMatcher {
		private static final long serialVersionUID = 1L;
		private Student iStudent;
		private CourseRequest iRequest;
		private InstructionalOffering iOffering;
		private String iDefaultStatus;
		private List<StudentClassEnrollment> iEnrollment = null;
		private NameFormat iFormat = null;
		private Reservation iReservation = null;
		private boolean iReservationGuessed = false;
		
		public DbCourseRequestMatcher(AcademicSessionInfo session, CourseRequest request, boolean isConsentToDoCourse, NameFormat format) {
			super(request.getCourseOffering(), isConsentToDoCourse);
			iStudent = request.getCourseDemand().getStudent();
			iRequest = request;
			iDefaultStatus = session.getDefaultSectioningStatus();
			iOffering = request.getCourseOffering().getInstructionalOffering();
			iFormat = format;
		}
		
		public CourseRequest request() { return iRequest; }
		public List<StudentClassEnrollment> enrollment() {
			if (iEnrollment == null)
				iEnrollment = iRequest.getClassEnrollments();
			return iEnrollment;
		}
		public Student student() { return iStudent; }
		public String status() { return student().getSectioningStatus() == null ? iDefaultStatus : student().getSectioningStatus().getReference(); }
		public InstructionalOffering offering() { return iOffering; }
		public String approval() {
			if (enrollment().isEmpty()) return null;
			Set<String> approval = new HashSet<String>();
			String ret = "";
			for (StudentClassEnrollment e: enrollment()) {
				if (e.getApprovedBy() == null || e.getApprovedDate() == null) return null;
				if (approval.add(e.getApprovedBy()))
					ret += (ret.isEmpty() ? "" : "|") + e.getApprovedBy();
			}
			return ret;
		}
		public Reservation reservation() {
			if (!iReservationGuessed) {
				iReservation = guessReservation();
				iReservationGuessed = true;
			}
			return iReservation;
		}
		
		protected Reservation guessReservation() {
			List<StudentClassEnrollment> enrollment = enrollment();
	    	if (enrollment.isEmpty()) return null;

	    	Reservation best = null;
	    	boolean mustBeUsed = false;
    		for (Reservation reservation: iOffering.getReservations()) {
    			if (reservation.isApplicable(iStudent, iRequest) && reservation.isMatching(enrollment)) {
    				if (!mustBeUsed && reservation.isMustBeUsed()) { best = null; mustBeUsed = true; }
	    			if (mustBeUsed && !reservation.isMustBeUsed()) continue;
	    			if (best == null || reservation.compareTo(best) < 0.0)
	    				best = reservation;
    			}
    		}
    		
    		return best;
		}
		
		public boolean canAssign() {
			if (!enrollment().isEmpty()) return true;
			int alt = 0;
			for (CourseDemand demand: student().getCourseDemands()) {
				boolean course = (!demand.getCourseRequests().isEmpty());
				boolean assigned = !course;
				if (course)
					for (CourseRequest request: demand.getCourseRequests())
						if (!request.getClassEnrollments().isEmpty()) { assigned = true; break; }
				if (assigned && demand.equals(request().getCourseDemand())) return false;
				boolean waitlist = (course && demand.isWaitlist());
				if (demand.isAlternative()) {
					if (assigned)
						alt --;
				} else {
					if (course && !waitlist && !assigned)
						alt ++;
				}
			}
			return alt >= 0;
		}
		
		@Override
		public boolean match(String attr, String term) {
			if (attr == null || "name".equals(attr) || "title".equals(attr) || "subject".equals(attr) || "number".equals(attr) || "course".equals(attr) || "department".equals(attr) || "registered".equals(attr))
				return super.match(attr, term);
			
			if ("limit".equals(attr)) return true;
			
			if ("area".equals(attr)) {
				for (StudentAreaClassificationMajor acm: student().getAreaClasfMajors())
					if (eq(acm.getAcademicArea().getAcademicAreaAbbreviation(), term)) return true;
			}
			
			if ("clasf".equals(attr) || "classification".equals(attr)) {
				for (StudentAreaClassificationMajor acm: student().getAreaClasfMajors())
					if (eq(acm.getAcademicClassification().getCode(), term)) return true;
			}
			
			if ("major".equals(attr)) {
				for (StudentAreaClassificationMajor acm: student().getAreaClasfMajors())
					if (eq(acm.getMajor().getCode(), term)) return true;
			}
			
			if ("group".equals(attr)) {
				for (StudentGroup group: student().getGroups())
					if (eq(group.getGroupAbbreviation(), term)) return true;
			}
			
			if ("accommodation".equals(attr)) {
				for (StudentAccomodation acc: student().getAccomodations())
					if (eq(acc.getAbbreviation(), term)) return true;
			}
			
			if ("student".equals(attr)) {
				return has(iFormat.format(student()), term) || eq(student().getExternalUniqueId(), term) || eq(iFormat.format(student()), term);
			}
			
			if ("assignment".equals(attr)) {
				if (eq("Assigned", term)) {
					return !enrollment().isEmpty();
				} else if (eq("Reserved", term)) {
					return !enrollment().isEmpty() && reservation() != null;
				} else if (eq("Not Assigned", term)) {
					return enrollment().isEmpty();
				} else if (eq("Wait-Listed", term)) {
					return enrollment().isEmpty() && request().getCourseDemand().isWaitlist();
				}
			}
			
			if ("assigned".equals(attr) || "scheduled".equals(attr)) {
				if (eq("true", term) || eq("1",term))
					return !enrollment().isEmpty();
				else
					return enrollment().isEmpty();
			}
			
			if ("waitlisted".equals(attr) || "waitlist".equals(attr)) {
				if (eq("true", term) || eq("1",term))
					return enrollment().isEmpty() && request().getCourseDemand().isWaitlist();
				else
					return !enrollment().isEmpty();
			}
			
			if ("reservation".equals(attr) || "reserved".equals(attr)) {
				if (eq("true", term) || eq("1",term))
					return !enrollment().isEmpty() && reservation() != null;
				else
					return !enrollment().isEmpty() && reservation() == null;
			}
			
			if ("consent".equals(attr)) {
				if (eq("none", term) || eq("No Consent", term)) {
					return course().getConsentType() == null;
				} else if (eq("Required", term) || eq("Consent", term)) {
					return course().getConsentType() != null && !enrollment().isEmpty();
				} else if (eq("Approved", term)) {
					return course().getConsentType() != null && !enrollment().isEmpty() && approval() != null;
				} else if (eq("Waiting", term)) {
					return course().getConsentType() != null && !enrollment().isEmpty() && approval() == null;
				} else if (eq("todo", term) || eq("To Do", term)) {
					return isConsentToDoCourse() && course().getConsentType() != null && !enrollment().isEmpty() && approval() == null;
				} else {
					return course().getConsentType() != null && (!enrollment().isEmpty() && ((approval() != null && has(approval(), term)) || eq(course().getConsentType().getAbbv(), term)));
				}
			}
			
			if ("approver".equals(attr)) {
				return course().getConsentType() != null && !enrollment().isEmpty() && approval() != null && has(approval(), term);
			}
			
			if ("status".equals(attr)) {
				if ("default".equalsIgnoreCase(term) || "Not Set".equalsIgnoreCase(term))
					return student().getSectioningStatus() == null;
				return term.equalsIgnoreCase(status());
			}
			
			if (!enrollment().isEmpty()) {
				for (StudentClassEnrollment e: enrollment()) {
					if (attr == null || attr.equals("crn") || attr.equals("id") || attr.equals("externalId") || attr.equals("exid") || attr.equals("name")) {
						if (e.getClazz().getClassSuffix(e.getCourseOffering()).toLowerCase().startsWith(term.toLowerCase()))
							return true;
					}
					if (attr == null || attr.equals("day")) {
						Assignment assignment = e.getClazz().getCommittedAssignment();
						if (assignment == null && term.equalsIgnoreCase("none")) return true;
						if (assignment != null) {
							int day = parseDay(term);
							if (day > 0 && (assignment.getDays() & day) == day) return true;
						}
					}
					if (attr == null || attr.equals("time")) {
						Assignment assignment = e.getClazz().getCommittedAssignment();
						if (assignment == null && term.equalsIgnoreCase("none")) return true;
						if (assignment != null) {
							int start = parseStart(term);
							if (start >= 0 && assignment.getStartSlot() == start) return true;
						}
					}
					if (attr != null && attr.equals("before")) {
						Assignment assignment = e.getClazz().getCommittedAssignment();
						if (assignment != null) {
							int end = parseStart(term);
							if (end >= 0 && assignment.getStartSlot() + assignment.getSlotPerMtg() - assignment.getBreakTime() / 5 <= end) return true;
						}
					}
					if (attr != null && attr.equals("after")) {
						Assignment assignment = e.getClazz().getCommittedAssignment();
						if (assignment != null) {
							int start = parseStart(term);
							if (start >= 0 && assignment.getStartSlot() >= start) return true;
						}
					}
					if (attr == null || attr.equals("date")) {
						ClassEvent event = e.getClazz().getEvent();
						if (event == null && term.equalsIgnoreCase("none")) return true;
						if (event != null) {
							Formats.Format<Date> df = Formats.getDateFormat(Formats.Pattern.DATE_PATTERN);
							for (Meeting m: event.getMeetings()) {
								if (eq(df.format(m.getMeetingDate()), term)) return true;
							}
						}
					}
					if (attr == null || attr.equals("room")) {
						Assignment assignment = e.getClazz().getCommittedAssignment();
						if ((assignment == null || assignment.getRooms().isEmpty()) && term.equalsIgnoreCase("none")) return true;
						if (assignment != null) {
							for (Location room: assignment.getRooms()) {
								if (has(room.getLabel(), term)) return true;
							}
						}
					}
					if (attr == null || attr.equals("instr") || attr.equals("instructor")) {
						if (attr != null && e.getClazz().getClassInstructors().isEmpty() && term.equalsIgnoreCase("none")) return true;
						for (ClassInstructor instuctor: e.getClazz().getClassInstructors()) {
							if (has(iFormat.format(instuctor.getInstructor()), term) || eq(instuctor.getInstructor().getExternalUniqueId(), term)) return true;
							if (instuctor.getInstructor().getEmail() != null) {
								String email = instuctor.getInstructor().getEmail();
								if (email.indexOf('@') >= 0) email = email.substring(0, email.indexOf('@'));
								if (eq(email, term)) return true;
							}
						}
					}
					if (attr != null) {
						int start = parseStart(attr + ":" + term);
						if (start >= 0 && e.getClazz().getCommittedAssignment() != null && e.getClazz().getCommittedAssignment().getStartSlot() == start) return true;
					}
				}
			}
			
			return false;
		}

		private boolean eq(String name, String term) {
			if (name == null) return false;
			return name.equalsIgnoreCase(term);
		}

		private boolean has(String name, String term) {
			if (name == null) return false;
			if (eq(name, term)) return true;
			for (String t: name.split(" |,"))
				if (t.equalsIgnoreCase(term)) return true;
			return false;
		}
		
		private int parseDay(String token) {
			int days = 0;
			boolean found = false;
			do {
				found = false;
				for (int i=0; i<CONSTANTS.longDays().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.longDays()[i].toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(CONSTANTS.longDays()[i].length());
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
				for (int i=0; i<CONSTANTS.days().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.days()[i].toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(CONSTANTS.days()[i].length());
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
				for (int i=0; i<CONSTANTS.days().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.days()[i].substring(0,2).toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(2);
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
				for (int i=0; i<CONSTANTS.shortDays().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.shortDays()[i].toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(CONSTANTS.shortDays()[i].length());
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
				for (int i=0; i<CONSTANTS.freeTimeShortDays().length; i++) {
					if (token.toLowerCase().startsWith(CONSTANTS.freeTimeShortDays()[i].toLowerCase())) {
						days |= DayCode.values()[i].getCode(); 
						token = token.substring(CONSTANTS.freeTimeShortDays()[i].length());
						while (token.startsWith(" ")) token = token.substring(1);
						found = true;
					}
				}
			} while (found);
			return (token.isEmpty() ? days : 0);
		}
		
		private int parseStart(String token) {
			int startHour = 0, startMin = 0;
			String number = "";
			while (!token.isEmpty() && token.charAt(0) >= '0' && token.charAt(0) <= '9') { number += token.substring(0, 1); token = token.substring(1); }
			if (number.isEmpty()) return -1;
			if (number.length() > 2) {
				startHour = Integer.parseInt(number) / 100;
				startMin = Integer.parseInt(number) % 100;
			} else {
				startHour = Integer.parseInt(number);
			}
			while (token.startsWith(" ")) token = token.substring(1);
			if (token.startsWith(":")) {
				token = token.substring(1);
				while (token.startsWith(" ")) token = token.substring(1);
				number = "";
				while (!token.isEmpty() && token.charAt(0) >= '0' && token.charAt(0) <= '9') { number += token.substring(0, 1); token = token.substring(1); }
				if (number.isEmpty()) return -1;
				startMin = Integer.parseInt(number);
			}
			while (token.startsWith(" ")) token = token.substring(1);
			boolean hasAmOrPm = false;
			if (token.toLowerCase().startsWith("am")) { token = token.substring(2); hasAmOrPm = true; }
			if (token.toLowerCase().startsWith("a")) { token = token.substring(1); hasAmOrPm = true; }
			if (token.toLowerCase().startsWith("pm")) { token = token.substring(2); hasAmOrPm = true; if (startHour<12) startHour += 12; }
			if (token.toLowerCase().startsWith("p")) { token = token.substring(1); hasAmOrPm = true; if (startHour<12) startHour += 12; }
			if (startHour < 7 && !hasAmOrPm) startHour += 12;
			if (startMin % 5 != 0) startMin = 5 * ((startMin + 2)/ 5);
			if (startHour == 7 && startMin == 0 && !hasAmOrPm) startHour += 12;
			return (60 * startHour + startMin) / 5;
		}
	}
	
	public static class DbStudentMatcher implements TermMatcher {
		private Student iStudent;
		private String iDefaultStatus;
		private NameFormat iFormat = null;
		
		public DbStudentMatcher(Student student, String defaultStatus, NameFormat format) {
			iStudent = student;
			iDefaultStatus = defaultStatus;
			iFormat = format;
		}
		
		public DbStudentMatcher(Student student) {
			iStudent = student;
			iDefaultStatus = (student.getSession().getDefaultSectioningStatus() == null ? null : student.getSession().getDefaultSectioningStatus().getReference());
			iFormat = NameFormat.fromReference(ApplicationProperty.OnlineSchedulingStudentNameFormat.value());
		}

		public Student student() { return iStudent; }
		public String status() {  return (iStudent.getSectioningStatus() == null ? iDefaultStatus : iStudent.getSectioningStatus().getReference()); }
		
		@Override
		public boolean match(String attr, String term) {
			if (attr == null && term.isEmpty()) return true;
			if ("limit".equals(attr)) return true;
			if ("area".equals(attr)) {
				for (StudentAreaClassificationMajor acm: student().getAreaClasfMajors())
					if (eq(acm.getAcademicArea().getAcademicAreaAbbreviation(), term)) return true;
			} else if ("clasf".equals(attr) || "classification".equals(attr)) {
				for (StudentAreaClassificationMajor acm: student().getAreaClasfMajors())
					if (eq(acm.getAcademicClassification().getCode(), term)) return true;
			} else if ("major".equals(attr)) {
				for (StudentAreaClassificationMajor acm: student().getAreaClasfMajors())
					if (eq(acm.getMajor().getCode(), term)) return true;
			} else if ("group".equals(attr)) {
				for (StudentGroup group: student().getGroups())
					if (eq(group.getGroupAbbreviation(), term)) return true;
			} else if ("accommodation".equals(attr)) {
				for (StudentAccomodation acc: student().getAccomodations())
					if (eq(acc.getAbbreviation(), term)) return true;
			} else if  ("student".equals(attr)) {
				return has(iFormat.format(student()), term) || eq(student().getExternalUniqueId(), term) || eq(iFormat.format(student()), term);
			} else if ("registered".equals(attr)) {
				if (eq("true", term) || eq("1",term))
					return false;
				else
					return true;
			} else if ("status".equals(attr)) {
				if ("default".equalsIgnoreCase(term) || "Not Set".equalsIgnoreCase(term))
					return iStudent.getSectioningStatus() == null;
				return term.equalsIgnoreCase(status());
			}
			return false;
		}
		
		private boolean eq(String name, String term) {
			if (name == null) return false;
			return name.equalsIgnoreCase(term);
		}

		private boolean has(String name, String term) {
			if (name == null) return false;
			if (eq(name, term)) return true;
			for (String t: name.split(" |,"))
				if (t.equalsIgnoreCase(term)) return true;
			return false;
		}
	}

	public static class DbFindStudentInfoMatcher extends FindStudentInfoMatcher {
		private static final long serialVersionUID = 1L;
		protected NameFormat iFormat;
		
		public DbFindStudentInfoMatcher(AcademicSessionInfo session, Query query, NameFormat format) {
			super(session, query);
			iFormat = format;
		}

		public boolean match(Student student) {
			return student != null && iQuery.match(new DbStudentMatcher(student, iDefaultSectioningStatus, iFormat));
		}
	}

}
