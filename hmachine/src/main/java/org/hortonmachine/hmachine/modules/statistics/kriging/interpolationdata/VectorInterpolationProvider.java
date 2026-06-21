package org.hortonmachine.hmachine.modules.statistics.kriging.interpolationdata;

import java.util.LinkedHashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.hortonmachine.hmachine.modules.statistics.kriging.utilities.Utility;
import org.locationtech.jts.geom.Coordinate;

/**
 * This class implements the InterpolationDataProvider interface for vector
 * data. It extracts coordinates and associated values from a
 * SimpleFeatureCollection.
 */
public class VectorInterpolationProvider implements InterpolationDataProvider {
	// The feature collection from which coordinates are extracted.
	private SimpleFeatureCollection featureCollection;
	// The field name that represents the unique identifier in the features.
	private String idField;
	// The field name representing the z value (e.g., elevation) used for
	// interpolation.
	private String zField;
	// Progress monitor to report status or errors during processing.
	private IHMProgressMonitor pm;
	// Message handler for localized messages.
	private HortonMessageHandler msg;

	/**
	 * Constructor to initialize the vector interpolation provider.
	 *
	 * @param features The collection of vector features.
	 * @param idField  The field name for the unique identifier.
	 * @param zField   The field name for the z (elevation) value.
	 * @param pm       The progress monitor for reporting progress.
	 * @param msg      The message handler for localized messages.
	 */
	public VectorInterpolationProvider(SimpleFeatureCollection features, String idField, String zField,
			IHMProgressMonitor pm, HortonMessageHandler msg) {
		this.featureCollection = features;
		this.idField = idField;
		this.zField = zField;
		this.pm = pm;
		this.msg = msg;
	}

	/**
	 * Extracts the coordinates from the feature collection using the
	 * Utility.getCoordinate method.
	 *
	 * @return A LinkedHashMap where each key is a unique integer identifier and
	 *         each value is a Coordinate.
	 * @throws Exception
	 */
	@Override
	public LinkedHashMap<Integer, Coordinate> getCoordinates() {
		// Use existing Utility.getCoordinate method.
		try {
			return Utility.getCoordinate(featureCollection, idField, zField, pm, msg);
		} catch (Exception e) {
			e.printStackTrace();
			pm.message("error getting value from to interpolated vector");
			// Improvement suggestion:
			// Instead of returning null, consider throwing a custom unchecked exception.
			// For example, you could define and throw an InterpolationDataException:
			// throw new InterpolationDataException("Failed to extract coordinates from
			// feature collection", e);
		}
		return null;
	}

	public void setFeatureCollection(SimpleFeatureCollection fc) {
		this.featureCollection = fc;
	}

	@Override
	public double getValueAt(Coordinate coordinate) {
		// Return the z value (or another appropriate attribute) from the feature.
		// Implementation will depend on your data schema.
		return coordinate.z;
	}
}