/*
 * $Id: Luca.java b658fd839d45 2016-05-13 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.dsl.cosu;

import java.io.FileNotFoundException;
import oms3.dsl.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.ngmf.util.UnifiedParams;
import oms3.ngmf.util.cosu.luca.ExecutionHandle;
import oms3.ngmf.util.cosu.luca.SCE;
import oms3.ComponentAccess;
import oms3.ComponentException;
import oms3.Conversions;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.Initialize;
import oms3.dsl.cosu.Step.Data;
import oms3.io.DataIO;
import oms3.io.CSProperties;
import oms3.ngmf.util.cosu.luca.ParameterData;

// initial parameter settings (reading)
// calibration date/time settings.
// 
public class Luca extends AbstractSimulation {

    protected List<Step> steps = new ArrayList<>();
    //
    Date calib_start;           // Calibration start date
    int startMonthOfYear = 1;
    int rounds = 1;             // number of rounds
    String traceFileName = null;
    Date startTime;
    Date endTime;
    List<String> existingFiles = new ArrayList<>();
    SummaryWriter summary = new SummaryWriter();

    CollectOutput c;

    boolean model_stdout = true;
    boolean model_stderr = true;


    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals(Step.DSL_NAME)) {
            Step step = new Step(steps.size() + 1);
            steps.add(step);
            return step;
        } else if (name.equals("rounds")) {
            rounds = (Integer) value;
            if (rounds < 1) {
                throw new ComponentException("Illegal 'rounds': " + rounds);
            }
        } else if (name.equals("calibration_start")) {
            calib_start = Conversions.convert(value, Date.class);
        } else if (name.equals("run_start")) {
            startTime = Conversions.convert(value, Date.class);
        } else if (name.equals("run_end")) {
            endTime = Conversions.convert(value, Date.class);
        } else if (name.equals("start_month_of_year")) {
            startMonthOfYear = (Integer) value - 1;
            if ((startMonthOfYear < 0) || (startMonthOfYear > 11)) {
                throw new ComponentException("start_month_of_year must be between 1-12 for Jan-Dec.");
            }
        } else if (name.equals("summary_file")) {
            String fname = value.toString();
            summary.setFile(fname);
        } else if (name.equals("trace_file")) {
            traceFileName = value.toString();
        } else if (name.equals("model_stdout")) {
            model_stdout = (Boolean) value;
        } else if (name.equals("model_stderr")) {
            model_stderr = (Boolean) value;
        } else if (name.equals("collect_output")) {
            c = new CollectOutput();
            return c;
        } else {
            return super.create(name, value);
        }
        return LEAF;
    }


    @Override
    public Object run() throws Exception {

        super.run();

        if (calib_start == null) {
            throw new ComponentException("missing 'calibration_start'");
        }
        if (steps.isEmpty()) {
            throw new ComponentException("missing 'step' definition(s)");
        }

        ModelExecution exec = new ModelExecution();

        if (startTime == null && endTime == null) {
            Object start = exec.getParameter().getParamValue("startTime");
            Object end = exec.getParameter().getParamValue("endTime");

            if (start == null || end == null) {
                throw new ComponentException("missing: run_start/run_end or model parameter startTime/endTime: ");
            }

            startTime = Conversions.convert(start, Date.class);
            endTime = Conversions.convert(end, Date.class);
        }

        if (calib_start.after(endTime)) {
            throw new ComponentException("illegal calibration_start: " + calib_start);
        }
        for (Step step : steps) {
            step.init(exec, startMonthOfYear, calib_start, endTime, rounds);
        }

        // Send info to summary writer
        String outFolder = steps.get(0).outFolder.getAbsolutePath();
        summary.setModelInfo(rounds, steps.size(), getModelElement().getClassname(),
                getModelElement().getParams(), calib_start, startTime, endTime, outFolder);
        for (int s = 0; s < steps.size(); s++) {
            Step step = steps.get(s);
            Data stepData = step.round()[0];
            SCE initSce = new SCE(exec, step, stepData, exec.lastFolder, traceFileName, false);
            double origOF = initSce.getInitialOF();
            stepData.setObjFuncValueOfBestPoint(origOF);
            summary.writeInitialStep(s, step, stepData, origOF);
        }

        // Call SCE for each step of each round.               
        for (int r = 0; r < rounds; r++) {
            for (int s = 0; s < steps.size(); s++) {
                Step step = steps.get(s);
                Data stepData = step.round()[r];

                // 
                System.out.println("\n\n>>>>>>>>>>>>>>  Round [" + (r + 1) + "]  Step [" + step.getName() + "] <<<<<<<<<<<<<<");
                SCE sce = new SCE(exec, step, stepData, exec.lastFolder, traceFileName, true);
                double finalOF = sce.run(stepData.getObjFuncValueOfBestPoint());
                int numIterations = sce.getNumIterations();

                // Step step0 = steps.get(0);
                summary.writeStep(r, s, step, stepData, finalOF, numIterations);
                exec.writeParameterCopy(step, r);
                postToSteps(s, r);
                Runtime.getRuntime().gc();
            }
            for (int s = 0; s < steps.size(); s++) {
                Step step = steps.get(s);
                Data stepData = step.round()[r];
                step.post(r, stepData);
            }
        }
        summary.close();
        return null;
    }


    void postToSteps(int s, int r) {
        Step step = steps.get(s);
        Data stepData = step.round()[r];
        for (Step destStep : steps) {
            Data otherStepData = destStep.round()[r];
//            System.out.println("post Step " + step.getName() + "->" + destStep.getName());
            Data.copyParamValuesDiffStepsSameRound(stepData, otherStepData);
        }
    }

    // Model execution
    //
    final class ModelExecution implements ExecutionHandle {

        File lastFolder;
        UnifiedParams csParameter;
        Logger logger; // reference to hold on to.


        ModelExecution() throws IOException {
            lastFolder = getOutputPath();
            lastFolder.mkdirs();
            csParameter = getModelElement().getParameter();
            // write the initial parameter file (param-s0r0)
            writeParameterCopy(null, -1);
            // disable logging for all.
            logger = Logger.getLogger("oms3.model");
            logger.setLevel(Level.OFF);
            logger.setUseParentHandlers(false);
        }


        UnifiedParams getParameter() {
            return csParameter;
        }

        PrintStream nullOutStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });
        
        PrintStream nullErrStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        });


        @Override
        public void execute(Step s, Step.Data step, int it) throws Exception {

            PrintStream save_out = null;
            PrintStream save_err = null;

            if (!model_stdout) {
                System.out.flush();
                save_out = System.out;
                System.setOut(nullOutStream);
            }
            if (!model_stderr) {
                System.err.flush();
                save_err = System.err;
                System.setErr(nullErrStream);
            }

            // Path
            String libPath = getModelElement().getLibpath();
            if (libPath != null) {
                System.setProperty("jna.library.path", libPath);
                if (log.isLoggable(Level.CONFIG)) {
                    log.config("Setting jna.library.path to " + libPath);
                }
            }

            Object comp = getModelElement().newModelComponent();

            writeParameterFile(step);
            log.config("Init ...");
            ComponentAccess.callAnnotated(comp, Initialize.class, true);

            // setting the input data;
            boolean success = getParameter().setInputData(comp, log);
            if (!success) {
                throw new RuntimeException("There are Parameter problems. Simulation exits.");
            }

            ComponentAccess.adjustOutputPath(lastFolder, comp, log);
            for (Output e : getOut()) {
                e.setup(comp, lastFolder, getName());
            }
            // execute phases and be done.
            log.config("Exec ...");
            ComponentAccess.callAnnotated(comp, Execute.class, false);
            log.config("Finalize ...");
            ComponentAccess.callAnnotated(comp, Finalize.class, true);

            for (Output e : getOut()) {
                e.done();
            }

            if (save_out != null) {
                System.setOut(save_out);
            }

            if (save_err != null) {
                System.setErr(save_err);
            }

            if (c != null) {
                c.collect("# round: " + (step.getRound() + 1) + ", step: " + s.getNumber() + ", iter: " + it);
            }
        }


        @Override
        public void writeParameterFile(Step.Data step) {
            ParameterData[] paramData = step.paramData;
            for (int i = 0; i < paramData.length; i++) {
                String name = paramData[i].getName();
//                System.out.println("NAME " + name);                
                int calibType = paramData[i].getCalibrationType();
                double[] val = paramData[i].getDataValue();

//if (name.equals("gwflow_coef")) {
//    String a = Arrays.toString(val);
//    System.out.println("\n        write: gwflow_coef " + a.substring(0, 250));
//}                
                double[][] val2D = Conversions.unrollDoubleArray(val, paramData[i].getRowSize());
                Object valSel = (paramData[i].getRowSize() > 1) ? val2D : val;

                if (calibType == ParameterData.BINARY) {
                    int[] ival = Conversions.convert(val, int[].class);
                    csParameter.putParamValue(name, csParameter.toValueI(name, ival));
                } else {
                    csParameter.putParamValue(name, csParameter.toValue(name, valSel));
                }
            }
        }


        void writeParameterCopy(Step step, int round) throws FileNotFoundException {
            StringBuilder val = new StringBuilder();
            String name = "0";

            if (step != null) {
                name = step.getName();
                for (Param param : step.params.getParam()) {
                    val.append(" ").append(param.getName());
                }
            }

            StringBuilder files = new StringBuilder();
            for (Params param : getModelElement().getParams()) {
                if (param.getFile() != null) {
                    files.append(" ").append(param.getFile());
                }
            }

            File params = new File(lastFolder, "params-r" + (round + 1) + "s" + name + ".csv");
            System.out.println(" Final parameter file: '" + params + "'");
            PrintWriter pw = new PrintWriter(params);
            CSProperties csp = csParameter.getParams();
            csp.setName("Parameter");
            csp.getInfo().put("round", Integer.toString(round + 1));
            csp.getInfo().put("step", name);
            csp.getInfo().put("step_params", val.toString().trim());
            csp.getInfo().put("calibration_start", calib_start.toString());
            csp.getInfo().put("luca_name", getName());
            if (getModelElement().getClassname() != null) {
                csp.getInfo().put("model_class", getModelElement().getClassname());
            }
            if (files.length() > 0) {
                csp.getInfo().put("param_files", files.toString().trim());
            }
            DataIO.printWithTables(csp, pw);
            pw.close();
        }
    }
}
