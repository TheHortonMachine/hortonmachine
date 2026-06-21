package org.hortonmachine.hmachine.geoframe.io.database;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.AbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPoligonSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.ClaibrationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.HydroMeteoSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.NetworkSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.RawDataSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.SimulationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.TopologySchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema;

/**
 * List of tables required for GeoFrame rainfall-runoff simulations.
 * 
 * @author Daniele Andreis
 */
public enum GeoFrameSimpleTable {
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

	/**
	 * Prefix used for simulation result tables.
	 */ // station.
	SIMULATION(new SimulationSchema()),

	VAR(new VarSchema()),

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

	GeoFrameSimpleTable(AbstractSchema tf) {
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
