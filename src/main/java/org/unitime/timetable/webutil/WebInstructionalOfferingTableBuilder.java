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
package org.unitime.timetable.webutil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import javax.servlet.jsp.JspWriter;

import org.springframework.web.util.HtmlUtils;
import org.unitime.commons.Debug;
import org.unitime.commons.web.htmlgen.TableCell;
import org.unitime.commons.web.htmlgen.TableHeaderCell;
import org.unitime.commons.web.htmlgen.TableRow;
import org.unitime.commons.web.htmlgen.TableStream;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.defaults.CommonValues;
import org.unitime.timetable.defaults.UserProperty;
import org.unitime.timetable.form.InstructionalOfferingListForm;
import org.unitime.timetable.form.InstructionalOfferingListFormInterface;
import org.unitime.timetable.gwt.resources.GwtConstants;
import org.unitime.timetable.interfaces.RoomAvailabilityInterface.TimeBlock;
import org.unitime.timetable.model.Assignment;
import org.unitime.timetable.model.BuildingPref;
import org.unitime.timetable.model.ClassDurationType;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.CourseOffering;
import org.unitime.timetable.model.DatePattern;
import org.unitime.timetable.model.DatePatternPref;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DistributionPref;
import org.unitime.timetable.model.Exam;
import org.unitime.timetable.model.ExamOwner;
import org.unitime.timetable.model.ExamType;
import org.unitime.timetable.model.InstrOfferingConfig;
import org.unitime.timetable.model.InstructionalMethod;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.InstructorAttributePref;
import org.unitime.timetable.model.InstructorPref;
import org.unitime.timetable.model.Location;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.model.PreferenceGroup;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.model.RoomFeaturePref;
import org.unitime.timetable.model.RoomGroupPref;
import org.unitime.timetable.model.RoomPref;
import org.unitime.timetable.model.SchedulingSubpart;
import org.unitime.timetable.model.SectioningInfo;
import org.unitime.timetable.model.StudentClassEnrollment;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimePref;
import org.unitime.timetable.model.comparators.ClassComparator;
import org.unitime.timetable.model.comparators.ClassCourseComparator;
import org.unitime.timetable.model.comparators.InstrOfferingConfigComparator;
import org.unitime.timetable.model.comparators.SchedulingSubpartComparator;
import org.unitime.timetable.model.dao.InstructionalOfferingDAO;
import org.unitime.timetable.model.dao.SubjectAreaDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.solver.CachedClassAssignmentProxy;
import org.unitime.timetable.solver.ClassAssignmentProxy;
import org.unitime.timetable.solver.exam.ExamAssignmentProxy;
import org.unitime.timetable.solver.exam.ui.ExamAssignment;
import org.unitime.timetable.solver.ui.AssignmentPreferenceInfo;
import org.unitime.timetable.util.Formats;


/**
 * @author Stephanie Schluttenhofer, Tomas Muller
 */
public class WebInstructionalOfferingTableBuilder {
	protected static GwtConstants CONSTANTS = Localization.create(GwtConstants.class);
	protected static CourseMessages MSG = Localization.create(CourseMessages.class);
	protected static Formats.Format<Date> sDateFormat = Formats.getDateFormat(Formats.Pattern.DATE_EVENT_SHORT);
	protected static DecimalFormat sRoomRatioFormat = new DecimalFormat("0.00");

	protected static int indent = 20;
    protected static String oddRowBGColor = "#f1f3f9";
    protected static String oddRowBGColorChild = "#EFEFEF";
    protected static String oddRowMouseOverBGColor = "#8EACD0";
    protected static String evenRowMouseOverBGColor = "#8EACD0";
    protected String disabledColor = "gray";
    protected static String formName = "instructionalOfferingListForm";
    
    //available columns for table
    protected static String LABEL = "&nbsp;";
    
    protected String[] COLUMNS = {LABEL,
            								MSG.columnTitle(),
    										MSG.columnExternalId(),
    										MSG.columnDemand(),
    										MSG.columnProjectedDemand(),
    										MSG.columnConsent(),
    										MSG.columnMinPerWk(),
    										MSG.columnLimit(),
    										MSG.columnSnapshotLimit(),
    										MSG.columnRoomRatio(),
    										MSG.columnManager(),
    										MSG.columnDatePattern(),
    										MSG.columnTimePattern(),
    										MSG.columnPreferences(),
    										MSG.columnTeachingLoad(),
    										MSG.columnInstructor(),
    										MSG.columnTimetable(),
    										MSG.columnOfferingCredit(),
    										MSG.columnSubpartCredit(),
    										MSG.columnSchedulePrintNote(),
    										MSG.columnNote(),
    										MSG.columnExam()};
    
    @Deprecated
    protected String[] TIMETABLE_COLUMN_ORDER = {
    		MSG.columnAssignedTime(),
			MSG.columnAssignedRoom(),
			MSG.columnAssignedRoomCapacity()};
    
    private boolean showLabel;
    private boolean showDivSec;
    private boolean showDemand;
    private boolean showProjectedDemand;
    private boolean showMinPerWk;
    private boolean showLimit;
    private boolean showSnapshotLimit;
    private boolean showRoomRatio;
    private boolean showManager;
    private boolean showDatePattern;
    private boolean showTimePattern;
    private boolean showPreferences;
    private boolean showInstructor;
    private boolean showTimetable;
    private boolean showCredit;
    private boolean showSubpartCredit;
    private boolean showSchedulePrintNote;
    private boolean showNote;
    private boolean showTitle;
    private boolean showConsent;
    private boolean showExam;
    private boolean showExamName=true;
    private boolean showExamTimetable;
    private boolean showInstructorAssignment;
    
    private boolean iDisplayDistributionPrefs = true;
    private boolean iDisplayTimetable = true;
    private boolean iDisplayConflicts = false;
    private boolean iDisplayInstructorPrefs = true;
    
    private String iBackType = null;
    private String iBackId = null;
    
    private Comparator iClassComparator = new ClassComparator(ClassComparator.COMPARE_BY_ITYPE);
    
    // Set whether edit/modify config buttons are displayed
    private boolean displayConfigOpButtons = false;
    
    public void setDisplayConfigOpButtons(boolean displayConfigOpButtons) {
        this.displayConfigOpButtons = displayConfigOpButtons;
    }
    public boolean getDisplayConfigOpButtons() {
        return this.displayConfigOpButtons;
    }
    
    public void setDisplayDistributionPrefs(boolean displayDistributionPrefs) {
    	iDisplayDistributionPrefs = displayDistributionPrefs;
    }
    public boolean getDisplayDistributionPrefs() { return iDisplayDistributionPrefs; }
    public void setDisplayInstructorPrefs(boolean displayInstructorPrefs) {
    	iDisplayInstructorPrefs = displayInstructorPrefs;
    }
    public boolean getDisplayInstructorPrefs() { return iDisplayInstructorPrefs && isShowInstructorAssignment(); }
    public void setDisplayTimetable(boolean displayTimetable) {
    	iDisplayTimetable = displayTimetable;
    }
    public boolean getDisplayTimetable() { return iDisplayTimetable; }
    
    public void setDisplayConflicts(boolean displayConflicts) {
    	iDisplayConflicts = displayConflicts;
    }
    public boolean getDisplayConflicts() { return iDisplayConflicts; }
    
    private boolean iTimeVertical = false;
    public void setTimeVertival(boolean timeVertical) {
    	iTimeVertical = timeVertical;
    }
    public boolean getTimeVertival() {
    	return iTimeVertical;
    }
    private boolean iGridAsText = false;
    public void setGridAsText(boolean gridAsText) {
    	iGridAsText = gridAsText;
    }
    public boolean getGridAsText() {
    	return iGridAsText;
    }
    public String iInstructorNameFormat = "last-first";
    public void setInstructorNameFormat(String instructorNameFormat) {
    	iInstructorNameFormat = instructorNameFormat;
    }
    public String getInstructorNameFormat() {
    	return iInstructorNameFormat;
    }
    private Boolean iHighlightClassPrefs = null;
    public void setHighlightClassPrefs(boolean highlightClassPrefs) {
    	iHighlightClassPrefs = highlightClassPrefs;
    }
    public boolean isHighlightClassPrefs() {
    	if (iHighlightClassPrefs == null) return ApplicationProperty.PreferencesHighlighClassPreferences.isTrue();
    	return iHighlightClassPrefs;
    }
    public String iDefaultTimeGridSize = null;
    public void setDefaultTimeGridSize(String defaultTimeGridSize) {
    	iDefaultTimeGridSize = defaultTimeGridSize;
    }
    public String getDefaultTimeGridSize() {
    	return iDefaultTimeGridSize;
    }
    public int getPreferenceColumns() {
    	if (isShowPreferences())
    		return 2 + (getDisplayDistributionPrefs() ? 1 : 0) + (getDisplayInstructorPrefs() ? 2 : 0);
    	else
    		return (getDisplayInstructorPrefs() ? 2 : 0);
    }
     
    public void setUserSettings(UserContext user) {
		setTimeVertival(RequiredTimeTable.getTimeGridVertical(user));
		setGridAsText(RequiredTimeTable.getTimeGridAsText(user));
		setInstructorNameFormat(UserProperty.NameFormat.get(user));
		setDefaultTimeGridSize(RequiredTimeTable.getTimeGridSize(user));
		String highlighClassPreferences = UserProperty.HighlighClassPreferences.get(user);
		if (CommonValues.Yes.eq(highlighClassPreferences))
			setHighlightClassPrefs(true);
		else if (CommonValues.No.eq(highlighClassPreferences))
			setHighlightClassPrefs(false);
		else
			setHighlightClassPrefs(ApplicationProperty.PreferencesHighlighClassPreferences.isTrue());
    }
    
    public boolean isShowConsent() {
        return showConsent;
    }
    public void setShowConsent(boolean showConsent) {
        this.showConsent = showConsent;
    }
    
    public boolean isShowTitle() {
        return showTitle;
    }
    public void setShowTitle(boolean showTitle) {
        this.showTitle = showTitle;
    }

    public boolean isShowExam() {
        return showExam;
    }
    public void setShowExam(boolean showExam) {
        this.showExam = showExam;
    }
    
    public boolean isShowExamName() {
        return showExamName;
    }
    public void setShowExamName(boolean showExamName) {
        this.showExamName = showExamName;
    }
    public boolean isShowExamTimetable() {
        return showExamTimetable;
    }
    public void setShowExamTimetable(boolean showExamTimetable) {
        this.showExamTimetable = showExamTimetable;
    }
    
    public boolean isShowInstructorAssignment() { return showInstructorAssignment; }
    public void setShowInstructorAssignment(boolean showInstructorAssignment) { this.showInstructorAssignment = showInstructorAssignment; }

    /**
     * 
     */
    public WebInstructionalOfferingTableBuilder() {
        super();
    }
    
    protected String getRowMouseOver(boolean isHeaderRow, boolean isControl){
        return ("this.style.backgroundColor='" 
                + (isHeaderRow ?oddRowMouseOverBGColor:evenRowMouseOverBGColor) 
                + "';this.style.cursor='"
                + (isControl ? "hand" : "default")
                + "';this.style.cursor='"
                + (isControl ? "pointer" : "default")+ "';");
   	
    }
    
    protected String getRowMouseOut(boolean isHeaderRow){
        return ("this.style.backgroundColor='"  + (isHeaderRow ?oddRowBGColor:"transparent") + "';");   	
    }

    protected String getRowMouseOut(boolean isHeaderRow, int ct){        
        return ( "this.style.backgroundColor='"  
                + (isHeaderRow 
                        ? oddRowBGColor 
                        : ( (ct%2==1) 
                                ? oddRowBGColorChild 
                                : "transparent") ) 
                + "';");   	
    }
    
    protected TableRow initRow(boolean isHeaderRow){
        TableRow row = new TableRow();
        if (isHeaderRow){
        	row.setBgColor(oddRowBGColor);
        }
        return (row);
    }
    
    protected TableHeaderCell headerCell(String content, int rowSpan, int colSpan){
    	TableHeaderCell cell = new TableHeaderCell();
    	cell.setRowSpan(rowSpan);
    	cell.setColSpan(colSpan);
    	cell.setAlign("left");
    	cell.setValign("bottom");
    	cell.addContent("<font size=\"-1\">");
    	cell.addContent(content);
    	cell.addContent("</font>");
		cell.setStyleClass("WebTableHeader");
    	return(cell);
     }
    
    private TableCell initCell(boolean isEditable, String onClick, int cols){
        return (initCell(isEditable, onClick, cols, false));
    }

    private TableCell initCell(boolean isEditable, String onClick, int cols, boolean nowrap){
        TableCell cell = new TableCell();
        cell.setValign("top");
        if (cols > 1){
            cell.setColSpan(cols);
        }
        if (nowrap){
            cell.setNoWrap(true);
        }
        if (onClick != null && onClick.length() > 0){
        	cell.setOnClick(onClick);
        }
        if (!isEditable){
        	cell.addContent("<span style=\"color:" + disabledColor + ";\">");
        }
        return (cell);
    }

    private void endCell(TableCell cell, boolean isEditable){
        if (!isEditable){
            cell.addContent("</span>");
        }   
    }
   
    protected TableCell initNormalCell(String text, boolean isEditable){
        return (initColSpanCell(text, isEditable, 1));
    }
    
    private TableCell initColSpanCell(String text, boolean isEditable, int cols){
        TableCell cell = initCell(isEditable, null, cols);
        cell.addContent(text);
        endCell(cell, isEditable);
        return (cell);
        
    } 
    //MSG.columnNote(): if changing column order column order must be changed in
    //		buildTableHeader, addInstrOffrRowsToTable, buildClassOrSubpartRow, and buildConfigRow
    protected void buildTableHeader(TableStream table, Long sessionId, String durationColName){  
    	TableRow row = new TableRow();
    	TableRow row2 = new TableRow();
    	TableHeaderCell cell = null;
    	if (isShowLabel()){
    		cell = this.headerCell(LABEL, 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowDivSec()){
    		cell = this.headerCell(MSG.columnExternalId(), 2, 1);
    		row.addContent(cell);
    	}   	
    	if (isShowDemand()){
    		if (StudentClassEnrollment.sessionHasEnrollments(sessionId)){
        		cell = this.headerCell(MSG.columnDemand(), 2, 1);
    		} else {
        		cell = this.headerCell((MSG.columnLastDemand()), 2, 1);    			
    		}
    		row.addContent(cell);
    	}
    	if (isShowProjectedDemand()){
    		cell = this.headerCell(MSG.columnProjectedDemand(), 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowLimit()){
    		cell = this.headerCell(MSG.columnLimit(), 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowSnapshotLimit()){
    		cell = this.headerCell(MSG.columnSnapshotLimit(), 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowRoomRatio()){
    		cell = this.headerCell(MSG.columnRoomRatio(), 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowManager()){
    		cell = this.headerCell(MSG.columnManager(), 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowDatePattern()){
    		cell = this.headerCell(MSG.columnDatePattern(), 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowMinPerWk()){
    		cell = this.headerCell(durationColName, 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowTimePattern()){
    		cell = this.headerCell(MSG.columnTimePattern(), 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowPreferences()){
    		cell = headerCell("----" + MSG.columnPreferences() + "----", 1, getPreferenceColumns());
    		cell.setStyleClass("WebTableHeaderFirstRow");
    		cell.setAlign("center");
	    	row.addContent(cell);
	    	cell = headerCell(MSG.columnTimePref(), 1, 1);
    		cell.setStyleClass("WebTableHeaderSecondRow");
    		row2.addContent(cell);
    		cell = headerCell(MSG.columnAllRoomPref(), 1, 1);
    		cell.setStyleClass("WebTableHeaderSecondRow");
    		row2.addContent(cell);
    		if (getDisplayDistributionPrefs()) {
    			cell = headerCell(MSG.columnDistributionPref(), 1, 1);
        		cell.setStyleClass("WebTableHeaderSecondRow");
        		row2.addContent(cell);
    		}
    		if (getDisplayInstructorPrefs()) {
    			cell = headerCell(MSG.columnInstructorAttributePref(), 1, 1);
        		cell.setStyleClass("WebTableHeaderSecondRow");
        		row2.addContent(cell);
    			cell = headerCell(MSG.columnInstructorPref(), 1, 1);
        		cell.setStyleClass("WebTableHeaderSecondRow");
        		row2.addContent(cell);
    		}
    	} else if (getDisplayInstructorPrefs()) {
    		cell = headerCell("----" + MSG.columnPreferences() + "----", 1, getPreferenceColumns());
    		cell.setStyleClass("WebTableHeaderFirstRow");
    		cell.setAlign("center");
	    	row.addContent(cell);
			cell = headerCell(MSG.columnInstructorAttributePref(), 1, 1);
    		cell.setStyleClass("WebTableHeaderSecondRow");
    		row2.addContent(cell);
			cell = headerCell(MSG.columnInstructorPref(), 1, 1);
    		cell.setStyleClass("WebTableHeaderSecondRow");
    		row2.addContent(cell);    		
    	}
    	if (isShowInstructorAssignment()) {
    		cell = this.headerCell(MSG.columnTeachingLoad(), 2, 1);
    		row.addContent(cell);
    	}
    	if (isShowInstructor()){
    		cell = this.headerCell(MSG.columnInstructor(), 2, 1);
    		row.addContent(cell);    		
    	}
    	if (getDisplayTimetable() && isShowTimetable()){
	    	cell = headerCell("--------" + MSG.columnTimetable() + "--------", 1, 3);
	    	cell.setStyleClass("WebTableHeaderFirstRow");
    		cell.setAlign("center");
    		row.addContent(cell);
    		cell = headerCell(MSG.columnAssignedTime(), 1, 1);
    		cell.setNoWrap(true);
    		cell.setStyleClass("WebTableHeaderSecondRow");
    		row2.addContent(cell);
    		cell = headerCell(MSG.columnAssignedRoom(), 1, 1);
    		cell.setNoWrap(true);
    		cell.setStyleClass("WebTableHeaderSecondRow");
    		row2.addContent(cell);
    		cell = headerCell(MSG.columnAssignedRoomCapacity(), 1, 1);
    		cell.setNoWrap(true);
    		cell.setStyleClass("WebTableHeaderSecondRow");
    		row2.addContent(cell);
    	}
    	if (isShowTitle()){
    		cell = this.headerCell(MSG.columnTitle(), 2, 1);
    		row.addContent(cell);    		
    	}
    	if (isShowCredit()){
    		cell = this.headerCell(MSG.columnOfferingCredit(), 2, 1);
    		row.addContent(cell);    		
    	}
    	if (isShowSubpartCredit()){
    		cell = this.headerCell(MSG.columnSubpartCredit(), 2, 1);
    		row.addContent(cell);    		
    	}
    	if (isShowConsent()){
    		cell = this.headerCell(MSG.columnConsent(), 2, 1);
    		row.addContent(cell);    		
    	}
    	if (isShowSchedulePrintNote()){
    		cell = this.headerCell(this.getSchedulePrintNoteLabel(), 2, 1);
    		row.addContent(cell);    		
    	}
    	if (isShowNote()){
    		cell = this.headerCell(MSG.columnNote(), 2, 1);
    		row.addContent(cell);    		
    	}
    	if (isShowExam()) {
    		cell = headerCell("-----------" + MSG.columnExam() + "--------", 1, (isShowExamName()?1:0)+(isShowExamTimetable()?2:0));
	    	cell.setStyleClass("WebTableHeaderFirstRow");
    		cell.setAlign("center");
    		cell.setNoWrap(true);
            row.addContent(cell);
            if (isShowExamName()) {
                cell = headerCell(MSG.columnExamName(), 1, 1);
                cell.setNoWrap(true);
	    		cell.setStyleClass("WebTableHeaderSecondRow");
                row2.addContent(cell);
            }
            if (isShowExamTimetable()) {
                cell = headerCell(MSG.columnExamPeriod(), 1, 1);
                cell.setNoWrap(true);
	    		cell.setStyleClass("WebTableHeaderSecondRow");
                row2.addContent(cell);
                cell = headerCell(MSG.columnExamRoom(), 1, 1);
                cell.setNoWrap(true);
	    		cell.setStyleClass("WebTableHeaderSecondRow");
                row2.addContent(cell);
                
            }
    	}
    	table.addContent(row);
    	table.addContent(row2);
   }
    
    protected String getSchedulePrintNoteLabel(){
    	return(MSG.columnSchedulePrintNote());
    }

    private String subjectOnClickAction(Long instrOfferingId){
        return("document.location='instructionalOfferingDetail.do?op=view&io=" + instrOfferingId + "';");
    }
    
    private TableCell subjectAndCourseInfo(InstructionalOffering io, CourseOffering co, boolean isEditable) {
        TableCell cell = this.initCell(isEditable && co.isIsControl().booleanValue(), null, 1, true);
    	if ("InstructionalOffering".equals(getBackType()) && io.getUniqueId().toString().equals(getBackId()))
    		cell.addContent("<A name=\"back\"></A>");
    	if ("PreferenceGroup".equals(getBackType())) {
    		for (Iterator i=io.getInstrOfferingConfigs().iterator();i.hasNext();) {
    			InstrOfferingConfig ioc = (InstrOfferingConfig)i.next();
    			for (Iterator j=ioc.getSchedulingSubparts().iterator();j.hasNext();) {
    				SchedulingSubpart ss = (SchedulingSubpart)j.next();
    	         	if (ss.getUniqueId().toString().equals(getBackId())) cell.addContent("<A name=\"back\"></A>");
    	         	for (Iterator k=ss.getClasses().iterator();k.hasNext();) {
    	         		Class_ c = (Class_)k.next();
    	         		if (c.getUniqueId().toString().equals(getBackId())) cell.addContent("<A name=\"back\"></A>");
    	         	}
    			}
    		}
    	}
        cell.addContent("<A name=\"A" + io.getUniqueId().toString() + "\"></A>");
        cell.addContent("<A name=\"A" + co.getUniqueId().toString() + "\"></A>");
        cell.addContent(co != null? ("<span title='" + co.getCourseNameWithTitle() + "'><b>" + co.getSubjectAreaAbbv() + "</b>") :"");
        cell.addContent(" ");
        cell.addContent(co!= null? ("<b>" + co.getCourseNbr() + "</b></span>") :"");
        InstructionalMethod im = (co != null && co.getInstructionalOffering().getInstrOfferingConfigs().size() == 1 ? co.getInstructionalOffering().getInstrOfferingConfigs().iterator().next().getInstructionalMethod() : null);
        if (co != null && co.getCourseType() != null) {
        	if (im != null) {
        		cell.addContent(" (<span title='" + co.getCourseType().getLabel() + "'>" + co.getCourseType().getReference() + "</span>, <span title='" + im.getLabel() + "'>" + im.getReference() + ")");
        	} else {
        		cell.addContent(" (<span title='" + co.getCourseType().getLabel() + "'>" + co.getCourseType().getReference() + "</span>)");
        	}
        } else if (im != null) {
        	cell.addContent(" (<span title='" + im.getLabel() + "'>" + im.getReference() + ")");
        }
        Iterator it = io.courseOfferingsMinusSortCourseOfferingForSubjectArea(co.getSubjectArea().getUniqueId()).iterator();
        StringBuffer addlCos = new StringBuffer();
        CourseOffering tempCo = null;
        addlCos.append("<font color='"+disabledColor+"'>");
        while(it.hasNext()){
            tempCo = (org.unitime.timetable.model.CourseOffering) it.next();
            addlCos.append("<br>"); 
            //addlCos.append("<A href=\"courseOfferingEdit.do?co=" + tempCo.getUniqueId() + "\">");
            addlCos.append("<span title='" + tempCo.getCourseNameWithTitle() + "' style='padding-left: " + indent + "px;'>");
            addlCos.append(tempCo.getSubjectAreaAbbv());
            addlCos.append(" ");
            addlCos.append(tempCo.getCourseNbr());
            if (tempCo != null && tempCo.getCourseType() != null) addlCos.append(" (<span title='" + tempCo.getCourseType().getLabel() + "'>" + tempCo.getCourseType().getReference() + "</span>)");
            addlCos.append("</span>");
            //addlCos.append("</A>");
        }
        addlCos.append("</font>");
        if (tempCo != null){
            cell.addContent(addlCos.toString());
        }
        this.endCell(cell, co.isIsControl().booleanValue());
        return (cell);
    }  
    
    protected TableCell buildPrefGroupLabel(CourseOffering co, PreferenceGroup prefGroup, int indentSpaces, boolean isEditable, String prevLabel, String icon){
    	TableCell cell = initNormalCell("", isEditable);
    	if (indentSpaces > 0) {
    		int pad = indentSpaces * indent;
    		if (icon != null) pad -= indent;
    		cell.setStyle("padding-left: " + pad + "px;");
    	}
        if (icon != null) cell.addContent(icon);
    	if(!isEditable){
    		cell.addContent("<font color='"+disabledColor+"'>");
    	}
        cell.addContent("<A name=\"A" + prefGroup.getUniqueId().toString() + "\"></A>");
        String label = prefGroup.htmlLabel();
        if (prefGroup instanceof Class_) {
			Class_ aClass = (Class_) prefGroup;
			if (!aClass.isEnabledForStudentScheduling().booleanValue()){
				cell.setTitle(MSG.tooltipDisabledForStudentScheduling(aClass.getClassLabelWithTitle(co)));
				label = "<i>" + label + "</i>";
			} else {
				cell.setTitle(aClass.getClassLabelWithTitle(co));
			}
		}
        if (prevLabel != null && label.equals(prevLabel)){
        	label = " &nbsp;";
        }
        cell.addContent(label);
        cell.setNoWrap(true);
        if(!isEditable){
        	cell.addContent("</font>");
        }
        return(cell);
    }
    
    protected TableCell buildDatePatternCell(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable){
    	Assignment a = null;
    	AssignmentPreferenceInfo p = null;
		if (getDisplayTimetable() && isShowTimetable() && classAssignment!=null && prefGroup instanceof Class_) {
			try {
				a = classAssignment.getAssignment((Class_)prefGroup);
				p = classAssignment.getAssignmentInfo((Class_)prefGroup);
			} catch (Exception e) {
				Debug.error(e);
			}
    	}
    	DatePattern dp = (a != null ? a.getDatePattern() : prefGroup.effectiveDatePattern());
    	TableCell cell = null;
    	if (dp==null) {
    		cell = initNormalCell("", isEditable);
    	} else if (dp.getType() == DatePattern.sTypePatternSet && isEditable) {
    		String text = "";
    		boolean hasReq = false;
			for (Iterator i=prefGroup.effectivePreferences(DatePatternPref.class).iterator(); i.hasNext();) {
				Preference pref = (Preference)i.next();
				if (!hasReq && PreferenceLevel.sRequired.equals(pref.getPrefLevel().getPrefProlog())) {
					hasReq = true; text = "";
				}
				if (!hasReq || PreferenceLevel.sRequired.equals(pref.getPrefLevel().getPrefProlog())) {
					text +=  (text.isEmpty() ? "" : "<br>") + pref.preferenceHtml(getInstructorNameFormat(), isHighlightClassPrefs());
				}
			}
    		cell = initNormalCell("<div>"+dp.getName()+"</div>" + text, isEditable);
    	} else {
    		cell = initNormalCell("<div title='"+sDateFormat.format(dp.getStartDate())+" - "+sDateFormat.format(dp.getEndDate())+"' " +
    				(p == null || !isEditable ? "" : "style='color:" + PreferenceLevel.int2color(p.getDatePatternPref()) + ";'") +
    				">"+dp.getName()+"</div>", isEditable);
    	}
        cell.setAlign("center");
        return(cell);
    }

    private TableCell buildTimePatternCell(PreferenceGroup prefGroup, boolean isEditable){
        TableCell cell = initNormalCell(prefGroup.effectiveTimePatternHtml(), isEditable);
        cell.setAlign("center");
        cell.setNoWrap(true);
        return(cell);
    }
    
    private TableCell buildTimePrefCell(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable){
		Assignment a = null;
		if (getDisplayTimetable() && isShowTimetable() && classAssignment!=null && prefGroup instanceof Class_) {
			try {
				a = classAssignment.getAssignment((Class_)prefGroup);
			} catch (Exception e) {
				Debug.error(e);
			}
    	}
    	
    	TableCell cell = initNormalCell(prefGroup.getEffectivePrefHtmlForPrefType(a,TimePref.class, getTimeVertival(), getGridAsText(), getDefaultTimeGridSize(), isHighlightClassPrefs()),isEditable);
        cell.setNoWrap(true);
    	return (cell);
    	
    }
    
    private TableCell buildPreferenceCell(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, Class prefType, boolean isEditable){
    	if (!isEditable) return initNormalCell("",false);
    	if (TimePref.class.equals(prefType)) {
    		return(buildTimePrefCell(classAssignment,prefGroup, isEditable));
    	/*
    	} else if (InstructorAttributePref.class.equals(prefType) || InstructorPref.class.equals(prefType)) {
    		TableCell cell = this.initNormalCell("", isEditable);
    		if (prefGroup instanceof Class_) {
    			for (TeachingClassRequest cr: ((Class_)prefGroup).getTeachingRequests()) {
    				if (!cr.isAssignInstructor()) continue;
    				cell.addContent(cr.getTeachingRequest().getEffectivePrefHtmlForPrefType(prefType, getInstructorNameFormat()));
    			}
    			return cell;
    		} else {
    			return initNormalCell("", false);
    		}
    	*/
    	} else {
    		if (!prefGroup.isInstructorAssignmentNeeded() && (InstructorPref.class.equals(prefType) || InstructorAttributePref.class.equals(prefType))) {
    			return initNormalCell("",false);
    		}
    		TableCell cell = this.initNormalCell(prefGroup.getEffectivePrefHtmlForPrefType(prefType, getInstructorNameFormat(), isHighlightClassPrefs()),isEditable);
    		cell.setNoWrap(true);
    		return(cell);
    	}
    	
    }
    
    private TableCell buildPreferenceCell(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, Class[] prefTypes, boolean isEditable){
    	if (!isEditable) return initNormalCell("",false);
    	StringBuffer pref = new StringBuffer();
    	boolean noRoomPrefs = false;
    	if (prefGroup instanceof Class_ && ((Class_)prefGroup).getNbrRooms().intValue()==0) {
    		noRoomPrefs = true;
    	}
        if (prefGroup instanceof SchedulingSubpart && ((SchedulingSubpart)prefGroup).getInstrOfferingConfig().isUnlimitedEnrollment().booleanValue())
            noRoomPrefs = true;
    	for (int i=0;i<prefTypes.length;i++) {
    		Class prefType = prefTypes[i];
    		if (noRoomPrefs) {
    			if (//prefType.equals(RoomPref.class) || 
    				prefType.equals(RoomGroupPref.class) || 
    				prefType.equals(RoomFeaturePref.class) || 
    				prefType.equals(BuildingPref.class))
    				continue;
    		}
    		String x = prefGroup.getEffectivePrefHtmlForPrefType(prefType, isHighlightClassPrefs());
    		if (x!=null && x.trim().length()>0) {
    			if (pref.length()>0) pref.append("<BR>");
    			pref.append(x);
    		}
    	}
    	TableCell cell = this.initNormalCell(noRoomPrefs && pref.length()==0?"<i>N/A</i>":pref.toString(),isEditable);
    	cell.setNoWrap(true);
    	return(cell);
    }

    private TableCell buildPrefGroupDemand(PreferenceGroup prefGroup, boolean isEditable){
    	if (prefGroup instanceof Class_) {
			Class_ c = (Class_) prefGroup;
			if (StudentClassEnrollment.sessionHasEnrollments(c.getSessionId())){
				TableCell tc = null;
				if (c.getEnrollment() != null){
					tc = this.initNormalCell(c.getEnrollment().toString(), isEditable);
				} else {
					tc = this.initNormalCell("0", isEditable);				
				}
				tc.setAlign("right");
				return(tc);
			}
		}
    	return(this.initNormalCell("&nbsp;", isEditable));
    }
    
    private TableCell buildPrefGroupProjectedDemand(PreferenceGroup prefGroup, boolean isEditable){
    	if (prefGroup instanceof Class_) {
    		Class_ c = (Class_)prefGroup;
    		SectioningInfo i = c.getSectioningInfo();
    		if (i != null && i.getNbrExpectedStudents() != null) {
    			TableCell cell = initNormalCell(String.valueOf(Math.round(Math.max(0.0, c.getEnrollment() + i.getNbrExpectedStudents()))), isEditable);
    			cell.setAlign("right");	
    			return cell;
    		}
    	}
    	return(this.initNormalCell("&nbsp;", isEditable));
    }
    
    private TableCell buildSnapshotLimit(PreferenceGroup prefGroup, boolean isEditable){
    	if (prefGroup instanceof Class_) {
    		Class_ c = (Class_)prefGroup;
    		if (c.getSnapshotLimit() != null) {
    			TableCell cell = null;
    			if (c.getSchedulingSubpart().getInstrOfferingConfig().isUnlimitedEnrollment()) {
    				cell = initNormalCell("<font size=\"+1\">&infin;</font>", isEditable);
    			} else {
    				cell = initNormalCell(c.getSnapshotLimit().toString(), isEditable);
    			}
    			cell.setAlign("right");	
    			return cell;
    		}
    	}
    	return(this.initNormalCell("&nbsp;", isEditable));
    }

    private TableCell buildLimit(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable){
    	TableCell cell = null;
    	boolean nowrap = false;
    	if (prefGroup instanceof SchedulingSubpart){
	    	SchedulingSubpart ss = (SchedulingSubpart) prefGroup;
	    	boolean unlimited = ss.getInstrOfferingConfig().isUnlimitedEnrollment().booleanValue();
	    	if (!unlimited) {
		    	int limit = (ss.getLimit()==null?0:ss.getLimit().intValue());
		    	int maxExpCap = ss.getMaxExpectedCapacity(); 
		    	if (limit==maxExpCap)
		    		cell = initNormalCell(String.valueOf(limit), isEditable);
		    	else {
		    		cell = initNormalCell(limit+"-"+maxExpCap, isEditable);
		    		nowrap = true;
		    	}
	    	}
	    	else {
	    	    cell = initNormalCell("&nbsp;", isEditable);	    	    
	    	}
    	} else if (prefGroup instanceof Class_){
    		Class_ aClass = (Class_) prefGroup;
	    	boolean unlimited = aClass.getSchedulingSubpart().getInstrOfferingConfig().isUnlimitedEnrollment().booleanValue();
	    	if (!unlimited) {
	    		String limitString = null;
                Assignment a = null;
                try {
                    if (classAssignment!=null) a = classAssignment.getAssignment(aClass);
                } catch (Exception e) {}
                if (a==null) {
                    if (aClass.getExpectedCapacity() != null){
                        limitString = aClass.getExpectedCapacity().toString();
                        if (aClass.getMaxExpectedCapacity() != null && !aClass.getMaxExpectedCapacity().equals(aClass.getExpectedCapacity())){
                            limitString = limitString + "-" + aClass.getMaxExpectedCapacity().toString();
                            nowrap = true;
                        }
                    } else {
                        limitString = "0";
                        if (aClass.getMaxExpectedCapacity() != null && aClass.getMaxExpectedCapacity().intValue() != 0){
                            limitString = limitString + "-" + aClass.getMaxExpectedCapacity().toString();
                            nowrap = true;
                        }
                    }
                } else {
                    limitString = ""+aClass.getClassLimit(classAssignment);
                    /*
                    if (aClass.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering().getCourseOfferings().size()>1) {
                        String title = "";
                        for (Iterator i=aClass.getSchedulingSubpart().getInstrOfferingConfig().getInstructionalOffering().getCourseOfferings().iterator();i.hasNext();) {
                            CourseOffering offering = (CourseOffering)i.next();
                            int limitThisOffering = aClass.getClassLimit(classAssignment,offering);
                            if (limitThisOffering<=0) continue;
                            if (title.length()>0) title+=", ";
                            title += limitThisOffering+" ("+offering.getCourseName()+")";
                        }
                        limitString = "<span title='"+title+"'>"+limitString+"</span>";
                    }
                    */
                }
	    		cell = initNormalCell(limitString, isEditable);
	    		if (nowrap) cell.setNoWrap(true);
	    	}
	    	else {
	    	    cell = initNormalCell("<font size=\"+1\">&infin;</font>", isEditable);	    	    
	    	}
	    		
    	} else {
    		cell = this.initNormalCell("&nbsp;" ,isEditable);
    	}
        cell.setAlign("right");	
        return(cell);
    }
    
    private TableCell buildDivisionSection(CourseOffering co, PreferenceGroup prefGroup, boolean isEditable){
    	TableCell cell = null;
    	if (prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
    		//String divSec = aClass.getDivSecNumber();
    		String divSec = aClass.getClassSuffix(co);
    		cell = initNormalCell((divSec==null?"&nbsp;":divSec), isEditable);
            cell.setAlign("right");	
    	} else {
    		cell = this.initNormalCell("&nbsp;" ,isEditable);
    	}
    	cell.setNoWrap(true);
        return(cell);
    }
    
    protected TableCell buildInstructorAssignment(PreferenceGroup prefGroup, boolean isEditable){
    	TableCell cell = this.initNormalCell("" ,isEditable);
    	if (prefGroup instanceof Class_) {
    		Class_ c = (Class_) prefGroup;
    		/*
    		for (TeachingClassRequest cr: c.getTeachingRequests()) {
    			if (!cr.isAssignInstructor()) continue;
    			cell.addContent(Formats.getNumberFormat("0.##").format(cr.getTeachingRequest().getTeachingLoad()) + " " + MSG.teachingLoadUnits()
    				+ (cr.getTeachingRequest().getResponsibility() == null ? "" : " (" + cr.getTeachingRequest().getResponsibility().getAbbreviation() + ")")
    				);
    			*/
    		if (c.isInstructorAssignmentNeeded()) {
    			cell.addContent(
    					(c.effectiveNbrInstructors() > 1 ? c.effectiveNbrInstructors() + " &times; " : "") +
    					Formats.getNumberFormat("0.##").format(c.effectiveTeachingLoad()) + " " + MSG.teachingLoadUnits());
    			cell.setAlign("right");
    			cell.setNoWrap(true);
    		} else if (c.getSchedulingSubpart().isInstructorAssignmentNeeded()) {
    			cell.addContent(MSG.cellNoInstructorAssignment());
    			cell.setAlign("right");
    		}
    	} else if (prefGroup instanceof SchedulingSubpart) {
    		SchedulingSubpart ss = (SchedulingSubpart)prefGroup;
    		if (ss.isInstructorAssignmentNeeded()) {
    			cell.addContent((ss.getNbrInstructors() > 1 ? ss.getNbrInstructors() + " &times; " : "") +
    					Formats.getNumberFormat("0.##").format(ss.getTeachingLoad()) + " " + MSG.teachingLoadUnits());
    			cell.setAlign("right");
    			cell.setNoWrap(true);
    		}
    	} else {
    		cell.addContent(" &nbsp; ");
    	}
        return(cell);
    }

    protected TableCell buildInstructor(PreferenceGroup prefGroup, boolean isEditable){
    	TableCell cell = this.initNormalCell("" ,isEditable);
    	if (prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
    		String label = aClass.instructorHtml(getInstructorNameFormat());
    		if (!aClass.isDisplayInstructor().booleanValue()){
    			label = "<i>" + label + "</i>";
    		}
    		if (!isEditable) {
    			label = "<span style=\"color:gray;\">" + label + "</span>";
    		}
    		cell.addContent(label);
            cell.setAlign("left");
    	} else {
    		cell.addContent(" &nbsp; ");
    	}
        return(cell);
    }

    private TableCell buildCredit(PreferenceGroup prefGroup, boolean isEditable){
    	TableCell cell = this.initNormalCell("" ,isEditable);
    	if (prefGroup instanceof SchedulingSubpart) {
    		SchedulingSubpart ss = (SchedulingSubpart) prefGroup;
     		if (ss.getCredit() != null) {
    			cell.addContent("<span title='"+ss.getCredit().creditText()+"'>"+ss.getCredit().creditAbbv()+"</span>");
    		} else {
    			cell.addContent(" &nbsp; ");
    		}   		
    		
            cell.setAlign("left");	
    	} else {
    		cell.addContent(" &nbsp; ");
    	}
        return(cell);
    }

    private TableCell buildSchedulePrintNote(PreferenceGroup prefGroup, boolean isEditable, UserContext user){
    	TableCell cell = null;
    	if (prefGroup instanceof Class_) {
    		Class_ c = (Class_) prefGroup;
    		if (c.getSchedulePrintNote()!=null && !c.getSchedulePrintNote().trim().isEmpty()) {
    			if (CommonValues.NoteAsFullText.eq(user.getProperty(UserProperty.SchedulePrintNoteDisplay))) {
	    			cell = initNormalCell(c.getSchedulePrintNote().replace("\n", "<br>"), isEditable);
	    			cell.setAlign("left");
    			} else if (CommonValues.NoteAsShortText.eq(user.getProperty(UserProperty.SchedulePrintNoteDisplay))) {
        			String note = (c.getSchedulePrintNote().length() <= 20 ? c.getSchedulePrintNote() : c.getSchedulePrintNote().substring(0, 20) + "...");
    				cell = initNormalCell(note.replace("\n", "<br>"), isEditable);
	    			cell.setAlign("left");
    			} else {
    	    		cell = initNormalCell("<IMG border='0' alt='" + MSG.altHasSchedulePrintNote() + "' title='" + c.getSchedulePrintNote() + "' align='absmiddle' src='images/note.png'>", isEditable);
    	    		cell.setAlign("center");
    			}
    		} else {
        		cell = this.initNormalCell("&nbsp;" ,isEditable);
        	}
    	}  else {
       		cell = this.initNormalCell("&nbsp;" ,isEditable);   		
    	}
        return(cell);
    }
    
    private TableCell buildExamName(TreeSet exams, boolean isEditable) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i=exams.iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            sb.append("<span "+(ExamType.sExamTypeFinal==exam.getExamType().getType()?"style='font-weight:bold;' ":"")+
                    "title='"+MSG.tooltipExam(exam.getLabel(), exam.getExamType().getLabel()) + "'>");
            sb.append(exam.getLabel());
            if (ExamType.sExamTypeFinal==exam.getExamType().getType()) sb.append("</span>");
            if (i.hasNext()) sb.append("<br>");
        }
        TableCell cell = this.initNormalCell(sb.toString() ,isEditable);
        cell.setAlign("left");
        cell.setNoWrap(true);
        return(cell);
    }

    private TableCell buildExamPeriod(ExamAssignmentProxy examAssignment, TreeSet exams, boolean isEditable) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i=exams.iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            sb.append("<span "+(ExamType.sExamTypeFinal==exam.getExamType().getType()?"style='font-weight:bold;' ":"")+
                    "title='"+MSG.tooltipExam(exam.getLabel(), exam.getExamType().getLabel()) + "'>");
            if (examAssignment!=null && examAssignment.getExamTypeId().equals(exam.getExamType().getUniqueId())) {
                ExamAssignment ea = examAssignment.getAssignment(exam.getUniqueId());
                if (ea==null && !isShowExamName()) continue;
                sb.append(ea==null?"":ea.getPeriodAbbreviationWithPref());
            } else {
                if (exam.getAssignedPeriod()==null && !isShowExamName()) continue;
                sb.append(exam.getAssignedPeriod()==null?"":exam.getAssignedPeriod().getAbbreviation());
            }
            if (ExamType.sExamTypeFinal==exam.getExamType().getType()) sb.append("</span>");
            if (i.hasNext()) sb.append("<br>");
        }
        TableCell cell = this.initNormalCell(sb.toString() ,isEditable);
        cell.setAlign("left");
        cell.setNoWrap(true);
        return(cell);
    }

    private TableCell buildExamRoom(ExamAssignmentProxy examAssignment, TreeSet exams, boolean isEditable) {
        StringBuffer sb = new StringBuffer();
        for (Iterator i=exams.iterator();i.hasNext();) {
            Exam exam = (Exam)i.next();
            sb.append("<span "+(ExamType.sExamTypeFinal==exam.getExamType().getType()?"style='font-weight:bold;' ":"")+
                    "title='" + MSG.tooltipExam(exam.getLabel(), exam.getExamType().getLabel()) + "'>");
            if (examAssignment!=null && examAssignment.getExamTypeId().equals(exam.getExamType().getUniqueId())) {
                ExamAssignment ea = examAssignment.getAssignment(exam.getUniqueId());
                if (ea==null && !isShowExamName()) continue;
                sb.append(ea==null?"":ea.getRoomsNameWithPref(", "));
            } else { 
                if (exam.getAssignedPeriod()==null && !isShowExamName()) continue;
                for (Iterator j=new TreeSet(exam.getAssignedRooms()).iterator();j.hasNext();) {
                    Location location = (Location)j.next();
                    sb.append(location.getLabelWithHint());
                    if (j.hasNext()) sb.append(", ");
                }
            }
            if (ExamType.sExamTypeFinal==exam.getExamType().getType()) sb.append("</span>");
            if (i.hasNext()) sb.append("<br>");
        }
        TableCell cell = this.initNormalCell(sb.toString() ,isEditable);
        cell.setAlign("left");
        cell.setNoWrap(true);
        return(cell);
    }

    protected TreeSet getExams(Class_ clazz) {
        return new TreeSet(Exam.findAll(ExamOwner.sOwnerTypeClass,clazz.getUniqueId()));
    }

    private TableCell buildSchedulePrintNote(InstructionalOffering io, boolean isEditable, UserContext user){
    	TableCell cell = null;
	    String note = "";
	    for (CourseOffering co: io.getCourseOfferings()) {
	    	if (co.getScheduleBookNote() != null && !co.getScheduleBookNote().trim().isEmpty()) {
	    		if (!note.isEmpty()) note += "<br>";
	    		if (CommonValues.NoteAsShortText.eq(user.getProperty(UserProperty.CourseOfferingNoteDisplay))) {
	    			note += (co.getScheduleBookNote().length() <= 20 ? co.getScheduleBookNote() : co.getScheduleBookNote().substring(0, 20) + "...");
				} else {
					note += co.getScheduleBookNote();
				}
	    	}
	    }
		if (note.isEmpty()) {
			cell = this.initNormalCell("&nbsp;" ,isEditable);
		} else {
			if (CommonValues.NoteAsIcon.eq(user.getProperty(UserProperty.CourseOfferingNoteDisplay))) {
	    		cell = initNormalCell("<IMG border='0' alt='" + MSG.altHasCourseOfferingNote() + "' title='" + note.replace("<br>", "\n") + "' align='absmiddle' src='images/note.png'>", isEditable);
	    		cell.setAlign("center");	
			} else {
				cell = initNormalCell(note.replace("\n", "<br>"), isEditable);
				cell.setAlign("left");
			}
		}
        return(cell);
    }
    
    protected TableCell buildNote(PreferenceGroup prefGroup, boolean isEditable, UserContext user){
    	TableCell cell = null;
    	if (prefGroup instanceof Class_) {
    		Class_ c = (Class_) prefGroup;
    		if (c.getNotes() != null && !c.getNotes().trim().isEmpty()) {
    			if (CommonValues.NoteAsShortText.eq(user.getProperty(UserProperty.ManagerNoteDisplay))) {
    				String note = (c.getNotes().length() <= 20 ? c.getNotes() : c.getNotes().substring(0, 20) + "...");
    				cell = initNormalCell(note.replaceAll("\n","<br>"), isEditable);
        			cell.setAlign("left");
    			} else if (CommonValues.NoteAsFullText.eq(user.getProperty(UserProperty.ManagerNoteDisplay))) {
    				cell = initNormalCell(c.getNotes().replaceAll("\n","<br>"), isEditable);
        			cell.setAlign("left");
    			} else {
    	    		cell = initNormalCell("<IMG border='0' alt='" + MSG.altHasNoteToMgr() + "' title='" + HtmlUtils.htmlEscape(c.getNotes()) + "' align='absmiddle' src='images/note.png'>", isEditable);
    	    		cell.setAlign("center");
    			}
    		} else { 
        		cell = this.initNormalCell("&nbsp;" ,isEditable);
        	}
    	} else { 
    		cell = this.initNormalCell("&nbsp;" ,isEditable);
    	}
        return(cell);
    }
    
    private TableCell buildNote(InstructionalOffering offering, boolean isEditable, UserContext user){
    	TableCell cell = null;
		if (offering.getNotes() != null && !offering.getNotes().trim().isEmpty()) {
			if (CommonValues.NoteAsShortText.eq(user.getProperty(UserProperty.ManagerNoteDisplay))) {
				String note = (offering.getNotes().length() <= 20 ? offering.getNotes() : offering.getNotes().substring(0, 20) + "...");
				cell = initNormalCell(note.replaceAll("\n","<br>"), isEditable);
    			cell.setAlign("left");
			} else if (CommonValues.NoteAsFullText.eq(user.getProperty(UserProperty.ManagerNoteDisplay))) {
				cell = initNormalCell(offering.getNotes().replaceAll("\n","<br>"), isEditable);
    			cell.setAlign("left");
			} else {
	    		cell = initNormalCell("<IMG border='0' alt='" + MSG.altHasNoteToMgr() + "' title='" + HtmlUtils.htmlEscape(offering.getNotes()) + "' align='absmiddle' src='images/note.png'>", isEditable);
	    		cell.setAlign("center");
			}
		} else { 
    		cell = this.initNormalCell("&nbsp;" ,isEditable);
    	}
        return(cell);
    }

    private TableCell buildManager(PreferenceGroup prefGroup, boolean isEditable){
    	TableCell cell = null;
    	Department managingDept = null;
    	if (prefGroup instanceof Class_) {
    		managingDept = ((Class_)prefGroup).getManagingDept();
    	} else if (prefGroup instanceof SchedulingSubpart) {
    		managingDept = ((SchedulingSubpart)prefGroup).getManagingDept();
    	} 
    	cell = initNormalCell(managingDept==null?"&nbsp;":managingDept.getManagingDeptAbbv(), isEditable);
        return(cell);
    }

    protected TableCell buildMinPerWeek(PreferenceGroup prefGroup, boolean isEditable){
    	TableCell cell = null;
    	if (prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
    		String suffix = "";
    		ClassDurationType dtype = aClass.getSchedulingSubpart().getInstrOfferingConfig().getEffectiveDurationType();
    		if (dtype != null && !dtype.equals(aClass.getSchedulingSubpart().getSession().getDefaultClassDurationType())) {
    			suffix = " " + dtype.getAbbreviation();
    		}
    		cell = initNormalCell(aClass.getSchedulingSubpart().getMinutesPerWk() + suffix, isEditable);
            cell.setAlign("right");	
    	} else if (prefGroup instanceof SchedulingSubpart) {
    			SchedulingSubpart aSchedulingSubpart = (SchedulingSubpart) prefGroup;
        		String suffix = "";
        		ClassDurationType dtype = aSchedulingSubpart.getInstrOfferingConfig().getEffectiveDurationType();
        		if (dtype != null && !dtype.equals(aSchedulingSubpart.getSession().getDefaultClassDurationType())) {
        			suffix = " " + dtype.getAbbreviation();
        		}
        		cell = initNormalCell(aSchedulingSubpart.getMinutesPerWk() + suffix, isEditable);
                cell.setAlign("right");	
    	} else {
    		cell = this.initNormalCell("&nbsp;" ,isEditable);
    	}
        return(cell);
    }

    private TableCell buildRoomLimit(PreferenceGroup prefGroup, boolean isEditable, boolean classLimitDisplayed){
    	TableCell cell = null;
    	if (prefGroup instanceof Class_){
    		Class_ aClass = (Class_) prefGroup;
    		if (aClass.getNbrRooms()!=null && aClass.getNbrRooms().intValue()!=1) {
    			if (aClass.getNbrRooms().intValue()==0)
    				cell = initNormalCell("<i>N/A</i>", isEditable);
    			else {
    				String text = aClass.getNbrRooms().toString();
    				text += " at ";
    				if (aClass.getRoomRatio() != null)
    					text += sRoomRatioFormat.format(aClass.getRoomRatio().floatValue());
    				else
    					text += "0";
    				cell = initNormalCell(text, isEditable);
    				cell.setNoWrap(true);
    			}
    		} else {
    			if (aClass.getRoomRatio() != null){
    				if (classLimitDisplayed && aClass.getRoomRatio().equals(new Float(1.0))){
    					cell = initNormalCell("&nbsp;", isEditable);
    				} else {
    					cell = initNormalCell(sRoomRatioFormat.format(aClass.getRoomRatio().floatValue()), isEditable);
    				}
    			} else {
    				if (aClass.getExpectedCapacity() == null){
    					cell = initNormalCell("&nbsp;", isEditable);
    				} else {
    					cell = initNormalCell("0", isEditable);
    				}
    			}
    		}
            cell.setAlign("right");	
    	} else {
    		cell = this.initNormalCell("&nbsp;" ,isEditable);
    	}
        return(cell);
    }
    
    private TableCell buildAssignedTime(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable){
    	TableCell cell = null;
    	if (classAssignment!=null && prefGroup instanceof Class_) {
    		Class_ aClass = (Class_) prefGroup;
    		Assignment a = null;
    		AssignmentPreferenceInfo info = null;
    		try {
    			a = classAssignment.getAssignment(aClass);
    			info = classAssignment.getAssignmentInfo(aClass);
    		} catch (Exception e) {
    			Debug.error(e);
    		}
    		if (a!=null) {
    			StringBuffer sb = new StringBuffer();
    			if (info!=null) {
    				sb.append("<font color='"+(isEditable?PreferenceLevel.int2color(info.getTimePreference()):disabledColor)+"'>");
    			}
   				Enumeration<Integer> e = a.getTimeLocation().getDays();
   				while (e.hasMoreElements()){
   					sb.append(CONSTANTS.shortDays()[e.nextElement()]);
   				}
   				sb.append(" ");
   				sb.append(a.getTimeLocation().getStartTimeHeader(CONSTANTS.useAmPm()));
   				sb.append("-");
   				sb.append(a.getTimeLocation().getEndTimeHeader(CONSTANTS.useAmPm()));
   				if (info!=null)
   					sb.append("</font>");
    			cell = initNormalCell(sb.toString(), isEditable);
    		} else {
    			cell = initNormalCell("&nbsp;", isEditable);
    		}
            cell.setAlign("left");
            cell.setNoWrap(true);
    	} else {
    		cell = this.initNormalCell("&nbsp;" ,isEditable);
    	}
        return(cell);
    }
   
    private TableCell buildAssignedRoom(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable) {
    	TableCell cell = null;
    	if (classAssignment!=null && prefGroup instanceof Class_){
    		Class_ aClass = (Class_) prefGroup;
    		Assignment a = null;
    		AssignmentPreferenceInfo info = null;
    		try {
    			a= classAssignment.getAssignment(aClass);
    			info = classAssignment.getAssignmentInfo(aClass);
    		} catch (Exception e) {
    			Debug.error(e);
    		}
    		if (a!=null) {
    			StringBuffer sb = new StringBuffer();
	    		Iterator it2 = a.getRooms().iterator();
	    		while (it2.hasNext()){
	    			Location room = (Location)it2.next();
	    			if (info!=null)
	    				sb.append("<span style='color:"+(isEditable?PreferenceLevel.int2color(info.getRoomPreference(room.getUniqueId())):disabledColor)+";' " +
	    						"onmouseover=\"showGwtRoomHint(this, '" + room.getUniqueId() + "', '" + (isEditable ? PreferenceLevel.int2string(info.getRoomPreference(room.getUniqueId())) : "") + "');\" onmouseout=\"hideGwtRoomHint();\">");
	    			sb.append(room.getLabel());
	   				if (info!=null)
	   					sb.append("</span>");
	    			if (it2.hasNext()){
	        			sb.append("<BR>");
	        		} 
	    		}	
	    		cell = initNormalCell(sb.toString(), isEditable);
    		} else {
    			cell = initNormalCell("&nbsp;", isEditable);
    		}
           cell.setAlign("left");
           cell.setNoWrap(true);
    	} else {
    		cell = this.initNormalCell(" &nbsp;" ,isEditable);
    	}
        return(cell);
    }
    private TableCell buildAssignedRoomCapacity(ClassAssignmentProxy classAssignment, PreferenceGroup prefGroup, boolean isEditable) {
    	TableCell cell = null;
    	if (classAssignment!=null && prefGroup instanceof Class_){
    		Class_ aClass = (Class_) prefGroup;
    		Assignment a = null;
   			try {
   				a = classAssignment.getAssignment(aClass);
   			} catch (Exception e) {
   				Debug.error(e);
   			}
	   		if (a!=null) {
	   			StringBuffer sb = new StringBuffer();
				Iterator it2 = a.getRooms().iterator();
				while (it2.hasNext()){
					sb.append(((Location) it2.next()).getCapacity());
					if (it2.hasNext()){
    					sb.append("<BR>");
    				} 
				}
				cell = initNormalCell(sb.toString(), isEditable);
    		} else {
    			cell = initNormalCell(" &nbsp;", isEditable);
    		}
           cell.setAlign("right");
           cell.setNoWrap(true);
    	} else {
    		cell = this.initNormalCell("&nbsp;" ,isEditable);
    	}
        return(cell);
    }
    
    //MSG.columnNote(): if changing column order column order must be changed in
    //		buildTableHeader, addInstrOffrRowsToTable, buildClassOrSubpartRow, and buildConfigRow
    protected void buildClassOrSubpartRow(ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, TableRow row, CourseOffering co, PreferenceGroup prefGroup, int indentSpaces, boolean isEditable, String prevLabel, String icon, SessionContext context){
    	boolean classLimitDisplayed = false;
    	if (isShowLabel()){
	        row.addContent(this.buildPrefGroupLabel(co, prefGroup, indentSpaces, isEditable, prevLabel, icon));
    	} 
    	if (isShowDivSec()){
    		row.addContent(this.buildDivisionSection(co, prefGroup, isEditable));
    	}
    	if (isShowDemand()){
    		row.addContent(this.buildPrefGroupDemand(prefGroup, isEditable));
    	} 
    	if (isShowProjectedDemand()){
    		row.addContent(this.buildPrefGroupProjectedDemand(prefGroup, isEditable));
    	} 
    	if (isShowLimit()){
    		classLimitDisplayed = true;
    		row.addContent(this.buildLimit(classAssignment, prefGroup, isEditable));
       	} 
    	if (isShowSnapshotLimit()){
    		row.addContent(this.buildSnapshotLimit(prefGroup, isEditable));
       	} 
     	if (isShowRoomRatio()){
    		row.addContent(this.buildRoomLimit(prefGroup, isEditable, classLimitDisplayed));
       	} 
    	if (isShowManager()){
    		row.addContent(this.buildManager(prefGroup, isEditable));
     	} 
    	if (isShowDatePattern()){
    		row.addContent(this.buildDatePatternCell(classAssignment,prefGroup, isEditable));
     	} 
    	if (isShowMinPerWk()){
    		row.addContent(this.buildMinPerWeek(prefGroup, isEditable));
       	} 
    	if (isShowTimePattern()){
    		row.addContent(this.buildTimePatternCell(prefGroup, isEditable));
    	} 
    	if (isShowPreferences()){
    		row.addContent(this.buildPreferenceCell(classAssignment,prefGroup, TimePref.class, isEditable));
    		row.addContent(this.buildPreferenceCell(classAssignment,prefGroup, new Class[] {RoomPref.class, BuildingPref.class, RoomFeaturePref.class, RoomGroupPref.class} , isEditable));
    		if (getDisplayDistributionPrefs()) {
    			row.addContent(this.buildPreferenceCell(classAssignment,prefGroup, DistributionPref.class, isEditable));
    		}
    	}
    	if (getDisplayInstructorPrefs()) {
			row.addContent(this.buildPreferenceCell(classAssignment,prefGroup, InstructorAttributePref.class, isEditable));
			row.addContent(this.buildPreferenceCell(classAssignment,prefGroup, InstructorPref.class, isEditable));
    	}
    	if (isShowInstructorAssignment()) {
    		row.addContent(this.buildInstructorAssignment(prefGroup, isEditable));
    	}
    	if (isShowInstructor()){
    		row.addContent(this.buildInstructor(prefGroup, isEditable));
    	}
    	if (getDisplayTimetable() && isShowTimetable()){
    		row.addContent(this.buildAssignedTime(classAssignment, prefGroup, isEditable));
    		row.addContent(this.buildAssignedRoom(classAssignment, prefGroup, isEditable));
    		row.addContent(this.buildAssignedRoomCapacity(classAssignment, prefGroup, isEditable));
    	} 
    	if (isShowTitle()){
    		row.addContent(this.initNormalCell("&nbsp;", isEditable));
    	}
    	if (isShowCredit()){
    		row.addContent(this.initNormalCell("&nbsp;", isEditable));
    	} 
    	if (isShowSubpartCredit()){
            row.addContent(this.buildCredit(prefGroup, isEditable));     		
    	} 
    	if (isShowConsent()){
    		row.addContent(this.initNormalCell("&nbsp;", isEditable));
    	}
    	if (isShowSchedulePrintNote()){
            row.addContent(this.buildSchedulePrintNote(prefGroup, isEditable, context.getUser()));     		
    	} 
    	if (isShowNote()){
            row.addContent(this.buildNote(prefGroup, isEditable, context.getUser()));
    	}
    	if (isShowExam()) {
    	    if (prefGroup instanceof Class_) {
    	        TreeSet exams = getExams((Class_)prefGroup);
    	        for (Iterator<Exam> i = exams.iterator(); i.hasNext(); ) {
                	if (!context.hasPermission(i.next(), Right.ExaminationView))
                		i.remove();
                }
    	        if (isShowExamName()) {
    	            row.addContent(this.buildExamName(exams, isEditable));
    	        }
    	        if (isShowExamTimetable()) {
    	            row.addContent(this.buildExamPeriod(examAssignment, exams, isEditable));
    	            row.addContent(this.buildExamRoom(examAssignment, exams, isEditable));
    	        }
    	    } else {
    	        if (isShowExamName()) {
    	            row.addContent(this.initNormalCell("&nbsp;", isEditable));
    	        }
                if (isShowExamTimetable()) {
                    row.addContent(this.initNormalCell("&nbsp;", isEditable));
                    row.addContent(this.initNormalCell("&nbsp;", isEditable));
                }
    	    }
    	}
    }
    
    private void buildSchedulingSubpartRow(ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, TableStream table, CourseOffering co, SchedulingSubpart ss, int indentSpaces, SessionContext context){
    	boolean isHeaderRow = true;
    	TableRow row = this.initRow(isHeaderRow);
    	boolean isEditable = context.hasPermission(ss, Right.SchedulingSubpartDetail);
        boolean isOffered = !ss.getInstrOfferingConfig().getInstructionalOffering().isNotOffered().booleanValue();        

        if(isOffered)
            row.setOnMouseOver(this.getRowMouseOver(isHeaderRow, isEditable));
        
        if (isEditable && isOffered) {
        	row.setOnClick("document.location='schedulingSubpartDetail.do?ssuid="+ss.getUniqueId().toString()+ "'");
        }

        if(isOffered)
            row.setOnMouseOut(this.getRowMouseOut(isHeaderRow));
        
        this.buildClassOrSubpartRow(classAssignment, examAssignment, row, co, ss, indentSpaces, isEditable, null, null, context);
      table.addContent(row);
    	
    }
    
    private void buildSchedulingSubpartRows(Vector subpartIds, ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, TableStream table, CourseOffering co, SchedulingSubpart ss, int indentSpaces, SessionContext context){
    	if (subpartIds!=null) subpartIds.add(ss.getUniqueId());
        this.buildSchedulingSubpartRow(classAssignment, examAssignment, table, co, ss, indentSpaces, context);
        Set childSubparts = ss.getChildSubparts();
        
		if (childSubparts != null && !childSubparts.isEmpty()){
		    
		    ArrayList childSubpartList = new ArrayList(childSubparts);
		    Collections.sort(childSubpartList, new SchedulingSubpartComparator());
            Iterator it = childSubpartList.iterator();
            SchedulingSubpart child = null;
            
            while (it.hasNext()){              
                child = (SchedulingSubpart) it.next();
                buildSchedulingSubpartRows(subpartIds, classAssignment, examAssignment, table, co, child, indentSpaces + 1, context);
            }
        }
    }
 
    protected void buildClassRow(ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, int ct, TableStream table, CourseOffering co, Class_ aClass, int indentSpaces, SessionContext context, String prevLabel){
    	boolean isHeaderRow = false;
    	boolean isEditable = context.hasPermission(aClass, Right.ClassDetail);
    	TableRow row = this.initRow(isHeaderRow);
        row.setOnMouseOver(this.getRowMouseOver(isHeaderRow, isEditable));
        row.setOnMouseOut(this.getRowMouseOut(isHeaderRow));
       
        if (isEditable) {
            	row.setOnClick("document.location='classDetail.do?cid=" + aClass.getUniqueId().toString() + "&sec=" + aClass.getSectionNumberString() + "'");
        }
        if (aClass.isCancelled()) {
        	row.setStyle("color: gray; font-style: italic;");
        	row.setTitle(MSG.classNoteCancelled(aClass.getClassLabel(co)));
        }
        String icon = null;
        if (getDisplayConflicts() && classAssignment != null) {
        	Set<Assignment> conflicts = null;
        	try { conflicts = classAssignment.getConflicts(aClass.getUniqueId()); } catch (Exception e) {}
        	if (conflicts != null && !conflicts.isEmpty()) {
        		row.setBgColor("#fff0f0");
        		row.setOnMouseOut("this.style.backgroundColor='#fff0f0';");
    			String s = "";
    			for (Assignment c: conflicts) {
    				if (!s.isEmpty()) s += ", ";
    				s += (c.getClassName() + " " + c.getPlacement().getName(CONSTANTS.useAmPm())).trim();
    			}
				row.setTitle(MSG.classIsConflicting(aClass.getClassLabel(co), s));
				icon = "<IMG alt='" + MSG.classIsConflicting(aClass.getClassLabel(co), s) + "' title='" + MSG.classIsConflicting(aClass.getClassLabel(co), s) + "' " +
						"src='images/warning.png' style='margin-left: 1px; margin-right: 3px; vertical-align: top;'>";
        	} else {
        		Set<TimeBlock> ec = null;
        		try { ec = classAssignment.getConflictingTimeBlocks(aClass.getUniqueId()); } catch (Exception e) {}
        		if (ec != null && !ec.isEmpty()) {
        			String s = "";
        			String lastName = null, lastType = null;
        			for (TimeBlock t: ec) {
        				if (lastName == null || !lastName.equals(t.getEventName()) || !lastType.equals(t.getEventType())) {
        					lastName = t.getEventName(); lastType = t.getEventType();
        					if (!s.isEmpty()) s += ", ";
            				s += lastName + " (" + lastType + ")";
        				}
        			}
            		row.setBgColor("#fff0f0");
            		row.setOnMouseOut("this.style.backgroundColor='#fff0f0';");
            		row.setTitle(MSG.classIsConflicting(aClass.getClassLabel(co), s));
            		icon = "<IMG alt='" + MSG.classIsConflicting(aClass.getClassLabel(co), s) + "' title='" + MSG.classIsConflicting(aClass.getClassLabel(co), s) + "' " +
            				"src='images/warning.png' style='margin-left: 1px; margin-right: 3px; vertical-align: top;'>";
        		}
        	}
        }
        
        this.buildClassOrSubpartRow(classAssignment, examAssignment, row, co, aClass, indentSpaces, isEditable && !aClass.isCancelled(), prevLabel, icon, context);
        table.addContent(row);
    }
    
    private void buildClassRows(ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, int ct, TableStream table, CourseOffering co, Class_ aClass, int indentSpaces, SessionContext context, String prevLabel){

        buildClassRow(classAssignment, examAssignment, ct, table, co, aClass, indentSpaces, context, prevLabel);
    	Set childClasses = aClass.getChildClasses();

    	if (childClasses != null && !childClasses.isEmpty()){
        
    	    ArrayList childClassesList = new ArrayList(childClasses);
            Collections.sort(childClassesList, iClassComparator);
            
            Iterator it = childClassesList.iterator();
            Class_ child = null;
            String previousLabel = aClass.htmlLabel();
            while (it.hasNext()){              
                child = (Class_) it.next();
                buildClassRows(classAssignment, examAssignment, ct, table, co, child, indentSpaces + 1, context, previousLabel);
            }
        }
    }


    //MSG.columnNote(): if changing column order column order must be changed in
    //		buildTableHeader, addInstrOffrRowsToTable, buildClassOrSubpartRow, and buildConfigRow
	protected void buildConfigRow(Vector subpartIds, ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, TableStream table, CourseOffering co, InstrOfferingConfig ioc, SessionContext context, boolean printConfigLine, boolean printConfigReservation) {
	    boolean isHeaderRow = true;
	    boolean isEditable = context.hasPermission(ioc.getInstructionalOffering(), Right.InstructionalOfferingDetail);
	    String configName = ioc.getName();
	    boolean unlimited = ioc.isUnlimitedEnrollment().booleanValue();
	    boolean hasConfig = false;
		if (printConfigLine) {
		    TableRow row = this.initRow(isHeaderRow);
	        TableCell cell = null;
        	if (isShowLabel()){
        	    if (configName==null || configName.trim().length()==0)
        	        configName = ioc.getUniqueId().toString();
        	    /*
        	    cell = this.initNormalCell(
        	            indent + "<u>Configuration</u>: <font class='configTitle'>" + configName + "</font> ", 
        	            isEditable);
        	    */
        	    if (ioc.getInstructionalMethod() != null)
        	    	cell = this.initNormalCell("<span title=\"" + ioc.getInstructionalMethod().getLabel() + "\">" + MSG.labelConfigurationWithInstructionalMethod(configName, ioc.getInstructionalMethod().getReference()) + "</span>", isEditable);
        	    else
        	    	cell = this.initNormalCell(MSG.labelConfiguration(configName), isEditable);
            	cell.setStyle("padding-left: " + indent + "px;");
        	    cell.setNoWrap(true);
        	    row.addContent(cell);

        	}
        	if (isShowDivSec()){
        		row.addContent(initNormalCell("", isEditable));
    		}
        	if (isShowDemand()){
    			row.addContent(initNormalCell("", isEditable));
    		}
        	if (isShowProjectedDemand()){
    			row.addContent(initNormalCell("", isEditable));
    		} 
        	if (isShowLimit()){
    		    cell = this.initNormalCell(
    	            	(unlimited ? "<font size=\"+1\">&infin;</font>" : ioc.getLimit().toString()),  
    		            isEditable);
    		    cell.setAlign("right");
	            row.addContent(cell);
        	} 
        	if (isShowSnapshotLimit()){
    			row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowRoomRatio()){
                row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowManager()){
                row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowDatePattern()){
                row.addContent(initNormalCell("", isEditable));
	       	} 
        	if (isShowMinPerWk()){
                row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowTimePattern()){
	       		row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowPreferences() || getDisplayInstructorPrefs()){
		        for (int j = 0; j < getPreferenceColumns(); j++) {
		            row.addContent(initNormalCell("", isEditable));
		        }
        	} 
        	if (isShowInstructorAssignment()) {
        		row.addContent(initNormalCell("", isEditable));
        	}
        	if (isShowInstructor()){
                row.addContent(initNormalCell("", isEditable));
        	} 
        	if (getDisplayTimetable() && isShowTimetable()){
        		row.addContent(initNormalCell("", isEditable));
        		row.addContent(initNormalCell("", isEditable));
        		row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowTitle()){
        		row.addContent(this.initNormalCell("", isEditable));
        	}
        	if (isShowCredit()){
                row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowSubpartCredit()){
                row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowConsent()){
        		row.addContent(this.initNormalCell("", isEditable));
        	}
        	if (isShowSchedulePrintNote()){
                row.addContent(initNormalCell("", isEditable));
        	} 
        	if (isShowNote()){
                row.addContent(initNormalCell("", isEditable));
        	}
	        
        	/* -- configuration line is not clickable
		    row.setOnMouseOver(this.getRowMouseOver(isHeaderRow, isEditable));
	        row.setOnMouseOut(this.getRowMouseOut(isHeaderRow));
	        */
        	
            if (isShowExam()) {
                TreeSet exams = new TreeSet(Exam.findAll(ExamOwner.sOwnerTypeConfig,ioc.getUniqueId()));
                for (Iterator<Exam> i = exams.iterator(); i.hasNext(); ) {
                	if (!context.hasPermission(i.next(), Right.ExaminationView))
                		i.remove();
                }
                if (isShowExamName()) {
                    row.addContent(this.buildExamName(exams, isEditable));
                }
                if (isShowExamTimetable()) {
                    row.addContent(this.buildExamPeriod(examAssignment, exams, isEditable));
                    row.addContent(this.buildExamRoom(examAssignment, exams, isEditable));
                }
            }

	    
	        table.addContent(row);
	        hasConfig = true;
		}
        ArrayList subpartList = new ArrayList(ioc.getSchedulingSubparts());
        Collections.sort(subpartList, new SchedulingSubpartComparator());
        Iterator it = subpartList.iterator();
        SchedulingSubpart ss = null;
        while(it.hasNext()){
            ss = (SchedulingSubpart) it.next();
            if (ss.getParentSubpart() == null){
                buildSchedulingSubpartRows(subpartIds, classAssignment, examAssignment, table, co, ss, (hasConfig ? 2 : 1) , context);
            }
        }
        it = subpartList.iterator();
        int ct = 0;
        String prevLabel = null;
        while (it.hasNext()) {   	
			ss = (SchedulingSubpart) it.next();
			if (ss.getParentSubpart() == null) {
				if (ss.getClasses() != null) {
					Vector classes = new Vector(ss.getClasses());
					Collections.sort(classes,iClassComparator);
					Iterator cIt = classes.iterator();					
					Class_ c = null;
					while (cIt.hasNext()) {
						c = (Class_) cIt.next();
						buildClassRows(classAssignment, examAssignment, ++ct, table, co, c, 1, context, prevLabel);
						prevLabel = c.htmlLabel();
					}
				}
			}
		}
        

   }

    private void buildConfigRows(ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, TableStream table, CourseOffering co, Set instrOfferingConfigs, SessionContext context, boolean printConfigLine, boolean printConfigReservation) {
        Iterator it = instrOfferingConfigs.iterator();
        InstrOfferingConfig ioc = null;
        while (it.hasNext()){
            ioc = (InstrOfferingConfig) it.next();
            buildConfigRow(null, classAssignment, examAssignment, table, co, ioc, context, printConfigLine && instrOfferingConfigs.size()>1, printConfigReservation);
        }
    }

    //MSG.columnNote(): if changing column order column order must be changed in
    //		buildTableHeader, addInstrOffrRowsToTable, buildClassOrSubpartRow, and buildConfigRow
    private void addInstrOffrRowsToTable(ClassAssignmentProxy classAssignment, ExamAssignmentProxy examAssignment, TableStream table, InstructionalOffering io, Long subjectAreaId, SessionContext context){
        CourseOffering co = io.findSortCourseOfferingForSubjectArea(subjectAreaId);
        boolean isEditable = context.hasPermission(io, Right.InstructionalOfferingDetail);
        TableRow row = (this.initRow(true));
        row.setOnMouseOver(this.getRowMouseOver(true, isEditable));
        row.setOnMouseOut(this.getRowMouseOut(true));
        if (isEditable) row.setOnClick(subjectOnClickAction(io.getUniqueId()));
        boolean isManagedAs = !co.isIsControl().booleanValue(); 
        
        TableCell cell = null;
    	if (isShowLabel()){
    		row.addContent(subjectAndCourseInfo(io, co, isEditable));
    	}
    	if (isShowDivSec()){
    		row.addContent(initNormalCell("", isEditable));
		}
    	if (isShowDemand()){
    		String demand = "0";
    		if (StudentClassEnrollment.sessionHasEnrollments(io.getSessionId())){
    			demand = (io.getEnrollment() != null?io.getEnrollment().toString(): "0");		
    		} else {
	    		demand = (io.getDemand() != null?io.getDemand().toString(): "0");
	    		if (co.isIsControl().booleanValue() && !io.isNotOffered().booleanValue() && (io.getDemand()==null || io.getDemand().intValue()==0)) {
	    			demand = "<span style='font-weight:bold;color:red;'>0</span>";
	    		}
    		}
	        cell = initNormalCell(demand, isEditable && co.isIsControl().booleanValue());
	        cell.setAlign("right");
	        row.addContent(cell);
		}
    	if (isShowProjectedDemand()){
	        cell = initNormalCell((io.getProjectedDemand() != null?io.getProjectedDemand().toString():"0"), isEditable && co.isIsControl().booleanValue());
	        cell.setAlign("right");
	        row.addContent(cell);
		} 
    	if (isShowLimit()){
			boolean unlimited = false;
			for (Iterator x=io.getInstrOfferingConfigs().iterator();!unlimited && x.hasNext();)
				if ((((InstrOfferingConfig)x.next())).isUnlimitedEnrollment().booleanValue())
					unlimited = true;
			if (unlimited)
				cell = initNormalCell("<font size=\"+1\">&infin;</font>", co.isIsControl().booleanValue());
			else
				cell = initNormalCell(io.getLimit() != null?io.getLimit().toString():"", isEditable && co.isIsControl().booleanValue());
            cell.setAlign("right");
            row.addContent(cell);
    	} 
    	if (isShowSnapshotLimit()){
			boolean unlimited = false;
			for (Iterator x=io.getInstrOfferingConfigs().iterator();!unlimited && x.hasNext();)
				if ((((InstrOfferingConfig)x.next())).isUnlimitedEnrollment().booleanValue())
					unlimited = true;
			if (unlimited)
				cell = initNormalCell("<font size=\"+1\">&infin;</font>", co.isIsControl().booleanValue());
			else
				cell = initNormalCell(io.getSnapshotLimit() != null?io.getSnapshotLimit().toString():"", isEditable && co.isIsControl().booleanValue());
            cell.setAlign("right");
            row.addContent(cell);
    	} 
    	int emptyCells = 0;
    	cell = null;
    	if (isShowRoomRatio()){
    		emptyCells ++;
    	} 
    	if (isShowManager()){
    		emptyCells ++;
    	}
    	if (isShowDatePattern()){
    		emptyCells ++;
       	}
    	if (isShowMinPerWk()){
    		emptyCells ++;
    	} 
    	if (isShowTimePattern()) {
    		emptyCells ++;
    	}
    	if (isShowPreferences() || getDisplayInstructorPrefs()) {
    		emptyCells += getPreferenceColumns();
    	}
    	if (isShowInstructorAssignment()) {
    		emptyCells ++;
    	}
    	if (isShowInstructor()){
    		emptyCells ++;
    	}
    	if (getDisplayTimetable() && isShowTimetable()) {
    		emptyCells += 3;
    	}
    	if (emptyCells>0) {
            if (isManagedAs) {
            	if (!isShowTitle() && io.getControllingCourseOffering().getTitle()!=null) {
            		String title = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
            		if (co.getTitle()!=null && co.getTitle().length()>0) {
            			title += "<b>" + co.getTitle() + "</b>";
            			title += " (<span title='" + io.getControllingCourseOffering().getCourseNameWithTitle() + "'>" + MSG.crossListManagedAs(io.getControllingCourseOffering().getCourseName()) + "</span>)";
            		} else {
            			title += "<span title='" + io.getControllingCourseOffering().getCourseNameWithTitle() + "'>" + MSG.crossListManagedAs(io.getControllingCourseOffering().getCourseName()) + "</span>";
            		}
                    for (Iterator it = io.courseOfferingsMinusSortCourseOfferingForSubjectArea(co.getSubjectArea().getUniqueId()).iterator(); it.hasNext();) {
                    	CourseOffering x = (CourseOffering)it.next();
                    	title += "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
                    	if (x.getTitle()!=null) title += "<font color=\"" + disabledColor + "\">" +x.getTitle() + "</font>";
                    }
                	cell = initNormalCell("<font color=\"" + disabledColor + "\">"+title+"</font>", isEditable);
            	} else {
            		cell = initNormalCell("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font color=\"" + disabledColor + "\"><span title='" + io.getControllingCourseOffering().getCourseNameWithTitle() + "'>" + MSG.crossListManagedAs(io.getControllingCourseOffering().getCourseName()) + "</span></font>", isEditable);
            	}
            } else {
            	if (!isShowTitle() && io.getControllingCourseOffering().getTitle()!=null) {
            		String title = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";
            		if (co.isIsControl().booleanValue()) title += "<b>";
            		title += (co.getTitle()==null?"":co.getTitle());
            		if (co.isIsControl().booleanValue()) title += "</b>";
            		for (Iterator it = io.courseOfferingsMinusSortCourseOfferingForSubjectArea(co.getSubjectArea().getUniqueId()).iterator(); it.hasNext();) {
                    	CourseOffering x = (CourseOffering)it.next();
                    	title += "<br>";
                    	if (x.getTitle()!=null) {
                    		title += "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<font color=\"" + disabledColor + "\">" + x.getTitle() + "</font>";
                    	}
                    }
            		cell = initNormalCell(title, isEditable);
            	} else {
            		cell = initNormalCell("", isEditable);
            	}
            }
            cell.setColSpan(emptyCells);
            cell.setAlign("left");
            row.addContent(cell);
    	}
    	if (isShowTitle()){
    		String title = "";
    		if (co.getTitle() != null) {
        		if (isManagedAs)
        			title = "<font color=\"" + disabledColor + "\">" + co.getTitle() + "</font>";
        		else
        			title = co.getTitle();
    		}
    		for (Iterator it = io.courseOfferingsMinusSortCourseOfferingForSubjectArea(co.getSubjectArea().getUniqueId()).iterator(); it.hasNext();) {
            	CourseOffering x = (CourseOffering)it.next();
            	title += "<br>" + (x.getTitle() == null ? "" : "<font color=\"" + disabledColor + "\">"+x.getTitle()+"</font>");
    		}
    		cell = initNormalCell(title, isEditable);
    		if (io.getCourseOfferings().size() > 1) cell.setNoWrap(true);
    		row.addContent(cell);
    	}
    	if (isShowCredit()){
    		String credit = "";
    		if (co.isIsControl().booleanValue()) credit += "<b>";
    		if (isManagedAs)
    			credit = "<font color=\"" + disabledColor + "\"><b>" + (co.getCredit() == null ? "" : "<span title='"+co.getCredit().creditText()+"'>" + co.getCredit().creditAbbv() + "</span>") + "</b></font>";
    		else
    			credit = "<b>" + (co.getCredit() == null ? "" : "<span title='"+co.getCredit().creditText()+"'>" + co.getCredit().creditAbbv() + "</span>") + "</b>";
    		if (co.isIsControl().booleanValue()) credit += "</b>";
    		for (Iterator it = io.courseOfferingsMinusSortCourseOfferingForSubjectArea(co.getSubjectArea().getUniqueId()).iterator(); it.hasNext();) {
            	CourseOffering x = (CourseOffering)it.next();
            	credit += "<br>";
            	if (x.getCredit() != null) {
            		credit += "<font color=\"" + disabledColor + "\"><span title='"+x.getCredit().creditText()+"'>"+x.getCredit().creditAbbv()+"</span></font>";
            	}
    		}
            row.addContent(initNormalCell(credit, isEditable));   
    	}
    	if (isShowSubpartCredit()){
            row.addContent(initNormalCell("", isEditable));
    	} 
    	if (isShowConsent()){
    		String consent = "";
    		if (co.isIsControl().booleanValue()) consent += "<b>";
    		if (isManagedAs)
    			consent = "<font color=\"" + disabledColor + "\"><b>" + (co.getConsentType() == null ? MSG.noConsentRequired() : "<span title='"+co.getConsentType().getLabel()+"'>" + co.getConsentType().getAbbv() + "</span>") + "</b></font>";
    		else
    			consent = "<b>" + (co.getConsentType() == null ? MSG.noConsentRequired() : "<span title='"+co.getConsentType().getLabel()+"'>" + co.getConsentType().getAbbv() + "</span>") + "</b>";
    		if (co.isIsControl().booleanValue()) consent += "</b>";
    		for (Iterator it = io.courseOfferingsMinusSortCourseOfferingForSubjectArea(co.getSubjectArea().getUniqueId()).iterator(); it.hasNext();) {
            	CourseOffering x = (CourseOffering)it.next();
            	consent += "<br>";
            	if (x.getConsentType() != null) {
            		consent += "<font color=\"" + disabledColor + "\"><span title='"+x.getConsentType().getLabel()+"'>"+x.getConsentType().getAbbv()+"</span></font>";
            	} else {
            		consent += "<font color=\"" + disabledColor + "\">"+MSG.noConsentRequired()+"</font>";
            	}
    		}
    		cell = initNormalCell(consent, isEditable);
    		if (io.getCourseOfferings().size() > 1)
    			cell.setNoWrap(true);
            row.addContent(cell);     		
    	}
    	if (isShowSchedulePrintNote()){
            row.addContent(buildSchedulePrintNote(io, isEditable, context.getUser()));     		
    	}
    	if (isShowNote()){
            row.addContent(buildNote(io, isEditable, context.getUser()));
    	}
        if (isShowExam()) {
            TreeSet exams = new TreeSet(Exam.findAll(ExamOwner.sOwnerTypeOffering,io.getUniqueId()));
            for (Iterator i=io.getCourseOfferings().iterator();i.hasNext();) {
                CourseOffering cox = (CourseOffering)i.next();
                exams.addAll(Exam.findAll(ExamOwner.sOwnerTypeCourse,cox.getUniqueId()));
            }
            if (io.getInstrOfferingConfigs().size()==1) {
                for (Iterator i=io.getInstrOfferingConfigs().iterator();i.hasNext();) {
                    InstrOfferingConfig ioc = (InstrOfferingConfig)i.next();
                    exams.addAll(Exam.findAll(ExamOwner.sOwnerTypeConfig,ioc.getUniqueId()));
                }
            }
            for (Iterator<Exam> i = exams.iterator(); i.hasNext(); ) {
            	if (!context.hasPermission(i.next(), Right.ExaminationView))
            		i.remove();
            }
            if (isShowExamName()) {
                row.addContent(this.buildExamName(exams, isEditable));
            }
            if (isShowExamTimetable()) {
                row.addContent(this.buildExamPeriod(examAssignment, exams, isEditable));
                row.addContent(this.buildExamRoom(examAssignment, exams, isEditable));
            }
        }
        table.addContent(row);
        if (io.getInstrOfferingConfigs() != null & !io.getInstrOfferingConfigs().isEmpty()){
        	TreeSet configs = new TreeSet(new InstrOfferingConfigComparator(io.getControllingCourseOffering().getSubjectArea().getUniqueId()));
        	configs.addAll(io.getInstrOfferingConfigs());
            buildConfigRows(classAssignment, examAssignment, table, io.getControllingCourseOffering(), configs, context, true, false);
        }
    }
    
    protected TableStream initTable(JspWriter outputStream, Long sessionId, String durationColName){
    	TableStream table = new TableStream(outputStream);
        table.setWidth("100%");
        table.setBorder(0);
        table.setCellSpacing(0);
        table.setCellPadding(3);
        table.tableDefComplete();
        this.buildTableHeader(table, sessionId, durationColName);
        return(table);
    }
    
    protected TableStream initTable(JspWriter outputStream, Long sessionId){
    	ClassDurationType dtype = ClassDurationType.findDefaultType(sessionId, null);
    	return initTable(outputStream, sessionId, dtype == null ? MSG.columnMinPerWk() : dtype.getLabel());
    }
    
    public void htmlTableForInstructionalOffering(
    		SessionContext context,
    		ClassAssignmentProxy classAssignment, 
    		ExamAssignmentProxy examAssignment,
            Long instructionalOfferingId, 
            JspWriter outputStream,
            Comparator classComparator){
    	
    	if (instructionalOfferingId != null) {
	        InstructionalOfferingDAO idao = new InstructionalOfferingDAO();
	        InstructionalOffering io = idao.get(instructionalOfferingId);
	        Long subjectAreaId = io.getControllingCourseOffering().getSubjectArea().getUniqueId();
	        
	        // Get Configuration
	        TreeSet ts = new TreeSet();
	        ts.add(io);
	        WebInstructionalOfferingTableBuilder iotbl = new WebInstructionalOfferingTableBuilder();
	        iotbl.setDisplayDistributionPrefs(false);
	        setVisibleColumns(COLUMNS);
		    htmlTableForInstructionalOfferings(
		    			context,
				        classAssignment,
				        examAssignment,
				        ts, subjectAreaId, false, false, outputStream, classComparator,
				        null);
    	}
    }
    
    public void htmlTableForInstructionalOfferings(
    		SessionContext context,
            ClassAssignmentProxy classAssignment, 
            ExamAssignmentProxy examAssignment,
            InstructionalOfferingListForm form, 
            String[] subjectAreaIds, 
            boolean displayHeader,
            boolean allCoursesAreGiven,
            JspWriter outputStream,
            String backType,
            String backId){
    	
    	setBackType(backType); setBackId(backId);
    	
    	this.setVisibleColumns(form);
    	
    	List<Long> navigationOfferingIds = new ArrayList<Long>();
    	
    	for (String subjectAreaId: subjectAreaIds) {
        	htmlTableForInstructionalOfferings(context, classAssignment, examAssignment,
        			form.getInstructionalOfferings(Long.valueOf(subjectAreaId)), 
         			Long.valueOf(subjectAreaId),
         			displayHeader, allCoursesAreGiven,
        			outputStream,
        			new ClassCourseComparator(form.getSortBy(), classAssignment, false),
        			navigationOfferingIds
        	);
    	}
   	
    }
    
    protected void htmlTableForInstructionalOfferings(
    		SessionContext context,
            ClassAssignmentProxy classAssignment, 
            ExamAssignmentProxy examAssignment,
            TreeSet insructionalOfferings, 
            Long subjectAreaId, 
            boolean displayHeader, boolean allCoursesAreGiven,
            JspWriter outputStream,
            Comparator classComparator,
            List<Long> navigationOfferingIds){
    	
    	if (insructionalOfferings == null) return;
    	
    	if (classComparator!=null)
    		setClassComparator(classComparator);
    	
    	SubjectArea subjectArea = SubjectAreaDAO.getInstance().get(Long.valueOf(subjectAreaId));
        
    	if (isShowTimetable()) {
            boolean hasTimetable = false;
            if (context.hasPermission(Right.ClassAssignments) && classAssignment != null) {
            	try {
                	if (classAssignment instanceof CachedClassAssignmentProxy) {
                		Vector allClasses = new Vector();
        				for (Iterator i=insructionalOfferings.iterator();!hasTimetable && i.hasNext();) {
        					InstructionalOffering io = (InstructionalOffering)i.next();
        					for (Iterator j=io.getInstrOfferingConfigs().iterator();!hasTimetable && j.hasNext();) {
        						InstrOfferingConfig ioc = (InstrOfferingConfig)j.next();
        						for (Iterator k=ioc.getSchedulingSubparts().iterator();!hasTimetable && k.hasNext();) {
        							SchedulingSubpart ss = (SchedulingSubpart)k.next();
        							for (Iterator l=ss.getClasses().iterator();l.hasNext();) {
        								Class_ clazz = (Class_)l.next();
        								allClasses.add(clazz);
        							}
        						}
        					}
        				}
                		((CachedClassAssignmentProxy)classAssignment).setCache(allClasses);
                		hasTimetable = !classAssignment.getAssignmentTable(allClasses).isEmpty();
                	} else {
        				for (Iterator i=insructionalOfferings.iterator();!hasTimetable && i.hasNext();) {
        					InstructionalOffering io = (InstructionalOffering)i.next();
        					for (Iterator j=io.getInstrOfferingConfigs().iterator();!hasTimetable && j.hasNext();) {
        						InstrOfferingConfig ioc = (InstrOfferingConfig)j.next();
        						for (Iterator k=ioc.getSchedulingSubparts().iterator();!hasTimetable && k.hasNext();) {
        							SchedulingSubpart ss = (SchedulingSubpart)k.next();
        							for (Iterator l=ss.getClasses().iterator();l.hasNext();) {
        								Class_ clazz = (Class_)l.next();
        								if (classAssignment.getAssignment(clazz)!=null) {
        									hasTimetable = true; break;
        								}
        							}
                				}
                			}
                		}
                	}
            	} catch (Exception e) {}
            }
            setDisplayTimetable(hasTimetable);
    	}
    	
    	if (isShowExam())
    	    setShowExamTimetable(examAssignment!=null || Exam.hasTimetable(context.getUser().getCurrentAcademicSessionId()));
    	
        ArrayList notOfferedOfferings = new ArrayList();
        ArrayList offeredOfferings = new ArrayList();
        
        Iterator it = insructionalOfferings.iterator();
        InstructionalOffering io = null;
        boolean hasOfferedCourses = false;
        boolean hasNotOfferedCourses = false;
		setUserSettings(context.getUser());
        
         while (it.hasNext()){
            io = (InstructionalOffering) it.next();
            if (io.isNotOffered() == null || io.isNotOffered().booleanValue()){
            	hasNotOfferedCourses = true;
            	notOfferedOfferings.add(io);
            } else {
            	hasOfferedCourses = true;
            	offeredOfferings.add(io);
            }
        }
         
        if (hasOfferedCourses || allCoursesAreGiven) {
    		if(displayHeader) {
    		    try {
    		    	if (allCoursesAreGiven)
    		    		outputStream.print("<DIV align=\"right\"><A class=\"l7\" href=\"#notOffered" + subjectAreaId + "\">" + MSG.labelNotOfferedCourses(subjectArea.getSubjectAreaAbbreviation()) + "</A></DIV>");
    			    outputStream.print("<DIV class=\"WelcomeRowHead\"><A name=\"offered" + subjectAreaId + "\"></A>" + MSG.labelOfferedCourses(subjectArea.getSubjectAreaAbbreviation()) + "</DIV>");
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
    		}
                  
            if (hasOfferedCourses){
                it = offeredOfferings.iterator();
                TableStream offeredTable = this.initTable(outputStream, context.getUser().getCurrentAcademicSessionId());
                
                while (it.hasNext()){
                    io = (InstructionalOffering) it.next();
                    if (navigationOfferingIds != null)
                    	navigationOfferingIds.add(io.getUniqueId());
                    this.addInstrOffrRowsToTable(classAssignment, examAssignment, offeredTable, io, subjectAreaId, context);            	
                }
                offeredTable.tableComplete();
            } else {
                if(displayHeader)
    				try {
    					outputStream.print("<font class=\"error\">" + MSG.errorNoCoursesOffered(subjectArea.getSubjectAreaAbbreviation()) + "</font>");
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
            }
        }
        
        if (hasNotOfferedCourses || allCoursesAreGiven) {
            if(displayHeader) {
    	        try {
    				outputStream.print("<br>");
    				if (allCoursesAreGiven)
    					outputStream.print("<DIV align=\"right\"><A class=\"l7\" href=\"#offered" + subjectAreaId + "\">" + MSG.labelOfferedCourses(subjectArea.getSubjectAreaAbbreviation()) + "</A></DIV>");
    		        outputStream.print("<DIV class=\"WelcomeRowHead\"><A name=\"notOffered" + subjectAreaId + "\"></A>" + MSG.labelNotOfferedCourses(subjectArea.getSubjectAreaAbbreviation()) + "</DIV>");
    			} catch (IOException e) {
    				e.printStackTrace();
    			}
            }
            
            if (hasNotOfferedCourses){
                it = notOfferedOfferings.iterator();
                TableStream notOfferedTable = this.initTable(outputStream, context.getUser().getCurrentAcademicSessionId());
                while (it.hasNext()){
                    io = (InstructionalOffering) it.next();
                    if (navigationOfferingIds != null)
                    	navigationOfferingIds.add(io.getUniqueId());
                    this.addInstrOffrRowsToTable(classAssignment, examAssignment, notOfferedTable, io, subjectAreaId, context);            	
                }
                notOfferedTable.tableComplete();
            } else {
                if(displayHeader)
    				try {
    					outputStream.print("<font class=\"normal\">&nbsp;" + MSG.errorAllCoursesOffered(subjectArea.getSubjectAreaAbbreviation()) + "</font>");
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
            }
        }
        
        if (navigationOfferingIds != null)
        	Navigation.set(context, Navigation.sInstructionalOfferingLevel, navigationOfferingIds);
    }


	protected void setVisibleColumns(InstructionalOfferingListFormInterface form){
		setShowLabel(true);
		setShowDivSec(form.getDivSec().booleanValue());
		setShowDemand(form.getDemand().booleanValue());
		setShowProjectedDemand(form.getProjectedDemand().booleanValue());
		setShowMinPerWk(form.getMinPerWk().booleanValue());
		setShowLimit(form.getLimit().booleanValue());
		setShowSnapshotLimit(form.getSnapshotLimit().booleanValue());
		setShowRoomRatio(form.getRoomLimit().booleanValue());
		setShowManager(form.getManager().booleanValue());
		setShowDatePattern(form.getDatePattern().booleanValue());
		setShowTimePattern(form.getTimePattern().booleanValue());
		setShowPreferences(form.getPreferences().booleanValue());
		setShowInstructor(form.getInstructor().booleanValue());
		if (form.getTimetable() != null)
			setShowTimetable(form.getTimetable().booleanValue());
		else
			setShowTimetable(false);
		setShowCredit(form.getCredit().booleanValue());
		setShowSubpartCredit(form.getSubpartCredit().booleanValue());
		setShowSchedulePrintNote(form.getSchedulePrintNote().booleanValue());
		setShowNote(form.getNote().booleanValue());
		setShowConsent(form.getConsent().booleanValue());
		setShowTitle(form.getTitle().booleanValue());
		if (form.getExams() != null) {
		    setShowExam(form.getExams());
		} else {
		    setShowExam(false);
		}
		if (form.getInstructorAssignment() != null) {
			setShowInstructorAssignment(form.getInstructorAssignment());
		} else {
			setShowInstructorAssignment(false);
		}
	}
	
	protected void setVisibleColumns(ArrayList<String> columns){
		
		setShowLabel(columns.contains(LABEL));
		setShowDivSec(columns.contains(MSG.columnExternalId()));
		setShowDemand(columns.contains(MSG.columnDemand()));
		setShowProjectedDemand(columns.contains(MSG.columnProjectedDemand()));
		setShowMinPerWk(columns.contains(MSG.columnMinPerWk()));
		setShowLimit(columns.contains(MSG.columnLimit()));
		setShowSnapshotLimit(columns.contains(MSG.columnSnapshotLimit()));
		setShowRoomRatio(columns.contains(MSG.columnRoomRatio()));
		setShowManager(columns.contains(MSG.columnManager()));
		setShowDatePattern(columns.contains(MSG.columnDatePattern()));
		setShowTimePattern(columns.contains(MSG.columnTimePattern()));
		setShowPreferences(columns.contains(MSG.columnPreferences()));
		setShowInstructor(columns.contains(MSG.columnInstructor()));
		setShowTimetable(columns.contains(MSG.columnTimetable()));
		setShowCredit(columns.contains(MSG.columnOfferingCredit()));
		setShowSubpartCredit(columns.contains(MSG.columnSubpartCredit()));
		setShowSchedulePrintNote(columns.contains(MSG.columnSchedulePrintNote()));
		setShowNote(columns.contains(MSG.columnNote()));
		setShowConsent(columns.contains(MSG.columnConsent()));
		setShowTitle(columns.contains(MSG.columnTitle()));
		setShowExam(columns.contains(MSG.columnExam()));
		
	}
	
	protected void setVisibleColumns(String[] columns){
		ArrayList<String> a = new ArrayList<String>();
		for (int i = 0 ; i < columns.length; i++){
			a.add(columns[i]);
		}
		setVisibleColumns(a);
				
	}

	public boolean isShowCredit() {
		return showCredit;
	}
	public void setShowCredit(boolean showCredit) {
		this.showCredit = showCredit;
	}
	public boolean isShowDatePattern() {
		return showDatePattern;
	}
	public void setShowDatePattern(boolean showDatePattern) {
		this.showDatePattern = showDatePattern;
	}
	public boolean isShowDemand() {
		return showDemand;
	}
	public void setShowDemand(boolean showDemand) {
		this.showDemand = showDemand;
	}
	public boolean isShowDivSec() {
		return showDivSec;
	}
	public void setShowDivSec(boolean showDivSec) {
		this.showDivSec = showDivSec;
	}
	public boolean isShowLabel() {
		return showLabel;
	}
	public void setShowLabel(boolean showLabel) {
		this.showLabel = showLabel;
	}
	public boolean isShowLimit() {
		return showLimit;
	}
	public void setShowLimit(boolean showLimit) {
		this.showLimit = showLimit;
	}
	public boolean isShowSnapshotLimit() {
		return showSnapshotLimit;
	}
	public void setShowSnapshotLimit(boolean showSnapshotLimit) {
		this.showSnapshotLimit = showSnapshotLimit;
	}
	public boolean isShowManager() {
		return showManager;
	}
	public void setShowManager(boolean showManager) {
		this.showManager = showManager;
	}
	public boolean isShowMinPerWk() {
		return showMinPerWk;
	}
	public void setShowMinPerWk(boolean showMinPerWk) {
		this.showMinPerWk = showMinPerWk;
	}
	public boolean isShowNote() {
		return showNote;
	}
	public void setShowNote(boolean showNote) {
		this.showNote = showNote;
	}
	public boolean isShowPreferences() {
		return showPreferences;
	}
	public void setShowPreferences(boolean showPreferences) {
		this.showPreferences = showPreferences;
	}
	public boolean isShowProjectedDemand() {
		return showProjectedDemand;
	}
	public void setShowProjectedDemand(boolean showProjectedDemand) {
		this.showProjectedDemand = showProjectedDemand;
	}
	public boolean isShowRoomRatio() {
		return showRoomRatio;
	}
	public void setShowRoomRatio(boolean showRoomRatio) {
		this.showRoomRatio = showRoomRatio;
	}
	public boolean isShowSchedulePrintNote() {
		return showSchedulePrintNote;
	}
	public void setShowSchedulePrintNote(boolean showSchedulePrintNote) {
		this.showSchedulePrintNote = showSchedulePrintNote;
	}
	public boolean isShowTimePattern() {
		return showTimePattern;
	}
	public void setShowTimePattern(boolean showTimePattern) {
		this.showTimePattern = showTimePattern;
	}
	public boolean isShowTimetable() {
		return showTimetable;
	}
	public void setShowTimetable(boolean showTimetable) {
		this.showTimetable = showTimetable;
	}
	public boolean isShowInstructor() {
		return showInstructor;
	}
	public void setShowInstructor(boolean showInstructor) {
		this.showInstructor = showInstructor;
	}

    public Comparator getClassComparator() {
    	return iClassComparator;
    }
    
    public void setClassComparator(Comparator comparator) {
    	iClassComparator = comparator;
    }
    
    public String getBackType() {
    	return iBackType;
    }
    public void setBackType(String backType) {
    	iBackType = backType;
    }
    public String getBackId() {
    	return iBackId;
    }
    public void setBackId(String backId) {
    	iBackId = backId;
    }
	public boolean isShowSubpartCredit() {
		return showSubpartCredit;
	}
	public void setShowSubpartCredit(boolean showSubpartCredit) {
		this.showSubpartCredit = showSubpartCredit;
	}
	

}
