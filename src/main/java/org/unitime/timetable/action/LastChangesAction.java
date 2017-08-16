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

import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.form.LastChangesForm;
import org.unitime.timetable.model.ChangeLog;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.util.ExportUtils;
import org.unitime.timetable.webutil.PdfWebTable;


/** 
 * @author Tomas Muller
 */
@Service("/lastChanges")
public class LastChangesAction extends Action {
	
	@Autowired SessionContext sessionContext;

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        LastChangesForm myForm = (LastChangesForm) form;
		
        // Check Access
        sessionContext.checkPermission(Right.LastChanges);

        // Read operation to be performed
        String op = (myForm.getOp()!=null?myForm.getOp():request.getParameter("op"));

        if ("Apply".equals(op) || "Export PDF".equals(op)) {
        	myForm.save(request);
        } else {
            myForm.load(request);
        }
        
        Long sessionId = sessionContext.getUser().getCurrentAcademicSessionId();
        
        request.setAttribute("departments",Department.findAll(sessionId));
        request.setAttribute("subjAreas",new TreeSet(SubjectArea.getSubjectAreaList(sessionId)));
        request.setAttribute("managers",TimetableManager.getManagerList());
        
        WebTable.setOrder(sessionContext,"lastChanges.ord2",request.getParameter("ord"),1);
        
        WebTable webTable = new WebTable( 7, "Last Changes",
                "lastChanges.do?ord=%%",
                new String[] {"Date", "Department", "Subject", "Manager", "Page", "Object", "Operation"},
                new String[] {"left", "left", "left", "left", "left", "left", "left"},
                new boolean[] { false, true, true, true, true, true, true} );
        
        List changes = ChangeLog.findLastNChanges(
                sessionId, 
                (myForm.getManagerId()==null || myForm.getManagerId().longValue()<0?null:myForm.getManagerId()), 
                (myForm.getSubjAreaId()==null || myForm.getSubjAreaId().longValue()<0?null:myForm.getSubjAreaId()),
                (myForm.getDepartmentId()==null || myForm.getDepartmentId().longValue()<0?null:myForm.getDepartmentId()),
                myForm.getN());
        
        if (changes!=null) {
            for (Iterator i=changes.iterator();i.hasNext();)
                printLastChangeTableRow(request, webTable, (ChangeLog)i.next(), true);
        }
        
        request.setAttribute("table", webTable.printTable(WebTable.getOrder(sessionContext,"lastChanges.ord2")));
        
        if ("Export PDF".equals(op) && changes!=null) {
            PdfWebTable pdfTable = new PdfWebTable( 7, "Last Changes",
                    "lastChanges.do?ord=%%",
                    new String[] {"Date", "Department", "Subject", "Manager", "Page", "Object", "Operation"},
                    new String[] {"left", "left", "left", "left", "left", "left", "left"},
                    new boolean[] { false, true, true, true, true, true, true} );
            for (Iterator i=changes.iterator();i.hasNext();)
                printLastChangeTableRow(request, pdfTable, (ChangeLog)i.next(), false);
            
            ExportUtils.exportPDF(pdfTable, WebTable.getOrder(sessionContext,"lastChanges.ord2"), response, "lastChanges");
            return null;
        }
        
        return mapping.findForward("display");
	}
    
    private int printLastChangeTableRow(HttpServletRequest request, WebTable webTable, ChangeLog lastChange, boolean html) {
        if (lastChange==null) return 0;
        webTable.addLine(null,
                new String[] {
                    ChangeLog.sDF.format(lastChange.getTimeStamp()),
                    (lastChange.getDepartment()==null?"":
                        (html?
                                "<span title='"+lastChange.getDepartment().getHtmlTitle()+"'>"+
                                lastChange.getDepartment().getShortLabel()+
                                "</span>":lastChange.getDepartment().getShortLabel())),
                    (lastChange.getSubjectArea()==null?"":lastChange.getSubjectArea().getSubjectAreaAbbreviation()),
                    (html?lastChange.getManager().getShortName():lastChange.getManager().getShortName().replaceAll("&nbsp;"," ")),
                    lastChange.getSourceTitle(),
                    lastChange.getObjectTitle(),
                    lastChange.getOperationTitle()
                    },
                new Comparable[] {
                    new Long(lastChange.getTimeStamp().getTime()),
                    (lastChange.getDepartment()==null?"":lastChange.getDepartment().getDeptCode()),
                    (lastChange.getSubjectArea()==null?"":lastChange.getSubjectArea().getSubjectAreaAbbreviation()),
                    lastChange.getManager().getName(),
                    lastChange.getSourceTitle(), //new Integer(lastChange.getSource().ordinal()),
                    lastChange.getObjectTitle(),
                    new Integer(lastChange.getOperation().ordinal())
                    });
        return 1;
    }
    
}

