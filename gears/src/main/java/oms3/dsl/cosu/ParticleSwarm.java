/*
 * $Id: ParticleSwarm.java 2fb4d513ea17 2014-06-04 odavid@colostate.edu $
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

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.ngmf.util.UnifiedParams;
import oms3.ComponentAccess;
import oms3.ComponentException;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.Initialize;
import oms3.dsl.*;

public class ParticleSwarm extends AbstractSimulation {

    // PS parameter
    int kmax = 1000;
    int numPart = 10;
    // internal
    double[][] p_best;
    double[] hist_g_best_value;
    double[] p_best_value;
    //
    public double[] g_best;
    public double g_best_value;
    // for now.
    double[] pParDowRange;
    double[] pParUpRange;
    //
    Params params;
    ObjFunc of;
    //
    String obs_data;
    String sim_data;
    //
    ModelExecution exec;
    //
    int check_after = 300;
    int check_last = 50;
    int check_min = 30;
    double check_delta = 0.001;
    int verbose = 0;

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("kmax")) {
            kmax = (Integer) value;
            if (kmax < 300) {
                throw new ComponentException("Illegal 'kmax': " + kmax);
            }
        } else if (name.equals("check_after")) {
            check_after = (Integer) value;
        } else if (name.equals("check_last")) {
            check_last = (Integer) value;
        } else if (name.equals("check_min")) {
            check_min = (Integer) value;
        } else if (name.equals("check_delta")) {
            check_delta = ((BigDecimal) value).doubleValue();
        } else if (name.equals("verbose")) {
            verbose = (Integer) value;
        } else if (name.equals("numPart")) {
            numPart = (Integer) value;
            if (numPart < 2) {
                throw new ComponentException("Illegal 'numPart': " + numPart);
            }
            // calibration parameter
        } else if (name.equals(Params.DSL_NAME)) {
            params = new Params();
            return params;
        } else if (name.equals(ObjFunc.DSL_NAME)) {
            of = new ObjFunc();
            return of;
        } else {
            return super.create(name, value);
        }
        return LEAF;
    }

    @Override
    public Object run() throws Exception {
        if (getModelElement() == null) {
            throw new ComponentException("missing 'model'.");
        }
        if (of == null) {
            throw new ComponentException("not found: " + ObjFunc.DSL_NAME);
        }
        if (params == null) {
            throw new ComponentException("not found: " + Params.DSL_NAME);
        }

        obs_data = of.getObserved().getData();
        sim_data = of.getSimulated().getData();

        if (obs_data == null || sim_data == null) {
            throw new ComponentException("no objfunc data.");
        }

        getModelElement().setOut2Out(new String[]{obs_data, sim_data});
        exec = new ModelExecution();

        if (verbose == 1) {
            System.out.println("Inputs :");
            for (Param p : params.getParam()) {
                System.out.println(p.getName() + " " + p.getLower() + " ... " + p.getUpper());
            }
            System.out.println(obs_data + " " + sim_data + " " + kmax + " " + numPart);
        }

        int noParams = params.getParam().size();

        pParDowRange = new double[noParams];
        pParUpRange = new double[noParams];
        
//        for (int i = 0; i < noParams; i++) {
//            if (!(params.getParam().get(i).getValue() instanceof Number)) {
//                System.out.println("Value " + params.getParam().get(i).getValue());
//                throw new IllegalArgumentException(params.getParam().get(i).getName() + " is not a scalar.");
//            }
//        }
        
        for (int i = 0; i < noParams; i++) {
            pParDowRange[i] = params.getParam().get(i).getLower();
            pParUpRange[i] = params.getParam().get(i).getUpper();
        }

        // parameters matrix [numPar][numPart]
        double matPar[][] = uniform(pParDowRange, pParUpRange, numPart);
        // check param bounds
        double matParBounded[][] = reflectBounds(matPar, pParUpRange, pParDowRange);

        // particles velocities
        double velRange_min[] = new double[pParDowRange.length];
        double velRange_max[] = new double[pParDowRange.length];

        // the initial velocity is between 0.1 and 10 min and max parameter
        // values
        for (int i = 0; i < pParUpRange.length; i++) {
            velRange_min[i] = pParDowRange[i] * 0.1;
            velRange_max[i] = pParUpRange[i] * 10;
        }

        double[][] velBounded = uniform(velRange_min, velRange_max, numPart);

        // calculate the cost of each particle
        double[] costvectold = computeCostFunction(matParBounded);

        // initialize p_best the best position visited by the particles
        p_best = matParBounded;
        // initialize p_best_value the best cost visited by the particles
        p_best_value = costvectold;

        // initialize the min value and the position of the min in the
        // parameters matirix as the min cost
        double min = Math.abs(costvectold[0]);
        int posmin = 0;
        for (int i = 1; i < costvectold.length; i++) {
            if (Math.abs(costvectold[i]) < min) {
                min = Math.abs(costvectold[i]);
                posmin = i;
            }
        }

        // initialize the global minimum g_best_value and the best parameter set
        g_best = new double[matParBounded[0].length];
        g_best_value = min;
        for (int i = 0; i < matParBounded[0].length; i++) {
            g_best[i] = matParBounded[posmin][i];
        }

        // keep in mind the history of the global minimum
        hist_g_best_value = new double[kmax];

        for (int k = 0; k < kmax - 1; k++) {
            double[][] matParBounded_old = matParBounded;
            // compute the new velocity
            double[][] velnew = compute_velocity(matParBounded_old, velBounded);

            velBounded = velnew;
            // compute the new parameters
            matParBounded = compute_particle(matParBounded_old, velnew);

            // compute the new cost vect
            double[] costvect = computeCostFunction(matParBounded);

            // compute the particle parameter best set
            p_best = compute_pBest(matParBounded, costvect);

            // compute the global parameter best set
            g_best = compute_gBest(matParBounded, costvect);

            // update the history
            hist_g_best_value[k] = g_best_value;

            //if there is no more improvement stop
            if (k > check_after) {
                int sum = 0;
                for (int c = 0; c < check_last; c++) {
                    if (Math.abs(hist_g_best_value[k - c]
                            - hist_g_best_value[k - c - 1]) < check_delta) {
                        sum++;
                    }
                }
                if (sum > check_min) {
                    break;
                }
            }
            if (verbose == 1) {
                System.out.println("k:" + k + " of:" + g_best_value + " g_best:" + Arrays.toString(g_best));
            }
        }

        if (verbose == 1) {
            System.out.println("\nParticle Results:");
            for (int i = 0; i < noParams; i++) {
                System.out.println(" " + params.getParam().get(i).getName() + ": " + g_best[i]);
            }
        }
        return exec.getLastComponent();
    }

    public double[] computeCostFunction(double xx[][]) throws Exception {
        double[] res = new double[xx.length];
        for (int part = 0; part < xx.length; part++) {
            Map<String, Object> pa = exec.getParameter();

            // new parameters
            List<Param> pl = params.getParam();
            for (int i = 0; i < pl.size(); i++) {
                String name = pl.get(i).getName();
                pa.put(name.replace('.', '_'), xx[part][i]);
                if (verbose == 2) {
                    System.out.println(" > param :" + name.replace('.', '_') + " " + xx[part][i]);
                }
            }

            // model execution
            Object comp = exec.execute();

            // of 
            res[part] = calcOF(comp, obs_data, sim_data);
            if (verbose == 2) {
                System.out.println(" > res " + part + " :" + res[part]);
            }
        }
        return res;
    }

    private double calcOF(Object comp, String obs_data, String sim_data) throws Exception {
        Class c = comp.getClass();
        double[] obsval = (double[]) c.getField(obs_data.replace('.', '_')).get(comp);
        double[] simval = (double[]) c.getField(sim_data.replace('.', '_')).get(comp);
        if (verbose == 2) {
            System.out.println("> OBS: " + Arrays.toString(obsval));
            System.out.println("> SIM: " + Arrays.toString(simval));
        }

        double of_val = of.getOF().calculate(obsval, simval, of.invalidData);
        if (Double.isNaN(of_val) || Double.isInfinite(of_val)) {
            return 10000;
        }
        return of.getOF().positiveDirection() ? (1 - of_val) : of_val;
    }

    // helper methods
    static double[][] uniform(double[] xmin, double[] xmax, int nsample) {
        int nvar = xmin.length;
        double[][] s = new double[nsample][nvar];
        double[][] ran = new double[nsample][nvar];
        for (int row = 0; row < ran.length; row++) {
            for (int col = 0; col < ran[0].length; col++) {
                s[row][col] = (xmax[col] - xmin[col]) * Math.random() + xmin[col];
            }
        }
        return s;
    }

    double[] compute_gBest(double xx[][], double[] vettcostnew) {
        double re[] = g_best;
        double min = g_best_value;
        for (int i = 0; i < vettcostnew.length; i++) {
            if (Math.abs(vettcostnew[i]) <= min) {
                g_best_value = Math.abs(vettcostnew[i]);
                min = Math.abs(vettcostnew[i]);
                for(int ii = 0; ii < xx[0].length; ii++) {
                    re[ii] = xx[i][ii];
                }
            }
        }
        return re;
    }

    double[][] compute_particle(double pos[][], double[][] vel) {
        double xnew[][] = new double[pos.length][pos[0].length];
        for (int i = 0; i < vel.length; i++) {
            for (int j = 0; j < vel[0].length; j++) {
                xnew[i][j] = pos[i][j] + vel[i][j];
            }
        }
        return reflectBounds(xnew, pParUpRange, pParDowRange);
    }

    double[][] compute_velocity(double pos[][], double[][] vel) {
        double velnew[][] = new double[pos.length][pos[0].length];
        for (int i = 0; i < vel.length; i++) {
            for (int j = 0; j < vel[0].length; j++) {
                double c1 = 1.5;
                double r1 = Math.random();
                double c2 = 2.5;
                double r2 = Math.random();
                double inertia = 0.5;

                velnew[i][j] = inertia * vel[i][j] + c1 * r1 * (p_best[i][j] - pos[i][j]) + c2 * r2
                        * (g_best[j] - pos[i][j]);
            }
        }
        return velnew;
    }

    double[][] compute_pBest(double currentpos[][], double[] currentbest) {
        double pos_best[][] = p_best;
        for (int i = 0; i < currentbest.length; i++) {
            if (Math.abs(currentbest[i]) < Math.abs(p_best_value[i])) {
                p_best_value[i] = Math.abs(currentbest[i]);
                for (int j = 0; j < currentpos[0].length; j++) {
                    pos_best[i][j] = currentpos[i][j];
                }
            }
        }
        return pos_best;
    }

    double[][] reflectBounds(double[][] neww, double[] ParRange_maxnn, double[] ParRange_minnn) {
        double[][] y = neww;
        for (int row = 0; row < neww.length; row++) {
            for (int col = 0; col < neww[0].length; col++) {
                if (y[row][col] < ParRange_minnn[col]) {
                    y[row][col] = 2 * ParRange_minnn[col] - y[row][col];
                }
                if (y[row][col] > ParRange_maxnn[col]) {
                    y[row][col] = 2 * ParRange_maxnn[col] - y[row][col];
                }
            }
        }
        for (int row = 0; row < neww.length; row++) {
            for (int col = 0; col < neww[0].length; col++) {
                if (y[row][col] < ParRange_minnn[col]) {
                    y[row][col] = ParRange_minnn[col] + Math.random()
                            * (ParRange_maxnn[col] - ParRange_minnn[col]);
                }
                if (y[row][col] > ParRange_maxnn[col]) {
                    y[row][col] = ParRange_minnn[col] + Math.random()
                            * (ParRange_maxnn[col] - ParRange_minnn[col]);
                }
            }
        }
        return y;
    }

    /**
     * ModelExecution
     */
    class ModelExecution {

        File lastFolder;
        Map<String, Object> parameter;
        UnifiedParams up;
        Object comp;

        ModelExecution() throws Exception {
            lastFolder = getOutputPath();
            lastFolder.mkdirs();
            up = getModelElement().getParameter();
            parameter = up.getParams();
            Logger.getLogger("oms3.model").setLevel(Level.WARNING);
            String libPath = getModelElement().getLibpath();
            if (libPath != null) {
                System.setProperty("jna.library.path", libPath);
                if (log.isLoggable(Level.CONFIG)) {
                    log.config("Setting jna.library.path to " + libPath);
                }
            }
        }

        Object getLastComponent() {
            return comp;
        }

        Map<String, Object> getParameter() {
            return parameter;
        }

        Object execute() throws Exception {
            comp = getModelElement().newModelComponent();
            log.config("Init ...");
            ComponentAccess.callAnnotated(comp, Initialize.class, true);

            // setting the input data;
//            boolean success = ComponentAccess.setInputData(parameter, comp, log);
            boolean success = up.setInputData(comp, log);
            if (!success) {
                throw new RuntimeException("There are Parameter problems. Simulation exits.");
            }

            boolean adjusted = ComponentAccess.adjustOutputPath(lastFolder, comp, log);
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
            return comp;
        }
    }
}
