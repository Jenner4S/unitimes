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
package org.unitime.timetable.model;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.unitime.commons.Debug;
import org.unitime.commons.hibernate.util.HibernateUtil;
import org.unitime.timetable.defaults.ApplicationProperty;
import org.unitime.timetable.model.base.BaseSession;
import org.unitime.timetable.model.dao.ExamDAO;
import org.unitime.timetable.model.dao.InstructionalOfferingDAO;
import org.unitime.timetable.model.dao.SessionDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningLog;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.updates.ReloadOfferingAction;
import org.unitime.timetable.security.Qualifiable;
import org.unitime.timetable.security.UserContext;
import org.unitime.timetable.solver.service.SolverServerService;
import org.unitime.timetable.spring.SpringApplicationContextHolder;
import org.unitime.timetable.util.DateUtils;
import org.unitime.timetable.util.ReferenceList;


/**
 * @hibernate.class table="SESSIONS" schema = "TIMETABLE"
 *
 * @author Tomas Muller, Stephanie Schluttenhofer, Zuzana Mullerova
 */
public class Session extends BaseSession implements Comparable, Qualifiable {

	public static final int sHolidayTypeNone = 0;

	public static final int sHolidayTypeHoliday = 1;

	public static final int sHolidayTypeBreak = 2;

	public static String[] sHolidayTypeNames = new String[] { "No Holiday",
			"Holiday", "(Spring/October/Thanksgiving) Break" };

	public static String[] sHolidayTypeColors = new String[] {
			"rgb(240,240,240)", "rgb(200,30,20)", "rgb(240,50,240)" };

	private static final long serialVersionUID = 3691040980400813366L;

	/*
	 * @return all sessions
	 */
	public static TreeSet<Session> getAllSessions() throws HibernateException {
		TreeSet<Session> ret = new TreeSet<Session>();
		for (Session session: SessionDAO.getInstance().findAll()) {
			if (session.getStatusType() != null && !session.getStatusType().isTestSession())
				ret.add(session);
		}
		return ret;
	}

	/**
	 * @param id
	 * @return
	 * @throws HibernateException
	 */
	public static Session getSessionById(Long id) throws HibernateException {
		return (new SessionDAO()).get(id);
	}

	private static void deleteObjects(org.hibernate.Session hibSession, String objectName, Iterator idIterator) {
		StringBuffer ids = new StringBuffer();
		int idx = 0;
		while (idIterator.hasNext()) {
			ids.append(idIterator.next()); idx++;
			if (idx==100) {
				hibSession.createQuery("delete "+objectName+" as x where x.uniqueId in ("+ids+")").executeUpdate();
				ids = new StringBuffer();
				idx = 0;
			} else if (idIterator.hasNext()) {
				ids.append(",");
			}
		}
		if (idx>0)
			hibSession.createQuery("delete "+objectName+" as x where x.uniqueId in ("+ids+")").executeUpdate();
	}
	
	/**
	 * @param id
	 * @throws HibernateException
	 */
	public static void deleteSessionById(Long id) throws HibernateException {
		org.hibernate.Session hibSession = new SessionDAO().getSession();
		Transaction tx = null;
		try {
		    tx = hibSession.beginTransaction();
		    for (Iterator i=hibSession.createQuery("from Location where session.uniqueId = :sessionId").setLong("sessionId", id).iterate();i.hasNext();) {
                Location loc = (Location)i.next();
                loc.getFeatures().clear();
                loc.getRoomGroups().clear();
                hibSession.update(loc);
            }
		    /*
            for (Iterator i=hibSession.createQuery("from Exam where session.uniqueId=:sessionId").setLong("sessionId", id).iterate();i.hasNext();) {
                Exam x = (Exam)i.next();
                for (Iterator j=x.getConflicts().iterator();j.hasNext();) {
                    ExamConflict conf = (ExamConflict)j.next();
                    hibSession.delete(conf);
                    j.remove();
                }
                hibSession.update(x);
            }
            */
            hibSession.flush();
            hibSession.createQuery(
	                    "delete DistributionPref p where p.owner in (select s from Session s where s.uniqueId=:sessionId)").
	                    setLong("sessionId", id).
	                    executeUpdate();
		    hibSession.createQuery(
                "delete InstructionalOffering o where o.session.uniqueId=:sessionId").
                setLong("sessionId", id).
                executeUpdate();
            hibSession.createQuery(
                "delete Department d where d.session.uniqueId=:sessionId").
                setLong("sessionId", id).
                executeUpdate();
		    hibSession.createQuery(
		            "delete Session s where s.uniqueId=:sessionId").
		            setLong("sessionId", id).
                    executeUpdate();
		    String[] a = { "DistributionPref", "RoomPref", "RoomGroupPref", "RoomFeaturePref", "BuildingPref", "TimePref", "DatePatternPref", "ExamPeriodPref" };
		    for (String str : a) {       
	            hibSession.createQuery(
	                    "delete " + str + " p where owner not in (from PreferenceGroup)").
	                    executeUpdate();
			}
		    deleteObjects(
					hibSession,
					"ExamConflict",
					hibSession.createQuery("select x.uniqueId from ExamConflict x where x.exams is empty").iterate()
					);
		    tx.commit();
		} catch (HibernateException e) {
		    try {
                if (tx!=null && tx.isActive()) tx.rollback();
            } catch (Exception e1) { }
            throw e;
		}
		HibernateUtil.clearCache();
	}

	public void saveOrUpdate() throws HibernateException {
		(new SessionDAO()).saveOrUpdate(this);
	}

	public String getAcademicYearTerm() {
		return (getAcademicYear() + getAcademicTerm());
	}

	/**
	 * @return Returns the term.
	 */
	public String getTermLabel() {
		return this.getAcademicTerm();
	}

	/**
	 * @return Returns the label.
	 */
	public String getLabel() {
		return getAcademicTerm() + " " + getAcademicYear() + " (" + getAcademicInitiative() + ")";
	}
	
	public String getReference() {
		return getAcademicTerm() + getAcademicYear() + getAcademicInitiative();
	}

	public String toString() {
		return this.getLabel();
	}

	/**
	 * @return Returns the year the session begins.
	 */
	public int getSessionStartYear() {
		if (getSessionBeginDateTime()!=null) {
			Calendar c = Calendar.getInstance(Locale.US);
			c.setTime(getSessionBeginDateTime());
			return c.get(Calendar.YEAR);
		}
		return Integer.parseInt(this.getAcademicYear());
	}

	/**
	 * @return Returns the year.
	 */
	public static int getYear(String acadYrTerm) {
		return Integer.parseInt(acadYrTerm.substring(0, 4));
	}

	/**
	 * @param bldgUniqueId
	 * @return
	 */
	public Building building(String bldgUniqueId) {
		// TODO make faster
		for (Iterator it = this.getBuildings().iterator(); it.hasNext();) {
			Building bldg = (Building) it.next();
			if (bldg.getExternalUniqueId().equals(bldgUniqueId)) {
				return bldg;
			}
		}
		return null;
	}

	public String academicInitiativeDisplayString() {
		return this.getAcademicInitiative();
	}

	public String statusDisplayString() {
		return getStatusType().getLabel();
	}

	/**
	 * @return Returns the sessionStatusMap.
	 */
	public static ReferenceList getSessionStatusList(boolean includeTestSessions) {
		ReferenceList ref = new ReferenceList();
		ref.addAll(DepartmentStatusType.findAllForSession(includeTestSessions));
		return ref;
	}

	public static Session getSessionUsingInitiativeYearTerm(
			String academicInitiative, String academicYear, String academicTerm) {
		Session s = null;
		StringBuffer queryString = new StringBuffer();
		SessionDAO sdao = new SessionDAO();

		queryString.append(" from Session as s where s.academicInitiative = '"
				+ academicInitiative + "' ");
		queryString.append(" and s.academicYear = '" + academicYear + "' ");
		queryString.append(" and s.academicTerm = '" + academicTerm + "' ");

		Query q = sdao.getQuery(queryString.toString());
		if (q.list().size() == 1) {
			s = (Session) q.list().iterator().next();
		}
		return (s);

	}

	public Session getLastLikeSession() {
		String lastYr = new Integer(this.getSessionStartYear() - 1).toString();
		return getSessionUsingInitiativeYearTerm(this.getAcademicInitiative(),
				lastYr, getAcademicTerm());
	}

	public Session getNextLikeSession() {
		String nextYr = new Integer(this.getSessionStartYear() + 1).toString();
		return getSessionUsingInitiativeYearTerm(this.getAcademicInitiative(),
				nextYr, getAcademicTerm());
	}

	public String loadInstrAndCrsOffering() throws Exception {
		return ("done");
	}

	public Long getSessionId() {
		return (this.getUniqueId());
	}

	public void setSessionId(Long sessionId) {
		this.setUniqueId(sessionId);
	}

	public String htmlLabel() {
		return (this.getLabel());
	}
	
	public Date earliestSessionRelatedDate(){
		return(getEventBeginDate()!=null&&getEventBeginDate().before(getSessionBeginDateTime())?getEventBeginDate():getSessionBeginDateTime());
	}
	
	public Date latestSessionRelatedDate(){
		return(getEventEndDate()!=null&&getEventEndDate().after(getSessionEndDateTime())?getEventEndDate():getSessionEndDateTime());
	}

	public int getStartMonth() {		
		return DateUtils.getStartMonth(
		        earliestSessionRelatedDate(),
		        getSessionStartYear(),
		        ApplicationProperty.SessionNrExcessDays.intValue());
	}

	public int getEndMonth() {
		return DateUtils.getEndMonth(
		        latestSessionRelatedDate(), getSessionStartYear(), 
		        ApplicationProperty.SessionNrExcessDays.intValue());
	}
	
	public int getPatternStartMonth() {
		return getStartMonth() - ApplicationProperty.DatePatternNrExessMonth.intValue();
	}
	
	public int getPatternEndMonth() {
		return getEndMonth() + ApplicationProperty.DatePatternNrExessMonth.intValue();
	}

	public int getDayOfYear(int day, int month) {
		return DateUtils.getDayOfYear(day, month, getSessionStartYear());
	}
	
	public int getHoliday(int day, int month) {
		return getHoliday(day, month, getSessionStartYear(), getStartMonth(), getHolidays());
	}
	
	public static int getHoliday(int day, int month, int year, int startMonth, String holidays) {
		try {
			if (holidays == null)
				return sHolidayTypeNone;
			int idx = DateUtils.getDayOfYear(day, month, year)
					- DateUtils.getDayOfYear(1, startMonth, year);
			if (idx < 0 || idx >= holidays.length())
				return sHolidayTypeNone;
			return (int) (holidays.charAt(idx) - '0');
		} catch (IndexOutOfBoundsException e) {
			return sHolidayTypeNone;
		}
	}

	public String getHolidaysHtml() {
		return getHolidaysHtml(true);
	}

	public String getHolidaysHtml(boolean editable) {
		return getHolidaysHtml(getSessionBeginDateTime(), getSessionEndDateTime(), getClassesEndDateTime(), getExamBeginDate(), getEventBeginDate(), getEventEndDate(), getSessionStartYear(), getHolidays(), editable, EventDateMapping.getMapping(getUniqueId()));
	}

	public static String getHolidaysHtml(
			Date sessionBeginTime, 
			Date sessionEndTime, 
			Date classesEndTime,
			Date examBeginTime,
			Date eventBeginTime,
			Date eventEndTime,
			int acadYear, 
			String holidays,
			boolean editable,
			EventDateMapping.Class2EventDateMap class2eventDateMap) {
		
		StringBuffer prefTable = new StringBuffer();
		StringBuffer prefNames = new StringBuffer();
		StringBuffer prefColors = new StringBuffer();
		
		for (int i = 0; i < sHolidayTypeNames.length; i++) {
			prefTable.append((i == 0 ? "" : ",") + "'" + i + "'");
			prefNames.append((i == 0 ? "" : ",") + "'" + sHolidayTypeNames[i]
					+ "'");
			prefColors.append((i == 0 ? "" : ",") + "'" + sHolidayTypeColors[i]
					+ "'");
		}

		StringBuffer holidayArray = new StringBuffer();
		StringBuffer borderArray = new StringBuffer();
		StringBuffer colorArray = new StringBuffer();
		
		Calendar sessionBeginDate = Calendar.getInstance(Locale.US);
		sessionBeginDate.setTime(sessionBeginTime);
		
		Calendar sessionEndDate = Calendar.getInstance(Locale.US);
		sessionEndDate.setTime(sessionEndTime);
		
		Calendar classesEndDate = Calendar.getInstance(Locale.US);
		classesEndDate.setTime(classesEndTime);

        Calendar examBeginDate = Calendar.getInstance(Locale.US);
        if (examBeginTime!=null) examBeginDate.setTime(examBeginTime);

        Calendar eventBeginDate = Calendar.getInstance(Locale.US);
        if (eventBeginTime!=null) eventBeginDate.setTime(eventBeginTime);

        Calendar eventEndDate = Calendar.getInstance(Locale.US);
        if (eventEndTime!=null) eventEndDate.setTime(eventEndTime);

        int startMonth = DateUtils.getStartMonth(eventBeginTime!=null&&eventBeginTime.before(sessionBeginTime)?eventBeginTime:sessionBeginTime, acadYear, ApplicationProperty.SessionNrExcessDays.intValue());
		int endMonth = DateUtils.getEndMonth(eventEndTime!=null&&eventEndTime.after(sessionEndTime)?eventEndTime:sessionEndTime, acadYear, ApplicationProperty.SessionNrExcessDays.intValue()); 
		
		for (int m = startMonth; m <= endMonth; m++) {
			int yr = DateUtils.calculateActualYear(m, acadYear);
			if (m != startMonth) {
				holidayArray.append(",");
				borderArray.append(",");
				colorArray.append(",");
			}
			holidayArray.append("[");
			borderArray.append("[");
			colorArray.append("[");
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, acadYear);
			for (int d = 1; d <= daysOfMonth; d++) {
				if (d > 1) {
					holidayArray.append(",");
					borderArray.append(",");
					colorArray.append(",");
				}
				holidayArray.append("'" + getHoliday(d, m, acadYear, startMonth, holidays) + "'");
				
				String color = "null";
				if (class2eventDateMap != null) {
					if (class2eventDateMap.hasClassDate(DateUtils.getDate(d, m, acadYear))) {
						color = "'#c0c'";
					} else if (class2eventDateMap.hasEventDate(DateUtils.getDate(d, m, acadYear))) {
						color = "'#0cc'";
					}
				}
				colorArray.append(color);
				
				if (d == sessionBeginDate.get(Calendar.DAY_OF_MONTH)
						&& (m%12) == sessionBeginDate.get(Calendar.MONTH)
						&& yr == sessionBeginDate.get(Calendar.YEAR))
					borderArray.append("'#660000 2px solid'");
				else if (d == sessionEndDate.get(Calendar.DAY_OF_MONTH)
						&& (m%12) == sessionEndDate.get(Calendar.MONTH)
						&& yr == sessionEndDate.get(Calendar.YEAR))
					borderArray.append("'#333399 2px solid'");
				else if (d == classesEndDate.get(Calendar.DAY_OF_MONTH)
						&& (m%12) == classesEndDate.get(Calendar.MONTH)
						&& yr == classesEndDate.get(Calendar.YEAR))
					borderArray.append("'#339933 2px solid'");
                else if (d == examBeginDate.get(Calendar.DAY_OF_MONTH)
                        && (m%12) == examBeginDate.get(Calendar.MONTH)
                        && yr == examBeginDate.get(Calendar.YEAR))
                    borderArray.append("'#999933 2px solid'");
                else if (d == eventBeginDate.get(Calendar.DAY_OF_MONTH)
                        && (m%12) == eventBeginDate.get(Calendar.MONTH)
                        && yr == eventBeginDate.get(Calendar.YEAR))
                    borderArray.append("'yellow 2px solid'");
                else if (d == eventEndDate.get(Calendar.DAY_OF_MONTH)
                        && (m%12) == eventEndDate.get(Calendar.MONTH)
                        && yr == eventEndDate.get(Calendar.YEAR))
                    borderArray.append("'red 2px solid'");
				else
					borderArray.append("null");
			}
			holidayArray.append("]");
			borderArray.append("]");
			colorArray.append("]");
		}

		StringBuffer table = new StringBuffer();
		table
				.append("<script language='JavaScript' type='text/javascript' src='scripts/datepatt.js'></script>");
		table.append("<script language='JavaScript'>");
		table.append("calGenerate2(" + acadYear + "," + startMonth + ","
				+ endMonth + "," + "[" + holidayArray + "]," + "[" + prefTable
				+ "]," + "[" + prefNames + "]," + "[" + prefColors + "]," + "'"
				+ sHolidayTypeNone + "'," + "[" + borderArray + "],[" + colorArray + "]," + editable
				+ "," + editable + ");");
		table.append("</script>");
		return table.toString();
	}

	public void setHolidays(String holidays) {
		super.setHolidays(holidays);
	}

	public void setHolidays(HttpServletRequest request) {
		int startMonth = getStartMonth();
		int endMonth = getEndMonth();
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(earliestSessionRelatedDate());			
		int startYear = getSessionStartYear();
		
		StringBuffer sb = new StringBuffer();
		for (int m = startMonth; m <= endMonth; m++) {
			int year = DateUtils.calculateActualYear(m, startYear);
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, startYear);
			for (int d = 1; d <= daysOfMonth; d++) {
				String holiday = request.getParameter("cal_val_" + year + "_"
						+ ((12 + m) % 12) + "_" + d);
				sb.append(holiday == null ? String.valueOf(sHolidayTypeNone)
						: holiday);
			}
		}
		setHolidays(sb.toString());
	}

	public int compareTo(Object o) {
		if (o == null || !(o instanceof Session)) return -1;
		Session s = (Session) o;
		
		int cmp = getAcademicInitiative().compareTo(s.getAcademicInitiative());
		if (cmp!=0) return cmp;
		
		cmp = getSessionBeginDateTime().compareTo(s.getSessionBeginDateTime());
		if (cmp!=0) return cmp;
		
		return (getUniqueId() == null ? new Long(-1) : getUniqueId()).compareTo(s.getUniqueId() == null ? -1 : s.getUniqueId());
	}

	public DatePattern getDefaultDatePatternNotNull() {
		DatePattern dp = super.getDefaultDatePattern();
		if (dp == null) {
			dp = DatePattern.findByName(this, "Full Term");
		}
		return dp;
	}

	public int getNrWeeks() {
		Calendar sessionBeginDate = Calendar.getInstance(Locale.US);
		sessionBeginDate.setTime(getSessionBeginDateTime());
		Calendar sessionEndDate = Calendar.getInstance(Locale.US);
		sessionEndDate.setTime(getSessionEndDateTime());
		int beginDay = getDayOfYear(
				sessionBeginDate.get(Calendar.DAY_OF_MONTH), sessionBeginDate
						.get(Calendar.MONTH))
				- getDayOfYear(1, getStartMonth());
		int endDay = getDayOfYear(sessionEndDate.get(Calendar.DAY_OF_MONTH),
				sessionEndDate.get(Calendar.MONTH))
				- getDayOfYear(1, getStartMonth());

		int nrDays = 0;
		for (int i = beginDay; i <= endDay; i++) {
			if (getHolidays() == null || i >= getHolidays().length()
					|| (getHolidays().charAt(i) - '0') == sHolidayTypeNone)
				nrDays++;
		}
		nrDays -= 7;

		return (6 + nrDays) / 7;
	}
	
	public int getExamBeginOffset() {
	    return (int)Math.round((getSessionBeginDateTime().getTime() - getExamBeginDate().getTime()) / 86.4e6); 
	}
	
	public String getBorder(int day, int month) {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.setTime(getSessionBeginDateTime());
		if (day==cal.get(Calendar.DAY_OF_MONTH) && ((12+month)%12)==cal.get(Calendar.MONTH))
			return "'blue 2px solid'";
		cal.setTime(getClassesEndDateTime());
		if (day==cal.get(Calendar.DAY_OF_MONTH) && ((12+month)%12)==cal.get(Calendar.MONTH))
			return "'blue 2px solid'";
		if (getExamBeginDate()!=null) {
		    cal.setTime(getExamBeginDate());
		    if (day==cal.get(Calendar.DAY_OF_MONTH) && ((12+month)%12)==cal.get(Calendar.MONTH))
		        return "'green 2px solid'";
		}
		int holiday = getHoliday(day, month);
		if (holiday!=Session.sHolidayTypeNone)
			return "'"+Session.sHolidayTypeColors[holiday]+" 2px solid'";
		return "null";
	}
	
	public String getColorArray() {
		EventDateMapping.Class2EventDateMap class2EventDateMap = EventDateMapping.getMapping(getUniqueId());
		int startMonth = getSession().getPatternStartMonth();
		int endMonth = getSession().getPatternEndMonth();
		int year = getSession().getSessionStartYear();
		StringBuffer sb = new StringBuffer("[");
		for (int m=startMonth;m<=endMonth;m++) {
			if (m!=startMonth) sb.append(",");
			sb.append("[");
			int daysOfMonth = DateUtils.getNrDaysOfMonth(m, year);
			for (int d=1;d<=daysOfMonth;d++) {
				if (d>1) sb.append(",");
				String color = "null";
				if (class2EventDateMap != null) {
					if (class2EventDateMap.hasClassDate(DateUtils.getDate(d, m, year))) {
						color = "'#c0c'";
					} else if (class2EventDateMap.hasEventDate(DateUtils.getDate(d, m, year))) {
						color = "'#0cc'";
					}
				}
				sb.append(color);
			}
			sb.append("]");
		}
		sb.append("]");
		return sb.toString();
	}
	
	/** Return distance of the given date outside the session start/end date (in milliseconds) */
	public long getDistance(Date date) {
		if (date.compareTo(getEventBeginDate())<0) //before session 
			return getEventBeginDate().getTime() - date.getTime();
		if (date.compareTo(getEventEndDate())>0) //after session
			return date.getTime() - getEventEndDate().getTime();
		return 0; //inside session
	}
	
	public boolean hasStudentSchedule() {
		return hasStudentSchedule(getUniqueId());
	}
	
	public static boolean hasStudentSchedule(Long sessionId) {
        return ((Number)new ExamDAO().getSession().
                createQuery("select count(x) from StudentClassEnrollment x " +
                        "where x.student.session.uniqueId=:sessionId").
                setLong("sessionId",sessionId).uniqueResult()).longValue()>0;
	}
	
	private OnlineSectioningServer getInstance() {
		if (getUniqueId() == null) return null;
		return ((SolverServerService)SpringApplicationContextHolder.getBean("solverServerService")).getOnlineStudentSchedulingContainer().getSolver(getUniqueId().toString());
	}

	public Collection<Long> getLockedOfferings() {
		if (!getStatusType().canLockOfferings()) return null;
		OnlineSectioningServer server = getInstance();
		return (server == null || !server.isReady() ? null : server.getLockedOfferings());		
	}
	
	public boolean isOfferingLocked(Long offeringId) {
		if (!getStatusType().canLockOfferings()) return false;
		OnlineSectioningServer server = getInstance();
		return server != null && server.isOfferingLocked(offeringId);
	}
	
	public void lockOffering(Long offeringId) {
		if (getStatusType().canLockOfferings()) {
			OnlineSectioningServer server = getInstance();
			if (server != null) server.lockOffering(offeringId);
		}
	}
	
	public void unlockOffering(InstructionalOffering offering, UserContext user) {
		OnlineSectioningServer server = getInstance();
		if (server != null) {
			server.execute(server.createAction(ReloadOfferingAction.class).forOfferings(offering.getUniqueId()),
					(user == null ? null :
					OnlineSectioningLog.Entity.newBuilder().setExternalId(user.getExternalUserId()).setName(user.getName()).setType(OnlineSectioningLog.Entity.EntityType.MANAGER).build()
					));
			server.unlockOffering(offering.getUniqueId());
		}
        try {
	        SessionFactory hibSessionFactory = SessionDAO.getInstance().getSession().getSessionFactory();
	        hibSessionFactory.getCache().evictEntity(InstructionalOffering.class, offering.getUniqueId());
	        for (CourseOffering course: offering.getCourseOfferings())
	        	hibSessionFactory.getCache().evictEntity(CourseOffering.class, course.getUniqueId());
	        for (InstrOfferingConfig config: offering.getInstrOfferingConfigs())
	        	for (SchedulingSubpart subpart: config.getSchedulingSubparts())
	        		for (Class_ clazz: subpart.getClasses())
	        			hibSessionFactory.getCache().evictEntity(Class_.class, clazz.getUniqueId());
        } catch (Exception e) {
        	Debug.error("Failed to evict cache: " + e.getMessage());
        }
	}
	
	@Override
	public Session getSession() { return this; }

	@Override
	public Serializable getQualifierId() {
		return getUniqueId();
	}

	@Override
	public String getQualifierType() {
		return getClass().getSimpleName();
	}

	@Override
	public String getQualifierReference() {
		return getReference();
	}

	@Override
	public String getQualifierLabel() {
		return getLabel();
	}
	
	@Override
	public Department getDepartment() { return null; }
	
	public boolean canNoRoleReportExamFinal() {
		return ((Number)new ExamDAO().getSession().
                createQuery("select count(e) from FinalExamEvent e inner join e.exam.session s where s.uniqueId = :sessionId and " +
                		"((e.examStatus is null and bit_and(s.statusType.status, :flag) > 0) or bit_and(e.examStatus, :flag) > 0)"
                		)
                .setLong("sessionId", getUniqueId())
                .setInteger("flag", DepartmentStatusType.Status.ReportExamsFinal.toInt())
                .setCacheable(true).uniqueResult()).longValue() > 0;
	}
	
	public boolean canNoRoleReportExamMidterm() {
		return ((Number)new ExamDAO().getSession().
                createQuery("select count(e) from MidtermExamEvent e inner join e.exam.session s where s.uniqueId = :sessionId and " +
                		"((e.examStatus is null and bit_and(s.statusType.status, :flag) > 0) or bit_and(e.examStatus, :flag) > 0)"
                		)
                .setLong("sessionId", getUniqueId())
                .setInteger("flag", DepartmentStatusType.Status.ReportExamsMidterm.toInt())
                .setCacheable(true).uniqueResult()).longValue() > 0;
	}
	
	public boolean canNoRoleReportClass() {
		return getStatusType() != null && getStatusType().can(DepartmentStatusType.Status.ReportClasses) && Solution.hasTimetable(getUniqueId());
	}
	
    public boolean canNoRoleReportExam() {
        return canNoRoleReportExamFinal() || canNoRoleReportExamMidterm();
    }

    public boolean canNoRoleReport() {
        return canNoRoleReportClass() || canNoRoleReportExam();
    }
    
    public Date getCurrentSnapshotDate() {
    	Object o = InstructionalOfferingDAO.getInstance()
				.getSession()
				.createQuery("select max(io.snapshotLimitDate) from InstructionalOffering io where io.session.uniqueId = :sessId")
				.setLong("sessId", getUniqueId().longValue())
				.uniqueResult();
    	Date snapshotDate = null;
    	if (o != null) {
    		snapshotDate = (Date) o;
    	}
    	return(snapshotDate);
    }
}
