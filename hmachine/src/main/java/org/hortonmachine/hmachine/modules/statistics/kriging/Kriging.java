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

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.utils.math.matrixes.ColumnVector;
import org.hortonmachine.gears.utils.math.matrixes.MatrixException;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.hortonmachine.hmachine.modules.statistics.kriging.interpolationdata.InterpolationDataProvider;
import org.hortonmachine.hmachine.modules.statistics.kriging.linearsystemsolver.SimpleLinearSystemSolverFactory;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationProcessor;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationsSelection;
import org.hortonmachine.hmachine.modules.statistics.kriging.utilities.Utility;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.TheoreticalVariogram;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.VariogramParameters;
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
import oms3.annotations.Status;

/**
 * Abstract class implementing the core Ordinary Kriging algorithm.
 *
 * <p>
 * This class performs the following steps:
 * <ol>
 * <li>Input verification and variogram parameter setup</li>
 * <li>Station selection based on distance and other criteria</li>
 * <li>Calculation of the covariance matrix and known terms vector</li>
 * <li>Solving the linear system to obtain the kriging weights</li>
 * <li>Computing the interpolated value (including trend adjustment, if
 * enabled)</li>
 * <li>Final validation and storage of results</li>
 * </ol>
 * </p>
 *
 * <p>
 * <strong>SUGGESTION:</strong> Consider refining exception handling throughout
 * the class to use custom exceptions (e.g.,
 * <code>InterpolationDataException</code>) instead of generic
 * NullPointerException.
 * </p>
 */
@Description("Ordinary kriging algorithm.")
@Documentation("Kriging.html")
@Author(name = "Giuseppe Formetta, Daniele Andreis, Silvia Franceschi, Andrea Antonello, Marialaura Bancheri & Francesco Serafin")
@Keywords("Kriging, Hydrology")
@Label("")
@Name("kriging")
@Status()
@License("General Public License Version 3 (GPLv3)")
@SuppressWarnings("nls")
public abstract class Kriging extends HMModel {

	@Description("The .shp of the measurement point, containing the position of the stations.")
	@In
	public SimpleFeatureCollection inStations = null;

	@Description("The field of the vector of stations, defining the id.")
	@In
	public String fStationsid = null;

	@Description("The field of the vector of stations, defining the elevation.")
	@In
	public String fStationsZ = null;

	@Description("The type of theoretical semivariogram: exponential, gaussian, spherical, pentaspherical"
			+ "linear, circular, bessel, periodic, hole, logaritmic, power, spline")
	@In
	public String pSemivariogramType = null;

	@Description("The HM with the measured data to be interpolated.")
	@In
	public HashMap<Integer, double[]> inData = null;

	@Description("The progress monitor.")
	@In
	public IHMProgressMonitor pm = new LogProgressMonitor();

	@Description("Include zeros in computations (default is true).")
	@In
	public boolean doIncludeZero = true;

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

	@Description("Degree of polynomial regression, default is 1")
	@In
	public final static int REGRESSION_ORDER = 1;

	@Description("Type of linear system solver")
	@In
	public String linearSystemSolverType = "math3";

	/** transform to log. */
	@In
	public boolean doLogarithmic = false;

	
	/** transform to log. */
	@In
	public boolean boundedToZero = false;

	@Description("The Experimental Variogram.")
	@In
	public HashMap<Integer, double[]> inTheoreticalVariogram;

	private static final double TOLL = 10E-7;

	private int step = 0;

	protected HortonMessageHandler msg = HortonMessageHandler.getInstance();

	/** The id of the cosidered station */
	int id;

	@In
	public String fileNoValue = "-9999";

	@In
	public String tStart = null;

	@In
	public int tTimeStep = 60;

	@In
	public double inIntercept = 0.0;
	@In
	public double inSlope = 0;
	@In
	public boolean parallelComputation = false;

	private VariogramParameters variogramParameters;

	protected InterpolationDataProvider provider = null;

	@Execute
	public void execute() throws Exception {
		if (parallelComputation) {
			this.executeKrigingParallel();
		} else {
			this.executeKriging();
		}

	}
	
	
		

	

	/**
	 * Executes Kriging algorithm.
	 * <p>
	 * Steps:
	 * <ol>
	 * <li>Initialize parameters and verify inputs.</li>
	 * <li>Extract interpolation points using the provider.</li>
	 * <li>Select stations using a StationsSelection object.</li>
	 * <li>Determine variogram parameters.</li>
	 * <li>Compute weights by solving the linear system.</li>
	 * <li>Calculate the interpolated value.</li>
	 * <li>Post-process and store results.</li>
	 * </ol>
	 * </p>
	 *
	 * @throws Exception if any error occurs during the kriging execution.
	 */
	private void executeKriging() throws Exception {
		// SUGGESTION: Consider logging the start of execution.
		// Initialization and verification
		VariogramParameters vp = initializeKrigingParameters();
		// Extract interpolation points from input feature collection

		LinkedHashMap<Integer, Coordinate> pointsToInterpolate = provider.getCoordinates();
		Set<Integer> pointsToInterpolateIdSet = pointsToInterpolate.keySet();
		Iterator<Integer> idIterator = pointsToInterpolateIdSet.iterator();
		double[] result = new double[pointsToInterpolate.size()];

		/**
		 * StationsSelection is an external class that allows the selection of the
		 * stations involved in the study. It is possible to define if to include
		 * stations with zero values, station in a define neighborhood or within a max
		 * distance from the considered point.
		 */
		StationsSelection stations = this.createAndConfigureStationSelection();
		determineVariogram(vp);
		StationProcessor sp = this.createStationProcessor(stations, variogramParameters);
		// If no neighborhood criteria is provided, update without spatial filtering.
		if (maxdist == 0 && inNumCloserStations == 0) {
			sp.updateForCoordinate(null, inData, 0, 0);
		}
		int j = 0;
		while (idIterator.hasNext()) {
			double interpolatedValue = -9999.0;
			id = idIterator.next();
			if (variogramParameters != null) {
				try {

					Coordinate coordinate = pointsToInterpolate.get(id);
					if (maxdist > 0 || inNumCloserStations > 0) {
						sp.updateForCoordinate(coordinate, inData, inNumCloserStations, maxdist);
					}
					int n1 = sp.getCount();
					boolean areAllEquals = sp.areAllEquals();

					if (n1 != 0) {

						if (!areAllEquals && n1 > 1) {

							interpolatedValue = interpolateValue(sp, coordinate);
							// pm.worked(1);
						} else if (n1 == 1 || areAllEquals) {
							interpolatedValue = sp.getHResiduals()[0];
							// pm.message(msg.message("kriging.setequalsvalue"));
							// pm.beginTask(msg.message("kriging.working"),
							// pointsToInterpolateId2Coordinates.size());
							// (SUGGESTION: Clarify purpose of this reset).
							n1 = 0;
							// pm.worked(1);
						}

						// pm.done();

					} else {

						// TODO make this a more informative message or throw an exception if appropriate.
//						pm.errorMessage("No value for this time step");

						interpolatedValue = inData.values().iterator().next()[0];

					}
				} catch (Exception e) {
					pm.errorMessage(e.getMessage());

				}
			}
			result[j] = postProcessResult(interpolatedValue);
			j++;
		}
		storeResult(result, pointsToInterpolate);

	}

	private void executeKrigingParallel() throws Exception {
		// Initialization and verification (assumed to be thread-safe or immutable)
		VariogramParameters vp = initializeKrigingParameters();

		LinkedHashMap<Integer, Coordinate> pointsMap = provider.getCoordinates();
		// Convert the entry set to a list for parallel processing.
		List<Map.Entry<Integer, Coordinate>> entries = new ArrayList<>(pointsMap.entrySet());
		double[] result = new double[entries.size()];

		List<Pair<Integer, Map.Entry<Integer, Coordinate>>> indexedEntries = new LinkedList<>();
		for (int i = 0; i < entries.size(); i++) {
			indexedEntries.add(new Pair<>(i, entries.get(i)));
		}

		determineVariogram(vp);

		// Decide whether to use a shared instance based on the parameters.
		final boolean useSharedInstance = (maxdist == 0 && inNumCloserStations == 0);

		indexedEntries.parallelStream().forEach(pair -> {
			double interpolatedValue = -9999.0;
			if (variogramParameters != null) {
				try {
					StationsSelection stations = createAndConfigureStationSelection();
					StationProcessor sp = new StationProcessor(stations, variogramParameters);
					Coordinate coordinate = pair.getValue().getValue();
					int i = pair.getKey();
					// Each thread creates its own instance of StationsSelection.
					// Variogram parameters might need to be recalculated per thread if mutable.
					try {
						if (useSharedInstance) {
							sp.updateForCoordinate(null, inData, inNumCloserStations, maxdist);
						} else {

							sp.updateForCoordinate(coordinate, inData, inNumCloserStations, maxdist);
						}
					} catch (Exception e) {
						e.printStackTrace();
						// pm.errorMessage("Error updating stations for kriging: " + e.getMessage());
						throw new ModelsRuntimeException("Error updating stations for kriging.",
								this.getClass().getSimpleName());
					}

					int n1 = sp.getCount();
					boolean areAllEquals = sp.areAllEquals();

					if (n1 != 0) {
						if (!areAllEquals && n1 > 1) {
							try {
								interpolatedValue = interpolateValue(sp, coordinate);
							} catch (MatrixException e) {
								throw new ModelsRuntimeException("Error solving kriging system.",
										this.getClass().getSimpleName());
							}
						} else if (n1 == 1 || areAllEquals) {
							interpolatedValue = sp.getHResiduals()[0];
						} else {
							// Fallback: inData stores a single double[] value; use its first entry.
							interpolatedValue = inData.values().iterator().next()[0];
						}
					} else {
						// TODO make this a more informative message or throw an exception if appropriate.
//						pm.errorMessage("No value for this time step");
						// inData stores a single double[] value; use its first entry.
						interpolatedValue = inData.values().iterator().next()[0];
					}

					// Post-process and store the result.
					result[i] = postProcessResult(interpolatedValue);
				} catch (Exception e) {
					pm.errorMessage(e.getMessage());

				}
			}
		});

		// Store results after all parallel tasks have completed.
		storeResult(result, pointsMap);
	}

	protected StationProcessor createStationProcessor(StationsSelection stations, VariogramParameters vp) {
		return new StationProcessor(stations, vp);
	}

	private double interpolateValue(StationProcessor sp, Coordinate coordinate) throws MatrixException {
		double interpolatedValue;
		double[] xStations = sp.getXStations();
		double[] yStations = sp.getYStations();
		double[] zStations = sp.getZStations();
		double[] hResiduals = sp.getHResiduals();
		int n1 = sp.getCount();
		xStations[n1] = coordinate.x;
		yStations[n1] = coordinate.y;
		zStations[n1] = coordinate.z;
		// pm.beginTask(msg.message("kriging.working"),0);

		double h0 = 0.0;

		/*
		 * calculating the covariance matrix.
		 */
		double[][] covarianceMatrix = covMatrixCalculating(xStations, yStations, zStations, n1);

		double[] knownTerm = knownTermsCalculation(xStations, yStations, zStations, n1);

		/*
		 * solve the linear system, where the result is the weight
		 * (moltiplicativeFactor).
		 */
		ColumnVector solution = SimpleLinearSystemSolverFactory.solve(knownTerm, covarianceMatrix,
				linearSystemSolverType);

		double[] moltiplicativeFactor = solution.copyValues1D();
		double sum = 0;
		for (int k = 0; k < n1; k++) {
			h0 = h0 + moltiplicativeFactor[k] * hResiduals[k];

			// sum is computed to check that
			// the sum of all the weights is 1
			sum = sum + moltiplicativeFactor[k];

		}
		// System.out.println(doDetrended);
		if (sp.getIsTrend()) {
			double trend = coordinate.z * sp.getTrendCoeff() + sp.getTrendIntercept();
			h0 = h0 + trend;
		}
		interpolatedValue = h0;

		if (Math.abs(sum - 1) >= TOLL) {
			throw new ModelsRuntimeException("Error in the coffeicients calculation", this.getClass().getSimpleName());
		}
		return interpolatedValue;
	}

	/**
	 * Applies post-processing to the interpolated value. This includes reversing a
	 * logarithmic transformation and bounding negative values to zero.
	 *
	 * @param interpolatedValue The raw interpolated value.
	 * @return The post-processed interpolated value.
	 */
	private double postProcessResult(double interpolatedValue) {
		if (!isNovalue(interpolatedValue)) {
			if (doLogarithmic) {
				interpolatedValue = Utility.getInverseLog(interpolatedValue);
			}
			if (boundedToZero && interpolatedValue < 0 && !isNovalue(interpolatedValue)) {
				interpolatedValue = 0;
			}
		}
		return interpolatedValue;
	}

	/**
	 * Determines the variogram parameters based on the provided theoretical
	 * variogram data.
	 *
	 * @param vp       The variogram parameters built from user input.
	 * @param stations The StationsSelection object containing station information.
	 */
	private void determineVariogram(VariogramParameters vp) {

		if (inTheoreticalVariogram != null && inTheoreticalVariogram.get(0)[0] != -9999) {
			variogramParameters = new VariogramParameters.Builder(
					VariogramParameters.getVariogramType(inTheoreticalVariogram.get(5)[0]),
					inTheoreticalVariogram.get(0)[0], inTheoreticalVariogram.get(2)[0],
					inTheoreticalVariogram.get(1)[0]).setLocal(inTheoreticalVariogram.get(3)[0])
					.setTrend(inTheoreticalVariogram.get(4)[0]).setTrendIntercept(inTheoreticalVariogram.get(6)[0])
					.setTrendSlope(inTheoreticalVariogram.get(7)[0]).build();
		} else if (vp.isValid()) {
			variogramParameters = vp;
		} else {
			variogramParameters = null;
			pm.errorMessage("no variogram provided");
		}

	}

	/**
	 * Initializes kriging parameters and verifies input.
	 *
	 * @return The constructed VariogramParameters.
	 */
	private VariogramParameters initializeKrigingParameters() {
		VariogramParameters vp = new VariogramParameters.Builder(pSemivariogramType, nugget, range, sill)
				.setLocal(false).setTrend(doDetrended).setTrendIntercept(inIntercept).setTrendSlope(inSlope).build();
		if (step == 0) {
			verifyInput();
		}
		step = step + 1;
		if (provider == null) {
			provider = initializeInterpolatorData();
		}
		return vp;
	}

	/**
	 * Creates and configures a StationsSelection object using the input stations.
	 *
	 * @return A configured StationsSelection object.
	 */
	protected StationsSelection createAndConfigureStationSelection() {
		StationsSelection stations = new StationsSelection();
		stations.inStations = inStations;
		stations.maxdist = maxdist;
		stations.fStationsid = fStationsid;
		stations.fStationsZ = fStationsZ;
		stations.doLogarithmic = doLogarithmic;
		stations.doIncludezero = doIncludeZero;
		return stations;
	}

	/**
	 * Validates the essential inputs for the kriging model. In detrended mode, both
	 * station and interpolation point elevation fields must be provided.
	 */
	protected void verifyInput() {

		requireNonNull(inData, "Missing input: inData (input raster/grid) is null.");
		requireNonNull(inStations, "Missing input: inStations (stations feature collection) is null.");
		requireNonNull(fStationsid, "Missing input: fStationsid (station id field) is null.");
		if (doDetrended) {
			requireNonNull(fStationsZ,
					"Detrending is enabled (doDetrended=true) but fStationsZ (elevation field) is null.");

			int idx = inStations.getSchema().indexOf(fStationsZ);
			if (idx < 0) {
				throw new IllegalArgumentException(
						"Detrending is enabled but field '" + fStationsZ + "' was not found in inStations schema. "
								+ "Check the field name (case-sensitive) and the input stations layer.");
			}
		}

		boolean anyFixed = (nugget != 0.0) || (sill != 0.0) || (range != 0.0);
		boolean typeMissing = (pSemivariogramType == null) || pSemivariogramType.isBlank();

		if (anyFixed && typeMissing) {
			throw new IllegalArgumentException(
					"Incomplete fixed semivariogram parameters: you set at least one of nugget/sill/range "
							+ "but pSemivariogramType is missing or blank. "
							+ "Provide pSemivariogramType (e.g., 'Spherical', 'Exponential', ...) or set nugget=sill=range=0.");
		}
	}

	private static <T> T requireNonNull(T obj, String message) {
		if (obj == null)
			throw new IllegalArgumentException(message);
		return obj;
	}

	/**
	 * Round.
	 *
	 * @param value  is the value of the variable considered
	 * @param places the places to consider after the comma
	 * @return the double value of the variable rounded
	 */
	public static double round(double value, int places) {
		if (places < 0) {
			throw new IllegalArgumentException();
		}

		long factor = (long) Math.pow(10, places);
		value = value * factor;
		long tmp = Math.round(value);
		return (double) tmp / factor;
	}

	/**
	 * Calculates the covariance matrix using the theoretical variogram. Each
	 * element [i][j] is computed via TheoreticalVariogram.calculateVGMxyz, which
	 * measures the spatial dissimilarity between points.
	 *
	 * @param x the x coordinates.
	 * @param y the y coordinates.
	 * @param z the z coordinates.
	 * @param n the number of the stations points.
	 * @return the double[][] matrix with the covariance
	 */
	private double[][] covMatrixCalculating(double[] x, double[] y, double[] z, int n) {
		double[][] covarianceMatrix = new double[n + 1][n + 1];
		for (int j = 0; j < n; j++) {
			for (int i = 0; i < n; i++) {
				covarianceMatrix[j][i] = TheoreticalVariogram.calculateVGMxyz(variogramParameters, x[i], y[i], z[i],
						x[j], y[j], z[j]);
				covarianceMatrix[i][j] = covarianceMatrix[j][i];
			}
		}
		for (int i = 0; i < n; i++) {
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
	private double[] knownTermsCalculation(double[] x, double[] y, double[] z, int n) {
		// known terms vector
		double[] gamma = new double[n + 1];
		for (int i = 0; i < n; i++) {
			gamma[i] = TheoreticalVariogram.calculateVGMxyz(variogramParameters, x[i], y[i], z[i], x[n], y[n], z[n]);
		}
		gamma[n] = 1.0;
		return gamma;
	}

	/**
	 * Abstract method to store the interpolation result. Implementations should
	 * store the result array mapped by unique point IDs.
	 *
	 * @param result                     The array of interpolated values.
	 * @param interpolatedCoordinatesMap A HashMap of interpolated coordinates.
	 */
	protected abstract void storeResult(double[] result, HashMap<Integer, Coordinate> interpolatedCoordinatesMap);

	/**
	 * Abstract method to initialize the interpolation data provider.
	 * Implementations should return a provider that extracts coordinates from the
	 * input data.
	 *
	 * @return An InterpolationDataProvider instance.
	 */
	protected abstract InterpolationDataProvider initializeInterpolatorData();

}
