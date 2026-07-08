package org.hortonmachine.hmachine.geoframe.io.database.tables;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.GeoAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.NetworkSchema;

/**
 * List of tables required for GeoFrame rainfall-runoff simulations.
 * 
 * @author Daniele Andreis
 */
public enum GeoFrameGeoTable {
	/**
	 * Basin metadata and properties.
	 */
	BASIN(new BasinPolygonSchema()),
	/**
	 * Network metadata and properties.
	 */
	NET(new NetworkSchema()),
	/**
	 * Network metadata and properties.
	 */
	HYDRO_METEO_STATION(new HydroMeteoSationSchema());

	private GeoAbstractSchema tableFields;

	GeoFrameGeoTable(GeoAbstractSchema tf) {
		this.tableFields = tf;
	}

	public String tableName() {
		return tableFields.tableName();
	}

	@Override
	public String toString() {
		return tableFields.tableName();
	}

	public GeoAbstractSchema getSchema() {
		return tableFields;
	}

}
