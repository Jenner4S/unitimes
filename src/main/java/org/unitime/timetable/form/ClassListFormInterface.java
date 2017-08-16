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
package org.unitime.timetable.form;

/**
 * @author Tomas Muller
 */
public interface ClassListFormInterface extends InstructionalOfferingListFormInterface{
	public String[] getSubjectAreaIds();
	public String getCourseNbr();
	
	public String getSortBy();
	public String getFilterManager();
	public String getFilterAssignedRoom();
	public String getFilterInstructor();
	public String getFilterIType();
	public int getFilterDayCode();
	public int getFilterStartSlot();
	public int getFilterLength();
	public boolean getSortByKeepSubparts();
	public boolean getShowCrossListedClasses();

	public boolean isReturnAllControlClassesForSubjects();
	public void setSortBy(String sortBy);
	public void setFilterAssignedRoom(String filterAssignedRoom);
	public void setFilterManager(String filterManager);
	public void setFilterIType(String filterIType);
	public void setFilterDayCode(int filterDayCode);
	public void setFilterStartSlot(int filterStartSlot);
	public void setFilterLength(int filterLength);
	public void setSortByKeepSubparts(boolean sortByKeepSubparts);
	public void setShowCrossListedClasses(boolean showCrossListedClasses);
	public boolean getIncludeCancelledClasses();
	public boolean getFilterNeedInstructor();
}
