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
package org.unitime.timetable.export.events;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.unitime.timetable.export.ExportHelper;
import org.unitime.timetable.export.PDFPrinter;
import org.unitime.timetable.gwt.client.events.EventComparator.EventMeetingSortBy;
import org.unitime.timetable.gwt.shared.EventInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ApprovalStatus;
import org.unitime.timetable.gwt.shared.EventInterface.EventFlag;
import org.unitime.timetable.gwt.shared.EventInterface.EventLookupRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.EventType;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingInterface;
import org.unitime.timetable.gwt.shared.EventInterface.MultiMeetingInterface;
import org.unitime.timetable.util.Formats;

/**
 * @author Tomas Muller
 */
@Service("org.unitime.timetable.export.Exporter:events.pdf")
public class EventsExportEventsToPDF extends EventsExporter {
	
	@Override
	public String reference() {
		return "events.pdf";
	}

	@Override
	protected void print(ExportHelper helper, EventLookupRpcRequest request, List<EventInterface> events, int eventCookieFlags, EventMeetingSortBy sort, boolean asc) throws IOException {
		sort(events, sort, asc);
		Printer printer = new PDFPrinter(helper.getOutputStream(), true);
		helper.setup(printer.getContentType(), reference(), false);
		hideColumns(printer, events, eventCookieFlags);
		print(printer, events, EventFlag.SHOW_MEETING_CONTACTS.in(eventCookieFlags));
	}
	
	@Override
	protected void hideColumn(Printer out, List<EventInterface> events, EventFlag flag) {
		switch (flag) {
		case SHOW_SECTION: out.hideColumn(1); break;
		case SHOW_TITLE: out.hideColumn(3); break;
		case SHOW_NOTE: out.hideColumn(4); break;
		case SHOW_PUBLISHED_TIME: out.hideColumn(6); break;
		case SHOW_ALLOCATED_TIME: out.hideColumn(7); break;
		case SHOW_SETUP_TIME: out.hideColumn(8); break;
		case SHOW_TEARDOWN_TIME: out.hideColumn(9); break;
		case SHOW_CAPACITY: out.hideColumn(11); break;
		case SHOW_MEETING_CONTACTS: out.hideColumn(12); break;
		case SHOW_ENROLLMENT: out.hideColumn(13); break;
		case SHOW_LIMIT: out.hideColumn(14); break;
		case SHOW_SPONSOR: out.hideColumn(15); break;
		case SHOW_MAIN_CONTACT: out.hideColumn(16); break;
		case SHOW_APPROVAL: out.hideColumn(17); break;
		case SHOW_LAST_CHANGE: out.hideColumn(18); break;
		}
	}

	protected void print(Printer out, List<EventInterface> events, boolean showMeetingContacts) throws IOException {
		out.printHeader(
				/*  0 */ MESSAGES.colName(),
				/*  1 */ MESSAGES.colSection(),
				/*  2 */ MESSAGES.colType(),
				/*  3 */ MESSAGES.colTitle(),
				/*  4 */ MESSAGES.colNote(),
				/*  5 */ MESSAGES.colDate(),
				/*  6 */ MESSAGES.colPublishedTime(),
				/*  7 */ MESSAGES.colAllocatedTime(),
				/*  8 */ MESSAGES.colSetupTimeShort(),
				/*  9 */ MESSAGES.colTeardownTimeShort(),
				/* 10 */ MESSAGES.colLocation(),
				/* 11 */ MESSAGES.colCapacity(),
				/* 12 */ MESSAGES.colMeetingContacts(),
				/* 13 */ MESSAGES.colEnrollment(),
				/* 14 */ MESSAGES.colLimit(),
				/* 15 */ MESSAGES.colSponsorOrInstructor(),
				/* 16 */ MESSAGES.colMainContact(),
				/* 17 */ MESSAGES.colApproval(),
				/* 18 */ MESSAGES.colLastChange());
		
		Formats.Format<Date> df = Formats.getDateFormat(Formats.Pattern.DATE_EVENT);
		Formats.Format<Date> dfLong = Formats.getDateFormat(Formats.Pattern.DATE_EVENT_LONG);
		Formats.Format<Date> dfShort = Formats.getDateFormat(Formats.Pattern.DATE_EVENT_SHORT);
		
		for (EventInterface event: events) {
			for (MultiMeetingInterface multi: EventInterface.getMultiMeetings(event.getMeetings(), false, showMeetingContacts)) {
				MeetingInterface meeting = multi.getMeetings().first();
				out.printLine(
					getName(event),
					getSection(event),
					event.hasInstruction() ? event.getInstruction() : event.getType().getAbbreviation(CONSTANTS),
					getTitle(event),
					event.hasEventNote() ? event.getEventNote("\n").replace("<br>", "\n") : "",
					meeting.isArrangeHours() ? event.hasMessage() ? event.getMessage() : CONSTANTS.arrangeHours() : (multi.getDays(CONSTANTS) + " " + 
							(multi.getNrMeetings() == 1 ? multi.getFirstMeetingDate() == null ? "" : dfLong.format(multi.getFirstMeetingDate()) : dfShort.format(multi.getFirstMeetingDate()) + " - " + dfLong.format(multi.getLastMeetingDate()))),
					meeting.isArrangeHours() && event.hasMessage() ? CONSTANTS.arrangeHours() : meeting.getMeetingTime(CONSTANTS),
					meeting.getAllocatedTime(CONSTANTS),
					String.valueOf(meeting.getStartOffset()),
					String.valueOf(-meeting.getEndOffset()),
					meeting.getLocationName(MESSAGES),
					meeting.hasLocation() && meeting.getLocation().hasSize() ? meeting.getLocation().getSize().toString() : null,
					meeting.getMeetingContacts(CONSTANTS.meetingContactsSeparator(), MESSAGES),
					event.hasEnrollment() ? event.getEnrollment().toString() : null,
					event.hasMaxCapacity() ? event.getMaxCapacity().toString() : null,
					event.hasInstructors() ? event.getInstructorNames("\n", MESSAGES) : event.hasSponsor() ? event.getSponsor().getName() : null,
					event.hasContact() ? event.getContact().getName(MESSAGES) : null,
					event.getType() == EventType.Unavailabile ? "" :
					multi.getApprovalStatus() == ApprovalStatus.Approved ? df.format(multi.getApprovalDate()) :
					multi.getApprovalStatus() == ApprovalStatus.Cancelled ? MESSAGES.approvalCancelled() :
					multi.getApprovalStatus() == ApprovalStatus.Rejected ? MESSAGES.approvalRejected() :
					multi.getApprovalStatus() == ApprovalStatus.Deleted ? MESSAGES.approvalDeleted() :
					multi.isPast() ? MESSAGES.approvalNotApprovedPast() :
					event.getExpirationDate() != null ? MESSAGES.approvalExpire(df.format(event.getExpirationDate())) :
					MESSAGES.approvalNotApproved(),
					event.getLastNote() != null ? df.format(event.getLastNote().getDate()) + " " + event.getLastNote().getType().getName() : null
					);
			}
			out.flush();
		}
		out.close();
	}
}
