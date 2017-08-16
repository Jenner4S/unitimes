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
package org.unitime.timetable.model;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.unitime.timetable.model.base.BaseCurriculumClassification;



/**
 * @author Tomas Muller
 */
public class CurriculumClassification extends BaseCurriculumClassification implements Comparable<CurriculumClassification> {
	private static Log sLog = LogFactory.getLog(CurriculumClassification.class);
	private static final long serialVersionUID = 1L;

/*[CONSTRUCTOR MARKER BEGIN]*/
	public CurriculumClassification () {
		super();
	}

	/**
	 * Constructor for primary key
	 */
	public CurriculumClassification (java.lang.Long uniqueId) {
		super(uniqueId);
	}

/*[CONSTRUCTOR MARKER END]*/

	public int compareTo(CurriculumClassification cc) {
		if (getAcademicClassification() != null && cc.getAcademicClassification() != null) {
			int cmp = getAcademicClassification().getCode().compareTo(cc.getAcademicClassification().getCode());
			if (cmp != 0) return cmp;
		}
	    if (getOrd()!=null && cc.getOrd()!=null && !getOrd().equals(cc.getOrd()))
	        return getOrd().compareTo(cc.getOrd());
	    int cmp = getName().compareToIgnoreCase(cc.getName());
	    if (cmp!=0) return cmp;
	    return (getUniqueId() == null ? new Long(-1) : getUniqueId()).compareTo(cc.getUniqueId() == null ? -1 : cc.getUniqueId());
	}
	
	public Document getStudentsDocument() {
		if (getStudents() == null) return null;
		try {
			return new SAXReader().read(new StringReader(getStudents()));
		} catch (Exception e) {
			sLog.warn("Failed to load cached students for " + getCurriculum().getAbbv() + " " + getName() + ": " + e.getMessage(), e);
			return null;
		}
	}
	
	public void setStudentsDocument(Document document) {
		try {
			if (document == null) {
				setStudents(null);
			} else {
				StringWriter string = new StringWriter();
				XMLWriter writer = new XMLWriter(string, OutputFormat.createCompactFormat());
				writer.write(document);
				writer.flush(); writer.close();
				setStudents(string.toString());
			}
		} catch (Exception e) {
			sLog.warn("Failed to store cached students for " + getCurriculum().getAbbv() + " " + getName() + ": " + e.getMessage(), e);
		}
	}
}