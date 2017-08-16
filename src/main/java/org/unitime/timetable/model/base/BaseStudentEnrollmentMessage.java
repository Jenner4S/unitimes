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

import org.unitime.timetable.model.CourseDemand;
import org.unitime.timetable.model.StudentEnrollmentMessage;

/**
 * Do not change this class. It has been automatically generated using ant create-model.
 * @see org.unitime.commons.ant.CreateBaseModelFromXml
 */
public abstract class BaseStudentEnrollmentMessage implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;
	private String iMessage;
	private Integer iLevel;
	private Integer iType;
	private Date iTimestamp;
	private Integer iOrder;

	private CourseDemand iCourseDemand;

	public static String PROP_UNIQUEID = "uniqueId";
	public static String PROP_MESSAGE = "message";
	public static String PROP_MSG_LEVEL = "level";
	public static String PROP_TYPE = "type";
	public static String PROP_TIMESTAMP = "timestamp";
	public static String PROP_ORD = "order";

	public BaseStudentEnrollmentMessage() {
		initialize();
	}

	public BaseStudentEnrollmentMessage(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public String getMessage() { return iMessage; }
	public void setMessage(String message) { iMessage = message; }

	public Integer getLevel() { return iLevel; }
	public void setLevel(Integer level) { iLevel = level; }

	public Integer getType() { return iType; }
	public void setType(Integer type) { iType = type; }

	public Date getTimestamp() { return iTimestamp; }
	public void setTimestamp(Date timestamp) { iTimestamp = timestamp; }

	public Integer getOrder() { return iOrder; }
	public void setOrder(Integer order) { iOrder = order; }

	public CourseDemand getCourseDemand() { return iCourseDemand; }
	public void setCourseDemand(CourseDemand courseDemand) { iCourseDemand = courseDemand; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof StudentEnrollmentMessage)) return false;
		if (getUniqueId() == null || ((StudentEnrollmentMessage)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((StudentEnrollmentMessage)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "StudentEnrollmentMessage["+getUniqueId()+"]";
	}

	public String toDebugString() {
		return "StudentEnrollmentMessage[" +
			"\n	CourseDemand: " + getCourseDemand() +
			"\n	Level: " + getLevel() +
			"\n	Message: " + getMessage() +
			"\n	Order: " + getOrder() +
			"\n	Timestamp: " + getTimestamp() +
			"\n	Type: " + getType() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
