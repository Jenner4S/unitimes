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
package org.unitime.timetable.gwt.client.curricula;

import java.util.Collection;
import java.util.TreeSet;

import org.unitime.timetable.gwt.client.widgets.CourseFinder;
import org.unitime.timetable.gwt.client.widgets.CourseFinderClasses;
import org.unitime.timetable.gwt.client.widgets.CourseFinderCourses;
import org.unitime.timetable.gwt.client.widgets.CourseFinderDetails;
import org.unitime.timetable.gwt.client.widgets.CourseFinderDialog;
import org.unitime.timetable.gwt.client.widgets.CourseFinderFactory;
import org.unitime.timetable.gwt.client.widgets.CourseSelectionSuggestBox;
import org.unitime.timetable.gwt.client.widgets.DataProvider;
import org.unitime.timetable.gwt.services.CurriculaService;
import org.unitime.timetable.gwt.services.CurriculaServiceAsync;
import org.unitime.timetable.gwt.shared.CurriculumInterface;
import org.unitime.timetable.gwt.shared.CurriculumInterface.CourseInterface;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.ClassAssignment;
import org.unitime.timetable.gwt.shared.ClassAssignmentInterface.CourseAssignment;
import org.unitime.timetable.gwt.shared.CourseRequestInterface.RequestedCourse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Tomas Muller
 */
public class CurriculaCourseSelectionBox extends CourseSelectionSuggestBox {
	private final CurriculaServiceAsync iCurriculaService = GWT.create(CurriculaService.class);
	
	public CurriculaCourseSelectionBox() {
		this(true, false);
	}
		
	protected CurriculaCourseSelectionBox(final boolean includeNotOffered, final boolean checkDepartment) {
		setCourseFinderFactory(new CourseFinderFactory() {
			@Override
			public CourseFinder createCourseFinder() {
				CourseFinder finder = new CourseFinderDialog();
				CourseFinderCourses courses = new CourseFinderCourses();
				courses.setDataProvider(new DataProvider<String, Collection<CourseAssignment>>() {
					@Override
					public void getData(String source, AsyncCallback<Collection<CourseAssignment>> callback) {
						iCurriculaService.listCourseOfferings(source, null, includeNotOffered, checkDepartment, callback);
					}
				});
				CourseFinderDetails details = new CourseFinderDetails();
				details.setDataProvider(new DataProvider<CourseAssignment, String>() {
					@Override
					public void getData(CourseAssignment source, AsyncCallback<String> callback) {
						iCurriculaService.retrieveCourseDetails(source.hasUniqueName() ? source.getCourseName() : source.getCourseNameWithTitle(), callback);
					}
				});
				CourseFinderClasses classes = new CourseFinderClasses(false);
				classes.setDataProvider(new DataProvider<CourseAssignment, Collection<ClassAssignment>>() {
					@Override
					public void getData(CourseAssignment source, AsyncCallback<Collection<ClassAssignment>> callback) {
						iCurriculaService.listClasses(source.hasUniqueName() ? source.getCourseName() : source.getCourseNameWithTitle(), callback);
					}
				});
				CourseFinderCurricula curricula = new CourseFinderCurricula();
				curricula.setDataProvider(new DataProvider<CourseAssignment, TreeSet<CurriculumInterface>>() {
					@Override
					public void getData(CourseAssignment source, AsyncCallback<TreeSet<CurriculumInterface>> callback) {
						iCurriculaService.findCurriculaForACourse(source.hasUniqueName() ? source.getCourseName() : source.getCourseNameWithTitle(), callback);
					}
				});
				courses.setCourseDetails(details, classes, curricula);				
				finder.setTabs(courses);
				return finder;
			}
		});
				

		setSuggestions(new DataProvider<String, Collection<CourseAssignment>>() {
			@Override
			public void getData(String source, AsyncCallback<Collection<CourseAssignment>> callback) {
				iCurriculaService.listCourseOfferings(source, 20, includeNotOffered, checkDepartment, callback);
			}
		});
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			iFinderButton.setTabIndex(0);
			iFinderButton.setVisible(true);
			iSuggest.getElement().getStyle().clearBackgroundColor();
			iSuggest.getElement().getStyle().clearBorderColor();
		} else {
			iFinderButton.setTabIndex(-1);
			iFinderButton.setVisible(false);
			iSuggest.getElement().getStyle().setBorderColor("transparent");
            iSuggest.getElement().getStyle().setBackgroundColor("transparent");
		}
	}
	
	public void setValue(CourseInterface course, boolean fireEvents) {
		RequestedCourse rc = new RequestedCourse(); rc.setCourseId(course.getId()); rc.setCourseName(course.getCourseName());
		setValue(rc, fireEvents);
	}
	
	public void setValue(CourseInterface course) {
		setValue(course, false);
	}
	
	public void setValue(String course, boolean fireEvents) {
		RequestedCourse rc = new RequestedCourse(); rc.setCourseName(course);
		setValue(rc, fireEvents);
	}
	
	public void setValue(String course) {
		setValue(course, false);
	}
}
