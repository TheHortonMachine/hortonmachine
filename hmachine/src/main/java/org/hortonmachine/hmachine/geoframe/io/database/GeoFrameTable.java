package org.hortonmachine.hmachine.geoframe.io.database;

import org.hortonmachine.hmachine.geoframe.io.database.tables.AbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.BasinPoligonSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.BasinSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.ClaibrationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.HydroMeteoSationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.HydroMeteoSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.RawDataSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.SimulationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.TopologySchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.VarSchema;

/**
 * List of tables required for GeoFrame rainfall-runoff simulations.
 * 
 * @author Daniele Andreis
 */
public enum GeoFrameTable {
	/**
	 * Basin metadata and properties.
	 */
	BASIN_POINT(new BasinSchema()),

	/**
	 * Basin metadata and properties.
	 */
	BASIN(new BasinPoligonSchema()),
	/**
	 * network topology
	 */
	TOPOLOGY(new TopologySchema()),

	/**
	 * Hydro-meteorological observations for each basin.
	 */
	HYDROMETEO(new HydroMeteoSchema()),

	/**
	 * Hydro-meteorological from the meteorological network station.
	 */
	RAW_METEO(new RawDataSchema()),


	HYDRO_METEO_STATION(new HydroMeteoSationSchema()),

	/**
	 * Prefix used for simulation result tables.
	 */ // station.
	SIMULATION(new SimulationSchema()),

	/**
	 * Prefix used for calibration result tables.
	 */
	CALIBRATION(new ClaibrationSchema()),

	/**
	 * list of all variable
	 * 
	 */
	VARIABLE(new VarSchema());

	private AbstractSchema tableFields;

	GeoFrameTable(AbstractSchema tf) {
		this.tableFields = tf;
	}

	public String tableName() {
		return tableFields.tableName();
	}

	@Override
	public String toString() {
		return tableFields.tableName();
	}

	public AbstractSchema getSchema() {
		return tableFields;
	}

}
