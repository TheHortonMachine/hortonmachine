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
package org.hortonmachine.hmachine.modules.statistics.kriging.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.locationtech.jts.geom.Coordinate;

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
public class OmsExperimentalVariogram extends HMModel {
    @Description("Valid station ids to coordinates map for current timestep.")
    @In
    public HashMap<Integer, Coordinate> inStationIds2CoordinateMap;

    @Description("Measurement data for current timestep and station ids.")
    @In
    public HashMap<Integer, double[]> inStationIds2ValueMap;

    @Description("Include zeros in computations (default is true).")
    @In
    public boolean doIncludezero = true;

    @Description("Number of bins to consider in the anlysis")
    @In
    public int pBins;

    @Description("The Experimental Variogram. The double array is of the form [distance, variance]")
    @Out
    public HashMap<Integer, double[]> outExperimentalVariogram;

    @Description("The matrix of distances between stations.")
    @Out
    public double[][] outStationsDistances;

    /**
     * Process.
     *
     * @throws Exception the exception
     */
    @Execute
    public void process() throws Exception {

        Set<Integer> stationIds = inStationIds2CoordinateMap.keySet();

        List<Coordinate> coordsList = new ArrayList<>();
        List<Double> valuesList = new ArrayList<>();
        for( Integer stationId : stationIds ) {
            double[] values = inStationIds2ValueMap.get(stationId);
            double sum = 0;
            for( double value : values ) {
                sum += value;
            }
            double avg = sum / values.length;

            Coordinate c = inStationIds2CoordinateMap.get(stationId);
            valuesList.add(avg);
            coordsList.add(c);
        }

        // Compute the distance matrix
        double maxDistance = 0;
        double mean = 0.0;
        int iCount = coordsList.size();// xStation.length;
        double distanceMatrix[][] = new double[iCount][iCount];
        double[] hStation = new double[iCount];

        int row = 0;

        for( int i = 0; i < coordsList.size(); i++ ) {
            Coordinate c1 = coordsList.get(i);
            double value = valuesList.get(i);
//        for( int i = 0; i < iCount - 1; i++ ) {
//            x1 = xStation[i];
//            y1 = yStation[i];
//            value = hStation[i];
            hStation[row] = value;

            mean += value;

            int col = 0;
            for( Coordinate c2 : coordsList ) {
//            for( int j = 0; j < iCount - 1; j++ ) {

//                x2 = xStation[j];
//                y2 = yStation[j];

//                double dDifX = c2.x - c1.x;
//                double dDifY = c2.y - c1.y;

                // Pitagora theorem
                distanceMatrix[row][col] = c1.distance(c2); // Math.sqrt(dDifX * dDifX + dDifY *
                                                            // dDifY);

                maxDistance = Math.max(maxDistance, distanceMatrix[row][col]);

                col++;
            }
            row++;
        }

        // compute the mean of the input hStation
        mean /= (double) iCount;

        double[][] outResult = calculateVariogram(distanceMatrix, hStation, mean, maxDistance);

        outStationsDistances = distanceMatrix;

        outExperimentalVariogram = new HashMap<Integer, double[]>();

        for( int i = 0; i < outResult.length; i++ ) {
            outExperimentalVariogram.put(i, new double[]{outResult[i][0], outResult[i][1]});
        }
    }

    /**
     * Calculate the variances and the distances
     *
     * @param distanceMatrix the distance matrix
     * @param hStation the vector containing the variable value of the station
     * @param mean the mean value of the input data
     * @param maxDistance the max distance value
     * @return the double[][] matrix with the results (the variances and the distances)
     */
    public double[][] calculateVariogram( double[][] distanceMatrix, double[] hStation, double mean, double maxDistance ) {

        pBins = (pBins == 0) ? 15 : pBins;
        double binAmplitude = ((int) Math.ceil(maxDistance / pBins));

        // number of distance for each bin
        int iClasses = pBins;// (int) (maxDistance / binAmplitude + 2);

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
                if (distanceMatrix[i][j] > 0) {

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

        double[][] result = new double[pBins][2];

        for( int i = 0; i < pBins; i++ ) {

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

}
