package org.hortonmachine.hmachine.geoframe.io.database.tables.definition;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;

/**
 * Represents a database table schema that can be exposed as a GeoTools feature
 * schema.
 *
 * <p>
 * Implementations provide a {@link SimpleFeatureBuilder} configured according
 * to the table structure and the specified coordinate reference system.
 * </p>
 *
 * @author Daniele Andreis
 */
public interface GeoTableSchema {
	/**
	 * Creates a feature builder for this schema.
	 *
	 * @param crs the coordinate reference system to associate with the generated
	 *            feature type
	 * @return a configured feature builder
	 */
	SimpleFeatureBuilder getSFBuilder(CoordinateReferenceSystem crs);

}
