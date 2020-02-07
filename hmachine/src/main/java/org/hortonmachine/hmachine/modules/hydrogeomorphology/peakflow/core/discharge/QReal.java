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

import java.util.Map;
import java.util.Set;

import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.iuh.IUHCalculator;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.jeff.RealJeff;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * @author Silvia franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class QReal implements DischargeCalculator {

    private RealJeff jeffC = null;

    private Map<DateTime, Double> jeff = null;

    private double[][] ampi = null;

    private double tpmax = 0f;

    private ParameterBox fixedParams = null;

    private double[][] Qtot = null;

    private final IHMProgressMonitor pm;

    double[][] volumeCheck = null;

    /**
     * Calculate the discharge with rainfall data
     */
    public QReal( ParameterBox fixedParameters, IUHCalculator iuhC, RealJeff jeffC, IHMProgressMonitor pm ) {
        this.jeffC = jeffC;

        this.fixedParams = fixedParameters;
        this.pm = pm;

        jeff = jeffC.calculateJeff();
        ampi = iuhC.calculateIUH();
    }

    /**
     * Calculate the discharge with rainfall data.
     */
    // public QReal( ParameterBox fixedParameters, double[][] iuhdata, double[][] jeffdata ) {
    // fixedParams = fixedParameters;
    //
    // jeff = jeffdata;
    // ampi = iuhdata;
    //
    // System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAA: ampilength " + ampi[ampi.length - 1][0]);
    // }

    /*
     * (non-Javadoc)
     * @see bsh.commands.h.peakflow.core.discharge.DischargeCalculator#calculateQ()
     */
    public double[][] calculateQ() {
        double timestep = fixedParams.getTimestep();
        double area_super = fixedParams.getArea();
        double area_sub = fixedParams.getArea_sub();
        double area_tot = 0f;

        double raintimestep = jeffC.getRain_timestep();
        DateTime firstDate = jeffC.getFirstDate();

        /*
         * The maximum rain time has no sense with the real precipitations. In this case it will use
         * the rain timestep for tp.
         */
        double tcorr = ampi[ampi.length - 1][0];
        tpmax = (double) raintimestep;
        int rainLength = jeff.size();
        double[][] totalQshiftMatrix = new double[rainLength][(int) (Math.floor((tcorr + tpmax) / timestep) + 1
                + rainLength * raintimestep / timestep)];
        double[][] Q = new double[(int) Math.floor((tcorr + tpmax) / timestep) + 1][3];
        volumeCheck = new double[(int) Math.floor((tcorr + tpmax) / timestep) + 1][2];

        if (area_sub != -9999.0) {
            area_tot = area_sub + area_super;
        } else {
            area_tot = area_super;
        }

        Set<DateTime> dates = jeff.keySet();
        pm.beginTask("Calculating discharge...", dates.size());
        int i = 0;
        for( DateTime dateTime : dates ) {
            double J = jeff.get(dateTime);
            /*
             * calculate the discharge for t < tcorr
             */
            int j = 0;
            for( int t = 1; t < tcorr; t += timestep ) {
                j = (int) Math.floor((t) / timestep);

                if (t <= tpmax) {
                    Q[j][0] = t;
                    double widthInterpolate = ModelsEngine.widthInterpolate(ampi, t, 0, 2);
                    Q[j][1] = (double) (J * area_tot * widthInterpolate);
                    Q[j][2] = Q[j - 1][2] + Q[j][1];
                } else {
                    Q[j][0] = t;
                    Q[j][1] = (double) (J * area_tot * (ModelsEngine.widthInterpolate(ampi, t, 0, 2)
                            - ModelsEngine.widthInterpolate(ampi, t - tpmax, 0, 2)));
                    Q[j][2] = Q[j - 1][2] + Q[j][1];
                }
                double diffJ = (area_tot * J - Q[j][1]) * timestep;
                volumeCheck[j][0] = Q[j][0];
                volumeCheck[j][1] = diffJ;
            }

            /*
             * calculate the discharge for t > tcorr
             */
            for( double t = tcorr; t < (tcorr + tpmax); t += timestep ) {
                j = (int) Math.floor(((int) t) / timestep);
                Q[j][0] = t;
                Q[j][1] = (double) (J * area_tot
                        * (ampi[ampi.length - 1][2] - ModelsEngine.widthInterpolate(ampi, t - tpmax, 0, 2)));
                Q[j][2] = Q[j - 1][2] + Q[j][1];

                double diffJ = (area_tot * J - Q[j][1]) * timestep;
                volumeCheck[j][0] = Q[j][0];
                volumeCheck[j][1] = diffJ;
            }

            /*
             * calculate the volumes
             */
            // double vol = Q[Q.length - 2][2] * timestep;
            // double vol2 = (double) (area_tot * J * raintimestep);

            /*
             * calculate zero padding before first value Note that jeff contains already the
             * progressive time of the rainfile.
             */
            int totalshiftmatrixindex = 0;
            int initalshiftmatrixindex = 0;
            // FIXME time in ???
            Duration duration = new Duration(firstDate, dateTime);
            long intervalSeconds = duration.getStandardSeconds();

            int paddingnumber = (int) (intervalSeconds / timestep);
            for( int m = 0; m < paddingnumber; m++ ) {
                totalQshiftMatrix[i][m] = 0;
                totalshiftmatrixindex++;
            }
            initalshiftmatrixindex = totalshiftmatrixindex;
            for( int k = initalshiftmatrixindex; k < Q.length + initalshiftmatrixindex; k++ ) {
                totalQshiftMatrix[i][k] = Q[k - initalshiftmatrixindex][1];
                totalshiftmatrixindex++;
            }
            for( int k = Q.length + totalshiftmatrixindex; k < totalQshiftMatrix[0].length; k++ ) {
                totalQshiftMatrix[i][k] = 0;
            }
            i++;
            pm.worked(1);
        }
        pm.done();

        /*
         * sum the discharge contributes
         */
        Qtot = new double[totalQshiftMatrix[0].length][2];
        double tottime = 0f;
        for( int k = 0; k < Qtot.length; k++ ) {
            double sum = 0f;
            for( int j = 0; j < totalQshiftMatrix.length; j++ ) {
                sum = sum + totalQshiftMatrix[j][k];
            }

            tottime = tottime + timestep;

            Qtot[k][1] = sum;
            Qtot[k][0] = tottime;
        }

        double total_vol = 0f;
        for( int k = 0; k < Qtot.length; k++ ) {
            total_vol = total_vol + Qtot[k][1];
        }
        double total_rain = 0.0;
        for( DateTime dateTime : dates ) {
            double J = jeff.get(dateTime);
            total_rain = total_rain + J;
        }
        total_rain = total_rain * area_tot * raintimestep;

        return Qtot;
    }

    public double[][] getVolumeCheck() {
        return volumeCheck;
    }

    /*
     * (non-Javadoc)
     * @see bsh.commands.h.peakflow.core.discharge.DischargeCalculator#calculateQmax()
     */
    public double calculateQmax() {
        if (Qtot == null) {
            calculateQ();
        }

        double qmax = 0f;
        for( int i = 0; i < Qtot.length; i++ ) {
            if (Qtot[i][1] > qmax)
                qmax = Qtot[i][1];
        }

        return qmax;
    }

    /*
     * (non-Javadoc)
     * @see bsh.commands.h.peakflow.core.discharge.DischargeCalculator#getTpMax()
     */
    public double getTpMax() {
        return tpmax;
    }

}