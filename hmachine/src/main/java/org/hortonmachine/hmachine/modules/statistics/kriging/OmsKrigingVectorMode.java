/* This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.hortonmachine.hmachine.modules.statistics.kriging;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.math.matrixes.ColumnVector;
import org.hortonmachine.gears.utils.math.regressions.PolyTrendLine;
import org.hortonmachine.gears.utils.math.regressions.RegressionLine;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.hortonmachine.hmachine.modules.statistics.kriging.utils.SimpleLinearSystemSolverFactory;
import org.hortonmachine.hmachine.modules.statistics.kriging.utils.StationsSelection;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.ITheoreticalVariogram;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.TheoreticalVariogram;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

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

@Description("Ordinary kriging algorithm.")
@Documentation("Kriging.html")
@Author(name = "Giuseppe Formetta, Daniele Andreis, Silvia Franceschi, Andrea Antonello, Marialaura Bancheri, Francesco Serafin")
@Keywords("Kriging, Hydrology")
@Label("")
@Name("kriging")
@Status()
@License("General Public License Version 3 (GPLv3)")
public class OmsKrigingVectorMode extends HMModel {

    @Description("The .shp of the measurement point, containing the position of the stations.")
    @In
    public SimpleFeatureCollection inStations = null;

    @Description("The field of the vector of stations, defining the id.")
    @In
    public String fStationsid = null;

    @Description("The field of the vector of stations, defining the elevation.")
    @In
    public String fStationsZ = null;

    @Description("The type of theoretical semivariogram: " + ITheoreticalVariogram.TYPES)
    @In
    public String pSemivariogramType = null;

    @Description("The HM with the measured data to be interpolated.")
    @In
    public HashMap<Integer, double[]> inData = null;

    @Description("The vector of the points in which the data have to be interpolated.")
    @In
    public SimpleFeatureCollection inInterpolate = null;

    @Description("The field of the interpolated vector points, defining the id.")
    @In
    public String fInterpolateid = null;

    @Description("The field of the interpolated vector points, defining the elevation.")
    @In
    public String fPointZ = null;

    @Description("Include zeros in computations (default is true).")
    @In
    public boolean doIncludezero = true;

    @Description("The range if the models runs with the gaussian variogram.")
    @In
    public double range;

    @Description("The sill if the models runs with the gaussian variogram.")
    @In
    public double sill;

    @Description("Is the nugget if the models runs with the gaussian variogram.")
    @In
    public double nugget;

    @Description("In the case of kriging with neighbor, maxdist is the maximum distance "
            + "within the algorithm has to consider the stations")
    @In
    public double maxdist;

    @Description("In the case of kriging with neighbor, inNumCloserStations is the number "
            + "of stations the algorithm has to consider")
    @In
    public int inNumCloserStations;

    @Description("Switch for detrended mode.")
    @In
    public boolean doDetrended;

    @Description("The double value of the trend")
    @In
    public double trend_intercept;

    @Description("The double value of the trend")
    @In
    public double trend_coefficient;

    @Description("Degree of polynomial regression, default is 1")
    @In
    public int regressionOrder = 1;

    @Description("Type of linear system solver")
    @In
    public String linearSystemSolverType = "default";

    @Description("The hashmap withe the interpolated results")
    @Out
    public HashMap<Integer, double[]> outData = null;

    private static final double TOLL = 1.0d * 10E-8;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    /** The id of the cosidered station */
    int id;

    /**
     * Executing ordinary kriging.
     * <p>
     * <li>Verify if the parameters are correct.
     * <li>Calculating the matrix of the covariance (a).
     * <li>For each point to interpolated, evalutate the know term vector (b)
     * and solve the system (a x)=b where x is the weight.
     * </p>
     *
     * @throws Exception the exception
     */

    @Execute
    public void executeKriging() throws Exception {

        verifyInput();

        LinkedHashMap<Integer, Coordinate> pointsToInterpolateId2Coordinates = null;

        pointsToInterpolateId2Coordinates = getCoordinate(0, inInterpolate, fInterpolateid);

        Set<Integer> pointsToInterpolateIdSet = pointsToInterpolateId2Coordinates.keySet();
        Iterator<Integer> idIterator = pointsToInterpolateIdSet.iterator();

        int j = 0;

        double[] result = new double[pointsToInterpolateId2Coordinates.size()];
        int[] idArray = new int[pointsToInterpolateId2Coordinates.size()];

        while( idIterator.hasNext() ) {

            double sum = 0.;
            id = idIterator.next();
            idArray[j] = id;

            Coordinate coordinate = (Coordinate) pointsToInterpolateId2Coordinates.get(id);

            /**
             * StationsSelection is an external class that allows the 
             * selection of the stations involved in the study.
             * It is possible to define if to include stations with zero values,
             * station in a define neighborhood or within a max distance from 
             * the considered point.
             */

            StationsSelection stations = new StationsSelection();

            stations.idx = coordinate.x;
            stations.idy = coordinate.y;
            stations.inStations = inStations;
            stations.inData = inData;
            stations.doIncludezero = doIncludezero;
            stations.maxdist = maxdist;
            stations.inNumCloserStations = inNumCloserStations;
            stations.fStationsid = fStationsid;
            stations.fStationsZ = fStationsZ;

            stations.execute();

            double[] xStations = stations.xStationInitialSet;
            double[] yStations = stations.yStationInitialSet;
            double[] zStations = stations.zStationInitialSet;
            double[] hStations = stations.hStationInitialSet;
            boolean areAllEquals = stations.areAllEquals;
            int n1 = xStations.length - 1;

            xStations[n1] = coordinate.x;
            yStations[n1] = coordinate.y;
            zStations[n1] = coordinate.getZ();

            double[] hresiduals = hStations;

            if (doDetrended) {

                RegressionLine t = new PolyTrendLine(regressionOrder);
                t.setValues(zStations, hStations);

                double[] regressionParameters = t.getRegressionParameters();
                trend_intercept = regressionParameters[0];
                trend_coefficient = regressionParameters[1];
                hresiduals = t.getResiduals();
                
//                Regression r = new Regression();
//
//                r = new Regression(zStations, hStations);
//                r.polynomial(regressionOrder);
//
//                /*If there is a trend for meteorological
//                 * variables and elevation and it is statistically significant 
//                 * then the residuals from this linear trend
//                 * are computed for each meteorological stations.
//                 */
//
//                trend_intercept = r.getBestEstimates()[0];
//                trend_coefficient = r.getBestEstimates()[1];
//                hresiduals = r.getResiduals();

            }

            if (n1 != 0) {

                if (!areAllEquals && n1 > 1) {
                    pm.beginTask(msg.message("kriging.working"), pointsToInterpolateId2Coordinates.size());

                    double h0 = 0.0;

                    /*
                     * calculating the covariance matrix.
                     */
                    double[][] covarianceMatrix = covMatrixCalculating(xStations, yStations, zStations, n1);

                    double[] knownTerm = knownTermsCalculation(xStations, yStations, zStations, n1);

                    /*
                     * solve the linear system, where the result is the weight (moltiplicativeFactor).
                     */
                    ColumnVector solution = SimpleLinearSystemSolverFactory.solve(knownTerm, covarianceMatrix,
                            linearSystemSolverType);

                    double[] moltiplicativeFactor = solution.copyValues1D();

                    for( int k = 0; k < n1; k++ ) {
                        h0 = h0 + moltiplicativeFactor[k] * hresiduals[k];

                        // sum is computed to check that
                        // the sum of all the weights is 1
                        sum = sum + moltiplicativeFactor[k];

                    }

                    double trend = (doDetrended) ? coordinate.getZ() * trend_coefficient + trend_intercept : 0;
                    h0 = h0 + trend;

                    result[j] = h0;
                    j++;

                    if (Math.abs(sum - 1) >= TOLL) {
                        throw new ModelsRuntimeException("Error in the coffeicients calculation",
                                this.getClass().getSimpleName());
                    }
                    pm.worked(1);
                } else if (n1 == 1 || areAllEquals) {

                    double tmp = hresiduals[0];
                    pm.message(msg.message("kriging.setequalsvalue"));
                    pm.beginTask(msg.message("kriging.working"), pointsToInterpolateId2Coordinates.size());
                    result[j] = tmp;
                    j++;
                    n1 = 0;
                    pm.worked(1);

                }

                pm.done();

            } else {

                pm.errorMessage("No value for this time step");

                double[] value = inData.values().iterator().next();
                result[j] = value[0];
                j++;

            }

        }

        storeResult(result, idArray);

    }

    /**
     * Verify the input of the model.
     */
    private void verifyInput() {
        if (inData == null || inStations == null) {
            throw new NullPointerException(msg.message("kriging.stationProblem"));
        }

    }

    /**
     * Round.
     *
     * @param value is the value of the variable considered 
     * @param places the places to consider after the comma
     * @return the double value of the variable rounded
     */
    public static double round( double value, int places ) {
        if (places < 0)
            throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    /**
     * Extract the coordinate of a FeatureCollection in a HashMap with an ID as
     * a key.
     *
     * @param nStaz the number of the stations
     * @param collection is the collection of the considered points 
     * @param idField the field containing the id of the stations 
     * @return the coordinate of the station
     * @throws Exception if a field of elevation isn't the same of the collection
     */
    private LinkedHashMap<Integer, Coordinate> getCoordinate( int nStaz, SimpleFeatureCollection collection, String idField )
            throws Exception {
        LinkedHashMap<Integer, Coordinate> id2CoordinatesMcovarianceMatrix = new LinkedHashMap<Integer, Coordinate>();
        FeatureIterator<SimpleFeature> iterator = collection.features();
        Coordinate coordinate = null;
        try {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                int name = ((Number) feature.getAttribute(idField)).intValue();
                coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid().getCoordinate();
                double z = 0;
                if (fPointZ != null) {
                    try {
                        z = ((Number) feature.getAttribute(fPointZ)).doubleValue();
                    } catch (NullPointerException e) {
                        pm.errorMessage(msg.message("kriging.noPointZ"));
                        throw new Exception(msg.message("kriging.noPointZ"));
                    }
                }
                coordinate.setZ(z);
                id2CoordinatesMcovarianceMatrix.put(name, coordinate);
            }
        } finally {
            iterator.close();
        }

        return id2CoordinatesMcovarianceMatrix;
    }

    /**
     * Covariance matrix calculation.
     *
     * @param x the x coordinates.
     * @param y the y coordinates.
     * @param z the z coordinates.
     * @param n the number of the stations points.
     * @return the double[][] matrix with the covariance
     */
    private double[][] covMatrixCalculating( double[] x, double[] y, double[] z, int n ) {

        double[][] covarianceMatrix = new double[n + 1][n + 1];

        for( int j = 0; j < n; j++ ) {
            for( int i = 0; i < n; i++ ) {
                double rx = x[i] - x[j];
                double ry = y[i] - y[j];
                double rz = z[i] - z[j];

                covarianceMatrix[j][i] = variogram(nugget, range, sill, rx, ry, rz);
                covarianceMatrix[i][j] = variogram(nugget, range, sill, rx, ry, rz);

            }
        }

        for( int i = 0; i < n; i++ ) {
            covarianceMatrix[i][n] = 1.0;
            covarianceMatrix[n][i] = 1.0;

        }
        covarianceMatrix[n][n] = 0;
        return covarianceMatrix;

    }

    /**
     * Known terms calculation.
     *
     * @param x the x coordinates.
     * @param y the y coordinates.
     * @param z the z coordinates.
     * @param n the number of the stations points.
     * @return the double[] vector of the known terms
     */
    private double[] knownTermsCalculation( double[] x, double[] y, double[] z, int n ) {

        // known terms vector
        double[] gamma = new double[n + 1];

        for( int i = 0; i < n; i++ ) {
            double rx = x[i] - x[n];
            double ry = y[i] - y[n];
            double rz = z[i] - z[n];
            gamma[i] = variogram(nugget, range, sill, rx, ry, rz);
        }

        gamma[n] = 1.0;
        return gamma;

    }

    /**
     * Variogram.
     *
     * @param nug is the nugget
     * @param range is the range
     * @param sill is the sill
     * @param rx is the x distance
     * @param ry is the y distance
     * @param rz is the z distance
     * @return the double value of the variance
     */
    private double variogram( double nug, double range, double sill, double rx, double ry, double rz ) {
        if (HMConstants.isNovalue(rz)) {
            rz = 0;
        }
        double h2 = Math.sqrt(rx * rx + rz * rz + ry * ry);
        double vgmResult;

        if (h2 != 0) {
            TheoreticalVariogram vgm = new TheoreticalVariogram();
            vgmResult = vgm.calculateVGM(pSemivariogramType, h2, sill, range, nug);
        } else {
            vgmResult = 0;
        }
        return vgmResult;
    }

    /**
     * Store the result in a HashMcovarianceMatrix (if the mode is 0 or 1).
     *
     * @param result the result
     * @param id            the associated id of the calculating points.
     * @throws SchemaException the schema exception
     */
    private void storeResult( double[] result, int[] id ) throws SchemaException {
        outData = new HashMap<Integer, double[]>();
        for( int i = 0; i < result.length; i++ ) {
            outData.put(id[i], new double[]{result[i]});
        }
    }

}
