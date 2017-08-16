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
package org.unitime.timetable.dataexchange;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.Element;
import org.unitime.timetable.model.Session;
import org.unitime.timetable.model.Student;
import org.unitime.timetable.model.StudentAccomodation;
import org.unitime.timetable.model.StudentAreaClassificationMajor;
import org.unitime.timetable.model.StudentAreaClassificationMinor;
import org.unitime.timetable.model.StudentGroup;

/**
 * @author Tomas Muller
 */
public class StudentExport extends BaseExport {
	
	@Override
	public void saveXml(Document document, Session session, Properties parameters) throws Exception {
		try {
			beginTransaction();
			
			Element root = document.addElement("students");
	        root.addAttribute("campus", session.getAcademicInitiative());
	        root.addAttribute("year", session.getAcademicYear());
	        root.addAttribute("term", session.getAcademicTerm());
	        
	        document.addDocType("students", "-//UniTime//UniTime Students DTD/EN", "http://www.unitime.org/interface/Student.dtd");
	        
	        for (Student student: (List<Student>)getHibSession().createQuery(
	        		"select s from Student s where s.session.uniqueId = :sessionId")
	        		.setLong("sessionId", session.getUniqueId()).list()) {
	        	
	        	Element studentEl = root.addElement("student");
	        	exportStudent(studentEl, student);
	        }
	        
            commitTransaction();
        } catch (Exception e) {
            fatal("Exception: "+e.getMessage(),e);
            rollbackTransaction();
		}
	}

	protected void exportStudent(Element studentEl, Student student) {
    	studentEl.addAttribute("externalId",
    			student.getExternalUniqueId() == null || student.getExternalUniqueId().isEmpty() ? student.getUniqueId().toString() : student.getExternalUniqueId());
    	
    	if (student.getFirstName() != null)
    		studentEl.addAttribute("firstName", student.getFirstName());
    	if (student.getMiddleName() != null)
    		studentEl.addAttribute("middleName", student.getMiddleName());
    	if (student.getLastName() != null)
    		studentEl.addAttribute("lastName", student.getLastName());
    	if (student.getEmail() != null)
    		studentEl.addAttribute("email", student.getEmail());
    	
    	if (!student.getAreaClasfMajors().isEmpty() || !student.getGroups().isEmpty()) {
    		Element e = studentEl.addElement("studentAcadAreaClass");
    		Set<String> ac = new HashSet<String>();
    		for (StudentAreaClassificationMajor aac: student.getAreaClasfMajors()) {
    			if (ac.add(aac.getAcademicArea().getAcademicAreaAbbreviation() + "|" + aac.getAcademicClassification().getCode()))
            		e.addElement("acadAreaClass")
            		.addAttribute("academicArea", aac.getAcademicArea().getAcademicAreaAbbreviation())
            		.addAttribute("academicClass", aac.getAcademicClassification().getCode());
    		}
    		for (StudentAreaClassificationMinor aac: student.getAreaClasfMinors()) {
    			if (ac.add(aac.getAcademicArea().getAcademicAreaAbbreviation() + "|" + aac.getAcademicClassification().getCode()))
            		e.addElement("acadAreaClass")
            		.addAttribute("academicArea", aac.getAcademicArea().getAcademicAreaAbbreviation())
            		.addAttribute("academicClass", aac.getAcademicClassification().getCode());
    		}
    	}
    	
    	if (!student.getAreaClasfMajors().isEmpty()) {
    		Element e = studentEl.addElement("studentMajors");
        	for (StudentAreaClassificationMajor aac: student.getAreaClasfMajors()) {
        		e.addElement("major")
        		.addAttribute("academicArea", aac.getAcademicArea().getAcademicAreaAbbreviation())
        		.addAttribute("academicClass", aac.getAcademicClassification().getCode())
        		.addAttribute("code", aac.getMajor().getCode());
        	}
    	}

    	if (!student.getAreaClasfMinors().isEmpty()) {
    		Element e = studentEl.addElement("studentMinors");
        	for (StudentAreaClassificationMinor aac: student.getAreaClasfMinors()) {
        		e.addElement("minor")
        		.addAttribute("academicArea", aac.getAcademicArea().getAcademicAreaAbbreviation())
        		.addAttribute("academicClass", aac.getAcademicClassification().getCode())
        		.addAttribute("code", aac.getMinor().getCode());
        	}
    	}
    	
    	if (!student.getGroups().isEmpty()) {
    		Element e = studentEl.addElement("studentGroups");
        	for (StudentGroup group: student.getGroups()) {
        		e.addElement("studentGroup").addAttribute("group", group.getGroupAbbreviation());
        	}
    	}
    	
    	if (!student.getAccomodations().isEmpty()) {
    		Element e = studentEl.addElement("studentAccomodations");
        	for (StudentAccomodation accomodation: student.getAccomodations()) {
        		e.addElement("studentAccomodation").addAttribute("accomodation", accomodation.getAbbreviation());
        	}
    	}		
	}

}
