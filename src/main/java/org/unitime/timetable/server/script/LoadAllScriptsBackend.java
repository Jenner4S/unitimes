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

import java.util.Collections;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.util.HtmlUtils;
import org.unitime.timetable.gwt.command.client.GwtRpcResponseList;
import org.unitime.timetable.gwt.command.server.GwtRpcImplementation;
import org.unitime.timetable.gwt.command.server.GwtRpcImplements;
import org.unitime.timetable.gwt.shared.ScriptInterface;
import org.unitime.timetable.gwt.shared.ScriptInterface.LoadAllScriptsRpcRequest;
import org.unitime.timetable.gwt.shared.ScriptInterface.ScriptParameterInterface;
import org.unitime.timetable.model.Building;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.RefTableEntry;
import org.unitime.timetable.model.Room;
import org.unitime.timetable.model.Script;
import org.unitime.timetable.model.ScriptParameter;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.dao.RefTableEntryDAO;
import org.unitime.timetable.model.dao.ScriptDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
@GwtRpcImplements(LoadAllScriptsRpcRequest.class)
public class LoadAllScriptsBackend implements GwtRpcImplementation<LoadAllScriptsRpcRequest, GwtRpcResponseList<ScriptInterface>> {

	@Override
	@PreAuthorize("checkPermission('Scripts')")
	public GwtRpcResponseList<ScriptInterface> execute(LoadAllScriptsRpcRequest request, SessionContext context) {
		GwtRpcResponseList<ScriptInterface> list = new GwtRpcResponseList<ScriptInterface>();
		
		for (Script s: ScriptDAO.getInstance().findAll()) {
			ScriptInterface script = load(s, context);
			if (script != null)
				list.add(script);
		}
		
		Collections.sort(list);
		
		return list;
	}
	
	public static ScriptInterface load(Script s, SessionContext context) {
		ScriptInterface script = new ScriptInterface();
		script.setId(s.getUniqueId());
		script.setName(s.getName());
		script.setEngine(s.getEngine());
		script.setDescription(s.getDescription());
		script.setPermission(s.getPermission());
		script.setScript(s.getScript());
		script.setCanDelete(context.hasPermission(Right.ScriptEdit));
		script.setCanEdit(context.hasPermission(Right.ScriptEdit));
		Right right = null;
		if (s.getPermission() == null) {
			script.setCanExecute(true);
		} else {
			try {
				right = Right.valueOf(s.getPermission().replace(" ", ""));
				script.setCanExecute(context.hasPermission(right));
			} catch (IllegalArgumentException e) {
				script.setCanExecute(false);
			}
		}
		for (ScriptParameter p: s.getParameters()) {
			ScriptParameterInterface parameter = new ScriptParameterInterface();
			parameter.setLabel(p.getLabel() == null ? p.getName() : p.getLabel());
			parameter.setName(p.getName());
			parameter.setDefaultValue(p.getDefaultValue());
			parameter.setType(p.getType());
			if (p.getType().startsWith("enum(") && p.getType().endsWith(")")) {
				for (String option: p.getType().substring("enum(".length(), p.getType().length() - 1).split(","))
					parameter.addOption(option, option);
			} else if (p.getType().startsWith("reference(") && p.getType().endsWith(")")) {
				try {
					String clazz = p.getType().substring("reference(".length(), p.getType().length() - 1);
					for (RefTableEntry entry: (List<RefTableEntry>)RefTableEntryDAO.getInstance().getSession().createQuery("from " + clazz).setCacheable(true).list()) {
						parameter.addOption(entry.getReference(), entry.getLabel());
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (p.getType().equalsIgnoreCase("department") || p.getType().equalsIgnoreCase("departments")) {
				if (p.getType().equalsIgnoreCase("departments")) parameter.setMultiSelect(true);
				for (Department department: Department.getUserDepartments(context.getUser())) {
					if (right != null && Department.class.equals(right.type()) && !context.hasPermission(department, right)) continue;
					parameter.addOption(department.getUniqueId().toString(), department.getDeptCode() + " - " + department.getName());
				}
			} else if (p.getType().equalsIgnoreCase("subject") || p.getType().equalsIgnoreCase("subjects")) {
				if (p.getType().equalsIgnoreCase("subjects")) parameter.setMultiSelect(true);
				for (SubjectArea subject: SubjectArea.getUserSubjectAreas(context.getUser())) {
					if (right != null && SubjectArea.class.equals(right.type()) && !context.hasPermission(subject, right)) continue;
					parameter.addOption(subject.getUniqueId().toString(), subject.getSubjectAreaAbbreviation() + " - " + HtmlUtils.htmlUnescape(subject.getTitle()));
				}
			} else if (p.getType().equalsIgnoreCase("building") || p.getType().equalsIgnoreCase("buildings")) {
				if (p.getType().equalsIgnoreCase("buildings")) parameter.setMultiSelect(true);
				for (Building building: Building.findAll(context.getUser().getCurrentAcademicSessionId())) {
					if (right != null && Building.class.equals(right.type()) && !context.hasPermission(building, right)) continue;
					parameter.addOption(building.getUniqueId().toString(), building.getAbbreviation() + " - " + HtmlUtils.htmlUnescape(building.getName()));
				}
			} else if (p.getType().equalsIgnoreCase("room") || p.getType().equalsIgnoreCase("rooms")) {
				if (p.getType().equalsIgnoreCase("rooms")) parameter.setMultiSelect(true);
				for (Room room: Room.findAllRooms(context.getUser().getCurrentAcademicSessionId())) {
					if (right != null && Room.class.equals(right.type()) && !context.hasPermission(room, right)) continue;
					parameter.addOption(room.getUniqueId().toString(), room.getLabel());
				}
			} else if (p.getType().equalsIgnoreCase("location") || p.getType().equalsIgnoreCase("locations")) {
				if (p.getType().equalsIgnoreCase("locations")) parameter.setMultiSelect(true);
				for (Location location: Location.findAll(context.getUser().getCurrentAcademicSessionId())) {
					if (right != null && Location.class.equals(right.type()) && !context.hasPermission(location, right)) continue;
					parameter.addOption(location.getUniqueId().toString(), location.getLabel());
				}
			}
			script.addParameter(parameter);
		}
		if (script.hasParameters())
			Collections.sort(script.getParameters());
		return script;
	}
}
