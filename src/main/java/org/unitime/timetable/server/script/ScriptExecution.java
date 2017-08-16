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
package org.unitime.timetable.server.script;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.fileupload.FileItem;
import org.hibernate.Transaction;
import org.unitime.commons.Debug;
import org.unitime.timetable.gwt.server.UploadServlet;
import org.unitime.timetable.gwt.shared.ScriptInterface.ExecuteScriptRpcRequest;
import org.unitime.timetable.model.Building;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Room;
import org.unitime.timetable.model.Script;
import org.unitime.timetable.model.ScriptParameter;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.dao.BuildingDAO;
import org.unitime.timetable.model.dao.DepartmentDAO;
import org.unitime.timetable.model.dao.LocationDAO;
import org.unitime.timetable.model.dao.RoomDAO;
import org.unitime.timetable.model.dao.ScriptDAO;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.model.dao.SubjectAreaDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.util.Formats;
import org.unitime.timetable.util.queue.QueueItem;

/**
 * @author Tomas Muller
 */
public class ScriptExecution extends QueueItem {
	private ExecuteScriptRpcRequest iRequest;
	private double iProgress = 0;
	private double iMaxProgress = 100.0;
	private FileItem iFile = null;
	
	public ScriptExecution(ExecuteScriptRpcRequest request, SessionContext context) {
		super(context.getUser());
		iRequest = request;
		
		Script script = ScriptDAO.getInstance().get(request.getScriptId());
		if (script.getPermission() != null)
			context.checkPermission(Right.valueOf(script.getPermission().replace(" ", "")));
		
		for (ScriptParameter parameter: script.getParameters())
			if ("file".equals(parameter.getType()))
				iFile = (FileItem)context.getAttribute(UploadServlet.SESSION_LAST_FILE);
	}
	
	public ExecuteScriptRpcRequest getRequest() { return iRequest; }

	@Override
	public String type() { return "Script"; }

	@Override
	public String name() { return iRequest.getScriptName(); }

	@Override
	public double progress() { return iProgress / iMaxProgress; }
	
	public void setStatus(String status, double maxProgress) {
		iProgress = 0; iMaxProgress = maxProgress;
		super.setStatus(status);
	}
	
	public void incProgress() { iProgress ++; }
	
	public void incProgress(double value) { iProgress += value; }
	
	public void setProgress(double value) { iProgress = value; }
	
	public void debug(String message) { log("&nbsp;&nbsp;<i><font color='gray'> " + message + "</font></i>"); }
	public void info(String message) { log("&nbsp;&nbsp;" + message); }
	public void warn(String message) { super.warn(message); }
	public void error(String message) { super.error(message); }
	public void error(String message, Throwable t) { super.error(message); setError(t); }
	
	public File createOutput(String prefix, String ext) {
		return super.createOutput(prefix, ext);
	}

	@Override
	protected void execute() throws Exception {
		org.hibernate.Session hibSession = ScriptDAO.getInstance().getSession();
		
		Transaction tx = hibSession.beginTransaction();
		try {
			setStatus("Starting up...", 3);

			Script script = ScriptDAO.getInstance().get(iRequest.getScriptId(), hibSession);
			
			ScriptEngineManager manager = new ScriptEngineManager();
			ScriptEngine engine = manager.getEngineByName(script.getEngine());
			engine.put("hibSession", hibSession);
			engine.put("session", SessionDAO.getInstance().get(getSessionId()));
			engine.put("log", this);
			
			incProgress();
			
			engine.getContext().setWriter(new Writer() {
				@Override
				public void write(char[] cbuf, int off, int len) throws IOException {
					String line = String.valueOf(cbuf, off, len);
					if (line.endsWith("\n")) line = line.substring(0, line.length() - 1);
					if (!line.isEmpty())
						info(line);
				}
				@Override
				public void flush() throws IOException {}
				@Override
				public void close() throws IOException {}
			});
			engine.getContext().setErrorWriter(new Writer() {
				@Override
				public void write(char[] cbuf, int off, int len) throws IOException {
					String line = String.valueOf(cbuf, off, len);
					if (line.endsWith("\n")) line = line.substring(0, line.length() - 1);
					if (!line.isEmpty())
						warn(line);
				}
				@Override
				public void flush() throws IOException {}
				@Override
				public void close() throws IOException {}
			});
			
			incProgress();
			
			debug("Engine: " + engine.getFactory().getEngineName() + " (ver. " + engine.getFactory().getEngineVersion() + ")");
			debug("Language: " + engine.getFactory().getLanguageName() + " (ver. " + engine.getFactory().getLanguageVersion() + ")");
			
			for (ScriptParameter parameter: script.getParameters()) {
				String value = iRequest.getParameters().get(parameter.getName());
				
				if ("file".equals(parameter.getType()) && iFile != null) {
					debug(parameter.getName() + ": " + iFile.getName() + " (" + iFile.getSize() + " bytes)");
					engine.put(parameter.getName(), iFile);
					continue;
				}
				
				if (value == null) value = parameter.getDefaultValue();
				if (value == null) {
					engine.put(parameter.getName(), null);
					continue;
				}
				debug(parameter.getName() + ": " + value);
				
				if (parameter.getType().equalsIgnoreCase("boolean")) {
					engine.put(parameter.getName(), "true".equalsIgnoreCase(value));
				} else if (parameter.getType().equalsIgnoreCase("long")) {
					engine.put(parameter.getName(), value == null || value.isEmpty() ? null : Long.valueOf(value));
				} else if (parameter.getType().equalsIgnoreCase("int") || parameter.getType().equalsIgnoreCase("integer")) {
					engine.put(parameter.getName(), value == null || value.isEmpty() ? null : Integer.valueOf(value));
				} else if (parameter.getType().equalsIgnoreCase("double")) {
					engine.put(parameter.getName(), value == null || value.isEmpty() ? null : Double.valueOf(value));
				} else if (parameter.getType().equalsIgnoreCase("float")) {
					engine.put(parameter.getName(), value == null || value.isEmpty() ? null : Float.valueOf(value));
				} else if (parameter.getType().equalsIgnoreCase("short")) {
					engine.put(parameter.getName(), value == null || value.isEmpty() ? null : Short.valueOf(value));
				} else if (parameter.getType().equalsIgnoreCase("byte")) {
					engine.put(parameter.getName(), value == null || value.isEmpty() ? null : Byte.valueOf(value));
				} else if (parameter.getType().equalsIgnoreCase("date")) {
					Formats.Format<Date> dateFormat = Formats.getDateFormat(Formats.Pattern.DATE_EVENT);
					engine.put(parameter.getName(), dateFormat.parse(value));
				} else if (parameter.getType().equalsIgnoreCase("department")) {
					engine.put(parameter.getName(), DepartmentDAO.getInstance().get(Long.valueOf(value), hibSession));
				} else if (parameter.getType().equalsIgnoreCase("departments")) {
					List<Department> departments = new ArrayList<Department>();
					for (String id: value.split(","))
						if (!id.isEmpty())
							departments.add(DepartmentDAO.getInstance().get(Long.valueOf(id), hibSession));
					engine.put(parameter.getName(), departments);
				} else if (parameter.getType().equalsIgnoreCase("subject")) {
					engine.put(parameter.getName(), SubjectAreaDAO.getInstance().get(Long.valueOf(value), hibSession));
				} else if (parameter.getType().equalsIgnoreCase("subjects")) {
					List<SubjectArea> subjects = new ArrayList<SubjectArea>();
					for (String id: value.split(","))
						if (!id.isEmpty())
							subjects.add(SubjectAreaDAO.getInstance().get(Long.valueOf(id), hibSession));
					engine.put(parameter.getName(), subjects);
				} else if (parameter.getType().equalsIgnoreCase("building")) {
					engine.put(parameter.getName(), BuildingDAO.getInstance().get(Long.valueOf(value), hibSession));
				} else if (parameter.getType().equalsIgnoreCase("buildings")) {
					List<Building> buildings = new ArrayList<Building>();
					for (String id: value.split(","))
						if (!id.isEmpty())
							buildings.add(BuildingDAO.getInstance().get(Long.valueOf(id), hibSession));
					engine.put(parameter.getName(), buildings);
				} else if (parameter.getType().equalsIgnoreCase("room")) {
					engine.put(parameter.getName(), RoomDAO.getInstance().get(Long.valueOf(value), hibSession));
				} else if (parameter.getType().equalsIgnoreCase("rooms")) {
					List<Room> rooms = new ArrayList<Room>();
					for (String id: value.split(","))
						if (!id.isEmpty())
							rooms.add(RoomDAO.getInstance().get(Long.valueOf(id), hibSession));
					engine.put(parameter.getName(), rooms);
				} else if (parameter.getType().equalsIgnoreCase("location")) {
					engine.put(parameter.getName(), LocationDAO.getInstance().get(Long.valueOf(value), hibSession));
				} else if (parameter.getType().equalsIgnoreCase("locations")) {
					List<Location> locations = new ArrayList<Location>();
					for (String id: value.split(","))
						if (!id.isEmpty())
							locations.add(LocationDAO.getInstance().get(Long.valueOf(id), hibSession));
					engine.put(parameter.getName(), locations);
				} else {
					engine.put(parameter.getName(), value);
				}
			}
			
			incProgress();
			
			if (engine instanceof Compilable) {
				setStatus("Compiling script...", 1);
				CompiledScript compiled = ((Compilable)engine).compile(script.getScript());
				incProgress();
				setStatus("Running script...", 100);
				compiled.eval();
			} else {
				setStatus("Running script...", 100);
				engine.eval(script.getScript());
			}
			
			hibSession.flush();
			tx.commit();
			
			setStatus("All done.", 1);
			incProgress();
		} catch (Exception e) {
			tx.rollback();
			error("Execution failed: " + e.getMessage(), e);
			Debug.error("Execution failed: " + e.getMessage(), e);
		} finally {
			hibSession.close();
		}
	}
}