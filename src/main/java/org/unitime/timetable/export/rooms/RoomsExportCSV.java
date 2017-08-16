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
package org.unitime.timetable.export.rooms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.unitime.timetable.export.CSVPrinter;
import org.unitime.timetable.export.ExportHelper;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomDetailInterface;
import org.unitime.timetable.gwt.shared.RoomInterface.RoomsColumn;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
@Service("org.unitime.timetable.export.Exporter:rooms.csv")
public class RoomsExportCSV extends RoomsExporter {

	@Override
	public String reference() { return "rooms.csv"; }
	
	@Override
	protected void print(ExportHelper helper, List<RoomDetailInterface> rooms, ExportContext context) throws IOException {
		helper.getSessionContext().hasPermission(Right.RoomsExportCsv);
		
		List<Column> columns = new ArrayList<Column>();
		for (RoomsColumn column: RoomsColumn.values()) {
			int nrCells = getNbrCells(column, context);
			for (int idx = 0; idx < nrCells; idx++) {
				Column c = new Column(column, idx);
				if (isColumnVisible(c, context)) columns.add(c);
			}
		}
				
		Printer printer = new CSVPrinter(helper.getWriter(), false);
		helper.setup(printer.getContentType(), reference(), false);
		
		String[] header = new String[columns.size()];
		for (int i = 0; i < columns.size(); i++)
			header[i] = getColumnName(columns.get(i), context).replace("<br>", "\n");
		printer.printHeader(header);
		printer.flush();
		
		for (RoomDetailInterface room: rooms) {
			String[] row = new String[columns.size()];
			for (int i = 0; i < columns.size(); i++)
				row[i] = getCell(room, columns.get(i), context);
			printer.printLine(row);
			printer.flush();
		}
		printer.close();
	}
	
	@Override
	protected int getNbrCells(RoomsColumn column, ExportContext ec) {
		switch (column) {
		case COORDINATES:
			return 2;
		case PICTURES:
		case MAP:
			return 0;
		}
		return super.getNbrCells(column, ec);
	}
	
	
	protected int getNbrColumns(ExportContext context) {
		int ret = 0;
		for (RoomsColumn rc: RoomsColumn.values())
			ret += getNbrCells(rc, context);
		return ret;
	}
	
	@Override
	protected String getColumnName(Column column, ExportContext context) {
		switch (column.getColumn()) {
		case COORDINATES:
			if (column.getIndex() == 0)
				return MESSAGES.colCoordinateX();
			else
				return MESSAGES.colCoordinateY();
		}
		return super.getColumnName(column, context).replace("<br>", "\n");
	}
	
	protected String getCell(RoomDetailInterface room, Column column, ExportContext context) {
		switch (column.getColumn()) {
		case NAME:
			return room.hasDisplayName() ? MESSAGES.label(room.getLabel(), room.getDisplayName()) : room.getLabel();
		case EXTERNAL_ID:
			return room.hasExternalId() ? room.getExternalId() : "";
		case TYPE:
			return room.getRoomType().getLabel();
		case CAPACITY:
			return room.getCapacity() == null ? "0" : room.getCapacity().toString();
		case EXAM_CAPACITY:
			return room.getExamCapacity() == null ? "" : room.getExamCapacity().toString();
		case AREA:
			return room.getArea() == null ? "" : room.getArea().toString();
		case COORDINATES:
			if (column.getIndex() == 0)
				return room.getX() == null ? "" : room.getX().toString();
			else
				return room.getY() == null ? "" : room.getY().toString();
		case ROOM_CHECK:
			return room.isIgnoreRoomCheck() ? MESSAGES.exportFalse() : MESSAGES.exportTrue();
		case DISTANCE_CHECK:
			return room.isIgnoreTooFar() ? MESSAGES.exportFalse() : MESSAGES.exportTrue();
		case PREFERENCE:
			return context.pref2string(room.getDepartments());
		case AVAILABILITY:
			return room.getAvailability();
		case DEPARTMENTS:
			return context.dept2string(room.getDepartments(), true);
		case CONTROL_DEPT:
			return context.dept2string(room.getControlDepartment(), true);
		case EXAM_TYPES:
			return context.examTypes2string(room.getExamTypes());
		case PERIOD_PREF:
			return room.getPeriodPreference();
		case EVENT_DEPARTMENT:
			return context.dept2string(room.getEventDepartment(), false);
		case EVENT_STATUS:
			return (room.getEventStatus() != null ? CONSTANTS.eventStatusAbbv()[room.getEventStatus()] : room.getDefaultEventStatus() != null ? CONSTANTS.eventStatusAbbv()[room.getDefaultEventStatus()] : "").replace("<br>", "\n");
		case EVENT_AVAILABILITY:
			return room.getEventAvailability();
		case EVENT_MESSAGE:
			return room.getEventNote() != null ? room.getEventNote() : room.getDefaultEventNote();
		case BREAK_TIME:
			return room.getBreakTime() != null ? room.getBreakTime().toString() : room.getDefaultBreakTime() != null ? room.getDefaultBreakTime().toString() : "";
		case GROUPS:
			return context.groups2string(room.getGroups());
		case FEATURES:
			if (column.getIndex() == 0)
				return context.features2string(room.getFeatures(), null);
			else
				return context.features2string(room.getFeatures(), context.getRoomFeatureTypes().get(column.getIndex() - 1));
		default:
			return null;
		}
	}
}
