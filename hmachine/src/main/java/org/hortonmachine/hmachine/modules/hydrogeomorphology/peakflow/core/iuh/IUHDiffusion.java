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

    private double[][] totalampidiffusion = null;

    private double tpmax = 0f;

    private double tstarmax = 0f;

    private double[][] ampisubsurface = null;
    private double[][] ampidiffsurface = null;

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
        double delta_sup = fixedParams.getDelta();
        double delta_sub = fixedParams.getDelta_sub();
        double vc = fixedParams.getVc();
        double prov = 0f;
        double dt = 0f;
        double tstar = 0f;
        double error = 100f;
        double area_tot = 0f;
        double area_sub = 0f;

        double[][] ampi_super = effectsBox.getAmpi();

        IUHDiffusionSurface iuhDiffSurface = new IUHDiffusionSurface(ampi_super, fixedParams, pm);
        ampidiffsurface = iuhDiffSurface.calculateIUH();

        if (effectsBox.ampi_subExists()) {
            area_sub = fixedParams.getArea_sub();
            double[][] ampi_help_sub = effectsBox.getAmpi_help_sub();

            IUHSubSurface iuhSubSurface = new IUHSubSurface(ampi_help_sub, fixedParams, pm);
            ampisubsurface = iuhSubSurface.calculateIUH();
        }

        double tcorr = ampidiffsurface[ampidiffsurface.length - 1][0];

        totalampidiffusion = calculateTotalDiffusion(ampidiffsurface, ampisubsurface, delta_sup, delta_sub, vc, tcorr, area_sub,
                area);

        /*
         * next calculates the maximum rain time
         */

        if (effectsBox.ampi_subExists()) {
            area_tot = area_sub + area;
        } else {
            area_tot = area;
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

            dt = ModelsEngine.henderson(totalampidiffusion, tp);
            tstar = tp + dt;
            if (tstar < tcorr) {
                prov = n_idf
                        - 1
                        + (tp * (double) ModelsEngine.width_interpolate(totalampidiffusion, tstar, 0, 1) / (area_tot * ((double) ModelsEngine
                                .width_interpolate(totalampidiffusion, tstar, 0, 2) - (double) ModelsEngine.width_interpolate(
                                totalampidiffusion, dt, 0, 2))));

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
     * 
     * @param ampidiffsurface
     * @param ampidiffsubsurface
     * @return
     */
    private double[][] calculateTotalDiffusion( double[][] ampidiffsurface, double[][] ampisubsurface, double delta_sup,
            double delta_sub, double vc, double tcorr, double area_sub, double area_super ) {

        double[][] totaldiff = null;

        if (ampisubsurface == null) {
            totaldiff = new double[ampidiffsurface.length][3];
            totaldiff = ampidiffsurface;

        } else {
            /*
             * calculate how many rows are in ampi_sub after ampi_sup has finished
             */
            int rowinampisubwhereampisupfinishes = 0;
            for( int i = 0; i < ampisubsurface.length; i++ ) {
                if (ampisubsurface[i][0] >= ampidiffsurface[ampidiffsurface.length - 1][0]) {
                    rowinampisubwhereampisupfinishes = i;
                    break;
                }
            }

            int totallength = ampidiffsurface.length + ampisubsurface.length - rowinampisubwhereampisupfinishes;

            totaldiff = new double[totallength][3];

            double intsub = 0f;
            double intsup = 0f;
            for( int i = 0; i < ampidiffsurface.length; i++ ) {
                totaldiff[i][0] = ampidiffsurface[i][0];
                intsub = (double) ModelsEngine.width_interpolate(ampisubsurface, ampidiffsurface[i][0], 0, 1);
                intsup = ampidiffsurface[i][1];
                if (isNovalue(intsub)) {
                    pm.errorMessage("Found undefined interpolated value for subsuperficial. Not summing it. Index: " + i);
                    totaldiff[i][1] = intsup;
                } else {
                    totaldiff[i][1] = intsup + intsub;

                }

            }
            for( int i = ampidiffsurface.length, j = rowinampisubwhereampisupfinishes; i < totallength; i++, j++ ) {
                totaldiff[i][0] = ampisubsurface[j][0];
                totaldiff[i][1] = ampisubsurface[j][1];
            }

            /*
             * calculation of the third column = cumulated The normalization occurs by means of the
             * superficial delta in the first part of the hydrogram, i.e. until the superficial
             * contributes, after that the delta is the one of the subsuperficial.
             */
            double cum = 0f;
            for( int i = 0; i < ampidiffsurface.length; i++ ) {
                cum = cum + (totaldiff[i][1] * delta_sup) / ((area_super + area_sub) * vc);
                totaldiff[i][2] = cum;
            }
            for( int i = ampidiffsurface.length, j = rowinampisubwhereampisupfinishes; i < totallength; i++, j++ ) {
                cum = cum + (totaldiff[i][1] * delta_sub) / ((area_super + area_sub) * vc);
                totaldiff[i][2] = cum;
            }
        }

        return totaldiff;
    }
    /*
     * (non-Javadoc)
     * @see bsh.commands.h.peakflow.iuh.IUHCalculator#calculateIUH()
     */
    public double[][] calculateIUH() {
        return totalampidiffusion;
    }

    /*
     * (non-Javadoc)
     * @see bsh.commands.h.peakflow.iuh.IUHCalculator#getTpMax()
     */
    public double getTpMax() {
        return tpmax;
    }

    /*
     * (non-Javadoc)
     * @see bsh.commands.h.peakflow.iuh.IUHCalculator#getTstarMax()
     */
    public double getTstarMax() {
        return tstarmax;
    }

    public double[][] getIUHSuperficial() {
        return ampidiffsurface;
    }

    public double[][] getIUHSubsuperficial() {
        return ampisubsurface;
    }

}