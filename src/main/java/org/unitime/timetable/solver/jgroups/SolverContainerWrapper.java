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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.ToolBox;
import org.jgroups.Address;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.Rsp;
import org.jgroups.util.RspList;


/**
 * @author Tomas Muller
 */
public class SolverContainerWrapper<T> implements SolverContainer<T> {
	private static Log sLog = LogFactory.getLog(SolverContainerWrapper.class);
	private RpcDispatcher iDispatcher;
	private RemoteSolverContainer<T> iContainer;
	private boolean iCheckLocal = true;

	public SolverContainerWrapper(RpcDispatcher dispatcher, RemoteSolverContainer<T> container, boolean checkLocal) {
		iDispatcher = dispatcher;
		iContainer = container;
		iCheckLocal = checkLocal;
	}

	@Override
	public Set<String> getSolvers() {
		Set<String> solvers = new HashSet<String>(iContainer.getSolvers());
		try {
			RspList<Set<String>> ret = iContainer.getDispatcher().callRemoteMethods(null, "getSolvers", new Object[] {}, new Class[] {}, SolverServerImplementation.sAllResponses);
			for (Rsp<Set<String>> rsp : ret) {
				if (rsp != null && rsp.getValue() != null)
					solvers.addAll(rsp.getValue());
			}
		} catch (Exception e) {
			sLog.error("Failed to retrieve solvers: " + e.getMessage(), e);
		}
		return solvers;
	}

	@Override
	public T getSolver(String user) {
		try {
			if (iCheckLocal) {
				T solver = iContainer.getSolver(user);
				if (solver != null) return solver;				
			}

			RspList<Boolean> ret = iContainer.getDispatcher().callRemoteMethods(null, "hasSolver", new Object[] { user }, new Class[] { String.class }, SolverServerImplementation.sAllResponses);
			List<Address> senders = new ArrayList<Address>();
			for (Rsp<Boolean> rsp : ret) {
				if (rsp != null && rsp.getValue() != null && rsp.getValue())
					senders.add(rsp.getSender());
			}
			if (senders.isEmpty())
				return null;
			else if (senders.size() == 1)
				return iContainer.createProxy(senders.get(0), user);
			else if (iContainer instanceof ReplicatedSolverContainer)
				return ((ReplicatedSolverContainer<T>)iContainer).createProxy(senders, user);
			else
				return iContainer.createProxy(ToolBox.random(senders), user);
		} catch (Exception e) {
			sLog.error("Failed to retrieve solver " + user + ": " + e.getMessage(), e);
		}
		return null;
	}
	
	@Override
	public long getMemUsage(String user) {
		try {
			if (iCheckLocal && iContainer.hasSolver(user))
				return iContainer.getMemUsage(user);

			RspList<Long> ret = iContainer.getDispatcher().callRemoteMethods(null, "getMemUsage", new Object[] { user }, new Class[] { String.class }, SolverServerImplementation.sAllResponses);
			long total = 0, count = 0;
			for (Rsp<Long> rsp : ret) {
				if (rsp != null && rsp.getValue() != null && rsp.getValue() > 0) {
					total += rsp.getValue();
					count ++;
				}
			}
			
			return count == 0 ? 0 : total / count;
		} catch (Exception e) {
			sLog.error("Failed to retrieve allocated memory " + user + ": " + e.getMessage(), e);
		}
		return 0;
	}

	@Override
	public boolean hasSolver(String user) {
		try {
			if (iContainer.hasSolver(user)) return true;

			RspList<Boolean> ret = iContainer.getDispatcher().callRemoteMethods(null, "hasSolver", new Object[] { user }, new Class[] { String.class }, SolverServerImplementation.sAllResponses);
			for (Rsp<Boolean> rsp : ret)
				if (rsp.getValue()) return true;
			return false;
		} catch (Exception e) {
			sLog.error("Failed to check solver " + user + ": " + e.getMessage(), e);
		}
		return false;
	}

	@Override
	public T createSolver(String user, DataProperties config) {
		try {
			Address bestAddress = null;
			int bestUsage = 0;
			RspList<Boolean> ret = iDispatcher.callRemoteMethods(null, "isAvailable", new Object[] {}, new Class[] {}, SolverServerImplementation.sAllResponses);
			for (Rsp<Boolean> rsp : ret) {
				if (Boolean.TRUE.equals(rsp.getValue())) {
					Integer usage = iDispatcher.callRemoteMethod(rsp.getSender(), "getUsage", new Object[] {}, new Class[] {}, SolverServerImplementation.sFirstResponse);
					if (bestAddress == null || bestUsage > usage) {
						bestAddress = rsp.getSender();
		                bestUsage = usage;
		            }
				}
			}
				
			if (bestAddress == null)
				throw new RuntimeException("Not enough resources to create a solver instance, please try again later.");
			
			if (bestAddress.equals(iDispatcher.getChannel().getAddress()))
				return iContainer.createSolver(user, config);
			
			iContainer.getDispatcher().callRemoteMethod(bestAddress, "createRemoteSolver", new Object[] { user, config, iDispatcher.getChannel().getAddress() }, new Class[] { String.class, DataProperties.class, Address.class }, SolverServerImplementation.sFirstResponse);
			return iContainer.createProxy(bestAddress, user);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException("Failed to start the solver: " + e.getMessage(), e);
		}
	}
	
	@Override
	public void unloadSolver(String user) {
		try {
			if (iContainer.hasSolver(user))
				iContainer.unloadSolver(user);
			
			RspList<Boolean> ret = iContainer.getDispatcher().callRemoteMethods(null, "hasSolver", new Object[] { user }, new Class[] { String.class }, SolverServerImplementation.sAllResponses);
			for (Rsp<Boolean> rsp : ret) {
				if (rsp.getValue())
					iContainer.getDispatcher().callRemoteMethod(rsp.getSender(), "unloadSolver", new Object[] { user }, new Class[] { String.class }, SolverServerImplementation.sFirstResponse);
			}
		} catch (Exception e) {
			sLog.error("Failed to unload solver " + user + ": " + e.getMessage(), e);
		}
	}

	@Override
	public int getUsage() {
		int usage = 0;
		try {
			RspList<Integer> ret = iContainer.getDispatcher().callRemoteMethods(null, "getUsage", new Object[] {}, new Class[] {}, SolverServerImplementation.sAllResponses);
			for (Rsp<Integer> rsp : ret)
				usage += rsp.getValue();
		} catch (Exception e) {
			sLog.error("Failed to check solver server usage: " + e.getMessage(), e);
		}
		return usage;
	}

	@Override
	public void start() {
		throw new RuntimeException("Method start is not supported on the solver container wrapper.");
	}

	@Override
	public void stop() {
		throw new RuntimeException("Method stop is not supported on the solver container wrapper.");
	}
	

}
