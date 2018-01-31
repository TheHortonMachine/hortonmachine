/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.cosu;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import oms3.ObjectiveFunction;
import oms3.SimConst;
import oms3.dsl.Buildable;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.ngmf.util.cosu.luca.of.AbsoluteDifference;
import oms3.ngmf.util.cosu.luca.of.AbsoluteDifferenceLog;
import oms3.ngmf.util.cosu.luca.of.NashSutcliffe;
import oms3.ngmf.util.cosu.luca.of.NormalizedRMSE;
import oms3.ngmf.util.cosu.luca.of.PearsonsCorrelation;

/** Objective function handling. 
 *
 * @author od
 */
public class ObjFunc implements Buildable {

    double weight = Double.NaN;
    String timestep = SimConst.DAILY;
    ObjectiveFunction of;
    //
    CSVColumn sim;
    CSVColumn obs;

    public CSVColumn getSimulated() {
        if (sim == null) {
            throw new IllegalArgumentException("Missing 'sim' argument");
        }
        return sim;
    }

    public CSVColumn getObserved() {
        if (obs == null) {
            throw new IllegalArgumentException("Missing 'obs' argument");
        }
        return obs;
    }

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("sim")) {
            return sim = new CSVColumn();
        } else if (name.equals("obs")) {
            return obs = new CSVColumn();
        }
        throw new IllegalArgumentException(name.toString());
    }

    public void setMethod(String method) {
        if (method.equals(SimConst.NS)) {
            of = new NashSutcliffe();
        } else if (method.equals(SimConst.RMSE)) {
            of = new NormalizedRMSE();
        } else if (method.equals(SimConst.ABSDIF)) {
            of = new AbsoluteDifference();
        } else if (method.equals(SimConst.LOGABSDIF)) {
            of = new AbsoluteDifferenceLog();
        } else if (method.equals(SimConst.PMCC)) {
            of = new PearsonsCorrelation();
        } else {
            try {
                // load this as a class name from default classpath
                Class<?> c = Class.forName(method);
                of = (ObjectiveFunction) c.newInstance();
            } catch (Exception E) {
                throw new IllegalArgumentException("No such method: " + method);
            }
        }
    }

    public void setTimestep(String timestep) {
        if ((!timestep.equals(SimConst.DAILY)) && 
            (!timestep.equals(SimConst.DAILY_MEAN)) && 
            (!timestep.equals(SimConst.MONTHLY_MEAN)) &&
            (!timestep.equals(SimConst.MEAN_MONTHLY)) &&
            (!timestep.equals(SimConst.PERIOD_MAXIMUM)) &&
            (!timestep.equals(SimConst.PERIOD_MAXIMUM)) &&
            (!timestep.equals(SimConst.PERIOD_MININUM)) &&
            (!timestep.equals(SimConst.PERIOD_MEDIAN)) &&
            (!timestep.equals(SimConst.PERIOD_STANDARD_DEVIATION))
                )
           {
            throw new IllegalArgumentException("SetTimeStep:  Illegal timestep: " + timestep);
        }
        this.timestep = timestep;
    }

    public void setWeight(double weight) {
        if (weight <= 0 || weight > 1) {
            throw new IllegalArgumentException("of weight out of range: " + weight);
        }
        this.weight = weight;
    }

    String getTimestep() {
        return timestep;
    }

    double getWeight() {
        return weight;
    }

    ObjectiveFunction getOF() {
        if (of == null) {
            throw new IllegalArgumentException("No Objective function method defined.");
        }
        return of;
    }

    // static 
    public static boolean isInc(List<ObjFunc> ofs) {
        if (ofs.isEmpty()) {
            throw new IllegalArgumentException("No Objective function(s) defined. ");
        }
        boolean inc = ofs.get(0).getOF().positiveDirection();
        for (ObjFunc of : ofs) {
            if (of.getOF().positiveDirection() != inc) {
                throw new IllegalArgumentException("Objective function(s) optimization direction mismatch!");
            }
        }
        return inc;
    }

    public static void adjustWeights(List<ObjFunc> ofs) {
        int noOf = ofs.size();
        for (ObjFunc of : ofs) {
            if (Double.isNaN(of.getWeight())) {
                of.setWeight((double) 1 / noOf);
            }
        }
    }

    public static double calculateObjectiveFunctionValue(List<ObjFunc> ofs, Date start, Date end, File folder) {
        try {
            if (ofs.isEmpty()) {
                throw new IllegalArgumentException("No Objective function(s) defined. ");
            }
            double val = 0.0;
            double weight = 0.0;
            adjustWeights(ofs);

            for (ObjFunc of : ofs) {
                CSVColumn obs = of.getObserved();
                String timeStepString = of.getTimestep();
                int timeStep = DataIO.DAILY;
                if(timeStepString.equals(SimConst.DAILY)) timeStep = DataIO.DAILY;
                else if(timeStepString.equals(SimConst.MEAN_MONTHLY)) timeStep = DataIO.MEAN_MONTHLY;
                else if(timeStepString.equals(SimConst.MONTHLY_MEAN)) timeStep = DataIO.MONTHLY_MEAN;
                else if(timeStepString.equals(SimConst.ANNUAL_MEAN)) timeStep = DataIO.ANNUAL_MEAN;
                else if(timeStepString.equals(SimConst.PERIOD_MEAN)) timeStep = DataIO.PERIOD_MEAN;              
                else if(timeStepString.equals(SimConst.PERIOD_MEDIAN)) timeStep = DataIO.PERIOD_MEDIAN;
                else if(timeStepString.equals(SimConst.PERIOD_MININUM)) timeStep = DataIO.PERIOD_MIN;
                else if(timeStepString.equals(SimConst.PERIOD_MAXIMUM)) timeStep = DataIO.PERIOD_MAX;
                else if(timeStepString.equals(SimConst.PERIOD_STANDARD_DEVIATION)) timeStep = DataIO.PERIOD_STANDARD_DEVIATION;
                else throw new IllegalArgumentException("TimeStep " + timeStepString + "unknown.");
                
                CSTable tobs = DataIO.table(resolve(obs.getFile(), folder), obs.getTable());
                double[] obsval = DataIO.getColumnDoubleValuesInterval(start, end, tobs, obs.getColumn(), timeStep);
                
                CSVColumn sim = of.getSimulated();
                CSTable tsim = DataIO.table(resolve(sim.getFile(), folder), sim.getTable());
                double[] simval = DataIO.getColumnDoubleValuesInterval(start, end, tsim, sim.getColumn(), timeStep);

                weight += of.getWeight();
                val += of.getOF().calculate(obsval, simval, -90.0) * of.getWeight();
            }
            if (weight != 1.0) {
                throw new IllegalArgumentException("sum of of weights != 1.0");
            }
            return val;
        } catch (IOException E) {
            throw new RuntimeException(E);
        }
    }

    private static File resolve(String file, File out) {
        File f = new File(file);
        if (!(f.isAbsolute() && f.exists())) {
            f = new File(out, file);
        }
        if (!f.exists()) {
            throw new IllegalArgumentException("File not found: " + file);
        }
        return f;
    }
}