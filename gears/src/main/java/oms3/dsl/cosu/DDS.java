///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package oms3.dsl.cosu;
//
//import oms3.dsl.AbstractSimulation;
//import oms3.dsl.*;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import oms3.Notification.*;
//import ngmf.util.OutputStragegy;
//import ngmf.util.cosu.luca.ParameterData;
//import oms3.ComponentAccess;
//import oms3.Conversions;
//import oms3.annotations.Execute;
//import oms3.annotations.Finalize;
//import oms3.annotations.Initialize;
//import oms3.io.CSTable;
//import oms3.io.DataIO;
//
///**
// *
// * @author od
// */
//public class DDS extends AbstractSimulation {
//
//    int samples = 2000;
//    int terms = 4;
//    Params params = new Params();
//    Date sens_start;
//    Date sens_end;
//    
//     List<ObjFunc> ofs = new ArrayList<ObjFunc>();
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        if (name.equals("parameter")) {
//            return params;
//        } else if (name.equals("objfunc")) {
//            ObjFunc of = new ObjFunc();
//            ofs.add(of);
//            return of;
//        } else if (name.equals("samples")) {
//            samples = (Integer) value;
////            if (samples<2000) {
////                throw new IllegalArgumentException("samples<2000");
////            }
//        } else if (name.equals("terms")) {
//            terms = (Integer) value;
//            if (terms != 4 && terms != 6) {
//                throw new IllegalArgumentException("terms 4 or 6 !");
//            }
//        } else if (name.equals("sens_start")) {
//            sens_start = Conversions.convert(value, Date.class);
//        } else if (name.equals("sens_end")) {
//            sens_end = Conversions.convert(value, Date.class);
//        } else {
//            return super.create(name, value);
//        }
//        return LEAF;
//    }
//
//    @Override
//    public Object run() throws Exception {
//        OutputStragegy st = getOutput().getOutputStrategy(getName());
//        File lastFolder = st.nextOutputFolder();
//        if (log.isLoggable(Level.CONFIG)) {
//            log.config("Simulation output folder: " + lastFolder);
//        }
//        lastFolder.mkdirs();
//        Logger.getLogger("oms3.model").setLevel(Level.WARNING);
//
//        ObjFunc.adjustWeights(ofs);
//
//        run(getModel(), getOut(), lastFolder, getName());
//
//        return null;
//    }
//
//    /// DDS
//    void run(Model model, List<Output> out, File folder, String name) throws Exception {
//
//        List<Param> pList = params.getParam();
//
//        int npar = params.getCount();   // number of parameters
//        int N = samples;    	        // number of samples
//        double M = terms;      		// number of terms in the partial variances summation (4 or 6)
//
//        double wi = Math.floor(N / (2 * M));
//        double m2 = Math.floor(wi / (2 * M));
//        double r = Math.floor((m2) / npar);
//        double[] w = new double[npar];
//
//        if (r < 1) {
//            for (int i = 0; i < npar; i++) {
//                w[i] = 1;
//            }
//        } else {
//            double t = Math.floor(m2 / npar);
//            w[0] = 1;
//            for (int i = 1; i < npar; i++) {
//                w[i] = 1 + i * t;
//            }
//        }
//
//        int k1 = 0;
//        double[][] w2 = new double[npar][w.length];
//        for (int i = 0; i < npar; i++) {
//            for (int j = 0; j < w.length; j++) {
//                w2[i][j] = (j == k1) ? wi : w[j];
//            }
//            k1++;
//        }
//
//        double inc = 2 * Math.PI / N;
//        double[] s = new double[N];
//        s[0] = -Math.PI;
//        for (int i = 1; i < s.length; i++) {
//            s[i] = s[i - 1] + inc;
//        }
//
//        double[][] x = new double[N][npar];
//        double[] y = new double[N];
//        double[] V = new double[npar];
//        double[] VT = new double[npar];
//        double[] Ak = new double[(int) Math.floor((N - 1) / 2)];
//        double[] Bk = new double[(int) Math.floor((N - 1) / 2)];
//        double[] S_par = new double[npar];
//        double[] Vex = new double[npar];
//        double[] Sex_par = new double[npar];
//
//        for (int h = 0; h < npar; h++) {
//            // Compute realizations
//            for (int j = 0; j < N; j++) {
//                for (int i = 0; i < npar; i++) {
//                    double p = 0.5 + Math.asin(Math.sin(w2[h][i] * s[j])) / Math.PI;
//                    Param par = pList.get(i);
//                    x[j][i] = p * (par.getUpper() - par.getLower()) + par.getLower();
//                }
//                y[j] = run_model(model, out, folder, name, x[j]);
//                System.out.println("par:" + h + " N:" + j + " of:" + y[j]);
//            }
//            // Compute total variance
//            V[h] = 0;
//            for (int k = 1; k <= ((N - 1) / 2); k++) {
//                double A = 0, B = 0;
//                for (int j = 0; j < N; j++) {
//                    A += y[j] * Math.cos(s[j] * k);
//                    B += y[j] * Math.sin(s[j] * k);
//                }
//                double ak = A * 2 / N;
//                double bk = B * 2 / N;
//                Ak[k - 1] = ak;
//                Bk[k - 1] = bk;
//                V[h] += ak * ak + bk * bk;
//            }
//            VT[h] = V[h] / 2;
//            //Compute partial variance
//            V[h] = 0;
//            for (int q = 1; q <= M; q++) {
//                int idx = (int) (q * w2[h][h]) - 1;
//                V[h] += Ak[idx] * Ak[idx] + Bk[idx] * Bk[idx];
//            }
//            V[h] /= 2;
//            S_par[h] = V[h] / VT[h];
//
//            //Compute Extended partial variance
//            Vex[h] = 0;
//            for (int q = 1; q <= M; q++) {
//                for (int c = 0; c < npar; c++) {
//                    if (c != h) {
//                        int idx = (int) (q * w2[h][c]) - 1;
//                        Vex[h] += Ak[idx] * Ak[idx] + Bk[idx] * Bk[idx];
//                    }
//                }
//            }
//            Vex[h] /= 2;
//            Sex_par[h] = 1 - Vex[h] / VT[h];
//        }
//
//        System.out.println();
//
//        StringBuilder b = new StringBuilder("Sensitivity");
//        //print out S values
//        for (int i = 0; i < pList.size(); i++) {
//            b.append(String.format(Locale.US, "%15s ", pList.get(i).getName()));
//        }
//        b.append("\n        S          ");
//
//        for (int i = 0; i < S_par.length; i++) {
//            b.append(String.format(Locale.US, "%-8.7f   ", S_par[i]));
//        }
//        b.append("\n       ST          ");
//        for (int i = 0; i < Sex_par.length; i++) {
//            b.append(String.format(Locale.US, "%-8.7f   ", Sex_par[i]));
//        }
//        b.append('\n');
//        System.out.println(b.toString());
//    }
//
//    private double run_model(Model model, List<Output> out, File folder, String simName, double[] x) throws Exception {
//
//        Map<String, Object> parameter = model.getParameter();
//        Object comp = model.getComponent();
//
//        // spatial params
//        ParameterData[] pd = Step.create(params, parameter);
//        for (int i = 0; i < pd.length; i++) {
//            pd[i].generateValues(x[i]);
//        }
//
//        for (int i = 0; i < pd.length; i++) {
//            String name = pd[i].getName();
//            double[] val = pd[i].getDataValue();
//            parameter.put(name, toValue(name, val, parameter));
//        }
//
//        ComponentAccess.callAnnotated(comp, Initialize.class, true);
//
//        // setting the input data;
//        boolean success = ComponentAccess.setInputData(parameter, comp, log);
//        if (!success) {
//            throw new RuntimeException("There are Parameter problems. Simulation exits.");
//        }
//
//        ComponentAccess.adjustOutputPath(folder, comp, log);
//        for (Output e : out) {
//            e.setup(comp, folder, simName);
//        }
//        // execute phases and be done.
//        log.config("Exec ...");
//        ComponentAccess.callAnnotated(comp, Execute.class, false);
//        log.config("Finalize ...");
//        ComponentAccess.callAnnotated(comp, Finalize.class, true);
//
//        for (Output e : out) {
//            e.done();
//        }
//
//        return ObjFunc.calculateObjectiveFunctionValue(ofs, sens_start, sens_end, folder);
//    }
//
//    private Object toValue(String name, double[] vals, Map<String, Object> parameter) {
//        Object orig = parameter.get(name);
//        if (orig.toString().indexOf('{') > -1) {
//            // this is an array (hopefully 1dim)
//            return Conversions.convert(vals, String.class);
//        } else {
//            return Double.toString(vals[0]);
//        }
//    }
//}
