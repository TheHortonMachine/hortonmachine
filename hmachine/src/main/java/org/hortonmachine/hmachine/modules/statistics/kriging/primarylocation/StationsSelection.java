package org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.utils.sorting.QuickSortAlgorithm;
import org.hortonmachine.hmachine.modules.statistics.kriging.utilities.Utility;

/*
 * Niccol� Tubini 28-12-2021
 * Replaced with lines 40 and 41
 * https://sourceforge.net/p/geotools/mailman/message/36652855/
 */
//import com.vividsolutions.jts.geom.Coordinate;
//import com.vividsolutions.jts.geom.Geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * The Class StationsSelection.
 *
 * This class filters stations for kriging by selecting only the nearest
 * stations based on either a distance threshold or a specified number of closer
 * stations.
 */
public class StationsSelection {

	/**
	 * The .shp of the measurement point, containing the position of the stations.
	 */
	public SimpleFeatureCollection inStations = null;

	/** The HM with the measured data to be interpolated. */
	public HashMap<Integer, double[]> inData = null;

	/** Include zeros in computations (default is true). */
	public boolean doIncludezero = true;

	/** Include zeros in computations (default is true). */
	public boolean doLogarithmic = false;

	/**
	 * In the case of kriging with neighbor, maxdist is the maximum distance within
	 * the algorithm has to consider the stations
	 */
	public double maxdist;

	/**
	 * In the case of kriging with neighbor, inNumCloserStations is the number of
	 * stations the algorithm has to consider
	 */
	public int inNumCloserStations;

	/** The field of the vector of stations, defining the id. */
	public String fStationsid = null;

	/** The pm. */
	public IHMProgressMonitor pm = new LogProgressMonitor();

	/** The x station initial set. */
	public double[] xStationInitialSet;

	/** The y station initial set. */
	public double[] yStationInitialSet;

	/** The z station initial set. */
	public double[] zStationInitialSet;

	/** The h station initial set. */
	public double[] hStationInitialSet;

	/** The id station initial set. */
	public int[] idStationInitialSet;

	/** The flag is true if all the stations are equals */
	public boolean areAllEquals = true;

	/** The number of different stations. */
	public int n1 = 0;

	public double idx;

	public double idy;

	public String fStationsZ = null;

	/** The model selection for the choice of the stations. */
	Model modelSelection;

	/**
	 * Executes the station selection process.
	 * <p>
	 * The method extracts station data from the input feature collection, filters
	 * out invalid stations (e.g., with no measured data or novalues), and then
	 * sorts the stations based on their distance to the interpolation point.
	 * </p>
	 *
	 * @throws Exception if an error occurs during processing.
	 */
	public void execute() throws Exception {

		// create the arraylist containing the station with the measurements
		List<Double> xStationList = new ArrayList<>();
		List<Double> yStationList = new ArrayList<>();
		List<Double> zStationList = new ArrayList<>();
		List<Double> hStationList = new ArrayList<>();
		List<Integer> idStationList = new ArrayList<>();

		/*
		 * Store the station coordinates and measured data in the array. Skip data for
		 * non existing stations and also skip novalues.
		 *
		 * @TODO maybe use Utility.
		 */

		FeatureIterator<SimpleFeature> stationsIter = inStations.features();
		try {
			SimpleFeatureType schema = inStations.getSchema();
			int idIdx = schema.indexOf(fStationsid);
			int zIdx = (fStationsZ != null) ? schema.indexOf(fStationsZ) : -1;
			while (stationsIter.hasNext()) {
				SimpleFeature feature = stationsIter.next();
				int id = ((Number) feature.getAttribute(idIdx)).intValue();
				double z = 0;
				if (zIdx > 0) {
					Object zv = feature.getAttribute(zIdx);
					if (zv instanceof Number)
						z = ((Number) zv).doubleValue();
					else
						pm.errorMessage("Missing/invalid z for station id " + id);
				}

				Coordinate coordinate = ((Geometry) feature.getDefaultGeometry()).getCoordinate();
				double[] h = inData.get(id);

				if (h == null || isNovalue(h[0])) {
					continue;
				}

				double h0 = doLogarithmic ? Utility.getLog(h[0]) : h[0];

				// If zeros are allowed, add all values; otherwise, skip zero values.
				if (doIncludezero) {
					xStationList.add(coordinate.x);
					yStationList.add(coordinate.y);
					zStationList.add(z);
					hStationList.add(h0);
					idStationList.add(id);
					n1 = n1 + 1;
				} else {
					if (Math.abs(h0) > 0.0) { // TOLL
						xStationList.add(coordinate.x);
						yStationList.add(coordinate.y);
						zStationList.add(z);
						hStationList.add(h0);
						idStationList.add(id);
						n1 = n1 + 1;
					}
				}
			}
		} finally {
			stationsIter.close();
		}

		int nStaz = xStationList.size();

		/*
		 * Check if the coordinates or the values are the same for all the measurements
		 * stations. xStationInitialSet has the dimensions of the coordinates of the
		 * measurements points plus 1 (the station where it is going to interpolate)
		 */
		// Allocate arrays with one extra element (for the interpolation point).
		xStationInitialSet = new double[nStaz + 1];
		yStationInitialSet = new double[nStaz + 1];
		zStationInitialSet = new double[nStaz + 1];
		hStationInitialSet = new double[nStaz + 1];
		idStationInitialSet = new int[nStaz + 1];

		if (nStaz != 0) {

			double previousValue = Double.NaN;

			/*
			 * for each station added to the vector, it checks the coordinate/values and if
			 * they are not null or equal, it adds to the list of the available stations. If
			 * the coordinates or the values are all different, the flag areAllEquals
			 * becomes false.
			 */

			for (int i = 0; i < nStaz; i++) {

				double xTmp = xStationList.get(i);
				double yTmp = yStationList.get(i);
				double zTmp = zStationList.get(i);
				double hTmp = hStationList.get(i);
				int idTmp = idStationList.get(i);

				boolean doubleStation = verifyDoubleStation(xStationInitialSet, yStationInitialSet, xTmp, yTmp, i);
				if (!doubleStation) {
					xStationInitialSet[i] = xTmp;
					yStationInitialSet[i] = yTmp;
					zStationInitialSet[i] = zTmp;
					hStationInitialSet[i] = hTmp;
					idStationInitialSet[i] = idTmp;
					if (areAllEquals && hStationInitialSet[i] != previousValue) {
						areAllEquals = false;
					}

					previousValue = hStationInitialSet[i];
				}
			}
		}

		/*
		 * in case of kriging with neighbor computes the distances between the point
		 * where is going to interpolate and the other stations and it sorts them
		 */

		if (inNumCloserStations > 0 || maxdist > 0) {

			double x2, y2;
			double dDifX, dDifY;
			double distanceVector[] = new double[xStationInitialSet.length - 1];
			double pos[] = new double[xStationInitialSet.length - 1];

			for (int jj = 0; jj < xStationInitialSet.length - 1; jj++) {

				x2 = xStationInitialSet[jj];
				y2 = yStationInitialSet[jj];

				dDifX = idx - x2;
				dDifY = idy - y2;
				distanceVector[jj] =  Math.sqrt(dDifX* dDifX + dDifY * dDifY);
				pos[jj] = jj;
			}

			// sorts the distances
			QuickSortAlgorithm t = new QuickSortAlgorithm(pm);
			if (distanceVector.length > 2) {
				t.sort(distanceVector, pos);
			}
			inNumCloserStations = (inNumCloserStations > nStaz) ? nStaz : inNumCloserStations;

			/*
			 * The dimension of the new vector of the station is then defined by the actual
			 * number of the station within the distance or defined by the users
			 */

			modelSelection = SimpleModelFactory.createModel(distanceVector, inNumCloserStations, maxdist);
			int dim = 0;
			if (modelSelection != null) {
				dim = modelSelection.numberOfStations();
			}
			;
			double[] xStationWithNeighbour = new double[dim + 1];
			double[] yStationWithNeighbour = new double[dim + 1];
			int[] idStationWithNeighbour = new int[dim + 1];
			double[] zStationWithNeighbour = new double[dim + 1];
			double[] hWithNeighbour = new double[dim + 1];

			for (int i = 0; i < dim; i++) {

				xStationWithNeighbour[i] = xStationInitialSet[(int) pos[i]];
				yStationWithNeighbour[i] = yStationInitialSet[(int) pos[i]];
				zStationWithNeighbour[i] = zStationInitialSet[(int) pos[i]];
				idStationWithNeighbour[i] = idStationInitialSet[(int) pos[i]];
				hWithNeighbour[i] = hStationInitialSet[(int) pos[i]];
			}

			xStationInitialSet = xStationWithNeighbour;
			yStationInitialSet = yStationWithNeighbour;
			zStationInitialSet = zStationWithNeighbour;
			idStationInitialSet = idStationWithNeighbour;
			hStationInitialSet = hWithNeighbour;

		}

	}

	/**
	 * Verify if the current station (i) is already into the arrays.
	 * 
	 * Modifuied the version (deprecated in the HM,
	 * org.hortonmachine.gears.libs.modules.ModelsEngine;). FOr kriging no point
	 * qith same coordinate should be exist
	 *
	 * @param xStation the x coordinate of the stations
	 * @param yStation the y coordinate of the stations
	 * @param zStation the z coordinate of the stations
	 * @param hStation the h value of the stations
	 * @param xTmp
	 * @param yTmp
	 * @param zTmp
	 * @param hTmp
	 * @param i        the current index
	 * @param doMean   if the h value of a double station have different value then
	 *                 do the mean.
	 * @return true if there is already this station.
	 * @throws Exception
	 */
	private boolean verifyDoubleStation(double[] xStation, double[] yStation, double xTmp, double yTmp, int i)
			throws Exception {
		for (int j = 0; j < i - 1; j++) {
			if (dEq(xTmp, xStation[j]) && dEq(yTmp, yStation[j])) {
				throw new IllegalArgumentException("verifyStation.equalsStation1" + xTmp + "/" + yTmp);
			}
		}
		return false;
	}

}
