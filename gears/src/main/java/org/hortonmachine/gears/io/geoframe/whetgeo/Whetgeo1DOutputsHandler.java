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
 * {@link #temperature}, {@link #theta}, {@link #heatFlux} and {@link #error}
 * are always required. Every other output field is independently optional:
 * leaving it null before the first {@link #write()} omits its column(s)
 * entirely; setting it adds the column(s) to the relevant table. This lets
 * each solver opt into exactly the columns it produces, without a fixed
 * enumeration of "modes". The surface-energy-balance scalars ({@link #airT}
 * and its 7 companions) are written as one bundle, keyed on {@link #airT}
 * being non-null, since they only ever come from the same physical
 * sub-model.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 * @since 2026-06
 */
public class Whetgeo1DOutputsHandler implements AutoCloseable {

	public static final String TABLE_OUTPUT_GRID = "output_grid";
	public static final String TABLE_OUTPUT_STATE = "output_state";
	public static final String TABLE_OUTPUT_FLUX = "output_flux";
	public static final String TABLE_OUTPUT_SCALARS = "output_scalars";

	public static final String COL_ID = "id";
	public static final String COL_TIMESTAMP = Whetgeo1DInputsHandler.COL_TEMPERATURE_TIMESTAMP;
	public static final String COL_ETA = "eta";
	public static final String COL_ETA_DUAL = "eta_dual";
	public static final String COL_CONTROL_VOLUME = "control_volume";
	public static final String COL_PSI = "psi";
	public static final String COL_TEMPERATURE_IC = "temperature_ic";
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

	// mandatory per-step outputs — every solver produces these
	public long timestamp;
	public double[] temperature;
	public double[] theta;
	public double[] heatFlux;
	public double error;

	// optional per-step outputs. Each is independently activated by being
	// non-null before the first write() — leave null to omit its column(s).
	public double[] internalEnergy;
	public double[] iceContent;
	public double[] waterSuction;
	public double[] darcyVelocity;
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
	private boolean withDarcyVelocity;
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
	private final List<double[]> heatFluxBuf = new ArrayList<>();
	private final List<Double> errorBuf = new ArrayList<>();

	private List<double[]> internalEnergyBuf;
	private List<double[]> iceContentBuf;
	private List<double[]> waterSuctionBuf;
	private List<double[]> darcyVelocityBuf;
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
		heatFluxBuf.add(heatFlux.clone());
		errorBuf.add(error);

		if (withInternalEnergy) {
			internalEnergyBuf.add(internalEnergy.clone());
		}
		if (withIceContent) {
			iceContentBuf.add(iceContent.clone());
		}
		if (withWaterSuction) {
			waterSuctionBuf.add(waterSuction.clone());
		}
		if (withDarcyVelocity) {
			darcyVelocityBuf.add(darcyVelocity.clone());
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
		withDarcyVelocity = (darcyVelocity != null);
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
		if (withDarcyVelocity)
			darcyVelocityBuf = new ArrayList<>();
		if (withTopBC)
			topBCBuf = new ArrayList<>();
		if (withBottomBC)
			bottomBCBuf = new ArrayList<>();
		if (withErrorVolume)
			errorVolumeBuf = new ArrayList<>();
		if (withSurfaceEnergyBalance)
			energyBalanceBuf = new ArrayList<>();

		SqlName gridTable = SqlName.m(TABLE_OUTPUT_GRID);
		SqlName stateTable = SqlName.m(TABLE_OUTPUT_STATE);
		SqlName fluxTable = SqlName.m(TABLE_OUTPUT_FLUX);
		SqlName scalarsTable = SqlName.m(TABLE_OUTPUT_SCALARS);

		if (dropAndRecreate) {
			for (String t : List.of(TABLE_OUTPUT_GRID, TABLE_OUTPUT_STATE, TABLE_OUTPUT_FLUX, TABLE_OUTPUT_SCALARS)) {
				db.executeInsertUpdateDeleteSql("DROP TABLE IF EXISTS \"" + t + "\"");
			}
		}

		if (!db.hasTable(gridTable)) {
			db.createTable(gridTable, COL_ETA + " REAL PRIMARY KEY", COL_CONTROL_VOLUME + " REAL", COL_PSI + " REAL",
					COL_TEMPERATURE_IC + " REAL");

			String sqlGrid = String.format("""
					INSERT INTO %s (%s, %s, %s, %s)
					VALUES (?, ?, ?, ?)
					""", TABLE_OUTPUT_GRID, COL_ETA, COL_CONTROL_VOLUME, COL_PSI, COL_TEMPERATURE_IC);

			db.execOnConnection(conn -> {
				boolean autoCommit = conn.getAutoCommit();
				conn.setAutoCommit(false);
				try (IHMPreparedStatement ps = conn.prepareStatement(sqlGrid)) {
					for (int k = 0; k < KMAX; k++) {
						ps.setDouble(1, eta[k]);
						ps.setDouble(2, controlVolume[k]);
						ps.setDouble(3, psi[k]);
						ps.setDouble(4, temperatureIC[k]);
						ps.addBatch();
					}
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

		// output_flux: timestamp, eta_dual, heat_flux + active optional columns
		List<String> fluxCols = new ArrayList<>(List.of(COL_TIMESTAMP, COL_ETA_DUAL, COL_HEAT_FLUX));
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

		// output_scalars: timestamp, error + active optional columns
		List<String> scalarCols = new ArrayList<>(List.of(COL_TIMESTAMP, COL_ERROR));
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
					double[] hf = heatFluxBuf.get(r);
					double[] dv = withDarcyVelocity ? darcyVelocityBuf.get(r) : null;
					for (int k = 0; k < DUALKMAX; k++) {
						int pos = 1;
						ps.setLong(pos++, ts);
						ps.setDouble(pos++, etaDual[k]);
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
		heatFluxBuf.clear();
		errorBuf.clear();
		if (withInternalEnergy)
			internalEnergyBuf.clear();
		if (withIceContent)
			iceContentBuf.clear();
		if (withWaterSuction)
			waterSuctionBuf.clear();
		if (withDarcyVelocity)
			darcyVelocityBuf.clear();
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
