/*
 * NormalizedRootMeanSquareError.java
 *
 * Created on January 28, 2007, 10:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu.luca.of;

import oms3.ObjectiveFunction;

/**
 */
public class NormalizedRMSE implements ObjectiveFunction {

    @Override
    public boolean positiveDirection() {
        return false;
    }

    @Override
    public double calculate(double[] obs, double[] sim, double missing) {
        return calc(obs, sim, missing);
    }

    static double calc(double[] obs, double[] sim, double missing) {
        double measuredMean = mean(obs, missing);
        int N = Math.min(obs.length, sim.length);
        double numerator = 0, denominator = 0;
        for (int i = 0; i < N; i++) {
            if (obs[i] > missing) {
                numerator += (obs[i] - sim[i]) * (obs[i] - sim[i]);
                denominator += (obs[i] - measuredMean) * (obs[i] - measuredMean);
            }
        }
        if (denominator == 0) {
            throw new RuntimeException("Error: The denominator is 0.\n" +
                    "This happens if all observed values are equal to their mean.");
        }
        return Math.sqrt(numerator / denominator);
    }

    static double mean(double[] array, double missing) {
        double sum = 0, size = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > missing) {
                sum += array[i];
                size++;
            }
        }
        return sum / size;
    }
}
