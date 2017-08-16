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
package org.unitime.timetable.solver.curricula;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;


import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.unitime.timetable.model.CurriculumProjectionRule;
import org.unitime.timetable.model.InstructionalOffering;
import org.unitime.timetable.model.Session;

/**
 * @author Tomas Muller
 */
public class ProjectedStudentCourseDemands extends LastLikeStudentCourseDemands {
	private Hashtable<String,Hashtable<String,Hashtable<String, Float>>> iAreaClasfMajor2Proj = new Hashtable<String, Hashtable<String,Hashtable<String,Float>>>();
	
	public ProjectedStudentCourseDemands(DataProperties properties) {
		super(properties);
	}
	
	@Override
	public void init(org.hibernate.Session hibSession, Progress progress, Session session, Collection<InstructionalOffering> offerings) {
		super.init(hibSession, progress, session, offerings);
		progress.setPhase("Loading curriculum projections", 1);
		for (CurriculumProjectionRule rule: (List<CurriculumProjectionRule>)hibSession.createQuery(
				"select r from CurriculumProjectionRule r where r.academicArea.session.uniqueId=:sessionId")
				.setLong("sessionId", session.getUniqueId()).setCacheable(true).list()) {
			String areaAbbv = rule.getAcademicArea().getAcademicAreaAbbreviation();
			String majorCode = (rule.getMajor() == null ? "" : rule.getMajor().getCode());
			String clasfCode = rule.getAcademicClassification().getCode();
			Float projection = rule.getProjection();
			Hashtable<String,Hashtable<String, Float>> clasf2major2proj = iAreaClasfMajor2Proj.get(areaAbbv);
			if (clasf2major2proj == null) {
				clasf2major2proj = new Hashtable<String, Hashtable<String,Float>>();
				iAreaClasfMajor2Proj.put(areaAbbv, clasf2major2proj);
			}
			Hashtable<String, Float> major2proj = clasf2major2proj.get(clasfCode);
			if (major2proj == null) {
				major2proj = new Hashtable<String, Float>();
				clasf2major2proj.put(clasfCode, major2proj);
			}
			major2proj.put(majorCode, projection);
		}
		progress.incProgress();
	}

	@Override
	public float getProjection(String areaAbbv, String clasfCode, String majorCode) {
		if (iAreaClasfMajor2Proj.isEmpty()) return 1.0f;
		Hashtable<String,Hashtable<String, Float>> clasf2major2proj = (areaAbbv == null ? null : iAreaClasfMajor2Proj.get(areaAbbv));
		if (clasf2major2proj == null || clasf2major2proj.isEmpty()) return 1.0f;
		Hashtable<String, Float> major2proj = (clasfCode == null ? null : clasf2major2proj.get(clasfCode));
		if (major2proj == null) return 1.0f;
		Float projection = (majorCode == null ? null : major2proj.get(majorCode));
		if (projection == null)
			projection = major2proj.get("");
		return (projection == null ? 1.0f : projection);
	}
}
