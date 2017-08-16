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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.DecimalFormat;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.cpsolver.exam.Test;
import org.cpsolver.exam.model.Exam;
import org.cpsolver.exam.model.ExamPlacement;
import org.cpsolver.ifs.assignment.Assignment;
import org.cpsolver.ifs.assignment.DefaultSingleAssignment;
import org.cpsolver.ifs.solution.Solution;
import org.cpsolver.ifs.solution.SolutionListener;
import org.cpsolver.ifs.solver.Solver;
import org.cpsolver.ifs.util.DataProperties;
import org.cpsolver.ifs.util.Progress;
import org.cpsolver.ifs.util.ProgressWriter;
import org.cpsolver.ifs.util.ToolBox;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.unitime.commons.hibernate.util.HibernateUtil;

/**
 * @author Tomas Muller
 */
public class ExamTest {
    private static Log sLog = LogFactory.getLog(ExamTest.class);
    
    public static class ShutdownHook extends Thread {
        Solver iSolver = null;
        public ShutdownHook(Solver solver) {
            setName("ShutdownHook");
            iSolver = solver;
        }
        public void run() {
            try {
                if (iSolver.isRunning()) iSolver.stopSolver();
                Solution solution = iSolver.lastSolution();
                if (solution.getBestInfo()==null) {
                    sLog.error("No best solution found.");
                } else solution.restoreBest();
                
                sLog.info("Best solution:"+ToolBox.dict2string(solution.getExtendedInfo(),1));
                
                sLog.info("Best solution found after "+solution.getBestTime()+" seconds ("+solution.getBestIteration()+" iterations).");
                sLog.info("Number of assigned variables is "+solution.getModel().nrAssignedVariables(solution.getAssignment()));
                sLog.info("Total value of the solution is "+solution.getModel().getTotalValue(solution.getAssignment()));
                
                if (iSolver.getProperties().getPropertyBoolean("General.Save", false))
                    new ExamDatabaseSaver(iSolver).save();
                
                File outFile = new File(iSolver.getProperties().getProperty("General.OutputFile",iSolver.getProperties().getProperty("General.Output")+File.separator+"solution.xml"));
                FileOutputStream fos = new FileOutputStream(outFile);
                (new XMLWriter(fos,OutputFormat.createPrettyPrint())).write(((ExamModel)solution.getModel()).save(solution.getAssignment()));
                fos.flush();fos.close();
                
                Test.createReports((ExamModel)solution.getModel(), solution.getAssignment(), outFile.getParentFile(), outFile.getName().substring(0,outFile.getName().lastIndexOf('.')));
                
                Progress.removeInstance(solution.getModel());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            DataProperties cfg = new DataProperties();
            if (args.length>=1) {
                cfg.load(new FileInputStream(args[0]));
            }
            cfg.putAll(System.getProperties());
            
            if (args.length>=2) {
                File logFile = new File(ToolBox.configureLogging(args[1], cfg, true, false));
                cfg.setProperty("General.Output", logFile.getParentFile().getAbsolutePath());
            } else {
                ToolBox.configureLogging();
                cfg.setProperty("General.Output", System.getProperty("user.home", ".")+File.separator+"Exam-Test");
            }
            if (!"true".equals(System.getProperty("debug","false")))
                Logger.getRootLogger().setLevel(Level.INFO);
            
            HibernateUtil.configureHibernate(cfg);
            
            ExamModel model = new ExamModel(cfg);
            Assignment<Exam, ExamPlacement> assignment = new DefaultSingleAssignment<Exam, ExamPlacement>();
            Progress.getInstance(model).addProgressListener(new ProgressWriter(System.out));
            try {
                new ExamDatabaseLoader(model, assignment).load();
            } catch (Exception e) {
                sLog.error("Unable to load problem, reason: "+e.getMessage(),e);
                return;
            }
            
            Solver solver = new Solver(cfg);
            solver.setInitalSolution(new Solution(model, assignment));
            
            solver.currentSolution().addSolutionListener(new SolutionListener() {
                public void solutionUpdated(Solution solution) {}
                public void getInfo(Solution solution, java.util.Map info) {}
                public void getInfo(Solution solution, java.util.Map info, java.util.Collection variables) {}
                public void bestCleared(Solution solution) {}
                public void bestSaved(Solution solution) {
                    ExamModel m = (ExamModel)solution.getModel();
                    Assignment<Exam, ExamPlacement> a = solution.getAssignment();
                    if (sLog.isInfoEnabled()) {
                        sLog.info("**BEST["+solution.getIteration()+"]** "+
                                (m.nrUnassignedVariables(a)>0?"V:"+m.nrAssignedVariables(a)+"/"+m.variables().size()+" - ":"")+
                                "T:"+new DecimalFormat("0.00").format(m.getTotalValue(a))+" ("+m+")");
                    }
                }
                public void bestRestored(Solution solution) {}
            });
            
            Runtime.getRuntime().addShutdownHook(new ShutdownHook(solver));
            
            solver.start();
            try {
                solver.getSolverThread().join();
            } catch (InterruptedException e) {}
            
        } catch (Exception e) {
            sLog.error(e.getMessage(),e);
            e.printStackTrace();
        }
    }
}
