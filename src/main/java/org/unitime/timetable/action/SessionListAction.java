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

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.HibernateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.unitime.commons.web.WebTable;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.security.SessionContext;
import org.unitime.timetable.security.context.UniTimeUserContext;
import org.unitime.timetable.security.rights.Right;
import org.unitime.timetable.util.Formats;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;


/** 
 * MyEclipse Struts
 * Creation date: 02-18-2005
 * 
 * XDoclet definition:
 * @struts:action path="/sessionList" name="sessionListForm" input="/admin/sessionList.jsp" scope="request" validate="true"
 *
 * @author Tomas Muller
 */
@Service("/sessionList")
public class SessionListAction extends Action {
	
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
	 * @throws HibernateException
	 */
	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response) throws Exception {

        // Check access
		sessionContext.checkPermission(Right.AcademicSessions);

		WebTable webTable = new WebTable(
				11, "", "sessionList.do?order=%%",					
				new String[] {
					"Default", "Academic<br>Session", "Academic<br>Initiative", "Session<br>Begins",
					"Classes<br>End", "Session<br>Ends", "Exams<br>Begins", "Date<br>Pattern", "Status", "Class<br>Duration", 
					"Events<br>Begins", "Events<br>Ends", "<br>Enrollment", "Deadline<br>Change", "<br>Drop", "Sectioning<br>Status" },
				new String[] { "center", "left", "left", "left", "left",
					"left", "left", "left", "left", "right", "left", "left", "left", "left", "left", "left" }, 
				new boolean[] { true, true, true, false, false, false, true, false, true, true, true, true, true, true, true });
		
		Formats.Format<Date> df = Formats.getDateFormat(Formats.Pattern.SESSION_DATE);
		
		TreeSet<Session> sessions = new TreeSet<Session>(SessionDAO.getInstance().findAll());
		Session defaultSession = UniTimeUserContext.defaultSession(sessions, sessionContext.getUser().getCurrentAuthority());

		for (Session s: SessionDAO.getInstance().findAll()) {
			Calendar ce = Calendar.getInstance(Locale.US); ce.setTime(s.getSessionBeginDateTime());
			ce.add(Calendar.WEEK_OF_YEAR, s.getLastWeekToEnroll()); ce.add(Calendar.DAY_OF_YEAR, -1);

			Calendar cc = Calendar.getInstance(Locale.US); cc.setTime(s.getSessionBeginDateTime());
			cc.add(Calendar.WEEK_OF_YEAR, s.getLastWeekToChange()); cc.add(Calendar.DAY_OF_YEAR, -1);

			Calendar cd = Calendar.getInstance(Locale.US); cd.setTime(s.getSessionBeginDateTime());
			cd.add(Calendar.WEEK_OF_YEAR, s.getLastWeekToDrop()); cd.add(Calendar.DAY_OF_YEAR, -1);
			
			webTable.addLine(
					sessionContext.hasPermission(s, Right.AcademicSessionEdit) ?  "onClick=\"document.location='sessionEdit.do?doit=editSession&sessionId=" + s.getSessionId() + "';\"" : null,
					new String[] {
						s.equals(defaultSession) ? "<img src='images/accept.png'> " : "&nbsp; ", 
						s.getAcademicTerm() + " " + s.getSessionStartYear(),
						s.academicInitiativeDisplayString(),
						df.format(s.getSessionBeginDateTime()).replace(" ", "&nbsp;"),
						df.format(s.getClassesEndDateTime()).replace(" ", "&nbsp;"),
						df.format(s.getSessionEndDateTime()).replace(" ", "&nbsp;"),
						(s.getExamBeginDate()==null?"N/A":df.format(s.getExamBeginDate()).replace(" ", "&nbsp;")),
						s.getDefaultDatePattern()!=null ? s.getDefaultDatePattern().getName() : "-", 
						s.statusDisplayString(),
						s.getDefaultClassDurationType() == null ? "&nbsp;" : s.getDefaultClassDurationType().getAbbreviation(),
						(s.getEventBeginDate()==null?"N/A":df.format(s.getEventBeginDate()).replace(" ", "&nbsp;")),
						(s.getEventEndDate()==null?"N/A":df.format(s.getEventEndDate()).replace(" ", "&nbsp;")),
						df.format(ce.getTime()).replace(" ", "&nbsp;"),
						df.format(cc.getTime()).replace(" ", "&nbsp;"),
						df.format(cd.getTime()).replace(" ", "&nbsp;"),
						(s.getDefaultSectioningStatus() == null ? "&nbsp;" : s.getDefaultSectioningStatus().getReference()),
						 },
					new Comparable[] {
						s.equals(defaultSession) ? "<img src='images/accept.png'>" : "",
						s.getLabel(),
						s.academicInitiativeDisplayString(),
						s.getSessionBeginDateTime(),
						s.getClassesEndDateTime(),
						s.getSessionEndDateTime(),
						s.getExamBeginDate(),
						s.getDefaultDatePattern()!=null ? s.getDefaultDatePattern().getName() : "-", 
						s.statusDisplayString(),
						s.getDefaultClassDurationType() == null ? " " : s.getDefaultClassDurationType().getAbbreviation(),
						s.getEventBeginDate(),
						s.getEventEndDate(),
						ce.getTime(), cc.getTime(), cd.getTime(),
						(s.getDefaultSectioningStatus() == null ? " " : s.getDefaultSectioningStatus().getReference()) } );
		}
				
		webTable.enableHR("#9CB0CE");
		
		int orderCol = 4;
		if (request.getParameter("order")!=null) {
			try {
				orderCol = Integer.parseInt(request.getParameter("order"));
			} catch (Exception e){
				orderCol = 4;
			}
		}
		request.setAttribute("table", webTable.printTable(orderCol));
		
		return mapping.findForward("showSessionList");
		
	}

}
