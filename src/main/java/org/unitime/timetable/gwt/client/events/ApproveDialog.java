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
package org.unitime.timetable.gwt.client.events;

import java.util.List;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.events.EventAdd.EventPropertiesProvider;
import org.unitime.timetable.gwt.client.events.EventMeetingTable.EventMeetingRow;
import org.unitime.timetable.gwt.client.events.EventMeetingTable.MeetingFilter;
import org.unitime.timetable.gwt.client.events.EventMeetingTable.OperationType;
import org.unitime.timetable.gwt.client.widgets.P;
import org.unitime.timetable.gwt.client.widgets.SimpleForm;
import org.unitime.timetable.gwt.client.widgets.UniTimeDialogBox;
import org.unitime.timetable.gwt.client.widgets.UniTimeFileUpload;
import org.unitime.timetable.gwt.client.widgets.UniTimeHeaderPanel;
import org.unitime.timetable.gwt.resources.GwtMessages;
import org.unitime.timetable.gwt.shared.EventInterface;
import org.unitime.timetable.gwt.shared.EventInterface.ApproveEventRpcRequest;
import org.unitime.timetable.gwt.shared.EventInterface.EventPropertiesRpcResponse;
import org.unitime.timetable.gwt.shared.EventInterface.MeetingInterface;
import org.unitime.timetable.gwt.shared.EventInterface.StandardEventNoteInterface;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Tomas Muller
 */
public abstract class ApproveDialog extends UniTimeDialogBox implements EventMeetingTable.Implementation {
	private static final GwtMessages MESSAGES = GWT.create(GwtMessages.class);
	private SimpleForm iForm;
	private TextArea iNotes;
	private EventMeetingTable iTable;
	private ListBox iStandardNotes;
	private UniTimeFileUpload iFileUpload;
	private UniTimeHeaderPanel iFooter;
	private CheckBox iEmailConfirmation;
	private Button iStandardNotesSelection;
	
	public ApproveDialog(EventPropertiesProvider properties) {
		super(true, false);
		addStyleName("unitime-ApproveDialog");
		iTable = new EventMeetingTable(EventMeetingTable.Mode.ApprovalOfSingleEventMeetings, false, properties);
		
		iForm = new SimpleForm();
		
		if (iTable instanceof Widget) {
			ScrollPanel scroll = new ScrollPanel((Widget)iTable);
			scroll.addStyleName("scroll");
			iForm.addRow(MESSAGES.propMeetings(), scroll);
		}
		
		P notes = new P("standard-notes");
		iStandardNotes = new ListBox();
		iStandardNotes.setVisibleItemCount(4);
		iStandardNotesSelection = new Button(MESSAGES.buttonSelect());
		ToolBox.setWhiteSpace(iStandardNotesSelection.getElement().getStyle(), "nowrap");
		Character accessKey = UniTimeHeaderPanel.guessAccessKey(MESSAGES.buttonSelect());
		if (accessKey != null)
			iStandardNotesSelection.setAccessKey(accessKey);
		iStandardNotesSelection.setEnabled(false);
		notes.add(iStandardNotes); notes.add(iStandardNotesSelection);
		iForm.addRow(MESSAGES.propStandardNotes(), notes);
		iStandardNotes.addDoubleClickHandler(new DoubleClickHandler() {
			@Override
			public void onDoubleClick(DoubleClickEvent event) {
				if (iStandardNotes.getItemCount() <= 0) return;
				String text = iNotes.getText();
				if (!text.isEmpty() && !text.endsWith("\n"))
					text += "\n";
				text += iStandardNotes.getValue(iStandardNotes.getSelectedIndex());
				iNotes.setText(text);
			}
		});
		iStandardNotes.addKeyPressHandler(new KeyPressHandler() {
			@Override
			public void onKeyPress(KeyPressEvent event) {
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					String text = iNotes.getText();
					if (!text.isEmpty() && !text.endsWith("\n"))
						text += "\n";
					text += iStandardNotes.getValue(iStandardNotes.getSelectedIndex());
					iNotes.setText(text);
					event.preventDefault();
					event.stopPropagation();
				}
			}
		});
		iStandardNotes.addChangeHandler(new ChangeHandler() {
			@Override
			public void onChange(ChangeEvent event) {
				iStandardNotesSelection.setEnabled(iStandardNotes.getSelectedIndex() >= 0);
			}
		});
		iStandardNotesSelection.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				if (iStandardNotes.getSelectedIndex() >= 0) {
					String text = iNotes.getText();
					if (!text.isEmpty() && !text.endsWith("\n"))
						text += "\n";
					text += iStandardNotes.getValue(iStandardNotes.getSelectedIndex());
					iNotes.setText(text);
				}
				Scheduler.get().scheduleDeferred(new ScheduledCommand() {
					@Override
					public void execute() {
						iNotes.setFocus(true);							
					}
				});
			}
		});
		
		iNotes = new TextArea();
		iNotes.setStyleName("unitime-TextArea");
		iNotes.setVisibleLines(5);
		iNotes.setCharacterWidth(80);
		iForm.addRow(MESSAGES.propNotes(), iNotes);
		
		iFileUpload = new UniTimeFileUpload();
		iForm.addRow(MESSAGES.propAttachment(), iFileUpload);
		
		iFooter = new UniTimeHeaderPanel();
		iFooter.addButton("approve", MESSAGES.opApproveMeetings(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onSubmit(ApproveEventRpcRequest.Operation.APPROVE, iTable.getValue(), iNotes.getText(), isSendEmailConformation());
				hide();
			}
		});
		iFooter.addButton("inquire", MESSAGES.opInquireMeetings(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onSubmit(ApproveEventRpcRequest.Operation.INQUIRE, iTable.getValue(), iNotes.getText(), isSendEmailConformation());
				hide();
			}
		});
		iFooter.addButton("reject", MESSAGES.opRejectMeetings(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onSubmit(ApproveEventRpcRequest.Operation.REJECT, iTable.getValue(), iNotes.getText(), isSendEmailConformation());
				hide();
			}
		});
		iFooter.addButton("cancel", MESSAGES.opCancelMeetings(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				onSubmit(ApproveEventRpcRequest.Operation.CANCEL, iTable.getValue(), iNotes.getText(), isSendEmailConformation());
				hide();
			}
		});

		iFooter.addButton("back", MESSAGES.opBack(), new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				hide();
			}
		});
		
		iEmailConfirmation = new CheckBox(MESSAGES.checkSendEmailConfirmation(), true);
		iEmailConfirmation.addStyleName("toggle");
		iFooter.insertRight(iEmailConfirmation, true);
		
		iForm.addBottomRow(iFooter);
		
		addCloseHandler(new CloseHandler<PopupPanel>() {
			@Override
			public void onClose(CloseEvent<PopupPanel> event) {
				RootPanel.getBodyElement().getStyle().setOverflow(Overflow.AUTO);
			}
		});
		
		setWidget(iForm);
	}
	
	protected abstract void onSubmit(ApproveEventRpcRequest.Operation operation, List<EventMeetingRow> items, String message, boolean sendEmailConfirmation);
	
	public void reset(EventPropertiesRpcResponse properties) {
		iNotes.setText("");
		iEmailConfirmation.setValue(properties == null ||  properties.isEmailConfirmation());
		iEmailConfirmation.setVisible(properties == null || properties.hasEmailConfirmation());
		iStandardNotes.clear();
		iForm.getRowFormatter().setVisible(iForm.getRow(MESSAGES.propStandardNotes()), properties != null && properties.hasStandardNotes());
		if (properties != null && properties.hasStandardNotes()) {
			for (StandardEventNoteInterface note: properties.getStandardNotes())
				iStandardNotes.addItem(note.toString(), note.getNote());
		}
		iFileUpload.reset();
	}
	
	public void show(List<EventMeetingRow> meetings, ApproveEventRpcRequest.Operation operation) {
		iTable.setValue(meetings);
		switch (operation) {
		case APPROVE: setText(MESSAGES.dialogApprove()); break;
		case REJECT: setText(MESSAGES.dialogReject()); break;
		case INQUIRE: setText(MESSAGES.dialogInquire()); break;
		case CANCEL: setText(MESSAGES.dialogCancel()); break;
		}
		iFooter.setEnabled("approve", operation == ApproveEventRpcRequest.Operation.APPROVE);
		iFooter.setEnabled("reject", operation == ApproveEventRpcRequest.Operation.REJECT);
		iFooter.setEnabled("inquire", operation == ApproveEventRpcRequest.Operation.INQUIRE);
		iFooter.setEnabled("cancel", operation == ApproveEventRpcRequest.Operation.CANCEL);
		iFileUpload.check();
		center();
		if (iStandardNotes.getItemCount() == 0)
			iNotes.setFocus(true);
		else
			iStandardNotes.setFocus(true);
		RootPanel.getBodyElement().getStyle().setOverflow(Overflow.HIDDEN);
	}
	
	public String getNote() {
		return iNotes.getText();
	}
	
	public boolean isSendEmailConformation() {
		return !iEmailConfirmation.isVisible() || iEmailConfirmation.getValue();
	}
	
	@Override
	public void execute(final EventMeetingTable source, OperationType operation, List<EventMeetingRow> selection) {
		switch (source.getMode()) {
		case ListOfEvents:
			iTable.setMode(EventMeetingTable.Mode.ApprovalOfEvents);
			break;
		case ListOfMeetings:
			iTable.setMode(EventMeetingTable.Mode.ApprovalOfMeetings);
			break;
		case MeetingsOfAnEvent:
			iTable.setMode(EventMeetingTable.Mode.ApprovalOfSingleEventMeetings);
			break;
		}
		switch (operation) {
		case Approve:
		case Reject:
			iTable.setMeetingFilter(new MeetingFilter() {
				@Override
				public boolean filter(EventInterface event, MeetingInterface meeting) {
					return !meeting.isCanApprove() || (source.getMeetingFilter() != null && source.getMeetingFilter().filter(event, meeting));
				}
			});
			break;
		case Inquire:
			iTable.setMeetingFilter(new MeetingFilter() {
				@Override
				public boolean filter(EventInterface event, MeetingInterface meeting) {
					return !meeting.isCanInquire() || (source.getMeetingFilter() != null && source.getMeetingFilter().filter(event, meeting));
				}
			});
			break;
		case Cancel:
			iTable.setMeetingFilter(new MeetingFilter() {
				@Override
				public boolean filter(EventInterface event, MeetingInterface meeting) {
					return !meeting.isCanCancel() || (source.getMeetingFilter() != null && source.getMeetingFilter().filter(event, meeting));
				}
			});
			break;
		}			
		switch (operation) {
		case Approve:
			show(selection, ApproveEventRpcRequest.Operation.APPROVE);
			break;
		case Reject:
			show(selection, ApproveEventRpcRequest.Operation.REJECT);
			break;
		case Cancel:
			show(selection, ApproveEventRpcRequest.Operation.CANCEL);
			break;
		case Inquire:
			show(selection, ApproveEventRpcRequest.Operation.INQUIRE);
			break;
		}
	}
}
