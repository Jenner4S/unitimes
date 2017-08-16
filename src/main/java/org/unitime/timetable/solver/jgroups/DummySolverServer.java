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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Message.Flag;
import org.jgroups.MessageListener;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.mux.MuxRpcDispatcher;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;
import org.unitime.timetable.ApplicationProperties;
import org.unitime.timetable.interfaces.RoomAvailabilityInterface;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.server.CheckMaster;
import org.unitime.timetable.onlinesectioning.server.CheckMaster.Master;
import org.unitime.timetable.solver.SolverProxy;
import org.unitime.timetable.solver.exam.ExamSolverProxy;
import org.unitime.timetable.solver.instructor.InstructorSchedulingProxy;
import org.unitime.timetable.solver.studentsct.StudentSolverProxy;

/**
 * @author Tomas Muller
 */
public class DummySolverServer extends AbstractSolverServer implements MessageListener {
	private static Log sLog = LogFactory.getLog(DummySolverServer.class);
	public static final RequestOptions sFirstResponse = new RequestOptions(ResponseMode.GET_FIRST, 0).setFlags(Flag.DONT_BUNDLE, Flag.OOB);
	public static final RequestOptions sAllResponses = new RequestOptions(ResponseMode.GET_ALL, 0).setFlags(Flag.DONT_BUNDLE, Flag.OOB);
	
	private JChannel iChannel = null;
	private RpcDispatcher iDispatcher;
	private RpcDispatcher iRoomAvailabilityDispatcher;
	protected Properties iProperties = null;
	
	private DummyContainer<SolverProxy> iCourseSolverContainer;
	private DummyContainer<ExamSolverProxy> iExamSolverContainer;
	private DummyContainer<StudentSolverProxy> iStudentSolverContainer;
	private DummyContainer<InstructorSchedulingProxy> iInstructorSchedulingContainer;
	private DummyContainer<OnlineSectioningServer> iOnlineStudentSchedulingContainer;

	private SolverContainerWrapper<SolverProxy> iCourseSolverContainerWrapper;
	private SolverContainerWrapper<ExamSolverProxy> iExamSolverContainerWrapper;
	private SolverContainerWrapper<StudentSolverProxy> iStudentSolverContainerWrapper;
	private SolverContainerWrapper<OnlineSectioningServer> iOnlineStudentSchedulingContainerWrapper;

	public DummySolverServer(JChannel channel) {
		iChannel = channel;
		iDispatcher = new MuxRpcDispatcher(SCOPE_SERVER, channel, null, null, this);
		iCourseSolverContainer = new DummyContainer<SolverProxy>(channel, SCOPE_COURSE, SolverProxy.class);
		iExamSolverContainer = new DummyContainer<ExamSolverProxy>(channel, SCOPE_EXAM, ExamSolverProxy.class);
		iStudentSolverContainer = new DummyContainer<StudentSolverProxy>(channel, SCOPE_STUDENT, StudentSolverProxy.class);
		iInstructorSchedulingContainer = new DummyContainer<InstructorSchedulingProxy>(channel, SCOPE_INSTRUCTOR, InstructorSchedulingProxy.class);
		iOnlineStudentSchedulingContainer = new ReplicatedDummyContainer<OnlineSectioningServer>(channel, SCOPE_ONLINE, OnlineSectioningServer.class);
		iRoomAvailabilityDispatcher = new MuxRpcDispatcher(SCOPE_AVAILABILITY, channel, null, null, this);
		
		iCourseSolverContainerWrapper = new SolverContainerWrapper<SolverProxy>(iDispatcher, iCourseSolverContainer, false);
		iExamSolverContainerWrapper = new SolverContainerWrapper<ExamSolverProxy>(iDispatcher, iExamSolverContainer, false);
		iStudentSolverContainerWrapper = new SolverContainerWrapper<StudentSolverProxy>(iDispatcher, iStudentSolverContainer, false);
		iOnlineStudentSchedulingContainerWrapper = new SolverContainerWrapper<OnlineSectioningServer>(iDispatcher, iOnlineStudentSchedulingContainer, false);
	}
	
	public Properties getProperties() {
		if (iProperties == null)
			iProperties = ApplicationProperties.getProperties();
		return iProperties;
	}
	
	@Override
	public boolean isLocal() {
		return false;
	}

	@Override
	public Address getAddress() {
		return iChannel.getAddress();
	}

	@Override
	public Address getLocalAddress() {
		try {
			RspList<Boolean> ret = iDispatcher.callRemoteMethods(null, "isLocal", new Object[] {}, new Class[] {}, sAllResponses);
			for (Rsp<Boolean> local: ret) {
				if (Boolean.TRUE.equals(local.getValue()))
					return local.getSender();
			}
			return null;
		} catch (Exception e) {
			sLog.error("Failed to retrieve local address: " + e.getMessage(), e);
			return null;
		}
	}

	@Override
	public String getHost() {
		return iChannel.getAddressAsString();
	}

	@Override
	public int getUsage() {
		return 0;
	}

	@Override
	public long getAvailableMemory() {
		return 0;
	}

	@Override
	public long getMemoryLimit() {
		return 0;
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public SolverContainer<SolverProxy> getCourseSolverContainer() {
		return iCourseSolverContainerWrapper;
	}
	
	@Override
	public SolverContainer<ExamSolverProxy> getExamSolverContainer() {
		return iExamSolverContainerWrapper;
	}
	
	@Override
	public SolverContainer<StudentSolverProxy> getStudentSolverContainer() {
		return iStudentSolverContainerWrapper;
	}
	
	@Override
	public SolverContainer<InstructorSchedulingProxy> getInstructorSchedulingContainer() {
		return iInstructorSchedulingContainer;
	}
	
	@Override
	public SolverContainer<OnlineSectioningServer> getOnlineStudentSchedulingContainer() {
		return iOnlineStudentSchedulingContainerWrapper;
	}

	@Override
	public RoomAvailabilityInterface getRoomAvailability() {
		Address local = getLocalAddress();
		if (local == null) return null;
		
		return (RoomAvailabilityInterface)Proxy.newProxyInstance(
				SolverServerImplementation.class.getClassLoader(),
				new Class[] {RoomAvailabilityInterface.class},
				new RoomAvailabilityInvocationHandler(local));
	}
	
	public class RoomAvailabilityInvocationHandler implements InvocationHandler {
		private Address iAddress;
		
		private RoomAvailabilityInvocationHandler(Address address) {
			iAddress = address;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				return iRoomAvailabilityDispatcher.callRemoteMethod(iAddress, "invoke",  new Object[] { method.getName(), method.getParameterTypes(), args }, new Class[] { String.class, Class[].class, Object[].class }, SolverServerImplementation.sFirstResponse);
			} catch (Exception e) {
				sLog.error("Excution of room availability method " + method + " failed: " + e.getMessage(), e);
				return null;
			}
		}
    }

	public class DummyContainer<T> implements RemoteSolverContainer<T> {
		protected RpcDispatcher iDispatcher;
		protected Class<T> iClazz;
		
		public DummyContainer(JChannel channel, short scope, Class<T> clazz) {
			iDispatcher = new MuxRpcDispatcher(scope, channel, null, null, this);
			iClazz = clazz;
		}
		
		@Override
		public Set<String> getSolvers() {
			return new HashSet<String>();
		}

		@Override
		public T getSolver(String user) {
			return null;
		}

		@Override
		public long getMemUsage(String user) {
			return 0;
		}
		
		@Override
		public boolean hasSolver(String user) {
			return false;
		}

		@Override
		public T createSolver(String user, DataProperties config) {
			return null;
		}

		@Override
		public void unloadSolver(String user) {
		}

		@Override
		public int getUsage() {
			return 0;
		}

		@Override
		public void start() {
		}

		@Override
		public void stop() {
		}

		@Override
		public boolean createRemoteSolver(String user, DataProperties config, Address caller) {
			return false;
		}

		@Override
		public RpcDispatcher getDispatcher() {
			return iDispatcher;
		}

		@Override
		public Object dispatch(Address address, String user, Method method, Object[] args) throws Exception {
			try {
				return iDispatcher.callRemoteMethod(address, "invoke",  new Object[] { method.getName(), user, method.getParameterTypes(), args }, new Class[] { String.class, String.class, Class[].class, Object[].class }, SolverServerImplementation.sFirstResponse);
			} catch (Exception e) {
				sLog.debug("Excution of " + method.getName() + " on solver " + user + " failed: " + e.getMessage(), e);
				throw e;
			}
		}

		@Override
		public Object invoke(String method, String user, Class[] types, Object[] args) throws Exception {
			throw new Exception("Method " + method + " not implemented.");
		}

		@Override
		public T createProxy(Address address, String user) {
			SolverInvocationHandler handler = new SolverInvocationHandler(address, user);
			return (T)Proxy.newProxyInstance(
					iClazz.getClassLoader(),
					new Class[] {iClazz, RemoteSolver.class, },
					handler);
		}
		
		public class SolverInvocationHandler implements InvocationHandler {
	    	private Address iAddress;
	    	private String iUser;
	    	
	    	private SolverInvocationHandler(Address address, String user) {
	    		iAddress = address;
	    		iUser = user;
	    	}
	    	
	    	public String getHost() {
	    		return iAddress.toString();
	    	}
	    	
	    	public String getUser() {
	    		return iUser;
	    	}
	    	
	    	@Override
	    	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    		try {
	    			return getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(this, args);
	    		} catch (NoSuchMethodException e) {}
	    		return dispatch(iAddress, iUser, method, args);
	        }
	    }
	}
	
	public class ReplicatedDummyContainer<T> extends DummyContainer<T> implements ReplicatedSolverContainer<T> {
		
		public ReplicatedDummyContainer(JChannel channel, short scope, Class<T> clazz) {
			super(channel, scope, clazz);
		}

		@Override
		public Object dispatch(Collection<Address> addresses, String sessionId, Method method, Object[] args) throws Exception {
			try {
				if (addresses.size() == 1) {
					return dispatch(ToolBox.random(addresses), sessionId, method, args);
				} else {
					Address address = ToolBox.random(addresses);
					CheckMaster ch = method.getAnnotation(CheckMaster.class);
					if (ch == null && "execute".equals(method.getName()))
						ch = args[0].getClass().getAnnotation(CheckMaster.class);
					RspList<Boolean> ret = iDispatcher.callRemoteMethods(addresses, "hasMaster", new Object[] { sessionId }, new Class[] { String.class }, SolverServerImplementation.sAllResponses);
					if (ch != null && ch.value() == Master.REQUIRED) {
						for (Rsp<Boolean> rsp : ret) {
							if (rsp != null && rsp.getValue()) {
								address = rsp.getSender();
								break;
							}
						}
					} else {
						List<Address> slaves = new ArrayList<Address>();
						for (Rsp<Boolean> rsp : ret) {
							if (rsp != null && !rsp.getValue()) {
								slaves.add(rsp.getSender());
							}
						}
						if (!slaves.isEmpty())
							address = ToolBox.random(slaves);
					}
					return dispatch(address, sessionId, method, args);
				}
			} catch (InvocationTargetException e) {
				if (e.getTargetException() != null && e.getTargetException() instanceof Exception)
					throw (Exception)e.getTargetException();
				else
					throw e;
			}
		}

		@Override
		public T createProxy(Collection<Address> addresses, String user) {
			ReplicatedServerInvocationHandler handler = new ReplicatedServerInvocationHandler(addresses, user);
			T px = (T)Proxy.newProxyInstance(
					iClazz.getClassLoader(),
					new Class[] {iClazz, RemoteSolver.class, },
					handler);
	    	return px;
		}

		@Override
		public boolean hasMaster(String user) {
			return false;
		}
		
		public class ReplicatedServerInvocationHandler implements InvocationHandler {
	    	private Collection<Address> iAddresses;
	    	private String iUser;
	    	
	    	private ReplicatedServerInvocationHandler(Collection<Address> addresses, String user) {
	    		iAddresses = addresses;
	    		iUser = user;
	    	}
	    	
	    	public String getHost() {
	    		return iAddresses.toString();
	    	}
	    	
	    	public String getUser() {
	    		return iUser;
	    	}
	    	
	    	@Override
	    	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
	    		try {
	    			return getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(this, args);
	    		} catch (NoSuchMethodException e) {}
	    		return dispatch(iAddresses, iUser, method, args);
	        }
	    }
	}
	
	@Override
	public void receive(Message msg) {
		sLog.info("receive(" + msg + ", " + msg.getObject() + ")");
	}


	@Override
	public void getState(OutputStream output) throws Exception {
		getProperties().store(output, "UniTime Application Properties");
	}


	@Override
	public void setState(InputStream input) throws Exception {
		if (iProperties == null) {
			iProperties = new Properties();
		} else {
			iProperties.clear();
		}
		iProperties.load(input);
	}

	@Override
	public boolean isCoordinator() {
		return false;
	}

	@Override
	public List<SolverServer> getServers(boolean onlyAvailable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public SolverServer crateServerProxy(Address address) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void shutdown() {
		throw new UnsupportedOperationException();
	}
}