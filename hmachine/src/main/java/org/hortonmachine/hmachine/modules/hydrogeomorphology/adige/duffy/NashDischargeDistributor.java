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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.duffy;

import static java.lang.Math.exp;
import static java.lang.Math.pow;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.gears.libs.modules.ModelsEngine;

/**
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NashDischargeDistributor extends ADischargeDistributor {

    private static final double N_THRESHOLD = 10.0;
    private double avgSup10;
    private double avgSup30;
    private double avgSup60;
    private double varSup10;
    private double varSup30;
    private double varSup60;
    private double avgSub;
    private double varSub;
    private double vSup;
    private double vSub;
    private long startDateMillis;
    private long endDateMillis;
    private double[] nashArraySub;
    private double[] nashArraySup10;
    private double[] nashArraySup30;
    private double[] nashArraySup60;
    private double[] currentSup;

    private long previousSuperficialTimeInMillis = -1;
    private long previousSubSuperficialTimeInMillis = -1;
    private double[] previousSuperficialContribution = null;
    private double[] previousSubSuperficialContribution = null;
    private int superficialArrayIndex;
    private int subSuperficialArrayIndex;

    public NashDischargeDistributor( long startDateMillis, long endDateMillis, long timeStepMillis,
            HashMap<Integer, Double> parameters ) {
        super(startDateMillis, endDateMillis, timeStepMillis, parameters);
        this.startDateMillis = startDateMillis;
        this.endDateMillis = endDateMillis;

        avgSup10 = parameters.get(ADischargeDistributor.PARAMS_AVG_SUP_10);
        avgSup30 = parameters.get(ADischargeDistributor.PARAMS_AVG_SUP_30);
        avgSup60 = parameters.get(ADischargeDistributor.PARAMS_AVG_SUP_60);
        varSup10 = parameters.get(ADischargeDistributor.PARAMS_VAR_SUP_10);
        varSup30 = parameters.get(ADischargeDistributor.PARAMS_VAR_SUP_30);
        varSup60 = parameters.get(ADischargeDistributor.PARAMS_VAR_SUP_60);
        avgSub = parameters.get(ADischargeDistributor.PARAMS_AVG_SUB);
        varSub = parameters.get(ADischargeDistributor.PARAMS_VAR_SUB);
        vSup = parameters.get(ADischargeDistributor.PARAMS_V_SUP);
        vSub = parameters.get(ADischargeDistributor.PARAMS_V_SUB);

        /*
         * valori fissi solo per test
         */
        avgSup10 = 3304.0;
        avgSup30 = 3603.0;
        avgSup60 = 20981.0;
        varSup10 = 2.41E6;
        varSup30 = 2.45E6;
        varSup60 = 2.26E8;
        avgSub = 50555.0;
        varSub = 1.47E9;

        double kSub = varSub / avgSub;
        double nSub = avgSub / kSub - 1.0;
        if (nSub < 0.0) {
            varSub = avgSub * avgSub;
        }
        if (nSub < N_THRESHOLD) {
            System.out.println("Nash subsuperficial...");
            nashArraySub = calculateNashDistribution(startDateMillis, endDateMillis,
                    timeStepMillis, avgSub, varSub, vSub);
        }
        double kSup = varSup10 / avgSup10;
        double nSup = avgSup10 / kSup - 1.0;
        if (nSup < 0.0) {
            varSup10 = avgSup10 * avgSup10;
        }
        if (nSup < N_THRESHOLD) {
            System.out.println("Nash superficial 10%...");
            nashArraySup10 = calculateNashDistribution(startDateMillis, endDateMillis,
                    timeStepMillis, avgSup10, varSup10, vSup);
        }
        kSup = varSup30 / avgSup30;
        nSup = avgSup30 / kSup - 1.0;
        if (nSup < 0.0) {
            varSup30 = avgSup30 * avgSup30;
        }
        if (nSup < N_THRESHOLD) {
            System.out.println("Nash superficial 30%...");
            nashArraySup30 = calculateNashDistribution(startDateMillis, endDateMillis,
                    timeStepMillis, avgSup30, varSup30, vSup);
        }
        kSup = varSup60 / avgSup60;
        nSup = avgSup60 / kSup - 1.0;
        if (nSup < 0.0) {
            varSup60 = avgSup60 * avgSup60;
        }
        if (nSup < N_THRESHOLD) {
            System.out.println("Nash superficial 60%...");
            nashArraySup60 = calculateNashDistribution(startDateMillis, endDateMillis,
                    timeStepMillis, avgSup60, varSup60, vSup);
        }

    }

    private double[] calculateNashDistribution( long startDateMillis, long endDateMillis,
            long timeStepMillis, double avg, double var, double v ) {
        double k = var / avg;
        double n = avg * avg / var;
        double sum = 0.0;
        double runningTime = 0.0;
        double timeStepHours = ((double) timeStepMillis) / 1000.0 / 3600.0;
        double endTime = ((double) (endDateMillis - startDateMillis - timeStepMillis)) / 1000.0 / 3600.0;

        double deltaD = timeStepHours * v * 3600.0;
        List<Double> nashList = new ArrayList<Double>();
        while( sum < 0.96 && runningTime <= endTime ) {
            // t is in hours
            double t = runningTime;
            // d is in meters, v is in meters/seconds
            double d = t * v * 3600.0;

            double nash = (1.0 / (k * ModelsEngine.gamma(n))) * pow(d / k, n - 1.0) * exp(-(d / k));
            double nashy = nash * deltaD;

            nashList.add(nashy);
            runningTime = runningTime + timeStepHours;
            sum = sum + nashy;
        }
        double[] nashArray = new double[nashList.size()];
        for( int i = 0; i < nashArray.length; i++ ) {
            nashArray[i] = nashList.get(i);
        }
        System.out.println("IUH calculated");
        return nashArray;
    }

    protected void distributeIncomingSuperficialDischarge( double superficialDischarge,
            double saturatedAreaPercentage, long currentTimeInMillis ) {
        double kSup;
        double nSup;
        double avg;
        if (saturatedAreaPercentage >= 0.0 && saturatedAreaPercentage <= 0.2) {
            avg = avgSup10;
            kSup = varSup10 / avgSup10;
            nSup = avgSup10 / kSup - 1.0;
            currentSup = nashArraySup10;
        } else if (saturatedAreaPercentage > 0.2 && saturatedAreaPercentage <= 0.5) {
            avg = avgSup30;
            kSup = varSup30 / avgSup30;
            nSup = avgSup30 / kSup - 1.0;
            currentSup = nashArraySup30;
        } else if (saturatedAreaPercentage > 0.5 && saturatedAreaPercentage <= 1.0) {
            avg = avgSup60;
            kSup = varSup60 / avgSup60;
            nSup = avgSup60 / kSup - 1.0;
            currentSup = nashArraySup60;
        } else {
            throw new IllegalArgumentException(
                    "The saturated area percentage has to be between 0 and 1. Current value is: "
                            + saturatedAreaPercentage);
        }

        if (nSup > N_THRESHOLD) {
            double t_seconds = avg / vSup;
            currentTimeInMillis = currentTimeInMillis + (long) t_seconds * 1000;
            if (currentTimeInMillis > endDateMillis - timeStepMillis) {
                currentTimeInMillis = endDateMillis - timeStepMillis;
            }
            int qArrayIndex = indexFromTimeInMillis(currentTimeInMillis);
            superficialDischargeArray[qArrayIndex] = superficialDischargeArray[qArrayIndex]
                    + superficialDischarge;
        } else {
            /*
             * If the current time is one of the global model timesteps,
             * we ADD the nash distributed discharge to the global 
             * discharge array (case A). In this case the index of the global array
             * has to depend on the global time steps, which is why it is calculated
             * only in this case.
             * 
             * The case B occurs when the model iterates inside a global timestep
             * to gain convergency on the discharge. In this case the discharge
             * will be SUBSTITUTED, which means that it first the cintribution of
             * the previous LOCAL timestep will be SUBTRACTED from the global array 
             * and then the current contribution will be ADDED at the same position
             * as defined in the initial superficialArrayIndex (i.e. folloing the
             * global timing mechanism).
             */
            double[] currentContribution = new double[currentSup.length];
            for( int i = 0; i < currentSup.length; i++ ) {
                currentContribution[i] = currentSup[i] * superficialDischarge;
            }
            int nashSize = currentSup.length;
            if (currentTimeInMillis == previousSuperficialTimeInMillis + timeStepMillis
                    || previousSuperficialTimeInMillis == -1) {
                // case A
                superficialArrayIndex = indexFromTimeInMillis(currentTimeInMillis);
                for( int i = 0; i < nashSize; i++ ) {
                    int j = superficialArrayIndex + i;
                    if (j > superficialDischargeArray.length - 1) {
                        break;
                    }
                    superficialDischargeArray[j] = superficialDischargeArray[j]
                            + currentContribution[i];
                }
                previousSuperficialTimeInMillis = currentTimeInMillis;
            } else {
                // case B
                for( int i = 0; i < nashSize; i++ ) {
                    int j = superficialArrayIndex + i;
                    if (j > superficialDischargeArray.length - 1) {
                        break;
                    }
                    if (i < previousSuperficialContribution.length) {
                        superficialDischargeArray[j] = superficialDischargeArray[j]
                                - previousSuperficialContribution[i];
                    }
                    if (i < currentContribution.length) {
                        superficialDischargeArray[j] = superficialDischargeArray[j]
                                + currentContribution[i];
                    }
                }
            }
            previousSuperficialContribution = currentContribution;
        }
    }

    protected void distributeIncomingSubSuperficialDischarge( double subSuperficialDischarge,
            double saturatedAreaPercentage, long currentTimeInMillis ) {
        double kSub = varSub / avgSub;
        double nSub = avgSub / kSub - 1.0;
        if (nSub < 0.0) {
            nSub = 0.0;
            kSub = avgSub;
            double var = avgSub * avgSub;
            nashArraySub = calculateNashDistribution(startDateMillis, endDateMillis,
                    timeStepMillis, avgSub, var, vSub);
        }

        if (nSub > N_THRESHOLD) {
            double t_seconds = avgSub / vSub;
            currentTimeInMillis = currentTimeInMillis + (long) t_seconds * 1000;
            if (currentTimeInMillis > endDateMillis - timeStepMillis) {
                currentTimeInMillis = endDateMillis - timeStepMillis;
            }
            int qArrayIndex = indexFromTimeInMillis(currentTimeInMillis);
            subSuperficialDischargeArray[qArrayIndex] = subSuperficialDischargeArray[qArrayIndex]
                    + subSuperficialDischarge;
        } else {
            /*
             * See comments about case A and B up in the superficial method. 
             */
            double[] currentContribution = new double[nashArraySub.length];
            for( int i = 0; i < nashArraySub.length; i++ ) {
                currentContribution[i] = nashArraySub[i] * subSuperficialDischarge;
            }
            int nashSize = nashArraySub.length;
            if (currentTimeInMillis == previousSubSuperficialTimeInMillis + timeStepMillis
                    || previousSubSuperficialTimeInMillis == -1) {
                subSuperficialArrayIndex = indexFromTimeInMillis(currentTimeInMillis);
                // case A
                for( int i = 0; i < nashSize; i++ ) {
                    int j = subSuperficialArrayIndex + i;
                    if (j > subSuperficialDischargeArray.length - 1) {
                        break;
                    }
                    subSuperficialDischargeArray[j] = subSuperficialDischargeArray[j]
                            + currentContribution[i];
                }
                previousSubSuperficialTimeInMillis = currentTimeInMillis;
            } else {
                // case B
                for( int i = 0; i < nashSize; i++ ) {
                    int j = subSuperficialArrayIndex + i;
                    if (j > subSuperficialDischargeArray.length - 1) {
                        break;
                    }
                    if (i < previousSubSuperficialContribution.length) {
                        subSuperficialDischargeArray[j] = subSuperficialDischargeArray[j]
                                - previousSubSuperficialContribution[i];
                    }
                    if (i < currentContribution.length) {
                        subSuperficialDischargeArray[j] = subSuperficialDischargeArray[j]
                                + currentContribution[i];
                    }
                }
            }
            previousSubSuperficialContribution = currentContribution;
        }
    }

    public static void main( String[] args ) throws Exception {
        SimpleDateFormat dF = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long startDate = dF.parse("2009-05-01 00:00").getTime();
        long endDate = dF.parse("2009-05-31 23:30").getTime();
        long timeStep = 1800000l;

        HashMap<Integer, Double> params = new HashMap<Integer, Double>();
        params.put(ADischargeDistributor.PARAMS_AVG_SUP_10, 14491.22);
        params.put(ADischargeDistributor.PARAMS_AVG_SUP_30, 14491.22);
        params.put(ADischargeDistributor.PARAMS_AVG_SUP_60, 14491.22);
        params.put(ADischargeDistributor.PARAMS_VAR_SUP_10, 34367480.0);
        params.put(ADischargeDistributor.PARAMS_VAR_SUP_30, 34367480.0);
        params.put(ADischargeDistributor.PARAMS_VAR_SUP_60, 34367480.0);
        params.put(ADischargeDistributor.PARAMS_AVG_SUB, 14491.22);
        params.put(ADischargeDistributor.PARAMS_VAR_SUB, 34367480.0);
        params.put(ADischargeDistributor.PARAMS_V_SUP, 2.0);
        params.put(ADischargeDistributor.PARAMS_V_SUB, 0.1);

        ADischargeDistributor dDistr = ADischargeDistributor.createDischargeDistributor(
                ADischargeDistributor.DISTRIBUTOR_TYPE_NASH, startDate, endDate, timeStep, params);

        double q = 100.0;

        long runningTime = startDate;
        while( runningTime < endDate ) {
            double newq = dDistr.calculateSuperficialDischarge(q, 0.15, runningTime);
            runningTime = runningTime + timeStep;
            System.out.println(newq);
        }

    }

}
