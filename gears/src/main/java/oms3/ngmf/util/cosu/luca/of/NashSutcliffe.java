/*
 * NashSutclitte.java
 *
 * Created on January 28, 2007, 10:29 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package oms3.ngmf.util.cosu.luca.of;

import oms3.ObjectiveFunction;

/**
 *
 */
public class NashSutcliffe implements ObjectiveFunction {

    @Override
    public boolean positiveDirection() {
        return true;
    }

    @Override
    public double calculate(double[] obs, double[] sim, double missing)  {
        return 1 - NormalizedRMSE.calc(obs, sim, missing);
    }
}
