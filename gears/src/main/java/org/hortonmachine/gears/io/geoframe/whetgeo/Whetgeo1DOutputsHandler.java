package org.hortonmachine.gears.io.geoframe.whetgeo;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * DB-based output handler for 1D heat diffusion results.
 *
 * <p>
 * Set the per-step fields and call {@link #write()} once per solver step. Rows
 * are batched internally and flushed every {@link #bufferSize} steps (and on
 * {@link #close()}).
 *
 * <p>
 * Table structure:
 * <ul>
 * <li>{@code output_state} — one row per (timestamp, eta centroid): id,
 * timestamp, eta, temperature, theta, internal_energy</li>
 * <li>{@code output_flux} — one row per (timestamp, etaDual interface): id,
 * timestamp, eta_dual, diffusion_heat_flux</li>
 * <li>{@code output_scalars} — one row per timestamp: id, timestamp, error,
 * top_bc, bottom_bc</li>
 * </ul>
 * Indexes on {@code timestamp} and {@code eta}/{@code eta_dual} support both
 * "all depths at one time" and "one depth over all time" queries.
 *
 * <pre>{@code
 * try (Whetgeo1DOutputsHandler writer = new Whetgeo1DOutputsHandler(db, 500)) {
 * 	writer.eta = inputsHandler.eta;
 * 	writer.etaDual = inputsHandler.etaDual;
 *
 * 	while (hasNextStep) {
 * 		solver.solve();
 * 		writer.timestamp = currentEpochMillis;
 * 		writer.temperature = solver.outTemperature;
 * 		writer.theta = solver.outTheta;
 * 		writer.internalEnergy = solver.outInternalEnergy;
 * 		writer.diffusionHeatFlux = solver.outDiffusionHeatFlux;
 * 		writer.error = solver.outErrorInternalEnergy;
 * 		writer.topBC = solver.outHeatFluxTop;
 * 		writer.bottomBC = solver.outHeatFluxBottom;
 * 		writer.write();
 * 	}
 * }
 * }</pre>
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
	public static final String COL_DIFF_HEAT_FLUX = "diffusion_heat_flux";
	public static final String COL_ERROR = "error";
	public static final String COL_TOP_BC = "top_bc";
	public static final String COL_BOTTOM_BC = "bottom_bc";

	/** Eta coordinates of cell centroids (length = KMAX). */
	public double[] eta;

	/** Eta coordinates of cell interfaces (length = KMAX+1). */
	public double[] etaDual;

	public double[] controlVolume;
	public double[] psi;
	public double[] temperatureIC;


	/** Epoch-millis timestamp of the current step. */
	public long timestamp;

	/** Temperature profile [KMAX]. */
	public double[] temperature;

	/** Volumetric water content profile [KMAX]. */
	public double[] theta;

	/** Internal energy profile [KMAX]. */
	public double[] internalEnergy;

	/** Diffusion heat flux at cell interfaces [DUALKMAX = KMAX+1]. */
	public double[] diffusionHeatFlux;

	/** Internal energy balance error for this step [J]. */
	public double error;

	/** Top boundary condition value for this step. */
	public double topBC;

	/** Bottom boundary condition value for this step. */
	public double bottomBC;

	/**
	 * When true, existing output tables are dropped and recreated on the first
	 * {@link #write()} call.
	 */
	public boolean dropAndRecreate = false;

	/**
	 * Minimum interval between writes, in minutes. A value of 0 (default) writes
	 * every step. When positive, a step is written only if at least this many
	 * minutes have elapsed since the last written step.
	 */
	public int writeIntervalMinutes = 0;

	private final ADb db;
	private final int bufferSize;

	private boolean initialized = false;
	private long lastWrittenTimestamp = Long.MIN_VALUE;
	private int KMAX;
	private int DUALKMAX;

	private String SQL_INSERT_STATE;
	private String SQL_INSERT_FLUX;
	private String SQL_INSERT_SCALARS;

	private final List<long[]> tsBuf = new ArrayList<>();
	private final List<double[]> temperatureBuf = new ArrayList<>();
	private final List<double[]> thetaBuf = new ArrayList<>();
	private final List<double[]> internalEnergyBuf = new ArrayList<>();
	private final List<double[]> diffHeatFluxBuf = new ArrayList<>();
	private final List<Double> errorBuf = new ArrayList<>();
	private final List<Double> topBCBuf = new ArrayList<>();
	private final List<Double> bottomBCBuf = new ArrayList<>();

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
		diffHeatFluxBuf.add(diffusionHeatFlux.clone());
		errorBuf.add(error);
		topBCBuf.add(topBC);
		bottomBCBuf.add(bottomBC);

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
			db.createTable(stateTable, COL_ID + " INTEGER PRIMARY KEY", COL_TIMESTAMP + " INTEGER", COL_ETA + " REAL",
					COL_TEMPERATURE + " REAL", COL_THETA + " REAL", COL_INTERNAL_ENERGY + " REAL");
			db.createIndex(stateTable, COL_TIMESTAMP, false);
			db.createIndex(stateTable, COL_ETA, false);
		}

		if (!db.hasTable(fluxTable)) {
			db.createTable(fluxTable, COL_ID + " INTEGER PRIMARY KEY", COL_TIMESTAMP + " INTEGER",
					COL_ETA_DUAL + " REAL", COL_DIFF_HEAT_FLUX + " REAL");
			db.createIndex(fluxTable, COL_TIMESTAMP, false);
			db.createIndex(fluxTable, COL_ETA_DUAL, false);
		}

		if (!db.hasTable(scalarsTable)) {
			db.createTable(scalarsTable, COL_ID + " INTEGER PRIMARY KEY", COL_TIMESTAMP + " INTEGER",
					COL_ERROR + " REAL", COL_TOP_BC + " REAL", COL_BOTTOM_BC + " REAL");
			db.createIndex(scalarsTable, COL_TIMESTAMP, false);
		}

		SQL_INSERT_STATE = String.format("""
				INSERT INTO %s (%s, %s, %s, %s, %s)
				VALUES (?, ?, ?, ?, ?)
				""", TABLE_OUTPUT_STATE, COL_TIMESTAMP, COL_ETA, COL_TEMPERATURE, COL_THETA, COL_INTERNAL_ENERGY);

		SQL_INSERT_FLUX = String.format("""
				INSERT INTO %s (%s, %s, %s)
				VALUES (?, ?, ?)
				""", TABLE_OUTPUT_FLUX, COL_TIMESTAMP, COL_ETA_DUAL, COL_DIFF_HEAT_FLUX);

		SQL_INSERT_SCALARS = String.format("""
				INSERT INTO %s (%s, %s, %s, %s)
				VALUES (?, ?, ?, ?)
				""", TABLE_OUTPUT_SCALARS, COL_TIMESTAMP, COL_ERROR, COL_TOP_BC, COL_BOTTOM_BC);

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
					for (int k = 0; k < KMAX; k++) {
						ps.setLong(1, ts);
						ps.setDouble(2, eta[k]);
						ps.setDouble(3, T[k]);
						ps.setDouble(4, th[k]);
						ps.setDouble(5, ie[k]);
						ps.addBatch();
					}
				}
				ps.executeBatch();
			}

			// output_flux: DUALKMAX rows per timestep
			try (IHMPreparedStatement ps = conn.prepareStatement(SQL_INSERT_FLUX)) {
				for (int r = 0; r < n; r++) {
					long ts = tsBuf.get(r)[0];
					double[] dhf = diffHeatFluxBuf.get(r);
					for (int k = 0; k < DUALKMAX; k++) {
						ps.setLong(1, ts);
						ps.setDouble(2, etaDual[k]);
						ps.setDouble(3, dhf[k]);
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
		diffHeatFluxBuf.clear();
		errorBuf.clear();
		topBCBuf.clear();
		bottomBCBuf.clear();
	}
}
