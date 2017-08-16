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

import java.util.List;

import org.unitime.timetable.model.OfferingCoordinator;
import org.unitime.timetable.model.dao._RootDAO;
import org.unitime.timetable.model.dao.OfferingCoordinatorDAO;

/**
 * Do not change this class. It has been automatically generated using ant create-model.
 * @see org.unitime.commons.ant.CreateBaseModelFromXml
 */
public abstract class BaseOfferingCoordinatorDAO extends _RootDAO<OfferingCoordinator,Long> {

	private static OfferingCoordinatorDAO sInstance;

	public static OfferingCoordinatorDAO getInstance() {
		if (sInstance == null) sInstance = new OfferingCoordinatorDAO();
		return sInstance;
	}

	public Class<OfferingCoordinator> getReferenceClass() {
		return OfferingCoordinator.class;
	}

	@SuppressWarnings("unchecked")
	public List<OfferingCoordinator> findByInstructor(org.hibernate.Session hibSession, Long instructorId) {
		return hibSession.createQuery("from OfferingCoordinator x where x.instructor.uniqueId = :instructorId").setLong("instructorId", instructorId).list();
	}

	@SuppressWarnings("unchecked")
	public List<OfferingCoordinator> findByOffering(org.hibernate.Session hibSession, Long offeringId) {
		return hibSession.createQuery("from OfferingCoordinator x where x.offering.uniqueId = :offeringId").setLong("offeringId", offeringId).list();
	}

	@SuppressWarnings("unchecked")
	public List<OfferingCoordinator> findByResponsibility(org.hibernate.Session hibSession, Long responsibilityId) {
		return hibSession.createQuery("from OfferingCoordinator x where x.responsibility.uniqueId = :responsibilityId").setLong("responsibilityId", responsibilityId).list();
	}

	@SuppressWarnings("unchecked")
	public List<OfferingCoordinator> findByTeachingRequest(org.hibernate.Session hibSession, Long teachingRequestId) {
		return hibSession.createQuery("from OfferingCoordinator x where x.teachingRequest.uniqueId = :teachingRequestId").setLong("teachingRequestId", teachingRequestId).list();
	}
}
