package org.hortonmachine.hmachine.modules.statistics.kriging.utilities;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class Utility {

	/**
	 * Applies a log transformation to the given value, adding 1.0 to avoid issues
	 * with zero or negative values.
	 *
	 * @param h the input value.
	 * @return the log-transformed value.
	 */
	public final static double getLog(double h) {
		return Math.log(h + 1.0);
	}

	/**
	 * Applies a log transformation to each element in the provided map. Negative
	 * values are converted to NaN.
	 *
	 * @param h a map where each key maps to an array of double values.
	 * @return a new map with log-transformed arrays.
	 */
	public final static HashMap<Integer, double[]> getLog(HashMap<Integer, double[]> h) {
		HashMap<Integer, double[]> logMap = new HashMap<>();
		for (Map.Entry<Integer, double[]> entry : h.entrySet()) {
			double[] originalArray = entry.getValue();
			double[] logArray = new double[originalArray.length];
			for (int i = 0; i < originalArray.length; i++) {
				if (originalArray[i] >= 0) {
					logArray[i] = getLog(originalArray[i]);
				} else {
					// Handle non-positive values, e.g., by setting to NaN or skipping
					logArray[i] = Double.NaN; // Or you can use Double.NEGATIVE_INFINITY
				}
			}
			logMap.put(entry.getKey(), logArray);
		}

		// Optionally, you can now replace the old map with the new map if modification
		// in place isn't desired
		return logMap;
	}

	/**
	 * Reverses the log transformation.
	 *
	 * @param h the log-transformed value.
	 * @return the original value.
	 */
	public final static double getInverseLog(double h) {
		return Math.exp(h) - 1.0;
	}

	/**
	 * Extracts the coordinates of features from a FeatureCollection into a map,
	 * using the specified id field as the key.
	 *
	 * @param collection the collection of features.
	 * @param idField    the field containing the ID of the features.
	 * @param fPointZ    the field containing the elevation (z-value) of the
	 *                   features.
	 * @param pm         the progress monitor.
	 * @param msg        the message handler.
	 * @return a LinkedHashMap mapping feature IDs to their corresponding
	 *         coordinates.
	 * @throws Exception if the elevation field is missing.
	 */
	public final static LinkedHashMap<Integer, Coordinate> getCoordinate(SimpleFeatureCollection collection,
			String idField, String fPointZ, IHMProgressMonitor pm, HortonMessageHandler msg) throws Exception {
		LinkedHashMap<Integer, Coordinate> id2CoordinatesMcovarianceMatrix = new LinkedHashMap<>();
		FeatureIterator<SimpleFeature> iterator = collection.features();
		Coordinate coordinate = null;
		try {
			while (iterator.hasNext()) {
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
				id2CoordinatesMcovarianceMatrix.put(name, coordinate);
			}
		} finally {
			iterator.close();
		}

		return id2CoordinatesMcovarianceMatrix;
	}

}
