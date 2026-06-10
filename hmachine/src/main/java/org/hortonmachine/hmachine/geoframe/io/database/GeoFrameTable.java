package org.hortonmachine.hmachine.geoframe.io.database;

import org.hortonmachine.hmachine.geoframe.io.database.tables.BasinMultiPolygonTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.CalibrationTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.EnvTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.ForecastTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.HydroMeteoSation;
import org.hortonmachine.hmachine.geoframe.io.database.tables.HydroMeteoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.RawTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.SimulationTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.TableFields;
import org.hortonmachine.hmachine.geoframe.io.database.tables.TopologyTable;

/**
 * List of tables required for GeoFrame rainfall-runoff simulations.
 * 
 * @author Daniele Andreis
 */
public enum GeoFrameTable {
	/**
	 * Basin metadata and properties.
	 */
	BASIN("basin", BasinMultiPolygonTable.class),

	/**
	 * network topology
	 */
	TOPOLOGY("topology", TopologyTable.class),

	/**
	 * Hydro-meteorological observations for each basin.
	 */
	HYDROMETEO("hydrometeo_data", HydroMeteoTable.class),

	/**
	 * Hydro-meteorological from the meteorological network station.
	 */
	RAW_METEO("raw_meteo_data", RawTable.class),

	/**
	 * Hydro-meteorological forecast.
	 */
	FORECAST_METEO("forecast_meteo_data", ForecastTable.class),

	HYDRO_METEO_STATION("hydro_meteo_station", HydroMeteoSation.class),

	/**
	 * Prefix used for simulation result tables.
	 */ // station.
	SIMULATION("simulation_", SimulationTable.class),

	/**
	 * Prefix used for calibration result tables.
	 */
	CALIBRATION_PREFIX("calibration_", CalibrationTable.class),

	/**
	 * list of all variable
	 * 
	 */
	VARIABLE("environmental_variables", EnvTable.class);

	private final String name;
	private Class<? extends TableFields> tableFields;

	GeoFrameTable(String name, Class<? extends TableFields> tf) {
		this.name = name;
		this.tableFields = tf;
	}

	public String tableName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	public String withSuffix(Object suffix) {
		return name + suffix;
	}

}
