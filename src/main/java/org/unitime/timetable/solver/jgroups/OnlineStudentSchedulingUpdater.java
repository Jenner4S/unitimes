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
package org.unitime.timetable.solver.jgroups;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.StudentSectioningQueue;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.StudentSectioningQueueDAO;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.onlinesectioning.AcademicSessionInfo;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer.ServerCallback;
import org.unitime.timetable.onlinesectioning.updates.CheckAllOfferingsAction;
import org.unitime.timetable.onlinesectioning.updates.ClassAssignmentChanged;
import org.unitime.timetable.onlinesectioning.updates.ExpireReservationsAction;
import org.unitime.timetable.onlinesectioning.updates.PersistExpectedSpacesAction;
import org.unitime.timetable.onlinesectioning.updates.ReloadAllData;
import org.unitime.timetable.onlinesectioning.updates.ReloadAllStudents;
import org.unitime.timetable.onlinesectioning.updates.ReloadOfferingAction;
import org.unitime.timetable.onlinesectioning.updates.ReloadStudent;

/**
 * @author Tomas Muller
 */
public class OnlineStudentSchedulingUpdater extends Thread {
	private Logger iLog;
	private long iSleepTimeInSeconds = 5;
	private boolean iRun = true;
	
	private OnlineStudentSchedulingContainer iContainer = null;
	private AcademicSessionInfo iSession = null; 
	private Date iLastTimeStamp = null;
	
	public OnlineStudentSchedulingUpdater(OnlineStudentSchedulingContainer container, AcademicSessionInfo session, Date lastTimeStamp) {
		super();
		iContainer = container;
		iSession = session;
		iLastTimeStamp = lastTimeStamp;
		setDaemon(true);
		setName("Updater[" + getAcademicSession().toCompactString() + "]");
		iSleepTimeInSeconds = ApplicationProperty.OnlineSchedulingQueueUpdateInterval.intValue();
		iLog = Logger.getLogger(OnlineStudentSchedulingUpdater.class + ".updater[" + getAcademicSession().toCompactString() + "]"); 
	}
	
	public void run() {
		try {
			iLog.info(getAcademicSession() + " updater started.");
			if (getAcademicSession() != null)
				ApplicationProperties.setSessionId(getAcademicSession().getUniqueId());
			while (iRun) {
				try {
					sleep(iSleepTimeInSeconds * 1000);
				} catch (InterruptedException e) {}
				if (!iRun) break;
				try {
					OnlineSectioningServer server = iContainer.getInstance(getAcademicSession().getUniqueId());
					if (server != null) {
						if (server.isMaster()) {
							checkForUpdates(server);
							if (!iRun) break;
							checkForExpiredReservations(server);
							persistExpectedSpaces(server);
						} else if (!ApplicationProperty.OnlineSchedulingServerReplicated.isTrue()) {
							// not master, but replication is disabled -> unload
							try {
								iContainer.unload(getAcademicSession().getUniqueId(), false);
							} catch (Exception e) {
								iLog.error("Failed to unload server: " + e.getMessage(), e);
							}
						}
					}
				} finally {
					_RootDAO.closeCurrentThreadSessions();
				}
			}
			iLog.info((getAcademicSession() == null ? "Generic" : getAcademicSession().toString()) + " updater stopped.");
		} catch (Exception e) {
			iLog.error((getAcademicSession() == null ? "Generic" : getAcademicSession().toString()) + " updater failed, " + e.getMessage(), e);
		} finally {
			ApplicationProperties.setSessionId(null);
		}
	}
	
	public AcademicSessionInfo getAcademicSession() {
		return iSession;
	}
	
	public OnlineSectioningServer getServer() {
		if (getAcademicSession() == null) return null;
		return iContainer.getInstance(getAcademicSession().getUniqueId());
	}
	
	public void checkForUpdates(OnlineSectioningServer server) {
		try {
			org.hibernate.Session hibSession = StudentSectioningQueueDAO.getInstance().createNewSession();
			try {
				iLastTimeStamp = server.getProperty("Updater.LastTimeStamp", iLastTimeStamp);
				for (StudentSectioningQueue q: StudentSectioningQueue.getItems(hibSession, getAcademicSession().getUniqueId(), iLastTimeStamp)) {
					try {
						processChange(server, q);
					} catch (Exception e) {
						iLog.error("Update failed: " + e.getMessage(), e);
					}
					if (!iRun) break;
					iLastTimeStamp = q.getTimeStamp();
					server.setProperty("Updater.LastTimeStamp", iLastTimeStamp);
				}
			} finally {
				hibSession.close();
			}
		} catch (Exception e) {
			iLog.error("Unable to check for updates: " + e.getMessage(), e);
		}
	}
	
	public void checkForExpiredReservations(OnlineSectioningServer server) {
		long ts = System.currentTimeMillis(); // current time stamp
		// the check was done within the last hour -> no need to repeat
		Long lastReservationCheck = server.getProperty("Updater.LastReservationCheck", null);
		if (lastReservationCheck != null && ts - lastReservationCheck < 3600000) return;
		
		if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 0) {
			// first time after midnight (TODO: allow change)
			server.setProperty("Updater.LastReservationCheck", ts);
			try {
				server.execute(server.createAction(ExpireReservationsAction.class), user());
			} catch (Exception e) {
				iLog.error("Expire reservations failed: " + e.getMessage(), e);
			}
		}
	}
	
	protected OnlineSectioningLog.Entity user() {
		return OnlineSectioningLog.Entity.newBuilder()
			.setExternalId(StudentClassEnrollment.SystemChange.SYSTEM.name())
			.setName(StudentClassEnrollment.SystemChange.SYSTEM.getName())
			.setType(OnlineSectioningLog.Entity.EntityType.OTHER).build();
	}
	
	public void persistExpectedSpaces(OnlineSectioningServer server) {
		try {
			List<Long> offeringIds = server.getOfferingsToPersistExpectedSpaces(2000 * iSleepTimeInSeconds);
			if (!offeringIds.isEmpty()) {
				server.execute(server.createAction(PersistExpectedSpacesAction.class).forOfferings(offeringIds), user(), new ServerCallback<Boolean>() {
					@Override
					public void onSuccess(Boolean result) {}
					@Override
					public void onFailure(Throwable exception) {
						iLog.error("Failed to persist expected spaces: " + exception.getMessage(), exception);
					}
				});
			}
		} catch (Exception e) {
			iLog.error("Failed to persist expected spaces: " + e.getMessage(), e);
		}
	}
	
	protected void processChange(OnlineSectioningServer server, StudentSectioningQueue q) {
		switch (StudentSectioningQueue.Type.values()[q.getType()]) {
		case SESSION_RELOAD:
			iLog.info("Reloading " + server.getAcademicSession());
			server.execute(server.createAction(ReloadAllData.class), q.getUser());
			if (server.getAcademicSession().isSectioningEnabled())
				server.execute(server.createAction(CheckAllOfferingsAction.class), q.getUser());
			break;
		case SESSION_STATUS_CHANGE:
			Session session = SessionDAO.getInstance().get(iSession.getUniqueId());
			if (session == null || (!session.getStatusType().canSectionAssistStudents() && !session.getStatusType().canOnlineSectionStudents())) {
				if (iContainer instanceof OnlineStudentSchedulingContainerRemote) {
					try {
						((OnlineStudentSchedulingContainerRemote)iContainer).getDispatcher().callRemoteMethods(
								null, "unload", new Object[] { session.getUniqueId(), false }, new Class[] { Long.class, boolean.class }, SolverServerImplementation.sAllResponses);
					} catch (Exception e) {
						iLog.error("Failed to unload server: " + e.getMessage(), e);
					}
				} else {
					iContainer.unload(getAcademicSession().getUniqueId(), false);
				}
			} else {
				iLog.info("Session status changed for " + session.getLabel());
				if (server.getAcademicSession().isSectioningEnabled() && !session.getStatusType().canOnlineSectionStudents())
					server.releaseAllOfferingLocks();
				getAcademicSession().update(session);
				server.setProperty("AcademicSession", getAcademicSession());
			}
			break;
		case STUDENT_ENROLLMENT_CHANGE:
			List<Long> studentIds = q.getIds();
			if (studentIds == null || studentIds.isEmpty()) {
				iLog.info("All students changed for " + server.getAcademicSession());
				server.execute(server.createAction(ReloadAllStudents.class), q.getUser());
			} else {
				server.execute(server.createAction(ReloadStudent.class).forStudents(studentIds), q.getUser());
			}
			break;
		case CLASS_ASSIGNMENT_CHANGE:
			server.execute(server.createAction(ClassAssignmentChanged.class).forClasses(q.getIds()), q.getUser());
			break;
		case OFFERING_CHANGE:
			server.execute(server.createAction(ReloadOfferingAction.class).forOfferings(q.getIds()), q.getUser());
			break;
		default:
			iLog.error("Student sectioning queue type " + StudentSectioningQueue.Type.values()[q.getType()] + " not known.");
		}
	}

	public void stopUpdating(boolean interrupt) {
		iRun = false;
		if (interrupt) {
			interrupt();
			try {
				this.join();
			} catch (InterruptedException e) {}
		}
	}
}