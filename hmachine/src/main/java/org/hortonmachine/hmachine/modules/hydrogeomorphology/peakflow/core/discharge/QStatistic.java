/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.discharge;

import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.iuh.IUHCalculator;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.jeff.StatisticJeff;

/**
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvano Pisoni
 */
public class QStatistic implements DischargeCalculator {

    private ParameterBox fixedParams = null;

    private IUHCalculator iuhC = null;

    private double tpmax = 0f;

    private double J = 0f;

    private double h = 0f;

    private double[][] ampidiff = null;

    private final IHMProgressMonitor pm;

    double[][] volumeCheck = null;

    /**
     * This class calculates maximum discharge and discharge.
     * 
     * @param fixedParameters - set of initial parameters
     * @param iuhC - abstraction of the iuh calculator
     * @param jeffC - abstraction of the jeff calculator
     * @param pm
     */
    public QStatistic( ParameterBox fixedParameters, IUHCalculator iuhC, StatisticJeff jeffC, IHMProgressMonitor pm ) {
        this.fixedParams = fixedParameters;

        this.iuhC = iuhC;
        this.pm = pm;

        double[][] jeff = jeffC.calculateJeff();
        J = jeff[0][0];
        h = jeff[0][1];
        tpmax = iuhC.getTpMax();

        ampidiff = iuhC.calculateIUH();
    }

    public double calculateQmax() {

        double area_super = fixedParams.getArea();
        double area_sub = fixedParams.getArea_sub();
        double area_tot = 0f;

        /* if (effectsBox.containsKey("ampi_sub")) */
        if (area_sub != 0) {
            area_tot = area_sub + area_super;
        } else {
            area_tot = area_super;
        }

        double qmax = (double) (J * area_tot * (ModelsEngine.widthInterpolate(ampidiff, iuhC.getTstarMax(), 0, 2)
                - ModelsEngine.widthInterpolate(ampidiff, iuhC.getTstarMax() - tpmax, 0, 2)));

        return qmax;
    }

    public double[][] calculateQ() {
        double timestep = fixedParams.getTimestep();
        double area_super = fixedParams.getArea();
        double area_sub = fixedParams.getArea_sub();
        double area_tot = 0f;

        double tcorr = ampidiff[ampidiff.length - 1][0];
        double[][] Q = new double[(int) Math.floor((tcorr + tpmax) / timestep) + 1][4];
        volumeCheck = new double[(int) Math.floor((tcorr + tpmax) / timestep) + 1][2];

        if (area_sub != -9999.0) {
            area_tot = area_sub + area_super;
        } else {
            area_tot = area_super;
        }

        /*
         * calculate the discharge for t < tcorr
         */
        int j = 0;
        pm.beginTask("Calculating discharge for t < tcorr...", (int) tcorr);
        for( int t = 1; t < tcorr; t += timestep ) {
            j = (int) Math.floor((t) / timestep);

            if (t <= tpmax) {
                Q[j][0] = t;
                Q[j][1] = (double) (J * area_tot * ModelsEngine.widthInterpolate(ampidiff, t, 0, 2));
                Q[j][2] = Q[j - 1][2] + Q[j][1];
                Q[j][3] = h;
            } else {
                Q[j][0] = t;
                Q[j][1] = (double) (J * area_tot * (ModelsEngine.widthInterpolate(ampidiff, t, 0, 2)
                        - ModelsEngine.widthInterpolate(ampidiff, t - tpmax, 0, 2)));
                Q[j][2] = Q[j - 1][2] + Q[j][1];
                Q[j][3] = 0.0;
            }

            double diffJ = (area_tot * J - Q[j][2]) * timestep;
            volumeCheck[j][0] = Q[j][0];
            volumeCheck[j][1] = diffJ;

            pm.worked((int) timestep);
        }
        pm.done();

        /*
         * calculate the discharge for t > tcorr
         */
        pm.beginTask("Calculating discharge for t > tcorr...", (int) tpmax);
        for( double t = tcorr; t < (tcorr + tpmax); t += timestep ) {
            j = (int) Math.floor(((int) t) / timestep);
            Q[j][0] = t;
            Q[j][1] = (double) (J * area_tot
                    * (ampidiff[ampidiff.length - 1][2] - ModelsEngine.widthInterpolate(ampidiff, t - tpmax, 0, 2)));
            Q[j][2] = Q[j - 1][2] + Q[j][1];
            Q[j][3] = 0.0;

            double diffJ = (area_tot * J - Q[j][2]) * timestep;
            volumeCheck[j][0] = Q[j][0];
            volumeCheck[j][1] = diffJ;

            pm.worked((int) timestep);
        }
        pm.done();

        /*
         * calculate the volumes
         */
        // double vol = Q[Q.length - 2][2] * timestep;
        // double vol2 = (double) (area_tot * h / 1000);

        return Q;
    }

    public double[][] getVolumeCheck() {
        return volumeCheck;
    }

    /*
     * (non-Javadoc)
     * @see bsh.commands.h.peakflow.discharge.DischargeCalculator#getTpMax()
     */
    public double getTpMax() {
        return tpmax;
    }

}