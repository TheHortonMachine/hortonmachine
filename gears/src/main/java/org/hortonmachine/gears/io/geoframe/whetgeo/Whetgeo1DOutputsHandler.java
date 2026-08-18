package org.hortonmachine.gears.io.geoframe.whetgeo;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * DB-based output handler for 1D heat/Richards solver results.
 *
 * <p>
 * Set the per-step fields and call {@link #write()} once per solver step. Rows
 * are batched internally and flushed every {@link #bufferSize} steps (and on
 * {@link #close()}).
 *
 * <p>
 * Base table structure (all cases):
 * <ul>
 * <li>{@code output_grid} — static grid geometry, written once on first
 * write</li>
 * <li>{@code output_state} — one row per (timestamp, eta centroid)</li>
 * <li>{@code output_flux} — one row per (timestamp, etaDual interface)</li>
 * <li>{@code output_scalars} — one row per timestamp</li>
 * </ul>
 *
 * <p>
 * {@link #temperature} and {@link #theta} are always required (every solver
 * — heat or Richards — produces a temperature and a water/energy content).
 * Every other output field, including {@link #heatFlux} and {@link #error},
 * is independently optional: leaving it null before the first
 * {@link #write()} omits its column(s) entirely; setting it adds the
 * column(s) to the relevant table. This lets each solver opt into exactly
 * the columns it produces, without a fixed enumeration of "modes" — e.g. a
 * Richards-only run has no heat flux or energy error, but does have
 * {@link #waterSuction} and {@link #errorVolume}. The surface-energy-balance
 * scalars ({@link #airT}
 * and its 7 companions) are written as one bundle, keyed on {@link #airT}
 * being non-null, since they only ever come from the same physical
 * sub-model.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 * @since 2026-06
 */
public class Whetgeo1DOutputsHandler implements AutoCloseable {

	public static final String PREFIX = "geoframe_whetgeo1d";

	// prefixed so a DB viewer (or anything else browsing an arbitrary geopackage)
	// can recognize these as whetgeo1d output tables, e.g. via a name pattern,
	// the same way GeoframeSchema does for basin/station simulation tables.
	public static final String TABLE_OUTPUT_GRID = PREFIX + "_output_grid";
	public static final String TABLE_OUTPUT_STATE = PREFIX + "_output_state";
	public static final String TABLE_OUTPUT_FLUX = PREFIX + "_output_flux";
	public static final String TABLE_OUTPUT_SCALARS = PREFIX + "_output_scalars";
	/**
	 * Optional table: a snapshot of the per-parameter-set SWRC values actually used
	 * for this run, one row per {@link #parameterID} value (1-indexed, matching
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
	public static final String COL_TIMESTAMP = Whetgeo1DInputsHandler.COL_TEMPERATURE_TIMESTAMP;
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

	// grid inputs (set once before first write)
	public double[] eta;
	public double[] etaDual;
	public double[] controlVolume;
	public double[] psi;
	public double[] temperatureIC;

	/**
	 * Per-cell parameter-set label (1-indexed, matching {@code
	 * Whetgeo1DInputsHandler.parameterID}). Optional: leave null to omit the
	 * {@link #COL_PARAMETER_ID} column. Needed to know which {@link
	 * #TABLE_OUTPUT_SWRC_PARAMETERS} row applies to which cell/depth range.
	 */
	public int[] parameterID;

	/**
	 * SWRC parameter snapshot, one array per property, 1-indexed by parameterID
	 * (index 0 unused) - the same arrays {@code Whetgeo1DInputsHandler} exposes
	 * after reading the input gpkg. All five are optional together: set every one
	 * of them (non-null) to write {@link #TABLE_OUTPUT_SWRC_PARAMETERS}, or leave
	 * all null to omit it entirely.
	 */
	public double[] swrcThetaS;
	public double[] swrcThetaR;
	public double[] swrcKs;
	public double[] swrcN;
	public double[] swrcAlpha;

	/**
	 * Boundary condition type labels for this run (e.g. {@code
	 * RichardsBoundaryConditionType.TOP_COUPLED.name()}) - a plain String rather
	 * than referencing the solver's own enum type, since this class has no
	 * dependency on (and shouldn't gain one on) any specific solver's boundary
	 * condition types. Independently optional: set either one (non-null) to write
	 * {@link #TABLE_OUTPUT_METADATA} with that value; the other column is left
	 * null if not set.
	 */
	public String topBCType;
	public String bottomBCType;

	// mandatory per-step outputs — every solver produces these
	public long timestamp;
	public double[] temperature;
	public double[] theta;

	// optional per-step outputs. Each is independently activated by being
	// non-null before the first write() — leave null to omit its column(s).
	public double[] internalEnergy;
	public double[] iceContent;
	public double[] waterSuction;
	public double[] heatFlux;
	public double[] darcyVelocity;
	public Double error;
	public Double topBC;
	public Double bottomBC;
	public Double errorVolume;

	// surface-energy-balance bundle: always set together, keyed on airT != null
	public Double airT;
	public double shortWaveIn;
	public double shortWaveOut;
	public double longWaveIn;
	public double longWaveOut;
	public double sensibleHeatFlux;
	public double latentHeatFlux;
	public double heatFluxBottom;

	/**
	 * When true, existing output tables are dropped and recreated on the first
	 * {@link #write()} call.
	 */
	public boolean dropAndRecreate = false;

	/**
	 * Minimum interval between writes, in minutes. 0 (default) writes every step.
	 */
	public int writeIntervalMinutes = 0;

	private final ADb db;
	private final int bufferSize;

	private boolean initialized = false;
	private long lastWrittenTimestamp = Long.MIN_VALUE;
	private int KMAX;
	private int DUALKMAX;

	private boolean withInternalEnergy;
	private boolean withIceContent;
	private boolean withWaterSuction;
	private boolean withHeatFlux;
	private boolean withDarcyVelocity;
	private boolean withError;
	private boolean withTopBC;
	private boolean withBottomBC;
	private boolean withErrorVolume;
	private boolean withSurfaceEnergyBalance;

	private String SQL_INSERT_STATE;
	private String SQL_INSERT_FLUX;
	private String SQL_INSERT_SCALARS;

	private final List<long[]> tsBuf = new ArrayList<>();
	private final List<double[]> temperatureBuf = new ArrayList<>();
	private final List<double[]> thetaBuf = new ArrayList<>();

	private List<double[]> internalEnergyBuf;
	private List<double[]> iceContentBuf;
	private List<double[]> waterSuctionBuf;
	private List<double[]> heatFluxBuf;
	private List<double[]> darcyVelocityBuf;
	private List<Double> errorBuf;
	private List<Double> topBCBuf;
	private List<Double> bottomBCBuf;
	private List<Double> errorVolumeBuf;
	// energy balance scalars packed as double[8]:
	// [airT, shortWaveIn, shortWaveOut, longWaveIn, longWaveOut, sensible, latent,
	// heatFluxBottom]
	private List<double[]> energyBalanceBuf;

	public Whetgeo1DOutputsHandler(ADb db, int bufferSize) {
		this.db = db;
		this.bufferSize = bufferSize;
	}

	public Whetgeo1DOutputsHandler(String dbPath, int bufferSize) throws Exception {
		this.db = EDb.GEOPACKAGE.getDb();
		this.db.open(dbPath);
		this.bufferSize = bufferSize;
	}

	/** Accumulate the current step and flush to DB when the buffer is full. */
	public void write() throws Exception {
		if (!initialized) {
			initialize();
		}
		if (writeIntervalMinutes > 0 && lastWrittenTimestamp != Long.MIN_VALUE) {
			long intervalMillis = (long) writeIntervalMinutes * 60 * 1000;
			if (timestamp - lastWrittenTimestamp < intervalMillis) {
				return;
			}
		}
		lastWrittenTimestamp = timestamp;

		tsBuf.add(new long[] { timestamp });
		temperatureBuf.add(temperature.clone());
		thetaBuf.add(theta.clone());

		if (withInternalEnergy) {
			internalEnergyBuf.add(internalEnergy.clone());
		}
		if (withIceContent) {
			iceContentBuf.add(iceContent.clone());
		}
		if (withWaterSuction) {
			waterSuctionBuf.add(waterSuction.clone());
		}
		if (withHeatFlux) {
			heatFluxBuf.add(heatFlux.clone());
		}
		if (withDarcyVelocity) {
			darcyVelocityBuf.add(darcyVelocity.clone());
		}
		if (withError) {
			errorBuf.add(error);
		}
		if (withTopBC) {
			topBCBuf.add(topBC);
		}
		if (withBottomBC) {
			bottomBCBuf.add(bottomBC);
		}
		if (withErrorVolume) {
			errorVolumeBuf.add(errorVolume);
		}
		if (withSurfaceEnergyBalance) {
			energyBalanceBuf.add(new double[] { airT, shortWaveIn, shortWaveOut, longWaveIn, longWaveOut,
					sensibleHeatFlux, latentHeatFlux, heatFluxBottom });
		}

		if (tsBuf.size() >= bufferSize) {
			flush();
		}
	}

	/** Flush remaining rows and close. */
	@Override
	public void close() throws Exception {
		flush();
	}

	private static String placeholders(int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			if (i > 0)
				sb.append(", ");
			sb.append("?");
		}
		return sb.toString();
	}

	private void initialize() throws Exception {
		KMAX = eta.length;
		DUALKMAX = etaDual.length;

		// ---- Capability detection -------------------------------------------
		// Each optional output field independently activates its own column(s)
		// when set (non-null) before the first write(). Add new flags here when
		// further solver output types are introduced — no fixed "mode" enum needed.
		withInternalEnergy = (internalEnergy != null);
		withIceContent = (iceContent != null);
		withWaterSuction = (waterSuction != null);
		withHeatFlux = (heatFlux != null);
		withDarcyVelocity = (darcyVelocity != null);
		withError = (error != null);
		withTopBC = (topBC != null);
		withBottomBC = (bottomBC != null);
		withErrorVolume = (errorVolume != null);
		withSurfaceEnergyBalance = (airT != null);
		// ----------------------------------------------------------------------

		if (withInternalEnergy)
			internalEnergyBuf = new ArrayList<>();
		if (withIceContent)
			iceContentBuf = new ArrayList<>();
		if (withWaterSuction)
			waterSuctionBuf = new ArrayList<>();
		if (withHeatFlux)
			heatFluxBuf = new ArrayList<>();
		if (withDarcyVelocity)
			darcyVelocityBuf = new ArrayList<>();
		if (withError)
			errorBuf = new ArrayList<>();
		if (withTopBC)
			topBCBuf = new ArrayList<>();
		if (withBottomBC)
			bottomBCBuf = new ArrayList<>();
		if (withErrorVolume)
			errorVolumeBuf = new ArrayList<>();
		if (withSurfaceEnergyBalance)
			energyBalanceBuf = new ArrayList<>();

		boolean withParameterID = (parameterID != null);
		boolean withSwrcSnapshot = (swrcThetaS != null && swrcThetaR != null && swrcKs != null && swrcN != null
				&& swrcAlpha != null);
		boolean withBCTypes = (topBCType != null || bottomBCType != null);

		SqlName gridTable = SqlName.m(TABLE_OUTPUT_GRID);
		SqlName stateTable = SqlName.m(TABLE_OUTPUT_STATE);
		SqlName fluxTable = SqlName.m(TABLE_OUTPUT_FLUX);
		SqlName scalarsTable = SqlName.m(TABLE_OUTPUT_SCALARS);
		SqlName swrcTable = SqlName.m(TABLE_OUTPUT_SWRC_PARAMETERS);
		SqlName metadataTable = SqlName.m(TABLE_OUTPUT_METADATA);

		if (dropAndRecreate) {
			for (String t : List.of(TABLE_OUTPUT_GRID, TABLE_OUTPUT_STATE, TABLE_OUTPUT_FLUX, TABLE_OUTPUT_SCALARS,
					TABLE_OUTPUT_SWRC_PARAMETERS, TABLE_OUTPUT_METADATA)) {
				db.executeInsertUpdateDeleteSql("DROP TABLE IF EXISTS \"" + t + "\"");
			}
		}

		// output_grid: eta, control_volume, psi, temperature_ic + parameter_id if present
		List<String> gridCols = new ArrayList<>(
				List.of(COL_ETA, COL_CONTROL_VOLUME, COL_PSI, COL_TEMPERATURE_IC));
		if (withParameterID)
			gridCols.add(COL_PARAMETER_ID);

		if (!db.hasTable(gridTable)) {
			List<String> gridFieldDefs = new ArrayList<>();
			for (String c : gridCols) {
				gridFieldDefs.add(c + (c.equals(COL_ETA) ? " REAL PRIMARY KEY"
						: c.equals(COL_PARAMETER_ID) ? " INTEGER" : " REAL"));
			}
			db.createTable(gridTable, gridFieldDefs.toArray(new String[0]));

			String gridColsCsv = String.join(", ", gridCols);
			String sqlGrid = String.format("""
					INSERT INTO %s (%s)
					VALUES (%s)
					""", TABLE_OUTPUT_GRID, gridColsCsv, placeholders(gridCols.size()));

			db.execOnConnection(conn -> {
				boolean autoCommit = conn.getAutoCommit();
				conn.setAutoCommit(false);
				try (IHMPreparedStatement ps = conn.prepareStatement(sqlGrid)) {
					for (int k = 0; k < KMAX; k++) {
						int pos = 1;
						ps.setDouble(pos++, eta[k]);
						ps.setDouble(pos++, controlVolume[k]);
						ps.setDouble(pos++, psi[k]);
						ps.setDouble(pos++, temperatureIC[k]);
						if (withParameterID)
							ps.setInt(pos++, parameterID[k]);
						ps.addBatch();
					}
					ps.executeBatch();
					conn.commit();
					conn.setAutoCommit(autoCommit);
				}
				return null;
			});
		}

		// output_swrc_parameters: one row per parameter set actually used (index 0 is
		// the unused dummy in Whetgeo1DInputsHandler's 1-indexed convention)
		if (withSwrcSnapshot && !db.hasTable(swrcTable)) {
			db.createTable(swrcTable, COL_ID + " INTEGER PRIMARY KEY", COL_THETA_S + " REAL", COL_THETA_R + " REAL",
					COL_KS + " REAL", COL_N + " REAL", COL_ALPHA + " REAL");

			String sqlSwrc = String.format("""
					INSERT INTO %s (%s, %s, %s, %s, %s, %s)
					VALUES (?, ?, ?, ?, ?, ?)
					""", TABLE_OUTPUT_SWRC_PARAMETERS, COL_ID, COL_THETA_S, COL_THETA_R, COL_KS, COL_N, COL_ALPHA);

			db.execOnConnection(conn -> {
				boolean autoCommit = conn.getAutoCommit();
				conn.setAutoCommit(false);
				try (IHMPreparedStatement ps = conn.prepareStatement(sqlSwrc)) {
					for (int id = 1; id < swrcThetaS.length; id++) {
						ps.setInt(1, id);
						ps.setDouble(2, swrcThetaS[id]);
						ps.setDouble(3, swrcThetaR[id]);
						ps.setDouble(4, swrcKs[id]);
						ps.setDouble(5, swrcN[id]);
						ps.setDouble(6, swrcAlpha[id]);
						ps.addBatch();
					}
					ps.executeBatch();
					conn.commit();
					conn.setAutoCommit(autoCommit);
				}
				return null;
			});
		}

		// output_metadata: one row, the BC type labels (whichever were set)
		if (withBCTypes && !db.hasTable(metadataTable)) {
			db.createTable(metadataTable, COL_TOP_BC_TYPE + " TEXT", COL_BOTTOM_BC_TYPE + " TEXT");

			String sqlMetadata = String.format("""
					INSERT INTO %s (%s, %s)
					VALUES (?, ?)
					""", TABLE_OUTPUT_METADATA, COL_TOP_BC_TYPE, COL_BOTTOM_BC_TYPE);

			db.execOnConnection(conn -> {
				boolean autoCommit = conn.getAutoCommit();
				conn.setAutoCommit(false);
				try (IHMPreparedStatement ps = conn.prepareStatement(sqlMetadata)) {
					ps.setString(1, topBCType);
					ps.setString(2, bottomBCType);
					ps.addBatch();
					ps.executeBatch();
					conn.commit();
					conn.setAutoCommit(autoCommit);
				}
				return null;
			});
		}

		// output_state: timestamp, eta, temperature, theta + active optional columns
		List<String> stateCols = new ArrayList<>(List.of(COL_TIMESTAMP, COL_ETA, COL_TEMPERATURE, COL_THETA));
		if (withInternalEnergy)
			stateCols.add(COL_INTERNAL_ENERGY);
		if (withIceContent)
			stateCols.add(COL_ICE_CONTENT);
		if (withWaterSuction)
			stateCols.add(COL_WATER_SUCTION);

		if (!db.hasTable(stateTable)) {
			List<String> stateFieldDefs = new ArrayList<>();
			stateFieldDefs.add(COL_ID + " INTEGER PRIMARY KEY");
			for (String c : stateCols) {
				stateFieldDefs.add(c + (c.equals(COL_TIMESTAMP) ? " INTEGER" : " REAL"));
			}
			db.createTable(stateTable, stateFieldDefs.toArray(new String[0]));
			db.createIndex(stateTable, COL_TIMESTAMP, false);
			db.createIndex(stateTable, COL_ETA, false);
		}

		// output_flux: timestamp, eta_dual + active optional columns
		List<String> fluxCols = new ArrayList<>(List.of(COL_TIMESTAMP, COL_ETA_DUAL));
		if (withHeatFlux)
			fluxCols.add(COL_HEAT_FLUX);
		if (withDarcyVelocity)
			fluxCols.add(COL_DARCY_VELOCITY);

		if (!db.hasTable(fluxTable)) {
			List<String> fluxFieldDefs = new ArrayList<>();
			fluxFieldDefs.add(COL_ID + " INTEGER PRIMARY KEY");
			for (String c : fluxCols) {
				fluxFieldDefs.add(c + (c.equals(COL_TIMESTAMP) ? " INTEGER" : " REAL"));
			}
			db.createTable(fluxTable, fluxFieldDefs.toArray(new String[0]));
			db.createIndex(fluxTable, COL_TIMESTAMP, false);
			db.createIndex(fluxTable, COL_ETA_DUAL, false);
		}

		// output_scalars: timestamp + active optional columns
		List<String> scalarCols = new ArrayList<>(List.of(COL_TIMESTAMP));
		if (withError)
			scalarCols.add(COL_ERROR);
		if (withTopBC)
			scalarCols.add(COL_TOP_BC);
		if (withBottomBC)
			scalarCols.add(COL_BOTTOM_BC);
		if (withErrorVolume)
			scalarCols.add(COL_ERROR_VOLUME);
		if (withSurfaceEnergyBalance) {
			scalarCols.addAll(List.of(COL_AIR_T, COL_SHORT_WAVE_IN, COL_SHORT_WAVE_OUT, COL_LONG_WAVE_IN,
					COL_LONG_WAVE_OUT, COL_SENSIBLE_HEAT_FLUX, COL_LATENT_HEAT_FLUX, COL_HEAT_FLUX_BOTTOM));
		}

		if (!db.hasTable(scalarsTable)) {
			List<String> scalarFieldDefs = new ArrayList<>();
			scalarFieldDefs.add(COL_ID + " INTEGER PRIMARY KEY");
			for (String c : scalarCols) {
				scalarFieldDefs.add(c + (c.equals(COL_TIMESTAMP) ? " INTEGER" : " REAL"));
			}
			db.createTable(scalarsTable, scalarFieldDefs.toArray(new String[0]));
			db.createIndex(scalarsTable, COL_TIMESTAMP, false);
		}

		String stateColsCsv = String.join(", ", stateCols);
		String fluxColsCsv = String.join(", ", fluxCols);
		String scalarColsCsv = String.join(", ", scalarCols);

		SQL_INSERT_STATE = String.format("""
				INSERT INTO %s (%s)
				VALUES (%s)
				""", TABLE_OUTPUT_STATE, stateColsCsv, placeholders(stateCols.size()));

		SQL_INSERT_FLUX = String.format("""
				INSERT INTO %s (%s)
				VALUES (%s)
				""", TABLE_OUTPUT_FLUX, fluxColsCsv, placeholders(fluxCols.size()));

		SQL_INSERT_SCALARS = String.format("""
				INSERT INTO %s (%s)
				VALUES (%s)
				""", TABLE_OUTPUT_SCALARS, scalarColsCsv, placeholders(scalarCols.size()));

		initialized = true;
	}

	private void flush() throws Exception {
		if (tsBuf.isEmpty())
			return;
		int n = tsBuf.size();

		db.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);

			// output_state: KMAX rows per timestep
			try (IHMPreparedStatement ps = conn.prepareStatement(SQL_INSERT_STATE)) {
				for (int r = 0; r < n; r++) {
					long ts = tsBuf.get(r)[0];
					double[] T = temperatureBuf.get(r);
					double[] th = thetaBuf.get(r);
					double[] ie = withInternalEnergy ? internalEnergyBuf.get(r) : null;
					double[] ic = withIceContent ? iceContentBuf.get(r) : null;
					double[] ws = withWaterSuction ? waterSuctionBuf.get(r) : null;
					for (int k = 0; k < KMAX; k++) {
						int pos = 1;
						ps.setLong(pos++, ts);
						ps.setDouble(pos++, eta[k]);
						ps.setDouble(pos++, T[k]);
						ps.setDouble(pos++, th[k]);
						if (ie != null)
							ps.setDouble(pos++, ie[k]);
						if (ic != null)
							ps.setDouble(pos++, ic[k]);
						if (ws != null)
							ps.setDouble(pos++, ws[k]);
						ps.addBatch();
					}
				}
				ps.executeBatch();
			}

			// output_flux: DUALKMAX rows per timestep
			try (IHMPreparedStatement ps = conn.prepareStatement(SQL_INSERT_FLUX)) {
				for (int r = 0; r < n; r++) {
					long ts = tsBuf.get(r)[0];
					double[] hf = withHeatFlux ? heatFluxBuf.get(r) : null;
					double[] dv = withDarcyVelocity ? darcyVelocityBuf.get(r) : null;
					for (int k = 0; k < DUALKMAX; k++) {
						int pos = 1;
						ps.setLong(pos++, ts);
						ps.setDouble(pos++, etaDual[k]);
						if (hf != null)
							ps.setDouble(pos++, hf[k]);
						if (dv != null)
							ps.setDouble(pos++, dv[k]);
						ps.addBatch();
					}
				}
				ps.executeBatch();
			}

			// output_scalars: 1 row per timestep
			try (IHMPreparedStatement ps = conn.prepareStatement(SQL_INSERT_SCALARS)) {
				for (int r = 0; r < n; r++) {
					int pos = 1;
					ps.setLong(pos++, tsBuf.get(r)[0]);
					if (withError)
						ps.setDouble(pos++, errorBuf.get(r));
					if (withTopBC)
						ps.setDouble(pos++, topBCBuf.get(r));
					if (withBottomBC)
						ps.setDouble(pos++, bottomBCBuf.get(r));
					if (withErrorVolume)
						ps.setDouble(pos++, errorVolumeBuf.get(r));
					if (withSurfaceEnergyBalance) {
						double[] eb = energyBalanceBuf.get(r);
						for (double v : eb) {
							ps.setDouble(pos++, v);
						}
					}
					ps.addBatch();
				}
				ps.executeBatch();
			}

			conn.commit();
			conn.setAutoCommit(autoCommit);
			return null;
		});

		tsBuf.clear();
		temperatureBuf.clear();
		thetaBuf.clear();
		if (withInternalEnergy)
			internalEnergyBuf.clear();
		if (withIceContent)
			iceContentBuf.clear();
		if (withWaterSuction)
			waterSuctionBuf.clear();
		if (withHeatFlux)
			heatFluxBuf.clear();
		if (withDarcyVelocity)
			darcyVelocityBuf.clear();
		if (withError)
			errorBuf.clear();
		if (withTopBC)
			topBCBuf.clear();
		if (withBottomBC)
			bottomBCBuf.clear();
		if (withErrorVolume)
			errorVolumeBuf.clear();
		if (withSurfaceEnergyBalance)
			energyBalanceBuf.clear();
	}
}
