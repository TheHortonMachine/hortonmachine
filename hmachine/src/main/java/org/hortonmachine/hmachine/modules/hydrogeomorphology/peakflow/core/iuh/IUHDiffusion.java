/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.iuh;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.EffectsBox;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;
/**
 * @author Andrea Antonello - www.hydrologis.com
 * @author Silvia Franceschi - www.hydrologis.com
 */
public class IUHDiffusion implements IUHCalculator {

    private double[][] totalAmpiDiffusion = null;

    private double tpmax = 0f;

    private double tstarmax = 0f;

    private double[][] iuhDiffusionSubSup = null;
    private double[][] iuhDiffusionSup = null;

    private final IHMProgressMonitor pm;

    /**
     * @param effectsBox
     * @param fixedParams
     * @param pm
     */
    public IUHDiffusion( EffectsBox effectsBox, ParameterBox fixedParams, IHMProgressMonitor pm ) {

        this.pm = pm;
        double n_idf = fixedParams.getN_idf();
        double area = fixedParams.getArea();
        double timestep = fixedParams.getTimestep();
        double deltaSup = fixedParams.getDelta();
        double deltaSubSup = fixedParams.getDelta_sub();
        double vc = fixedParams.getVc();
        double prov = 0f;
        double dt = 0f;
        double tstar = 0f;
        double error = 100f;
        double areaTot = 0f;
        double areaSub = 0f;

        double[][] ampiSuper = effectsBox.getAmpi();

        IUHSurface iuhDiffSurface = new IUHSurface(ampiSuper, fixedParams, pm);
        iuhDiffusionSup = iuhDiffSurface.calculateIUH();

        if (effectsBox.ampi_subExists()) {
            areaSub = fixedParams.getArea_sub();
            double[][] ampi_help_sub = effectsBox.getAmpi_sub();

            IUHSubSurface iuhSubSurface = new IUHSubSurface(ampi_help_sub, fixedParams, pm);
            iuhDiffusionSubSup = iuhSubSurface.calculateIUH();
        }

        double tcorr = iuhDiffusionSup[iuhDiffusionSup.length - 1][0];

        totalAmpiDiffusion = calculateTotalDiffusion(iuhDiffusionSup, iuhDiffusionSubSup, deltaSup, deltaSubSup, vc, tcorr,
                areaSub, area);

        /*
         * next calculates the maximum rain time
         */
        if (effectsBox.ampi_subExists()) {
            areaTot = areaSub + area;
        } else {
            areaTot = area;
        }

        /*
         * Skip the tpmax calculation if real rainfall data
         */
        if (effectsBox.rainDataExists()) {
            tpmax = 0f;
            return;
        }

        int index = 0;
        int threshold = (int) (tcorr / 100);
        pm.beginTask("IUH Diffusion...", (int) tcorr);
        for( int tp = 1; tp <= tcorr; tp += timestep ) {
            if (index > threshold) {
                index = 0;
            } else {
                index++;
            }

            dt = ModelsEngine.henderson(totalAmpiDiffusion, tp);
            tstar = tp + dt;
            if (tstar < tcorr) {
                prov = n_idf - 1
                        + (tp * (double) ModelsEngine.widthInterpolate(totalAmpiDiffusion, tstar, 0, 1)
                                / (areaTot * ((double) ModelsEngine.widthInterpolate(totalAmpiDiffusion, tstar, 0, 2)
                                        - (double) ModelsEngine.widthInterpolate(totalAmpiDiffusion, dt, 0, 2))));

                if (Math.abs(prov) < error) {
                    tpmax = tp;
                    tstarmax = tpmax + dt;
                    error = Math.abs(prov);
                }
            }
            pm.worked((int) timestep);
        }
        pm.done();

    }

    /**
     * Calculate the total IUH by summing the superficial and the subsuperficial IUH
     */
    private double[][] calculateTotalDiffusion( double[][] iuhDiffSup, double[][] iuhDiffSubSup, double deltaSup,
            double deltaSubSup, double channelVelocity, double tcorr, double areaSubSup, double areaSup ) {

        double[][] totalIuhDiff = null;

        if (iuhDiffSubSup == null) {
            totalIuhDiff = iuhDiffSup;
        } else {
            /*
             * calculate how many rows are in ampi_sub after ampi_sup has finished
             */
            int imstantInAmpiSubSupWhereAmpiSupFinishes = 0;
            for( int i = 0; i < iuhDiffSubSup.length; i++ ) {
                if (iuhDiffSubSup[i][0] >= iuhDiffSup[iuhDiffSup.length - 1][0]) {
                    imstantInAmpiSubSupWhereAmpiSupFinishes = i;
                    break;
                }
            }

            int totalLength = iuhDiffSup.length + iuhDiffSubSup.length - imstantInAmpiSubSupWhereAmpiSupFinishes;

            totalIuhDiff = new double[totalLength][3];

            for( int i = 0; i < iuhDiffSup.length; i++ ) {
                totalIuhDiff[i][0] = iuhDiffSup[i][0];
                double intSubSup = (double) ModelsEngine.widthInterpolate(iuhDiffSubSup, iuhDiffSup[i][0], 0, 1);
                double intSup = iuhDiffSup[i][1];
                if (isNovalue(intSubSup)) {
                    pm.errorMessage("Found undefined interpolated value for subsuperficial. Not summing it. Index: " + i);
                    totalIuhDiff[i][1] = intSup;
                } else {
                    totalIuhDiff[i][1] = intSup + intSubSup;

                }

            }
            for( int i = iuhDiffSup.length, j = imstantInAmpiSubSupWhereAmpiSupFinishes; i < totalLength; i++, j++ ) {
                totalIuhDiff[i][0] = iuhDiffSubSup[j][0];
                totalIuhDiff[i][1] = iuhDiffSubSup[j][1];
            }

            double totalDiffSum = 0;
            for( int i = 0; i < totalIuhDiff.length; i++ ) {
                totalDiffSum +=  totalIuhDiff[i][1];
            }
            double widthFunctionSum = 0;
            for( int i = 0; i < iuhDiffSup.length; i++ ) {
                widthFunctionSum +=  iuhDiffSup[i][1];
            }
            for( int i = 0; i < iuhDiffSubSup.length; i++ ) {
                widthFunctionSum +=  iuhDiffSubSup[i][1];
            }
            pm.message("Widthfunction sum = " + widthFunctionSum);
            pm.message("Total diff sum = " + totalDiffSum);
            
            /*
             * calculation of the third column = cumulated The normalization occurs by means of the
             * superficial delta in the first part of the hydrogram, i.e. until the superficial
             * contributes, after that the delta is the one of the subsuperficial.
             */
            double cum = 0f;
            for( int i = 0; i < iuhDiffSup.length; i++ ) {
                cum = cum + (totalIuhDiff[i][1] * deltaSup) / ((areaSup + areaSubSup) * channelVelocity);
                totalIuhDiff[i][2] = cum;
            }
            for( int i = iuhDiffSup.length; i < totalLength; i++ ) {
                cum = cum + (totalIuhDiff[i][1] * deltaSubSup) / ((areaSup + areaSubSup) * channelVelocity);
                totalIuhDiff[i][2] = cum;
            }
        }

        return totalIuhDiff;
    }

    public double[][] calculateIUH() {
        return totalAmpiDiffusion;
    }

    public double getTpMax() {
        return tpmax;
    }

    public double getTstarMax() {
        return tstarmax;
    }

    public double[][] getIUHSuperficial() {
        return iuhDiffusionSup;
    }

    public double[][] getIUHSubsuperficial() {
        return iuhDiffusionSubSup;
    }

}