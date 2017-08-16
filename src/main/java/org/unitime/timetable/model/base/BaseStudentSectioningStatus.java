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

import org.unitime.timetable.model.CourseType;
import org.unitime.timetable.model.RefTableEntry;
import org.unitime.timetable.model.StudentSectioningStatus;

/**
 * Do not change this class. It has been automatically generated using ant create-model.
 * @see org.unitime.commons.ant.CreateBaseModelFromXml
 */
public abstract class BaseStudentSectioningStatus extends RefTableEntry implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer iStatus;
	private String iMessage;

	private Set<CourseType> iTypes;

	public static String PROP_STATUS = "status";
	public static String PROP_MESSAGE = "message";

	public BaseStudentSectioningStatus() {
		initialize();
	}

	public BaseStudentSectioningStatus(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Integer getStatus() { return iStatus; }
	public void setStatus(Integer status) { iStatus = status; }

	public String getMessage() { return iMessage; }
	public void setMessage(String message) { iMessage = message; }

	public Set<CourseType> getTypes() { return iTypes; }
	public void setTypes(Set<CourseType> types) { iTypes = types; }
	public void addTotypes(CourseType courseType) {
		if (iTypes == null) iTypes = new HashSet<CourseType>();
		iTypes.add(courseType);
	}

	public boolean equals(Object o) {
		if (o == null || !(o instanceof StudentSectioningStatus)) return false;
		if (getUniqueId() == null || ((StudentSectioningStatus)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((StudentSectioningStatus)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "StudentSectioningStatus["+getUniqueId()+" "+getLabel()+"]";
	}

	public String toDebugString() {
		return "StudentSectioningStatus[" +
			"\n	Label: " + getLabel() +
			"\n	Message: " + getMessage() +
			"\n	Reference: " + getReference() +
			"\n	Status: " + getStatus() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
