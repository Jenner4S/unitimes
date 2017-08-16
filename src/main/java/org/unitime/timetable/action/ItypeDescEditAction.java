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
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.commons.Debug;
import org.unitime.timetable.form.ItypeDescEditForm;
import org.unitime.timetable.model.ItypeDesc;
import org.unitime.timetable.model.dao.ItypeDescDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.util.LookupTables;


/** 
 * @author Tomas Muller
 */
@Service("/itypeDescEdit")
public class ItypeDescEditAction extends Action {
	
	@Autowired SessionContext sessionContext;

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
		ItypeDescEditForm myForm = (ItypeDescEditForm) form;
		
        // Check Access
		sessionContext.checkPermission(Right.InstructionalTypes);
        
        // Read operation to be performed
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));

        if (op==null) {
            myForm.reset(mapping, request);
            myForm.setOp("Save");
        }
        
        // Return
        if ("Back".equals(op)) {
            return mapping.findForward("back");
        }
        
        if ("Add IType".equals(op)) {
        	
        	sessionContext.checkPermission(Right.InstructionalTypeAdd);
        	
            myForm.setOp("Save");
        }

        LookupTables.setupItypes(request, true);
        
        // Add / Update
        if ("Update".equals(op) || "Save".equals(op)) {
            // Validate input
            ActionMessages errors = myForm.validate(mapping, request);
            if(errors.size()>0) {
                saveErrors(request, errors);
                return mapping.findForward("Save".equals(op)?"add":"edit");
            } else {
        		Transaction tx = null;
        		
        		if (myForm.getUniqueId() == null || myForm.getUniqueId() < 0)
        			sessionContext.checkPermission(Right.InstructionalTypeAdd);
        		else
        			sessionContext.checkPermission(myForm.getUniqueId(), "ItypeDesc", Right.InstructionalTypeEdit);
        		
                try {
                	org.hibernate.Session hibSession = (new ItypeDescDAO()).getSession();
                	if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
                		tx = hibSession.beginTransaction();
                	
                	myForm.saveOrUpdate(hibSession);
                	
        			if (tx!=null) tx.commit();
        	    } catch (Exception e) {
        	    	if (tx!=null) tx.rollback();
        	    	throw e;
        	    }

                return mapping.findForward("back");
            }
        }

        // Edit
        if("Edit".equals(op)) {
            String id = request.getParameter("id");
            
            sessionContext.checkPermission(Integer.valueOf(id), "ItypeDesc", Right.InstructionalTypeEdit);
            
            ActionMessages errors = new ActionMessages();
            if(id==null || id.trim().length()==0) {
                errors.add("externalId", new ActionMessage("errors.invalid", id));
                saveErrors(request, errors);
                return mapping.findForward("edit");
            } else {
                ItypeDesc itype = new ItypeDescDAO().get(Integer.valueOf(id));
            	
                if (itype==null) {
                    return mapping.findForward("back");
                } else {
                	myForm.load(itype);
                }
            }
        }

        // Delete 
        if("Delete".equals(op)) {
    		Transaction tx = null;
    		
    		sessionContext.checkPermission(myForm.getUniqueId(), "ItypeDesc", Right.InstructionalTypeDelete);
    		
            try {
            	org.hibernate.Session hibSession = (new ItypeDescDAO()).getSession();
            	if (hibSession.getTransaction()==null || !hibSession.getTransaction().isActive())
            		tx = hibSession.beginTransaction();
            	
            	myForm.delete(hibSession);
            	
    			tx.commit();
    	    } catch (Exception e) {
    	    	if (tx!=null) tx.rollback();
    	    	throw e;
    	    }

            return mapping.findForward("back");
        }
        
        return mapping.findForward("Save".equals(myForm.getOp())?"add":"edit");
		} catch (Exception e) {
			Debug.error(e);
			throw e;
		}
	}
}

