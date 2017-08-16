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

import org.unitime.timetable.model.PitDepartmentalInstructor;
import org.unitime.timetable.model.PitInstructionalOffering;
import org.unitime.timetable.model.PitOfferingCoordinator;
import org.unitime.timetable.model.TeachingResponsibility;

/**
 * Do not change this class. It has been automatically generated using ant create-model.
 * @see org.unitime.commons.ant.CreateBaseModelFromXml
 */
public abstract class BasePitOfferingCoordinator implements Serializable {
	private static final long serialVersionUID = 1L;

	private Long iUniqueId;
	private Integer iPercentShare;

	private PitInstructionalOffering iPitInstructionalOffering;
	private PitDepartmentalInstructor iPitDepartmentalInstructor;
	private TeachingResponsibility iResponsibility;

	public static String PROP_UNIQUEID = "uniqueId";
	public static String PROP_PERCENT_SHARE = "percentShare";

	public BasePitOfferingCoordinator() {
		initialize();
	}

	public BasePitOfferingCoordinator(Long uniqueId) {
		setUniqueId(uniqueId);
		initialize();
	}

	protected void initialize() {}

	public Long getUniqueId() { return iUniqueId; }
	public void setUniqueId(Long uniqueId) { iUniqueId = uniqueId; }

	public Integer getPercentShare() { return iPercentShare; }
	public void setPercentShare(Integer percentShare) { iPercentShare = percentShare; }

	public PitInstructionalOffering getPitInstructionalOffering() { return iPitInstructionalOffering; }
	public void setPitInstructionalOffering(PitInstructionalOffering pitInstructionalOffering) { iPitInstructionalOffering = pitInstructionalOffering; }

	public PitDepartmentalInstructor getPitDepartmentalInstructor() { return iPitDepartmentalInstructor; }
	public void setPitDepartmentalInstructor(PitDepartmentalInstructor pitDepartmentalInstructor) { iPitDepartmentalInstructor = pitDepartmentalInstructor; }

	public TeachingResponsibility getResponsibility() { return iResponsibility; }
	public void setResponsibility(TeachingResponsibility responsibility) { iResponsibility = responsibility; }

	public boolean equals(Object o) {
		if (o == null || !(o instanceof PitOfferingCoordinator)) return false;
		if (getUniqueId() == null || ((PitOfferingCoordinator)o).getUniqueId() == null) return false;
		return getUniqueId().equals(((PitOfferingCoordinator)o).getUniqueId());
	}

	public int hashCode() {
		if (getUniqueId() == null) return super.hashCode();
		return getUniqueId().hashCode();
	}

	public String toString() {
		return "PitOfferingCoordinator["+getUniqueId()+"]";
	}

	public String toDebugString() {
		return "PitOfferingCoordinator[" +
			"\n	PercentShare: " + getPercentShare() +
			"\n	PitDepartmentalInstructor: " + getPitDepartmentalInstructor() +
			"\n	PitInstructionalOffering: " + getPitInstructionalOffering() +
			"\n	Responsibility: " + getResponsibility() +
			"\n	UniqueId: " + getUniqueId() +
			"]";
	}
}
