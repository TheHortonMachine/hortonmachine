package org.hortonmachine.geoframe.utils;

import java.util.Date;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.utils.time.ETimeUtilities;

/**
 * The state of a basin in a particular timestep of the water budget simulation.
 */
public class WaterBudgetState {

	public double separatedPrecipitationRain;
	public double separatedPrecipitationSnow;

	public double solidWaterInitial;
	public double liquidWaterInitial;
	public double solidWaterFinal;
	public double liquidWaterFinal;
	public double swe;
	public double freezing;
	public double melting;
	public double meltingDischarge;
	public double errorODESolidWater;
	public double errorODELiquidWater;
	public double errorSWE;

	public double canopyInitial;
	public double canopyFinal;
	public double canopyThroughfall;
	public double canopyAET;
	public double canopyActualInput;
	public double canopyActualOutput;
	public double canopyError;

	public double rootzoneInitial;
	public double rootzoneFinal;
	public double rootzoneActualInput;
	public double rootzoneAET;
	public double rootzoneRecharge;
	public double rootzoneQuick;
	public double rootzoneAlpha;
	public double rootzoneError;

	public double runoffInitial;
	public double runoffFinal;
	public double runoffDischarge;
	public double runoffError;
	public double groundInitial;
	public double groundFinal;
	public double groundDischarge;
	public double groundError;
	
	public static final WaterBudgetState DUMMY = new WaterBudgetState();

	public static String initTable(ADb db) throws Exception {
		// create table if not exists, with primary key on basin id and timestamp,
		// and an index on basin_id and one on timestamp for faster queries
		String tsStr = ETimeUtilities.INSTANCE.TIMESTAMPFORMATTER_UTC.format(new Date());
		String tableName = "sim" + tsStr + "_water_budget_state";
		String createTableSql = """
				CREATE TABLE IF NOT EXISTS %s (
				    basin_id INTEGER NOT NULL,
				    timestamp BIGINT NOT NULL,
				    separated_precipitation_rain REAL,
				    separated_precipitation_snow REAL,
				    solid_water_initial REAL,
				    liquid_water_initial REAL,
				    solid_water_final REAL,
				    liquid_water_final REAL,
				    swe REAL,
				    freezing REAL,
				    melting REAL,
				    melting_discharge REAL,
				    error_ode_solid_water REAL,
				    error_ode_liquid_water REAL,
				    error_swe REAL,
				    canopy_initial REAL,
				    canopy_final REAL,
				    canopy_throughfall REAL,
				    canopy_aet REAL,
				    canopy_actual_input REAL,
				    canopy_actual_output REAL,
				    canopy_error REAL,
				    rootzone_initial REAL,
				    rootzone_final REAL,
				    rootzone_actual_input REAL,
				    rootzone_aet REAL,
				    rootzone_recharge REAL,
				    rootzone_quick REAL,
				    rootzone_alpha REAL,
				    rootzone_error REAL,
				    runoff_initial REAL,
				    runoff_final REAL,
				    runoff_discharge REAL,
				    runoff_error REAL,
				    ground_initial REAL,
				    ground_final REAL,
				    ground_discharge REAL,
				    ground_error REAL,
				    PRIMARY KEY (basin_id, timestamp)
				);
				""".formatted(tableName);

		String createIndexBasin = """
				CREATE INDEX IF NOT EXISTS %s_basin_idx
				ON %s (basin_id);
				""".formatted(tableName, tableName);

		String createIndexTs = """
				CREATE INDEX IF NOT EXISTS %s_timestamp_idx
				ON %s (timestamp);
				""".formatted(tableName, tableName);

		db.executeInsertUpdateDeleteSql(createTableSql);
		db.executeInsertUpdateDeleteSql(createIndexBasin);
		db.executeInsertUpdateDeleteSql(createIndexTs);

		return tableName;
	}

	public static String getPreparedInsertSql(String tableName) {
		return """
				INSERT OR REPLACE INTO %s (
				    basin_id,
				    timestamp,
				    separated_precipitation_rain,
				    separated_precipitation_snow,
				    solid_water_initial,
				    liquid_water_initial,
				    solid_water_final,
				    liquid_water_final,
				    swe,
				    freezing,
				    melting,
				    melting_discharge,
				    error_ode_solid_water,
				    error_ode_liquid_water,
				    error_swe,
				    canopy_initial,
				    canopy_final,
				    canopy_throughfall,
				    canopy_aet,
				    canopy_actual_input,
				    canopy_actual_output,
				    canopy_error,
				    rootzone_initial,
				    rootzone_final,
				    rootzone_actual_input,
				    rootzone_aet,
				    rootzone_recharge,
				    rootzone_quick,
				    rootzone_alpha,
				    rootzone_error,
				    runoff_initial,
				    runoff_final,
				    runoff_discharge,
				    runoff_error,
				    ground_initial,
				    ground_final,
				    ground_discharge,
				    ground_error
				) VALUES (
					?, ?, ?, ?, ?,
					?, ?, ?, ?, ?,
				    ?, ?, ?, ?, ?,
				    ?, ?, ?, ?, ?,
				    ?, ?, ?, ?, ?,
				    ?, ?, ?, ?, ?,
				    ?, ?, ?, ?, ?,
				    ?, ?, ?
				);
				""".formatted(tableName);
	}

	public Object[] getInsertIntoDbObjects(int basinId, long timestamp) throws Exception {
		return new Object[] { basinId, timestamp, separatedPrecipitationRain, separatedPrecipitationSnow,
				solidWaterInitial, liquidWaterInitial, solidWaterFinal, liquidWaterFinal, swe, freezing, melting,
				meltingDischarge, errorODESolidWater, errorODELiquidWater, errorSWE, canopyInitial, canopyFinal,
				canopyThroughfall, canopyAET, canopyActualInput, canopyActualOutput, canopyError, rootzoneInitial,
				rootzoneFinal, rootzoneActualInput, rootzoneAET, rootzoneRecharge, rootzoneQuick, rootzoneAlpha,
				rootzoneError, runoffInitial, runoffFinal, runoffDischarge, runoffError, groundInitial, groundFinal,
				groundDischarge, groundError };

	}
}
