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
package org.unitime.timetable.onlinesectioning.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServerContext;
import org.unitime.timetable.onlinesectioning.match.CourseMatcher;
import org.unitime.timetable.onlinesectioning.match.StudentMatcher;
import org.unitime.timetable.onlinesectioning.model.XCourse;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XEnrollment;
import org.unitime.timetable.onlinesectioning.model.XExpectations;
import org.unitime.timetable.onlinesectioning.model.XOffering;
import org.unitime.timetable.onlinesectioning.model.XRequest;
import org.unitime.timetable.onlinesectioning.model.XStudent;

/**
 * @author Tomas Muller
 */
public class InMemoryServer extends AbstractLockingServer {
	private Hashtable<Long, XCourseId> iCourseForId = new Hashtable<Long, XCourseId>();
	private Hashtable<String, TreeSet<XCourseId>> iCourseForName = new Hashtable<String, TreeSet<XCourseId>>();
	
	private Hashtable<Long, XStudent> iStudentTable = new Hashtable<Long, XStudent>();
	private Hashtable<Long, XOffering> iOfferingTable = new Hashtable<Long, XOffering>();
	private Hashtable<Long, List<XCourseRequest>> iOfferingRequests = new Hashtable<Long, List<XCourseRequest>>();
	private Hashtable<Long, XExpectations> iExpectations = new Hashtable<Long, XExpectations>();
	private Hashtable<String, Set<Long>> iInstructedOfferings = new Hashtable<String, Set<Long>>();
	
	public InMemoryServer(OnlineSectioningServerContext context) throws SectioningException {
		super(context);
	}

	@Override
	public Collection<XCourseId> findCourses(String query, Integer limit, CourseMatcher matcher) {
		if (matcher != null) matcher.setServer(this);
		Lock lock = readLock();
		try {
			SubSet<XCourseId> ret = new SubSet<XCourseId>(limit, new CourseComparator(query));
			String queryInLowerCase = query.toLowerCase();
			for (XCourseId c : iCourseForId.values()) {
				if (c.matchCourseName(queryInLowerCase) && (matcher == null || matcher.match(c))) ret.add(c);
			}
			if (!ret.isLimitReached() && queryInLowerCase.length() > 2) {
				for (XCourseId c : iCourseForId.values()) {
					if (c.matchTitle(queryInLowerCase) && (matcher == null || matcher.match(c))) ret.add(c);
				}
			}
			return ret;
		} finally {
			lock.release();
		}
	}

	@Override
	public Collection<XCourseId> findCourses(CourseMatcher matcher) {
		if (matcher != null) matcher.setServer(this);
		Lock lock = readLock();
		try {
			Set<XCourseId> ret = new TreeSet<XCourseId>();
			for (XCourseId c : iCourseForId.values()) {
				if (matcher.match(c)) ret.add(c);
			}
			return ret;
		} finally {
			lock.release();
		}
	}

	@Override
	public Collection<XStudent> findStudents(StudentMatcher matcher) {
		if (matcher != null) matcher.setServer(this);
		Lock lock = readLock();
		try {
			List<XStudent> ret = new ArrayList<XStudent>();
			for (XStudent s: iStudentTable.values())
				if (matcher.match(s)) ret.add(s);
			return ret;
		} finally {
			lock.release();
		}
	}

	@Override
	public XCourseId getCourse(String course) {
		Lock lock = readLock();
		try {
			for (int idx = course.indexOf('-'); idx >= 0; idx = course.indexOf('-', idx + 1)) {
				String courseName = course.substring(0, idx).trim();
				String title = course.substring(idx + 1).trim();
				TreeSet<XCourseId> infos = iCourseForName.get(courseName.toLowerCase());
				if (infos!= null && !infos.isEmpty())
					for (XCourseId info: infos)
						if (title.equalsIgnoreCase(info.getTitle())) return info;
			}
			TreeSet<XCourseId> infos = iCourseForName.get(course.toLowerCase());
			if (infos!= null && !infos.isEmpty()) return infos.first();
			return null;
		} finally {
			lock.release();
		}
	}
	
	private XCourse toCourse(XCourseId course) {
		if (course == null) return null;
		if (course instanceof XCourse)
			return (XCourse)course;
		XOffering offering = getOffering(course.getOfferingId());
		return offering == null ? null : offering.getCourse(course);
	}
	
	@Override
	public XCourse getCourse(Long courseId) {
		Lock lock = readLock();
		try {
			return toCourse(iCourseForId.get(courseId));
		} finally {
			lock.release();
		}
	}

	@Override
	public XStudent getStudent(Long studentId) {
		Lock lock = readLock();
		try {
			return iStudentTable.get(studentId);
		} finally {
			lock.release();
		}
	}

	@Override
	public XOffering getOffering(Long offeringId) {
		Lock lock = readLock();
		try {
			return iOfferingTable.get(offeringId);
		} finally {
			lock.release();
		}
	}

	@Override
	public Collection<XCourseRequest> getRequests(Long offeringId) {
		Lock lock = readLock();
		try {
			return iOfferingRequests.get(offeringId);
		} finally {
			lock.release();
		}		
	}

	@Override
	public XExpectations getExpectations(Long offeringId) {
		Lock lock = readLock();
		try {
			XExpectations expectations = iExpectations.get(offeringId);
			return expectations == null ? new XExpectations(offeringId) : expectations;
		} finally {
			lock.release();
		}
	}

	@Override
	public void update(XExpectations expectations) {
		Lock lock = writeLock();
		try {
			iExpectations.put(expectations.getOfferingId(), expectations);
		} finally {
			lock.release();
		}
	}

	@Override
	public void remove(XStudent student) {
		Lock lock = writeLock();
		try {
			XStudent oldStudent = iStudentTable.remove(student.getStudentId());
			if (oldStudent != null) {
				for (XRequest request: oldStudent.getRequests())
					if (request instanceof XCourseRequest)
						for (XCourseId course: ((XCourseRequest)request).getCourseIds()) {
							List<XCourseRequest> requests = iOfferingRequests.get(course.getOfferingId());
							if (requests != null) requests.remove(request);
						}
			}
		} finally {
			lock.release();
		}
	}

	@Override
	public void update(XStudent student, boolean updateRequests) {
		Lock lock = writeLock();
		try {
			XStudent oldStudent = iStudentTable.put(student.getStudentId(), student);
			if (updateRequests) {
				if (oldStudent != null) {
					for (XRequest request: oldStudent.getRequests())
						if (request instanceof XCourseRequest)
							for (XCourseId course: ((XCourseRequest)request).getCourseIds()) {
								List<XCourseRequest> requests = iOfferingRequests.get(course.getOfferingId());
								if (requests != null) requests.remove(request);
							}
				}
				for (XRequest request: student.getRequests())
					if (request instanceof XCourseRequest)
						for (XCourseId course: ((XCourseRequest)request).getCourseIds()) {
							List<XCourseRequest> requests = iOfferingRequests.get(course.getOfferingId());
							if (requests == null) {
								requests = new ArrayList<XCourseRequest>();
								iOfferingRequests.put(course.getOfferingId(), requests);
							}
							requests.add((XCourseRequest)request);
						}
			}
		} finally {
			lock.release();
		}
	}

	@Override
	public void remove(XOffering offering) {
		remove(offering, true);
	}
	
	protected void remove(XOffering offering, boolean removeExpectations) {
		Lock lock = writeLock();
		try {
			for (XCourse course: offering.getCourses()) {
				iCourseForId.remove(course.getCourseId());
				TreeSet<XCourseId> courses = iCourseForName.get(course.getCourseNameInLowerCase());
				if (courses != null) {
					courses.remove(course);
					if (courses.size() == 1) 
						for (XCourseId x: courses) x.setHasUniqueName(true);
					if (courses.isEmpty())
						iCourseForName.remove(course.getCourseNameInLowerCase());
				}
			}
			iOfferingTable.remove(offering.getOfferingId());
			if (removeExpectations)
				iExpectations.remove(offering.getOfferingId());
			for (String externalId: offering.getInstructorExternalIds()) {
				Set<Long> offeringIds = iInstructedOfferings.get(externalId);
				if (offeringIds != null) offeringIds.remove(offering.getOfferingId());
			}
		} finally {
			lock.release();
		}
	}

	@Override
	public void update(XOffering offering) {
		Lock lock = writeLock();
		try {
			XOffering oldOffering = iOfferingTable.get(offering.getOfferingId());
			if (oldOffering != null)
				remove(oldOffering, false);
			
			iOfferingTable.put(offering.getOfferingId(), offering);
			for (XCourse course: offering.getCourses()) {
				iCourseForId.put(course.getCourseId(), course);
				TreeSet<XCourseId> courses = iCourseForName.get(course.getCourseNameInLowerCase());
				if (courses == null) {
					courses = new TreeSet<XCourseId>();
					iCourseForName.put(course.getCourseNameInLowerCase(), courses);
				}
				courses.add(course);
				if (courses.size() == 1) 
					for (XCourseId x: courses) x.setHasUniqueName(true);
				else if (courses.size() > 1)
					for (XCourseId x: courses) x.setHasUniqueName(false);
			}
			for (String externalId: offering.getInstructorExternalIds()) {
				Set<Long> offeringIds = iInstructedOfferings.get(externalId);
				if (offeringIds == null) {
					offeringIds = new HashSet<Long>();
					iInstructedOfferings.put(externalId, offeringIds);
				}
				offeringIds.add(offering.getOfferingId());
			}
		} finally {
			lock.release();
		}
	}

	@Override
	public void clearAll() {
		Lock lock = writeLock();
		try {
			if (iStudentTable == null)
				iStudentTable = new Hashtable<Long, XStudent>();
			else
				iStudentTable.clear();
			if (iOfferingTable == null)
				iOfferingTable = new Hashtable<Long, XOffering>();
			else
				iOfferingTable.clear();
			if (iOfferingRequests == null)
				iOfferingRequests = new Hashtable<Long, List<XCourseRequest>>();
			else
				iOfferingRequests.clear();
			if (iExpectations == null)
				iExpectations = new Hashtable<Long, XExpectations>();
			else
				iExpectations.clear();
			if (iCourseForId == null)
				iCourseForId = new Hashtable<Long, XCourseId>();
			else
				iCourseForId.clear();
			if (iCourseForName == null)
				iCourseForName = new Hashtable<String, TreeSet<XCourseId>>();
			else
				iCourseForName.clear();
			if (iInstructedOfferings == null)
				iInstructedOfferings = new Hashtable<String, Set<Long>>();
			else
				iInstructedOfferings.clear();
		} finally {
			lock.release();
		}
	}

	@Override
	public void clearAllStudents() {
		Lock lock = writeLock();
		try {
			iStudentTable.clear();
			iOfferingRequests.clear();
		} finally {
			lock.release();
		}
	}

	@Override
	public XCourseRequest assign(XCourseRequest request, XEnrollment enrollment) {
		Lock lock = writeLock();
		try {
			XStudent student = iStudentTable.get(request.getStudentId());
			for (XRequest r: student.getRequests()) {
				if (r.equals(request)) {
					XCourseRequest cr = (XCourseRequest)r;

					// remove old requests
					for (XCourseId course: cr.getCourseIds()) {
						List<XCourseRequest> requests = iOfferingRequests.get(course.getOfferingId());
						if (requests != null) requests.remove(cr);
					}

					// assign
					cr.setEnrollment(enrollment);
					
					// put new requests
					for (XCourseId course: cr.getCourseIds()) {
						List<XCourseRequest> requests = iOfferingRequests.get(course.getOfferingId());
						if (requests == null) {
							requests = new ArrayList<XCourseRequest>();
							iOfferingRequests.put(course.getOfferingId(), requests);
						}
						requests.add(cr);
					}
					
					return cr;
				}
			}
			return null;
		} finally {
			lock.release();
		}
	}

	@Override
	public XCourseRequest waitlist(XCourseRequest request, boolean waitlist) {
		Lock lock = writeLock();
		try {
			XStudent student = iStudentTable.get(request.getStudentId());
			for (XRequest r: student.getRequests()) {
				if (r.equals(request)) {
					XCourseRequest cr = (XCourseRequest)r;

					// remove old requests
					for (XCourseId course: cr.getCourseIds()) {
						List<XCourseRequest> requests = iOfferingRequests.get(course.getOfferingId());
						if (requests != null) requests.remove(cr);
					}

					// assign
					cr.setWaitlist(waitlist);
					
					// put new requests
					for (XCourseId course: cr.getCourseIds()) {
						List<XCourseRequest> requests = iOfferingRequests.get(course.getOfferingId());
						if (requests == null) {
							requests = new ArrayList<XCourseRequest>();
							iOfferingRequests.put(course.getOfferingId(), requests);
						}
						requests.add(cr);
					}
					
					return cr;
				}
			}
			return null;
		} finally {
			lock.release();
		}
	}

	@Override
	public Collection<Long> getInstructedOfferings(String instructorExternalId) {
		Lock lock = readLock();
		try {
			return iInstructedOfferings.get(instructorExternalId);
		} finally {
			lock.release();
		}
	}

}
