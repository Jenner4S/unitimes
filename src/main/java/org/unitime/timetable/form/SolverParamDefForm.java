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

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.unitime.timetable.model.SolverParameterDef;
import org.unitime.timetable.model.SolverParameterGroup;


/** 
 * @author Tomas Muller
 */
public class SolverParamDefForm extends ActionForm {
	private static final long serialVersionUID = -1210805253091416573L;
	private Long uniqueId;
	private String type;
	private String description;
	private Boolean visible;
	private String defaultValue;
	private int order;
	private String name;
	private String group;
	private String op;
	private String[] groups;

	public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = new ActionErrors();
        
        if(name==null || name.trim().length()==0)
            errors.add("name", new ActionMessage("errors.required", ""));
        else {
        	if ("Save".equals(op)) {
        		SolverParameterDef def = SolverParameterDef.findByNameGroup(name, group);
        		if (def!=null)
        			errors.add("name", new ActionMessage("errors.exists", name));
        	}
        }
        
        if(group==null || group.trim().length()==0)
            errors.add("group", new ActionMessage("errors.required", ""));

        if(type==null || type.trim().length()==0)
            errors.add("type", new ActionMessage("errors.required", ""));

        if(description==null || description.trim().length()==0)
            errors.add("description", new ActionMessage("errors.required", ""));
        
        return errors;
	}

	public void reset(ActionMapping mapping, HttpServletRequest request) {
		op="List";
		uniqueId=null;
		type="";
		description="";
		name="";
		defaultValue="";
		order=-1;
		group=null;
		visible=Boolean.FALSE;
		groups = SolverParameterGroup.getGroupNames();
	}

    public String getOp() { return op; }
    public void setOp(String op) { this.op = op; }
	public Long getUniqueId() { return uniqueId; }
	public void setUniqueId(Long uniqueId) { this.uniqueId = uniqueId; }
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	public Boolean getVisible() { return visible; }
	public void setVisible(Boolean visible) { this.visible = visible; }
	public String getDefault() { return defaultValue; }
	public void setDefault(String defaultValue) { this.defaultValue = defaultValue; }
	public int getOrder() { return order; }
	public void setOrder(int order) { this.order = order; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public void setGroup(String group) { this.group = group ; }
	public String getGroup() { return this.group; }
	public String[] getGroups() { return this.groups; }

}
