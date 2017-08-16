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

import java.util.HashSet;
import java.util.List;
import java.util.Set;


import org.cpsolver.ifs.util.ToolBox;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.unitime.localization.impl.Localization;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.SimpleEditInterface;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Field;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.FieldType;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Flag;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.PageName;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Record;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.CourseType;
import org.unitime.timetable.model.StudentSectioningStatus;
import org.unitime.timetable.model.ChangeLog.Operation;
import org.unitime.timetable.model.ChangeLog.Source;
import org.unitime.timetable.model.dao.CourseTypeDAO;
import org.unitime.timetable.model.dao.StudentSectioningStatusDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
@Service("gwtAdminTable[type=sectioning]")
public class StudentSchedulingStatusTypes implements AdminTable {
	protected static final GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	@Override
	public PageName name() {
		return new PageName(MESSAGES.pageStudentSchedulingStatusType(), MESSAGES.pageStudentSchedulingStatusTypes());
	}
	
	enum StatusOption {
		Access(MESSAGES.toggleAccess(), StudentSectioningStatus.Option.enabled),
		Enrollment(MESSAGES.toggleEnrollment(), StudentSectioningStatus.Option.enrollment),
		Advisor(MESSAGES.toggleAdvisor(), StudentSectioningStatus.Option.advisor),
		Admin(MESSAGES.toggleAdmin(), StudentSectioningStatus.Option.admin),
		Email(MESSAGES.toggleEmail(), StudentSectioningStatus.Option.email),
		WaitListing(MESSAGES.toggleWaitList(), StudentSectioningStatus.Option.waitlist),
		NoBatch(MESSAGES.toggleNoBatch(), StudentSectioningStatus.Option.nobatch),
		;
		
		private StudentSectioningStatus.Option iOption;
		private String iLabel;
		
		StatusOption(String label, StudentSectioningStatus.Option option) {
			iLabel = label; iOption = option;  
		}
		
		public String getLabel() { return iLabel; }
		public StudentSectioningStatus.Option getOption() { return iOption; }		
	}

	@Override
	@PreAuthorize("checkPermission('StudentSchedulingStatusTypes')")
	public SimpleEditInterface load(SessionContext context, Session hibSession) {
		List<CourseType> courseTypes = CourseTypeDAO.getInstance().findAll(Order.asc("reference"));
		SimpleEditInterface.Field[] fields = new SimpleEditInterface.Field[courseTypes.isEmpty() ? 3 + StatusOption.values().length : 4 + StatusOption.values().length + courseTypes.size()];
		int idx = 0;
		fields[idx++] = new Field(MESSAGES.fieldAbbreviation(), FieldType.text, 160, 20, Flag.UNIQUE);
		fields[idx++] = new Field(MESSAGES.fieldName(), FieldType.text, 300, 60, Flag.UNIQUE);
		for (StatusOption t: StatusOption.values())
			fields[idx++] = new Field(t.getLabel(), FieldType.toggle, 40);
		fields[idx++] = new Field(MESSAGES.fieldMessage(), FieldType.text, 400, 200);
		if (!courseTypes.isEmpty()) {
			for (int i = 0; i < courseTypes.size(); i++)
				fields[idx++] = new Field(courseTypes.get(i).getReference(), FieldType.toggle, 40);
			fields[idx++] = new Field(MESSAGES.toggleNoCourseType(), FieldType.toggle, 40);
		}
		SimpleEditInterface data = new SimpleEditInterface(fields);
		data.setSortBy(0, 1);
		for (StudentSectioningStatus status: StudentSectioningStatusDAO.getInstance().findAll()) {
			Record r = data.addRecord(status.getUniqueId());
			idx = 0;
			r.setField(idx++, status.getReference());
			r.setField(idx++, status.getLabel());
			for (StatusOption t: StatusOption.values())
				r.setField(idx++, status.hasOption(t.getOption()) ? "true" : "false");
			r.setField(idx++, status.getMessage());
			if (!courseTypes.isEmpty()) {
				for (int i = 0; i < courseTypes.size(); i++)
					r.setField(idx++, status.getTypes().contains(courseTypes.get(i)) ? "true" : "false");
				r.setField(idx++, status.hasOption(StudentSectioningStatus.Option.notype) ? "false" : "true");
			}
		}
		data.setEditable(context.hasPermission(Right.StudentSchedulingStatusTypeEdit));
		return data;
	}

	@Override
	@PreAuthorize("checkPermission('StudentSchedulingStatusTypeEdit')")
	public void save(SimpleEditInterface data, SessionContext context, Session hibSession) {
		for (StudentSectioningStatus status: StudentSectioningStatusDAO.getInstance().findAll()) {
			Record r = data.getRecord(status.getUniqueId());
			if (r == null)
				delete(status, context, hibSession);
			else
				update(status, r, context, hibSession);
		}
		for (Record r: data.getNewRecords())
			save(r, context, hibSession);
	}

	@Override
	@PreAuthorize("checkPermission('StudentSchedulingStatusTypeEdit')")
	public void save(Record record, SessionContext context, Session hibSession) {
		StudentSectioningStatus status = new StudentSectioningStatus();
		int value = 0;
		for (int i = 0; i < StatusOption.values().length; i++)
			if ("true".equals(record.getField(2 + i))) value += StatusOption.values()[i].getOption().toggle();
		status.setTypes(new HashSet<CourseType>());
		List<CourseType> courseTypes = CourseTypeDAO.getInstance().findAll(Order.asc("reference"));
		if (!courseTypes.isEmpty()) {
			for (int i = 0; i < courseTypes.size(); i++)
				if ("true".equals(record.getField(3 + StatusOption.values().length + i))) status.getTypes().add(courseTypes.get(i));
			if (!"true".equals(record.getField(3 + StatusOption.values().length + courseTypes.size()))) value += StudentSectioningStatus.Option.notype.toggle();
		}
		status.setReference(record.getField(0));
		status.setLabel(record.getField(1));
		status.setStatus(value);
		status.setMessage(record.getField(2 + StatusOption.values().length));
		record.setUniqueId((Long)hibSession.save(status));
		ChangeLog.addChange(hibSession,
				context,
				status,
				status.getReference() + " " + status.getLabel(),
				Source.SIMPLE_EDIT, 
				Operation.CREATE,
				null,
				null);
	}
	
	protected void update(StudentSectioningStatus status, Record record, SessionContext context, Session hibSession) {
		if (status == null) return;
		int value = 0;
		for (int i = 0; i < StatusOption.values().length; i++)
			if ("true".equals(record.getField(2 + i))) value += StatusOption.values()[i].getOption().toggle();
		Set<CourseType> types = new HashSet<CourseType>();
		List<CourseType> courseTypes = CourseTypeDAO.getInstance().findAll(Order.asc("reference"));
		if (!courseTypes.isEmpty()) {
			for (int i = 0; i < courseTypes.size(); i++)
				if ("true".equals(record.getField(3 + StatusOption.values().length + i))) types.add(courseTypes.get(i));
			if (!"true".equals(record.getField(3 + StatusOption.values().length + courseTypes.size()))) value += StudentSectioningStatus.Option.notype.toggle();
		}
		boolean changed = 
			!ToolBox.equals(status.getReference(), record.getField(0)) ||
			!ToolBox.equals(status.getLabel(), record.getField(1)) ||
			!ToolBox.equals(status.getStatus(), value) ||
			!ToolBox.equals(status.getTypes(), types) ||
			!ToolBox.equals(status.getMessage(), record.getField(2 + StatusOption.values().length));
		status.setReference(record.getField(0));
		status.setLabel(record.getField(1));
		status.setStatus(value);
		status.setTypes(types);
		status.setMessage(record.getField(2 + StatusOption.values().length));
		hibSession.saveOrUpdate(status);
		if (changed)
			ChangeLog.addChange(hibSession,
					context,
					status,
					status.getReference() + " " + status.getLabel(),
					Source.SIMPLE_EDIT, 
					Operation.UPDATE,
					null,
					null);
	}

	@Override
	@PreAuthorize("checkPermission('StudentSchedulingStatusTypeEdit')")
	public void update(Record record, SessionContext context, Session hibSession) {
		update(StudentSectioningStatusDAO.getInstance().get(record.getUniqueId(), hibSession), record, context, hibSession);
	}

	protected void delete(StudentSectioningStatus status, SessionContext context, Session hibSession) {
		if (status == null) return;
		ChangeLog.addChange(hibSession,
				context,
				status,
				status.getReference() + " " + status.getLabel(),
				Source.SIMPLE_EDIT, 
				Operation.DELETE,
				null,
				null);
		hibSession.delete(status);
	}
	
	@Override
	@PreAuthorize("checkPermission('StudentSchedulingStatusTypeEdit')")
	public void delete(Record record, SessionContext context, Session hibSession) {
		delete(StudentSectioningStatusDAO.getInstance().get(record.getUniqueId(), hibSession), context, hibSession);
	}
}
