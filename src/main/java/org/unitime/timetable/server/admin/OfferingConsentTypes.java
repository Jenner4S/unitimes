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
import org.unitime.timetable.model.OfferingConsentType;
import org.unitime.timetable.model.ChangeLog.Operation;
import org.unitime.timetable.model.ChangeLog.Source;
import org.unitime.timetable.model.dao.OfferingConsentTypeDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
@Service("gwtAdminTable[type=consent]")
public class OfferingConsentTypes implements AdminTable {
	protected static final GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	@Override
	public PageName name() {
		return new PageName(MESSAGES.pageOfferingConsentType(), MESSAGES.pageOfferingConsentTypes());
	}

	@Override
	@PreAuthorize("checkPermission('OfferingConsentTypes')")
	public SimpleEditInterface load(SessionContext context, Session hibSession) {
		SimpleEditInterface data = new SimpleEditInterface(
				new Field(MESSAGES.fieldReference(), FieldType.text, 160, 20, Flag.UNIQUE),
				new Field(MESSAGES.fieldName(), FieldType.text, 300, 60, Flag.UNIQUE),
				new Field(MESSAGES.fieldAbbreviation(), FieldType.text, 160, 20, Flag.UNIQUE));
		data.setSortBy(0, 1);
		data.setAddable(false);
		for (OfferingConsentType consent: OfferingConsentTypeDAO.getInstance().findAll()) {
			Record r = data.addRecord(consent.getUniqueId(), false);
			r.setField(0, consent.getReference(), false);
			r.setField(1, consent.getLabel(), true);
			r.setField(2, consent.getAbbv(), true);
		}
		data.setEditable(context.hasPermission(Right.OfferingConsentTypeEdit));
		return data;
	}

	@Override
	@PreAuthorize("checkPermission('OfferingConsentTypeEdit')")
	public void save(SimpleEditInterface data, SessionContext context, Session hibSession) {
		for (OfferingConsentType consent: OfferingConsentTypeDAO.getInstance().findAll()) {
			Record r = data.getRecord(consent.getUniqueId());
			if (r == null)
				delete(consent, context, hibSession);
			else 
				update(consent, r, context, hibSession);
		}
		for (Record r: data.getNewRecords()) 
			save(r, context, hibSession);
	}

	@Override
	@PreAuthorize("checkPermission('OfferingConsentTypeEdit')")
	public void save(Record record, SessionContext context, Session hibSession) {
		OfferingConsentType consent = new OfferingConsentType();
		consent.setReference(record.getField(0));
		consent.setLabel(record.getField(1));
		consent.setAbbv(record.getField(2));
		record.setUniqueId((Long)hibSession.save(consent));
		ChangeLog.addChange(hibSession,
				context,
				consent,
				consent.getReference() + " " + consent.getLabel(),
				Source.SIMPLE_EDIT, 
				Operation.CREATE,
				null,
				null);
	}
	
	protected void update(OfferingConsentType consent, Record record, SessionContext context, Session hibSession) {
		if (consent == null) return;
		if (ToolBox.equals(consent.getReference(), record.getField(0)) &&
				ToolBox.equals(consent.getLabel(), record.getField(1)) &&
				ToolBox.equals(consent.getAbbv(), record.getField(2))) return;
		consent.setReference(record.getField(0));
		consent.setLabel(record.getField(1));
		consent.setAbbv(record.getField(2));
		hibSession.saveOrUpdate(consent);
		ChangeLog.addChange(hibSession,
				context,
				consent,
				consent.getReference() + " " + consent.getLabel(),
				Source.SIMPLE_EDIT, 
				Operation.UPDATE,
				null,
				null);
	}

	@Override
	@PreAuthorize("checkPermission('OfferingConsentTypeEdit')")
	public void update(Record record, SessionContext context, Session hibSession) {
		update(OfferingConsentTypeDAO.getInstance().get(record.getUniqueId(), hibSession), record, context, hibSession);
	}
	
	protected void delete(OfferingConsentType consent, SessionContext context, Session hibSession) {
		if (consent == null) return;
		ChangeLog.addChange(hibSession,
				context,
				consent,
				consent.getReference() + " " + consent.getLabel(),
				Source.SIMPLE_EDIT, 
				Operation.DELETE,
				null,
				null);
		hibSession.delete(consent);
	}

	@Override
	@PreAuthorize("checkPermission('OfferingConsentTypeEdit')")
	public void delete(Record record, SessionContext context, Session hibSession) {
		delete(OfferingConsentTypeDAO.getInstance().get(record.getUniqueId(), hibSession), context, hibSession);
	}

}
