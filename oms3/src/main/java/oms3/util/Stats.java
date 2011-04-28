/*
 * Stats.java
 *
 * Created on April 27, 2007, 10:42 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package oms3.util;

import java.util.Arrays;

/**
 *
 * @author Olaf David
 */
public class Stats {

    /** Creates a new instance of Stats */
    private Stats() {
    }

    /** Normalized Vector.
     */
    public static double norm_vec(double x, double y, double z) {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static double max(double[] vals) {
        double max = vals[0];
        for (double v : vals) {
            if (v > max) {
                max = v;
            }
        }
        return max;
    }

    public static double min(double[] vals) {
        double min = vals[0];
        for (double v : vals) {
            if (v < min) {
                min = v;
            }
        }
        return min;
    }

    public static double range(double[] vals) {
        double min = vals[0];
        double max = vals[0];
        for (double v : vals) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
        }
        return max - min;
    }

    public static int length(double[] vals) {
        return vals.length;
    }

    public static double median(double[] vals) {
        return quantile(vals, 0.5);
    }

    public static double mean(double[] vals) {
        return sum(vals) / vals.length;
    }

    public static double stddev(double[] vals) {
        double mean = mean(vals);
        double squareSum = 0;
        for (double v : vals) {
            squareSum += v * v;
        }
        return Math.sqrt(squareSum / vals.length - mean * mean);
    }

    public static double stderr(double[] vals) {
        return Math.sqrt(variance(vals) / vals.length);
    }

    public static double variance(double[] vals) {
        double stddev = stddev(vals);
        return stddev * stddev;
    }

    public static double meandev(double[] vals) {
        double mean = mean(vals);
        int size = vals.length;
        double sum = 0;
        for (int i = size; --i >= 0;) {
            sum += Math.abs(vals[i] - mean);
        }
        return sum / size;
    }

    public static double sum(double[] vals) {
        double sum = 0;
        for (double v : vals) {
            sum = sum + v;
        }
        return sum;
    }

    public static double product(double[] vals) {
        double prod = 1;
        for (double v : vals) {
            prod = prod * v;
        }
        return prod;
    }

    public static double quantile(double[] vals, double phi) {
        if (vals.length == 0) {
            return 0.0;
        }

        double[] sortedElements = Arrays.copyOf(vals, vals.length);
        Arrays.sort(sortedElements);
        int n = sortedElements.length;

        double index = phi * (n - 1);
        int lhs = (int) index;
        double delta = index - lhs;
        double result;

        if (lhs == n - 1) {
            result = sortedElements[lhs];
        } else {
            result = (1 - delta) * sortedElements[lhs] + delta * sortedElements[lhs + 1];
        }
        return result;
    }

    /**  Returns the lag-1 autocorrelation of a dataset;
     */
    public static double lag1(double[] vals) {
        double mean = mean(vals);
        int size = vals.length;
        double r1;
        double q = 0;
        double v = (vals[0] - mean) * (vals[0] - mean);
        for (int i = 1; i < size; i++) {
            double delta0 = (vals[i - 1] - mean);
            double delta1 = (vals[i] - mean);
            q += (delta0 * delta1 - q) / (i + 1);
            v += (delta1 * delta1 - v) / (i + 1);
        }
        r1 = q / v;
        return r1;
    }

    public static double rmse(double[] pred, double[] valid) {
        double error = 0;
        for (int i = 0; i < pred.length; i++) {
            double diff = pred[i] - valid[i];
            error += diff * diff;
        }
        error /= pred.length;
        return Math.sqrt(error);
    }

    public static double bias(double[] pred, double[] valid) {
        double sum = 0;
        for (int i = 0; i < pred.length; i++) {
            sum += pred[i] - valid[i];
        }
        return sum / sum(pred);
    }

    /** Calculates the efficiency between a test data set and a verification data set
     * after Nash & Sutcliffe (1970). The efficiency is described as the proportion of
     * the cumulated cubic deviation between both data sets and the cumulated cubic
     * deviation between the verification data set and its mean value.
     * @param prediction the simulation data set
     * @param validation the validation (observed) data set
     * @param pow the power for the deviation terms
     * @return the calculated efficiency or -9999 if an error occurs
     */
    public static double nashsut(double[] prediction, double[] validation, double pow) {
        int pre_size = prediction.length;
        int val_size = validation.length;

        int steps = 0;

        double sum_td = 0;
        double sum_vd = 0;

        /** checking if both data arrays have the same number of elements*/
        if (pre_size != val_size) {
            System.err.println("Prediction data and validation data are not consistent!");
            return -9999;
        } else {
            steps = pre_size;
        }

        /**summing up both data sets */
        for (int i = 0; i < steps; i++) {
            sum_td = sum_td + prediction[i];
            sum_vd = sum_vd + validation[i];
        }

        /** calculating mean values for both data sets */
        double mean_td = sum_td / steps;
        double mean_vd = sum_vd / steps;

        /** calculating mean pow deviations */
        double td_vd = 0;
        double vd_mean = 0;
        for (int i = 0; i < steps; i++) {
            td_vd = td_vd + (Math.pow((Math.abs(validation[i] - prediction[i])), pow));
            vd_mean = vd_mean + (Math.pow((Math.abs(validation[i] - mean_vd)), pow));
        }
        /** calculating efficiency after Nash & Sutcliffe (1970) */
        double efficiency = 1 - (td_vd / vd_mean);
        return efficiency;
    }

    /** Calculates the efficiency between the log values of a test data set and a verification data set
     * after Nash & Sutcliffe (1970). The efficiency is described as the proportion of
     * the cumulated cubic deviation between both data sets and the cumulated cubic
     * deviation between the verification data set and its mean value.
     * @param prediction the simulation data set
     * @param validation the validation (observed) data set
     * @param pow the power for the deviation terms
     * @return the calculated log_efficiency or -9999 if an error occurs
     */
    public static double nashsut_log(double[] prediction, double[] validation, double pow) {
        int pre_size = prediction.length;
        int val_size = validation.length;

        int steps = 0;

        double sum_log_pd = 0;
        double sum_log_vd = 0;

        /** checking if both data arrays have the same number of elements*/
        if (pre_size != val_size) {
            System.err.println("Prediction data and validation data are not consistent!");
            return -9999;
        } else {
            steps = pre_size;
        }

        /** calculating logarithmic values of both data sets. Sets 0 if data is 0 */
        double[] log_preData = new double[pre_size];
        double[] log_valData = new double[val_size];

        for (int i = 0; i < steps; i++) {
            if (prediction[i] < 0) {
                System.err.println("Logarithmic efficiency can only be calculated for positive values!");
                return -9999;
            }
            if (validation[i] < 0) {
                System.err.println("Logarithmic efficiency can only be calculated for positive values!");
                return -9999;
            }

            if (prediction[i] == 0) {
                log_preData[i] = 0;
            } else {
                log_preData[i] = Math.log(prediction[i]);
            }

            if (validation[i] == 0) {
                log_valData[i] = 0;
            } else {
                log_valData[i] = Math.log(validation[i]);
            }
        }

        /**summing up both data sets */
        for (int i = 0; i < steps; i++) {
            sum_log_pd = sum_log_pd + log_preData[i];
            sum_log_vd = sum_log_vd + log_valData[i];
        }

        /** calculating mean values for both data sets */
        double mean_log_pd = sum_log_pd / steps;
        double mean_log_vd = sum_log_vd / steps;

        /** calculating mean pow deviations */
        double pd_log_vd = 0;
        double vd_log_mean = 0;
        for (int i = 0; i < steps; i++) {
            pd_log_vd = pd_log_vd + (Math.pow(Math.abs(log_valData[i] - log_preData[i]), pow));
            vd_log_mean = vd_log_mean + (Math.pow(Math.abs(log_valData[i] - mean_log_vd), pow));
        }

        /** calculating efficiency after Nash & Sutcliffe (1970) */
        double log_efficiency = 1 - (pd_log_vd / vd_log_mean);
        return log_efficiency;
    }

    public static double err_sum(double[] validation, double[] prediction) {
        double volError = 0;
        for (int i = 0; i < prediction.length; i++) {
            volError += (prediction[i] - validation[i]);
        }
        return volError;
    }

    /** Calculates the index of agreement (ioa) between a test data set and a verification data set
     * after Willmot & Wicks (1980). The ioa is described as the proportion of
     * the cumulated cubic deviation between both data sets and the squared sum of the absolute
     * deviations between the verification data set and the test mean value and the test data set and
     * its mean value.
     * @param prediction the test Data set
     * @param validation the verification data set
     * @param pow the power
     * @return the calculated ioa or -9999 if an error occurs
     */
    public static double ioa(double[] prediction, double[] validation, double pow) {
        double ioa;
        int td_size = prediction.length;
        int vd_size = validation.length;
        if (td_size != vd_size) {
            throw new IllegalArgumentException("Data sets in ioa does not match!");
        }

        int steps = td_size;

        double sum_td = 0;
        double sum_vd = 0;

        /**summing up both data sets */
        for (int i = 0; i < steps; i++) {
            sum_td = sum_td + prediction[i];
            sum_vd = sum_vd + validation[i];
        }

        /** calculating mean values for both data sets */
        double mean_td = sum_td / steps;
        double mean_vd = sum_vd / steps;

        /** calculating mean cubic deviations */
        double td_vd = 0;
        double vd_mean = 0;
        for (int i = 0; i < steps; i++) {
            td_vd = td_vd + (Math.pow((Math.abs(validation[i] - prediction[i])), pow));
            vd_mean = vd_mean + (Math.pow((Math.abs(validation[i] - mean_vd)), pow));
        }

        /** calculating absolute squared sum of deviations from verification mean */
        double ad_test = 0;
        double ad_veri = 0;
        double abs_sqDevi = 0;
        for (int i = 0; i < steps; i++) {
            abs_sqDevi = abs_sqDevi + Math.pow(Math.abs(prediction[i] - mean_vd) + Math.abs(validation[i] - mean_vd), pow);
        }

        /** calculating ioa */
        ioa = 1.0 - (td_vd / abs_sqDevi);
        return ioa;
    }

    /**
     * Calcs coefficients of linear regression between x, y data
     * @param xData the independent data array (x)
     * @param yData the dependent data array (y)
     * @return (intercept, gradient, r2)
     */
    private static double[] calcLinReg(double[] xData, double[] yData) {
        double sumX = 0;
        double sumY = 0;
        double prod = 0;
        int nstat = xData.length;
        double[] regCoef = new double[3]; //(intercept, gradient, r2)

        double meanYValue = mean(yData);
        double meanXValue = mean(xData);

        //calculating regression coefficients
        for (int i = 0; i < nstat; i++) {
            sumX += (xData[i] - meanXValue) * (xData[i] - meanXValue);
            sumY += (yData[i] - meanYValue) * (yData[i] - meanYValue);
            prod += (xData[i] - meanXValue) * (yData[i] - meanYValue);
        }
        if (sumX > 0 && sumY > 0) {
            regCoef[1] = prod / sumX;  //gradient
            regCoef[0] = meanYValue - regCoef[1] * meanXValue; //intercept
            regCoef[2] = Math.pow((prod / Math.sqrt(sumX * sumY)), 2); //r2
        }
        return regCoef;
    }

    public static double intercept(double[] xData, double[] yData) {
        return calcLinReg(xData, yData)[0];
    }

    public static double gradient(double[] xData, double[] yData) {
        return calcLinReg(xData, yData)[1];
    }

    public static double r2(double[] xData, double[] yData) {
        return calcLinReg(xData, yData)[2];
    }

    /**
     * Round a double value to a specified number of decimal
     * places.
     *
     * @param val the value to be rounded.
     * @param places the number of decimal places to round to.
     * @return val rounded to places decimal places.
     */
    public static double round(double val, int places) {
        long factor = (long) Math.pow(10, places);

        // Shift the decimal the correct number of places
        // to the right.
        val = val * factor;

        // Round to the nearest integer.
        long tmp = Math.round(val);

        // Shift the decimal the correct number of places
        // back to the left.
        return (double) tmp / factor;
    }

    /**
     * Round a float value to a specified number of decimal
     * places.
     *
     * @param val the value to be rounded.
     * @param places the number of decimal places to round to.
     * @return val rounded to places decimal places.
     */
    public static float round(float val, int places) {
        return (float) round((double) val, places);
    }

    /**
     * Generate a random number in a range.
     *
     * @param min
     * @param max
     * @return the random value in the min/max range
     */
    public static double random(double min, double max) {
        assert max > min;
        return min + Math.random() * (max - min);
    }
}
