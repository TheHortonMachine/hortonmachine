package org.hortonmachine.gears.io.geoframe.whetgeo;

/**
 * Table and column name constants for a WHETGEO-1D output GeoPackage, shared
 * between the WHETGEO-1D project's own {@code Whetgeo1DOutputsHandler} (which
 * writes these tables) and this library's DB Viewer chart addon (which reads
 * them, via {@code org.hortonmachine.database.addons.whetgeo}) - the actual
 * read/write logic lives in WHETGEO-1D's own repo, not here; this class is
 * only the naming convention both sides need to agree on.
 *
 * <p>
 * Table names are prefixed {@link #PREFIX} so a DB viewer (or anything else
 * browsing an arbitrary geopackage) can recognize these as whetgeo1d output
 * tables, the same way {@code GeoframeSchema} does for basin/station
 * simulation tables.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 * @since 2026-08
 */
public class Whetgeo1DOutputSchema {

	public static final String PREFIX = "geoframe_whetgeo1d";

	public static final String TABLE_OUTPUT_GRID = PREFIX + "_output_grid";
	public static final String TABLE_OUTPUT_STATE = PREFIX + "_output_state";
	public static final String TABLE_OUTPUT_FLUX = PREFIX + "_output_flux";
	public static final String TABLE_OUTPUT_SCALARS = PREFIX + "_output_scalars";
	/**
	 * Optional table: a snapshot of the per-parameter-set SWRC values actually used
	 * for this run, one row per {@code parameterID} value (1-indexed, matching
	 * {@code Whetgeo1DInputsHandler}'s convention). Written once, alongside {@link
	 * #TABLE_OUTPUT_GRID}, so the output file is self-contained even though the
	 * parameters themselves were originally read from a separate input gpkg -
	 * without this, nothing connected to just the output file (e.g. a chart
	 * annotation) could know which soil properties produced it.
	 */
	public static final String TABLE_OUTPUT_SWRC_PARAMETERS = PREFIX + "_output_swrc_parameters";
	/**
	 * Optional table, one row, written once: the boundary condition *types* used
	 * for this run (e.g. {@code TOP_COUPLED}, {@code BOTTOM_FREE_DRAINAGE}). Only
	 * the numeric {@link #COL_TOP_BC}/{@link #COL_BOTTOM_BC} values are stored per
	 * timestep in {@link #TABLE_OUTPUT_SCALARS} - without this table nothing
	 * connected to just the output file could tell a fixed head apart from a flux
	 * or a coupled forcing, since a value alone doesn't say what kind of condition
	 * produced it.
	 */
	public static final String TABLE_OUTPUT_METADATA = PREFIX + "_output_metadata";

	public static final String COL_ID = "id";
	public static final String COL_TIMESTAMP = "timestamp";
	public static final String COL_ETA = "eta";
	public static final String COL_ETA_DUAL = "eta_dual";
	public static final String COL_CONTROL_VOLUME = "control_volume";
	public static final String COL_PSI = "psi";
	public static final String COL_TEMPERATURE_IC = "temperature_ic";
	public static final String COL_PARAMETER_ID = "parameter_id";
	public static final String COL_THETA_S = "theta_s";
	public static final String COL_THETA_R = "theta_r";
	public static final String COL_KS = "ks";
	public static final String COL_N = "n";
	public static final String COL_ALPHA = "alpha";
	public static final String COL_TOP_BC_TYPE = "top_bc_type";
	public static final String COL_BOTTOM_BC_TYPE = "bottom_bc_type";
	public static final String COL_TEMPERATURE = "temperature";
	public static final String COL_THETA = "theta";
	public static final String COL_INTERNAL_ENERGY = "internal_energy";
	public static final String COL_ICE_CONTENT = "ice_content";
	public static final String COL_WATER_SUCTION = "water_suction";
	public static final String COL_HEAT_FLUX = "heat_flux";
	public static final String COL_DARCY_VELOCITY = "darcy_velocity";
	public static final String COL_ERROR = "error";
	public static final String COL_TOP_BC = "top_bc";
	public static final String COL_BOTTOM_BC = "bottom_bc";
	public static final String COL_ERROR_VOLUME = "error_volume";
	public static final String COL_AIR_T = "air_t";
	public static final String COL_SHORT_WAVE_IN = "short_wave_in";
	public static final String COL_SHORT_WAVE_OUT = "short_wave_out";
	public static final String COL_LONG_WAVE_IN = "long_wave_in";
	public static final String COL_LONG_WAVE_OUT = "long_wave_out";
	public static final String COL_SENSIBLE_HEAT_FLUX = "sensible_heat_flux";
	public static final String COL_LATENT_HEAT_FLUX = "latent_heat_flux";
	public static final String COL_HEAT_FLUX_BOTTOM = "heat_flux_bottom";

	private Whetgeo1DOutputSchema() {
	}
}
