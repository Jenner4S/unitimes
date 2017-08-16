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
package org.unitime.timetable.onlinesectioning.model;

import java.util.List;

import org.unitime.timetable.model.ExactTimeMins;
import org.unitime.timetable.model.dao.ExactTimeMinsDAO;
import org.unitime.timetable.util.Constants;

/**
 * @author Tomas Muller
 */
public class XExactTimeConversion {
	private int[] iBreakTimes, iLength;
	
	public XExactTimeConversion(org.hibernate.Session hibSession) {
		List<ExactTimeMins> list = ExactTimeMinsDAO.getInstance().findAll(hibSession);
		int max = 0;
		for (ExactTimeMins e: list)
			if (e.getMinsPerMtgMax() > max) max = e.getMinsPerMtgMax();
		iBreakTimes = new int[1 + max];
		iLength = new int[1 + max];
		for (int i = 0; i <= max; i++) {
			iLength[i] = -1; iBreakTimes[i] = -1;
		}
		for (ExactTimeMins e: list)
			for (int i = e.getMinsPerMtgMin(); i <= e.getMinsPerMtgMax(); i++) {
				iBreakTimes[i] = e.getBreakTime();
				iLength[i] = e.getNrSlots();
			}
	}
	
	public static int getDefaultLength(int minutesPerMeeting) {
		int len = (int)Math.round( (6.0/5.0) * minutesPerMeeting / 5);
		if (minutesPerMeeting < 30) len = Math.min(6, len);
		return len;
	}
	
	public static int getDefaultBreakTime(int minutesPerMeeting) {
		int len = getDefaultLength(minutesPerMeeting);
		if (len <= 6) return 0;
		if (len % 12 == 0) return 10;
		return 15;
	}
	
	public int getLength(int minutesPerMeeting) {
		if (minutesPerMeeting < iLength.length) {
			int len = iLength[minutesPerMeeting];
			if (len >= 0) return len;
		}
		return getDefaultLength(minutesPerMeeting);
	}
	
	public int getBreakTime(int minutesPerMeeting) {
		if (minutesPerMeeting < iBreakTimes.length) {
			int breakTime = iBreakTimes[minutesPerMeeting];
			if (breakTime >= 0) return breakTime;
		}
		return getDefaultBreakTime(minutesPerMeeting);
	}
	
	private static int toMinutesPerMeeting(int days, int minPerWeek) {
		int nrDays = 0;
		for (int i = 0; i < Constants.NR_DAYS ; i++)
			if ((days & Constants.DAY_CODES[i]) != 0) nrDays++;
		return minPerWeek / (nrDays == 0 ? 1 : nrDays);
	}
	
	public int getLength(int days, int minPerWeek) {
		return getLength(toMinutesPerMeeting(days, minPerWeek));
	}

	public int getBreakTime(int days, int minPerWeek) {
		return getBreakTime(toMinutesPerMeeting(days, minPerWeek));
	}

	public static int getDefaultLength(int days, int minPerWeek) {
		return getDefaultLength(toMinutesPerMeeting(days, minPerWeek));
	}

	public static int getDefaultBreakTime(int days, int minPerWeek) {
		return getDefaultBreakTime(toMinutesPerMeeting(days, minPerWeek));
	}

}
