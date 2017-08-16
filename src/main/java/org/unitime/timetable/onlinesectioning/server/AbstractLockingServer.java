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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;
import org.unitime.timetable.onlinesectioning.MultiLock;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServerContext;
import org.unitime.timetable.onlinesectioning.model.XCourseId;
import org.unitime.timetable.onlinesectioning.model.XCourseRequest;
import org.unitime.timetable.onlinesectioning.model.XRequest;
import org.unitime.timetable.onlinesectioning.model.XStudent;

/**
 * @author Tomas Muller
 */
public abstract class AbstractLockingServer extends AbstractServer {
	private ReentrantReadWriteLock iLock = new ReentrantReadWriteLock();
	private MultiLock iMultiLock;
	private Map<Long, Lock> iOfferingLocks = new Hashtable<Long, Lock>();
	
	public AbstractLockingServer(OnlineSectioningServerContext context) throws SectioningException {
		super(context);
	}
	
	protected AbstractLockingServer(AcademicSessionInfo session, boolean allowAsyncCalls) {
		super(session, allowAsyncCalls);
		iMultiLock = new MultiLock(getAcademicSession());
	}
	
	@Override
	protected void load(OnlineSectioningServerContext context) {
		iMultiLock = new MultiLock(getAcademicSession());
		super.load(context);
	}
	
	@Override
	public Lock readLock() {
		if (iLock == null)
			return new Lock() {
				public void release() {}
			};
		iLock.readLock().lock();
		return new Lock() {
			public void release() {
				iLock.readLock().unlock();
			}
		};
	}
	
	public Lock writeLockIfNotHeld() {
		if (iLock == null || iLock.isWriteLockedByCurrentThread()) return null;
		iLock.writeLock().lock();
		return new Lock() {
			public void release() {
				iLock.writeLock().unlock();
			}
		};
	}

	@Override
	public Lock writeLock() {
		if (iLock == null)
			return new Lock() {
				public void release() {}
			};
		iLock.writeLock().lock();
		return new Lock() {
			public void release() {
				iLock.writeLock().unlock();
			}
		};
	}

	@Override
	public Lock lockAll() {
		if (iLock == null)
			return new Lock() {
				public void release() {}
			};
		iLock.writeLock().lock();
		return new Lock() {
			public void release() {
				iLock.writeLock().unlock();
			}
		};
	}
	
	@Override
	public Lock lockStudent(Long studentId, Collection<Long> offeringIds, String actionName) {
		Set<Long> ids = new HashSet<Long>();
		boolean lockStudents = getConfig().getPropertyBoolean(actionName + ".LockStudents", true);
		boolean lockOfferings = getConfig().getPropertyBoolean(actionName + ".LockOfferings", true);
		boolean excludeLockedOfferings = lockOfferings && getConfig().getPropertyBoolean(actionName + ".ExcludeLockedOfferings", true);
		iLock.readLock().lock();
		try {
			if (lockStudents) {
				ids.add(-studentId);
			}
			
			if (lockOfferings) {
				if (offeringIds != null)
					for (Long offeringId: offeringIds)
					if (!excludeLockedOfferings || !isOfferingLocked(offeringId))
						ids.add(offeringId);
				
				XStudent student = getStudent(studentId);
				
				if (student != null)
					for (XRequest r: student.getRequests()) {
						if (r instanceof XCourseRequest && ((XCourseRequest)r).getEnrollment() != null) {
							Long offeringId = ((XCourseRequest)r).getEnrollment().getOfferingId();
							if (!excludeLockedOfferings || !isOfferingLocked(offeringId)) ids.add(offeringId);
						}
					}
			}
		} finally {
			iLock.readLock().unlock();
		}
		return iMultiLock.lock(ids);
	}
	
	@Override
	public Lock lockOffering(Long offeringId, Collection<Long> studentIds, String actionName) {
		Set<Long> ids = new HashSet<Long>();
		boolean lockStudents = getConfig().getPropertyBoolean(actionName + ".LockStudents", true);
		boolean lockOfferings = getConfig().getPropertyBoolean(actionName + ".LockOfferings", true);
		boolean excludeLockedOffering = lockOfferings && getConfig().getPropertyBoolean(actionName + ".ExcludeLockedOfferings", true);
		iLock.readLock().lock();
		try {
			if (lockOfferings) {
				if (!excludeLockedOffering || !isOfferingLocked(offeringId))
					ids.add(offeringId);
			}
			
			if (lockStudents) {
				if (studentIds != null)
					for (Long studentId: studentIds)
					ids.add(-studentId);
				
				Collection<XCourseRequest> requests = getRequests(offeringId);
				if (requests != null) {
					for (XCourseRequest request: requests)
						ids.add(-request.getStudentId());
				}
			}
		} finally {
			iLock.readLock().unlock();
		}
		return iMultiLock.lock(ids);
	}
	
	private Long getOfferingIdFromCourseName(String courseName) {
		if (courseName == null) return null;
		XCourseId c = getCourse(courseName);
		return (c == null ? null : c.getOfferingId());
	}
	
	public Lock lockRequest(CourseRequestInterface request, String actionName) {
		Set<Long> ids = new HashSet<Long>();
		boolean lockStudents = getConfig().getPropertyBoolean(actionName + ".LockStudents", true);
		boolean lockOfferings = getConfig().getPropertyBoolean(actionName + ".LockOfferings", true);
		boolean excludeLockedOffering = lockOfferings && getConfig().getPropertyBoolean(actionName + ".ExcludeLockedOfferings", true);
		iLock.readLock().lock();
		try {
			if (lockStudents) {
				if (request.getStudentId() != null)
					ids.add(-request.getStudentId());
			}
			
			if (lockOfferings) {
				for (CourseRequestInterface.Request r: request.getCourses()) {
					if (r.hasRequestedCourse()) {
						for (CourseRequestInterface.RequestedCourse rc: r.getRequestedCourse()) {
							if (rc.hasCourseId()) {
								XCourseId c = getCourse(rc.getCourseId());
								Long id = (c == null ? null : c.getOfferingId());
								if (id != null && (!excludeLockedOffering || !isOfferingLocked(id))) ids.add(id);
							} else if (rc.hasCourseName()) {
								Long id = getOfferingIdFromCourseName(rc.getCourseName());
								if (id != null && (!excludeLockedOffering || !isOfferingLocked(id))) ids.add(id);
							}
						}
					}
				}
				for (CourseRequestInterface.Request r: request.getAlternatives()) {
					if (r.hasRequestedCourse()) {
						for (CourseRequestInterface.RequestedCourse rc: r.getRequestedCourse()) {
							if (rc.hasCourseId()) {
								XCourseId c = getCourse(rc.getCourseId());
								Long id = (c == null ? null : c.getOfferingId());
								if (id != null && (!excludeLockedOffering || !isOfferingLocked(id))) ids.add(id);
							} else if (rc.hasCourseName()) {
								Long id = getOfferingIdFromCourseName(rc.getCourseName());
								if (id != null && (!excludeLockedOffering || !isOfferingLocked(id))) ids.add(id);
							}
						}
					}
				}
			}
		} finally {
			iLock.readLock().unlock();
		}
		return iMultiLock.lock(ids);
	}

	@Override
	public boolean isOfferingLocked(Long offeringId) {
		synchronized (iOfferingLocks) {
			return iOfferingLocks.containsKey(offeringId);
		}
	}

	@Override
	public void lockOffering(Long offeringId) {
		synchronized (iOfferingLocks) {
			if (iOfferingLocks.containsKey(offeringId)) return;
		}
		Lock lock = iMultiLock.lock(offeringId);
		synchronized (iOfferingLocks) {
			if (iOfferingLocks.containsKey(offeringId))
				lock.release();
			else
				iOfferingLocks.put(offeringId, lock);
		}
	}

	@Override
	public void unlockOffering(Long offeringId) {
		synchronized (iOfferingLocks) {
			Lock lock = iOfferingLocks.remove(offeringId);
			if (lock != null)
				lock.release();
		}
	}
	
	@Override
	public Collection<Long> getLockedOfferings() {
		synchronized (iOfferingLocks) {
			return new ArrayList<Long>(iOfferingLocks.keySet());
		}
	}
	
	@Override
	public void releaseAllOfferingLocks() {
		synchronized (iOfferingLocks) {
			for (Lock lock: iOfferingLocks.values())
				lock.release();
			iOfferingLocks.clear();
		}
	}
	
	
}
