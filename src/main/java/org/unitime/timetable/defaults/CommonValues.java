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
package org.unitime.timetable.defaults;

/**
 * @author Tomas Muller
 */
public enum CommonValues {
	NoteAsIcon("icon"),
	NoteAsFullText("full text"),
	NoteAsShortText("shortened text"),
	
	HorizontalGrid("horizontal"),
	VerticalGrid("vertical"),
	TextGrid("text"),
	
	NameLastFirst("last-first"),
	NameFirstLast("first-last"),
	NameInitialLast("initial-last"),
	NameLastInitial("last-initial"),
	NameFirstMiddleLast("first-middle-last"),
	NameShort("short"),
	
	Yes("yes"),
	No("no"),
	
	Ask("ask"),
	Always("always"),
	Never("never"),
	
	SortByLastName("Always by Last Name"),
	SortAsDisplayed("Natural Order (as displayed)"),
	
	UseSystemDefault("not set"),
	
	;

	String iValue;
	CommonValues(String value) {
		iValue = value;
	}
	
	public String value() { return iValue; }
	
	public boolean eq(String value) { return iValue.equals(value); }
	
	public boolean ne(String value) { return !eq(value); }
}
