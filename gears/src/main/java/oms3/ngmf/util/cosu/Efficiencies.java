/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu;

import oms3.util.Stats;

/**
 *
 * @author od
 */
public class Efficiencies {

    public static final int MAXIMIZATION = 1;
    public static final int MINIMIZATION = 2;
    public static final int ABSMAXIMIZATION = 3;
    public static final int ABSMINIMIZATION = 4;

    private Efficiencies() {
    }

    private static void sameArrayLen(double[]... arr) {
        int len = arr[0].length;
        for (double[] a : arr) {
            if (a.length != len) {
                throw new IllegalArgumentException("obs and sim data have not same size (" + a.length + "/" + len + ")");
            }
        }
    }

    /** Calculates the efficiency between a test data set and a verification data set
     * after Nash & Sutcliffe (1970). The efficiency is described as the proportion of
     * the cumulated cubic deviation between both data sets and the cumulated cubic
     * deviation between the verification data set and its mean value.
     * @param sim the simulation data set
     * @param obs the validation (observed) data set
     * @param pow the power for the deviation terms
     * @return the calculated efficiency or -9999 if an error occurs
     */
    public static double nashSutcliffe(double[] obs, double[] sim, double pow) {
        sameArrayLen(obs, sim);
        int pre_size = sim.length;

        int steps = pre_size;
        double sum_td = 0;
        double sum_vd = 0;

        /**summing up both data sets */
        for (int i = 0; i < steps; i++) {
            sum_td = sum_td + sim[i];
            sum_vd = sum_vd + obs[i];
        }

        /** calculating mean values for both data sets */
        double mean_vd = sum_vd / steps;

        /** calculating mean pow deviations */
        double td_vd = 0;
        double vd_mean = 0;
        for (int i = 0; i < steps; i++) {
            td_vd = td_vd + (Math.pow((Math.abs(obs[i] - sim[i])), pow));
            vd_mean = vd_mean + (Math.pow((Math.abs(obs[i] - mean_vd)), pow));
        }

        return 1 - (td_vd / vd_mean);
    }

    /** Calculates the efficiency between the log values of a test data set and a verification data set
     * after Nash & Sutcliffe (1970). The efficiency is described as the proportion of
     * the cumulated cubic deviation between both data sets and the cumulated cubic
     * deviation between the verification data set and its mean value. If either prediction or validation has a
     * value of <= 0 then the pair is ommited from the calculation and a message is put to system out.
     * @param sim the simulation data set
     * @param obs the validation (observed) data set
     * @param pow the power for the deviation terms
     * @return the calculated log_efficiency or -9999 if an error occurs
     */
    public static double nashSutcliffeLog(double[] obs, double[] sim, double pow) {
        sameArrayLen(obs, sim);
        int size = sim.length;

        double sum_log_pd = 0;
        double sum_log_vd = 0;

        /** calculating logarithmic values of both data sets. Sets 0 if data is 0 */
        double[] log_preData = new double[size];
        double[] log_valData = new double[size];

        int validPairs = 0;

        for (int i = 0; i < size; i++) {
            //either prediction or validation shows a value of zero
            //in this case the pair is excluded from the further calculation,
            //simply by setting the values to -1 and not increasing valid pairs
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

        /**summing up both data sets */
        for (int i = 0; i < size; i++) {
            if (log_preData[i] >= 0) {
                sum_log_pd = sum_log_pd + log_preData[i];
                sum_log_vd = sum_log_vd + log_valData[i];
            }
        }

        /** calculating mean values for both data sets */
        double mean_log_vd = sum_log_vd / validPairs;

        /** calculating mean pow deviations */
        double pd_log_vd = 0;
        double vd_log_mean = 0;
        for (int i = 0; i < size; i++) {
            if (log_preData[i] >= 0) {
                pd_log_vd = pd_log_vd + (Math.pow(Math.abs(log_valData[i] - log_preData[i]), pow));
                vd_log_mean = vd_log_mean + (Math.pow(Math.abs(log_valData[i] - mean_log_vd), pow));
            }
        }
        return 1 - (pd_log_vd / vd_log_mean);
    }

    /** Calculates the index of agreement (ioa) between a test data set and a verification data set
     * after Willmot & Wicks (1980). The ioa is described as the proportion of
     * the cumulated cubic deviation between both data sets and the squared sum of the absolute
     * deviations between the verification data set and the test mean value and the test data set and
     * its mean value.
     * @param sim the test Data set
     * @param obs the verification data set
     * @param pow the power
     * @return the calculated ioa or -9999 if an error occurs
     */
    public static double ioa(double[] obs, double[] sim, double pow) {
        sameArrayLen(obs, sim);
        int steps = sim.length;

        double sum_obs = 0;

        /**summing up both data sets */
        for (int i = 0; i < steps; i++) {
            sum_obs += obs[i];
        }

        /** calculating mean values for both data sets */
        double mean_obs = sum_obs / steps;

        /** calculating mean cubic deviations */
        /** calculating absolute squared sum of deviations from verification mean */
        double td_vd = 0;
        double abs_sqDevi = 0;
        for (int i = 0; i < steps; i++) {
            td_vd +=  (Math.pow((Math.abs(obs[i] - sim[i])), pow));
            abs_sqDevi +=  Math.pow(Math.abs(sim[i] - mean_obs) + Math.abs(obs[i] - mean_obs), pow);
        }
        return 1.0 - (td_vd / abs_sqDevi);
    }

    /**
     * Calcs coefficients of linear regression between x, y data
     * @param xData the independent data array (x)
     * @param yData the dependent data array (y)
     * @return (intercept, gradient, r?)
     */
    public static double[] linearReg(double[] xData, double[] yData) {
        sameArrayLen(xData, yData);
        double sumYValue = 0;
        double meanYValue = 0;
        double sumXValue = 0;
        double meanXValue = 0;
        double sumX = 0;
        double sumY = 0;
        double prod = 0;
        double NODATA = -9999;
        int nstat = xData.length;
        double[] regCoef = new double[3]; //(intercept, gradient, r?)
        int counter = 0;
        //calculating sums
        for (int i = 0; i < nstat; i++) {
            if ((yData[i] != NODATA) && (xData[i] != NODATA)) {
                sumYValue += yData[i];
                sumXValue += xData[i];
                counter++;
            }
        }
        //calculating means
        meanYValue = sumYValue / counter;
        meanXValue = sumXValue / counter;

        //calculating regression coefficients
        for (int i = 0; i < nstat; i++) {
            if ((yData[i] != NODATA) && (xData[i] != NODATA)) {
                sumX += Math.pow((xData[i] - meanXValue), 2);
                sumY += Math.pow((yData[i] - meanYValue), 2);
                prod += ((xData[i] - meanXValue) * (yData[i] - meanYValue));
            }
        }
        if (sumX > 0 && sumY > 0) {
            regCoef[1] = prod / sumX;  //gradient
            regCoef[0] = meanYValue - regCoef[1] * meanXValue; //intercept
            regCoef[2] = Math.pow((prod / Math.sqrt(sumX * sumY)), 2); //r?
        } else {
            regCoef[1] = 0;
            regCoef[0] = 0;
            regCoef[2] = 0;
        }
        return regCoef;
    }

    /**
     * 
     * @param prediction
     * @param validation
     * @return
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
     * 
     * @param prediction
     * @param validation
     * @return
     */
    public static double absVolumeError(double[] obs, double[] sim) {
        sameArrayLen(obs, sim);
        double volError = 0;
        for (int i = 0; i < sim.length; i++) {
            volError += (sim[i] - obs[i]);
        }
        return Math.abs(volError);
    }

    /**
     * 
     * @param prediction
     * @param validation
     * @return
     */
    public static double pbias(double[] obs, double[] sim) {
        sameArrayLen(obs, sim);
        double sumObs = 0;
        double sumDif = 0;
        for (int i = 0; i < sim.length; i++) {
            sumDif += (sim[i] - obs[i]);
            sumObs += obs[i];
        }
        return (sumDif / sumObs) * 100;
    }

    /**
     * 
     * @param prediction
     * @param validation
     * @return
     */
    public static double rmse(double[] obs, double[] sim) {
        sameArrayLen(obs, sim);
        double error = 0;
        for (int i = 0; i < sim.length; i++) {
            error += Math.pow((sim[i] - obs[i]), 2);
        }
        return Math.sqrt(error / sim.length);
    }

    /**
     * 
     * @param validation
     * @param prediction
     * @param missVal
     * @return
     */
    public static double absDiffLog(double[] obs, double[] sim) {
        sameArrayLen(obs, sim);
        int N = obs.length;
        double abs = 0;
        for (int i = 0; i < N; i++) {
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
        return abs;
    }

    /**
     * 
     * @param validation
     * @param prediction
     * @param missVal
     * @return
     */
    public static double absDiff(double[] obs, double[] sim) {
        sameArrayLen(obs, sim);
        int N = obs.length;
        double abs = 0;
        for (int i = 0; i < N; i++) {
            double measured = obs[i];
            if (measured == 0) {
                measured = 0.0000001;
            }
            abs += Math.abs((measured - sim[i]) / measured);
        }
        return abs;
    }

    /**
     * 
     * @param validation
     * @param prediction
     * @param missVal
     * @return
     */
    public static double pearsonsCorrelatrion(double[] obs, double[] sim) {
        sameArrayLen(obs, sim);
        double syy = 0.0, sxy = 0.0, sxx = 0.0, ay = 0.0, ax = 0.0;

        int n = 0;
        for (int j = 0; j < obs.length; j++) {
            ax += obs[j];
            ay += sim[j];
            n++;
        }
        if (n == 0) {
            throw new RuntimeException("Pearson's Correlation cannot be calculated due to no observed values");
        }
        ax = ax / ((double) n);
        ay = ay / ((double) n);
        for (int j = 0; j < obs.length; j++) {
            double xt = obs[j] - ax;
            double yt = sim[j] - ay;
            sxx += xt * xt;
            syy += yt * yt;
            sxy += xt * yt;
        }
        return sxy / Math.sqrt(sxx * syy);
    }

    /**
     * transformedRootMeanSquareError TRMSE
     * 
     * @param obs
     * @param sim
     * @return
     */
    public static double transformedRmse(double[] obs, double[] sim) {
        sameArrayLen(sim, obs);
        double error = 0;
        double z_pred = 0.;
        double z_val = 0.;
        for (int i = 0; i < sim.length; i++) {
            z_pred = (Math.pow((1.0 + sim[i]), 0.3) - 1.0) / 0.3;
            z_val = (Math.pow((1.0 + obs[i]), 0.3) - 1.0) / 0.3;
            error += (z_pred - z_val) * (z_pred - z_val);
        }
        return Math.sqrt(error / sim.length);
    }

    /** Runoff coefficient error ROCE
     *
     * @param obs
     * @param sim
     * @param precip
     * @return
     */
    public static double runoffCoefficientError(double[] obs, double[] sim, double[] precip) {
        sameArrayLen(sim, obs, precip);

        double mean_pred = Stats.mean(sim);
        double mean_val = Stats.mean(obs);
        double mean_ppt = Stats.mean(precip);
        double error = Math.abs((mean_pred / mean_ppt) - (mean_val / mean_ppt));
        return Math.sqrt(error);
    }
}
