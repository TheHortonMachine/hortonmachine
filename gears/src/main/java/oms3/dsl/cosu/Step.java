/*
 * $Id: Step.java e6d7b0143a21 2015-06-08 odavid@colostate.edu $
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

import oms3.dsl.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import oms3.ngmf.util.UnifiedParams;
import oms3.ngmf.util.cosu.luca.ExecutionHandle;
import oms3.ngmf.util.cosu.luca.ParameterData;
import oms3.Conversions;
import oms3.dsl.cosu.Luca.ModelExecution;

/**
 *
 * @author od
 */
public class Step implements Buildable {

    public static final String DSL_NAME = "step";
    String name;
    Params params = new Params();
    // sce control parameter
    int NumOfParams = 0; // number of individual params (e.g. for 1 parameter with 12 entries, it is 12)
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
    List<ObjFunc> ofs = new ArrayList<>();

    //

    Step(int number) {
        this.number = number;
    }


    public List<ObjFunc> getOfs() {
        return ofs;
    }
    

    public int getNumber() {
        return number;
    }


    public void setName(String name) {
        this.name = name;
    }


    public void setName(int number) {
        this.name = Integer.toString(number);
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
        return evolutions > -1 ? evolutions : (NumOfParams * 2 + 1);
    }


    public int getPointsPerComplex() {
        return pointsPerComplex > -1 ? pointsPerComplex : (NumOfParams * 2 + 1);
    }


    public int getPointsPerSubcomplex() {
        return pointsPerSubcomplex > -1 ? pointsPerSubcomplex : (NumOfParams + 1);
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
        } else if (name.equals("objfunc")) {
            ObjFunc of = new ObjFunc();
            ofs.add(of);
            return of;
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


        public int getRound() {
            return round;
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
//            bestOFPoint = Double.NaN;
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


        public ParameterData[] getParamData() {
            return paramData;
        }


        public void setBestParamData(ParameterData[] bestParamData) {
            this.bestParamData = bestParamData;
        }


        public ParameterData[] getBestParamData() {
            return bestParamData;
        }


        public void setParamValues(double[] paramValues) {
            this.paramValues = paramValues;
            int index = 0;
            for (int i = 0; i < paramData.length; i++) {
                double[] data = new double[paramData[i].getCalibrationDataSize()];
                if (paramValues.length < data.length) {
                    throw new RuntimeException("CalibrationDataSize = " + paramData[i].getCalibrationDataSize()
                            + ";  but paramValues.length = " + paramValues.length);
                }
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


        // data from same steps
        static public void copyParamValuesSameStepDiffRounds(Data source, Data dest) {
            if (source == dest) {
                return;
            }

            double[] paramValueArray = new double[source.getParamValues().length];
            for (int i = 0; i < paramValueArray.length; i++) {
                paramValueArray[i] = source.getParamValues()[i];
            }
            dest.setParamValues(paramValueArray);

            double[] bp = source.getBestParamDataArray();
            double[] bestparamValueArray = new double[bp.length];
            for (int i = 0; i < bestparamValueArray.length; i++) {
                bestparamValueArray[i] = bp[i];
            }
            dest.setBestParamData(bestparamValueArray); // correct
            dest.setObjFuncValueOfBestPoint(source.getObjFuncValueOfBestPoint());

            for (int i = 0; i < source.paramData.length; i++) {   // source parameter
                ParameterData sp = source.paramData[i];
                ParameterData spb = source.bestParamData[i];
                int dindex = 0;
                for (int j = 0; j < dest.paramData.length; j++) {   // dest parameter
                    ParameterData dp = dest.paramData[j];
                    ParameterData dpb = dest.bestParamData[j];
                    if (sp.getName().equals(dp.getName())) {
                        // parameter
//                        System.out.println("Copy between rounds: " + sp.getName());                        
//                        System.out.println("calibdata " + dp.getCalibrationDataSize() + " " + sindex + " " + dindex + " " + Arrays.toString(srcParam) + " " + Arrays.toString(destParam));
//                        System.out.println("Src value : " + Arrays.toString(sp.getDataValue()));

                        // set all the 
//                        dp.setDataValue(sp.getDataValue());
//                        dpb.setDataValue(spb.getDataValue());
//                        System.out.println("r src :" + Arrays.toString(sp.getDataValue()));
//                        System.out.println("r des :" + Arrays.toString(dp.getDataValue()));
//                        System.out.println("r fla :" + Arrays.toString(sp.getCalibrationFlag()));
                         System.arraycopy(sp.getDataValue(), 0, dp.getDataValue(), 0, sp.getCalibrationFlag().length);
//                        mergevalues(sp.getDataValue(), dp.getDataValue(), sp.getCalibrationFlag());
//                        System.out.println("r des1 :" + Arrays.toString(dp.getDataValue()));

                        // the same parameter can have different subsets, hence the minimum
                    }
                }
            }

//            dest.setParameterData(source.getParamData());
//            dest.setBestParamData(source.getBestParamData());
//            dest.setObjFuncValueOfBestPoint(source.getObjFuncValueOfBestPoint());
        }


        // data from different steps
        static public void copyParamValuesDiffStepsSameRound(Data source, Data dest) {
            if (source == dest) {
                return;
            }

            double[] srcParam = source.getParamValues();
            double[] destParam = dest.getParamValues();
            double[] srcBestParam = source.getBestParamDataArray();

//            System.out.println("Source " + Arrays.toString(source.getParamValues()));
//
//            System.out.println("Source Best" + Arrays.toString(source.getBestParamDataArray()));
//            System.out.println("Source Best OF " + source.getObjFuncValueOfBestPoint());
//            System.out.println("Dest   Best OF " + dest.getObjFuncValueOfBestPoint());

//            dest.setObjFuncValueOfBestPoint(source.getObjFuncValueOfBestPoint());
//            double[] destBestParam = dest.getBestParamDataArray();
//            int sindex = 0;
            for (int i = 0; i < source.paramData.length; i++) {   // source parameter
                ParameterData sp = source.paramData[i];
                ParameterData spb = source.bestParamData[i];
//                int dindex = 0;
                for (int j = 0; j < dest.paramData.length; j++) {   // dest parameter
                    ParameterData dp = dest.paramData[j];
                    ParameterData dbp = dest.bestParamData[j];
                    if (sp.getName().equals(dp.getName())) {
                        // parameter
//                        System.out.println("Copy between steps: " + sp.getName());
//                        System.out.println("calibdata " + dp.getCalibrationDataSize() + " " + sindex + " " + dindex + " " + Arrays.toString(srcParam) + " " + Arrays.toString(destParam));
//                        System.out.println("Src value : " + Arrays.toString(sp.getDataValue()));

                        // set all the 
//                        System.out.println(Arrays.toString(sp.getCalibrationFlag()));
//                        System.out.println(Arrays.toString(dp.getCalibrationFlag()));
//                        System.out.println("src :" + Arrays.toString(sp.getDataValue()));
//                        System.out.println("des :" + Arrays.toString(dp.getDataValue()));
//                        System.out.println("fla :" + Arrays.toString(sp.getCalibrationFlag()));
                        System.arraycopy(sp.getDataValue(), 0, dp.getDataValue(), 0, sp.getCalibrationFlag().length);
//                        mergevalues(sp.getDataValue(), dp.getDataValue(), sp.getCalibrationFlag());
//                        System.out.println("des1 :" + Arrays.toString(dp.getDataValue()));

//                        dp.setDataValue(sp.getDataValue());
//                        dbp.setDataValue(spb.getDataValue());
                        // the same parameter can have different subsets, hence the minimum
//                        int cp_len = Math.min(sp.getCalibrationDataSize(), dp.getCalibrationDataSize());
//                        System.arraycopy(srcParam, sindex, destParam, dindex, cp_len);  // should be dp.getCalibrationDataSize()
//System.out.println("Src value : " + Arrays.toString(srcParam));
//System.out.println("Src sindex/len : " + sindex + "/" + cp_len);
//System.out.println("Src calib size : " + sp.getCalibrationDataSize());
//System.out.println("dest calib size : " + dp.getCalibrationDataSize());
//                        
//                        double[] d = new double[dp.getCalibrationDataSize()];
//                        System.arraycopy(srcParam, sindex, d, 0, cp_len); // should be dp.getCalibrationDataSize()
//                        dp.generateValues(d);
//
//                        // best param
//                        double[] best = new double[dbp.getCalibrationDataSize()];
//                        System.arraycopy(srcBestParam, sindex, best, 0, cp_len); // should be dp.getCalibrationDataSize()
//                        dbp.generateValues(best);
                        break;
                    }
//                    dindex += dp.getCalibrationDataSize();
                }
//                sindex += sp.getCalibrationDataSize();
            }
        }
    }



    //
    Data[] r;


    public Data[] round() {
        return r;
    }
    //
    ////////////////// Runtime fields and methods
    Date calibStart;
    Date calibEnd;
    File outFolder;
    int startMonthOfYear;
    boolean maximizeOFValue;


    public int getStartMonth() {
        return this.startMonthOfYear;
    }


    void post(int round, Data step) {
        for (int i = round + 1; i < r.length; i++) {
            System.out.println("Copy from: " + round + " to: " + i);
            Data.copyParamValuesSameStepDiffRounds(step, r[i]);
        }
    }


    void init(ModelExecution model, int startMonthOfYear, Date calibStart, Date calibEnd, int rounds) throws IOException {
        this.calibStart = calibStart;
        this.calibEnd = calibEnd;
        this.outFolder = model.lastFolder;
        this.startMonthOfYear = startMonthOfYear;
        r = new Data[rounds];
        ParameterData[] p = createParamData(params(), model.getParameter());
        for (int i = 0; i < rounds; i++) {
            r[i] = new Data();
            r[i].round = i;
            r[i].init(p);
//            copy(sourceStepData.get(i), this.stepData[i], executionHandle);
            r[i].createBestParamData();
            this.NumOfParams += r[i].getParamValues().length;
        }
        maximizeOFValue = ObjFunc.isInc(ofs);
    }


    private static int count(String s, char c) {
        int count = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == c) {
                count++;
            }
        }
        return count;
    }


    static ParameterData[] createParamData(Params params, UnifiedParams modelparams) throws IOException {
        System.out.println("Calling create param Data.");
        ParameterData[] paramData = new ParameterData[params.getCount()];
        for (int i = 0; i < paramData.length; i++) {
            Param p = params.getParam().get(i);

            String pname = p.getName();
            Object pval = modelparams.getParamValue(pname);

            if (pval == null) {
//                System.out.println(modelparams);
                throw new RuntimeException("Paramter not found '" + pname + "'");
            }

            double[] pv;
            int length_col;
            int length_row;
            if (pval.toString().indexOf('{') == -1 && pval.toString().indexOf('}') == -1) {
                pval = '{' + pval.toString() + '}';
            }
            boolean is2dArray = count(pval.toString(), '{') > 1;
            if (is2dArray) {
                double[][] dd = Conversions.convert(pval.toString(), double[][].class);
                length_col = dd[0].length;
                length_row = dd.length;
                pv = Conversions.rollDoubleArray(dd);
                System.out.println("Unrolled 2D array " + pname + " to 1D for SCE");
            } else {
                // 1D array.
                pv = Conversions.convert(pval, double[].class);
                length_col = pv.length;
                length_row = 1;
            }
//            boolean[] calibrate = p.getCalibrateFlags(modelparams.getParams(), length_col, length_row);
            boolean[] calibrate = p.getCalibrateFlags(modelparams, length_col, length_row);
            System.out.println("Generated calibration flags for " + pname);
            // Make sure something is selected for calibration.
            boolean calCheck = false;
            for (int j = 0; j < calibrate.length; j++) {
                calCheck = calCheck | calibrate[j];
            }
            if (!calCheck) {
                throw new RuntimeException("No parameters selected for calibration");
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
            //paramData[i].converted_2d_to_1d = is2dArray;
            paramData[i].set(pv, lower, upper, p.getStrategyAsInt(), calibrate, length_row, length_col);
        }
        return paramData;
    }


    public boolean maximizeObjectiveFunctionValue() {
        return maximizeOFValue;
    }


    public double calculateObjectiveFunctionValue(ExecutionHandle executionHandle) {
        return ObjFunc.calculateObjectiveFunctionValue(ofs, startMonthOfYear, calibStart, calibEnd, outFolder);
    }

//    public static void main(String[] args) {
//       ParameterData p = new ParameterData("ggg");
//       p.set(new double[] {2.3}, 0, 4, ParameterData.MEAN, new boolean[] {true});
//       System.out.println(p.getMean());
//       p.generateValues(4.0);
//       System.out.println(Arrays.toString(p.getDataValue()));
//       System.out.println(p.getMean());
//    }
}
