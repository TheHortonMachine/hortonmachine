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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.iuh;

import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.EffectsBox;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;

/**
 * @author Silvia Franceschi - www.hydrologis.com
 * @author Andrea Antonello - www.hydrologis.com
 */
public class IUHKinematic implements IUHCalculator {

    private double[][] ampiKinematic = null;

    private double[][] ampiSubSurface = null;

    private double[][] totalAmpiKinematic = null;

    private double tpmax = 0f;

    private double tstarmax = 0f;

    private double tstar = 0f;

    private double error = 100f;

    /**
     * @param effectsBox
     * @param fixedParams
     * @param out printstream for info purposes
     */
    public IUHKinematic( EffectsBox effectsBox, ParameterBox fixedParams, IHMProgressMonitor pm ) {

        double area = fixedParams.getArea();
        double area_sub = fixedParams.getArea_sub();
        double delta_sup = fixedParams.getDelta();
        double delta_sub = fixedParams.getDelta_sub();
        double vc = fixedParams.getVc();
        double timestep = fixedParams.getTimestep();
        double n_idf = fixedParams.getN_idf();

        double[][] ampi = effectsBox.getAmpi();
        double tcorr = ampi[ampi.length - 1][0];

        /*
         * Copy the ampi matrix in the ampikinematic because it represents the superficial
         * contribute in the kinematic algorithm.
         */
        ampiKinematic = new double[ampi.length][ampi[0].length];
        for( int i = 0; i < ampi.length; i++ ) {
            System.arraycopy(ampi[i], 0, ampiKinematic[i], 0, ampi[0].length);
        }

        if (effectsBox.ampi_subExists()) {
            area_sub = fixedParams.getArea_sub();
            double[][] ampi_help_sub = effectsBox.getAmpi_sub();

            IUHSubSurface iuhSubSurface = new IUHSubSurface(ampi_help_sub, fixedParams, pm);
            ampiSubSurface = iuhSubSurface.calculateIUH();
        }

        totalAmpiKinematic = calculateTotalKinematic(ampiKinematic, ampiSubSurface, delta_sup, delta_sub, vc, tcorr, area_sub,
                area);

        /*
         * solve the equation of henderson W(dt)=W(dt+tp). Calculate tpmax e tstar max for the
         * kinematic case.
         */
        // double area_tot = 0f;
        // if (effectsBox.ampi_subExists()) {
        // area_tot = area_sub + area;
        // } else {
        // area_tot = area;
        // }

        /*
         * Skip the tpmax calculation if real rainfall data
         */
        if (effectsBox.rainDataExists()) {
            tpmax = 0f;
            return;
        }

        double dt = 0f;
        double prov = 0f;

        int index = 0;
        int threshold = (int) (tcorr / 100);
        pm.beginTask("IUH kinematic...", (int) tcorr);
        for( int tp = 1; tp <= tcorr; tp += timestep ) {

            if (index > threshold) {
                index = 0;
            } else {
                index++;
            }
            dt = ModelsEngine.henderson(totalAmpiKinematic, tp);
            tstar = tp + dt;
            if (tstar < tcorr) {
                prov = n_idf
                        - 1
                        + (tp * (double) ModelsEngine.widthInterpolate(totalAmpiKinematic, tstar, 0, 1) / (area * ((double) ModelsEngine
                                .widthInterpolate(totalAmpiKinematic, tstar, 0, 2) - (double) ModelsEngine.widthInterpolate(
                                totalAmpiKinematic, dt, 0, 2))));

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
     * @param ampiKineSurface
     * @param ampiSubSurface
     * @param delta_sup
     * @param delta_sub
     * @param vc
     * @param tcorr
     * @param area_sub
     * @param area
     * @return
     */
    private double[][] calculateTotalKinematic( double[][] ampiKineSurface, double[][] ampiSubSurface, double delta_sup,
            double delta_sub, double vc, double tcorr, double area_sub, double area_super ) {

        double[][] totalKinematic = null;

        if (ampiSubSurface == null) {
            totalKinematic = new double[ampiKineSurface.length][3];
            totalKinematic = ampiKineSurface;
        } else {
            /*
             * calculate how many rows are in ampi_sub after ampi_sup has finished
             */
            int rowInAmpiSubSupWhereAmpiSupFinishes = 0;
            for( int i = 0; i < ampiSubSurface.length; i++ ) {
                if (ampiSubSurface[i][0] >= ampiKineSurface[ampiKineSurface.length - 1][0]) {
                    rowInAmpiSubSupWhereAmpiSupFinishes = i;
                    break;
                }
            }

            int totallength = ampiKineSurface.length + ampiSubSurface.length - rowInAmpiSubSupWhereAmpiSupFinishes;

            totalKinematic = new double[totallength][3];

            double intSubSup = 0f;
            double intSup = 0f;
            for( int i = 0; i < ampiKineSurface.length; i++ ) {
                totalKinematic[i][0] = ampiKineSurface[i][0];
                intSubSup = (double) ModelsEngine.widthInterpolate(ampiSubSurface, ampiKineSurface[i][0], 0, 1);
                intSup = ampiKineSurface[i][1];

                totalKinematic[i][1] = intSup + intSubSup;
            }
            for( int i = ampiKineSurface.length, j = rowInAmpiSubSupWhereAmpiSupFinishes; i < totallength; i++, j++ ) {
                totalKinematic[i][0] = ampiSubSurface[j][0];
                totalKinematic[i][1] = ampiSubSurface[j][1];
            }

            /*
             * calculation of the third column = cumulated The normalization occurs by means of the
             * superficial delta in the first part of the hydrogram, i.e. until the superficial
             * contributes, after that the delta is the one of the subsuperficial.
             */
            double cum = 0f;
            for( int i = 0; i < ampiKineSurface.length; i++ ) {
                cum = cum + (totalKinematic[i][1] * delta_sup) / ((area_super + area_sub) * vc);
                totalKinematic[i][2] = cum;
            }
            for( int i = ampiKineSurface.length; i < totallength; i++ ) {
                cum = cum + (totalKinematic[i][1] * delta_sub) / ((area_super + area_sub) * vc);
                totalKinematic[i][2] = cum;
            }
        }

        return totalKinematic;
    }

    public double[][] calculateIUH() {
        return totalAmpiKinematic;
    }

    public double getTpMax() {
        return tpmax;
    }

    public double getTstarMax() {
        return tstarmax;
    }

    public double[][] getIUHSuperficial() {
        return ampiKinematic;
    }

    public double[][] getIUHSubsuperficial() {
        return ampiSubSurface;
    }

}