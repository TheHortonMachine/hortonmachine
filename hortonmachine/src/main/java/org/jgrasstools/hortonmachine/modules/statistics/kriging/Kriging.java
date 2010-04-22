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
package org.jgrasstools.hortonmachine.modules.statistics.kriging;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;
import oms3.annotations.Role;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IHMProgressMonitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import Jama.Matrix;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
/**
 * This class is an implementation of the ordinary kriging algorithm.
 * <p>
 * Kriging is a group of geostatistical techniques to interpolate the value of a
 * random field (e.g., the elevation, z, of the landscape as a function of the
 * geographic location) at an unobserved location from observations of its value
 * at nearby locations. (for more details see <a
 * href="http://en.wikipedia.org/wiki/Kriging">kriging</a>.
 *</p>
 *<p>
 * The input variables are:
 *<li>stations coordinates.
 *<li>measured value in each station.
 *<li>coordinates of the points where you want to interpolate the value.
 *<li>the choose of the variogram type (gaussian variogram or a variogram
 * calculated with an explicit integral scale.
 *The output are the variable value in the requested points.
 *</p>
 * @author <a href="mailto:daniele.andreis@gmail.com>daniele andreis</a>,
 *         giuseppe formetta.
 * @author Andrea Antonello (www.hydrologis.com).
 */
public class Kriging extends JGTModel {

    @Description("The collection of the measurement point, containing the position of the station.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inStations = null;

    @Description("The field of the stations collections, defining the id.")
    @In
    public String fStationsid = null;

    @Description("The measured data, to be interpolated.")
    @In
    public HashMap<Integer, double[]> inData = null;

    @Description("The collection of the points in which the data needs to be interpolated.")
    @In
    public FeatureCollection<SimpleFeatureType, SimpleFeature> inInterpolate = null;

    @Description("The field of the interpolated points collections, defining the id.")
    @In
    public String fInterpolateid = null;

    @Description("The interpolated data.")
    @Out
    public HashMap<Integer, double[]> outData = null;

    /**
     * Define the mode. It is possible 4 alternatives: <li>mode ==0, the value
     * to calculate are in a non-regular grid (the coordinates are stored in a
     * {@link FeatureCollection}, pointsToInterpolate. This is a 2-D
     * interpolation, so the z coordinates are null. <li>mode ==1, the value to
     * calculate are in a non-regular grid (the coordinates are stored in a
     * {@link FeatureCollection}, pointsToInterpolate. This is a 3-D
     * interpolation.. <li>mode ==2, the value to calculate are in a regular
     * grid (the coordinates are stored in a {@link GridCoverage2D},
     * gridToInterpolate. This is a 2-D interpolation. <li>mode ==3, the value
     * to calculate are in a regular grid (the coordinates are stored in a
     * {@link GridCoverage2D}, gridToInterpolate. This is a 3-D interpolation,
     * so the grid have to contains a dem.
     */
    @Role(Role.PARAMETER)
    @Description("The interpolation mode.")
    @In
    public int pMode = 0;

    /**
     * The integral scale, this is necessary to calculate the variogram if the
     * program use {@link Kriging2.variogram(rx,ry,rz)}.
     */
    @Role(Role.PARAMETER)
    @Description("The integral scale.")
    @In
    public double[] pIntegralscale = null;

    /**
     * Variance of the measure field.
     */
    @Role(Role.PARAMETER)
    @Description("The variance.")
    @In
    public double pVariance = 0;

    /**
     * The logarithm selector, if it's true then the models runs with the log of
     * the data.
     */
    @Role(Role.PARAMETER)
    @Description("Switch for logaritmic run selection.")
    @In
    public boolean doLogarithmic = false;

    /**
     * The output: the calculated value in mode 2 or 3.
     */
    // @Description("The interpolated data.")
    // @Out
    // public GridCoverage2D gridResult = null;

    /**
     * The input: the points where the variable is calculated in mode 2 or 3.
     */
    // @Description("The collection of the points in which the data needs to be interpolated.")
    // @In
    // public GridCoverage2D gridToInterpolate = null;

    @Role(Role.PARAMETER)
    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    private int variogramMode = 0;

    /**
     * The range if the models runs with the gaussian variogram.
     */
    private double a = 0.04943906;

    /**
     * The sill if the models runs with the gaussian variogram.
     */
    private double s = 0.6806828;

    /**
     * Is the nugget if the models runs with the gaussian variogram.
     */
    private double nug = 0.0;

    /**
     * the number of rows if the mode is 2 or 3.
     */
    // private int nRows = 0;

    /**
     * the number of columns if the mode is 2 or 3.
     */
    // private int nCols = 0;

    /**
     *A tolerance.
     */
    private static final double TOLL = 1.0d * 10E-8;

    /**
     */
    // private double xRes = 0.0;

    // private double yRes = 0.0;

    /**
     * Executing ordinary kriging.
     * <p>
     * <li>Verify if the parameters are correct.
     * <li>Calculating the matrix of the covariance (a).
     * <li>For each point to interpolated, evalutate the know term vector (b) and solve the system (a x)=b where x is the weight. 
     * </p>
     * @throws SchemaException
     */

    @Execute
    public void executeKriging() throws Exception {
        
        System.out.println("Kriging processing");
        
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
                int id = ((Number) feature.getAttribute(fStationsid)).intValue();
                Coordinate coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid()
                        .getCoordinate();
                double[] h = inData.get(id);
                if (h == null || isNovalue(h[0])) {
                    /*
                     * skip data for non existing stations, they are
                     * allowed. Also skip novalues.
                     */
                    continue;
                }
                if (Math.abs(h[0]) >= TOLL) {
                    xStationList.add(coordinate.x);
                    yStationList.add(coordinate.y);
                    zStationList.add(coordinate.z);
                    hStationList.add(h[0]);
                    n1 = n1 + 1;
                }
            }
        } finally {
            inStations.close(stationsIter);
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
        for( int i = 0; i < xStation.length - 1; i++ ) {
            xStation[i] = xStationList.get(i);
            yStation[i] = yStationList.get(i);
            zStation[i] = zStationList.get(i);
            hStation[i] = hStationList.get(i);
        }

        /*
         * if the isLogarithmic is true then execute the model with log value.
         */
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
        HashMap<Integer, Coordinate> pointsToInterpolateId2Coordinates = new HashMap<Integer, Coordinate>();
        int numPointToInterpolate = getNumPoint(inInterpolate);
        if (pMode == 0 || pMode == 1) {
            pointsToInterpolateId2Coordinates = getCoordinate(numPointToInterpolate, inInterpolate,
                    fInterpolateid);
        } else if (pMode == 2) {
            throw new RuntimeException("Not implemented yet!");
            // Raster grid = (Raster)
            // gridToInterpolate.view(ViewType.GEOPHYSICS).getRenderedImage();
            // nRows = grid.getHeight();
            // nCols = grid.getWidth();
            // Envelope2D envelope2d = gridToInterpolate.getEnvelope2D();
            // double xMin = envelope2d.getMinX();
            // double yMin = envelope2d.getMinY();
            // numPointToInterpolate = nRows * nCols;
            // coordinateToInterpolate = getCoordinate(numPointToInterpolate, xMin, yMin);

        } else if (pMode == 3) {
            throw new RuntimeException("Not implemented yet!");
            // Raster grid = (Raster)
            // gridToInterpolate.view(ViewType.GEOPHYSICS).getRenderedImage();
            // nRows = grid.getHeight();
            // nCols = grid.getWidth();
            // Envelope2D envelope2d = gridToInterpolate.getEnvelope2D();
            // double xMin = envelope2d.getMinX();
            // double yMin = envelope2d.getMinY();
            // numPointToInterpolate = nRows * nCols;
            // coordinateToInterpolate = getCoordinate(numPointToInterpolate, xMin, yMin, grid);
        }

        /*
         * initialize the solution and its variance vector.
         */
        double[] result = new double[numPointToInterpolate];

        Set<Integer> pointsToInterpolateIdSet = pointsToInterpolateId2Coordinates.keySet();
        Iterator<Integer> idIterator = pointsToInterpolateIdSet.iterator();
        int j = 0;
        int[] idArray = new int[inInterpolate.size()];
        while( idIterator.hasNext() ) {
            double sum = 0.;
            int id = idIterator.next();
            idArray[j] = id;
            Coordinate coordinate = (Coordinate) pointsToInterpolateId2Coordinates.get(id);
            xStation[n1] = coordinate.x;
            yStation[n1] = coordinate.y;
            zStation[n1] = coordinate.z;
            /*
             * calculating the right hand side of the kriging linear system.
             */
            double[] knowsTerm = knowsTermsCalculating(xStation, yStation, zStation, n1);

            /*
             * solve the linear system, where the result is the weight.
             */
            Matrix a = new Matrix(covarianceMatrix);
            Matrix b = new Matrix(knowsTerm, knowsTerm.length);
            Matrix x = a.solve(b);
            double[] moltiplicativeFactor = x.getColumnPackedCopy();
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
                throw new ModelsRuntimeException("Error in the coffeicients calculation", this
                        .getClass().getSimpleName());
            }
        }

        if (pMode == 0 || pMode == 1) {
            storeResult(result, idArray);
        } else {
            // storeResult(result);
        }
    }

    /**
     * Verify the input of the model.
     */
    private void verifyInput() {
        if (inData == null || inStations == null) {
            throw new NullPointerException(
                    "problema nelle stazioni di misura, non ci sono stazioni o dati");
        }
        if (pMode < 0 || pMode > 3) {
            throw new IllegalArgumentException("Modalita' di esecuzione non esistente");
        }
        if (variogramMode != 0 && variogramMode != 1) {
            throw new IllegalArgumentException(
                    "Tipo di variogramma sbagliato, imposta variogramMode");
        }
        if (variogramMode == 0) {
            if (pVariance == 0 || pIntegralscale[0] == 0 || pIntegralscale[1] == 0
                    || pIntegralscale[2] == 0) {

                pm
                        .errorMessage("Attenzione una dei parametri del modello potrebbe essere non settata");
                pm.errorMessage("varianza " + pVariance);
                pm.errorMessage("Scala Integrale x " + pIntegralscale[0]);
                pm.errorMessage("Scala Integrale y " + pIntegralscale[1]);
                pm.errorMessage("Scala Integrale z " + pIntegralscale[2]);
            }
        }
        if (variogramMode == 1) {
            if (nug == 0 || s == 0 || a == 0) {
                pm
                        .errorMessage("Attenzione una dei parametri del modello potrebbe essere non settata");
                pm.errorMessage("Nugget " + nug);
                pm.errorMessage("Sill " + s);
                pm.errorMessage("Range " + a);
            }
        }

        if ((pMode == 1 || pMode == 0) && inInterpolate == null) {
            throw new NullPointerException("problema nei punti da interpolarei");
        }
        // if ((mode == 2 || mode == 3) && gridToInterpolate == null) {
        // throw new NullPointerException("problema nei punti da interpolare");
        // }

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
        if (pMode == 0 || pMode == 1) {
            outData = new HashMap<Integer, double[]>();
            for( int i = 0; i < result2.length; i++ ) {
                outData.put(id[i], new double[]{result2[i]});
            }
        }
    }

    // /**
    // * Store the result in a GridCoverage(if the mode is 2 or 3).
    // *
    // * @param result2
    // * the result of the model
    // * @throws SchemaException
    // * @throws SchemaException
    // */
    // private void storeResult( double[] result2 ) {
    //
    // if (mode == 2 || mode == 3) {
    // WritableRaster raster = CoverageUtilities.createDoubleWritableRaster(nCols, nRows,
    // null, null, null);
    // WritableRectIter rectIter = RectIterFactory.createWritable(raster, null);
    // int i = 0;
    // rectIter.startLines();
    // do {
    // rectIter.startPixels();
    // i = 0;
    // do {
    // rectIter.setSample(result2[i]);
    // i++;
    // } while( !rectIter.nextPixelDone() );
    //
    // } while( !rectIter.nextLineDone() );
    //
    // HashMap<String, Double> regionMap = CoverageUtilities
    // .getRegionParamsFromGridCoverage(gridToInterpolate);
    // gridResult = CoverageUtilities.buildCoverage("interpolated", raster, regionMap,
    // gridToInterpolate.getCoordinateReferenceSystem());
    // }
    //
    // }

    // /**
    // * Extract the coordinates to interpolate from a regular grid in 3D.
    // *
    // * @param numPointToInterpolate the amount of the points to interpolate
    // * @return an {@link HashMap} which contains, for each points, the coordinate and its ID
    // */
    // private HashMap<Integer, Coordinate> getCoordinate( int numPointToInterpolate, double minX,
    // double minY, Raster grid ) {
    // Coordinate coordinate = new Coordinate();
    // HashMap<Integer, Coordinate> coord = new HashMap<Integer, Coordinate>();
    //
    // // gridToInterpolate.
    // int count = 0;
    //
    // for( int j = 0; j < nRows; j++ ) {
    // for( int i = 0; i < nCols; i++ ) {
    // coordinate.x = minX + i * xRes;
    // coordinate.y = minY + j * yRes;
    // coordinate.z = grid.getSampleDouble(i, j, 0);
    // count++;
    // coord.put(count, coordinate);
    // }
    // }
    // return coord;
    // }

    // /**
    // * * Extract the coordinates to interpolate from a regular grid in 2D.
    // *
    // * @param numPointToInterpolate
    // * @return an {@link HashMap} which contains, for each points, the coordinate and its ID
    // */
    // private HashMap<Integer, Coordinate> getCoordinate( int numPointToInterpolate, double minX,
    // double minY ) {
    // Coordinate coordinate = new Coordinate();
    // HashMap<Integer, Coordinate> coord = new HashMap<Integer, Coordinate>();
    // int count = 0;
    // for( int j = 0; j < nRows; j++ ) {
    // for( int i = 0; i < nCols; i++ ) {
    // coordinate.x = minX + i * xRes;
    // coordinate.y = minY + j * yRes;
    // count++;
    // coord.put(count, coordinate);
    //
    // }
    // }
    // return coord;
    // }

    /**
     * Extract the coordinate of a FeatureCollection in a HashMap with an ID as
     * a key.
     * @param nStaz
     * @param collection
     */
    private HashMap<Integer, Coordinate> getCoordinate( int nStaz,
            FeatureCollection<SimpleFeatureType, SimpleFeature> collection, String idField ) {
        HashMap<Integer, Coordinate> id2CoordinatesMap = new HashMap<Integer, Coordinate>();
        FeatureIterator<SimpleFeature> iterator = collection.features();
        Coordinate coordinate = null;
        try {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                int name = ((Number) feature.getAttribute(idField)).intValue();
                coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid()
                        .getCoordinate();
                id2CoordinatesMap.put(name, coordinate);
            }
        } finally {
            collection.close(iterator);
        }

        return id2CoordinatesMap;
    }

    /**
     * Return the number of features.
     * 
     * @param collection
     * @return
     * @throws ModelsIOException 
     */
    private int getNumPoint( FeatureCollection<SimpleFeatureType, SimpleFeature> collection )
            throws ModelsIOException {
        int nStaz = 0;
        if (collection != null) {
            nStaz = collection.size();
        }
        if (nStaz == 0) {
            throw new ModelsIOException("Didn't find any input station", this.getClass()
                    .getSimpleName());
        }
        return nStaz;
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
        double h2 = Math.sqrt(rx * rx + rz * rz + ry * ry);
        return c0 + sill * (1 - Math.exp(-(h2 * h2) / (a * a)));

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
        double h2 = (rx / pIntegralscale[0]) * (rx / pIntegralscale[0]) + (ry / pIntegralscale[1])
                * (ry / pIntegralscale[1]) + (rz / pIntegralscale[2]) * (rz / pIntegralscale[2]);
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
        if (variogramMode == 0) {
            for( int j = 0; j < n; j++ ) {
                for( int i = 0; i <= j; i++ ) {
                    double rx = x[i] - x[j];
                    double ry = y[i] - y[j];
                    double rz = z[i] - z[j];
                    double tmp = variogram(rx, ry, rz);

                    ap[j][i] = tmp;
                    ap[i][j] = tmp;

                }
            }
        } else if (variogramMode == 1) {
            for( int j = 0; j < n; j++ ) {
                for( int i = 0; i < n; i++ ) {
                    double rx = x[i] - x[j];
                    double ry = y[i] - y[j];
                    double rz = z[i] - z[j];
                    double tmp = variogram(nug, a, s, rx, ry, rz);

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
    private double[] knowsTermsCalculating( double[] x, double[] y, double[] z, int n ) {

        double[] gamma = new double[n + 1];
        if (variogramMode == 0) {
            for( int i = 0; i < n; i++ ) {
                double rx = x[i] - x[n];
                double ry = y[i] - y[n];
                double rz = z[i] - z[n];
                gamma[i] = variogram(rx, ry, rz);
            }
        } else if (variogramMode == 1) {
            for( int i = 0; i < n; i++ ) {
                double rx = x[i] - x[n];
                double ry = y[i] - y[n];
                double rz = z[i] - z[n];
                gamma[i] = variogram(nug, a, s, rx, ry, rz);
            }

        }
        gamma[n] = 1.0;
        return gamma;

    }

}
