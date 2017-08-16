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
import org.unitime.timetable.gwt.command.client.GwtRpcException;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.SimpleEditInterface;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Field;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.FieldType;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Flag;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.ListItem;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.PageName;
import org.unitime.timetable.gwt.shared.SimpleEditInterface.Record;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.ExamType;
import org.unitime.timetable.model.ChangeLog.Operation;
import org.unitime.timetable.model.ChangeLog.Source;
import org.unitime.timetable.model.dao.ExamTypeDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
@Service("gwtAdminTable[type=examType]")
public class ExaminationTypes implements AdminTable {
	protected static final GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	@Override
	public PageName name() {
		return new PageName(MESSAGES.pageExaminationType(), MESSAGES.pageExaminationTypes());
	}

	@Override
	@PreAuthorize("checkPermission('ExamTypes')")
	public SimpleEditInterface load(SessionContext context, Session hibSession) {
		List<ListItem> types = new ArrayList<ListItem>();
		types.add(new ListItem(String.valueOf(ExamType.sExamTypeFinal), MESSAGES.finalExaminations()));
		types.add(new ListItem(String.valueOf(ExamType.sExamTypeMidterm), MESSAGES.midtermExaminations()));
		SimpleEditInterface data = new SimpleEditInterface(
				new Field(MESSAGES.fieldReference(), FieldType.text, 160, 20, Flag.UNIQUE),
				new Field(MESSAGES.fieldName(), FieldType.text, 300, 60, Flag.UNIQUE),
				new Field(MESSAGES.fieldType(), FieldType.list, 300, types, Flag.NOT_EMPTY),
				new Field(MESSAGES.fieldHighlightInEvents(), FieldType.toggle, 40)
				);
		data.setSortBy(2, 1);
		for (ExamType xtype: ExamTypeDAO.getInstance().findAll()) {
			Record r = data.addRecord(xtype.getUniqueId());
			r.setField(0, xtype.getReference(), !xtype.getReference().equals("final") && !xtype.getReference().equals("midterm"));
			r.setField(1, xtype.getLabel());
			r.setField(2, xtype.getType().toString(), !xtype.isUsed(null) && !xtype.getReference().equals("final") && !xtype.getReference().equals("midterm"));
			r.setField(3, xtype.isHighlightInEvents() != null && xtype.isHighlightInEvents() ? "true": "false");
			r.setDeletable(!xtype.isUsed(null) && !xtype.getReference().equals("final") && !xtype.getReference().equals("midterm"));
		}
		data.setEditable(context.hasPermission(Right.ExamTypeEdit));
		return data;
	}

	@Override
	@PreAuthorize("checkPermission('ExamTypeEdit')")
	public void save(SimpleEditInterface data, SessionContext context, Session hibSession) {
		for (ExamType type: ExamTypeDAO.getInstance().findAll(hibSession)) {
			Record r = data.getRecord(type.getUniqueId());
			if (r == null)
				delete(type, context, hibSession);
			else
				update(type, r, context, hibSession);
		}
		for (Record r: data.getNewRecords())
			save(r, context, hibSession);
	}

	@Override
	@PreAuthorize("checkPermission('ExamTypeEdit')")
	public void save(Record record, SessionContext context, Session hibSession) {
		ExamType type = new ExamType();
		type.setReference(record.getField(0));
		type.setLabel(record.getField(1));
		type.setType(Integer.valueOf(record.getField(2)));
		type.setHighlightInEvents("true".equalsIgnoreCase(record.getField(3)));
		record.setUniqueId((Long)hibSession.save(type));
		ChangeLog.addChange(hibSession,
				context,
				type,
				type.getReference(),
				Source.SIMPLE_EDIT, 
				Operation.CREATE,
				null,
				null);		
	}
	
	protected void update(ExamType type, Record record, SessionContext context, Session hibSession) {
		if (type == null) return;
		if (ToolBox.equals(type.getReference(), record.getField(0)) &&
				ToolBox.equals(type.getLabel(), record.getField(1)) &&
				ToolBox.equals(type.getType(), Integer.valueOf(record.getField(2))) &&
				ToolBox.equals(type.isHighlightInEvents(), "true".equalsIgnoreCase(record.getField(3)))) return;
		type.setReference(record.getField(0));
		type.setLabel(record.getField(1));
		type.setType(Integer.valueOf(record.getField(2)));
		type.setHighlightInEvents("true".equalsIgnoreCase(record.getField(3)));
		hibSession.saveOrUpdate(type);
		ChangeLog.addChange(hibSession,
				context,
				type,
				type.getReference(),
				Source.SIMPLE_EDIT, 
				Operation.UPDATE,
				null,
				null);
	}

	@Override
	@PreAuthorize("checkPermission('ExamTypeEdit')")
	public void update(Record record, SessionContext context, Session hibSession) {
		update(ExamTypeDAO.getInstance().get(record.getUniqueId(), hibSession), record, context, hibSession);
	}
	
	protected void delete(ExamType type, SessionContext context, Session hibSession) {
		if (type == null) return;
		if (type.isUsed(null))
			throw new GwtRpcException(MESSAGES.failedDeleteUsedExaminationType(type.getReference()));
		ChangeLog.addChange(hibSession,
				context,
				type,
				type.getReference(),
				Source.SIMPLE_EDIT, 
				Operation.DELETE,
				null,
				null);
		hibSession.delete(type);
	}

	@Override
	@PreAuthorize("checkPermission('ExamTypeEdit')")
	public void delete(Record record, SessionContext context, Session hibSession) {
		delete(ExamTypeDAO.getInstance().get(record.getUniqueId(), hibSession), context, hibSession);
	}
}
