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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cpsolver.ifs.util.DataProperties;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jgroups.blocks.locking.LockService;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.gwt.shared.SectioningException;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.StudentSectioningQueue;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLogger;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServerContext;
import org.unitime.timetable.onlinesectioning.server.InMemoryServer;
import org.unitime.timetable.onlinesectioning.server.ReplicatedServerWithMaster;

/**
 * @author Tomas Muller
 */
public class OnlineStudentSchedulingContainer implements SolverContainer<OnlineSectioningServer> {
	private static Log sLog = LogFactory.getLog(OnlineStudentSchedulingContainer.class);
	
	protected Hashtable<Long, OnlineSectioningServer> iInstances = new Hashtable<Long, OnlineSectioningServer>();
	private Hashtable<Long, OnlineStudentSchedulingUpdater> iUpdaters = new Hashtable<Long, OnlineStudentSchedulingUpdater>();
	
	private ReentrantReadWriteLock iGlobalLock = new ReentrantReadWriteLock();

	@Override
	public Set<String> getSolvers() {
		iGlobalLock.readLock().lock();
		try {
			TreeSet<String> ret = new TreeSet<String>();
			for (Map.Entry<Long, OnlineSectioningServer> entry: iInstances.entrySet()) {
				try {
					ret.add(entry.getValue().getAcademicSession().getUniqueId().toString());
				} catch (IllegalStateException e) {
					sLog.error("Server " + entry.getKey() + " appears to be in an inconsistent state: " + e.getMessage());
				}
			}
			return ret;
		} finally {
			iGlobalLock.readLock().unlock();
		}
	}

	@Override
	public OnlineSectioningServer getSolver(String sessionId) {
		return getInstance(Long.valueOf(sessionId));
	}
	
	@Override
	public long getMemUsage(String user) {
		OnlineSectioningServer solver = getSolver(user);
		return solver == null ? 0 : solver.getMemUsage();
	}
	
	public OnlineSectioningServer getInstance(Long sessionId) {
		iGlobalLock.readLock().lock();
		try {
			OnlineSectioningServer instance = iInstances.get(sessionId);
			if (instance == null) return null;
			try {
				instance.getAcademicSession();
			} catch (IllegalStateException e) {
				sLog.error("Server " + sessionId + " appears to be in an inconsistent state: " + e.getMessage(), e);
				return null;
			}
			return instance;
		} finally {
			iGlobalLock.readLock().unlock();
		}
	}

	@Override
	public boolean hasSolver(String sessionId) {
		iGlobalLock.readLock().lock();
		try {
			return iInstances.containsKey(Long.valueOf(sessionId));
		} finally {
			iGlobalLock.readLock().unlock();
		}
	}

	@Override
	public OnlineSectioningServer createSolver(String sessionId, DataProperties config) {
		if (!canCreateSolver(Long.valueOf(sessionId))) return null;
		return createInstance(Long.valueOf(sessionId), config);
	}
	
	protected boolean canCreateSolver(Long sessionId) {
		org.hibernate.Session hibSession = SessionDAO.getInstance().createNewSession();
		try {
			Session session = SessionDAO.getInstance().get(sessionId, hibSession);
			if (session == null) return false;
			
			String year = ApplicationProperty.OnlineSchedulingAcademicYear.value();
			if (year != null && !session.getAcademicYear().matches(year)) return false;

			String term = ApplicationProperty.OnlineSchedulingAcademicTerm.value();
			if (term != null && !session.getAcademicTerm().matches(term)) return false;

			String campus = ApplicationProperty.OnlineSchedulingAcademicCampus.value();
			if (campus != null && !session.getAcademicInitiative().matches(campus)) return false;

			return true;
		} finally {
			hibSession.close();
		}
	}
	
	public OnlineSectioningServer createInstance(final Long academicSessionId, DataProperties config) {
		unload(academicSessionId, true);
		iGlobalLock.writeLock().lock();
		try {
			ApplicationProperties.setSessionId(academicSessionId);
			String serverClassName = ApplicationProperty.OnlineSchedulingServerClass.value();
			if (serverClassName == null)
				if (ApplicationProperty.OnlineSchedulingServerReplicated.isTrue())
					serverClassName = ReplicatedServerWithMaster.class.getName();
				else
					serverClassName = InMemoryServer.class.getName();
			Class serverClass = Class.forName(serverClassName);
			OnlineSectioningServer server = (OnlineSectioningServer)serverClass.getConstructor(OnlineSectioningServerContext.class).newInstance(getServerContext(academicSessionId));
			iInstances.put(academicSessionId, server);
			org.hibernate.Session hibSession = SessionDAO.getInstance().createNewSession();
			try {
				OnlineStudentSchedulingUpdater updater = new OnlineStudentSchedulingUpdater(this, server.getAcademicSession(), StudentSectioningQueue.getLastTimeStamp(hibSession, academicSessionId));
				iUpdaters.put(academicSessionId, updater);
				updater.start();
			} finally {
				hibSession.close();
			}
			return server;
		} catch (SectioningException e) {
			throw e;
		} catch (Exception e) {
			throw new SectioningException(e.getMessage(), e);
		} finally {
			iGlobalLock.writeLock().unlock();
			ApplicationProperties.setSessionId(null);
		}
	}
	
	public OnlineSectioningServerContext getServerContext(final Long academicSessionId) {
		return new OnlineSectioningServerContext() {
			@Override
			public Long getAcademicSessionId() {
				return academicSessionId;
			}

			@Override
			public boolean isWaitTillStarted() {
				return false;
			}

			@Override
			public EmbeddedCacheManager getCacheManager() {
				return null;
			}

			@Override
			public LockService getLockService() {
				return null;
			}
		};
	}
	
	@Override
	public void unloadSolver(String sessionId) {
		unload(Long.valueOf(sessionId), true);
	}
	
	public void unload(Long academicSessionId, boolean interrupt) {
		iGlobalLock.writeLock().lock();
		try {
			OnlineStudentSchedulingUpdater u = iUpdaters.get(academicSessionId);
			if (u != null)
				u.stopUpdating(interrupt);
			OnlineSectioningServer s = iInstances.get(academicSessionId);
			if (s != null) {
				sLog.info("Unloading " + u.getAcademicSession() + "...");
				s.unload();
			}
			iInstances.remove(academicSessionId);
			iUpdaters.remove(academicSessionId);
		} finally {
			iGlobalLock.writeLock().unlock();
		}
	}

	@Override
	public int getUsage() {
		iGlobalLock.readLock().lock();
		int ret = 0;
		try {
			for (OnlineSectioningServer s: iInstances.values())
				if (s.isMaster())
					ret += 200;
				else
					ret += 100;
		} finally {
			iGlobalLock.readLock().unlock();
		}
		return ret;
	}

	@Override
	public void start() {
		sLog.info("Student Sectioning Service is starting up ...");
		OnlineSectioningLogger.startLogger();
	}
	
	public boolean isEnabled() {
		// quick check for existing instances
		if (!iInstances.isEmpty()) return true;
		
		// otherwise, look for a session that has sectioning enabled
		String year = ApplicationProperty.OnlineSchedulingAcademicYear.value();
		String term = ApplicationProperty.OnlineSchedulingAcademicTerm.value();
		String campus = ApplicationProperty.OnlineSchedulingAcademicCampus.value();
		for (Iterator<Session> i = SessionDAO.getInstance().findAll().iterator(); i.hasNext(); ) {
			final Session session = i.next();
			
			if (year != null && !session.getAcademicYear().matches(year)) continue;
			if (term != null && !session.getAcademicTerm().matches(term)) continue;
			if (campus != null && !session.getAcademicInitiative().matches(campus)) continue;
			if (session.getStatusType().isTestSession()) continue;

			if (!session.getStatusType().canSectionAssistStudents() && !session.getStatusType().canOnlineSectionStudents()) continue;

			return true;
		}
		return false;
	}
	
	public boolean isRegistrationEnabled() {
		for (Session session: SessionDAO.getInstance().findAll()) {
			if (session.getStatusType().isTestSession()) continue;
			if (!session.getStatusType().canOnlineSectionStudents() && !session.getStatusType().canSectionAssistStudents() && session.getStatusType().canPreRegisterStudents()) return true;
		}
		return false;
	}
	
	public void unloadAll() {
		iGlobalLock.writeLock().lock();
		try {
			for (OnlineStudentSchedulingUpdater u: iUpdaters.values()) {
				u.stopUpdating(true);
				if (u.getAcademicSession() != null) {
					OnlineSectioningServer s = iInstances.get(u.getAcademicSession().getUniqueId());
					if (s != null) s.unload();
				}
			}
			iInstances.clear();
			iUpdaters.clear();
		} finally {
			iGlobalLock.writeLock().unlock();
		}		
	}

	@Override
	public void stop() {
		sLog.info("Student Sectioning Service is going down ...");
		unloadAll();
		OnlineSectioningLogger.stopLogger();
	}

}
