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
package org.unitime.timetable.security.permissions;

import org.springframework.beans.factory.annotation.Autowired;
import org.unitime.timetable.model.Department;
import org.unitime.timetable.model.DepartmentStatusType;
import org.unitime.timetable.model.EventDateMapping;
import org.unitime.timetable.model.ItypeDesc;
import org.unitime.timetable.model.PointInTimeData;
import org.unitime.timetable.model.Roles;
import org.unitime.timetable.model.SavedHQL;
import org.unitime.timetable.model.SavedHQL.Flag;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.SponsoringOrganization;
import org.unitime.timetable.model.SubjectArea;
import org.unitime.timetable.model.TimetableManager;
import org.unitime.timetable.model.User;
import org.unitime.timetable.model.dao.DepartmentDAO;
import org.unitime.timetable.model.dao.ItypeDescDAO;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.security.rights.Right;

/**
 * @author Tomas Muller
 */
public class AdministrationPermissions {
	
	@PermissionForRight(Right.Chameleon)
	public static class Chameleon extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.DatePatterns)
	public static class DatePatterns extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.TimePatterns)
	public static class TimePatterns extends SimpleSessionPermission {}

	@PermissionForRight(Right.ExaminationPeriods)
	public static class ExaminationPeriods extends SimpleSessionPermission {}

	@PermissionForRight(Right.DataExchange)
	public static class DataExchange extends SimpleSessionPermission {}

	@PermissionForRight(Right.SessionRollForward)
	public static class SessionRollForward extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.Departments)
	public static class Departments extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.DepartmentAdd)
	public static class DepartmentAdd extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.DepartmentEdit)
	public static class DepartmentEdit extends SimpleDepartmentPermission {}
	
	@PermissionForRight(Right.DepartmentDelete)
	public static class DepartmentDelete implements Permission<Department> {
		@Autowired Permission<Department> permissionDepartment;

		@Override
		public boolean check(UserContext user, Department source) {
			if (!permissionDepartment.check(user, source))
				return false;
			
			if (source.getSolverGroup() != null)
				return false;
			
			int nrOffered = ((Number)DepartmentDAO.getInstance().getSession().
                    createQuery("select count(io) from CourseOffering co inner join co.instructionalOffering io " +
                    		"where co.subjectArea.department.uniqueId=:deptId and io.notOffered = 0").
                    setLong("deptId", source.getUniqueId()).setCacheable(true).uniqueResult()).intValue();
            
			return nrOffered == 0;
		}

		@Override
		public Class<Department> type() { return Department.class; }
	}
	
	@PermissionForRight(Right.DepartmentEditChangeExternalManager)
	public static class DepartmentEditChangeExternalManager implements Permission<Department> {
		@Autowired Permission<Department> permissionDepartmentEdit;

		@Override
		public boolean check(UserContext user, Department source) {
			if (!permissionDepartmentEdit.check(user, source))
				return false;
			
			if (source.isExternalManager()) {
	            int nrExtManaged = ((Number)DepartmentDAO.getInstance().getSession().
	                    createQuery("select count(c) from Class_ c where c.managingDept.uniqueId=:deptId").
	                    setLong("deptId", source.getUniqueId()).setCacheable(true).uniqueResult()).intValue();
	            
	            return nrExtManaged == 0;
			} else {
				return source.getSubjectAreas().isEmpty();
			}

		}

		@Override
		public Class<Department> type() { return Department.class; }
	}
	
	@PermissionForRight(Right.AcademicSessionEdit)
	public static class AcademicSessionEdit extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.AcademicSessionDelete)
	public static class AcademicSessionDelete implements Permission<Session> {
		@Autowired Permission<Session> permissionSession;
		
		@Override
		public boolean check(UserContext user, Session source) {
			if (!permissionSession.check(user, source)) return false;
			
			return source.getStatusType() == null || !source.getStatusType().isActive() || source.getStatusType().isTestSession();
		}

		@Override
		public Class<Session> type() { return Session.class; }
	}
	
	@PermissionForRight(Right.TimetableManagerEdit)
	public static class TimetableManagerEdit implements Permission<TimetableManager> {
		@Autowired Permission<Session> permissionSession;

		@Override
		public boolean check(UserContext user, TimetableManager source) {
			for (Department d: source.getDepartments()) {
				if (d.getSessionId().equals(user.getCurrentAcademicSessionId())) {
					return permissionSession.check(user, d.getSession());
				}
			}
			
			return true;
		}

		@Override
		public Class<TimetableManager> type() { return TimetableManager.class; }
	}
	
	@PermissionForRight(Right.TimetableManagerDelete)
	public static class TimetableManagerDelete extends TimetableManagerEdit {
		@Override
		public boolean check(UserContext user, TimetableManager source) {
			for (Department d: source.getDepartments())
				if (!permissionSession.check(user, d.getSession()))
					return false;
			
			return true;
		}
	}
	
	@PermissionForRight(Right.SolverGroups)
	public static class SolverGroups extends SimpleSessionPermission {}

	@PermissionForRight(Right.SubjectAreas)
	public static class SubjectAreas extends SimpleSessionPermission {}

	@PermissionForRight(Right.SubjectAreaAdd)
	public static class SubjectAreaAdd extends SubjectAreas {}

	@PermissionForRight(Right.SubjectAreaEdit)
	public static class SubjectAreaEdit implements Permission<SubjectArea> {
		@Autowired Permission<Session> permissionSession;

		@Override
		public boolean check(UserContext user, SubjectArea source) {
			return permissionSession.check(user, source.getSession());
		}

		@Override
		public Class<SubjectArea> type() { return SubjectArea.class; }
	}
	
	@PermissionForRight(Right.SubjectAreaDelete)
	public static class SubjectAreaDelete extends SubjectAreaEdit {
		@Override
		public boolean check(UserContext user, SubjectArea source) {
			if (!super.check(user, source)) return false;
			
			return !source.hasOfferedCourses();
		}
	}
	
	@PermissionForRight(Right.SubjectAreaChangeDepartment)
	public static class SubjectAreaChangeDepartment extends SubjectAreaEdit {
		@Override
		public boolean check(UserContext user, SubjectArea source) {
			if (!super.check(user, source)) return false;
			
			return !source.hasOfferedCourses() || source.getDepartment() == null || source.getDepartment().getSolverGroup() == null ||
					source.getDepartment().getSolverGroup().getSolutions() == null || 
					source.getDepartment().getSolverGroup().getSolutions().isEmpty();
		}
	}
	
	@PermissionForRight(Right.LastChanges)
	public static class LastChanges extends SimpleSessionPermission {}

	@PermissionForRight(Right.InstructionalTypeEdit)
	public static class InstructionalTypeEdit implements Permission<ItypeDesc> {

		@Override
		public boolean check(UserContext user, ItypeDesc source) {
			return true;
		}

		@Override
		public Class<ItypeDesc> type() { return ItypeDesc.class; }
	}
	
	@PermissionForRight(Right.InstructionalTypeDelete)
	public static class InstructionalTypeDelete extends InstructionalTypeEdit {

		@Override
		public boolean check(UserContext user, ItypeDesc source) {
	        int nrUsed = ((Number)ItypeDescDAO.getInstance().getSession().
	        		createQuery("select count(s) from SchedulingSubpart s where s.itype.itype=:itype").
	                setInteger("itype", source.getItype()).
	                setCacheable(true).
	                uniqueResult()).intValue();
	        int nrChildren = ((Number)ItypeDescDAO.getInstance().getSession().
	        		createQuery("select count(i) from ItypeDesc i where i.parent.itype=:itype").
	        		setInteger("itype", source.getItype()).
	        		setCacheable(true).
	                uniqueResult()).intValue();
	        
	        return nrUsed == 0 && nrChildren == 0;
		}
	}
	
	@PermissionForRight(Right.SponsoringOrganizationEdit)
	public static class SponsoringOrganizationEdit implements Permission<SponsoringOrganization> {
		@Autowired Permission<Session> permissionSession;

		@Override
		public boolean check(UserContext user, SponsoringOrganization source) {
			return true;
		}

		@Override
		public Class<SponsoringOrganization> type() { return SponsoringOrganization.class; }
	}
		
	@PermissionForRight(Right.SponsoringOrganizationDelete)
	public static class SponsoringOrganizationDelete extends SponsoringOrganizationEdit {}

	@PermissionForRight(Right.DistributionTypeEdit)
	public static class DistributionTypeEdit extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.HQLReports)
	public static class HQLReports extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.HQLReportAdd)
	public static class HQLReportAdd extends HQLReports {}
		
	@PermissionForRight(Right.HQLReportEdit)
	public static class HQLReportEdit implements Permission<SavedHQL>{
		@Override
		public boolean check(UserContext user, SavedHQL source) {
			if (source.isSet(SavedHQL.Flag.ADMIN_ONLY))
				return user.getCurrentAuthority().hasRight(Right.HQLReportsAdminOnly);
			return true;
		}

		@Override
		public Class<SavedHQL> type() { return SavedHQL.class; }
	}

	@PermissionForRight(Right.HQLReportDelete)
	public static class HQLReportDelete extends HQLReportEdit {}
	
	@PermissionForRight(Right.HQLReportsCourses)
	public static class HQLReportsCourses extends HQLReports {
		@Override
		public boolean check(UserContext user, Session source) {
			return super.check(user, source) && SavedHQL.hasQueries(Flag.APPEARANCE_COURSES, user.getCurrentAuthority().hasRight(Right.HQLReportsAdminOnly));
		}
	}

	@PermissionForRight(Right.HQLReportsExaminations)
	public static class HQLReportsExaminations extends HQLReports {
		@Override
		public boolean check(UserContext user, Session source) {
			return super.check(user, source) && SavedHQL.hasQueries(Flag.APPEARANCE_EXAMS, user.getCurrentAuthority().hasRight(Right.HQLReportsAdminOnly));
		}
	}

	@PermissionForRight(Right.HQLReportsEvents)
	public static class HQLReportsEvents extends HQLReports {
		@Override
		public boolean check(UserContext user, Session source) {
			return super.check(user, source) && SavedHQL.hasQueries(Flag.APPEARANCE_EVENTS, user.getCurrentAuthority().hasRight(Right.HQLReportsAdminOnly));
		}
	}

	@PermissionForRight(Right.HQLReportsStudents)
	public static class HQLReportsStudents extends HQLReports {
		@Override
		public boolean check(UserContext user, Session source) {
			return super.check(user, source) && SavedHQL.hasQueries(Flag.APPEARANCE_SECTIONING, user.getCurrentAuthority().hasRight(Right.HQLReportsAdminOnly));
		}
	}

	@PermissionForRight(Right.HQLReportsAdministration)
	public static class HQLReportsAdministration extends HQLReports {
		@Override
		public boolean check(UserContext user, Session source) {
			return super.check(user, source) && (user.getCurrentAuthority().hasRight(Right.HQLReportAdd) || SavedHQL.hasQueries(Flag.APPEARANCE_ADMINISTRATION, user.getCurrentAuthority().hasRight(Right.HQLReportsAdminOnly)));
		}
	}

	@PermissionForRight(Right.HQLReportsAdminOnly)
	public static class HQLReportsAdminOnly extends HQLReports {}
	
	@PermissionForRight(Right.PointInTimeDataReports)
	public static class PointInTimeDataReports extends SimpleSessionPermission {
		@Override
		public boolean check(UserContext user, Session session, DepartmentStatusType.Status... status) {
			if (!super.check(user, session, status)){
				return(false);
			}
			if (PointInTimeData.findAllForSession(session).isEmpty()){
				return(false);
			}
			return(true);
		}

	}

	@PermissionForRight(Right.AcademicAreas)
	public static class AcademicAreas extends SimpleSessionPermission {}

	@PermissionForRight(Right.AcademicClassifications)
	public static class AcademicClassifications extends SimpleSessionPermission {}

	@PermissionForRight(Right.Majors)
	public static class Majors extends SimpleSessionPermission {}

	@PermissionForRight(Right.Minors)
	public static class Minors extends SimpleSessionPermission {}

	@PermissionForRight(Right.StudentGroups)
	public static class StudentGroups extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.StudentAccommodations)
	public static class StudentAccommodations extends SimpleSessionPermission {}

	@PermissionForRight(Right.AcademicAreaEdit)
	public static class AcademicAreaEdit extends AcademicAreas {}

	@PermissionForRight(Right.AcademicClassificationEdit)
	public static class AcademicClassificationEdit extends AcademicClassifications {}

	@PermissionForRight(Right.MajorEdit)
	public static class MajorEdit extends Majors {}

	@PermissionForRight(Right.MinorEdit)
	public static class MinorEdit extends MajorEdit {}

	@PermissionForRight(Right.StudentGroupEdit)
	public static class StudentGroupEdit extends StudentGroups {}
	
	@PermissionForRight(Right.StudentAccommodationEdit)
	public static class StudentAccommodationEdit extends StudentAccommodations {}
	
	@PermissionForRight(Right.EventStatuses)
	public static class EventStatuses implements Permission<Department> {
		@Autowired Permission<Department> permissionDepartment;

		@Override
		public boolean check(UserContext user, Department source) {
			if (!permissionDepartment.check(user, source))
				return false;
			
			if (!source.isAllowEvents())
				return false;

			int nrRooms = ((Number)DepartmentDAO.getInstance().getSession().
                    createQuery("select count(r) from Room r " +
                    		"where r.eventDepartment.uniqueId=:deptId").
                    setLong("deptId", source.getUniqueId()).setCacheable(true).uniqueResult()).intValue();
			
			if (nrRooms > 0) return true;
			
			int nrLocations = ((Number)DepartmentDAO.getInstance().getSession().
                    createQuery("select count(r) from NonUniversityLocation r " +
                    		"where r.eventDepartment.uniqueId=:deptId").
                    setLong("deptId", source.getUniqueId()).setCacheable(true).uniqueResult()).intValue();
			
			return nrLocations > 0;
		}

		@Override
		public Class<Department> type() { return Department.class;}
	}
	
	@PermissionForRight(Right.EventStatusEdit)
	public static class EventStatusEdit extends EventStatuses {}
	
	@PermissionForRight(Right.InstructorRoles)
	public static class InstructorRoles implements Permission<Department> {
		@Autowired Permission<Department> permissionDepartment;

		@Override
		public boolean check(UserContext user, Department source) {
			if (!permissionDepartment.check(user, source))
				return false;
			
			return source.isAllowEvents() && !Roles.findAllInstructorRoles().isEmpty();
		}

		@Override
		public Class<Department> type() { return Department.class;}
	}
	
	@PermissionForRight(Right.InstructorRoleEdit)
	public static class InstructorRoleEdit extends InstructorRoles {}
	
	@PermissionForRight(Right.EventDateMappings)
	public static class EventDateMappings implements Permission<Session> {
		@Autowired Permission<Session> permissionSession;
		@Autowired Permission<Session> permissionEventDateMappingEdit;

		@Override
		public boolean check(UserContext user, Session source) {
			if (!permissionSession.check(user, source)) return false;
			
			// Is there a mapping to show?
			if (EventDateMapping.hasMapping(source.getUniqueId())) return true;
			
			// Is there a mapping to add?
			if (user.getCurrentAuthority().hasRight(Right.EventDateMappingEdit) && permissionEventDateMappingEdit.check(user, source))
				return true;
			
			return false;
		}

		@Override
		public Class<Session> type() { return Session.class;}
	}
	
	@PermissionForRight(Right.EventDateMappingEdit)
	public static class EventDateMappingEdit implements Permission<Session> {
		@Autowired Permission<Session> permissionSession;

		@Override
		public boolean check(UserContext user, Session source) {
			if (!permissionSession.check(user, source))
				return false;
			
			int nrCommitted = ((Number)SessionDAO.getInstance().getSession().
                    createQuery("select count(s) from Solution s where s.owner.session.uniqueId = :sessionId and s.commited = true").
                    setLong("sessionId", source.getUniqueId()).setCacheable(true).uniqueResult()).intValue();
            
			return nrCommitted == 0;
		}

		@Override
		public Class<Session> type() { return Session.class;}
	}
	
	@PermissionForRight(Right.StandardEventNotesSessionEdit)
	public static class StandardEventNotesSessionEdit extends SimpleSessionPermission {} 

	@PermissionForRight(Right.StandardEventNotesDepartmentEdit)
	public static class StandardEventNotesDepartmentEdit extends SimpleDepartmentPermission {}
	
	@PermissionForRight(Right.ChangePassword)
	public static class ChangePassword implements Permission<Session> {
		@Override
		public boolean check(UserContext user, Session source) {
			if (user instanceof UserContext.Chameleon) return false;
			
			return User.findByUserName(user.getUsername()) != null;
		}

		@Override
		public Class<Session> type() { return Session.class;}
	}
	
	@PermissionForRight(Right.ApiRetrieveRooms)
	public static class ApiRetrieveRooms extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRoomPictureUpload)
	public static class ApiRoomPictureUpload extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRoomEdit)
	public static class ApiRoomEdit extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRetrieveEvents)
	public static class ApiRetrieveEvents extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRetrieveEnrollments)
	public static class ApiRetrieveEnrollments extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRetrieveInstructorSchedule)
	public static class ApiRetrieveInstructorSchedule extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRetrieveClassInfo)
	public static class ApiRetrieveClassInfo extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiOnlineStudentScheduliung)
	public static class ApiOnlineStudentScheduliung extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRetrieveInstructors)
	public static class ApiRetriveInstructors extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRetrieveCurricula)
	public static class ApiRetriveCurricula extends SimpleSessionPermission {}
	
	@PermissionForRight(Right.ApiRetrieveStudentGroups)
	public static class ApiRetrieveStudentGroups extends SimpleSessionPermission {}
}
