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
package org.unitime.timetable.action;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.ActionRedirect;
import org.apache.struts.util.MessageResources;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.commons.Debug;
import org.unitime.commons.web.WebTable;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.defaults.CommonValues;
import org.unitime.timetable.defaults.UserProperty;
import org.unitime.timetable.form.InstructorEditForm;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.ClassInstructor;
import org.unitime.timetable.model.Class_;
import org.unitime.timetable.model.DepartmentalInstructor;
import org.unitime.timetable.model.InstructorCoursePref;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.model.TimePattern;
import org.unitime.timetable.model.TimePref;
import org.unitime.timetable.model.dao.DepartmentalInstructorDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.util.LookupTables;
import org.unitime.timetable.webutil.BackTracker;


/** 
 * MyEclipse Struts
 * Creation date: 07-24-2006
 * 
 * XDoclet definition:
 * @struts.action path="/instructorPrefEdit" name="instructorEditForm" input="/user/instructorPrefsEdit.jsp" scope="request"
 * @struts.action-forward name="showEdit" path="instructorPrefsEditTile"
 *
 * @author Tomas Muller, Zuzana Mullerova
 */
@Service("/instructorPrefEdit")
public class InstructorPrefEditAction extends PreferencesAction {

	protected final static CourseMessages MSG = Localization.create(CourseMessages.class);
	
	@Autowired SessionContext sessionContext;

	
	// --------------------------------------------------------- Instance Variables

	// --------------------------------------------------------- Methods

	/** 
	 * Method execute
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 */
	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response) throws Exception {
		
    	try {

            // Set common lookup tables
            super.execute(mapping, form, request, response);
            
    		InstructorEditForm frm = (InstructorEditForm) form;       
            MessageResources rsc = getResources(request);
    		ActionMessages errors = new ActionMessages();
            
            // Read parameters
            String instructorId = request.getParameter("instructorId");
            String op = frm.getOp();            
            String reloadCause = request.getParameter("reloadCause");
            
            // Read subpart id from form
            if(op.equals(rsc.getMessage("button.reload"))
            		|| op.equals(MSG.actionAddTimePreference())
                    || op.equals(MSG.actionAddRoomPreference())
                    || op.equals(MSG.actionAddBuildingPreference())
                    || op.equals(MSG.actionAddRoomFeaturePreference())
                    || op.equals(MSG.actionAddDistributionPreference()) 
                    || op.equals(MSG.actionAddRoomGroupPreference())
                    || op.equals(MSG.actionAddCoursePreference())
                    || op.equals(MSG.actionAddAttributePreference())
                    || op.equals(MSG.actionUpdatePreferences()) 
                  //  || op.equals(rsc.getMessage("button.cancel")) -- not used???
                    || op.equals(MSG.actionClearInstructorPreferences())                 
                  //  || op.equals(rsc.getMessage("button.delete")) -- not used???
                    || op.equals(MSG.actionBackToDetail())
                    || op.equals(MSG.actionNextInstructor())
                    || op.equals(MSG.actionPreviousInstructor())) {
            	instructorId = frm.getInstructorId();
            }
            
            // Determine if initial load
            if(op==null || op.trim().length()==0 
                    || ( op.equals(rsc.getMessage("button.reload")) 						
                    	 && (reloadCause==null || reloadCause.trim().length()==0) )) {     
                op = "init";
            }
            
            // Check op exists
            if(op==null || op.trim()=="") 
                throw new Exception (MSG.exceptionNullOperationNotSupported());
            
            //Check instructor exists
            if(instructorId==null || instructorId.trim()=="") 
                throw new Exception (MSG.exceptionInstructorInfoNotSupplied());
            
            sessionContext.checkPermission(instructorId, "DepartmentalInstructor", Right.InstructorPreferences);
            
            boolean timeVertical = CommonValues.VerticalGrid.eq(sessionContext.getUser().getProperty(UserProperty.GridOrientation));
            
            // Set screen name
            frm.setScreenName("instructorPref");
            
            // If subpart id is not null - load subpart info
            DepartmentalInstructorDAO idao = new DepartmentalInstructorDAO();
            DepartmentalInstructor inst = idao.get(new Long(instructorId));
            LookupTables.setupInstructorDistribTypes(request, sessionContext, inst);
            
            // Cancel - Go back to Instructors Detail Screen
            if(op.equals(MSG.actionBackToDetail()) 
                    && instructorId!=null && instructorId.trim()!="") {
	            ActionRedirect redirect = new ActionRedirect(mapping.findForward("showDetail"));
	            redirect.addParameter("instructorId", frm.getInstructorId());
	            return redirect;
            }
            
            // Clear all preferences
            if(op.equals(MSG.actionClearInstructorPreferences())) {
            	doClear(inst.getPreferences(), Preference.Type.TIME, Preference.Type.ROOM, Preference.Type.ROOM_FEATURE, Preference.Type.ROOM_GROUP, Preference.Type.BUILDING, Preference.Type.DISTRIBUTION);
                idao.update(inst);
                op = "init";            	
                
                ChangeLog.addChange(
                        null, 
                        sessionContext,
                        inst, 
                        ChangeLog.Source.INSTRUCTOR_PREF_EDIT, 
                        ChangeLog.Operation.CLEAR_PREF, 
                        null, 
                        inst.getDepartment());
                
	            ActionRedirect redirect = new ActionRedirect(mapping.findForward("showDetail"));
	            redirect.addParameter("instructorId", instructorId);
	            return redirect;
            }
            
            // Reset form for initial load
            if(op.equals("init")) { 
                frm.reset(mapping, request);
            }
            
            // Load form attributes that are constant
            doLoad(request, frm, inst, instructorId);
            
            // Update Preferences for InstructorDept
            if(op.equals(MSG.actionUpdatePreferences()) 
            		|| op.equals(MSG.actionNextInstructor()) 
            		|| op.equals(MSG.actionPreviousInstructor())) {	

            	// Validate input prefs
                errors = frm.validate(mapping, request);
                
                // No errors - Add to instructorDept and update
                if(errors.size()==0) {
                    Set s = inst.getPreferences();
             
                    // Clear all old prefs (excluding course preferences)
                    for (Iterator i = s.iterator(); i.hasNext(); ) {
                    	Preference p = (Preference)i.next();
                    	if (p instanceof InstructorCoursePref) continue;
                    	i.remove();
                    }                
                    
            		super.doUpdate(request, frm, inst, s, timeVertical,
            				Preference.Type.TIME, Preference.Type.ROOM, Preference.Type.ROOM_FEATURE, Preference.Type.ROOM_GROUP, Preference.Type.BUILDING, Preference.Type.DISTRIBUTION);
                    
                    ChangeLog.addChange(
                            null, 
                            sessionContext,
                            inst, 
                            ChangeLog.Source.INSTRUCTOR_PREF_EDIT, 
                            ChangeLog.Operation.UPDATE, 
                            null, 
                            inst.getDepartment());

            		idao.saveOrUpdate(inst);
                    
    	        	if (op.equals(MSG.actionNextInstructor())) {
    	            	response.sendRedirect(response.encodeURL("instructorPrefEdit.do?instructorId="+frm.getNextId()));
    	            	return null;
    	        	}
    	            
    	            if (op.equals(MSG.actionPreviousInstructor())) {
    	            	response.sendRedirect(response.encodeURL("instructorPrefEdit.do?instructorId="+frm.getPreviousId()));
    	            	return null;
    	            }
                    
    	            ActionRedirect redirect = new ActionRedirect(mapping.findForward("showDetail"));
    	            redirect.addParameter("instructorId", frm.getInstructorId());
    	            redirect.addParameter("showPrefs", "true");
    	            return redirect;
                }
                else {
                    saveErrors(request, errors);
                }
            }
            
	        // Initialize Preferences for initial load 
            Set timePatterns = new HashSet();
            frm.setAvailableTimePatterns(null);
            if(op.equals("init")) {
            	initPrefs(frm, inst, null, true);
            	timePatterns.add(new TimePattern(new Long(-1)));
        		//timePatterns.addAll(TimePattern.findApplicable(request,30,false));
            }
            
            //load class assignments
    		if (!inst.getClasses().isEmpty()) {
    		    WebTable classTable = new WebTable( 3,
    			   	null,
    		    	new String[] {"class", "Type" , "Limit"},
    			    new String[] {"left", "left","left"},
    			    null );
    					
    		    //Get class assignment information
    			for (Iterator iterInst = inst.getClasses().iterator(); iterInst.hasNext();) {
    				ClassInstructor ci = (ClassInstructor) iterInst.next();
    				Class_ c = ci.getClassInstructing();
    				classTable.addLine(
    					null,
    					new String[] {
    						c.getClassLabel(),
    						c.getItypeDesc(),
    						c.getExpectedCapacity().toString(),
    					},
    					null,null);
    			}
    			String tblData = classTable.printTable();
    			request.setAttribute("classTable", tblData);
    		}
    		
    		//// Set display distribution to Not Applicable
    		/*
    		request.setAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR, 
    				"<FONT color=696969>Distribution Preferences Not Applicable</FONT>");
    				*/
            
    		// Process Preferences Action
    		processPrefAction(request, frm, errors);
    		
    		// Generate Time Pattern Grids
    		// super.generateTimePatternGrids(request, frm, inst, timePatterns, op, timeVertical, true, null);
    		for (Preference pref: inst.getPreferences()) {
				if (pref instanceof TimePref) {
					frm.setAvailability(((TimePref)pref).getPreference());
					break;
				}
			}

            LookupTables.setupRooms(request, inst);		 // Room Prefs
            LookupTables.setupBldgs(request, inst);		 // Building Prefs
            LookupTables.setupRoomFeatures(request, inst); // Preference Levels
            LookupTables.setupRoomGroups(request, inst);   // Room Groups
		
            BackTracker.markForBack(
            		request,
            		"instructorDetail.do?instructorId="+frm.getInstructorId(),
            		MSG.backInstructor(frm.getName()==null?"null":frm.getName().trim()),
            		true, false);

            return mapping.findForward("showEdit");
            
    	} catch (Exception e) {
    		Debug.error(e);
    		throw e;
    	}
	}
	
	/**
	 * Loads the non-editable instructor info into the form
	 * @param request
	 * @param frm
	 * @param inst
	 * @param instructorId
	 */
	private void doLoad(HttpServletRequest request, InstructorEditForm frm, DepartmentalInstructor inst, String instructorId) {
        // populate form
		frm.setInstructorId(instructorId);		

		frm.setName(
				inst.getName(UserProperty.NameFormat.get(sessionContext.getUser())) + 
    			(inst.getPositionType() == null ? "" : " (" + inst.getPositionType().getLabel() + ")"));
		
		if (inst.getExternalUniqueId() != null) {
			frm.setPuId(inst.getExternalUniqueId());
		}
				
		frm.setDeptName(inst.getDepartment().getName().trim());
		
		if (inst.getPositionType() != null) {
			frm.setPosType(inst.getPositionType().getUniqueId().toString());
		}
		
		if (inst.getCareerAcct() != null) {
			frm.setCareerAcct(inst.getCareerAcct().trim());
		}
		
		frm.setEmail(inst.getEmail());
		
		if (inst.getNote() != null) {
			frm.setNote(inst.getNote().trim());
		}
		
		try {
			DepartmentalInstructor previous = inst.getPreviousDepartmentalInstructor(sessionContext, Right.InstructorPreferences);
			frm.setPreviousId(previous==null?null:previous.getUniqueId().toString());
			DepartmentalInstructor next = inst.getNextDepartmentalInstructor(sessionContext, Right.InstructorPreferences);
			frm.setNextId(next==null?null:next.getUniqueId().toString());
		} catch (Exception e) {
			Debug.error(e);
		}
	}

}

