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
package org.unitime.timetable.solver.instructor;

import java.util.List;

import org.unitime.timetable.gwt.shared.EventInterface.FilterRpcRequest;
import org.unitime.timetable.gwt.shared.InstructorInterface.AssignmentChangesRequest;
import org.unitime.timetable.gwt.shared.InstructorInterface.AssignmentChangesResponse;
import org.unitime.timetable.gwt.shared.InstructorInterface.AssignmentInfo;
import org.unitime.timetable.gwt.shared.InstructorInterface.ComputeSuggestionsRequest;
import org.unitime.timetable.gwt.shared.InstructorInterface.InstructorInfo;
import org.unitime.timetable.gwt.shared.InstructorInterface.SuggestionsResponse;
import org.unitime.timetable.gwt.shared.InstructorInterface.TeachingRequestInfo;
import org.unitime.timetable.solver.CommonSolverInterface;

/**
 * @author Tomas Muller
 */
public interface InstructorSchedulingProxy extends CommonSolverInterface {
	public List<TeachingRequestInfo> getTeachingRequests(FilterRpcRequest filter);
	public List<InstructorInfo> getInstructors(FilterRpcRequest filter);
	public TeachingRequestInfo getTeachingRequestInfo(Long requestId);
	public InstructorInfo getInstructorInfo(Long instructorId);
	public void assign(List<AssignmentInfo> assignments);
	public SuggestionsResponse computeSuggestions(ComputeSuggestionsRequest request);
	public AssignmentChangesResponse getAssignmentChanges(AssignmentChangesRequest request);
}
