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
package org.unitime.timetable.authenticate.jaas;

import java.io.Serializable;
import java.security.Principal;

/**
 * Represents an authenticated and authorized timetable user
 *
 * @author Tomas Muller
 */
final public class AuthenticatedUser implements Principal, Serializable, HasExternalId {
	private static final long serialVersionUID = 11L;

	String iName, iExternalId;

	public AuthenticatedUser(String name, String externalId) {
		iName = name;
		iExternalId = externalId;
	}

	public boolean equals(Object obj) {
		return getName().equals(((Principal)obj).getName());
	}

	public int hashCode() {
		return getName().hashCode();
	}

	public String toString() {
		return getName();
	}

	public String getName() {
		return iName;
	}
	
	@Override
	public String getExternalId() {
		return iExternalId;
	}
}