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
package org.unitime.timetable.gwt.client.widgets;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unitime.timetable.gwt.command.client.GwtRpcRequest;
import org.unitime.timetable.gwt.command.client.GwtRpcResponse;
import org.unitime.timetable.gwt.command.client.GwtRpcService;
import org.unitime.timetable.gwt.command.client.GwtRpcServiceAsync;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * @author Tomas Muller
 */
public class ServerDateTimeFormat extends DateTimeFormat {
	protected static final GwtRpcServiceAsync RPC = GWT.create(GwtRpcService.class);
	private static TimeZone sServerTimeZone = null;
	private static final Map<String, ServerDateTimeFormat> sFormatCache;
	
	static {
		String cookie = Cookies.getCookie("UniTime:ServerTimeZone");
		if (cookie != null) {
			try {
				sServerTimeZone = TimeZone.createTimeZone(cookie);
			} catch (Exception e) {}
		}
		if (sServerTimeZone == null) {
			RPC.execute(new ServerTimeZoneRequest(), new AsyncCallback<ServerTimeZoneResponse>() {
				@Override
				public void onFailure(Throwable caught) {}

				@Override
				public void onSuccess(ServerTimeZoneResponse result) {
					sServerTimeZone = TimeZone.createTimeZone(result.toJsonString());
					Cookies.setCookie("UniTime:ServerTimeZone", result.toJsonString());
				}
			});
		}
		sFormatCache = new HashMap<String, ServerDateTimeFormat>();
	}
	
	public ServerDateTimeFormat(String pattern) {
		super(pattern);
	}
	
	public static TimeZone getServerTimeZone() {
		return sServerTimeZone;
	}
	
	public static Integer getOffset(Date date) {
		return sServerTimeZone.getStandardOffset() - sServerTimeZone.getDaylightAdjustment(date);
	}
	
	@SuppressWarnings("deprecation")
	public static Date toLocalDate(Date serverDate) {
		if (serverDate == null || sServerTimeZone == null) return serverDate;
		return new Date(serverDate.getTime() + 60000 * (serverDate.getTimezoneOffset() - getOffset(serverDate)));
	}
	
	@SuppressWarnings("deprecation")
	public static Date toServerDate(Date localDate) {
		if (localDate == null || sServerTimeZone == null) return localDate;
		return new Date(localDate.getTime() + 60000 * (getOffset(localDate) - localDate.getTimezoneOffset()));
	}
	
	@Override
	public String format(Date date) {
		return super.format(date, sServerTimeZone);
	}
	
	public static DateTimeFormat getFormat(String pattern) {
		ServerDateTimeFormat format = sFormatCache.get(pattern);
		if (format == null) {
			format = new ServerDateTimeFormat(pattern);
			sFormatCache.put(pattern, format);
		}
		return format;
	}
	
	public static class ServerTimeZoneRequest implements GwtRpcRequest<ServerTimeZoneResponse> {}
	
	public static class ServerTimeZoneResponse implements GwtRpcResponse {
		private String iId;
		private List<String> iNames;
		private int iTimeZoneOffsetInMinutes;
		private List<Integer> iTransitions = null;
		
		public ServerTimeZoneResponse() {}
		
		public ServerTimeZoneResponse(int timeZoneOffsetInMinutes) {
			iTimeZoneOffsetInMinutes = timeZoneOffsetInMinutes;
		}
		
		public void setId(String id) { iId = id; }
		public String getId() { return iId; }
		
		public void addName(String name) {
			if (iNames == null)
				iNames = new ArrayList<String>();
			iNames.add(name);
		}
		
		public void setTimeZoneOffsetInMinutes(int timeZoneOffestInMinutes) { iTimeZoneOffsetInMinutes = timeZoneOffestInMinutes; }
		public int getTimeZoneOffsetInMinutes() { return iTimeZoneOffsetInMinutes; }
		
		public void addTransition(int transition, int adjustment) {
			if (iTransitions == null)
				iTransitions = new ArrayList<Integer>();
			iTransitions.add(transition);
			iTransitions.add(adjustment);
		}
		
		public String toJsonString() {
			String ret = "{\"id\":\"" + getId() +"\",\"std_offset\":" + getTimeZoneOffsetInMinutes();
			if (iTransitions != null) {
				ret += ",\"transitions\":[";
				for (int i = 0; i < iTransitions.size(); i++)
					ret += (i == 0 ? "" : ",") + iTransitions.get(i);
				ret += "]";
			}
			if (iNames == null) {
				ret += ",\"names\":[]";
			} else {
				ret += ",\"names\":[";
				for (int i = 0; i < iNames.size(); i++)
					ret += (i == 0 ? "" : ",") + "\"" + iNames.get(i) + "\"";
				ret += "]";
			}
			ret += "}";
			return ret;
		}
		
		public String toString() {
			return toJsonString();
		}
	}
	
}
