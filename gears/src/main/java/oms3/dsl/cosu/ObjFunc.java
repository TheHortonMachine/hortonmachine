/*
 * $Id: ObjFunc.java dba0d06e37a0 2015-04-14 odavid@colostate.edu $
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
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import oms3.ObjectiveFunction;
import oms3.SimBuilder;
import oms3.dsl.Buildable;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.ngmf.util.cosu.luca.of.*;

/**
 * Objective function handling.
 *
 * @author od
 */
public class ObjFunc implements Buildable {

    public static final String DSL_NAME = "objfunc";
    static final List<String> months = Arrays.asList("Jan", "Feb",
            "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
    //
    double weight = Double.NaN;
    String timestep = SimBuilder.DAILY;
    ObjectiveFunction of;
    //
    CSVColumn sim;
    CSVColumn obs;
    CSVColumn sd = null;
    //
    String method;
    // observed values (cached for efficiency)
    private double[] cached_obs = null;
    boolean[] periodMask = null;
    private List<Integer> subDivideValue = new ArrayList<Integer>();
    boolean[] subDivideMask = null;
    double invalidData = -90.0;

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
        } else if (name.equals("subdivide")) {
            return sd = new CSVColumn();
        }
        throw new IllegalArgumentException(name.toString());
    }

    public void setMethod(String method) {
        this.method = method;
        if (method.equals(SimBuilder.ABSDIF)) {
            of = new ABSDIF();
        } else if (method.equals(SimBuilder.ABSDIFLOG)) {
            of = new ABSDIFLOG();
        } else if (method.equals(SimBuilder.AVE)) {
            of = new AVE();
        } else if (method.equals(SimBuilder.IOA)) {
            of = new IOA();
        } else if (method.equals(SimBuilder.IOA2)) {
            of = new IOA2();
        } else if (method.equals(SimBuilder.NS)) {
            of = new NS();
        } else if (method.equals(SimBuilder.NSLOG)) {
            of = new NSLOG();
        } else if (method.equals(SimBuilder.NS2LOG)) {
            of = new NS2LOG();
        } else if (method.equals(SimBuilder.NBIAS)) {
            of = new BIAS();
        } else if (method.equals(SimBuilder.PMCC)) {
            of = new PMCC();
        } else if (method.equals(SimBuilder.RMSE)) {
            of = new RMSE();
        } else if (method.equals(SimBuilder.MSE)) {
            of = new MSE();
        } else if (method.equals(SimBuilder.FLF)) {
            of = new FLF();
        } else if (method.equals(SimBuilder.FHF)) {
            of = new FHF();
        } else if (method.equals(SimBuilder.KGE)) {
            of = new KGE();
        } else if (method.equals(SimBuilder.TRMSE)) {
            of = new TRMSE();
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

    public String getMethod() {
        return method;
    }

    public void setTimestep(String timestep) {
        if ((!timestep.equals(SimBuilder.DAILY))
                && (!timestep.equals(SimBuilder.TIME_STEP))
                && (!timestep.equals(SimBuilder.RAW))
                && (!timestep.equals(SimBuilder.DAILY_MEAN))
                && (!timestep.equals(SimBuilder.MONTHLY_MEAN))
                && (!timestep.equals(SimBuilder.MEAN_MONTHLY))
                && (!timestep.equals(SimBuilder.ANNUAL_MEAN))
                && (!timestep.equals(SimBuilder.PERIOD_MAXIMUM))
                && (!timestep.equals(SimBuilder.PERIOD_MININUM))
                && (!timestep.equals(SimBuilder.PERIOD_MEDIAN))
                && (!timestep.equals(SimBuilder.PERIOD_STANDARD_DEVIATION))) {
            throw new IllegalArgumentException("SetTimeStep:  Illegal timestep: " + timestep);
        }
        this.timestep = timestep;
    }

    public void setInvalidDataValue(String invalidDataValue) {
        invalidData = Double.parseDouble(invalidDataValue);
    }

    public double getInvalidData() {
        return invalidData;
    }

    public void setInvalidData(double invalidData) {
        this.invalidData = invalidData;
    }

    public void setSubdivide_value(String subdivide_value) {
        int maxSD = Integer.MAX_VALUE; // arbitrary max value in case the uses puts in "n-*"
        List<Integer> range = RangeParser.parse(subdivide_value, maxSD);
        for (Integer r : range) {
            subDivideValue.add(r);
        }
    }

    public List<Integer> getSubDivideValue() {
        return subDivideValue;
    }

    public void setSubDivideMask(boolean[] in) {
        this.subDivideMask = in;
    }

    public String getSubDivideString() {
        if (sd == null) {
            return null;
        } else {
            return "File: " + sd.getFile() + ",  Table: " + sd.getTable() + ", Column: " + sd.getColumn();
        }
    }

    public boolean[] getSubDivideMask(CSVColumn sdi, Date start, Date end, File folder) {
        if (sdi != null) {
            try {
                File fname = ObjFunc.resolve(sdi.getFile(), folder);
                CSTable sdt = DataIO.table(fname, sdi.getTable());
                String column = sdi.getColumn();

                double[] retData = DataIO.getColumnDoubleValuesInterval(start, end, sdt, column, DataIO.DAILY, 0, null, null);
                int[] subDivideData = new int[retData.length];
                for (int i = 0; i < retData.length; i++) {
                    subDivideData[i] = (int) retData[i];
                }

                if ((subDivideMask == null) && (subDivideData != null)) {
                    this.subDivideMask = new boolean[subDivideData.length];
                    for (int i = 0; i < subDivideData.length; i++) {
                        this.subDivideMask[i] = this.getSubDivideValue().isEmpty(); // if no subdivide value, don't use subdivide (mask=1)
                        for (Integer sd : this.getSubDivideValue()) {
                            this.subDivideMask[i] = this.subDivideMask[i] | (subDivideData[i] == sd);
                        }
                    }
                }
                return this.subDivideMask;
            } catch (IOException E) {
                throw new RuntimeException(E);
            }
        } else {
            return null;
        }
    }

    public void setPeriod_range(String period_range) {
        System.out.println("*****Setting Period Range to " + period_range);
        RangeParser pr = new RangeParser();
        boolean[] range_tmp = pr.getArray(period_range, 13);
        this.periodMask = new boolean[12];
        for (int i = 0; i < 12; i++) {
            this.periodMask[i] = range_tmp[i + 1];  // convert from 1-12 to 0-13.
        }
    }

    public boolean[] getPeriodMask() {
        return this.periodMask;
    }

    public String getPeriodStartName(int start) {
        return months.get(start);
    }

    public String getPeriodEndName(int start) {
        int periodEnd = (start + 11) % 12;
        return months.get(periodEnd);
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

    public ObjectiveFunction getOF() {
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

    public static double[] getObs(ObjFunc of, Date start, Date end, CSTable tobs, String columnName, int timeStep, int startMonth,
            boolean[] periodMask, boolean[] sdMask) {
        synchronized (of) {
            if (of.cached_obs == null) {
                of.cached_obs = DataIO.getColumnDoubleValuesInterval(start, end, tobs, columnName, timeStep, startMonth, periodMask, sdMask);
            }
            return of.cached_obs;
        }
    }

    public static double calculateObjectiveFunctionValue(List<ObjFunc> ofs, Date start, Date end, File folder) {
        return calculateObjectiveFunctionValue(ofs, 0, start, end, folder);
    }

    public static double calculateObjectiveFunctionValue(List<ObjFunc> ofs, int startMonthOfYear, Date start, Date end, File folder) {
        try {
            if (ofs.isEmpty()) {
                throw new IllegalArgumentException("No Objective function(s) defined. ");
            }
            double val = 0.0;
            double weight = 0.0;
            adjustWeights(ofs);

            for (ObjFunc of : ofs) {
                String timeStepString = of.getTimestep();
                int timeStep = DataIO.DAILY;

                if (timeStepString.equals(SimBuilder.DAILY)) {
                    timeStep = DataIO.DAILY;
                } else if (timeStepString.equals(SimBuilder.RAW)) {
                    timeStep = DataIO.RAW;
                } else if (timeStepString.equals(SimBuilder.TIME_STEP)) {
                    timeStep = DataIO.TIME_STEP;                         
                } else if (timeStepString.equals(SimBuilder.MEAN_MONTHLY)) {
                    timeStep = DataIO.MEAN_MONTHLY;
                } else if (timeStepString.equals(SimBuilder.MONTHLY_MEAN)) {
                    timeStep = DataIO.MONTHLY_MEAN;
                } else if (timeStepString.equals(SimBuilder.ANNUAL_MEAN)) {
                    timeStep = DataIO.ANNUAL_MEAN;
                } else if (timeStepString.equals(SimBuilder.PERIOD_MEAN)) {
                    timeStep = DataIO.PERIOD_MEAN;
                } else if (timeStepString.equals(SimBuilder.PERIOD_MEDIAN)) {
                    timeStep = DataIO.PERIOD_MEDIAN;
                } else if (timeStepString.equals(SimBuilder.PERIOD_MININUM)) {
                    timeStep = DataIO.PERIOD_MIN;
                } else if (timeStepString.equals(SimBuilder.PERIOD_MAXIMUM)) {
                    timeStep = DataIO.PERIOD_MAX;
                } else if (timeStepString.equals(SimBuilder.PERIOD_STANDARD_DEVIATION)) {
                    timeStep = DataIO.PERIOD_STANDARD_DEVIATION;
                } else {
                    throw new IllegalArgumentException("TimeStep " + timeStepString + "unknown.");
                }

                boolean[] pMask = of.getPeriodMask();
                boolean[] sdMask = of.getSubDivideMask(of.sd, start, end, folder);

                CSVColumn obs = of.getObserved();
                CSTable tobs = DataIO.table(resolve(obs.getFile(), folder), obs.getTable());
                double[] obsval = getObs(of, start, end, tobs, obs.getColumn(), timeStep, startMonthOfYear, pMask, sdMask);

                CSVColumn sim = of.getSimulated();
                CSTable tsim = DataIO.table(resolve(sim.getFile(), folder), sim.getTable());
                double[] simval = DataIO.getColumnDoubleValuesInterval(start, end, tsim, sim.getColumn(), timeStep, startMonthOfYear, pMask, sdMask);

                weight += of.getWeight();
                Double ofa  = of.getOF().calculate(obsval, simval, of.invalidData);
                if (Double.isNaN(ofa)) {                   
                    System.out.println("Warning : objective function computed as NaN. Using unfavorable ObjFunc value in it's place.");
                    ofa = of.of.positiveDirection() ? Double.MIN_VALUE : Double.MAX_VALUE;
                }
                val += ofa * of.getWeight();
            }
            if (Math.abs(weight - 1.0) > 0.001) {
                throw new IllegalArgumentException("sum of of weights != 1.0");
            }
            return val;
        } catch (IOException E) {
            throw new RuntimeException(E);
        }
    }

    public static File resolve(String file, File out) {
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
