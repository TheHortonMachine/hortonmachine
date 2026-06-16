package org.hortonmachine.gears.io.geoframe.whetgeo;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * DB-based output handler for 1D heat solver results.
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
 * Extended schema (freezing-thawing with surface energy balance): enabled
 * automatically when {@link #iceContent} is set to a non-null array before the
 * first {@link #write()} call. Adds {@code ice_content} to {@code output_state}
 * and energy-balance columns to {@code output_scalars}.
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
	public static final String COL_HEAT_FLUX = "heat_flux";
	public static final String COL_ERROR = "error";
	public static final String COL_TOP_BC = "top_bc";
	public static final String COL_BOTTOM_BC = "bottom_bc";
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

	//  base outputs (written every step)
	public long timestamp;
	public double[] temperature;
	public double[] theta;
	public double[] internalEnergy;
	public double[] heatFlux;
	public double error;
	public double topBC;
	public double bottomBC;

	// Set iceContent to a non-null array before the first write() to activate the
	// extended schema. All energy-balance scalars below are then also written.
	// TODO check if this works as intended
	public double[] iceContent;
	public double airT;
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

	private boolean withIceContent;

	private String SQL_INSERT_STATE;
	private String SQL_INSERT_FLUX;
	private String SQL_INSERT_SCALARS;

	private final List<long[]> tsBuf = new ArrayList<>();
	private final List<double[]> temperatureBuf = new ArrayList<>();
	private final List<double[]> thetaBuf = new ArrayList<>();
	private final List<double[]> internalEnergyBuf = new ArrayList<>();
	private final List<double[]> heatFluxBuf = new ArrayList<>();
	private final List<Double> errorBuf = new ArrayList<>();
	private final List<Double> topBCBuf = new ArrayList<>();
	private final List<Double> bottomBCBuf = new ArrayList<>();

	private List<double[]> iceContentBuf;
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
		internalEnergyBuf.add(internalEnergy.clone());
		heatFluxBuf.add(heatFlux.clone());
		errorBuf.add(error);
		topBCBuf.add(topBC);
		bottomBCBuf.add(bottomBC);

		if (withIceContent) {
			iceContentBuf.add(iceContent.clone());
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

	private void initialize() throws Exception {
		KMAX = eta.length;
		DUALKMAX = etaDual.length;

		// SOLVER OUTPUT TYPES
		// Each flag controls schema creation, SQL preparation, and flush branching.
		// TODO check if this holds with new types added
		withIceContent = (iceContent != null);

		if (withIceContent) {
			iceContentBuf = new ArrayList<>();
			energyBalanceBuf = new ArrayList<>();
		}

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

		if (!db.hasTable(stateTable)) {
			if (withIceContent) {
				db.createTable(stateTable, COL_ID + " INTEGER PRIMARY KEY", COL_TIMESTAMP + " INTEGER",
						COL_ETA + " REAL", COL_TEMPERATURE + " REAL", COL_THETA + " REAL",
						COL_INTERNAL_ENERGY + " REAL", COL_ICE_CONTENT + " REAL");
			} else {
				db.createTable(stateTable, COL_ID + " INTEGER PRIMARY KEY", COL_TIMESTAMP + " INTEGER",
						COL_ETA + " REAL", COL_TEMPERATURE + " REAL", COL_THETA + " REAL",
						COL_INTERNAL_ENERGY + " REAL");
			}
			db.createIndex(stateTable, COL_TIMESTAMP, false);
			db.createIndex(stateTable, COL_ETA, false);
		}

		if (!db.hasTable(fluxTable)) {
			db.createTable(fluxTable, COL_ID + " INTEGER PRIMARY KEY", COL_TIMESTAMP + " INTEGER",
					COL_ETA_DUAL + " REAL", COL_HEAT_FLUX + " REAL");
			db.createIndex(fluxTable, COL_TIMESTAMP, false);
			db.createIndex(fluxTable, COL_ETA_DUAL, false);
		}

		if (!db.hasTable(scalarsTable)) {
			if (withIceContent) {
				db.createTable(scalarsTable, COL_ID + " INTEGER PRIMARY KEY", COL_TIMESTAMP + " INTEGER",
						COL_ERROR + " REAL", COL_TOP_BC + " REAL", COL_BOTTOM_BC + " REAL", COL_AIR_T + " REAL",
						COL_SHORT_WAVE_IN + " REAL", COL_SHORT_WAVE_OUT + " REAL", COL_LONG_WAVE_IN + " REAL",
						COL_LONG_WAVE_OUT + " REAL", COL_SENSIBLE_HEAT_FLUX + " REAL", COL_LATENT_HEAT_FLUX + " REAL",
						COL_HEAT_FLUX_BOTTOM + " REAL");
			} else {
				db.createTable(scalarsTable, COL_ID + " INTEGER PRIMARY KEY", COL_TIMESTAMP + " INTEGER",
						COL_ERROR + " REAL", COL_TOP_BC + " REAL", COL_BOTTOM_BC + " REAL");
			}
			db.createIndex(scalarsTable, COL_TIMESTAMP, false);
		}

		if (withIceContent) {
			SQL_INSERT_STATE = String.format("""
					INSERT INTO %s (%s, %s, %s, %s, %s, %s)
					VALUES (?, ?, ?, ?, ?, ?)
					""", TABLE_OUTPUT_STATE, COL_TIMESTAMP, COL_ETA, COL_TEMPERATURE, COL_THETA, COL_INTERNAL_ENERGY,
					COL_ICE_CONTENT);

			SQL_INSERT_SCALARS = String.format("""
					INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
					VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
					""", TABLE_OUTPUT_SCALARS, COL_TIMESTAMP, COL_ERROR, COL_TOP_BC, COL_BOTTOM_BC, COL_AIR_T,
					COL_SHORT_WAVE_IN, COL_SHORT_WAVE_OUT, COL_LONG_WAVE_IN, COL_LONG_WAVE_OUT, COL_SENSIBLE_HEAT_FLUX,
					COL_LATENT_HEAT_FLUX, COL_HEAT_FLUX_BOTTOM);
		} else {
			SQL_INSERT_STATE = String.format("""
					INSERT INTO %s (%s, %s, %s, %s, %s)
					VALUES (?, ?, ?, ?, ?)
					""", TABLE_OUTPUT_STATE, COL_TIMESTAMP, COL_ETA, COL_TEMPERATURE, COL_THETA, COL_INTERNAL_ENERGY);

			SQL_INSERT_SCALARS = String.format("""
					INSERT INTO %s (%s, %s, %s, %s)
					VALUES (?, ?, ?, ?)
					""", TABLE_OUTPUT_SCALARS, COL_TIMESTAMP, COL_ERROR, COL_TOP_BC, COL_BOTTOM_BC);
		}

		SQL_INSERT_FLUX = String.format("""
				INSERT INTO %s (%s, %s, %s)
				VALUES (?, ?, ?)
				""", TABLE_OUTPUT_FLUX, COL_TIMESTAMP, COL_ETA_DUAL, COL_HEAT_FLUX);

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
					double[] ie = internalEnergyBuf.get(r);
					double[] ic = withIceContent ? iceContentBuf.get(r) : null;
					for (int k = 0; k < KMAX; k++) {
						ps.setLong(1, ts);
						ps.setDouble(2, eta[k]);
						ps.setDouble(3, T[k]);
						ps.setDouble(4, th[k]);
						ps.setDouble(5, ie[k]);
						if (withIceContent)
							ps.setDouble(6, ic[k]);
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
					for (int k = 0; k < DUALKMAX; k++) {
						ps.setLong(1, ts);
						ps.setDouble(2, etaDual[k]);
						ps.setDouble(3, hf[k]);
						ps.addBatch();
					}
				}
				ps.executeBatch();
			}

			// output_scalars: 1 row per timestep
			try (IHMPreparedStatement ps = conn.prepareStatement(SQL_INSERT_SCALARS)) {
				for (int r = 0; r < n; r++) {
					ps.setLong(1, tsBuf.get(r)[0]);
					ps.setDouble(2, errorBuf.get(r));
					ps.setDouble(3, topBCBuf.get(r));
					ps.setDouble(4, bottomBCBuf.get(r));
					if (withIceContent) {
						double[] eb = energyBalanceBuf.get(r);
						ps.setDouble(5, eb[0]); // airT
						ps.setDouble(6, eb[1]); // shortWaveIn
						ps.setDouble(7, eb[2]); // shortWaveOut
						ps.setDouble(8, eb[3]); // longWaveIn
						ps.setDouble(9, eb[4]); // longWaveOut
						ps.setDouble(10, eb[5]); // sensibleHeatFlux
						ps.setDouble(11, eb[6]); // latentHeatFlux
						ps.setDouble(12, eb[7]); // heatFluxBottom
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
		internalEnergyBuf.clear();
		heatFluxBuf.clear();
		errorBuf.clear();
		topBCBuf.clear();
		bottomBCBuf.clear();
		if (withIceContent) {
			iceContentBuf.clear();
			energyBalanceBuf.clear();
		}
	}
}
