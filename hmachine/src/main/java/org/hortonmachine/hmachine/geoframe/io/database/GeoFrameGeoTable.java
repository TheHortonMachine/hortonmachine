package org.hortonmachine.hmachine.geoframe.io.database;

import org.hortonmachine.hmachine.geoframe.io.database.tables.AbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.BasinPoligonSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.BasinSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.ClaibrationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.HydroMeteoSationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.HydroMeteoSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.NetworkSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.RawDataSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.SimulationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.TopologySchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.VarSchema;

/**
 * List of tables required for GeoFrame rainfall-runoff simulations.
 * 
 * @author Daniele Andreis
 */
public enum GeoFrameGeoTable {
	/**
	 * Basin metadata and properties.
	 */
	BASIN_POINT(new BasinSchema()),

	/**
	 * Basin metadata and properties.
	 */
	BASIN(new BasinPoligonSchema()),

	NET(new NetworkSchema()),

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
