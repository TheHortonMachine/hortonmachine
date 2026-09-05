/*
 * $Id: Statistics.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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
package oms3.util;

import java.util.Arrays;

/**
 * Statistics basics.
 *
 * @author od
 */
public class Statistics {

    public static final int MAXIMIZATION = 1;
    public static final int MINIMIZATION = 2;
    public static final int ABSMAXIMIZATION = 3;
    public static final int ABSMINIMIZATION = 4;

    private Statistics() {
    }

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
//        return Math.sqrt(variance(vals) / vals.length);
        return stddev(vals) / Math.sqrt(vals.length);
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

    /**
     * Returns the lag-1 autocorrelation of a dataset;
     * 
     * @param vals the array of input values
     * @return the lag-1 autocorrelation
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

    public static double rmse(double[] obs, double[] sim, double missingValue) {
        return Math.sqrt(mse(obs, sim, missingValue));
    }

    public static double mse(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(sim, obs);
        double error = 0;
        int len = 0;
        for (int i = 0; i < sim.length; i++) {
            if (obs[i] > missingValue) {
                double diff = obs[i] - sim[i];
                error += diff * diff;
                len++;
            }
        }
        error /= len;
        return error;
    }

    public static double norm_rmse(double[] obs, double[] sim, double missing) {
        double sum = 0, size = 0;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i] > missing) {
                sum += obs[i];
                size++;
            }
        }

        double measuredMean = sum / size;
        int N = Math.min(obs.length, sim.length);
        double numerator = 0, denominator = 0;
        for (int i = 0; i < N; i++) {
            if (obs[i] > missing) {
                numerator += (obs[i] - sim[i]) * (obs[i] - sim[i]);
                denominator += (obs[i] - measuredMean) * (obs[i] - measuredMean);
            }
        }
        if (denominator == 0) {
            throw new RuntimeException("Error: The denominator is 0.\n"
                    + "This happens if all observed values are equal to their mean.");
        }
        return Math.sqrt(numerator / denominator);
    }

    public static double nbias(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(sim, obs);
        double sum = 0;
        double sumsim = 0;
        for (int i = 0; i < sim.length; i++) {
            if (obs[i] > missingValue) {
                sum += obs[i] - sim[i];
                sumsim += sim[i];
            }
        }
        return sum / sumsim;
    }

    /**
     *
     * @param obs observed data
     * @param sim simulated data
     * @param missingValue the missing value
     * @return pbias
     */
    public static double pbias(double[] obs, double[] sim, double missingValue) {
        return nbias(obs, sim, missingValue) * 100;
    }

    /**
     *
     * @param obs observed data
     * @param sim simulated data
     * @param missingValue the missing value
     * @return abs vol error
     */
    public static double absVolumeError(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(obs, sim);
        double volError = 0;
        for (int i = 0; i < sim.length; i++) {
            if (obs[i] > missingValue) {
                volError += sim[i] - obs[i];
            }
        }
        return Math.abs(volError);
    }

    /**
     * Calculates the efficiency between a test data set and a verification data
     * set after Nash {@literal &} Sutcliffe (1970). The efficiency is described as the
     * proportion of the cumulated cubic deviation between both data sets and
     * the cumulated cubic deviation between the verification data set and its
     * mean value.
     *
     * @param sim the simulation data set
     * @param obs the validation (observed) data set
     * @param pow the power for the deviation terms
     * @param missingValue missingValue
     * @return the calculated efficiency or -9999 if an error occurs
     */
    public static double nashSutcliffe(double[] obs, double[] sim, double pow, double missingValue) {
        sameArrayLen(obs, sim);
        int pre_size = sim.length;

        int steps = pre_size;
        double sum_vd = 0;

        double size = 0;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i] > missingValue) {
                sum_vd += obs[i];
                size++;
            }
        }

        double mean_vd = sum_vd / size;

        /*
         * calculating mean pow deviations
         */
        double td_vd = 0;
        double vd_mean = 0;
        for (int i = 0; i < steps; i++) {
            if (obs[i] > missingValue) {
                td_vd += (Math.pow((Math.abs(obs[i] - sim[i])), pow));
                vd_mean += (Math.pow((Math.abs(obs[i] - mean_vd)), pow));
            }
        }
        return 1 - (td_vd / vd_mean);
    }

    /**
     * Calculates the efficiency between the log values of a test data set and a
     * verification data set after Nash {@literal &} Sutcliffe (1970). The efficiency is
     * described as the proportion of the cumulated cubic deviation between both
     * data sets and the cumulated cubic deviation between the verification data
     * set and its mean value.
     *
     * @param sim the simulation data set
     * @param obs the validation (observed) data set
     * @param pow the power for the deviation terms
     * @param missingValue the missing value
     * @return the calculated log_efficiency or -9999 if an error occurs
     */
    public static double nashSutcliffeLog(double[] obs, double[] sim, double pow, double missingValue) {
        sameArrayLen(obs, sim);
        int size = sim.length;

        double sum_log_pd = 0;
        double sum_log_vd = 0;

        /**
         * calculating logarithmic values of both data sets. Sets 0 if data is 0
         */
        double[] log_preData = new double[size];
        double[] log_valData = new double[size];

        int validPairs = 0;

        for (int i = 0; i < size; i++) {
            //either prediction or validation shows a value of zero
            //in this case the pair is excluded from the further calculation,
            //simply by setting the values to -1 and not increasing valid pairs
            // this will also catch missing values
            if (sim[i] <= 0 || obs[i] <= 0) {
                log_preData[i] = -1;
                log_valData[i] = -1;
            }
            //both prediction and validation shows a value of exact zero
            //in this case the pair is taken as a perfect fit and included
            //into the further calculation
            if (sim[i] == 0 && obs[i] == 0) {
                log_preData[i] = 0;
                log_valData[i] = 0;
                validPairs++;
            }
            //both prediction and validation are greater than zero
            //no problem for the calculation
            if (sim[i] > 0 && obs[i] > 0) {
                log_preData[i] = Math.log(sim[i]);
                log_valData[i] = Math.log(obs[i]);
                validPairs++;
            }
        }

        /*
         * summing up both data sets
         */
        for (int i = 0; i < size; i++) {
            if (log_preData[i] >= 0) {
                sum_log_pd += log_preData[i];
                sum_log_vd += log_valData[i];
            }
        }

        /*
         * calculating mean values for both data sets
         */
        double mean_log_vd = sum_log_vd / validPairs;

        /*
         * calculating mean pow deviations
         */
        double pd_log_vd = 0;
        double vd_log_mean = 0;
        for (int i = 0; i < size; i++) {
            if (log_preData[i] >= 0) {
                pd_log_vd += (Math.pow(Math.abs(log_valData[i] - log_preData[i]), pow));
                vd_log_mean += (Math.pow(Math.abs(log_valData[i] - mean_log_vd), pow));
            }
        }
        return 1 - (pd_log_vd / vd_log_mean);
    }

    public static double err_sum(double[] obs, double[] sim) {
        double volError = 0;
        for (int i = 0; i < sim.length; i++) {
            volError += (sim[i] - obs[i]);
        }
        return volError;
    }

    /**
     * Calculates the index of agreement (ioa) between a test data set and a
     * verification data set after Willmot {@literal &} Wicks (1980). The ioa is described
     * as the proportion of the cumulated cubic deviation between both data sets
     * and the squared sum of the absolute deviations between the verification
     * data set and the test mean value and the test data set and its mean
     * value.
     *
     * @param obs the test Data set
     * @param sim the verification data set
     * @param missingValue missingValue
     * @param pow the power
     * @return the calculated ioa or -9999 if an error occurs
     */
    public static double ioa(double[] obs, double[] sim, double pow, double missingValue) {
        sameArrayLen(obs, sim);
        int steps = sim.length;
        double sum_obs = 0;

        double len = 0.0;

        for (int i = 0; i < steps; i++) {
            if (obs[i] > missingValue) {
                sum_obs += obs[i];
                len++;
            }
        }

        // calculating mean values for both data sets
        double mean_obs = sum_obs / len;

        // calculating absolute squared sum of deviations from verification mean
        double td_vd = 0;
        double abs_sqDevi = 0;
        for (int i = 0; i < steps; i++) {
            if (obs[i] > missingValue) {
                td_vd += (Math.pow((Math.abs(obs[i] - sim[i])), pow));
                abs_sqDevi += Math.pow(Math.abs(sim[i] - mean_obs) + Math.abs(obs[i] - mean_obs), pow);
            }
        }
        return 1.0 - (td_vd / abs_sqDevi);
    }

    /**
     * Calcs coefficients of linear regression between x, y data
     *
     * @param xData the independent data array (x)
     * @param yData the dependent data array (y)
     * @return (intercept, gradient, r2)
     */
    public static double[] linearReg(double[] xData, double[] yData) {
        sameArrayLen(xData, yData);
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
        return linearReg(xData, yData)[0];
    }

    public static double gradient(double[] xData, double[] yData) {
        return linearReg(xData, yData)[1];
    }

    public static double r2(double[] xData, double[] yData) {
        return linearReg(xData, yData)[2];
    }

    /**
     * Round a double value to a specified number of decimal places.
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
     * Round a float value to a specified number of decimal places.
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
     * @param min the min value of the range
     * @param max the max value of the range
     * @return the random value in the min/max range
     */
    public static double random(double min, double max) {
        assert max > min;
        return min + Math.random() * (max - min);
    }

    /**
     * Runoff coefficient error ROCE
     *
     * @param obs the observed data set
     * @param sim the simulated data set
     * @param precip the precip data
     * @return roce
     */
    public static double runoffCoefficientError(double[] obs, double[] sim, double[] precip) {
        sameArrayLen(sim, obs, precip);
        double mean_pred = mean(sim);
        double mean_val = mean(obs);
        double mean_ppt = mean(precip);
        double error = Math.abs((mean_pred / mean_ppt) - (mean_val / mean_ppt));
        return Math.sqrt(error);
    }

    /**
     * transformedRootMeanSquareError TRMSE
     *
     * @param obs the observed data set
     * @param sim the simulated data set
     * @param missingValue the missing value
     * @return TRMSE
     */
    public static double transformedRmse(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(sim, obs);
        double error = 0;
        double z_pred = 0.;
        double z_val = 0.;
        int len = 0;
        for (int i = 0; i < sim.length; i++) {
            if (obs[i] > missingValue) {
                z_pred = (Math.pow((1.0 + sim[i]), 0.3) - 1.0) / 0.3;
                z_val = (Math.pow((1.0 + obs[i]), 0.3) - 1.0) / 0.3;
                error += (z_pred - z_val) * (z_pred - z_val);
                len++;
            }
        }
        return Math.sqrt(error / len);
    }

    /**
     * Compute the Pearson correlation
     *
     * @param obs observed data
     * @param sim simulated data
     * @param missingValue missing Value
     * @return pearsonsCorrelation
     */
    public static double pearsonsCorrelation(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(obs, sim);
        double syy = 0.0, sxy = 0.0, sxx = 0.0, ay = 0.0, ax = 0.0;

        int n = 0;
        for (int j = 0; j < obs.length; j++) {
            if (obs[j] > missingValue) {
                ax += obs[j];
                ay += sim[j];
                n++;
            }
        }
        if (n == 0) {
            throw new RuntimeException("Pearson's Correlation cannot be calculated due to no observed values");
        }
        ax = ax / ((double) n);
        ay = ay / ((double) n);
        for (int j = 0; j < obs.length; j++) {
            if (obs[j] > missingValue) {
                double xt = obs[j] - ax;
                double yt = sim[j] - ay;
                sxx += xt * xt;
                syy += yt * yt;
                sxy += xt * yt;
            }
        }
        return sxy / Math.sqrt(sxx * syy);
    }

    /**
     *
     * @param obs observed data
     * @param sim simulated data
     * @param missingValue the missing value
     * @return absdiff
     */
    public static double absDiff(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(obs, sim);
        double abs = 0;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i] > missingValue) {
                abs += Math.abs(obs[i] - sim[i]);
            }
        }
        return abs;
    }

    public static double stderrReg(double[] regcoeff, double[] obs, double[] sim) {
        sameArrayLen(obs, sim);
        double sum = 0;
        for (int i = 0; i < obs.length; i++) {
            double res = sim[i] * regcoeff[1] + regcoeff[0];
            sum += (obs[i] - res) * (obs[i] - res);
        }
        return Math.sqrt(sum / (obs.length - 2));
    }

    /**
     *
     * @param obs observed data
     * @param sim simulated data
     * @param missingValue missing Value
     * @return abslogdiff
     */
    public static double absDiffLog(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(obs, sim);
        int N = obs.length;
        double abs = 0;
        for (int i = 0; i < N; i++) {
            if (obs[i] > missingValue) {
                double measured = obs[i];
                double simulated = sim[i];
                if (measured == 0) {
                    measured = 0.0000001;
                } else if (measured < 0) {
                    throw new RuntimeException("Error on Absolute Difference (log): Observed value is negative.");
                }
                if (simulated == 0) {
                    simulated = 0.0000001;
                } else if (simulated < 0) {
                    throw new RuntimeException("Error on Absolute Difference (log): Simulated value is negative.");
                }
                abs += Math.abs(Math.log(measured) - Math.log(simulated));
            }
        }
        return abs;
    }

    /**
     *
     * @param obs observed data
     * @param sim simulated data
     * @return dsGrad
     */
    public static double dsGrad(double[] obs, double[] sim) {
        sameArrayLen(obs, sim);
        int dsLength = sim.length;

        double[] cumPred = new double[dsLength];
        double[] cumVali = new double[dsLength];

        double cp = 0;
        double cv = 0;
        for (int i = 0; i < dsLength; i++) {
            cp += sim[i];
            cv += obs[i];
            cumPred[i] = cp;
            cumVali[i] = cv;
        }

        //interc., grad., r?
        double[] regCoef = linearReg(cumVali, cumPred);
        return regCoef[1];
    }

    /**
     * Fenicia low flow
     * 
     * @param obs the observed data set
     * @param sim the simulated data set
     * @param missingValue the missing value
     * @return the Fenicia low flow
     */
    public static double flf(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(obs, sim);
        int count = 0;
        double FLF = 0;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i] > missingValue) {
                if (sim[i] != 0 && obs[i] != 0) {
                    count++;
                    FLF += (Math.log(sim[i]) - Math.log(obs[i])) * (Math.log(sim[i]) - Math.log(obs[i]));
                }
            }
        }
        return FLF / count;
    }

    /**
     * * Fenicia high flow
     * 
     * @param obs the observed data set
     * @param sim the simulated data set
     * @param missingValue the missing value
     * @return the Fenicia high flow
     */
    public static double fhf(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(obs, sim);
        int count = 0;
        double FHF = 0;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i] > missingValue) {
                count++;
                FHF += (sim[i] - obs[i]) * (sim[i] - obs[i]);
            }
        }
        return FHF / count;
    }

    /**
     * Kling and Gupta Efficiency.
     *
     * @param obs the observed data set
     * @param sim the simulated data set
     * @param missingValue the missing value
     * @return the kge
     */
    public static double kge(double[] obs, double[] sim, double missingValue) {
        sameArrayLen(obs, sim);
        int contamedia = 0;
        double sommamediaoss = 0;
        double sommamediasim = 0;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i] > missingValue) {
                contamedia++;
                sommamediaoss += obs[i];
                sommamediasim += sim[i];
            }
        }
        double mediaoss = sommamediaoss / contamedia;
        double mediasim = sommamediasim / contamedia;
        int count = 0;
        double numvaprev = 0;
        double coef1_den = 0;
        double numR = 0;
        double den1R = 0;
        double den2R = 0;
        for (int i = 0; i < obs.length; i++) {
            if (obs[i] > missingValue) {
                count++;
                coef1_den += (obs[i] - mediaoss) * (obs[i] - mediaoss);
                numR += (obs[i] - mediaoss) * (sim[i] - mediasim);
                den1R += (obs[i] - mediaoss) * (obs[i] - mediaoss);
                den2R += (sim[i] - mediasim) * (sim[i] - mediasim);
                numvaprev += (sim[i] - mediasim) * (sim[i] - mediasim);
            }
        }
        double sdosservati = Math.sqrt(coef1_den / (count - 1));
        double sdsimulati = Math.sqrt(numvaprev / (count - 1));
        double R = numR / (Math.sqrt(den1R) * Math.sqrt(den2R));
        double alpha = sdsimulati / sdosservati;
        double beta = mediasim / mediaoss;
        return 1 - Math.sqrt((R - 1) * (R - 1) + (alpha - 1) * (alpha - 1) + (beta - 1) * (beta - 1));
    }

    /**
     * Check if the arrays have the same length
     *
     * @param arr a list of arrays
     */
    private static void sameArrayLen(double[]... arr) {
        int len = arr[0].length;
        for (double[] a : arr) {
            if (a.length != len) {
                throw new IllegalArgumentException("obs and sim data have not same size (" + a.length + "/" + len + ")");
            }
        }
    }
}
