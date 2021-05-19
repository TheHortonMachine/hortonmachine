/* This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.hmachine.modules.statistics.kriging.old;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_doIncludezero_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_doLogarithmic_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_fInterpolateid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_fPointZ_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_fStationsZ_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_fStationsid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_inData_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_inInterpolate_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_inInterpolationGrid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_inStations_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_outData_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_outGrid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pIntegralscale_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pMode_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pNug_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pSemivariogramType_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pVariance_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.geotools.geometry.DirectPosition2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.matrixes.ColumnVector;
import org.hortonmachine.gears.utils.math.matrixes.LinearSystem;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Description(OMSKRIGING_DESCRIPTION)
@Author(name = OMSKRIGING_AUTHORNAMES, contact = OMSKRIGING_AUTHORCONTACTS)
@Keywords(OMSKRIGING_KEYWORDS)
@Label(OMSKRIGING_LABEL)
@Name(OMSKRIGING_NAME)
@Status(OMSKRIGING_STATUS)
@License(OMSKRIGING_LICENSE)
public class OmsKriging extends HMModel {

    @Description(OMSKRIGING_inStations_DESCRIPTION)
    @In
    public SimpleFeatureCollection inStations = null;

    @Description(OMSKRIGING_fStationsid_DESCRIPTION)
    @In
    public String fStationsid = null;

    @Description(OMSKRIGING_fStationsZ_DESCRIPTION)
    @In
    public String fStationsZ = null;

    @Description(OMSKRIGING_inData_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inData = null;

    @Description(OMSKRIGING_inInterpolate_DESCRIPTION)
    @In
    public SimpleFeatureCollection inInterpolate = null;

    @Description(OMSKRIGING_fInterpolateid_DESCRIPTION)
    @In
    public String fInterpolateid = null;

    @Description(OMSKRIGING_fPointZ_DESCRIPTION)
    @In
    public String fPointZ = null;

    /**
     * Define the calculation mode. It can be 0 or 1.
     *
     * <li>When mode == 0, the values to calculate are in a non-regular 
     * grid (the coordinates are stored in a {@link FeatureCollection}, 
     * so parameters inInterpolate and fInterpolateid must be set, and
     * the calculated values will be in the outData field.
     *
     * <li>When mode == 1, the values are in a regular grid (the coordinates 
     * are stored in a {@link GridCoverage2D), so parameter gridToInterpolate
     * must be set, and the calculated values will be in the outGrid field.
     */
    @Description(OMSKRIGING_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    /**
     * The integral scale, used when defaultVariogramMode is 0. Must be
     * a 3-element double array containing the scaling factor for the
     * x, y and z dimensions in the elements 0, 1 and 2, respectively.
     */
    @Description(OMSKRIGING_pIntegralscale_DESCRIPTION)
    @In
    public double[] pIntegralscale = null;

    /**
     * Variance of the measure field. Used when defaultVariogramMode is 0.
     */
    @Description(OMSKRIGING_pVariance_DESCRIPTION)
    @In
    public double pVariance = 0;

    /**
     * The logarithm selector, if it's true then the models runs with the log of
     * the data.
     */
    @Description(OMSKRIGING_doLogarithmic_DESCRIPTION)
    @In
    public boolean doLogarithmic = false;

    @Description(OMSKRIGING_inInterpolationGrid_DESCRIPTION)
    @In
    public GridGeometry2D inInterpolationGrid = null;

    public int defaultVariogramMode = 0;

    @Description(OMSKRIGING_pSemivariogramType_DESCRIPTION)
    @In
    public double pSemivariogramType = 0;

    @Description(OMSKRIGING_doIncludezero_DESCRIPTION)
    @In
    public boolean doIncludezero = true;

    @Description(OMSKRIGING_pA_DESCRIPTION)
    @In
    public double pA;

    @Description(OMSKRIGING_pS_DESCRIPTION)
    @In
    public double pS;

    @Description(OMSKRIGING_pNug_DESCRIPTION)
    @In
    public double pNug;

    @Description(OMSKRIGING_outGrid_DESCRIPTION)
    @Out
    public GridCoverage2D outGrid = null;

    @Description(OMSKRIGING_outData_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> outData = null;

    /**
     * A tolerance.
     */
    private static final double TOLL = 1.0d * 10E-8;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private WritableRaster outWR = null;
    private int cols;
    private int rows;
    private double south;
    private double west;
    private double xres;
    private double yres;

    /**
     * Executing ordinary kriging.
     * <p>
     * <li>Verify if the parameters are correct.
     * <li>Calculating the matrix of the covariance (a).
     * <li>For each point to interpolated, evalutate the know term vector (b)
     * and solve the system (a x)=b where x is the weight.
     * </p>
     * 
     * @throws SchemaException
     */

    @Execute
    public void process() throws Exception {
        verifyInput();

        List<Double> xStationList = new ArrayList<Double>();
        List<Double> yStationList = new ArrayList<Double>();
        List<Double> zStationList = new ArrayList<Double>();
        List<Double> hStationList = new ArrayList<Double>();

        /*
         * counter for the number of station with measured value !=0.
         */
        int n1 = 0;
        /*
         * Store the station coordinates and measured data in the array.
         */
        FeatureIterator<SimpleFeature> stationsIter = inStations.features();
        try {
            while( stationsIter.hasNext() ) {
                SimpleFeature feature = stationsIter.next();
                Object stationId = feature.getAttribute(fStationsid);
                int id;
                if (stationId instanceof Number) {
                    id = ((Number) stationId).intValue();
                } else if (stationId instanceof String) {
                    id = (int) Double.parseDouble((String) stationId);
                } else {
                    throw new ModelsIllegalargumentException("Unreadable type found for the station id.", this, pm);
                }
                double z = 0;
                if (fStationsZ != null) {
                    try {
                        z = ((Number) feature.getAttribute(fStationsZ)).doubleValue();
                    } catch (NullPointerException e) {
                        pm.errorMessage(msg.message("kriging.noStationZ"));
                        throw new Exception(msg.message("kriging.noStationZ"));

                    }
                }
                Coordinate coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid().getCoordinate();
                double[] h = inData.get(id);
                if (h == null || isNovalue(h[0])) {
                    /*
                     * skip data for non existing stations, they are allowed.
                     * Also skip novalues.
                     */
                    continue;
                }
                if (defaultVariogramMode == 0) {
                    if (doIncludezero) {
                        if (Math.abs(h[0]) >= 0.0) { // TOLL
                            xStationList.add(coordinate.x);
                            yStationList.add(coordinate.y);
                            zStationList.add(z);
                            hStationList.add(h[0]);
                            n1 = n1 + 1;
                        }
                    } else {
                        if (Math.abs(h[0]) > 0.0) { // TOLL
                            xStationList.add(coordinate.x);
                            yStationList.add(coordinate.y);
                            zStationList.add(z);
                            hStationList.add(h[0]);
                            n1 = n1 + 1;
                        }
                    }
                } else if (defaultVariogramMode == 1) {
                    if (doIncludezero) {
                        if (Math.abs(h[0]) >= 0.0) { // TOLL
                            xStationList.add(coordinate.x);
                            yStationList.add(coordinate.y);
                            zStationList.add(z);
                            hStationList.add(h[0]);
                            n1 = n1 + 1;
                        }
                    } else {
                        if (Math.abs(h[0]) > 0.0) { // TOLL
                            xStationList.add(coordinate.x);
                            yStationList.add(coordinate.y);
                            zStationList.add(z);
                            hStationList.add(h[0]);
                            n1 = n1 + 1;
                        }
                    }

                }
            }
        } finally {
            stationsIter.close();
        }

        int nStaz = xStationList.size();
        /*
         * The coordinates of the station points plus in last position a place
         * for the coordinate of the point to interpolate.
         */
        double[] xStation = new double[nStaz + 1];
        double[] yStation = new double[nStaz + 1];
        double[] zStation = new double[nStaz + 1];
        double[] hStation = new double[nStaz + 1];
        boolean areAllEquals = true;
        if (nStaz != 0) {
            xStation[0] = xStationList.get(0);
            yStation[0] = yStationList.get(0);
            zStation[0] = zStationList.get(0);
            hStation[0] = hStationList.get(0);
            double previousValue = hStation[0];

            for( int i = 1; i < nStaz; i++ ) {

                double xTmp = xStationList.get(i);
                double yTmp = yStationList.get(i);
                double zTmp = zStationList.get(i);
                double hTmp = hStationList.get(i);
                boolean doubleStation = ModelsEngine.verifyDoubleStation(xStation, yStation, zStation, hStation, xTmp, yTmp,
                        zTmp, hTmp, i, false);
                if (!doubleStation) {
                    xStation[i] = xTmp;
                    yStation[i] = yTmp;
                    zStation[i] = zTmp;
                    hStation[i] = hTmp;
                    if (areAllEquals && hStation[i] != previousValue) {
                        areAllEquals = false;
                    }
                    previousValue = hStation[i];
                }
            }
        }
        LinkedHashMap<Integer, Coordinate> pointsToInterpolateId2Coordinates = null;
        // vecchio int numPointToInterpolate = getNumPoint(inInterpolate);
        int numPointToInterpolate = 0;

        /*
         * if the isLogarithmic is true then execute the model with log value.
         */
        // vecchio double[] result = new double[numPointToInterpolate];

        if (pMode == 0) {
            pointsToInterpolateId2Coordinates = getCoordinate(numPointToInterpolate, inInterpolate, fInterpolateid);
        } else if (pMode == 1) {
            pointsToInterpolateId2Coordinates = getCoordinate(inInterpolationGrid);
            numPointToInterpolate = pointsToInterpolateId2Coordinates.size();
        } else {
            throw new ModelsIllegalargumentException("The parameter pMode can only be 0 or 1.", this, pm);
        }

        Set<Integer> pointsToInterpolateIdSet = pointsToInterpolateId2Coordinates.keySet();
        Iterator<Integer> idIterator = pointsToInterpolateIdSet.iterator();
        int j = 0;
        // vecchio int[] idArray = new int[inInterpolate.size()];
        int[] idArray = new int[pointsToInterpolateId2Coordinates.size()];
        double[] result = new double[pointsToInterpolateId2Coordinates.size()];
        if (n1 != 0) {
            if (doLogarithmic) {
                for( int i = 0; i < nStaz; i++ ) {
                    if (hStation[i] > 0.0) {
                        hStation[i] = Math.log(hStation[i]);
                    }
                }
            }

            /*
             * calculating the covariance matrix.
             */
            double[][] covarianceMatrix = covMatrixCalculating(xStation, yStation, zStation, n1);
            /*
             * extract the coordinate of the points where interpolated.
             */

            /*
             * initialize the solution and its variance vector.
             */

            if (!areAllEquals && n1 > 1) {
                // pm.beginTask(msg.message("kriging.working"),inInterpolate.size());
                while( idIterator.hasNext() ) {
                    double sum = 0.;
                    int id = idIterator.next();
                    idArray[j] = id;
                    Coordinate coordinate = (Coordinate) pointsToInterpolateId2Coordinates.get(id);
                    xStation[n1] = coordinate.x;
                    yStation[n1] = coordinate.y;
                    zStation[n1] = coordinate.z;
                    /*
                     * calculating the right hand side of the kriging linear
                     * system.
                     */
                    double[] knownTerm = knownTermsCalculation(xStation, yStation, zStation, n1);

                    /*
                     * solve the linear system, where the result is the weight.
                     */
                    ColumnVector knownTermColumn = new ColumnVector(knownTerm);
                    LinearSystem linearSystem = new LinearSystem(covarianceMatrix);
                    ColumnVector solution = linearSystem.solve(knownTermColumn, true);
                    // Matrix a = new Matrix(covarianceMatrix);
                    // Matrix b = new Matrix(knownTerm, knownTerm.length);
                    // Matrix x = a.solve(b);
                    double[] moltiplicativeFactor = solution.copyValues1D();

                    double h0 = 0.0;
                    for( int k = 0; k < n1; k++ ) {
                        h0 = h0 + moltiplicativeFactor[k] * hStation[k];
                        sum = sum + moltiplicativeFactor[k];
                    }

                    if (doLogarithmic) {
                        h0 = Math.exp(h0);
                    }
                    result[j] = h0;
                    j++;
                    if (Math.abs(sum - 1) >= TOLL) {
                        throw new ModelsRuntimeException("Error in the coffeicients calculation", this.getClass().getSimpleName());
                    }

                }
            } else if (n1 == 1 || areAllEquals) {
                double tmp = hStation[0];
                int k = 0;
                pm.message(msg.message("kriging.setequalsvalue"));
                while( idIterator.hasNext() ) {
                    int id = idIterator.next();
                    result[k] = tmp;
                    idArray[k] = id;
                    k++;
                }

            }
            if (pMode == 0) {
                storeResult(result, idArray);
            } else {
                storeResult(result, pointsToInterpolateId2Coordinates);
            }
        } else {
            pm.errorMessage("No rain for this time step");
            j = 0;
            double[] value = inData.values().iterator().next();
            while( idIterator.hasNext() ) {
                int id = idIterator.next();
                idArray[j] = id;
                result[j] = value[0];
                j++;
            }
            if (pMode == 0) {
                storeResult(result, idArray);
            } else {
                storeResult(result, pointsToInterpolateId2Coordinates);
            }
        }
    }

    /**
     * Verify the input of the model.
     */
    private void verifyInput() {
        if (inData == null || inStations == null) {
            throw new NullPointerException(msg.message("kriging.stationproblem"));
        }
        if (pMode < 0 || pMode > 1) {
            throw new IllegalArgumentException(msg.message("kriging.defaultMode"));
        }

        if (defaultVariogramMode != 0 && defaultVariogramMode != 1) {
            throw new IllegalArgumentException(msg.message("kriging.variogramMode"));
        }
        if (defaultVariogramMode == 0) {
            if (pVariance == 0 || pIntegralscale[0] == 0 || pIntegralscale[1] == 0 || pIntegralscale[2] == 0) {

                pm.errorMessage(msg.message("kriging.noParam"));
                pm.errorMessage("varianza " + pVariance);
                pm.errorMessage("Integral scale x " + pIntegralscale[0]);
                pm.errorMessage("Integral scale y " + pIntegralscale[1]);
                pm.errorMessage("Integral scale z " + pIntegralscale[2]);
            }
        }
        if (defaultVariogramMode == 1) {
            if (pNug == 0 || pS == 0 || pA == 0) {
                pm.errorMessage(msg.message("kriging.noParam"));
                pm.errorMessage("Nugget " + pNug);
                pm.errorMessage("Sill " + pS);
                pm.errorMessage("Range " + pA);
            }
        }

        if ((pMode == 0) && inInterpolate == null) {
            throw new ModelsIllegalargumentException(msg.message("kriging.noPoint"), this, pm);
        }
        if (pMode == 1 && inInterpolationGrid == null) {
            throw new ModelsIllegalargumentException("The gridded interpolation needs a gridgeometry in input.", this, pm);
        }

    }

    /**
     * Store the result in a HashMap (if the mode is 0 or 1)
     * 
     * @param result2
     *            the result of the model
     * @param id
     *            the associated id of the calculating points.
     * @throws SchemaException
     * @throws SchemaException
     */
    private void storeResult( double[] result2, int[] id ) throws SchemaException {
        outData = new HashMap<Integer, double[]>();
        for( int i = 0; i < result2.length; i++ ) {
            outData.put(id[i], new double[]{checkResultValue(result2[i])});
        }
    }

    private void storeResult( double[] interpolatedValues, HashMap<Integer, Coordinate> interpolatedCoordinatesMap )
            throws MismatchedDimensionException, Exception {

        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        Set<Integer> pointsToInterpolateIdSett = interpolatedCoordinatesMap.keySet();
        Iterator<Integer> idIterator = pointsToInterpolateIdSett.iterator();
        int c = 0;
        MathTransform transf = inInterpolationGrid.getCRSToGrid2D();

        final DirectPosition gridPoint = new DirectPosition2D();

        while( idIterator.hasNext() ) {
            int id = idIterator.next();
            Coordinate coordinate = (Coordinate) interpolatedCoordinatesMap.get(id);

            DirectPosition point = new DirectPosition2D(inInterpolationGrid.getCoordinateReferenceSystem(), coordinate.x,
                    coordinate.y);
            transf.transform(point, gridPoint);

            double[] gridCoord = gridPoint.getCoordinate();
            int x = (int) gridCoord[0];
            int y = (int) gridCoord[1];

            outIter.setSample(x, y, 0, checkResultValue(interpolatedValues[c]));
            c++;

        }

        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(inInterpolationGrid);

        outGrid = CoverageUtilities
                .buildCoverage("gridded", outWR, regionMap, inInterpolationGrid.getCoordinateReferenceSystem());

    }

    private double checkResultValue( double resultValue ) {
        if (resultValue < 0) {
            return 0.0;
        }
        return resultValue;
    }

    private LinkedHashMap<Integer, Coordinate> getCoordinate( GridGeometry2D grid ) {
        LinkedHashMap<Integer, Coordinate> out = new LinkedHashMap<Integer, Coordinate>();
        int count = 0;
        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(grid);
        cols = regionMap.getCols();
        rows = regionMap.getRows();
        south = regionMap.getSouth();
        west = regionMap.getWest();
        xres = regionMap.getXres();
        yres = regionMap.getYres();

        outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, null);

        double northing = south;
        double easting = west;
        for( int i = 0; i < cols; i++ ) {
            easting = easting + xres;
            for( int j = 0; j < rows; j++ ) {
                northing = northing + yres;
                Coordinate coordinate = new Coordinate();
                coordinate.x = west + i * xres;
                coordinate.y = south + j * yres;
                out.put(count, coordinate);
                count++;
            }
        }

        return out;
    }

    /**
     * Extract the coordinate of a FeatureCollection in a HashMap with an ID as
     * a key.
     * 
     * @param nStaz
     * @param collection
     * @throws Exception
     *             if a fiel of elevation isn't the same of the collection
     */
    private LinkedHashMap<Integer, Coordinate> getCoordinate( int nStaz, SimpleFeatureCollection collection, String idField )
            throws Exception {
        LinkedHashMap<Integer, Coordinate> id2CoordinatesMap = new LinkedHashMap<Integer, Coordinate>();
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
                coordinate.z = z;
                id2CoordinatesMap.put(name, coordinate);
            }
        } finally {
            iterator.close();
        }

        return id2CoordinatesMap;
    }

    /**
     * The gaussian variogram
     * 
     * @param c0
     *            nugget.
     * @param a
     *            range.
     * @param sill
     *            sill.
     * @param rx
     *            x distance.
     * @param ry
     *            y distance.
     * @param rz
     *            z distance.
     * @return the variogram value
     */
    private double variogram( double c0, double a, double sill, double rx, double ry, double rz ) {
        if (isNovalue(rz)) {
            rz = 0;
        }
        double value = 0;
        double h2 = Math.sqrt(rx * rx + rz * rz + ry * ry);
        if (pSemivariogramType == 0) {
            value = c0 + sill * (1 - Math.exp(-(h2 * h2) / (a * a)));
        }
        if (pSemivariogramType == 1) {
            // primotest semivariogram
            value = c0 + sill * (1 - Math.exp(-(h2) / (a)));
        }
        return value;
    }

    /**
     * 
     * @param rx
     *            x distance.
     * @param ry
     *            y distance.
     * @param rz
     *            z distance.
     * @return
     */
    private double variogram( double rx, double ry, double rz ) {
        if (isNovalue(rz)) {
            rz = 0;
        }
        double h2 = (rx / pIntegralscale[0]) * (rx / pIntegralscale[0]) + (ry / pIntegralscale[1]) * (ry / pIntegralscale[1])
                + (rz / pIntegralscale[2]) * (rz / pIntegralscale[2]);
        if (h2 < TOLL) {
            return pVariance;
        } else {
            return pVariance * Math.exp(-Math.sqrt(h2));
        }

    }

    /**
     * 
     * 
     * @param x
     *            the x coordinates.
     * @param y
     *            the y coordinates.
     * @param z
     *            the z coordinates.
     * @param n
     *            the number of the stations points.
     * @return
     */
    private double[][] covMatrixCalculating( double[] x, double[] y, double[] z, int n ) {
        double[][] ap = new double[n + 1][n + 1];
        if (defaultVariogramMode == 0) {
            for( int j = 0; j < n; j++ ) {
                for( int i = 0; i <= j; i++ ) {
                    double rx = x[i] - x[j];
                    double ry = y[i] - y[j];
                    double rz = 0;
                    if (pMode == 0) {
                        rz = z[i] - z[j];
                    }
                    double tmp = variogram(rx, ry, rz);

                    ap[j][i] = tmp;
                    ap[i][j] = tmp;

                }
            }
        } else if (defaultVariogramMode == 1) {
            for( int j = 0; j < n; j++ ) {
                for( int i = 0; i < n; i++ ) {
                    double rx = x[i] - x[j];
                    double ry = y[i] - y[j];
                    double rz = 0;
                    if (pMode == 0) {
                        rz = z[i] - z[j];
                    }
                    double tmp = variogram(pNug, pA, pS, rx, ry, rz);

                    ap[j][i] = tmp;
                    ap[i][j] = tmp;

                }
            }

        }
        for( int i = 0; i < n; i++ ) {
            ap[i][n] = 1.0;
            ap[n][i] = 1.0;

        }
        ap[n][n] = 0;
        return ap;

    }

    /**
     * 
     * @param x
     *            the x coordinates.
     * @param y
     *            the y coordinates.
     * @param z
     *            the z coordinates.
     * @param n
     *            the number of the stations points.
     * @return
     */
    private double[] knownTermsCalculation( double[] x, double[] y, double[] z, int n ) {

        double[] gamma = new double[n + 1];
        if (defaultVariogramMode == 0) {
            for( int i = 0; i < n; i++ ) {
                double rx = x[i] - x[n];
                double ry = y[i] - y[n];
                double rz = z[i] - z[n];
                gamma[i] = variogram(rx, ry, rz);
            }
        } else if (defaultVariogramMode == 1) {
            for( int i = 0; i < n; i++ ) {
                double rx = x[i] - x[n];
                double ry = y[i] - y[n];
                double rz = z[i] - z[n];
                gamma[i] = variogram(pNug, pA, pS, rx, ry, rz);
            }

        }
        gamma[n] = 1.0;
        return gamma;

    }

}
