/*
 * AbsoluteDifference.java
 *
 * Created on January 28, 2007, 10:42 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu.luca.of;

import oms3.ObjectiveFunction;

/**
 *
 * @author Makiko
 */
public class AbsoluteDifference implements ObjectiveFunction {

    @Override
    public double calculate(double[] obs, double[] sim, double missingValue) {
        int N = Math.min(obs.length, sim.length);
        double abs = 0;
        for (int i = 0; i < N; i++) {
            if (obs[i] > missingValue) {
                double measured = obs[i];
                if (measured == 0) {
                    measured = 0.0000001;
                }
                abs += Math.abs((measured - sim[i]) / measured);
            }
        }
        return abs;
    }

    @Override
    public boolean positiveDirection() {
        return false;
    }
}
