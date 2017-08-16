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
package org.unitime.timetable.solver.exam;

import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamModel;
import org.cpsolver.exam.model.ExamPeriodPlacement;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.exam.model.ExamRoomPlacement;
import org.cpsolver.exam.model.ExamRoomSharing;
import org.cpsolver.exam.model.ExamStudent;
import org.cpsolver.ifs.assignment.Assignment;
import org.unitime.timetable.solver.exam.ui.ExamAssignment;
import org.unitime.timetable.solver.exam.ui.ExamAssignmentInfo;
import org.unitime.timetable.solver.exam.ui.ExamProposedChange;

/**
 * @author Tomas Muller
 */
public class ExamSuggestions {
    private ExamSolver iSolver;
    private ExamModel iModel;
    private Assignment<Exam, ExamPlacement> iAssignment;
    private Hashtable<Exam,ExamPlacement> iInitialAssignment;
    private Hashtable<Exam,ExamAssignment> iInitialInfo;
    private Vector<Exam> iInitialUnassignment;
    
    private TreeSet<ExamProposedChange> iSuggestions;
    private Vector<Exam> iResolvedExams;
    private Hashtable<Exam,ExamPlacement> iConflictsToResolve;
    private Exam iExam;
    
    private int iDepth = 2;
    private int iLimit = 20;
    private int iNrSolutions = 0, iNrCombinationsConsidered = 0;
    private long iTimeOut = 5000;
    private long iStartTime = 0;
    private boolean iTimeoutReached = false;
    private String iFilter = null;
    
    public ExamSuggestions(ExamSolver solver) {
        iSolver = solver;
        iModel = (ExamModel)solver.currentSolution().getModel();
        iAssignment = solver.currentSolution().getAssignment();
        iInitialAssignment = new Hashtable();
        iInitialUnassignment = new Vector();
        iInitialInfo = new Hashtable();
        for (Exam exam: iModel.variables()) {
            ExamPlacement placement = iAssignment.getValue(exam);
            if (placement==null) {
                iInitialUnassignment.add(exam);
            } else {
                iInitialAssignment.put(exam, placement);
                iInitialInfo.put(exam, new ExamAssignment(exam, placement, iAssignment));
            }
        }
    }
    
    public int getDepth() { return iDepth; }
    public void setDepth(int depth) { iDepth = depth; }
    public int getLimit() { return iLimit; }
    public void setLimit(int limit) { iLimit = limit; }
    public long getTimeOut() { return iTimeOut; }
    public void setTimeOut(long timeOut) { iTimeOut = timeOut; }
    public String getFilter() { return iFilter; }
    public void setFilter(String filter) { iFilter = filter; }
    
    public int getNrSolutions() { return iNrSolutions; }
    public int getNrCombinationsConsidered() { return iNrCombinationsConsidered; }
    public boolean wasTimeoutReached() { return iTimeoutReached; }
    
    public boolean match(String name) {
        if (iFilter==null || iFilter.trim().length()==0) return true;
        String n = name.toUpperCase();
        StringTokenizer stk1 = new StringTokenizer(iFilter.toUpperCase(),";");
        while (stk1.hasMoreTokens()) {
            StringTokenizer stk2 = new StringTokenizer(stk1.nextToken()," ,");
            boolean match = true;
            while (match && stk2.hasMoreTokens()) {
                String token = stk2.nextToken().trim();
                if (token.length()==0) continue;
                if (token.indexOf('*')>=0 || token.indexOf('?')>=0) {
                    try {
                        String tokenRegExp = "\\s+"+token.replaceAll("\\.", "\\.").replaceAll("\\?", ".+").replaceAll("\\*", ".*")+"\\s";
                        if (!Pattern.compile(tokenRegExp).matcher(" "+n+" ").find()) match = false;
                    } catch (PatternSyntaxException e) { match = false; }
                } else if (n.indexOf(token)<0) match = false;
            }
            if (match) return true;
        }
        return false;        
    }
    
    public synchronized TreeSet<ExamProposedChange> computeSuggestions(Exam exam, Collection<ExamAssignmentInfo> givenAssignments) {
        iSuggestions = new TreeSet<ExamProposedChange>();
        
        iResolvedExams = new Vector();
        iConflictsToResolve = new Hashtable();
        iNrSolutions = 0;
        iNrCombinationsConsidered = 0;
        iTimeoutReached = false;
        iExam = exam;
        
        if (givenAssignments!=null) {
            for (ExamAssignment assignment : givenAssignments) {
                ExamPlacement placement = iSolver.getPlacement(assignment);
                if (placement==null) continue;
                if (placement.variable().equals(exam)) continue;
                Set conflicts = iModel.conflictValues(iAssignment, placement);
                for (Iterator i=conflicts.iterator();i.hasNext();) {
                    ExamPlacement conflictPlacement = (ExamPlacement)i.next();
                    iConflictsToResolve.put((Exam)conflictPlacement.variable(),conflictPlacement);
                    iAssignment.unassign(0, conflictPlacement.variable());
                }
                iResolvedExams.add((Exam)placement.variable());
                iConflictsToResolve.remove((Exam)placement.variable());
                iAssignment.assign(0,placement);
            }
        }
        
        iStartTime= System.currentTimeMillis();
        backtrack(iDepth);
        
        for (Exam x : iInitialUnassignment)
            if (iAssignment.getValue(x)!=null) iAssignment.unassign(0, x);
        for (Map.Entry<Exam, ExamPlacement> x : iInitialAssignment.entrySet())
            if (!x.getValue().equals(iAssignment.getValue(x.getKey()))) iAssignment.assign(0, x.getValue());
        
        return iSuggestions;
    }
    
    public Set findBestAvailableRooms(Exam exam, ExamPeriodPlacement period, boolean checkConstraints) {
        if (exam.getMaxRooms()==0) return new HashSet();
        ExamRoomSharing sharing = iModel.getRoomSharing();
        loop: for (int nrRooms=1;nrRooms<=exam.getMaxRooms();nrRooms++) {
            HashSet rooms = new HashSet(); int size = 0;
            while (rooms.size()<nrRooms && size<exam.getSize()) {
                int minSize = (exam.getSize()-size)/(nrRooms-rooms.size());
                ExamRoomPlacement best = null; int bestSize = 0, bestPenalty = 0;
                for (ExamRoomPlacement room: exam.getRoomPlacements()) {
                    if (!room.isAvailable(period.getPeriod())) continue;
                    if (checkConstraints) {
                        if (nrRooms == 1 && sharing != null) {
                            if (sharing.inConflict(exam, room.getRoom().getPlacements(iAssignment, period.getPeriod()), room.getRoom()))
                                continue;
                        } else {
                            if (!room.getRoom().getPlacements(iAssignment, period.getPeriod()).isEmpty())
                                continue;
                        }
                    }
                    if (rooms.contains(room)) continue;
                    if (checkConstraints && !exam.checkDistributionConstraints(iAssignment, room)) continue;
                    int s = room.getSize(exam.hasAltSeating());
                    if (s<minSize) break;
                    int p = room.getPenalty(period.getPeriod());
                    if (best==null || bestPenalty>p || (bestPenalty==p && bestSize>s)) {
                        best = room;
                        bestSize = s;
                        bestPenalty = p;
                    }
                }
                if (best==null) continue loop;
                rooms.add(best); size+=bestSize;
            }
            if (size>=exam.getSize()) return rooms;
        }
        return null;
    }
    
    private void tryPlacement(ExamPlacement placement, int depth) {
        if (placement.equals(iAssignment.getValue(placement.variable()))) return;
        if (placement.variable().equals(iExam) && !match(placement.getPeriod().toString()+" "+placement.getRoomName(", "))) return;
        Set conflicts = iModel.conflictValues(iAssignment, placement);
        tryPlacement(placement, depth, conflicts);
        if (iConflictsToResolve.size()+conflicts.size()<depth) {
            Exam exam = (Exam)placement.variable();
            HashSet adepts = new HashSet();
            for (ExamStudent s: exam.getStudents()) {
                Set exams = s.getExams(iAssignment, placement.getPeriod());
                for (Iterator i=exams.iterator();i.hasNext();) {
                    ExamPlacement conf = iAssignment.getValue((Exam)i.next());
                    if (conf==null || conflicts.contains(conf)) continue;
                    if (iResolvedExams.contains((Exam)conf.variable())) continue;
                    adepts.add(conf);
                }
            }
            for (Iterator i=adepts.iterator();i.hasNext();) {
                ExamPlacement adept = (ExamPlacement)i.next();
                conflicts.add(adept);
                tryPlacement(placement, depth, conflicts);
                conflicts.remove(adept);
            }
            if (iConflictsToResolve.size()+conflicts.size()+1<depth) {
                for (Iterator i1=adepts.iterator();i1.hasNext();) {
                    ExamPlacement a1 = (ExamPlacement)i1.next();
                    conflicts.add(a1);
                    for (Iterator i2=adepts.iterator();i2.hasNext();) {
                        ExamPlacement a2 = (ExamPlacement)i2.next();
                        if (a2.variable().getId()>=a1.variable().getId()) continue;
                        conflicts.add(a2);
                        tryPlacement(placement, depth, conflicts);
                        conflicts.remove(a2);
                    }
                    conflicts.remove(a1);
                }
            }
        }
    }
    
    private void tryPlacement(ExamPlacement placement, int depth, Set conflicts) {
        iNrCombinationsConsidered++;
        if (iConflictsToResolve.size()+conflicts.size()>depth) return;
        for (Iterator i=conflicts.iterator();i.hasNext();) {
            ExamPlacement c = (ExamPlacement)i.next();
            if (iResolvedExams.contains((Exam)c.variable())) return;
        }
        Exam exam = (Exam)placement.variable();
        ExamPlacement cur = iAssignment.getValue(exam);
        if (conflicts!=null) {
            for (Iterator i=conflicts.iterator(); i.hasNext();) {
                ExamPlacement c = (ExamPlacement)i.next();
                iAssignment.unassign(0, c.variable());
            }
        }
        iAssignment.assign(0, placement);
        for (Iterator i=conflicts.iterator();i.hasNext();) {
            ExamPlacement c = (ExamPlacement)i.next();
            iConflictsToResolve.put((Exam)c.variable(),c);
        }
        ExamPlacement resolvedConf = iConflictsToResolve.remove(exam);
        backtrack(depth-1);
        if (cur==null)
        	iAssignment.unassign(0, exam);
        else
        	iAssignment.assign(0, cur);
        for (Iterator i=conflicts.iterator();i.hasNext();) {
            ExamPlacement p = (ExamPlacement)i.next();
            iAssignment.assign(0, p);
            iConflictsToResolve.remove((Exam)p.variable());
        }
        if (resolvedConf!=null)
            iConflictsToResolve.put(exam, resolvedConf);
    }
    
    private void backtrack(int depth) {
        if (iDepth>depth && iConflictsToResolve.isEmpty()) {
            if (iSuggestions.size()==iLimit && iSuggestions.last().isBetter(iModel, iAssignment)) return;
            iSuggestions.add(new ExamProposedChange(iModel, iAssignment, iInitialAssignment, iInitialInfo, iConflictsToResolve.values(), iResolvedExams));
            iNrSolutions++;
            if (iSuggestions.size()>iLimit) iSuggestions.remove(iSuggestions.last());
            return;
        }
        if (depth<=0) return;
        if (iTimeOut>0 && System.currentTimeMillis()-iStartTime>iTimeOut) {
            iTimeoutReached = true;
            return;
        }
        Exam exam = (iDepth==depth && !iResolvedExams.contains(iExam)?iExam:iConflictsToResolve.keys().nextElement());
        if (iResolvedExams.contains(exam)) return;
        iResolvedExams.add(exam);
        for (ExamPeriodPlacement period: exam.getPeriodPlacements()) {
            //if (exam.equals(iExam) && !match(period.getPeriod().toString())) continue;
            Set rooms = findBestAvailableRooms(exam, period, true);
            if (rooms!=null) {
                tryPlacement(new ExamPlacement(exam, period, rooms), depth);
            } else {
                rooms = findBestAvailableRooms(exam, period, false);
                if (rooms!=null) tryPlacement(new ExamPlacement(exam, period, rooms), depth);
            }
        }
        iResolvedExams.remove(exam);
    }
}
