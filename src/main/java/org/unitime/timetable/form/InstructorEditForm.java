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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.util.LabelValueBean;
import org.unitime.localization.impl.Localization;
import org.unitime.localization.messages.CourseMessages;
import org.unitime.timetable.interfaces.ExternalUidLookup.UserInfo;
import org.unitime.timetable.model.PositionType;
import org.unitime.timetable.model.PreferenceLevel;
import org.unitime.timetable.util.IdValue;


/** 
 * MyEclipse Struts
 * Creation date: 10-20-2005
 * 
 * XDoclet definition:
 * @struts:form name="instructorEditForm"
 *
 * @author Tomas Muller, Zuzana Mullerova
 */
public class InstructorEditForm extends PreferencesForm  {

	protected final static CourseMessages MSG = Localization.create(CourseMessages.class);
	
	// --------------------------------------------------------- Instance Variables

	/**
	 * 
	 */
	private static final long serialVersionUID = 7234507709430023477L;

	/** deptCode property */
	private String deptCode;

	/** instructorId property */
	private String instructorId;

	/** puId property */
	private String puId;

	/** name property */
	private String name;
	
	private String careerAcct;
	private String posType;
	private String note;
	private boolean displayPrefs;
	private String lname;
	private String mname;
	private String fname;
	private String title;
	private String deptName;
	private String email;

	private String searchSelect;
	private UserInfo i2a2Match;
	private Collection staffMatch;
	private Boolean matchFound;
	
	private String screenName;

	private String prevId;
	private String nextId;
    
    private boolean ignoreDist;
	private Boolean lookupEnabled;
	
	private String maxLoad;
	private String teachingPreference;
	private Map<Long, Boolean> attributes;
	private List<IdValue> departments;
    
	// --------------------------------------------------------- Methods
    
    public boolean getIgnoreDist() {
        return ignoreDist;
    }
    
    public void setIgnoreDist(boolean ignoreDist) {
        this.ignoreDist = ignoreDist; 
    }

	public String getDeptName() {
		return deptName;
	}

	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}
	
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }

	public String getFname() {
		return fname;
	}

	public void setFname(String fname) {
		this.fname = fname;
	}
	
	public String getTitle() { return title; }
	public void setTitle(String title) { this.title = title; }

	public String getLname() {
		return lname;
	}

	public void setLname(String lname) {
		this.lname = lname;
	}

	public String getMname() {
		return mname;
	}

	public void setMname(String mname) {
		this.mname = mname;
	}

	public String getCareerAcct() {
		return careerAcct;
	}

	public void setCareerAcct(String careerAcct) {
		this.careerAcct = careerAcct;
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public String getPosType() {
		return posType;
	}

	public void setPosType(String posCode) {
		this.posType = posCode;
	}

    public String getScreenName() {
        return screenName;
    }
    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }
    
    public String getTeachingPreference() { return teachingPreference; }
    public void setTeachingPreference(String teachingPref) { this.teachingPreference = teachingPref; }
    
    public String getMaxLoad() { return maxLoad; }
    public void setMaxLoad(String maxLoad) { this.maxLoad = maxLoad; }
    
    public boolean getAttribute(long attributeId) {
    	if (attributes == null) return false;
    	Boolean value = attributes.get(attributeId);
    	return (value == null ? false : value.booleanValue());
    }
    
    public void setAttribute(long attributeId, Boolean value) {
    	if (attributes == null) attributes = new HashMap<Long, Boolean>();
    	attributes.put(attributeId, value == null ? false : value.booleanValue());
    }

    public boolean getAttribute(String attributeId) {
    	if (attributes == null) return false;
    	Boolean value = attributes.get(Long.valueOf(attributeId));
    	return (value == null ? false : value.booleanValue());
    }
    
    public void setAttribute(String attributeId, boolean value) {
    	if (attributes == null) attributes = new HashMap<Long, Boolean>();
    	attributes.put(Long.valueOf(attributeId), value);
    }
    
    public void clearAttributes() {
    	attributes = null;
    }
    
	/** 
	 * Method reset
	 * @param mapping
	 * @param request
	 */
	public void reset(ActionMapping mapping, HttpServletRequest request) {
		instructorId = "";
		screenName = "instructor";
		super.reset(mapping, request);
		
        //Set request attributes
        setPosType(request);
        prevId = nextId = null;
        ignoreDist = false;
        email = null;
        teachingPreference = PreferenceLevel.sProhibited;
        maxLoad = null;
        attributes = null;
	}
	
	/**
	 * 
	 * @param request
	 */
	private void setPosType(HttpServletRequest request) {
		ArrayList list = new ArrayList();
		
		for (PositionType pt: PositionType.getPositionTypeList()) {
			list.add(new LabelValueBean(pt.getLabel().trim(), pt.getUniqueId().toString()));
		}
		
		request.setAttribute(PositionType.POSTYPE_ATTR_NAME, list);
		
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
		
        ActionErrors errors = new ActionErrors();

        if (op.equals(MSG.actionLookupInstructor())) {
    		if ( (fname==null || fname.trim().length()==0) 
    		        && (lname==null || lname.trim().length()==0) 
    		        && (careerAcct==null || careerAcct.trim().length()==0) ) {
				errors.add("fname", 
	                    new ActionMessage("errors.generic", MSG.errorSupplyInfoForInstructorLookup()) );
    		}
    		
    		return errors;
        }
        
        if (!screenName.equalsIgnoreCase("instructorPref") ) {
					
			if (lname == null || lname.trim().equals("")) {
				errors.add("Last Name", 
	                    new ActionMessage("errors.generic", MSG.errorRequiredLastName()) );
			}
        }
        
		if (errors.size() == 0) {
			return super.validate(mapping, request); 
		} else {
			return errors;
		}
	}

	/** 
	 * Returns the instructorId.
	 * @return String
	 */
	public String getInstructorId() {
		return instructorId;
	}

	/** 
	 * Set the instructorId.
	 * @param instructorId The instructorId to set
	 */
	public void setInstructorId(String instructorId) {
		this.instructorId = instructorId;
	}

	/** 
	 * Returns the puId.
	 * @return String
	 */
	public String getPuId() {
		return puId;
	}

	/** 
	 * Set the puId.
	 * @param puId The puId to set
	 */
	public void setPuId(String puId) {
		this.puId = puId;
	}

	/** 
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/** 
	 * Set the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return
	 */
	public String getDeptCode() {
		return deptCode;
	}

	/**
	 * 
	 * @param deptCode
	 */
	public void setDeptCode(String deptCode) {
		this.deptCode = deptCode;
	}

	public boolean isDisplayPrefs() {
		return displayPrefs;
	}

	public void setDisplayPrefs(boolean displayPrefs) {
		this.displayPrefs = displayPrefs;
	}	
	
    public UserInfo getI2a2Match() {
        return i2a2Match;
    }
    public void setI2a2Match(UserInfo match) {
        i2a2Match = match;
    }
    
    public Collection getStaffMatch() {
        return staffMatch;
    }
    public void setStaffMatch(Collection staffMatch) {
        this.staffMatch = staffMatch;
    }
    
    public Boolean getMatchFound() {
        return matchFound;
    }
    public void setMatchFound(Boolean matchFound) {
        this.matchFound = matchFound;
    }
        
    public String getSearchSelect() {
        return searchSelect;
    }
    public void setSearchSelect(String searchSelect) {
        this.searchSelect = searchSelect;
    }
        
    public void setPreviousId(String prevId) {
    	this.prevId = prevId;
    }
    public String getPreviousId() {
    	return prevId;
    }
    
    public void setNextId(String nextId) {
    	this.nextId = nextId;
    }
    public String getNextId() {
    	return nextId;
    }

	public Boolean getLookupEnabled() {
		return lookupEnabled;
	}

	public void setLookupEnabled(Boolean lookupEnabled) {
		this.lookupEnabled = lookupEnabled;
	}
	
	public List<IdValue> getDepartments() { return departments; }
	public void setDepartments(List<IdValue> departments) { this.departments = departments; }
}

