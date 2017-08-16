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
package org.unitime.timetable.model.base;

import java.io.Serializable;
import java.util.Date;

import org.unitime.timetable.model.PointInTimeData;
import org.unitime.timetable.model.Session;

/**
 * Do not change this class. It has been automatically generated using ant create-model.
 * @see org.unitime.commons.ant.CreateBaseModelFromXml
 */
public abstract class BasePointInTimeData implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;
	private Date iTimestamp;
	private String iName;
	private String iNote;
	private Boolean iSavedSuccessfully;

	private Session iSession;

	public static String PROP_UNIQUEID = "uniqueId";
	public static String PROP_TIMESTAMP = "timestamp";
	public static String PROP_NAME = "name";
	public static String PROP_NOTE = "note";
	public static String PROP_SAVED_SUCCESSFULLY = "savedSuccessfully";

	public BasePointInTimeData() {
		initialize();
	}

	public BasePointInTimeData(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public Date getTimestamp() { return iTimestamp; }
	public void setTimestamp(Date timestamp) { iTimestamp = timestamp; }

	public String getName() { return iName; }
	public void setName(String name) { iName = name; }

	public String getNote() { return iNote; }
	public void setNote(String note) { iNote = note; }

	public Boolean isSavedSuccessfully() { return iSavedSuccessfully; }
	public Boolean getSavedSuccessfully() { return iSavedSuccessfully; }
	public void setSavedSuccessfully(Boolean savedSuccessfully) { iSavedSuccessfully = savedSuccessfully; }

	public Session getSession() { return iSession; }
	public void setSession(Session session) { iSession = session; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof PointInTimeData)) return false;
		if (getUniqueId() == null || ((PointInTimeData)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((PointInTimeData)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "PointInTimeData["+getUniqueId()+" "+getName()+"]";
	}

	public String toDebugString() {
		return "PointInTimeData[" +
			"\n	Name: " + getName() +
			"\n	Note: " + getNote() +
			"\n	SavedSuccessfully: " + getSavedSuccessfully() +
			"\n	Session: " + getSession() +
			"\n	Timestamp: " + getTimestamp() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
