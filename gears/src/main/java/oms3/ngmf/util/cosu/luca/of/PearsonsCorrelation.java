/*
 * PearsonsCorrelation.java
 *
 * Created on February 13, 2007, 5:51 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu.luca.of;

import oms3.ObjectiveFunction;

/**
 *
 * @author Makiko, od
 */
public class PearsonsCorrelation implements ObjectiveFunction {

    @Override
    public double calculate(double[] obs, double[] sim, double missingValue)  {
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

    @Override
    public boolean positiveDirection() {
        return true;
    }
}
