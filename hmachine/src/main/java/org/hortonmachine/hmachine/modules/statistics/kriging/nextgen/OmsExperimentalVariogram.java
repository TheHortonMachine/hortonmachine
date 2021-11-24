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

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
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

    @Description("Specified cutoff")
    @In
    public double pCutoff;

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
        int iCount = inStationIds2CoordinateMap.size();// xStation.length;
        double distanceMatrix[][] = new double[iCount][iCount];

        double x_max = Double.NEGATIVE_INFINITY;
        double x_min = Double.POSITIVE_INFINITY;
        double y_max = Double.NEGATIVE_INFINITY;
        double y_min = Double.POSITIVE_INFINITY;

        for( Entry<Integer, Coordinate> entry : inStationIds2CoordinateMap.entrySet() ) {
            Coordinate coord = entry.getValue();
            x_min = Math.min(x_min, coord.x);
            y_min = Math.min(y_min, coord.y);
            x_max = Math.max(x_max, coord.x);
            y_max = Math.max(y_max, coord.y);
        }
        double diagonal = Math.sqrt((x_max - x_min) * (x_max - x_min) + (y_max - y_min) * (y_max - y_min));

        double cutoff = pCutoff;
        if (pCutoff == 0) {
            cutoff = diagonal / 3;
        }

        // Compute the distance matrix
        double maxDistance = 0;
        double mean = 0.0;
        List<Integer> stationIds = inStationIds2CoordinateMap.keySet().stream().collect(Collectors.toList());
        double[] hStation = new double[iCount];
        
        int row = 0;
        for( Integer stationId1 : stationIds ) {
//        for( int i = 0; i < iCount - 1; i++ ) {
            Coordinate c1 = inStationIds2CoordinateMap.get(stationId1);
//            x1 = xStation[i];
//            y1 = yStation[i];
            double value = inStationIds2ValueMap.get(stationId1)[0];
//            value = hStation[i];
            hStation[row] = value;

            mean += value;

            int col = 0;
            for( Integer stationId2 : stationIds ) {
//            for( int j = 0; j < iCount - 1; j++ ) {

                Coordinate c2 = inStationIds2CoordinateMap.get(stationId2);
//                x2 = xStation[j];
//                y2 = yStation[j];

//                double dDifX = c2.x - c1.x;
//                double dDifY = c2.y - c1.y;

                // Pitagora theorem
                distanceMatrix[row][col] = c1.distance(c2); // Math.sqrt(dDifX * dDifX + dDifY * dDifY);

                maxDistance = Math.max(maxDistance, distanceMatrix[row][col]);
                
                col++;
            }
            row++;
        }

        // compute the mean of the input hStation
        mean /= (double) iCount;

        double[][] outResult = calculateVariogram(cutoff, distanceMatrix, hStation, mean, maxDistance);

        outStationsDistances = distanceMatrix;
        
        outExperimentalVariogram = new HashMap<Integer, double[]>();

        for( int i = 0; i < outResult.length; i++ ) {
            outExperimentalVariogram.put(i, new double[]{outResult[i][0], outResult[i][1]});
        }
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
    public double[][] calculateVariogram( double cutoff, double[][] distanceMatrix, double[] hStation, double mean, double maxDistance ) {

        pBins = (pBins == 0) ? 15 : pBins;
        double binAmplitude = cutoff / pBins;

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
