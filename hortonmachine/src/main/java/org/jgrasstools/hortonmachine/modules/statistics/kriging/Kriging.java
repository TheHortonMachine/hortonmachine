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
package org.jgrasstools.hortonmachine.modules.statistics.kriging;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.SchemaException;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.math.matrixes.ColumnVector;
import org.jgrasstools.gears.utils.math.matrixes.LinearSystem;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description("Ordinary kriging algorithm.")
@Documentation("Kriging.html")
@Author(name = "Giuseppe Formetta, Daniele Andreis, Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com,  http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Kriging, Hydrology")
@Label(JGTConstants.STATISTICS)
@Name("kriging")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Kriging extends JGTModel {

	@Description("The vector of the measurement point, containing the position of the stations.")
	@In
	public SimpleFeatureCollection inStations = null;

	@Description("The field of the vector of stations, defining the id.")
	@In
	public String fStationsid = null;

	@Description("The field of the vector of stations, defining the elevation.")
	@In
	public String fStationsZ = null;

	@Description("The file with the measured data, to be interpolated.")
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
	@Description("The interpolation mode.")
	@In
	public int pMode = 0;

	/**
	 * The integral scale, this is necessary to calculate the variogram if the
	 * program use {@link Kriging2.variogram(rx,ry,rz)}.
	 */
	@Description("The integral scale.")
	@In
	public double[] pIntegralscale = null;

	/**
	 * Variance of the measure field.
	 */
	@Description("The variance.")
	@In
	public double pVariance = 0;

	/**
	 * The logarithm selector, if it's true then the models runs with the log of
	 * the data.
	 */
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

	@Description("The progress monitor.")
	@In
	public IJGTProgressMonitor pm = new LogProgressMonitor();

	public int defaultVariogramMode = 0;

	@Description("The type of theoretical semivariogram: 0 = Gaussian; 1 = Exponential.")
	@In
	public double semivariogramType = 0;

	@Description("Include zeros in computations: MODE 0=true MODE 1=false.")
	@In
	public double includezero = 0;

	@Description("The range if the models runs with the gaussian variogram.")
	@In
	public double pA;

	@Description("The sill if the models runs with the gaussian variogram.")
	@In
	public double pS;

	@Description("Is the nugget if the models runs with the gaussian variogram.")
	@In
	public double pNug;

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

	private HortonMessageHandler msg = HortonMessageHandler.getInstance();

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
    public void executeKriging() throws Exception {
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
                     * skip data for non existing stations, they are
                     * allowed. Also skip novalues.
                     */
                    continue;
                }
                if (defaultVariogramMode == 0) {
                	if(includezero==0){
                		if (Math.abs(h[0]) >= 0.0) { // TOLL
                			xStationList.add(coordinate.x);
                			yStationList.add(coordinate.y);
                			zStationList.add(z);
                			hStationList.add(h[0]);
                			n1 = n1 + 1;
                		}
                	}
                	if(includezero==1){
                        if (Math.abs(h[0]) > 0.0) { // TOLL
                            xStationList.add(coordinate.x);
                            yStationList.add(coordinate.y);
                            zStationList.add(z);
                            hStationList.add(h[0]);
                            n1 = n1 + 1;
                        }
                	}
                } else if (defaultVariogramMode == 1) {
                	if(includezero==0){
                		if (Math.abs(h[0]) >= 0.0) { // TOLL
                			xStationList.add(coordinate.x);
                			yStationList.add(coordinate.y);
                			zStationList.add(z);
                			hStationList.add(h[0]);
                			n1 = n1 + 1;
                		}
                	}
                	if(includezero==1){
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
                        zTmp, hTmp, i, false, pm);
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
        HashMap<Integer, Coordinate> pointsToInterpolateId2Coordinates = new HashMap<Integer, Coordinate>();
        int numPointToInterpolate = getNumPoint(inInterpolate);

        /*
         * if the isLogarithmic is true then execute the model with log value.
         */
        double[] result = new double[numPointToInterpolate];

        if (pMode == 0 || pMode == 1) {
            pointsToInterpolateId2Coordinates = getCoordinate(numPointToInterpolate, inInterpolate, fInterpolateid);
        } else if (pMode == 2) {
            throw new RuntimeException(msg.message("notImplemented"));
//             Raster grid = (Raster) 
//             gridToInterpolate.view(ViewType.GEOPHYSICS).getRenderedImage();
//             nRows = grid.getHeight();
//             nCols = grid.getWidth();
//             Envelope2D envelope2d = gridToInterpolate.getEnvelope2D();
//             double xMin = envelope2d.getMinX();
//             double yMin = envelope2d.getMinY();
//             numPointToInterpolate = nRows * nCols;
//             coordinateToInterpolate = getCoordinate(numPointToInterpolate, xMin, yMin);

        } else if (pMode == 3) {
            throw new RuntimeException(msg.message("notImplemented"));
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
        Set<Integer> pointsToInterpolateIdSet = pointsToInterpolateId2Coordinates.keySet();
        Iterator<Integer> idIterator = pointsToInterpolateIdSet.iterator();
        int j = 0;
        int[] idArray = new int[inInterpolate.size()];
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
                pm.beginTask(msg.message("kriging.working"), inInterpolate.size());
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
                pm.worked(1);
            } else if (n1 == 1 || areAllEquals) {
                double tmp = hStation[0];
                int k = 0;
                pm.message(msg.message("kriging.setequalsvalue"));
                pm.beginTask(msg.message("kriging.working"), inInterpolate.size());
                while( idIterator.hasNext() ) {
                    int id = idIterator.next();
                    result[k] = tmp;
                    idArray[k] = id;
                    k++;
                    pm.worked(1);
                }

            }
            pm.done();
            if (pMode == 0 || pMode == 1) {
                storeResult(result, idArray);
            } else {
                throw new RuntimeException("Not implemented"); // storeResult(result);
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
            if (pMode == 0 || pMode == 1) {
                storeResult(result, idArray);
            } else {
                throw new RuntimeException("Not implemented");

            }
        }
    }

	/**
	 * Verify the input of the model.
	 */
	private void verifyInput() {
		if (inData == null || inStations == null) {
			throw new NullPointerException(msg
					.message("kriging.stationproblem"));
		}
		if (pMode < 0 || pMode > 3) {
			throw new IllegalArgumentException(msg
					.message("kriging.defaultMode"));
		}
		if (pMode == 1 && (fStationsZ == null || fPointZ == null)) {
			pm.errorMessage(msg.message("kriging.noElevation"));
			throw new IllegalArgumentException(msg
					.message("kriging.noElevation"));
		}

		if (defaultVariogramMode != 0 && defaultVariogramMode != 1) {
			throw new IllegalArgumentException(msg
					.message("kriging.variogramMode"));
		}
		if (defaultVariogramMode == 0) {
			if (pVariance == 0 || pIntegralscale[0] == 0
					|| pIntegralscale[1] == 0 || pIntegralscale[2] == 0) {

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

		if ((pMode == 1 || pMode == 0) && inInterpolate == null) {
			throw new NullPointerException(msg.message("kriging.noPoint"));
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
	private void storeResult(double[] result2, int[] id) throws SchemaException {
		if (pMode == 0 || pMode == 1) {
			outData = new HashMap<Integer, double[]>();
			for (int i = 0; i < result2.length; i++) {
				outData.put(id[i], new double[] { result2[i] });
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
	// WritableRaster raster =
	// CoverageUtilities.createDoubleWritableRaster(nCols, nRows,
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
	// gridResult = CoverageUtilities.buildCoverage("interpolated", raster,
	// regionMap,
	// gridToInterpolate.getCoordinateReferenceSystem());
	// }
	//
	// }

	// /**
	// * Extract the coordinates to interpolate from a regular grid in 3D.
	// *
	// * @param numPointToInterpolate the amount of the points to interpolate
	// * @return an {@link HashMap} which contains, for each points, the
	// coordinate and its ID
	// */
	// private HashMap<Integer, Coordinate> getCoordinate( int
	// numPointToInterpolate, double minX,
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
	// * @return an {@link HashMap} which contains, for each points, the
	// coordinate and its ID
	// */
	// private HashMap<Integer, Coordinate> getCoordinate( int
	// numPointToInterpolate, double minX,
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
	 * 
	 * @param nStaz
	 * @param collection
	 * @throws Exception
	 *             if a fiel of elevation isn't the same of the collection
	 */
	private HashMap<Integer, Coordinate> getCoordinate(int nStaz,
			SimpleFeatureCollection collection, String idField)
			throws Exception {
		HashMap<Integer, Coordinate> id2CoordinatesMap = new HashMap<Integer, Coordinate>();
		FeatureIterator<SimpleFeature> iterator = collection.features();
		Coordinate coordinate = null;
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				int name = ((Number) feature.getAttribute(idField)).intValue();
				coordinate = ((Geometry) feature.getDefaultGeometry())
						.getCentroid().getCoordinate();
				double z = 0;
				if (fPointZ != null) {
					try {
						z = ((Number) feature.getAttribute(fPointZ))
								.doubleValue();
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
	 * Return the number of features.
	 * 
	 * @param collection
	 * @return
	 * @throws ModelsIOException
	 */
	private int getNumPoint(SimpleFeatureCollection collection)
			throws ModelsIOException {
		int nStaz = 0;
		if (collection != null) {
			nStaz = collection.size();
		}
		if (nStaz == 0) {
			throw new ModelsIOException(
					"Didn't find any point in the FeatureCollection", this
							.getClass().getSimpleName());
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
	private double variogram(double c0, double a, double sill, double rx,
			double ry, double rz) {
		if (isNovalue(rz)) {
			rz = 0;
		}
		double value = 0;
		double h2 = Math.sqrt(rx * rx + rz * rz + ry * ry);
		if (semivariogramType == 0) {
			value = c0 + sill * (1 - Math.exp(-(h2 * h2) / (a * a)));
		}
		if (semivariogramType == 1) {
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
	private double variogram(double rx, double ry, double rz) {
		if (isNovalue(rz)) {
			rz = 0;
		}
		double h2 = (rx / pIntegralscale[0]) * (rx / pIntegralscale[0])
				+ (ry / pIntegralscale[1]) * (ry / pIntegralscale[1])
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
	private double[][] covMatrixCalculating(double[] x, double[] y, double[] z,
			int n) {
		double[][] ap = new double[n + 1][n + 1];
		if (defaultVariogramMode == 0) {
			for (int j = 0; j < n; j++) {
				for (int i = 0; i <= j; i++) {
					double rx = x[i] - x[j];
					double ry = y[i] - y[j];
					double rz = 0;
					if (pMode == 1) {
						rz = z[i] - z[j];
					}
					double tmp = variogram(rx, ry, rz);

					ap[j][i] = tmp;
					ap[i][j] = tmp;

				}
			}
		} else if (defaultVariogramMode == 1) {
			for (int j = 0; j < n; j++) {
				for (int i = 0; i < n; i++) {
					double rx = x[i] - x[j];
					double ry = y[i] - y[j];
					double rz = 0;
					if (pMode == 1) {
						rz = z[i] - z[j];
					}
					double tmp = variogram(pNug, pA, pS, rx, ry, rz);

					ap[j][i] = tmp;
					ap[i][j] = tmp;

				}
			}

		}
		for (int i = 0; i < n; i++) {
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
	private double[] knownTermsCalculation(double[] x, double[] y, double[] z,
			int n) {

		double[] gamma = new double[n + 1];
		if (defaultVariogramMode == 0) {
			for (int i = 0; i < n; i++) {
				double rx = x[i] - x[n];
				double ry = y[i] - y[n];
				double rz = z[i] - z[n];
				gamma[i] = variogram(rx, ry, rz);
			}
		} else if (defaultVariogramMode == 1) {
			for (int i = 0; i < n; i++) {
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
