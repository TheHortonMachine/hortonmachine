/*
CUENCAS is a River Network Oriented GIS
Copyright (C) 2005  Ricardo Mantilla

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/*
 * RKF.java
 *
 * Created on November 11, 2001, 10:23 AM
 */

package org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.duffy;

import java.io.IOException;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.adige.OmsAdige;
import org.joda.time.DateTime;

/**
 * An implementation of the Runge-Kutta-Felberg algorithm for solving non-linear ordinary
 * differential equations. It uses a time step control algorithm to avoid numerical errors while
 * solving the equations
 * 
 * @author Ricardo Mantilla
 */
public class RungeKuttaFelberg {

    private DuffyModel duffy;
    /**
     * An array containing the value of the function that was last calculated by the RKF algoritm
     */
    private double[] finalCond;
    private double epsilon;
    private double basicTimeStepInMinutes = 10. / 60.;
    // private double[] a = {0., 1. / 5., 3. / 10., 3. / 5., 1., 7. / 8.};
    private double[][] b = {{0.}, {1. / 5.}, {3. / 40., 9. / 40.}, {3. / 10., -9. / 10., 6. / 5.},
            {-11. / 54., 5. / 2., -70. / 27., 35. / 27.},
            {1631. / 55296., 175. / 512., 575. / 13824., 44275. / 110592., 253. / 4096.}};

    private double[] c = {37. / 378., 0., 250. / 621., 125. / 594., 0., 512. / 1771.};
    private double[] cStar = {2825. / 27648., 0., 18575. / 48384., 13525. / 55296., 277. / 14336., 1. / 4.};

    private final boolean doLog;

    private boolean isAtFinalSubtimestep = true;
    private IHMProgressMonitor outputStream;

    /**
     * Creates new RKF
     * 
     * @param fu The differential equation to solve described by a {@link IBasicFunction}
     * @param eps The value error allowed by the step forward algorithm
     * @param basTs The step size
     * @param doLog
     */
    public RungeKuttaFelberg( DuffyModel fu, double eps, double basTs, IHMProgressMonitor out, boolean doLog ) {
        duffy = fu;
        epsilon = eps;
        basicTimeStepInMinutes = basTs;
        this.outputStream = out;
        this.doLog = doLog;
    }

    /**
     * Returns the value of the function described by differential equations in the next time step
     * 
     * @param currentTimeInMinutes The current time
     * @param initialConditions The value of the initial condition
     * @param timeStepInMinutes The desired step size
     * @param finalize A boolean indicating in the timeStep provided is final or if it needs to be
     *        refined
     * @param currentSolution
     * @param rainArray
     * @param etpArray
     */
    private void step( double currentTimeInMinutes, double[] initialConditions, double timeStepInMinutes, boolean finalize,
            CurrentTimestepSolution currentSolution, double[] rainArray, double[] etpArray ) {

        double[] carrier = new double[initialConditions.length];

        double[] k0 = duffy.eval(currentTimeInMinutes, initialConditions, rainArray, etpArray, false);
        for( int i = 0; i < initialConditions.length; i++ )
            carrier[i] = Math.max(0, initialConditions[i] + timeStepInMinutes * b[1][0] * k0[i]);

        double[] k1 = duffy.eval(currentTimeInMinutes, carrier, rainArray, etpArray, false);
        for( int i = 0; i < initialConditions.length; i++ )
            carrier[i] = Math.max(0, initialConditions[i] + timeStepInMinutes * (b[2][0] * k0[i] + b[2][1] * k1[i]));

        double[] k2 = duffy.eval(currentTimeInMinutes, carrier, rainArray, etpArray, false);
        for( int i = 0; i < initialConditions.length; i++ )
            carrier[i] = Math.max(0, initialConditions[i] + timeStepInMinutes
                    * (b[3][0] * k0[i] + b[3][1] * k1[i] + b[3][2] * k2[i]));

        double[] k3 = duffy.eval(currentTimeInMinutes, carrier, rainArray, etpArray, false);
        for( int i = 0; i < initialConditions.length; i++ )
            carrier[i] = Math.max(0, initialConditions[i] + timeStepInMinutes
                    * (b[4][0] * k0[i] + b[4][1] * k1[i] + b[4][2] * k2[i] + b[4][3] * k3[i]));

        double[] k4 = duffy.eval(currentTimeInMinutes, carrier, rainArray, etpArray, false);
        for( int i = 0; i < initialConditions.length; i++ )
            carrier[i] = Math.max(0, initialConditions[i] + timeStepInMinutes
                    * (b[5][0] * k0[i] + b[5][1] * k1[i] + b[5][2] * k2[i] + b[5][3] * k3[i] + b[5][4] * k4[i]));

        double[] k5 = duffy.eval(currentTimeInMinutes, carrier, rainArray, etpArray, isAtFinalSubtimestep);

        double[] newY = new double[initialConditions.length];
        for( int i = 0; i < initialConditions.length; i++ ) {
            newY[i] = initialConditions[i] + timeStepInMinutes
                    * (c[0] * k0[i] + c[1] * k1[i] + c[2] * k2[i] + c[3] * k3[i] + c[4] * k4[i] + c[5] * k5[i]);
            newY[i] = Math.max(0, newY[i]);
            if (Double.isInfinite(newY[i]) || newY[i] != newY[i]) {
                throw new ModelsIllegalargumentException("An error occurred during the integration procedure.", this);
            }
        }

        double[] newYstar = new double[initialConditions.length];
        for( int i = 0; i < initialConditions.length; i++ ) {
            newYstar[i] = initialConditions[i]
                    + timeStepInMinutes
                    * (cStar[0] * k0[i] + cStar[1] * k1[i] + cStar[2] * k2[i] + cStar[3] * k3[i] + cStar[4] * k4[i] + cStar[5]
                            * k5[i]);
            newYstar[i] = Math.max(0, newYstar[i]);
            if (Double.isInfinite(newYstar[i]) || newYstar[i] != newYstar[i]) {
                throw new ModelsIllegalargumentException("An error occurred during the integration procedure.", this);
            }
        }

        double delta = 0;
        for( int i = 0; i < initialConditions.length; i++ ) {
            if ((newY[i] + newYstar[i]) > 0)
                delta = Math.max(delta, Math.abs(2 * (newY[i] - newYstar[i]) / (newY[i] + newYstar[i])));
        }

        double newTimeStepInMinutes = timeStepInMinutes;

        if (finalize) {
            currentSolution.newTimeStepInMinutes = newTimeStepInMinutes;
            currentSolution.solution = newY;
        } else {
            double factor;
            if (delta != 0.0) {
                factor = epsilon / delta;

                if (factor >= 1)
                    newTimeStepInMinutes = timeStepInMinutes * Math.pow(factor, 0.15);
                else
                    newTimeStepInMinutes = timeStepInMinutes * Math.pow(factor, 0.25);
            } else {
                factor = 1e8;
                newTimeStepInMinutes = timeStepInMinutes * Math.pow(factor, 0.15);
                finalize = true;
            }

            // System.out.println(" --> "+timeStep+" "+epsilon+" "+Delta+" "+factor+"
            // "+newTimeStep+" ("+java.util.Calendar.getInstance().getTime()+")");

            step(currentTimeInMinutes, initialConditions, newTimeStepInMinutes, true, currentSolution, rainArray, etpArray);
        }

    }

    public void printDate( double minutes ) {
        double millis = minutes * 1000d * 60d;
        System.out.println(new DateTime((long) millis).toString(HMConstants.utcDateFormatterYYYYMMDDHHMM));
    }

    /**
     * Writes (in ascii format) to a specified file the values of the function described by
     * differential equations in the the intermidia steps requested to go from the Initial to the
     * Final time. This method is very specific for solving equations of flow in a network. It
     * prints output for the flow component at all locations.
     * 
     * @param intervalStartTimeInMinutes The initial time of the solution
     * @param intervalEndTimeInMinutes The final time of the solution
     * @param timeStepInMinutes How often the values are desired
     * @param initialConditions The value of the initial condition
     * @param etpArray 
     */
    @SuppressWarnings("nls")
    public void solve( DateTime currentTimstamp, int modelTimestepInMinutes, double internalTimestepInMinutes,
            double[] initialConditions, double[] rainArray, double[] etpArray ) throws IOException {
        isAtFinalSubtimestep = false;

        double intervalStartTimeInMinutes = currentTimstamp.getMillis() / 1000d / 60d;
        double intervalEndTimeInMinutes = intervalStartTimeInMinutes + modelTimestepInMinutes;

        // the running time inside the interval
        double currentTimeInMinutes = intervalStartTimeInMinutes;
        // the end time inside the interval
        double targetTimeInMinutes = intervalStartTimeInMinutes;

        // the object holding the iterated solution and internal timestep
        CurrentTimestepSolution currentSolution = new CurrentTimestepSolution();

        while( currentTimeInMinutes < intervalEndTimeInMinutes ) {
            /*
             * split the user set time interval into smaller intervals of time timeStepInMinutes.
             */
            targetTimeInMinutes = currentTimeInMinutes + internalTimestepInMinutes;
            while( currentTimeInMinutes < targetTimeInMinutes ) {
                /*
                 * inside step the intervals of time timeStepInMinutes are splitted again in
                 * intervals that begin with basicTimeStepInMinutes and are changed while iteration.
                 */
                step(currentTimeInMinutes, initialConditions, basicTimeStepInMinutes, false, currentSolution, rainArray, etpArray);
                if (currentTimeInMinutes + currentSolution.newTimeStepInMinutes > targetTimeInMinutes) {
                    break;
                }
                basicTimeStepInMinutes = currentSolution.newTimeStepInMinutes;
                currentTimeInMinutes += basicTimeStepInMinutes;
                currentSolution.newTimeStepInMinutes = currentTimeInMinutes;
                initialConditions = currentSolution.solution;
                for( int i = 0; i < initialConditions.length; i++ ) {
                    if (initialConditions[i] != initialConditions[i]) {
                        throw new ModelsIllegalargumentException("Problems occure during the integration procedure.", this
                                .getClass().getSimpleName());
                    }
                }
            }

            if (Math.abs(targetTimeInMinutes - intervalEndTimeInMinutes) < .0000001) {
                break;
            }

            step(currentTimeInMinutes, initialConditions, targetTimeInMinutes - currentTimeInMinutes, true, currentSolution,
                    rainArray, etpArray);

            if (currentTimeInMinutes + currentSolution.newTimeStepInMinutes >= intervalEndTimeInMinutes) {
                break;
            }

            if (initialConditions[0] < 1e-3) {
                System.out.println("Discharge in outlet less than the threshold.");
                break;
            }

            basicTimeStepInMinutes = currentSolution.newTimeStepInMinutes;
            currentTimeInMinutes += basicTimeStepInMinutes;
            currentSolution.newTimeStepInMinutes = currentTimeInMinutes;
            initialConditions = currentSolution.solution;
            for( int i = 0; i < initialConditions.length; i++ ) {
                if (initialConditions[i] != initialConditions[i]) {
                    throw new ModelsIllegalargumentException("Problems occure during the integration procedure.", this.getClass()
                            .getSimpleName());
                }
            }

            if (doLog) {
                outputStream.message("->  "
                        + new DateTime((long) (currentTimeInMinutes * 60.0 * 1000.0)).toString(OmsAdige.adigeFormatter) + " / "
                        + new DateTime((long) (intervalEndTimeInMinutes * 60. * 1000.)).toString(OmsAdige.adigeFormatter)
                        + " Outlet Duffy Discharge: " + initialConditions[0]);
            }
            // int hillslopeNum = rainArray.length;
            // for( int i = 0; i < hillslopeNum; i++ ) {
            // System.out.println(i + " Discharge " + initialConditions[i] + " qsub "
            // + initialConditions[i + hillslopeNum] + " S1 "
            // + initialConditions[i + 2 * hillslopeNum] + " S2 "
            // + initialConditions[i + 3 * hillslopeNum] + " rain " + rainArray[i]);
            // System.out.println("----------------------");
            // }
            // double avg = 0.0;
            // for( int i = 0; i < rainArray.length; i++ ) {
            // avg = avg + rainArray[i];
            // }
            // avg = avg / rainArray.length;
            // System.out.println("Outlet Discharge " + initialConditions[0] + " qsub "
            // + initialConditions[hillslopeNum] + " S1 "
            // + initialConditions[2 * hillslopeNum] + " S2 "
            // + initialConditions[3 * hillslopeNum] + " rain " + avg);

        }

        isAtFinalSubtimestep = true;
        //
        if (NumericsUtilities.dEq(currentTimeInMinutes, intervalEndTimeInMinutes) && initialConditions[0] > 1e-3) {
            step(currentTimeInMinutes, initialConditions, intervalEndTimeInMinutes - currentTimeInMinutes - 1. / 60., true,
                    currentSolution, rainArray, etpArray);
            basicTimeStepInMinutes = currentSolution.newTimeStepInMinutes;
            currentTimeInMinutes += basicTimeStepInMinutes;
            currentSolution.newTimeStepInMinutes = currentTimeInMinutes;
            initialConditions = currentSolution.solution;
            for( int i = 0; i < initialConditions.length; i++ ) {
                if (initialConditions[i] != initialConditions[i]) {
                    throw new ModelsIllegalargumentException("Problems occure during the integration procedure.", this.getClass()
                            .getSimpleName());
                }
            }

            double sum = 0;
            for( double d : rainArray ) {
                sum = sum + d;
            }
            sum = sum / rainArray.length;
            int hillslopeNum = rainArray.length;
            double currentDischarge = initialConditions[0] + initialConditions[hillslopeNum];

            outputStream.message("->  "
                    + new DateTime((long) (currentTimeInMinutes * 60.0 * 1000.0)).toString(OmsAdige.adigeFormatter) + " / "
                    + new DateTime((long) (intervalEndTimeInMinutes * 60. * 1000.)).toString(OmsAdige.adigeFormatter) + " "
                    + currentDischarge + " with avg rain: " + sum);
        } else {
            outputStream.errorMessage("WARNING, UNEXPECTED");
        }

        finalCond = initialConditions;

    }

    public double[] getFinalCond() {
        return finalCond;
    }

}
