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


import org.cpsolver.ifs.util.ToolBox;
import org.hibernate.Session;
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
import org.unitime.timetable.model.CourseCreditType;
import org.unitime.timetable.model.ChangeLog.Operation;
import org.unitime.timetable.model.ChangeLog.Source;
import org.unitime.timetable.model.dao.CourseCreditTypeDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
@Service("gwtAdminTable[type=creditType]")
public class CourseCreditTypes implements AdminTable {
	protected static final GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	@Override
	public PageName name() {
		return new PageName(MESSAGES.pageCourseCreditType(), MESSAGES.pageCourseCreditTypes());
	}

	@Override
	@PreAuthorize("checkPermission('CourseCreditTypes')")
	public SimpleEditInterface load(SessionContext context, Session hibSession) {
		SimpleEditInterface data = new SimpleEditInterface(
				new Field(MESSAGES.fieldReference(), FieldType.text, 160, 20, Flag.UNIQUE),
				new Field(MESSAGES.fieldName(), FieldType.text, 300, 60, Flag.UNIQUE),
				new Field(MESSAGES.fieldAbbreviation(), FieldType.text, 80, 10));
		data.setSortBy(0, 1, 2);
		for (CourseCreditType credit: CourseCreditTypeDAO.getInstance().findAll()) {
			int used =
				((Number)hibSession.createQuery(
						"select count(c) from CourseCreditUnitConfig c where c.creditType.uniqueId = :uniqueId")
						.setLong("uniqueId", credit.getUniqueId()).uniqueResult()).intValue();
			Record r = data.addRecord(credit.getUniqueId(), used == 0);
			r.setField(0, credit.getReference());
			r.setField(1, credit.getLabel());
			r.setField(2, credit.getAbbreviation());
		}
		data.setEditable(context.hasPermission(Right.CourseCreditTypeEdit));
		return data;
	}

	@Override
	@PreAuthorize("checkPermission('CourseCreditTypeEdit')")
	public void save(SimpleEditInterface data, SessionContext context, Session hibSession) {
		for (CourseCreditType credit: CourseCreditTypeDAO.getInstance().findAll()) {
			Record r = data.getRecord(credit.getUniqueId());
			if (r == null)
				delete(credit, context, hibSession);
			else
				update(credit, r, context, hibSession);
		}
		for (Record r: data.getNewRecords())
			save(r, context, hibSession);
	}

	@Override
	@PreAuthorize("checkPermission('CourseCreditTypeEdit')")
	public void save(Record record, SessionContext context, Session hibSession) {
		CourseCreditType credit = new CourseCreditType();
		credit.setReference(record.getField(0));
		credit.setLabel(record.getField(1));
		credit.setAbbreviation(record.getField(2));
		record.setUniqueId((Long)hibSession.save(credit));
		ChangeLog.addChange(hibSession,
				context,
				credit,
				credit.getReference() + " " + credit.getLabel(),
				Source.SIMPLE_EDIT, 
				Operation.CREATE,
				null,
				null);		
	}

	protected void update(CourseCreditType credit, Record record, SessionContext context, Session hibSession) {
		if (credit == null) return;
		if (ToolBox.equals(credit.getReference(), record.getField(0)) &&
				ToolBox.equals(credit.getLabel(), record.getField(1)) &&
				ToolBox.equals(credit.getAbbreviation(), record.getField(2))) return;
		credit.setReference(record.getField(0));
		credit.setLabel(record.getField(1));
		credit.setAbbreviation(record.getField(2));
		hibSession.saveOrUpdate(credit);
		ChangeLog.addChange(hibSession,
				context,
				credit,
				credit.getReference() + " " + credit.getLabel(),
				Source.SIMPLE_EDIT, 
				Operation.UPDATE,
				null,
				null);
	}
	
	@Override
	@PreAuthorize("checkPermission('CourseCreditTypeEdit')")
	public void update(Record record, SessionContext context, Session hibSession) {
		update(CourseCreditTypeDAO.getInstance().get(record.getUniqueId(), hibSession), record, context, hibSession);
	}
	
	protected void delete(CourseCreditType credit, SessionContext context, Session hibSession) {
		if (credit == null) return;
		ChangeLog.addChange(hibSession,
				context,
				credit,
				credit.getReference() + " " + credit.getLabel(),
				Source.SIMPLE_EDIT, 
				Operation.DELETE,
				null,
				null);
		hibSession.delete(credit);
	}

	@Override
	@PreAuthorize("checkPermission('CourseCreditTypeEdit')")
	public void delete(Record record, SessionContext context, Session hibSession) {
		delete(CourseCreditTypeDAO.getInstance().get(record.getUniqueId(), hibSession), context, hibSession);
	}
}
