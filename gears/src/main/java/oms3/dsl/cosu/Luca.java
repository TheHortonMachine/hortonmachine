//package oms3.dsl.cosu;
//
//import java.io.FileNotFoundException;
//import oms3.dsl.*;
//import java.io.File;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import ngmf.util.OutputStragegy;
//import ngmf.util.cosu.luca.ExecutionHandle;
//import ngmf.util.cosu.luca.ParameterData;
//import ngmf.util.cosu.luca.SCE;
//import oms3.ComponentAccess;
//import oms3.ComponentException;
//import oms3.Compound;
//import oms3.Conversions;
//import oms3.annotations.Execute;
//import oms3.annotations.Finalize;
//import oms3.annotations.Initialize;
//import oms3.dsl.cosu.Step.Data;
//import oms3.io.DataIO;
//import ngmf.util.cosu.luca.ParameterData;
//
//// initial parameter settings (reading)
//// calibration date/time settings.
//// 
//public class Luca extends AbstractSimulation {
//
//    List<Step> steps = new ArrayList<Step>();
//    //
//    Date calib_start;           // Calibration start date
//    int rounds = 1;             // number of rounds
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        if (name.equals("step")) {
//            Step step = new Step(steps.size() + 1);
//            steps.add(step);
//            return step;
//        } else if (name.equals("rounds")) {
//            rounds = (Integer) value;
//            if (rounds < 1) {
//                throw new ComponentException("Illegal 'rounds': " + rounds);
//            }
//        } else if (name.equals("calibration_start")) {
//            calib_start = Conversions.convert(value, Date.class);
//        } else {
//            return super.create(name, value);
//        }
//        return LEAF;
//    }
//
//    @Override
//    public Object run() throws Exception {
//        if (getModel() == null) {
//            throw new ComponentException("missing 'model'.");
//        }
//        if (calib_start == null) {
//            throw new ComponentException("missing 'calibration_start'");
//        }
//        if (steps.isEmpty()) {
//            throw new ComponentException("missing 'step' definition(s)");
//        }
//
//        ModelExecution exec = new ModelExecution();
//        Object end = exec.getParameter().get("endTime");
//        Date endTime = Conversions.convert(end, Date.class);
//        if (calib_start.after(endTime)) {
//            throw new ComponentException("illegal calibration_start: " + calib_start);
//        }
//        for (Step step : steps) {
//            step.init(exec, calib_start, endTime, rounds);
//        }
//
//        for (int r = 0; r < rounds; r++) {
//            for (int s = 0; s < steps.size(); s++) {
//                Step step = steps.get(s);
//                Data stepData = step.round()[r];
//                System.out.println("\n\n>>>>>>>>>>>>>>  Round [" + (r + 1) + "]  Step [" + step.getName() + "] <<<<<<<<<<<<<<");
//                SCE sce = new SCE(exec, step, stepData);
//                sce.run();
//                exec.writeParameterCopy(step, r);
//                step.post(r, stepData);
//                Runtime.getRuntime().gc();
//            }
//        }
//        Compound.shutdown();
//        return null;
//    }
//
//    class ModelExecution implements ExecutionHandle {
//
//        File lastFolder;
//        Map<String, Object> parameter;
//
//        public ModelExecution() throws IOException {
//            OutputStragegy st = getOutput().getOutputStrategy(getName());
//            lastFolder = st.nextOutputFolder();
//            if (log.isLoggable(Level.CONFIG)) {
//                log.config("Simulation output folder: " + lastFolder);
//            }
//            lastFolder.mkdirs();
//
//            parameter = getModel().getParameter();
//            Logger.getLogger("oms3.model").setLevel(Level.WARNING);
//        }
//
//        Map<String, Object> getParameter() {
//            return parameter;
//        }
//
//        @Override
//        public void execute(Step.Data step) throws Exception {
//
//            // Path
//            String libPath = getModel().getLibpath();
//            if (libPath != null) {
//                System.setProperty("jna.library.path", libPath);
//                if (log.isLoggable(Level.CONFIG)) {
//                    log.config("Setting jna.library.path to " + libPath);
//                }
//            }
//
//            Object comp = getModel().getComponent();
//
//            writeParameterFile(step);
//            log.config("Init ...");
//            ComponentAccess.callAnnotated(comp, Initialize.class, true);
//
//            // setting the input data;
//            boolean success = ComponentAccess.setInputData(parameter, comp, log);
//            if (!success) {
//                throw new RuntimeException("There are Parameter problems. Simulation exits.");
//            }
//
//            boolean adjusted = ComponentAccess.adjustOutputPath(lastFolder, comp, log);
//
//            for (Output e : getOut()) {
//                e.setup(comp, lastFolder, getName());
//            }
//            // execute phases and be done.
//            log.config("Exec ...");
//            ComponentAccess.callAnnotated(comp, Execute.class, false);
//            log.config("Finalize ...");
//            ComponentAccess.callAnnotated(comp, Finalize.class, true);
//
//            for (Output e : getOut()) {
//                e.done();
//            }
//        }
//
//        @Override
//        public void writeParameterFile(Step.Data step) {
//            ParameterData[] paramData = step.paramData;
//            for (int i = 0; i < paramData.length; i++) {
//                String name = paramData[i].getName();
//                int calibType = paramData[i].getCalibrationType();
//               // System.out.println("****** " + name + " is ctype " + calibType);
//                double[] val = paramData[i].getDataValue();
//                if (calibType == ParameterData.BINARY) {
//                  int [] ival = new int[val.length];
//                  
//                    for (int j=0; j< val.length; j++) {
//                        ival[j] = (int) val[j];
//                    }
//                    parameter.put(name, toValueI(name,ival));
//                }
//                else {      
//                    parameter.put(name, toValue(name, val));
//                }
//            }
//        }
//        
//
//        public void writeParameterCopy(Step step, int round) throws FileNotFoundException {
//            File params = new File(lastFolder, "round-" + (round + 1) + "_step-" + step.getName() + ".csv");
//            System.out.println(" Final parameter file: '" + params + "'");
//            PrintWriter pw = new PrintWriter(params);
//            DataIO.print(parameter, "Parameter", pw);
//            pw.close();
//        }
//
//        private Object toValue(String name, double[] vals) {
//            Object orig = parameter.get(name);
//            if (orig.toString().indexOf('{') > -1) {
//                // this is an array (hopefully 1dim)
//                return Conversions.convert(vals, String.class);
//            } else {
//                return Double.toString(vals[0]);
//            }
//        }
//        
//        private Object toValueI(String name, int[] vals) {
//            Object orig = parameter.get(name);
//            if (orig.toString().indexOf('{') > -1) {
//                // this is an array (hopefully 1dim)
//                return Conversions.convert(vals, String.class);
//            } else {
//                return Integer.toString(vals[0]);
//            }
//        }
//        
//    }
//}
