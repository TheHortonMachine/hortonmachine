/*
 * ParamRange.java
 *
 * Created on January 19, 2007, 2:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu.luca;

/**
 * The class stores the data and provides methods related to parameter data.
 * @author Makiko
 */
public class ParameterData {

    /** Calibration type: the mean value is used for calibration */
    public final static int MEAN = 1;
    /** Calibration type: inidividual paramever values are used for calibration */
    public final static int INDIVIDUAL = 2;
    /** Calibration type: parameter values are binary */
    public final static int BINARY = 3;
    private int calibrationType = MEAN; // this must be set to MEAN, INDIVIDUAL, or BINARY
    String name;
    double[] data;
    double lowerBound;
    double upperBound;
    double originalLowerBound;
    double originalUpperBound;
    boolean hasBounds = false;
    double min;
    double max;
    double offset;
    double[] proportional_dev = null;
    /** This array of flags tell you which parameter value should be calculated.
     * For example, data[i] is calibrated if calibrationFlag[i] is true. This
     * array of flags is used when calibrationType is either INDIVIDUAL or BINARY. */
    boolean[] calibrationFlag;
    /** The number of the parameter values that are calibrated. In otherwords,
     * this is equivalent to the number of true flags in calibrationFlag if
     * calibrationType is either INDIVIDUAL or BINARY. If calibrationType is MEAN,
     * then this value is the same as the size of the double array data. */
    int calibrationDataSize;
    double mean;

    /* Creates a new instance of ParamRange */
    public ParameterData(String paramName) {
        this.name = paramName;
    }

    public ParameterData(ParameterData pdata) {
        name = pdata.getName();
        data = new double[pdata.getDataSize()];
        for (int i = 0; i < data.length; i++) {
            data[i] = pdata.getDataValueAt(i);
        }

        calibrationFlag = new boolean[pdata.getDataSize()];
        for (int i = 0; i < calibrationFlag.length; i++) {
            calibrationFlag[i] = pdata.getCalibrationFlag()[i];
        }

        set(data, pdata.getOriginalLowerBound(), pdata.getOriginalUpperBound(),
                pdata.getCalibrationType(), calibrationFlag);

    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Methods to simply set each field or get each field
    ////////////////////////////////////////////////////////////////////////////////////
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasBounds() {
        return hasBounds;
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    public double getOriginalLowerBound() {
        return originalLowerBound;
    }

    public void setOriginalLowerBound(double lowerBound) {
        this.originalLowerBound = lowerBound;
    }

    public double getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public double getOriginalUpperBound() {
        return originalUpperBound;
    }

    public void setOriginalUpperBound(double upperBound) {
        this.originalUpperBound = upperBound;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double[] getDataValue() {
        return data;
    }

    public double getDataValueAt(int index) {
        return data[index];
    }

    public void setDataValue(double[] data) {
        this.data = data;
    }

    public int getDataSize() {
        return data.length;
    }

    public int getCalibrationType() {
        return calibrationType;
    }

    public void setCalibrationType(int calibrationType) {
        this.calibrationType = calibrationType;
    }

    public int getCalibrationDataSize() {
        if (calibrationType == MEAN) {
            return 1;
        } else {
            return calibrationDataSize;
        }
    }

    public boolean needCalibrationAt(int index) {
        return calibrationFlag[index];
    }

    public boolean[] getCalibrationFlag() {
        return calibrationFlag;
    }

    public void setCalibrationFlag(boolean[] calibrate) {
        this.calibrationFlag = calibrate;
        for (int i = 0; i < this.calibrationFlag.length; i++) {
            if (calibrate[i]) {
                calibrationDataSize++;
            }
        }
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    public boolean hasMinAndMax() {
        return calibrationType == MEAN || calibrationDataSize > 0;
    }

    public void removeBounds() {
        hasBounds = false;
    }

    ////////////////////////////////////////////////////////////////////////////////////
    // Methods to find mean, max, and min, and to calculate actual lower & upper bound
    ////////////////////////////////////////////////////////////////////////////////////
    /* Sets the parameter values, the type of calibration, and the calibration flag.
     * Also, the mean of the parameter values is calculated, and the max and min value
     * of the parameter values are determined. */
    public void setStat(double[] dataValue, int calibrationType, boolean[] calibrate) {
        this.data = dataValue;
        this.calibrationType = calibrationType;
        this.calibrationFlag = calibrate;

        calibrationDataSize = 0;
        for (int i = 0; i < this.calibrationFlag.length; i++) {
            if (calibrate[i]) {
                calibrationDataSize++;
            }
        }

        calculateMean();
        findMin();
        findMax();
//        setDeviation();
    }

    /* Sets all the fields and calculates everything needed for the calibration
     * strategy. */
    public void set(double[] dataValue, double lowerBound, double upperBound,
            int calibrationType, boolean[] needCalibration) {
        setStat(dataValue, calibrationType, needCalibration);
        setLowerAndUpperBounds(lowerBound, upperBound);
    }

    /** Set the lower and upper bounds, and the actual bounds are determined. */
    public void setLowerAndUpperBounds(double lower, double upper) {
        if (data == null) {
            return;
        }

        this.originalLowerBound = lower;
        this.originalUpperBound = upper;
        if (originalLowerBound < min) {
            offset = Math.abs(originalLowerBound) + 10;
        } else {
            offset = Math.abs(min) + 10;
        }

        if (calibrationType == MEAN) {
            lowerBound = (originalLowerBound + offset) * (mean + offset) / (min + offset) - offset;
            upperBound = (originalUpperBound + offset) * (mean + offset) / (max + offset) - offset;
        } else {
            lowerBound = originalLowerBound;
            upperBound = originalUpperBound;
        }
        hasBounds = true;
        setDeviation();
    }

    private void setDeviation() {
        /*
         **  Compute the proportional deviation from the mean for each
         **  individual parameter value.
         */
        proportional_dev = new double[data.length];
        for (int i = 0; i < proportional_dev.length; i++) {
            proportional_dev[i] = (data[i] + offset) / (mean + offset);
        }
    }

   
        private void calculateMean() {
        
        double sum = 0;
        Integer count = 0;
        
        for(int i=0; i<data.length; i++) {
            if (calibrationFlag[i]) {
                sum += data[i];
                count++;
            }
        }
        mean = sum / count.doubleValue();
        
    }
    
  
    private void findMax() {
        
        if(calibrationDataSize > 0) {
            int index = 0;
            while(!calibrationFlag[index]) {
                index++;
            }
            max = data[index];
            index++;
            for(int i=index; i<data.length; i++) {
                if(calibrationFlag[i] && data[i] > max) {
                    max = data[i];
                }
            }
        }
        
    }
    
    private void findMin() {
        
        if(calibrationDataSize > 0)  {
            int index = 0;
            while(!calibrationFlag[index]) {
                index++;
            }
            min = data[index];
            index++;
            for(int i=index; i<data.length; i++) {
                if(calibrationFlag[i] && data[i] < min) {
                    min = data[i];
                }
            }
        }
        
    }
  

    ////////////////////////////////////////////////////////////////////////////////////
    //  Methods the conversion from/to values used in SCE to/from the values in statvar file
    ////////////////////////////////////////////////////////////////////////////////////
    /* returns the data array that must be calibrated. */
    public double[] getCalibrationData() {
        if (calibrationType == MEAN) {
            double[] meanArray = new double[1];
            meanArray[0] = mean;
            return meanArray;
        } else {
            double[] calibrationData = new double[calibrationDataSize];
            int index = 0;
            for (int i = 0; i < data.length; i++) {
                if (calibrationFlag[i]) {
                    calibrationData[index] = data[i];
                    index++;
                }
            }
            return calibrationData;
        }
    }

    /* Generate individual parameter values based on the new mean (newMean). */
    public void generateValues(double newMean) {
        double meanWithOffset = newMean + offset;
        for (int i = 0; i < data.length; i++) {
            data[i] = meanWithOffset * proportional_dev[i] - offset;
        }
        mean = newMean;
    }

    /* Generates the parameter values based on the new values (values). */
    public void generateValues(double[] values) {
        if (calibrationType == MEAN) {
            generateValues(values[0]);
        } else if (calibrationType == INDIVIDUAL) {
            int index = 0;
            for (int i = 0; i < data.length; i++) {
                if (calibrationFlag[i]) {
                    data[i] = values[index];
                    index++;
                }
            }      
            calculateMean();
        } else { // Binary
            int index = 0;
            for (int i = 0; i < data.length; i++) {
                if (calibrationFlag[i]) {
                    if (values[index] < 0.5) {
                        data[i] = 0;
                    } else {
                        data[i] = 1;
                    }
                    index++;
                }
            }
            calculateMean();
        }
    }
}
