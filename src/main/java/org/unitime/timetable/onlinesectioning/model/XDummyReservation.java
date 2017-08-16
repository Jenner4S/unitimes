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
package org.unitime.timetable.onlinesectioning.model;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author Tomas Muller
 */
@SerializeWith(XDummyReservation.XDummyReservationSerializer.class)
public class XDummyReservation extends XReservation {
	private static final long serialVersionUID = 1L;

	public XDummyReservation() {
		super();
	}
	
	public XDummyReservation(ObjectInput in) throws IOException, ClassNotFoundException {
		super();
		readExternal(in);
	}
	
	public XDummyReservation(XOffering offering) {
		super(XReservationType.Dummy, offering, null);
	}

    /**
     * Dummy reservation is unlimited
     */
    @Override
    public int getReservationLimit() {
        return -1;
    }

    /**
     * Dummy reservation is not applicable to any students
     */
    @Override
    public boolean isApplicable(XStudent student) {
        return false;
    }

	public static class XDummyReservationSerializer implements Externalizer<XDummyReservation> {
		private static final long serialVersionUID = 1L;

		@Override
		public void writeObject(ObjectOutput output, XDummyReservation object) throws IOException {
			object.writeExternal(output);
		}

		@Override
		public XDummyReservation readObject(ObjectInput input) throws IOException, ClassNotFoundException {
			return new XDummyReservation(input);
		}
	}
}