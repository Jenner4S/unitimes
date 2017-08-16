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
import org.unitime.timetable.model.RoomFeatureType;
import org.unitime.timetable.model.ChangeLog.Operation;
import org.unitime.timetable.model.ChangeLog.Source;
import org.unitime.timetable.model.dao.RoomFeatureTypeDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
@Service("gwtAdminTable[type=featureType]")
public class RoomFeatureTypes implements AdminTable {
	protected static final GwtMessages MESSAGES = Localization.create(GwtMessages.class);
	
	@Override
	public PageName name() {
		return new PageName(MESSAGES.pageRoomFeatureType(), MESSAGES.pageRoomFeatureTypes());
	}

	@Override
	@PreAuthorize("checkPermission('RoomFeatureTypes')")
	public SimpleEditInterface load(SessionContext context, Session hibSession) {
		SimpleEditInterface data = new SimpleEditInterface(
				new Field(MESSAGES.fieldAbbreviation(), FieldType.text, 160, 20, Flag.UNIQUE),
				new Field(MESSAGES.fieldName(), FieldType.text, 300, 60, Flag.UNIQUE),
				new Field(MESSAGES.fieldEventManagement(), FieldType.toggle, 40)
				);
		data.setSortBy(2, 1);
		for (RoomFeatureType ftype: RoomFeatureTypeDAO.getInstance().findAll()) {
			Record r = data.addRecord(ftype.getUniqueId());
			r.setField(0, ftype.getReference());
			r.setField(1, ftype.getLabel());
			r.setField(2, ftype.isShowInEventManagement() ? "true" : "false");
			int used =
					((Number)hibSession.createQuery(
							"select count(f) from RoomFeature f where f.featureType.uniqueId = :uniqueId")
							.setLong("uniqueId", ftype.getUniqueId()).uniqueResult()).intValue();
			r.setDeletable(used == 0);
		}
		data.setEditable(context.hasPermission(Right.RoomFeatureTypeEdit));
		return data;
	}

	@Override
	@PreAuthorize("checkPermission('RoomFeatureTypeEdit')")
	public void save(SimpleEditInterface data, SessionContext context, Session hibSession) {
		for (RoomFeatureType type: RoomFeatureTypeDAO.getInstance().findAll(hibSession)) {
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
	@PreAuthorize("checkPermission('RoomFeatureTypeEdit')")
	public void save(Record record, SessionContext context, Session hibSession) {
		RoomFeatureType type = new RoomFeatureType();
		type.setReference(record.getField(0));
		type.setLabel(record.getField(1));
		type.setShowInEventManagement("true".equals(record.getField(2)));
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
	
	protected void update(RoomFeatureType type, Record record, SessionContext context, Session hibSession) {
		if (type == null) return;
		if (ToolBox.equals(type.getReference(), record.getField(0)) &&
				ToolBox.equals(type.getLabel(), record.getField(1)) &&
				ToolBox.equals(type.getShowInEventManagement(), "true".equals(record.getField(2)))) return;
		type.setReference(record.getField(0));
		type.setLabel(record.getField(1));
		type.setShowInEventManagement("true".equals(record.getField(2)));
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
	@PreAuthorize("checkPermission('RoomFeatureTypeEdit')")
	public void update(Record record, SessionContext context, Session hibSession) {
		update(RoomFeatureTypeDAO.getInstance().get(record.getUniqueId(), hibSession), record, context, hibSession);
	}
	
	protected void delete(RoomFeatureType type, SessionContext context, Session hibSession) {
		if (type == null) return;
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
	@PreAuthorize("checkPermission('RoomFeatureTypeEdit')")
	public void delete(Record record, SessionContext context, Session hibSession) {
		delete(RoomFeatureTypeDAO.getInstance().get(record.getUniqueId(), hibSession), context, hibSession);
	}
}
