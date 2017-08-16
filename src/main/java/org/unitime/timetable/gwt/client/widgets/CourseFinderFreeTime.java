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
package org.unitime.timetable.gwt.client.widgets;

import java.util.List;

import org.unitime.timetable.gwt.client.ToolBox;
import org.unitime.timetable.gwt.client.aria.AriaStatus;
import org.unitime.timetable.gwt.client.widgets.CourseFinder.CourseFinderCourseDetails;
import org.unitime.timetable.gwt.client.widgets.CourseFinder.ResponseEvent;
import org.unitime.timetable.gwt.client.widgets.CourseFinder.ResponseHandler;
import org.unitime.timetable.gwt.resources.GwtAriaMessages;
import org.unitime.timetable.gwt.resources.StudentSectioningConstants;
import org.unitime.timetable.gwt.resources.StudentSectioningMessages;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.FreeTime;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;

/**
 * @author Tomas Muller
 */
public class CourseFinderFreeTime extends P implements CourseFinder.CourseFinderTab<List<CourseRequestInterface.FreeTime>> {
	protected static final StudentSectioningConstants CONSTANTS = GWT.create(StudentSectioningConstants.class);
	protected static final StudentSectioningMessages MESSAGES = GWT.create(StudentSectioningMessages.class);
	protected static final GwtAriaMessages ARIA = GWT.create(GwtAriaMessages.class);
	
	private FreeTimePicker iFreeTimePicker;
	private Label iFreeTimeError, iFreeTimeTip;
	private FreeTimeParser iDataProvider;
	
	public CourseFinderFreeTime() {
		super("free-times");
		iFreeTimePicker = new FreeTimePicker();
		iFreeTimePicker.addValueChangeHandler(new ValueChangeHandler<List<FreeTime>>() {
			@Override
			public void onValueChange(ValueChangeEvent<List<FreeTime>> event) {
				ValueChangeEvent.fire(CourseFinderFreeTime.this, new RequestedCourse(event.getValue()));
				iFreeTimeError.setVisible(false);
			}
		});
		ScrollPanel panel = new ScrollPanel(iFreeTimePicker);
		panel.setStyleName("unitime-ScrollPanel-inner");
		panel.addStyleName("free-time-picker");
		add(panel);

		iFreeTimeTip = new Label(CONSTANTS.freeTimeTips()[(int)(Math.random() * CONSTANTS.freeTimeTips().length)]);
		iFreeTimeTip.setStyleName("unitime-Hint");
		ToolBox.disableTextSelectInternal(iFreeTimeTip.getElement());
		iFreeTimeTip.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				String oldText = iFreeTimeTip.getText();
				do {
					iFreeTimeTip.setText(CONSTANTS.freeTimeTips()[(int)(Math.random() * CONSTANTS.freeTimeTips().length)]);
				} while (oldText.equals(iFreeTimeTip.getText()));
			}
		});
		add(iFreeTimeTip);

		iFreeTimeError = new Label();
		iFreeTimeError.setStyleName("unitime-ErrorMessage");
		iFreeTimeError.setVisible(false);
		add(iFreeTimeError);
	}

	@Override
	public RequestedCourse getValue() {
		List<CourseRequestInterface.FreeTime> ret = iFreeTimePicker.getValue();
		if (ret == null || ret.isEmpty()) return null;
		return new RequestedCourse(ret);
	}
	
	@Override
	public void setValue(RequestedCourse value) {
		setValue(value, false);
	}

	@Override
	public void setValue(RequestedCourse value, final boolean fireEvents) {
		if (value == null) {
			iFreeTimePicker.setValue(null);
			iFreeTimeError.setText(MESSAGES.courseSelectionNoFreeTime());
			iFreeTimeError.setVisible(true);
		} else if (value.isFreeTime()) {
			iFreeTimePicker.setValue(value.getFreeTime(), fireEvents);
			iFreeTimeError.setVisible(false);
			String status = "";
			for (CourseRequestInterface.FreeTime ft: value.getFreeTime()) {
				status += ft.toAriaString(CONSTANTS.longDays(), CONSTANTS.useAmPm()) + " ";
			}
			if (!status.isEmpty())
				AriaStatus.getInstance().setText(ARIA.courseFinderSelectedFreeTime(status));
			ResponseEvent.fire(CourseFinderFreeTime.this, true);
		} else if (value.isCourse()) {
			iDataProvider.getData(value.getCourseName(), new AsyncCallback<List<FreeTime>>() {
				@Override
				public void onSuccess(List<FreeTime> freeTimes) {
					iFreeTimePicker.setValue(freeTimes, fireEvents);
					iFreeTimeError.setVisible(false);
					String status = "";
					for (CourseRequestInterface.FreeTime ft: freeTimes) {
						status += ft.toAriaString(CONSTANTS.longDays(), CONSTANTS.useAmPm()) + " ";
					}
					if (!status.isEmpty())
						AriaStatus.getInstance().setText(ARIA.courseFinderSelectedFreeTime(status));
					ResponseEvent.fire(CourseFinderFreeTime.this, true);
				}
				
				@Override
				public void onFailure(Throwable caught) {
					iFreeTimePicker.setValue(null);
					iFreeTimeError.setText(caught.getMessage());
					iFreeTimeError.setVisible(true);
					ResponseEvent.fire(CourseFinderFreeTime.this, false);
				}
			});
		}
	}
	
	@Override
	public HandlerRegistration addValueChangeHandler(ValueChangeHandler<RequestedCourse> handler) {
		return addHandler(handler, ValueChangeEvent.getType());
	}

	@Override
	public HandlerRegistration addSelectionHandler(SelectionHandler<RequestedCourse> handler) {
		return addHandler(handler, SelectionEvent.getType());
	}

	@Override
	public String getName() {
		return MESSAGES.courseSelectionFreeTime();
	}

	@Override
	public void setDataProvider(DataProvider<String, List<FreeTime>> provider) {
		iDataProvider = (FreeTimeParser)provider;
	}

	@Override
	public boolean isCourseSelection() {
		return false;
	}

	@Override
	public void setCourseDetails(CourseFinderCourseDetails... details) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
	}

	@Override
	public HandlerRegistration addResponseHandler(ResponseHandler handler) {
		return addHandler(handler, ResponseEvent.getType());
	}
	
	@Override
	public void changeTip() {
		iFreeTimeTip.setText(CONSTANTS.freeTimeTips()[(int)(Math.random() * CONSTANTS.freeTimeTips().length)]);
	}
}
