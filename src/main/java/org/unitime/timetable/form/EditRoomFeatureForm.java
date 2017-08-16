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
package org.unitime.timetable.form;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.unitime.timetable.model.DepartmentRoomFeature;
import org.unitime.timetable.model.GlobalRoomFeature;
import org.unitime.timetable.model.Preference;
import org.unitime.timetable.util.DynamicList;
import org.unitime.timetable.util.DynamicListObjectFactory;


/** 
 * MyEclipse Struts
 * Creation date: 05-12-2006
 * 
 * XDoclet definition:
 * @struts.form name="editRoomFeatureForm"
 *
 * @author Tomas Muller
 */
public class EditRoomFeatureForm extends ActionForm {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7728130917482276173L;
	// --------------------------------------------------------- Instance Variables
	String doit;
	String id;
	String roomLabel;
	private List globalRoomFeatureIds;
	private List departmentRoomFeatureIds;
	private List globalRoomFeatureNames;
	private List departmentRoomFeatureNames;
	private List globalRoomFeaturesEditable;
	private List departmentRoomFeaturesEditable;
	private List globalRoomFeaturesAssigned;
	private List departmentRoomFeaturesAssigned;

    // --------------------------------------------------------- Classes

    /** Factory to create dynamic list element for room groups */
    protected DynamicListObjectFactory factoryRoomFeatures = new DynamicListObjectFactory() {
        public Object create() {
            return new String(Preference.BLANK_PREF_VALUE);
        }
    };
    
	// --------------------------------------------------------- Methods

	public String getDoit() {
		return doit;
	}

	public void setDoit(String doit) {
		this.doit = doit;
	}

	/** 
	 * Method validate
	 * @param mapping
	 * @param request
	 * @return ActionErrors
	 */
	public ActionErrors validate(
		ActionMapping mapping,
		HttpServletRequest request) {

		return null;
	}

	/** 
	 * Method reset
	 * @param mapping
	 * @param request
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		globalRoomFeatureIds = DynamicList.getInstance(new ArrayList(), factoryRoomFeatures);
		departmentRoomFeatureIds = DynamicList.getInstance(new ArrayList(), factoryRoomFeatures);
		globalRoomFeatureNames = DynamicList.getInstance(new ArrayList(), factoryRoomFeatures);
		departmentRoomFeatureNames = DynamicList.getInstance(new ArrayList(), factoryRoomFeatures);
		globalRoomFeaturesEditable = DynamicList.getInstance(new ArrayList(), factoryRoomFeatures);
		departmentRoomFeaturesEditable = DynamicList.getInstance(new ArrayList(), factoryRoomFeatures);
		globalRoomFeaturesAssigned = DynamicList.getInstance(new ArrayList(), factoryRoomFeatures);
		departmentRoomFeaturesAssigned = DynamicList.getInstance(new ArrayList(), factoryRoomFeatures);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRoomLabel() {
		return roomLabel;
	}

	public void setRoomLabel(String roomLabel) {
		this.roomLabel = roomLabel;
	}

	public List getGlobalRoomFeatureIds() {
		return globalRoomFeatureIds;
	}

	public void setGlobalRoomFeatureIds(List globalRoomFeatureIds) {
		this.globalRoomFeatureIds = globalRoomFeatureIds;
	}

	public List getGlobalRoomFeatureNames() {
		return globalRoomFeatureNames;
	}

	public void setGlobalRoomFeatureNames(List globalRoomFeatureNames) {
		this.globalRoomFeatureNames = globalRoomFeatureNames;
	}

	public List getGlobalRoomFeaturesEditable() {
		return globalRoomFeaturesEditable;
	}

	public void setGlobalRoomFeaturesEditable(List globalRoomFeaturesEditable) {
		this.globalRoomFeaturesEditable = globalRoomFeaturesEditable;
	}

    public String getGlobalRoomFeaturesEditable(int key) {
        return globalRoomFeaturesEditable.get(key).toString();
    }
    
    public void setGlobalRoomFeaturesEditable(int key, Object value) {
        this.globalRoomFeaturesEditable.set(key, value);
    }
    
	public List getDepartmentRoomFeatureIds() {
		return departmentRoomFeatureIds;
	}

	public void setDepartmentRoomFeatureIds(List departmentRoomFeatureIds) {
		this.departmentRoomFeatureIds = departmentRoomFeatureIds;
	}

	public List getDepartmentRoomFeatureNames() {
		return departmentRoomFeatureNames;
	}

	public void setDepartmentRoomFeatureNames(List departmentRoomFeatureNames) {
		this.departmentRoomFeatureNames = departmentRoomFeatureNames;
	}

	public List getDepartmentRoomFeaturesEditable() {
		return departmentRoomFeaturesEditable;
	}

	public void setDepartmentRoomFeaturesEditable(List departmentRoomFeaturesEditable) {
		this.departmentRoomFeaturesEditable = departmentRoomFeaturesEditable;
	}
	
    public String getdepartmentRoomFeaturesEditable(int key) {
        return departmentRoomFeaturesEditable.get(key).toString();
    }
    
    public void setdepartmentRoomFeaturesEditable(int key, Object value) {
        this.departmentRoomFeaturesEditable.set(key, value);
    }
	
	public void addToGlobalRoomFeatures(GlobalRoomFeature rf, Boolean editable, Boolean assigned) {
		this.globalRoomFeatureIds.add(rf.getUniqueId().toString());
		this.globalRoomFeatureNames.add(rf.getLabel() + (rf.getFeatureType() == null ? "" : " (" + rf.getFeatureType().getLabel() + ")"));
		this.globalRoomFeaturesEditable.add(editable);
		this.globalRoomFeaturesAssigned.add(assigned);
	}
	
	public void addToDepartmentRoomFeatures(DepartmentRoomFeature rf, Boolean editable, Boolean assigned) {
		this.departmentRoomFeatureIds.add(rf.getUniqueId().toString());
		this.departmentRoomFeatureNames.add(rf.getLabel()+" ("+(rf.getDepartment().isExternalManager().booleanValue()?rf.getDepartment().getExternalMgrLabel():rf.getDepartment().getDeptCode()+" - "+rf.getDepartment().getName())+(rf.getFeatureType() == null ? "" : ", " + rf.getFeatureType().getLabel())+")");
		this.departmentRoomFeaturesEditable.add(editable);
		this.departmentRoomFeaturesAssigned.add(assigned);
	}

	public List getGlobalRoomFeaturesAssigned() {
		return globalRoomFeaturesAssigned;
	}

	public void setGlobalRoomFeaturesAssigned(List globalRoomFeaturesAssigned) {
		this.globalRoomFeaturesAssigned = globalRoomFeaturesAssigned;
	}

	public List getDepartmentRoomFeaturesAssigned() {
		return departmentRoomFeaturesAssigned;
	}

	public void setDepartmentRoomFeaturesAssigned(List departmentRoomFeaturesAssigned) {
		this.departmentRoomFeaturesAssigned = departmentRoomFeaturesAssigned;
	}

}

