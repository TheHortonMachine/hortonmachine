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
package org.hortonmachine.hmachine.modules.statistics.kriging.loo;

import java.util.HashMap;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.pointcase.KrigingPointCase;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

public class LeaveOneOutKrigings extends HMModel {
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

	/** transform to log. */
	@In
	public boolean doLogarithmic = false;

	/** transform to log. */
	@In
	public boolean boundedToZero = false;
	@Description("The hashmap withe the interpolated results")
	@Out
	public HashMap<Integer, double[]> outData = null;

	@Description("The Experimental Variogram.")
	@In
	public HashMap<Integer, double[]> inTheoreticalVariogram;

	@In
	public double pAGlobal;
	@In
	public double pSGlobal;

	@In
	public double pNugGlobal;

	@In
	public double pAGlobalDeTrended;

	@In
	public double pSGlobalDeTrended;

	@In
	public double pNugGlobalDeTrended;
	@In
	public String fileNoValue = "-9999";

	@In
	public String tStart = null;

	@In
	public int tTimeStep = 60;
	@In
	public String globalDetrendedVariogramType;
	@In
	public String globalVariogramType;

	private boolean isFirstStep = true;
	private KrigingPointCase kriging;
	private Map<Integer, SimpleFeatureCollection> featureCollectionMap = new HashMap<>();

	@Execute
	public void executeKriging() throws Exception {		outData = new HashMap<>();
		inizialize();
		kriging.inStations = inStations;
		ExcludingMap exMap =  new ExcludingMap(inData);
		kriging.inData = exMap;
		for (var e : featureCollectionMap.entrySet()) {
			int idToCheck = e.getKey();
			SimpleFeatureCollection fc = e.getValue();

			if (fc == null) {
				pm.message("Warning: no feature found for station id " + idToCheck);
			} else {
                exMap.setExcludedId(idToCheck);
				//kriging.inInterpolate = fc;
				
				if (inData.containsKey(idToCheck)) {
					exMap.setExcludedId(idToCheck);
					kriging.resetProvider(fc);
					kriging.execute();
					outData.put(idToCheck, kriging.outData.get(idToCheck));
				}
			}
			// pm.worked(1);
		}
		;
		pm.done();
	}

	private void inizialize() {

		// TODO Auto-generated method stub
		if (isFirstStep) {
			kriging = new KrigingPointCase();
			kriging.boundedToZero = boundedToZero;
			kriging.maxdist = maxdist;
			kriging.inNumCloserStations = inNumCloserStations;
			kriging.pSemivariogramType = pSemivariogramType;
			kriging.nugget = nugget;
			kriging.sill = sill;
			kriging.range = range;
			kriging.tStart = tStart;
			kriging.fStationsid = fStationsid;
			kriging.fInterpolateid = fStationsid;
			kriging.doDetrended = doDetrended;
			kriging.fPointZ = fStationsZ;
			kriging.fStationsZ = fStationsZ;
			kriging.inNumCloserStations = inNumCloserStations;
			kriging.doLogarithmic = doLogarithmic;
			kriging.doIncludeZero = doIncludeZero;

			SimpleFeatureIterator iterator = inStations.features();
			try {
				while (iterator.hasNext()) {
					SimpleFeature feature = iterator.next();
					int id = ((Long) feature.getProperty(fStationsid).getValue()).intValue();
					featureCollectionMap.put(id, DataUtilities.collection(feature));
				}
			} finally {
				iterator.close();
			}
		}
		kriging.inTheoreticalVariogram = inTheoreticalVariogram;
		kriging.tTimeStep = tTimeStep;
	}
}
