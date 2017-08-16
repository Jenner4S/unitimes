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

import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.hibernate.HibernateException;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.unitime.timetable.model.base.BaseRoomGroup;
import org.unitime.timetable.model.dao.RoomGroupDAO;




/**
 * @author Tomas Muller
 */
public class RoomGroup extends BaseRoomGroup implements Comparable {
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public RoomGroup () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public RoomGroup (java.lang.Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/
	
    /** Request attribute name for available room groups **/
    public static String GROUP_LIST_ATTR_NAME = "roomGroupsList";
	
	public static Collection getAllRoomGroups() throws HibernateException {
		return (new RoomGroupDAO()).findAll(Order.asc("name"));
	}
	
	public static List<RoomGroup> getAllGlobalRoomGroups(Long sessionId) throws HibernateException {
		return (List<RoomGroup>)RoomGroupDAO.getInstance().getSession().createQuery(
				"from RoomGroup g where g.global = true and g.session.uniqueId = :sessionId order by name"
				).setLong("sessionId", sessionId).setCacheable(true).list();
	}
	
	public static List<RoomGroup> getAllGlobalRoomGroups(Session session) throws HibernateException {
		return getAllGlobalRoomGroups(session.getUniqueId());
	}
	
	public static RoomGroup findGlobalRoomGroupForName(Session session, String name){
		List groups = RoomGroupDAO.getInstance().getSession().createCriteria(RoomGroup.class)
			.add(Restrictions.eq("global",Boolean.TRUE))
			.add(Restrictions.eq("name", name))
			.add(Restrictions.eq("session.uniqueId", session.getUniqueId()))
			.setCacheable(true).list();
		return (groups.size() == 1 ? (RoomGroup)groups.get(0) : null);
	}
	
	/**
	 * Gets the default global room group. Only one exists hence only one
	 * record is returned. If more than one exists then it returns the first 
	 * one in the list
\	 * @return Room Group if found, null otherwise
	 */
	public static RoomGroup getGlobalDefaultRoomGroup(Long sessionId) {
		List<RoomGroup> groups = (List<RoomGroup>)RoomGroupDAO.getInstance().getSession().createQuery(
				"from RoomGroup g where g.global = true and g.session.uniqueId = :sessionId and g.defaultGroup = true order by name"
				).setLong("sessionId", sessionId).setCacheable(true).list();
		return (groups.isEmpty() ? null : groups.get(0));
	}
	
	public static RoomGroup getGlobalDefaultRoomGroup(Session session) {
		return getGlobalDefaultRoomGroup(session.getUniqueId());
	}
	
	public static List<RoomGroup> getAllDepartmentRoomGroups(Department dept) {
		return (List<RoomGroup>)RoomGroupDAO.getInstance().getSession().createQuery(
				"from RoomGroup g where g.global = false and g.department.uniqueId = :deptId order by name"
				).setLong("deptId", dept.getUniqueId()).setCacheable(true).list();
	}
	
	public static List<RoomGroup> getAllRoomGroupsForSession(Long sessionId) {
		return (List<RoomGroup>)RoomGroupDAO.getInstance().getSession().createQuery(
				"from RoomGroup g where g.session.uniqueId = :sessionId order by name"
				).setLong("sessionId", sessionId).setCacheable(true).list();
	}
	
	public static List<RoomGroup> getAllRoomGroupsForSession(Session session) {
		return getAllRoomGroupsForSession(session.getUniqueId());
	}
    
    public int compareTo(Object o) {
    	if (o==null || !(o instanceof RoomGroup)) return -1;
    	RoomGroup rg = (RoomGroup)o;
    	int cmp = (isGlobal().booleanValue() == rg.isGlobal().booleanValue() ? 0 : (isGlobal().booleanValue() ? -1 : 1));
    	if (cmp!=0) return cmp;
    	cmp = getName().compareTo(rg.getName());
    	if (cmp!=0) return cmp;
    	return (getUniqueId() == null ? new Long(-1) : getUniqueId()).compareTo(rg.getUniqueId() == null ? -1 : rg.getUniqueId());
    }
    
	public boolean hasLocation (Location location) {
		return getRooms().contains(location);
	}
    
	public String htmlLabel() {
		return "<span "+
			(isGlobal().booleanValue()?"":"style='color:#"+getDepartment().getRoomSharingColor(null)+";font-weight:bold;' ")+
			"title='"+getName()+
			" ("+(isGlobal().booleanValue()?"global":getDepartment().isExternalManager().booleanValue()?getDepartment().getExternalMgrLabel():getDepartment().getName())+")'>"+
			getName() +
			"</span>";
	}
	
	public String getNameWithTitle() {
		return getName()+(isGlobal()!=null && isGlobal().booleanValue()?"":" (Department)");
	}
    
    public String toString() {
        return getName();
    }

	public Object clone(){
		RoomGroup newRoomGroup = new RoomGroup();
		newRoomGroup.setDefaultGroup(isDefaultGroup());
		newRoomGroup.setDepartment(getDepartment());
		newRoomGroup.setDescription(getDescription());
		newRoomGroup.setGlobal(isGlobal());
		newRoomGroup.setName(getName());
		newRoomGroup.setAbbv(getAbbv());
		newRoomGroup.setSession(getSession());
		return(newRoomGroup);
	}

	public RoomGroup findSameRoomGroupInSession(Session session) {
		if(session == null){
			return(null);
		}
		List matchinRoomGroups = null;
		if (isGlobal()) {
			matchinRoomGroups = RoomGroupDAO.getInstance().getSession().
			createCriteria(RoomGroup.class).
				add(Restrictions.eq("global",Boolean.TRUE)).
				add(Restrictions.eq("session.uniqueId", session.getUniqueId())).
				add(Restrictions.eq("name", getName())).
				addOrder(Order.asc("name")).
				setCacheable(true).list();
		} else {
			Department d = (getDepartment() == null ? null : getDepartment().findSameDepartmentInSession(session));
			if (d != null)
				matchinRoomGroups = RoomGroupDAO.getInstance().getSession().
					createCriteria(RoomGroup.class).
					add(Restrictions.eq("global",Boolean.FALSE)).
					add(Restrictions.eq("department.uniqueId", d.getUniqueId())).
					add(Restrictions.eq("name", getName())).
					addOrder(Order.asc("name")).
					setCacheable(true).list();
		}
		return (matchinRoomGroups != null && matchinRoomGroups.size() == 1 ? (RoomGroup)matchinRoomGroups.get(0) : null);
	}
    
    public String getAbbv() {
        if (super.getAbbv()!=null && super.getAbbv().trim().length()>0) return super.getAbbv();
        StringBuffer sb = new StringBuffer();
        for (StringTokenizer stk = new StringTokenizer(getName()," ");stk.hasMoreTokens();) {
            String word = stk.nextToken();
            if ("and".equalsIgnoreCase(word))
                sb.append("&amp;");
            else if (word.replaceAll("[a-zA-Z\\.]*", "").length()==0) {
                for (int i=0;i<word.length();i++) {
                    if (i==0)
                        sb.append(word.substring(i,i+1).toUpperCase());
                    else if ((i==1 && word.length()>3) || (word.charAt(i)>='A' && word.charAt(i)<='Z'))
                        sb.append(word.charAt(i));
                }
            } else
                sb.append(word);
        }
        return sb.toString();
    }
    
}
