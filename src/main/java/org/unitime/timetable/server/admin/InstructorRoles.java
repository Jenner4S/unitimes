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
package org.unitime.timetable.server.admin;

import java.util.ArrayList;
import java.util.List;

import org.cpsolver.ifs.util.ToolBox;
import org.hibernate.Session;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.defaults.UserProperty;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.SimpleEditInterface;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Field;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.FieldType;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.ListItem;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.PageName;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Record;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.ChangeLog.Operation;
import org.unitime.timetable.model.ChangeLog.Source;
import org.unitime.timetable.model.dao.DepartmentDAO;
import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
import org.unitime.timetable.model.dao.RolesDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.util.NameFormat;

/**
 * @author Tomas Muller
 */
@Service("gwtAdminTable[type=instructorRole]")
public class InstructorRoles implements AdminTable {
	protected static final GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	@Override
	public PageName name() {
		return new PageName(MESSAGES.pageInstructorRole(), MESSAGES.pageInstructorRoles());
	}

	@Override
	@PreAuthorize("checkPermission('InstructorRoles')")
	public SimpleEditInterface load(SessionContext context, Session hibSession) {
		List<ListItem> departments = new ArrayList<ListItem>();
		List<ListItem> instructorRoles = new ArrayList<ListItem>();
		instructorRoles.add(new ListItem("", ""));
		for (Roles role: Roles.findAllInstructorRoles()) {
			instructorRoles.add(new ListItem(role.getUniqueId().toString(), role.getAbbv()));
		}
		SimpleEditInterface data = new SimpleEditInterface(
				new Field(MESSAGES.fieldDepartment(), FieldType.list, 160, departments),
				new Field(MESSAGES.fieldInstructor(), FieldType.person, 300),
				new Field(MESSAGES.fieldRole(), FieldType.list, 300, instructorRoles)
				);
		data.setSortBy(0, 1);
		
		boolean deptIndep = context.getUser().getCurrentAuthority().hasRight(Right.DepartmentIndependent);
		NameFormat nameFormat = NameFormat.fromReference(context.getUser().getProperty(UserProperty.NameFormat));

		for (Department department: Department.getUserDepartments(context.getUser())) {
			if (!department.isAllowEvents()) continue;
			departments.add(new ListItem(department.getUniqueId().toString(), department.getLabel()));
			for (DepartmentalInstructor instructor: (List<DepartmentalInstructor>)hibSession.createQuery(
					"from DepartmentalInstructor i where i.department.uniqueId = :departmentId and i.externalUniqueId is not null order by i.lastName, i.firstName")
					.setLong("departmentId", department.getUniqueId()).list()) {
				if (deptIndep && instructor.getRole() == null) continue;
				Record r = data.addRecord(instructor.getUniqueId(), false);
				r.setField(0, instructor.getDepartment().getUniqueId().toString(), false);
				r.setField(1, null, false);
				r.addToField(1, instructor.getLastName() == null ? "" : instructor.getLastName());
				r.addToField(1, instructor.getFirstName() == null ? "" : instructor.getFirstName());
				r.addToField(1, instructor.getMiddleName() == null ? "" : instructor.getMiddleName());
				r.addToField(1, instructor.getExternalUniqueId());
				r.addToField(1, instructor.getEmail() == null ? "" : instructor.getEmail());
				r.addToField(1, instructor.getAcademicTitle() == null ? "" : instructor.getAcademicTitle());
				r.addToField(1, nameFormat.format(instructor));
				r.setField(2, instructor.getRole() == null ? "" : instructor.getRole().getUniqueId().toString());
				r.setDeletable(deptIndep);
			}
		}
		data.setEditable(context.hasPermission(Right.InstructorRoleEdit));
		return data;
	}

	@Override
	@PreAuthorize("checkPermission('InstructorRoleEdit')")
	public void save(SimpleEditInterface data, SessionContext context, Session hibSession) {
		for (Department department: Department.getUserDepartments(context.getUser())) {
			if (!department.isAllowEvents()) continue;
			
			List<DepartmentalInstructor> instructors = (List<DepartmentalInstructor>)hibSession.createQuery(
					"from DepartmentalInstructor i where i.department.uniqueId = :departmentId and i.externalUniqueId is not null order by i.lastName, i.firstName")
					.setLong("departmentId", department.getUniqueId()).list();
			
			for (DepartmentalInstructor instructor: instructors) {
				Record r = data.getRecord(instructor.getUniqueId());
				if (r == null)
					delete(instructor, context, hibSession);
				else
					update(instructor, r, context, hibSession);
			}
		
			for (Record r: data.getNewRecords())
				if (department.getUniqueId().toString().equals(r.getField(0)))
					save(department, instructors, r, context, hibSession);
		}
	}
	
	protected void save(Department department, List<DepartmentalInstructor> instructors, Record record, SessionContext context, Session hibSession) {
		if (department == null) return;
		
		if (record.getField(1) == null || record.getField(1).isEmpty()) return;
		
		String[] name = record.getValues(1);

		DepartmentalInstructor instructor = null;
		boolean add = true;
		
		if (instructors == null) {
			instructor = DepartmentalInstructor.findByPuidDepartmentId(name[3], department.getUniqueId());
		} else {
			for (DepartmentalInstructor i: instructors)
				if (name[3].equals(i.getExternalUniqueId())) {
					instructor = i;
					add = false;
					break;
				}
		}
		
		if (instructor == null) {
			instructor = new DepartmentalInstructor();
			instructor.setExternalUniqueId(name[3]);
			instructor.setLastName(name[0]);
			instructor.setFirstName(name[1]);
			instructor.setMiddleName(name[2].isEmpty() ? null : name[2]);
			instructor.setEmail(name.length <=4 || name[4].isEmpty() ? null : name[4]);
			instructor.setAcademicTitle(name.length <= 5 || name[5].isEmpty() ? null : name[5]);
			instructor.setIgnoreToFar(false);
			instructor.setDepartment(department);

			instructor.setRole(record.getField(2) == null || record.getField(2).isEmpty() ? null : RolesDAO.getInstance().get(Long.valueOf(record.getField(2))));

			record.setUniqueId((Long)hibSession.save(instructor));
		} else {
			record.setUniqueId(instructor.getUniqueId());
			instructor.setRole(record.getField(2) == null || record.getField(2).isEmpty() ? null : RolesDAO.getInstance().get(Long.valueOf(record.getField(2))));

			hibSession.update(instructor);
		}
		
		record.setDeletable(false);
		record.setField(0, record.getField(0), false);
		record.setField(1, record.getField(1), false);
		
		ChangeLog.addChange(hibSession,
				context,
				instructor,
				instructor.getName(DepartmentalInstructor.sNameFormatLastInitial) + ": " + (instructor.getRole() == null ? MESSAGES.noRole() : instructor.getRole().getAbbv()),
				Source.SIMPLE_EDIT, 
				(add ? Operation.CREATE : Operation.UPDATE),
				null,
				instructor.getDepartment());		
	}

	@Override
	@PreAuthorize("checkPermission('InstructorRoleEdit')")
	public void save(Record record, SessionContext context, Session hibSession) {
		save(DepartmentDAO.getInstance().get(Long.valueOf(record.getField(0))), null, record, context, hibSession);
	}
	
	protected void update(DepartmentalInstructor instructor, Record record, SessionContext context, Session hibSession) {
		if (instructor == null) return;
		
		if (ToolBox.equals(instructor.getRole() == null ? "" : instructor.getRole().getUniqueId().toString(), record.getField(2))) return;
		
		instructor.setRole(record.getField(2) == null || record.getField(2).isEmpty() ? null : RolesDAO.getInstance().get(Long.valueOf(record.getField(2))));
		
		hibSession.update(instructor);
		
		ChangeLog.addChange(hibSession,
				context,
				instructor,
				instructor.getName(DepartmentalInstructor.sNameFormatLastInitial) + ": " + (instructor.getRole() == null ? MESSAGES.noRole() : instructor.getRole().getAbbv()),
				Source.SIMPLE_EDIT, 
				Operation.UPDATE,
				null,
				instructor.getDepartment());	
	}

	@Override
	@PreAuthorize("checkPermission('InstructorRoleEdit')")
	public void update(Record record, SessionContext context, Session hibSession) {
		update(DepartmentalInstructorDAO.getInstance().get(record.getUniqueId(), hibSession), record, context, hibSession);
	}
	
	protected void delete(DepartmentalInstructor instructor, SessionContext context, Session hibSession) {
		if (instructor == null) return;
		
		if (instructor.getRole() == null) return;
		
		instructor.setRole(null);
		
		hibSession.update(instructor);
		
		ChangeLog.addChange(hibSession,
				context,
				instructor,
				instructor.getName(DepartmentalInstructor.sNameFormatLastInitial) + ": " + MESSAGES.noRole(),
				Source.SIMPLE_EDIT, 
				Operation.DELETE,
				null,
				instructor.getDepartment());
	}

	@Override
	@PreAuthorize("checkPermission('InstructorRoleEdit')")
	public void delete(Record record, SessionContext context, Session hibSession) {
		delete(DepartmentalInstructorDAO.getInstance().get(record.getUniqueId(), hibSession), context, hibSession);
	}
}
