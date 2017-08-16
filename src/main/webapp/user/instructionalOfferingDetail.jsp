<%--
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
--%>
<%@ page import="org.unitime.timetable.defaults.SessionAttribute"%>
<%@ page import="org.unitime.timetable.util.Constants" %>
<%@ page import="org.unitime.timetable.model.DistributionPref" %>
<%@ page import="org.unitime.timetable.webutil.JavascriptFunctions" %>
<%@ page import="org.unitime.timetable.webutil.WebInstrOfferingConfigTableBuilder"%>
<%@ page import="org.unitime.timetable.form.InstructionalOfferingDetailForm"%>
<%@ page import="org.unitime.timetable.solver.WebSolver"%>
<%@ page import="org.unitime.timetable.model.CourseOffering" %>
<%@ page import="org.unitime.timetable.model.Reservation" %>

<%@ taglib uri="http://struts.apache.org/tags-bean" prefix="bean" %>
<%@ taglib uri="http://struts.apache.org/tags-html" prefix="html" %>
<%@ taglib uri="http://struts.apache.org/tags-logic" prefix="logic" %>
<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://www.unitime.org/tags-custom" prefix="tt" %>
<%@ taglib uri="http://www.unitime.org/tags-localization" prefix="loc" %> 
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<tiles:importAttribute />
<tt:session-context/>
<% 
	String frmName = "instructionalOfferingDetailForm";
	InstructionalOfferingDetailForm frm = (InstructionalOfferingDetailForm) request.getAttribute(frmName);

	String crsNbr = (String)sessionContext.getAttribute(SessionAttribute.OfferingsCourseNumber);
%>
<loc:bundle name="CourseMessages">
<SCRIPT language="javascript">
	<!--
		<%= JavascriptFunctions.getJsConfirm(sessionContext) %>
		
		function confirmMakeOffered() {
			if (jsConfirm!=null && !jsConfirm)
				return true;

			if (!confirm('<%=MSG.confirmMakeOffered() %>')) {
				return false;
			}

			return true;
		}

		function confirmMakeNotOffered() {
			if (jsConfirm!=null && !jsConfirm)
				return true;
				
			if (!confirm('<%=MSG.confirmMakeNotOffered() %>')) {
				return false;
			}
			
			return true;
		}
		
		function confirmDelete() {
			if (jsConfirm!=null && !jsConfirm)
				return true;

			if (!confirm('<%=MSG.confirmDeleteIO() %>')) {
				return false;
			}

			return true;
		}

	// -->
</SCRIPT>

	<bean:define name="instructionalOfferingDetailForm" property="instrOfferingName" id="instrOfferingName"/>
	<TABLE width="100%" border="0" cellspacing="0" cellpadding="3">
		<TR>
			<TD valign="middle" colspan='2'>
				<html:form action="/instructionalOfferingDetail" styleClass="FormWithNoPadding">
					<input type='hidden' name='confirm' value='y'/>
					<html:hidden property="instrOfferingId"/>	
					<html:hidden property="nextId"/>
					<html:hidden property="previousId"/>
					<html:hidden property="catalogLinkLabel"/>
					<html:hidden property="catalogLinkLocation"/>
					
				<tt:section-header>
					<tt:section-title>
							<A  title="<%=MSG.titleBackToIOList(MSG.accessBackToIOList()) %>" 
								accesskey="<%=MSG.accessBackToIOList() %>"
								class="l8" 
								href="instructionalOfferingShowSearch.do?doit=Search&subjectAreaId=<bean:write name="instructionalOfferingDetailForm" property="subjectAreaId" />&courseNbr=<%=crsNbr%>#A<bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" />"
							><bean:write name="instructionalOfferingDetailForm" property="instrOfferingName" /></A> 
					</tt:section-title>						
					<bean:define id="instrOfferingId">
						<bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" />				
					</bean:define>
					<bean:define id="subjectAreaId">
						<bean:write name="instructionalOfferingDetailForm" property="subjectAreaId" />				
					</bean:define>
				 
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingCanLock')">
						<html:submit property="op" styleClass="btn" 
								accesskey="<%=MSG.accessLockIO() %>" 
								title="<%=MSG.titleLockIO(MSG.accessLockIO()) %>"
								onclick="<%=MSG.jsSubmitLockIO((String)instrOfferingName)%>">
							<loc:message name="actionLockIO"/>
						</html:submit>
					</sec:authorize>
					 <sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingCanUnlock')">
						<html:submit property="op" styleClass="btn" 
								accesskey="<%=MSG.accessUnlockIO() %>" 
								title="<%=MSG.titleUnlockIO(MSG.accessUnlockIO()) %>"
								onclick="<%=MSG.jsSubmitUnlockIO((String)instrOfferingName)%>">
							<loc:message name="actionUnlockIO"/>
						</html:submit>
					</sec:authorize>

					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'InstrOfferingConfigAdd')">
							<html:submit property="op" 
									styleClass="btn" 
									accesskey="<%=MSG.accessAddConfiguration() %>" 
									title="<%=MSG.titleAddConfiguration(MSG.accessAddConfiguration()) %>">
								<loc:message name="actionAddConfiguration" />
							</html:submit>
					</sec:authorize>
					
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'InstructionalOfferingCrossLists')">
							<html:submit property="op" 
									styleClass="btn" 
									accesskey="<%=MSG.accessCrossLists() %>" 
									title="<%=MSG.titleCrossLists(MSG.accessCrossLists()) %>">
								<loc:message name="actionCrossLists" />
							</html:submit>
					</sec:authorize>

					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingMakeOffered')">
							<html:submit property="op" 
									onclick="return confirmMakeOffered();"
									styleClass="btn" 
									accesskey="<%=MSG.accessMakeOffered() %>" 
									title="<%=MSG.titleMakeOffered(MSG.accessMakeOffered()) %>">
								<loc:message name="actionMakeOffered" />
							</html:submit>
					</sec:authorize>
					
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingDelete')">
							<html:submit property="op" 
									onclick="return confirmDelete();"
									styleClass="btn" 
									accesskey="<%=MSG.accessDeleteIO() %>" 
									title="<%=MSG.titleDeleteIO(MSG.accessDeleteIO()) %>">
								<loc:message name="actionDeleteIO" />
							</html:submit>
					</sec:authorize>
					
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingMakeNotOffered')">
							<html:submit property="op" 
									onclick="return confirmMakeNotOffered();"
									styleClass="btn" 
									accesskey="<%=MSG.accessMakeNotOffered() %>"
									title="<%=MSG.titleMakeNotOffered(MSG.accessMakeNotOffered()) %>">
								<loc:message name="actionMakeNotOffered" />
							</html:submit>
					</sec:authorize>
									
					<logic:notEmpty name="instructionalOfferingDetailForm" property="previousId">
						<html:submit property="op" 
								styleClass="btn" 
								accesskey="<%=MSG.accessPreviousIO() %>" 
								title="<%=MSG.titlePreviousIO(MSG.accessPreviousIO()) %>">
							<loc:message name="actionPreviousIO" />
						</html:submit> 
					</logic:notEmpty>
					<logic:notEmpty name="instructionalOfferingDetailForm" property="nextId">
						<html:submit property="op" 
								styleClass="btn" 
								accesskey="<%=MSG.accessNextIO() %>" 
								title="<%=MSG.titleNextIO(MSG.accessNextIO()) %>">
							<loc:message name="actionNextIO" />
						</html:submit> 
					</logic:notEmpty>

					<tt:back styleClass="btn" 
							name="<%=MSG.actionBackIODetail() %>" 
							title="<%=MSG.titleBackIODetail(MSG.accessBackIODetail()) %>" 
							accesskey="<%=MSG.accessBackIODetail() %>" 
							type="InstructionalOffering">
						<bean:write name="instructionalOfferingDetailForm" property="instrOfferingId"/>
					</tt:back>
				</tt:section-header>					
				
				</html:form>
			</TD>
		</TR>		

		<logic:messagesPresent>
		<TR>
			<TD colspan="2" align="left" class="errorCell">
					<B><U><loc:message name="errors"/></U></B><BR>
				<BLOCKQUOTE>
				<UL>
				    <html:messages id="error">
				      <LI>
						${error}
				      </LI>
				    </html:messages>
			    </UL>
			    </BLOCKQUOTE>
			</TD>
		</TR>
		</logic:messagesPresent>

		<TR>
			<TD width="20%" valign="top"><loc:message name="propertyCourseOfferings"/></TD>
			<TD>
				<div class='unitime-ScrollTableCell'>
				<TABLE border="0" width="100%" cellspacing="0" cellpadding="2">
					<TR>
						<TD align="center" class="WebTableHeader">&nbsp;</TD>
						<logic:equal name="instructionalOfferingDetailForm" property="hasCourseTypes" value="true">
							<TD align="left" class="WebTableHeader"><loc:message name="columnCourseType"/></TD>
						</logic:equal>
						<TD align="left" class="WebTableHeader"><loc:message name="columnTitle"/></TD>
						<logic:equal name="instructionalOfferingDetailForm" property="hasCourseExternalId" value="true">
							<TD align="left" class="WebTableHeader"><loc:message name="columnExternalId"/></TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasCourseReservation" value="true">
							<TD align="left" class="WebTableHeader"><loc:message name="columnReserved"/></TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasCredit" value="true">
							<TD align="left" class="WebTableHeader"><loc:message name="columnCredit"/></TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasScheduleBookNote" value="true">
							<TD align="left" class="WebTableHeader"><loc:message name="columnScheduleOfClassesNote"/></TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasDemandOfferings" value="true">
							<TD align="left" class="WebTableHeader"><loc:message name="columnDemandsFrom"/></TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasAlternativeCourse" value="true">
							<TD align="left" class="WebTableHeader"><loc:message name="columnAlternativeCourse"/></TD>
						</logic:equal>
						<TD align="left" class="WebTableHeader"><loc:message name="columnConsent"/></TD>
						<tt:hasProperty name="unitime.custom.CourseUrlProvider">
						<TD align="left" class="WebTableHeader"><loc:message name="columnCourseCatalog"/></TD>
						</tt:hasProperty>
						<TD align="center" class="WebTableHeader">&nbsp;</TD>
					</TR>
				<logic:iterate id="co" name="instructionalOfferingDetailForm" property="courseOfferings" type="org.unitime.timetable.model.CourseOffering">
					<TR>
						<TD align="center" class="BottomBorderGray">
							&nbsp;
							<logic:equal name="co" property="isControl" value="true">
								<IMG src="images/accept.png" alt="<%=MSG.altControllingCourse() %>" title="<%=MSG.titleControllingCourse() %>" border="0">
							</logic:equal>
							&nbsp;
						</TD>
						<logic:equal name="instructionalOfferingDetailForm" property="hasCourseTypes" value="true">
							<TD class="BottomBorderGray">
								<logic:notEmpty name="co" property="courseType">
									<span title='<%=co.getCourseType().getLabel()%>'><%=co.getCourseType().getReference()%></span>
								</logic:notEmpty>
							</TD>
						</logic:equal>
						<TD class="BottomBorderGray"><bean:write name="co" property="courseNameWithTitle"/></TD>
						<logic:equal name="instructionalOfferingDetailForm" property="hasCourseExternalId" value="true">
							<TD class="BottomBorderGray">
								<logic:notEmpty name="co" property="externalUniqueId">
									<bean:write name="co" property="externalUniqueId"/>
								</logic:notEmpty>
							</TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasCourseReservation" value="true">
							<TD class="BottomBorderGray">
								<logic:notEmpty name="co" property="reservation">
									<bean:write name="co" property="reservation"/>
								</logic:notEmpty>
							</TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasCredit" value="true">
							<TD class="BottomBorderGray">
								<logic:notEmpty name="co" property="credit">
									<span title='<%=co.getCredit().creditText()%>'><%=co.getCredit().creditAbbv()%></span>
								</logic:notEmpty>
							</TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasScheduleBookNote" value="true">
							<TD class="BottomBorderGray">&nbsp;<bean:write name="co" property="scheduleBookNote"/></TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasDemandOfferings" value="true">
							<TD class="BottomBorderGray">&nbsp;
							<%
								CourseOffering cod = ((CourseOffering)co).getDemandOffering();
								if (cod!=null) out.write(cod.getCourseName()); 
							 %>
							</TD>
						</logic:equal>
						<logic:equal name="instructionalOfferingDetailForm" property="hasAlternativeCourse" value="true">
							<TD class="BottomBorderGray">&nbsp;
							<%
								CourseOffering cod = ((CourseOffering)co).getAlternativeOffering();
								if (cod!=null) out.write(cod.getCourseName()); 
							 %>
							</TD>
						</logic:equal>
						<TD class="BottomBorderGray">
							<logic:empty name="co" property="consentType">
								<loc:message name="noConsentRequired"/>
							</logic:empty>
							<logic:notEmpty name="co" property="consentType">
								<bean:define name="co" property="consentType" id="consentType"/>
								<bean:write name="consentType" property="abbv"/>
							</logic:notEmpty>
						</TD>
						<tt:hasProperty name="unitime.custom.CourseUrlProvider">
							<TD class="BottomBorderGray">
								<span name='UniTimeGWT:CourseLink' style="display: none;"><bean:write name="co" property="uniqueId"/></span>
							</TD>
						</tt:hasProperty>
						<TD align="right" class="BottomBorderGray">
							<sec:authorize access="hasPermission(#co, 'EditCourseOffering') or hasPermission(#co, 'EditCourseOfferingNote') or hasPermission(#co, 'EditCourseOfferingCoordinators')">
								<html:form action="/courseOfferingEdit" styleClass="FormWithNoPadding">
									<html:hidden property="courseOfferingId" value="<%= ((CourseOffering)co).getUniqueId().toString() %>" />
									<html:submit property="op" 
											styleClass="btn" 
											title="<%=MSG.titleEditCourseOffering() %>">
										<loc:message name="actionEditCourseOffering" />
									</html:submit>
								</html:form>
							</sec:authorize>
						</TD>
					</TR>
				</logic:iterate>
				</TABLE>
				</div>
			</TD>
		</TR>
		
		<TR>
			<TD><loc:message name="propertyEnrollment"/> </TD>
			<TD>
				<bean:write name="instructionalOfferingDetailForm" property="enrollment" /> 
			</TD>
		</TR>

		<TR>
			<TD><loc:message name="propertyLastEnrollment"/> </TD>
			<TD>
				<logic:equal name="instructionalOfferingDetailForm" property="demand" value="0">
					-
				</logic:equal>
				<logic:notEqual name="instructionalOfferingDetailForm" property="demand" value="0">
					<bean:write name="instructionalOfferingDetailForm" property="demand" /> 
				</logic:notEqual>
			</TD>
		</TR>

		<logic:notEqual name="instructionalOfferingDetailForm" property="projectedDemand" value="0">
			<TR>
				<TD><loc:message name="propertyProjectedDemand"/> </TD>
				<TD>
					<bean:write name="instructionalOfferingDetailForm" property="projectedDemand" /> 
				</TD>
			</TR>
		</logic:notEqual>

		<TR>
			<TD><loc:message name="propertyOfferingLimit"/> </TD>
			<TD>
				<logic:equal name="instructionalOfferingDetailForm" property="unlimited" value="false">
					<bean:write name="instructionalOfferingDetailForm" property="limit" />
					<logic:present name="limitsDoNotMatch" scope="request"> 
						&nbsp;
						<img src='images/cancel.png' alt='<%=MSG.altLimitsDoNotMatch() %>' title='<%=MSG.titleLimitsDoNotMatch() %>' border='0' align='top'>
						<font color="#FF0000"><loc:message name="errorReservedSpacesForOfferingsTotal"><bean:write name="limitsDoNotMatch" scope="request"/></loc:message></font>
					</logic:present>
					<logic:present name="configsWithTooHighLimit" scope="request">
						<logic:notPresent name="limitsDoNotMatch" scope="request">
							&nbsp;
							<img src='images/cancel.png' alt='<%=MSG.altLimitsDoNotMatch() %>' title='<%=MSG.titleLimitsDoNotMatch() %>' border='0' align='top'>
						</logic:notPresent>
						<font color="#FF0000"><bean:write name="configsWithTooHighLimit" scope="request"/></font>
					</logic:present>
				</logic:equal>
				<logic:equal name="instructionalOfferingDetailForm" property="unlimited" value="true">
					<span title="<%=MSG.titleUnlimitedEnrollment() %>"><font size="+1">&infin;</font></span>
				</logic:equal>
			</TD>
		</TR>
		<logic:equal name="instructionalOfferingDetailForm" property="unlimited" value="false">
		<logic:notEmpty name="instructionalOfferingDetailForm" property="snapshotLimit">
		<TR>
			<TD><loc:message name="propertySnapshotLimit"/> </TD>
			<TD>
				<logic:equal name="instructionalOfferingDetailForm" property="unlimited" value="false">
						<bean:write name="instructionalOfferingDetailForm" property="snapshotLimit" /> 
				</logic:equal>
				<logic:equal name="instructionalOfferingDetailForm" property="unlimited" value="true">
					<span title="<%=MSG.titleUnlimitedEnrollment() %>"><font size="+1">&infin;</font></span>
				</logic:equal>
			</TD>
		</TR>
		</logic:notEmpty>
		</logic:equal>

		<logic:equal name="instructionalOfferingDetailForm" property="byReservationOnly" value="true">
			<TR>
				<TD><loc:message name="propertyByReservationOnly"/></TD>
				<TD>
					<IMG src="images/accept.png" alt="ENABLED" title="<%=MSG.descriptionByReservationOnly2() %>" border="0">
					<i><loc:message name="descriptionByReservationOnly2"/></i>
				</TD>
			</TR>
		</logic:equal>
		
		<logic:notEmpty name="instructionalOfferingDetailForm" property="coordinators">
			<TR>
				<TD valign="top"><loc:message name="propertyCoordinators"/></TD>
				<TD>
					<bean:write name="instructionalOfferingDetailForm" property="coordinators" filter="false"/>
				</TD>
			</TR>
		</logic:notEmpty>
		
		<logic:notEmpty name="instructionalOfferingDetailForm" property="wkEnroll">
			<TR>
				<TD valign="top"><loc:message name="propertyLastWeekEnrollment"/></TD>
				<TD>
					<loc:message name="textLastWeekEnrollment"><bean:write name="instructionalOfferingDetailForm" property="wkEnroll" /></loc:message>
				</TD>
			</TR>
		</logic:notEmpty>
		
		<logic:notEmpty name="instructionalOfferingDetailForm" property="wkChange">
			<TR>
				<TD valign="top"><loc:message name="propertyLastWeekChange"/></TD>
				<TD>
					<loc:message name="textLastWeekChange"><bean:write name="instructionalOfferingDetailForm" property="wkChange" /></loc:message>
				</TD>
			</TR>
		</logic:notEmpty>

		<logic:notEmpty name="instructionalOfferingDetailForm" property="wkDrop">
			<TR>
				<TD valign="top"><loc:message name="propertyLastWeekDrop"/></TD>
				<TD>
					<loc:message name="textLastWeekDrop"><bean:write name="instructionalOfferingDetailForm" property="wkDrop" /></loc:message>
				</TD>
			</TR>
		</logic:notEmpty>
		
		<logic:equal name="instructionalOfferingDetailForm" property="displayEnrollmentDeadlineNote" value="true">
			<TR>
				<TD valign="top">&nbsp;</TD>
				<TD>
					<i><loc:message name="descriptionEnrollmentDeadlines"><bean:write name="instructionalOfferingDetailForm" property="weekStartDayOfWeek" /></loc:message></i>
				</TD>
			</TR>
		</logic:equal>

		<logic:notEmpty name="instructionalOfferingDetailForm" property="catalogLinkLabel">
		<TR>
			<TD><loc:message name="propertyCourseCatalog"/> </TD>
			<TD>
				<A href="<bean:write name="instructionalOfferingDetailForm" property="catalogLinkLocation" />" 
						target="_blank"><bean:write name="instructionalOfferingDetailForm" property="catalogLinkLabel" /></A>
			</TD>
		</TR>
		</logic:notEmpty>
		<logic:notEmpty name="instructionalOfferingDetailForm" property="accommodation">
			<TR>
				<TD valign="top"><loc:message name="propertyAccommodations"/></TD>
				<TD>
					<bean:write name="instructionalOfferingDetailForm" property="accommodation" filter="false"/>
				</TD>
			</TR>
		</logic:notEmpty>
		<logic:equal name="instructionalOfferingDetailForm" property="hasConflict" value="true">
			<TR>
				<TD></TD>
				<TD>
					<IMG src="images/warning.png" alt="WARNING" title="<%=MSG.warnOfferingHasConflictingClasses() %>" border="0">
					<font color="#FF0000"><loc:message name="warnOfferingHasConflictingClasses"/></font>
				</TD>
			</TR>
		</logic:equal>
		<logic:notEmpty name="instructionalOfferingDetailForm" property="notes">
			<TR>
				<TD valign="top"><loc:message name="propertyRequestsNotes"/></TD>
				<TD>
					<div class='unitime-ScrollTableCell'>
						<span style='white-space: pre-wrap;'><bean:write name="instructionalOfferingDetailForm" property="notes" filter="false"/></span>
					</div>
				</TD>
			</TR>
		</logic:notEmpty>
		
		<sec:authorize access="hasPermission(null, 'Session', 'CurriculumView')">
		<TR>
			<TD colspan="2">
				<div id='UniTimeGWT:CourseCurricula' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
			</TD>
		</TR>
		</sec:authorize>
		
		<sec:authorize access="hasPermission(null, 'Department', 'Reservations')">
		<TR>
			<TD colspan="2">
				<a name="reservations"></a>
				<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'ReservationOffering') and hasPermission(null, null, 'ReservationAdd')">
					<div id='UniTimeGWT:OfferingReservations' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
				</sec:authorize>
				<sec:authorize access="not hasPermission(#instrOfferingId, 'InstructionalOffering', 'ReservationOffering') or not hasPermission(null, null, 'ReservationAdd')">
					<div id='UniTimeGWT:OfferingReservationsRO' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
				</sec:authorize>
			</TD>
		</TR>
		</sec:authorize>

		<TR>
			<TD colspan="2" >&nbsp;</TD>
		</TR>

<!-- Configuration -->
		<TR>
			<TD colspan="2" valign="middle">
	<% //output configuration
	if (frm.getInstrOfferingId() != null){
		WebInstrOfferingConfigTableBuilder ioTableBuilder = new WebInstrOfferingConfigTableBuilder();
		ioTableBuilder.setDisplayDistributionPrefs(false);
		ioTableBuilder.setDisplayConfigOpButtons(true);
		ioTableBuilder.setDisplayConflicts(true);
		ioTableBuilder.htmlConfigTablesForInstructionalOffering(
									sessionContext,
				    		        WebSolver.getClassAssignmentProxy(session),
				    		        WebSolver.getExamSolver(session),
				    		        frm.getInstrOfferingId(), 
				    		        out,
				    		        request.getParameter("backType"),
				    		        request.getParameter("backId"));
	}
	%>
			</TD>
		</TR>

		<TR>
			<TD valign="middle" colspan='3' align='left'>
				<tt:displayPrefLevelLegend/>
			</TD>
		</TR>
		
		<% if (request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)!=null) { %>
			<TR>
				<TD colspan="2" >&nbsp;</TD>
			</TR>
	
			<TR>
				<TD colspan="2">
					<TABLE width="100%" cellspacing="0" cellpadding="0" border="0" style="margin:0;">
						<%=request.getAttribute(DistributionPref.DIST_PREF_REQUEST_ATTR)%>
					</TABLE>
				</TD>
			</TR>
		<% } %>
		
		<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="false">
			<sec:authorize access="hasPermission(null, 'SolverGroup', 'InstructorScheduling') and hasPermission(null, 'Department', 'InstructorAssignmentPreferences')">
			<TR>
				<TD colspan="2">
					<a name="instructors"></a>
					<div id='UniTimeGWT:TeachingRequests' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
				</TD>
			</TR>
			</sec:authorize>
		</logic:equal>

		<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="false">
		<TR>
			<TD colspan="2">
				<tt:exams type='InstructionalOffering' add='true'>
					<bean:write name="<%=frmName%>" property="instrOfferingId"/>
				</tt:exams>
			</TD>
		</TR>
		</logic:equal>
		
		<tt:last-change type='InstructionalOffering'>
			<bean:write name="<%=frmName%>" property="instrOfferingId"/>
		</tt:last-change>		

		<logic:equal name="instructionalOfferingDetailForm" property="notOffered" value="false">
			<TR>
				<TD colspan="2">
					<div id='UniTimeGWT:OfferingEnrollments' style="display: none;"><bean:write name="instructionalOfferingDetailForm" property="instrOfferingId" /></div>
				</TD>
			</TR>
		</logic:equal>

<!-- Buttons -->
		<TR>
			<TD colspan="2" valign="middle">
				<DIV class="WelcomeRowHeadBlank">&nbsp;</DIV>
			</TD>
		</TR>

		<TR>
			<TD colspan="2" align="right">
			
				<html:form action="/instructionalOfferingDetail" styleClass="FormWithNoPadding">
					<input type='hidden' name='confirm' value='y'/>
					<html:hidden property="instrOfferingId"/>	
					<html:hidden property="nextId"/>
					<html:hidden property="previousId"/>
					
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingCanLock')">
						<html:submit property="op" styleClass="btn" 
								accesskey="<%=MSG.accessLockIO() %>" 
								title="<%=MSG.titleLockIO(MSG.accessLockIO()) %>"
								onclick="<%=MSG.jsSubmitLockIO((String)instrOfferingName)%>">
							<loc:message name="actionLockIO"/>
						</html:submit>
					</sec:authorize>
					 <sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingCanUnlock')">
						<html:submit property="op" styleClass="btn" 
								accesskey="<%=MSG.accessUnlockIO() %>" 
								title="<%=MSG.titleUnlockIO(MSG.accessUnlockIO()) %>"
								onclick="<%=MSG.jsSubmitUnlockIO((String)instrOfferingName)%>">
							<loc:message name="actionUnlockIO"/>
						</html:submit>
					</sec:authorize>
				
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'InstrOfferingConfigAdd')">
							<html:submit property="op" 
									styleClass="btn" 
									accesskey="<%=MSG.accessAddConfiguration() %>" 
									title="<%=MSG.titleAddConfiguration(MSG.accessAddConfiguration()) %>">
								<loc:message name="actionAddConfiguration" />
							</html:submit>
					</sec:authorize>
					
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'InstructionalOfferingCrossLists')">
							<html:submit property="op" 
									styleClass="btn" 
									accesskey="<%=MSG.accessCrossLists() %>" 
									title="<%=MSG.titleCrossLists(MSG.accessCrossLists()) %>">
								<loc:message name="actionCrossLists" />
							</html:submit>
					</sec:authorize>

					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingMakeOffered')">
							<html:submit property="op" 
									onclick="return confirmMakeOffered();"
									styleClass="btn" 
									accesskey="<%=MSG.accessMakeOffered() %>" 
									title="<%=MSG.titleMakeOffered(MSG.accessMakeOffered()) %>">
								<loc:message name="actionMakeOffered" />
							</html:submit>
					</sec:authorize>
					
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingDelete')">
							<html:submit property="op" 
									onclick="return confirmDelete();"
									styleClass="btn" 
									accesskey="<%=MSG.accessDeleteIO() %>" 
									title="<%=MSG.titleDeleteIO(MSG.accessDeleteIO()) %>">
								<loc:message name="actionDeleteIO" />
							</html:submit>
					</sec:authorize>
					
					<sec:authorize access="hasPermission(#instrOfferingId, 'InstructionalOffering', 'OfferingMakeNotOffered')">
							<html:submit property="op" 
									onclick="return confirmMakeNotOffered();"
									styleClass="btn" 
									accesskey="<%=MSG.accessMakeNotOffered() %>"
									title="<%=MSG.titleMakeNotOffered(MSG.accessMakeNotOffered()) %>">
								<loc:message name="actionMakeNotOffered" />
							</html:submit>
					</sec:authorize>
					
					<logic:notEmpty name="instructionalOfferingDetailForm" property="previousId">
						<html:submit property="op" 
								styleClass="btn" 
								accesskey="<%=MSG.accessPreviousIO() %>" 
								title="<%=MSG.titlePreviousIO(MSG.accessPreviousIO()) %>">
							<loc:message name="actionPreviousIO" />
						</html:submit> 
					</logic:notEmpty>
					<logic:notEmpty name="instructionalOfferingDetailForm" property="nextId">
						<html:submit property="op" 
								styleClass="btn" 
								accesskey="<%=MSG.accessNextIO() %>" 
								title="<%=MSG.titleNextIO(MSG.accessNextIO()) %>">
							<loc:message name="actionNextIO" />
						</html:submit> 
					</logic:notEmpty>

					<tt:back styleClass="btn" 
							name="<%=MSG.actionBackIODetail() %>" 
							title="<%=MSG.titleBackIODetail(MSG.accessBackIODetail()) %>" 
							accesskey="<%=MSG.accessBackIODetail() %>" 
							type="InstructionalOffering">
						<bean:write name="instructionalOfferingDetailForm" property="instrOfferingId"/>
					</tt:back>				

				</html:form>					
			</TD>
		</TR>

	</TABLE>
</loc:bundle>
