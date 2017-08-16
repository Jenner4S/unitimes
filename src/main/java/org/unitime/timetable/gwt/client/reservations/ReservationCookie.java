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
package org.unitime.timetable.gwt.client.reservations;

import com.google.gwt.user.client.Cookies;

/**
 * @author Tomas Muller
 */
public class ReservationCookie {
	private boolean iCourseDetails = false;
	private int iSortBy = 0;
	
	private static ReservationCookie sInstance = null;
	
	private ReservationCookie() {
		try {
			String cookie = Cookies.getCookie("UniTime:Reservations");
			if (cookie != null && cookie.length() > 0) {
				String[] values = cookie.split(":");
				iCourseDetails = "T".equals(values[0]);
				iSortBy = Integer.valueOf(values[1]);
			}
		} catch (Exception e) {
		}
	}
	
	private void save() {
		String cookie =  (iCourseDetails ? "T": "F") + ":" + iSortBy;
		Cookies.setCookie("UniTime:Reservations", cookie);
	}
	
	public static ReservationCookie getInstance() {
		if (sInstance == null)
			sInstance = new ReservationCookie();
		return sInstance;
	}
	
	public boolean getReservationCoursesDetails() {
		return iCourseDetails;
	}
	
	public void setReservationCoursesDetails(boolean details) {
		iCourseDetails = details;
		save();
	}
	
	public int getSortBy() {
		return iSortBy;
	}
	
	public void setSortBy(int sortBy) {
		iSortBy = sortBy;
		save();
	}
}
