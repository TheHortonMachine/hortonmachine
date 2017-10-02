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

import java.util.HashMap;

/**
 * Class taking care of the distribution of discharge to gain delayed release. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public abstract class ADischargeDistributor {
    /**
     * Identifier for usage of Nash distribution.
     */
    public static final int DISTRIBUTOR_TYPE_NASH = 0;

    /*
     * Parameters of mean and variance considering saturation.
     */
    public static final int PARAMS_AVG_SUP_10 = 0;
    public static final int PARAMS_AVG_SUP_30 = 1;
    public static final int PARAMS_AVG_SUP_60 = 2;
    public static final int PARAMS_VAR_SUP_10 = 3;
    public static final int PARAMS_VAR_SUP_30 = 4;
    public static final int PARAMS_VAR_SUP_60 = 5;
    public static final int PARAMS_AVG_SUB = 6;
    public static final int PARAMS_VAR_SUB = 7;
    public static final int PARAMS_V_SUP = 8;
    public static final int PARAMS_V_SUB = 9;

    protected double[] subSuperficialDischargeArray;
    protected double[] superficialDischargeArray;
    protected final long startDateMillis;
    protected final long timeStepMillis;
    protected final HashMap<Integer, Double> parameters;

    /**
     * Creates a discharge distributor called from the extending class.
     * 
     * @param startDateMillis see {@link ADischargeDistributor#createDischargeDistributor(int, long, long, long)} doc.
     * @param endDateMillis see {@link ADischargeDistributor#createDischargeDistributor(int, long, long, long)} doc.
     * @param timeStepMillis see {@link ADischargeDistributor#createDischargeDistributor(int, long, long, long)} doc.
     * @param parameters see {@link ADischargeDistributor#createDischargeDistributor(int, long, long, long)} doc.
     */
    protected ADischargeDistributor( long startDateMillis, long endDateMillis, long timeStepMillis,
            HashMap<Integer, Double> parameters ) {
        this.startDateMillis = startDateMillis;
        this.timeStepMillis = timeStepMillis;
        this.parameters = parameters;
        long intervals = (endDateMillis - startDateMillis) / timeStepMillis + 1;
        subSuperficialDischargeArray = new double[(int) intervals];
        superficialDischargeArray = new double[(int) intervals];
    }

    /**
     * Creates a {@link ADischargeDistributor discharge distributor}.
     * 
     * @param distributorType defines the type to be used. Possible values are:
     *              <ul>
     *              <li>{@link ADischargeDistributor#DISTRIBUTOR_TYPE_NASH NASH: 0}</li>
     *              </ul>
     * @param startDateMillis the start time used to define the complete time horizont.
     * @param endDateMillis the end time used to define the complete time horizont.
     * @param timeStepMillis the time step used to define the complete time horizont.
     * @param parameters a {@link HashMap map of parameters} to be used for the 
     *              distribution model. Supported values are:
     *              <ul>
     *              <li>{@link ADischargeDistributor#PARAMS_AVG_SUP_10}: the mean 
     *              of the width function for 10% of saturated areas
     *              for the superficial flow case.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_AVG_SUP_30}: the mean 
     *              of the width function for 30% of saturated areas
     *              for the superficial flow case.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_AVG_SUP_60}: the mean 
     *              of the width function for 60% of saturated areas
     *              for the superficial flow case.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_VAR_SUP_10}: the variance 
     *              of the width function for 10% of saturated areas
     *              for the superficial flow case.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_VAR_SUP_30}: the variance 
     *              of the width function for 30% of saturated areas
     *              for the superficial flow case.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_VAR_SUP_60}: the variance 
     *              of the width function for 60% of saturated areas
     *              for the superficial flow case.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_AVG_SUB}: the mean 
     *              of the width function for the subsuperficial flow case.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_VAR_SUB}: the variance 
     *              of the width function for the subsuperficial flow case.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_V_SUP}: the 
     *              speed of the superficial flow.</li>
     *              <li>{@link ADischargeDistributor#PARAMS_V_SUB}: the 
     *              speed of the subsuperficial flow.</li>
     *              </ul>
     *
     * @return the created discharge distributor.
     */
    public static ADischargeDistributor createDischargeDistributor( int distributorType,
            long startDateMillis, long endDateMillis, long timeStepMillis,
            HashMap<Integer, Double> parameters ) {
        if (distributorType == DISTRIBUTOR_TYPE_NASH) {
            return new NashDischargeDistributor(startDateMillis, endDateMillis, timeStepMillis,
                    parameters);
        } else {
            throw new IllegalArgumentException("No such distribution model available.");
        }
    }

    /**
     * Calculates the current superficial discharge.
     * 
     * <p>
     * The discharge takes into account the distribution
     * of all the distributed discharge contributions
     * in the prior timesteps. 
     * </p>
     * 
     * @param superficialDischarge the non distributed discharge value.
     * @param saturatedAreaPercentage the percentage of saturated area.
     * @param timeInMillis the current timestep.
     * @return the calculated discharge.
     */
    public double calculateSuperficialDischarge( double superficialDischarge,
            double saturatedAreaPercentage, long timeInMillis ) {
        distributeIncomingSuperficialDischarge(superficialDischarge, saturatedAreaPercentage,
                timeInMillis);
        return superficialDischargeArray[indexFromTimeInMillis(timeInMillis)];
    }

    /**
     * Calculates the current subsuperficial discharge.
     * 
     * <p>
     * The discharge takes into account the distribution
     * of all the distributed discharge contributions
     * in the prior timesteps. 
     * </p>
     * 
     * @param subSuperficialDischarge the non distributed discharge value.
     * @param saturatedAreaPercentage the percentage of saturated area.
     * @param timeInMillis the current timestep.
     * @return the calculated discharge.
     */
    public double calculateSubsuperficialDischarge( double subSuperficialDischarge,
            double saturatedAreaPercentage, long timeInMillis ) {
        distributeIncomingSubSuperficialDischarge(subSuperficialDischarge, saturatedAreaPercentage,
                timeInMillis);
        return subSuperficialDischargeArray[indexFromTimeInMillis(timeInMillis)];
    }

    /**
     * Get the discharge array index for the current time.
     * 
     * @param currentTimeInMillis the current time in milliseconds.
     * @return the index that can be used in the discharge arrays.
     */
    protected int indexFromTimeInMillis( long currentTimeInMillis ) {
        int index = (int) ((currentTimeInMillis - startDateMillis) / timeStepMillis);
        return index;
    }

    /**
     * The method that applies the distribution method to the incoming discharge.
     * 
     * <p><b>NOTE</b>: this is for the superficial case.</p>
     * 
     * @param ssuperficialDischarge the non distributed discharge value. 
     * @param saturatedAreaPercentage the percentage of saturated area.
     * @param currentTimeInMillis the current timestep.
     */
    protected abstract void distributeIncomingSuperficialDischarge( double superficialDischarge,
            double saturatedAreaPercentage, long currentTimeInMillis );

    /**
     * The method that applies the distribution method to the incoming discharge.
     * 
     * <p><b>NOTE</b>: this is for the subsuperficial case.</p>
     * 
     * @param subSuperficialDischarge the non distributed discharge value. 
     * @param saturatedAreaPercentage the percentage of saturated area.
     * @param currentTimeInMillis the current timestep.
     */
    protected abstract void distributeIncomingSubSuperficialDischarge(
            double subSuperficialDischarge, double saturatedAreaPercentage, long currentTimeInMillis );

}