/*
 * $Id: Param.java 2fb4d513ea17 2014-06-04 odavid@colostate.edu $
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
package oms3.dsl;

import static oms3.SimBuilder.*;
import oms3.dsl.cosu.RangeParser;
import java.util.List;
import oms3.ngmf.util.cosu.luca.ParameterData;
import oms3.ngmf.util.UnifiedParams;
import oms3.Conversions;

/**
 *
 * @author od
 */
public class Param extends AbstractBuildableLeaf {

    String name;
    Object value;
    double lower = Double.NaN;
    double upper = Double.NaN;
    String calibStrategy = MEAN;
    String calibFilterParam_col = null;
    String calibFilterParam_row = null;
    String calibSubset_col = "0-*";
    String calibSubset_row = "0-*";
    int paramLength = 0;

    public Param() {
    }
    
    public Param(String name, Object value) {
        this.name = name;
        if (value instanceof CharSequence) {
        value = value.toString();
        } 
        this.value = value;
    }

    public void setCalib_strategy(String calib_strategy) {
        calibStrategy = calib_strategy;
    }

    public void setFilter_param(String filter_param) {
        calibFilterParam_col = filter_param;
    }

    public void setFilter_param_col(String filter_param_col) {
        calibFilterParam_col = filter_param_col;
    }

    public void setFilter_param_row(String filter_param_row) {
        calibFilterParam_row = filter_param_row;
    }

    public void setSubset(String subset) {
        calibSubset_col = subset;
    }

    public void setSubset_col(String subset_col) {
        calibSubset_col = subset_col;
    }

    public void setSubset_row(String subset_row) {
        calibSubset_row = subset_row;
    }

    public double getLower() {
        return lower;
    }

    public double getUpper() {
        return upper;
    }

    public void setLower(double lower) {
        this.lower = lower;
    }

    public void setUpper(double upper) {
        this.upper = upper;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    // called by groovy on xxx(abc:v) value
    public void call(Object value) {
        this.value = value;
    }

    public static String ConvertStrategy(int calibType) {
        if (calibType == ParameterData.MEAN) {
            return MEAN;
        } else if (calibType == ParameterData.BINARY) {
            return BINARY;
        } else if (calibType == ParameterData.INDIVIDUAL) {
            return INDIVIDUAL;
        } else {
            return "Unknown";
        }
    }

    public int getStrategyAsInt() {
        if (calibStrategy.equals(MEAN)) {
            return ParameterData.MEAN;
        } else if (calibStrategy.equals(INDIVIDUAL)) {
            return ParameterData.INDIVIDUAL;
        } else if (calibStrategy.equals(BINARY)) {
            return ParameterData.BINARY;
        } else {
            throw new IllegalArgumentException("Calibration strategy " + calibStrategy + "not valid.");
        }
    }

    public int getLength() { // return size of this param array used for calibration.
        if (calibStrategy.equals(MEAN)) {
            return 1;
        } else {
            return this.paramLength;
        }
    }

    public boolean[] getCalibrateFlags(UnifiedParams modelparams, int length_col, int length_row) {
//    public boolean[] getCalibrateFlags(Map<String, Object> modelparams, int length_col, int length_row) {
        boolean[] colFlags = getCalibrateFlags_col(modelparams, length_col);
        boolean[] rowFlags = getCalibrateFlags_row(modelparams, length_row);
        if ((rowFlags == null) || (rowFlags.length < 2)) {
            //System.out.println("Returning calibFlags from column only= " + colFlags.toString());  // KMOlson TODO remove
            return colFlags;
        } else {
            int index = 0;
            boolean[] flags = new boolean[colFlags.length * rowFlags.length];
            for (int row = 0; row < rowFlags.length; row++) {
                for (int col = 0; col < colFlags.length; col++) {
                    boolean lit = rowFlags[row] && colFlags[col];
                    flags[index++] = lit;
                    if (lit) {
                        paramLength++;
                    }
                }
            }
            //System.out.println("Returning calibFlags from row, column = " + flags.toString());// KMOlson TODO remove
            return flags;
        }
    }

//    private boolean[] getCalibrateFlags_col(Map<String, Object> modelparams, int length) {
    private boolean[] getCalibrateFlags_col(UnifiedParams modelparams, int length) {
        return getCalibrateArray(modelparams, length, calibFilterParam_col, calibSubset_col);
    }

    private boolean[] getCalibrateFlags_row(UnifiedParams modelparams, int length) {
//    private boolean[] getCalibrateFlags_row(Map<String, Object> modelparams, int length) {
        return getCalibrateArray(modelparams, length, calibFilterParam_row, calibSubset_row);
    }

//    private boolean[] getCalibrateArray(Map<String, Object> modelparams, int length, String calibFilterParam, String calibSubset) {
    private boolean[] getCalibrateArray(UnifiedParams modelparams, int length, String calibFilterParam, String calibSubset) {
        if (calibFilterParam == null) { // determine calibFlags based on range matching param index           
            boolean[] calibrationFlags = RangeParser.getArray(calibSubset, length);
            return calibrationFlags;
//        } else if (!modelparams.containsKey(calibFilterParam)) {
        } else if (modelparams.getInternalKey(calibFilterParam) == null) {
            System.out.println(modelparams);      
//            System.out.println(modelparams.keySet());
            throw new IllegalArgumentException("Calibration Filter Parameter (filter_param) " + calibFilterParam + " not found in parameter set.");
        } else {
//            Object values = modelparams.get(calibFilterParam);
            Object values = modelparams.getParamValue(calibFilterParam);
            int[] parr = Conversions.convert(values.toString(), int[].class);

            //  Find max calib param value to limit range array size.
            int pmax = Integer.MIN_VALUE;
            for (int i = 0; i < parr.length; i++) {
                if (parr[i] > pmax) {
                    pmax = parr[i];
                }
            }
            
            // See what filterParams match any of the values in calibSubset
            List<Integer> rlist = RangeParser.parse(calibSubset, pmax+1);
            boolean[] calibrationFlags = new boolean[parr.length];
            for (int i = 0; i < parr.length; i++) {
                calibrationFlags[i] = false;
                for (Integer r : rlist) {
                    if (parr[i] == r) {
                        calibrationFlags[i] = true;
                    }
                }
            }
            return calibrationFlags;
        }
    }
}
