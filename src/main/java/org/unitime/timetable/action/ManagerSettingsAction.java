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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.util.MessageResources;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.form.ManagerSettingsForm;
import org.unitime.timetable.model.Settings;
import org.unitime.timetable.model.dao.SettingsDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;


/** 
 * MyEclipse Struts
 * Creation date: 10-17-2005
 * 
 * XDoclet definition:
 * @struts:action path="/managerSettings" name="managerSettingsForm" input="/user/managerSettings.jsp" scope="request"
 *
 * @author Tomas Muller
 */
@Service("/managerSettings")
public class ManagerSettingsAction extends Action {
	
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
        
        // Check Access
    	sessionContext.checkPermission(Right.SettingsUser);
        
		MessageResources rsc = getResources(request);
        ManagerSettingsForm frm = (ManagerSettingsForm) form;
        String op = frm.getOp();
 
        if(op==null) {
            op = request.getParameter("op");
            if(op==null) {
                frm.setOp("List");
	            op = "List";
            }
        }
        
        // Reset Form
        if(op.equals(rsc.getMessage("button.cancelUpdateSetting"))) {
            frm.reset(mapping, request);
            frm.setOp("List");
        }

        // Edit - Load setting with allowed values for user to update
        if(op.equals("Edit")) {
            // Load Settings object
            Settings s = SettingsDAO.getInstance().get(Long.valueOf(request.getParameter("id")));

            // Set Form values
            frm.setOp("Edit");            
            frm.setAllowedValues(s.getAllowedValues());
            frm.setKey(s.getKey());
            frm.setName(s.getDescription());
            frm.setDefaultValue(s.getDefaultValue());
            frm.setValue(sessionContext.getUser().getProperty(s.getKey(), s.getDefaultValue()));

            return mapping.findForward("editManagerSettings");
        }
 
        // Save changes made by the user
        if(op.equals(rsc.getMessage("button.updateSetting"))) {
            ActionMessages errors = frm.validate(mapping, request);
            if(errors.size()>0) {
                saveErrors(request, errors);
                frm.setOp("Edit");
            } else {
            	sessionContext.getUser().setProperty(frm.getKey(), frm.getValue());
    	    }
        }

        // Read all existing settings and store in request
        getSettingsList(request);        
        return mapping.findForward("showManagerSettings");
    }

    /**
     * Retrieve all existing defined settings
     * @param request Request object
     * @throws Exception
     */
    private void getSettingsList(HttpServletRequest request) throws Exception {
        WebTable.setOrder(sessionContext,"managerSettings.ord",request.getParameter("ord"),1);

		// Create web table instance 
        WebTable webTable = new WebTable( 2,
			    "Manager Settings", "managerSettings.do?ord=%%",
			    new String[] {"Setting", "Value"},
			    new String[] {"left", "left"},
			    null );
        
        for (Settings s: SettingsDAO.getInstance().findAll(Order.asc("key"))) {
        	String onClick = "onClick=\"document.location='managerSettings.do?op=Edit&id=" + s.getUniqueId() + "';\"";
        	String value = sessionContext.getUser().getProperty(s.getKey(), s.getDefaultValue());
        	webTable.addLine(onClick, new String[] {s.getDescription(), value}, new String[] {s.getDescription(), value});
        }

	    request.setAttribute("table", webTable.printTable(WebTable.getOrder(sessionContext,"managerSettings.ord")));
    }   
    
}
