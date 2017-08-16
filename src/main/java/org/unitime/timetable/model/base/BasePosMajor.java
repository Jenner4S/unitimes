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
import java.util.HashSet;
import java.util.Set;

import org.unitime.timetable.model.AcademicArea;
import org.unitime.timetable.model.PosMajor;
import org.unitime.timetable.model.Session;

/**
 * Do not change this class. It has been automatically generated using ant create-model.
 * @see org.unitime.commons.ant.CreateBaseModelFromXml
 */
public abstract class BasePosMajor implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;
	private String iExternalUniqueId;
	private String iCode;
	private String iName;

	private Session iSession;
	private Set<AcademicArea> iAcademicAreas;

	public static String PROP_UNIQUEID = "uniqueId";
	public static String PROP_EXTERNAL_UID = "externalUniqueId";
	public static String PROP_CODE = "code";
	public static String PROP_NAME = "name";

	public BasePosMajor() {
		initialize();
	}

	public BasePosMajor(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public String getExternalUniqueId() { return iExternalUniqueId; }
	public void setExternalUniqueId(String externalUniqueId) { iExternalUniqueId = externalUniqueId; }

	public String getCode() { return iCode; }
	public void setCode(String code) { iCode = code; }

	public String getName() { return iName; }
	public void setName(String name) { iName = name; }

	public Session getSession() { return iSession; }
	public void setSession(Session session) { iSession = session; }

	public Set<AcademicArea> getAcademicAreas() { return iAcademicAreas; }
	public void setAcademicAreas(Set<AcademicArea> academicAreas) { iAcademicAreas = academicAreas; }
	public void addToacademicAreas(AcademicArea academicArea) {
		if (iAcademicAreas == null) iAcademicAreas = new HashSet<AcademicArea>();
		iAcademicAreas.add(academicArea);
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof PosMajor)) return false;
		if (getUniqueId() == null || ((PosMajor)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((PosMajor)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "PosMajor["+getUniqueId()+" "+getName()+"]";
	}

	public String toDebugString() {
		return "PosMajor[" +
			"\n	Code: " + getCode() +
			"\n	ExternalUniqueId: " + getExternalUniqueId() +
			"\n	Name: " + getName() +
			"\n	Session: " + getSession() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
