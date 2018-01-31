/*
 * AbsoluteDifferenceLog.java
 *
 * Created on January 28, 2007, 10:43 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu.luca.of;

import oms3.ObjectiveFunction;

/**
 */
public class AbsoluteDifferenceLog implements ObjectiveFunction {

    @Override
    public double calculate(double[] obs, double[] sim, double missingValue) {
        int N = Math.min(obs.length, sim.length);
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

    @Override
    public boolean positiveDirection() {
        return false;
    }
}
