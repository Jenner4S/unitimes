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
package org.unitime.timetable.onlinesectioning.test;

import java.util.ArrayList;
import java.util.List;

import org.unitime.timetable.gwt.shared.ClassAssignmentInterface;
import org.unitime.timetable.gwt.shared.CourseRequestInterface;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.onlinesectioning.OnlineSectioningServer;
import org.unitime.timetable.onlinesectioning.OnlineSectioningTestFwk;
import org.unitime.timetable.onlinesectioning.basic.GetRequest;
import org.unitime.timetable.onlinesectioning.solver.FindAssignmentAction;

/**
 * @author Tomas Muller
 */
public class FindAssignmentsTest extends OnlineSectioningTestFwk {
		
	public List<Operation> operations() {

		org.hibernate.Session hibSession = new _RootDAO().getSession();
		
		List<Operation> operations = new ArrayList<Operation>();
		
		for (final Long studentId: (List<Long>)hibSession.createQuery(
				"select s.uniqueId from Student s where s.session.uniqueId = :sessionId")
				.setLong("sessionId", getServer().getAcademicSession().getUniqueId()).list()) {
			
			CourseRequestInterface request = getServer().execute(createAction(GetRequest.class).forStudent(studentId), user());
			if (request == null || request.getCourses().isEmpty()) continue;
			
			operations.add(new Operation() {
				@Override
				public double execute(OnlineSectioningServer s) {
					CourseRequestInterface request = s.execute(createAction(GetRequest.class).forStudent(studentId), user());
					if (request != null && !request.getCourses().isEmpty()) {
						FindAssignmentAction action = s.createAction(FindAssignmentAction.class).forRequest(request).withAssignment(new ArrayList<ClassAssignmentInterface.ClassAssignment>()); 
						List<ClassAssignmentInterface> ret = s.execute(action, user());
						return ret == null || ret.isEmpty() ? 0.0 : ret.get(0).getValue();
					} else {
						return 1.0;
					}
				}
			});
		}
		
		hibSession.close();

		return operations;
	}
	
	public static void main(String[] args) {
		new FindAssignmentsTest().test(-1, 1, 2, 5, 10, 20, 50, 100, 250, 1000);
	}
}
