package org.hortonmachine.hmachine.geoframe.io.database.tables;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.feature.simple.SimpleFeatureBuilder;

public interface GeoTableSchema {


	SimpleFeatureBuilder getSFBuilder(CoordinateReferenceSystem crs, String name);

}
