package org.hortonmachine.hmachine.geoframe.io.database.tables;

import org.hortonmachine.hmachine.geoframe.io.database.tables.definition.SimpleAbstractSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.ClaibrationSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinDataSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.StationDataSchema;
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
	BASINDATA(new BasinDataSchema()),

	/**
	 * Hydro-meteorological from the meteorological network station.
	 */
	STATIONDATA(new StationDataSchema()),

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

	private SimpleAbstractSchema tableFields;

	GeoFrameSimpleTable(SimpleAbstractSchema tf) {
		this.tableFields = tf;
	}

	public String tableName() {
		return tableFields.tableName();
	}

	@Override
	public String toString() {
		return tableFields.tableName();
	}

	public SimpleAbstractSchema getSchema() {
		return tableFields;
	}

}
