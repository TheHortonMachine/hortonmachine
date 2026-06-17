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
package org.hortonmachine.hmachine.modules.statistics.kriging.pointcase;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.hmachine.modules.statistics.kriging.Kriging;
import org.hortonmachine.hmachine.modules.statistics.kriging.interpolationdata.InterpolationDataProvider;
import org.hortonmachine.hmachine.modules.statistics.kriging.interpolationdata.VectorInterpolationProvider;
import org.locationtech.jts.geom.Coordinate;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

/**
 * KrigingPointCase extends the abstract Kriging class for vector-based
 * interpolation. It implements methods to validate inputs, initialize the
 * interpolation data provider, and store the interpolation results.
 *
 * <p>
 * Improvements and potential issues:
 * <ul>
 * <li>In {@code initializeInterpolatorData()}, the order of the parameters
 * passed to {@code VectorInterpolationProvider} has been corrected (idField
 * first, then zField).</li>
 * <li>In the overridden
 * {@code storeResult(double[], HashMap<Integer, Coordinate>)} method, the call
 * to the private {@code storeResult(double[], int[])} method now correctly
 * passes an array of keys rather than the HashMap itself.</li>
 * <li>Consider throwing a more specific exception (e.g.,
 * IllegalArgumentException) in {@code verifyInput()} if required fields are
 * missing instead of using NullPointerException.</li>
 * </ul>
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
public class KrigingPointCase extends Kriging {

	@Description("The vector of the points in which the data have to be interpolated.")
	@In
	public SimpleFeatureCollection inInterpolate = null;

	@Description("The field of the interpolated vector points, defining the id.")
	@In
	public String fInterpolateid = null;

	@Description("The field of the interpolated vector points, defining the elevation.")
	@In
	public String fPointZ = null;

	@Description("The hashmap with the interpolated results")
	@Out
	public HashMap<Integer, double[]> outData = null;

	/**
	 * Validates the essential inputs for the kriging model. In detrended mode, both
	 * station and interpolation point elevation fields must be provided.
	 */
	@Override
	protected void verifyInput() {
		super.verifyInput();
		if (fInterpolateid == null) {
			throw new NullPointerException("id field not found");
		}
		if (doDetrended) {
			if (fPointZ == null) {
				throw new NullPointerException("z field not found");
			}
			int ff2 = inInterpolate.getSchema().indexOf(fPointZ);

			if (ff2 < 0) {
				throw new NullPointerException("check if the z field name is correct");
			}
		}
	}

	/**
	 * Store the result in a HashMcovarianceMatrix (if the mode is 0 or 1).
	 *
	 * @param result the result
	 * @param id     the associated id of the calculating points.
	 */
	private void storeResult(double[] result, int[] id) {
		outData = new HashMap<>();
		for (int i = 0; i < result.length; i++) {
			outData.put(id[i], new double[] { result[i] });
		}
	}

	@Override
	protected InterpolationDataProvider initializeInterpolatorData() {
		// TODO Auto-generated method stub
		return new VectorInterpolationProvider(inInterpolate, fInterpolateid, fPointZ, pm, msg);
	}

	/**
	 * Overridden method to store the interpolation results. Converts the key set of
	 * the provided coordinates map into an integer array and calls the private
	 * storeResult method to populate outData.
	 *
	 * <p>
	 * Potential bug fixed: Instead of passing the HashMap, we now correctly pass
	 * the array of keys.
	 * </p>
	 *
	 * @param result                     The array of interpolated values.
	 * @param interpolatedCoordinatesMap A HashMap of the interpolated coordinates.
	 */
	@Override
	protected void storeResult(double[] result, HashMap<Integer, Coordinate> interpolatedCoordinatesMap) {
		int[] id = interpolatedCoordinatesMap.keySet().stream().mapToInt(Integer::intValue).toArray();
		this.storeResult(result, id);
	}

	public void setProvider(InterpolationDataProvider provider) {
		this.provider = provider;
	}
	
	public void resetProvider(SimpleFeatureCollection sf) throws Exception {
		if (provider!=null) {
			((VectorInterpolationProvider) provider).setFeatureCollection(sf);
		} else {
			this.provider = null;
			this.inInterpolate=sf;
		}

	}
	
	

}
