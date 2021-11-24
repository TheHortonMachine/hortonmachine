/*
 * GNU GPL v3 License
 *
 * Copyright 2015 Marialaura Bancheri
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.experimental;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.utils.StationsSelection;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description("Experimental semivariogram algorithm.")
@Documentation("Experimental semivariogram")
@Author(name = "Giuseppe Formetta, Francesco Adami, Silvia Franceschi & Marialaura Bancheri")
@Keywords("Experimental semivariogram, Kriging, Hydrology")
@Label(HMConstants.STATISTICS)
@Name("variogram")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class ExperimentalVariogram extends HMModel {

    @Description("The vector of the measurement point, containing the position of the stations.")
    @In
    public SimpleFeatureCollection inStations = null;

    @Description("The field of the vector of stations, defining the id.")
    @In
    public String fStationsid = null;

    @Description("The file with the measured data, to be interpolated.")
    @In
    public HashMap<Integer, double[]> inData = null;

    @Description("Include zeros in computations (default is true).")
    @In
    public boolean doIncludezero = true;

    @Description("Specified cutoff")
    @In
    public double Cutoffinput;

    @Description("In the case of kriging with neighbor, inNumCloserStations is the number "
            + "of stations the algorithm has to consider")
    @In
    public int inNumCloserStations;

    @Description("Number of bins to consider in the anlysis")
    @In
    public int Cutoff_divide;

    @Description("The Experimental Distances.")
    @Out
    public HashMap<Integer, double[]> outDistances;

    @Description("The Experimental Variogram.")
    @Out
    public HashMap<Integer, double[]> outExperimentalVariogram;

    int differents;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new LogProgressMonitor();

    /**
     * Process.
     *
     * @throws Exception the exception
     */
    @Execute
    public void process() throws Exception {

        StationsSelection stations = new StationsSelection();

        stations.inStations = inStations;
        stations.inData = inData;
        stations.doIncludezero = doIncludezero;
        stations.fStationsid = fStationsid;

        stations.execute();

        differents = stations.n1;

        double[] xStations = stations.xStationInitialSet;
        double[] yStations = stations.yStationInitialSet;
        double[] hStations = stations.hStationInitialSet;
        int[] idStations = stations.idStationInitialSet;

        // number of different stations
        if (differents > 2) {

            double[][] outResult = processAlgorithm(xStations, yStations, hStations, Cutoffinput);
            storeResult(outResult);

        } else {
            System.out.println("Only 1 data >0 or All the data are equal. Variogram is not running");
        }

    }

    /**
     * Process algorithm.
     *
     * @param xStation the vector containing the x value of the station
     * @param yStation the vector containing the y value of the station
     * @param hStation the vector containing the variable value of the station
     * @param cutoffInput the cutoff input
     * @return the double[][] matrix of the results of the processing
     */
    private double[][] processAlgorithm( double[] xStation, double yStation[], double[] hStation, double cutoffInput ) {

        double x1, x2, y1, y2;
        double dDifX, dDifY;
        double value;
        double mean = 0;
        double maxDistance = 0;

        double cutoff;
        int iCount = xStation.length;
        double distanceMatrix[][] = new double[iCount][iCount];

        double x_max = xStation[0], y_max = yStation[0], diagonal;
        double x_min = xStation[0], y_min = yStation[0];
        for( int i = 1; i < iCount - 1; i++ ) {

            x_min = Math.min(x_min, xStation[i]);
            y_min = Math.min(y_min, yStation[i]);
            x_max = Math.max(x_max, xStation[i]);
            y_max = Math.max(y_max, yStation[i]);

        }

        diagonal = Math.sqrt((x_max - x_min) * (x_max - x_min) + (y_max - y_min) * (y_max - y_min));

        if (cutoffInput == 0) {
            cutoff = diagonal / 3;
        } else
            cutoff = cutoffInput;

        // Compute the distance matrix
        for( int i = 0; i < iCount - 1; i++ ) {
            x1 = xStation[i];
            y1 = yStation[i];
            value = hStation[i];

            mean += value;

            for( int j = 0; j < iCount - 1; j++ ) {

                x2 = xStation[j];
                y2 = yStation[j];

                dDifX = x2 - x1;
                dDifY = y2 - y1;

                // Pitagora theorem
                distanceMatrix[i][j] = Math.sqrt(dDifX * dDifX + dDifY * dDifY);

                maxDistance = Math.max(maxDistance, distanceMatrix[i][j]);

            }
        }

        // compute the mean of the input hStation
        mean /= (double) iCount;

        return calculate(cutoff, distanceMatrix, hStation, mean, maxDistance);

    }

    /**
     * Calculate the variances and the distances
     *
     * @param cutoff the cutoff
     * @param distanceMatrix the distance matrix
     * @param hStation the vector containing the variable value of the station
     * @param mean the mean value of the input data
     * @param maxDistance the max distance value
     * @return the double[][] matrix with the results (the variances and the distances)
     */
    public double[][] calculate( double cutoff, double[][] distanceMatrix, double[] hStation, double mean, double maxDistance ) {

        Cutoff_divide = (Cutoff_divide == 0) ? 15 : Cutoff_divide;
        double binAmplitude = cutoff / Cutoff_divide;

        // number of distance for each bin
        int iClasses = (int) (maxDistance / binAmplitude + 2);

        // definition of the vectors containing the variance, covariance, semivariance,
        // number of the points in the specified bin..

        double[] m_dSemivar = new double[iClasses];

        int[] iPointsInClass = new int[iClasses];

        double[] m_ddist = new double[iClasses];

        int contaNONzero = 0;

        for( int i = 0; i < distanceMatrix.length; i++ ) {
            // first cycle input hStation
            double value1 = hStation[i];

            for( int j = i + 1; j < distanceMatrix.length; j++ ) {
                if (distanceMatrix[i][j] > 0 && distanceMatrix[i][j] < cutoff) {

                    // return the class of considered distance
                    int iClass = (int) Math.floor((distanceMatrix[i][j]) / binAmplitude);

                    // counts the number of distances of each class
                    iPointsInClass[iClass]++;

                    // second cycle input hStation
                    double value2 = hStation[j];

                    // compute the numerator of the semivariance
                    double dSemivar = Math.pow((value1 - value2), 2.);

                    // sum all the semivariances for the considered class

                    m_dSemivar[iClass] += dSemivar;

                    m_ddist[iClass] += distanceMatrix[i][j];

                }
            }

        }

        double[][] result = new double[Cutoff_divide][2];

        for( int i = 0; i < Cutoff_divide; i++ ) {

            contaNONzero = (iPointsInClass[i] == 0) ? contaNONzero : contaNONzero + 1;

            // Compute the semivariance
            m_dSemivar[i] = (iPointsInClass[i] == 0) ? 0 : m_dSemivar[i] / (2. * iPointsInClass[i]);

            // Compute the mean distance for each class
            m_ddist[i] = (iPointsInClass[i] == 0) ? 0 : m_ddist[i] / iPointsInClass[i];

            result[i][0] = m_ddist[i];
            result[i][1] = m_dSemivar[i];
        }

        return result;

    }

    /**
     * Store result.
     *
     * @param result are the resulting variances and the distances
     * @throws SchemaException the schema exception
     */
    private void storeResult( double[][] result ) throws SchemaException {
        outDistances = new HashMap<Integer, double[]>();
        outExperimentalVariogram = new HashMap<Integer, double[]>();

        for( int i = 0; i < result.length; i++ ) {
            outDistances.put(i, new double[]{result[i][0]});
            outExperimentalVariogram.put(i, new double[]{result[i][1]});
        }
    }

}
