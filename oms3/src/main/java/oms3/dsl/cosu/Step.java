/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.dsl.cosu;

import oms3.dsl.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import ngmf.util.cosu.luca.ExecutionHandle;
import ngmf.util.cosu.luca.ParameterData;
import oms3.Conversions;
import oms3.dsl.cosu.Luca.ModelExecution;
import oms3.io.CSTable;
import oms3.io.DataIO;

/**
 *
 * @author od
 */
public class Step implements Buildable {

    String name;
    Params params = new Params();
    Opt opt = new Opt();
    // sce control parameter
    int maxExec = 10000;
    int initComplexes = 2;
    int minComplexes = 1;
    // computed sce parameter
    int pointsPerComplex = -1;
    int pointsPerSubcomplex = -1;
    int evolutions = -1;
    //
    int shufflingLoops = 5;
    double ofPercentage = 0.01;
    //
    int number;

    Step(int number) {
        this.number = number;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name == null ? Integer.toString(number) : name;
    }

    public int getInitComplexes() {
        return initComplexes;
    }

    public int getMaxExec() {
        return maxExec;
    }

    public int getMinComplexes() {
        return minComplexes;
    }

    public int getEvolutions() {
        return evolutions > -1 ? evolutions : (params.getCount() * 2 + 1);
    }

    public int getPointsPerComplex() {
        return pointsPerComplex > -1 ? pointsPerComplex : (params.getCount() * 2 + 1);
    }

    public int getPointsPerSubcomplex() {
        return pointsPerSubcomplex > -1 ? pointsPerComplex : (params.getCount() + 1);
    }

    public double getOfPercentage() {
        return ofPercentage;
    }

    public int getShufflingLoops() {
        return shufflingLoops;
    }

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("max_exec")) {
            maxExec = (Integer) value;
            if (maxExec < 1) {
                throw new IllegalArgumentException("max_exec: " + maxExec);
            }
        } else if (name.equals("init_complexes")) {
            initComplexes = (Integer) value;
            if (initComplexes < 1) {
                throw new IllegalArgumentException("init_complexes: " + initComplexes);
            }
        } else if (name.equals("points_per_complex")) {
            pointsPerComplex = (Integer) value;
        } else if (name.equals("points_per_subcomplex")) {
            pointsPerSubcomplex = (Integer) value;
        } else if (name.equals("evolutions")) {
            evolutions = (Integer) value;
        } else if (name.equals("min_complexes")) {
            minComplexes = (Integer) value;
            if (minComplexes < 1) {
                throw new IllegalArgumentException("minComplexes: " + minComplexes);
            }
        } else if (name.equals("shuffling_loops")) {
            shufflingLoops = (Integer) value;
            if (shufflingLoops < 1) {
                throw new IllegalArgumentException("shufflingLoops: " + shufflingLoops);
            }
        } else if (name.equals("of_percentage")) {
            ofPercentage = Conversions.convert(value, Double.class);
            if (ofPercentage <= 0.0 || ofPercentage > 1.0) {
                throw new IllegalArgumentException("of_percentage: " + ofPercentage);
            }
        } else if (name.equals("parameter")) {
            return params;
        } else if (name.equals("optimization")) {
            return opt;
        } else {
            throw new IllegalArgumentException(name.toString());
        }
        return LEAF;
    }

    public Params params() {
        return params;
    }

    public boolean needsToStopSCE() {
       return false;
    }

    public void setStatus(String string) {
        
    }

    public static class Data {

        int round;
        double bestOFPoint;
        double[] upperBound;
        double[] lowerBound;
        double[] paramValues;
        //
        ParameterData[] paramData;
        ParameterData[] bestParamData;

        public void createBestParamData() {
            bestParamData = new ParameterData[paramData.length];
            for (int i = 0; i < bestParamData.length; i++) {
                bestParamData[i] = new ParameterData(paramData[i]);
            }
        }

        void init(ParameterData[] params) {

            ParameterData[] pd = new ParameterData[params.length];
            for (int i = 0; i < params.length; i++) {
                pd[i] = new ParameterData(params[i]);
            }
            setParameterData(pd);

            int numOfParams = 0;
            for (int i = 0; i < paramData.length; i++) {
                numOfParams += paramData[i].getCalibrationDataSize();
            }

            // transfer values from paramData array to paramValue
            paramValues = new double[numOfParams];
            lowerBound = new double[numOfParams];
            upperBound = new double[numOfParams];

            int index = 0;
            for (int i = 0; i < paramData.length; i++) {
                double[] values = paramData[i].getCalibrationData();
                for (int j = 0; j < values.length; j++) {
                    paramValues[index] = values[j];
                    lowerBound[index] = paramData[i].getLowerBound();
                    upperBound[index] = paramData[i].getUpperBound();
                    index++;
                }
            }
            bestOFPoint = 0.0;
        }

        public void setObjFuncValueOfBestPoint(double d) {
            bestOFPoint = d;
        }

        public double getObjFuncValueOfBestPoint() {
            return bestOFPoint;
        }

        public void setParameterData(ParameterData[] paramData) {
            this.paramData = paramData;
        }

        public void setParamValues(double[] paramValues) {
            this.paramValues = paramValues;
            int index = 0;
            for (int i = 0; i < paramData.length; i++) {
                double[] data = new double[paramData[i].getCalibrationDataSize()];
                for (int j = 0; j < data.length; j++) {
                    data[j] = paramValues[index];
                    index++;
                }
                paramData[i].generateValues(data);
            }
        }

        public void setBestParamData(double[] bestParamValues, double bestOFPoint) {
            this.bestOFPoint = bestOFPoint;
            setBestParamData(bestParamValues);
        }

        public void setBestParamData(double[] bestParamValues) {
            int index = 0;
            for (int i = 0; i < bestParamData.length; i++) {
                double[] data = new double[bestParamData[i].getCalibrationDataSize()];
                for (int j = 0; j < data.length; j++) {
                    data[j] = bestParamValues[index];
                    index++;
                }
                bestParamData[i].generateValues(data);
            }
        }

        public double[] getBestParamDataArray() {
            double[] bestParamValues = new double[paramValues.length];
            int index = 0;
            for (int i = 0; i < bestParamData.length; i++) {
                double[] values = bestParamData[i].getCalibrationData();
                for (int j = 0; j < values.length; j++) {
                    bestParamValues[index] = values[j];
                    index++;
                }
            }
            return bestParamValues;
        }

        public double[] getUpperBound() {
            return upperBound;
        }

        public double[] getLowerBound() {
            return lowerBound;
        }

        public double[] getParamValues() {
            return paramValues;
        }

        static public void copyParamValues(Data source, Data dest) {
            double[] paramValueArray = new double[source.getParamValues().length];
            for (int i = 0; i < paramValueArray.length; i++) {
                paramValueArray[i] = source.getParamValues()[i];
            }
            dest.setParamValues(paramValueArray);
            dest.setBestParamData(paramValueArray);
        }
    }
    Data[] r;

    public Data[] round() {
        return r;
    }
    ////////////////// Runtime fields and methods
    Date calibStart;
    Date calibEnd;
    File outFolder;

    void post(int round, Data step) {
        for (int i = round + 1; i < r.length; i++) {
            Data.copyParamValues(step, r[i]);
        }
    }

    void init(ModelExecution model, Date calibStart, Date calibEnd, int rounds) throws IOException {
        this.calibStart = calibStart;
        this.calibEnd = calibEnd;
        this.outFolder = model.lastFolder;

        r = new Data[rounds];
        ParameterData[] p = create(params(), model.getParameter());
        for (int i = 0; i < rounds; i++) {
            r[i] = new Data();
            r[i].round = i;
            r[i].init(p);
//            copy(sourceStepData.get(i), this.stepData[i], executionHandle);
            r[i].createBestParamData();
        }
    }

    public static ParameterData[] create(Params params, Map<String, Object> modelparams) {
        ParameterData[] paramData = new ParameterData[params.getCount()];
        for (int i = 0; i < paramData.length; i++) {
            Param p = params.getParam().get(i);
            String pname = p.getName();
            Object pval = modelparams.get(pname);
            if (pval == null) {
                throw new RuntimeException("Paramter not found '" + pname + "'");
            }
//            System.out.println(pval + " " + pval.getClass());
            if (pval.toString().indexOf('{') == -1 && pval.toString().indexOf('}') == -1) {
                pval = '{' + pval.toString() + '}';
            }
            double[] pv = Conversions.convert(pval, double[].class);
            boolean[] calibrate = new boolean[pv.length];
            for (int j = 0; j < calibrate.length; j++) {
                calibrate[j] = true;
            }
            double lower = p.getLower();
            double upper = p.getUpper();
            if (Double.isNaN(upper)) {
                throw new IllegalArgumentException("'upper' not set:" + pname);
            }
            if (Double.isNaN(lower)) {
                throw new IllegalArgumentException("'lower' not set:" + pname);
            }
            if (lower > upper) {
                throw new IllegalArgumentException("'lower' > 'upper' :" + pname);
            }
            paramData[i] = new ParameterData(pname);
            paramData[i].set(pv, lower, upper, ParameterData.MEAN, calibrate);
        }
        return paramData;
    }

    public boolean maximizeObjectiveFunctionValue() {
        return opt.isInc();
    }

    public double calculateObjectiveFunctionValue(ExecutionHandle executionHandle) {
        try {
            opt.adjustWeights();

            String[] obs = opt.getObserved().split("\\s*\\|\\s*");
            if (obs.length != 3) {
                throw new IllegalArgumentException("Invalid column spec: " + opt.getObserved());
            }
            CSTable tobs = DataIO.table(resolve(obs[0], outFolder), obs[1]);
            double[] obsval = DataIO.getColumnDoubleValuesInterval(calibStart, calibEnd, tobs, obs[2]);

            String[] sim = opt.getSimulated().split("\\s*\\|\\s*");
            if (sim.length != 3) {
                throw new IllegalArgumentException("Invalid column spec: " + opt.getSimulated());
            }
            CSTable tsim = DataIO.table(resolve(sim[0], outFolder), sim[1]);
            double[] simval = DataIO.getColumnDoubleValuesInterval(calibStart, calibEnd, tsim, sim[2]);

            return opt.getMultiOFValue(obsval, simval);
            
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


    public static void main(String[] args) {
       ParameterData p = new ParameterData("ggg");
       p.set(new double[] {2.3}, 0, 4, ParameterData.MEAN, new boolean[] {true});
       System.out.println(p.getMean());
       p.generateValues(4.0);
       System.out.println(Arrays.toString(p.getDataValue()));
       System.out.println(p.getMean());
    }
}
