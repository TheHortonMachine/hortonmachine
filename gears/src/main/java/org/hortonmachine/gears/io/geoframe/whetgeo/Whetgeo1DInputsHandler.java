package org.hortonmachine.gears.io.geoframe.whetgeo;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
import org.joda.time.DateTime;

/**
 * Inputs handler for Whetgeoinputs.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class Whetgeo1DInputsHandler {
	/*
	 * Names for input files and corresponding database tables and columns. These
	 * are fixed by convention and must match the CSV files
	 */
	public static final String PREFIX = "geoframe_whetgeo1d";
	public static final String TABLE_DICTIONARY = "swrc_parameter_types";
	public static final String TABLE_IC = "initial_condition";
	public static final String TABLE_GRID = "grid_geometry";
	public static final String TABLE_RICHARDS_PARAMETERS = "swrc_parameters";
	public static final String TABLE_TEMPERATURE_BOTTOM_INTERFACE = "temperature_bottom_interface";
	public static final String TABLE_TEMPERATURE_TOP_INTERFACE = "temperature_top_interface";

	// dictionary columns
	public static final String COL_DICTIONARY_VAL1 = "val1";
	public static final String COL_DICTIONARY_VAL2 = "val2";

	// ic columns
	public static final String COL_IC_ETA = "eta";
	public static final String COL_IC_PSI0 = "psi0";
	public static final String COL_IC_T0 = "t0";

	// input_grid columns
	public static final String COL_GRID_TYPE = "type";
	public static final String COL_GRID_ETA = "eta";
	public static final String COL_GRID_K = "k";
	public static final String COL_GRID_EQUATION_STATE_ID = "equationStateID";
	public static final String COL_GRID_PARAMETER_ID = "parameterID";

	// richards_parameters columns
	public static final String COL_RICHARDS_ID = "id";
	public static final String COL_RICHARDS_THETAS = "thetaS";
	public static final String COL_RICHARDS_THETAR = "thetaR";
	public static final String COL_RICHARDS_N = "n";
	public static final String COL_RICHARDS_ALPHA = "alpha";
	public static final String COL_RICHARDS_ALPHA_SPECIFIC_STORAGE = "alphaSpecificStorage";
	public static final String COL_RICHARDS_BETA_SPECIFIC_STORAGE = "betaSpecificStorage";
	public static final String COL_RICHARDS_KS = "Ks";

	// temperature columns (both bottom and top interface tables)
	public static final String COL_TEMPERATURE_TIMESTAMP = "timestamp";

	private ADb db;

	/** Number of control volumes. */
	public int KMAX;

	/** 
     * Eta coordinate of volume centroids. 
     * 
     * <p>The 'primal' grid holds the centroids, where the state variables are defined.
     */
	public double[] eta;
	/** 
     * Eta coordinate of volume interfaces (KMAX+1). 
     * 
     * <p>The 'dual' grid holds the interfaces, where the fluxes are computed.
     */
	public double[] etaDual;

	/** Z coordinate of volume centroids. Direction is positive upwards, starting from the bottom boundary. */
	public double[] z;
	/** Z coordinate of volume interfaces (KMAX+1). Direction is positive upwards, starting from the bottom boundary. */
	public double[] zDual;

	/** Water suction initial condition at top and bottom interfaces. */
	public double[] psi;
	/** Temperature initial condition at top and bottom interfaces [K]. */
	public double[] temperatureIC;

	/** Distance between consecutive centroids / boundary (KMAX+1). */
	public double[] spaceDelta;
	/** Control-volume thickness per centroid. */
	public double[] controlVolume;

	/** Equation-state label per centroid. */
	public int[] equationStateID;
	/** Parameter-set label per centroid. */
	public int[] parameterID;

	/** Soil-particles density per parameter set [kg m-3]. NOT IN DB */
	public double[] soilParticlesDensity;
	/**
	 * Soil-particles thermal conductivity per parameter set [W m-1 K-1]. NOT IN DB
	 */
	public double[] soilParticlesThermalConductivity;
	/**
	 * Soil-particles specific heat capacity per parameter set [J kg-1 K-1]. NOT IN
	 * DB
	 */
	public double[] soilParticlesSpecificHeatCapacity;

	/** Saturated volumetric water content per parameter set [-]. */
	public double[] thetaS;
	/** Residual volumetric water content per parameter set [-]. */
	public double[] thetaR;

	/** Melting temperature per parameter set [K]. NOT IN DB */
	public double[] meltingTemperature;

	/** Hydraulic conductivity at saturation per parameter set [m s-1]. */
	public double[] Ks;
	/** Aquitard compressibility per parameter set [Pa-1]. */
	public double[] alphaSS;
	/** Water compressibility per parameter set [Pa-1]. */
	public double[] betaSS;

	/** SWRC parameter 1 per parameter set. */
	public double[] par1SWRC;
	/** SWRC parameter 2 per parameter set. */
	public double[] par2SWRC;
	/** SWRC parameter 3 per parameter set. */
	public double[] par3SWRC;
	/** SWRC parameter 4 per parameter set. */
	public double[] par4SWRC;
	/** SWRC parameter 5 per parameter set. */
	public double[] par5SWRC;

	// -------------------------------------------------------------------------

	/**
	 * Create a Whetgeo1DInputsHandler from a database.
	 */
	public Whetgeo1DInputsHandler(ADb db) {
		this.db = db;
	}
	
	public Whetgeo1DInputsHandler(String dbPath) throws Exception {
		this.db = EDb.GEOPACKAGE.getDb();
		this.db.open(dbPath);
	}
	

	/**
	 * Read all parameters from the database into the public fields.
	 *
	 * <p>
	 * Mirrors the output of {@code ReadNetCDFHeatDiffusionGrid1D}.
	 */
	public void read() throws Exception {
		readGrid();
		readSwrcParameters();
		readInitialConditions();
	}

	// ---- private helpers ----------------------------------------------------

	private void readGrid() throws Exception {
		SqlName gridTable = SqlName.m(TABLE_GRID);
		String sql = String.format("""
				SELECT %s, %s, %s, %s FROM %s ORDER BY %s DESC
				""", COL_GRID_ETA, COL_GRID_K, COL_GRID_EQUATION_STATE_ID, COL_GRID_PARAMETER_ID,
				gridTable.fixedDoubleName, COL_GRID_ETA);

		// Collect all rows (including the K=0 sentinel that marks the bottom boundary)
		final List<double[]> etaKRows = new ArrayList<>(); // [eta, K]
		final List<int[]> idsRows = new ArrayList<>(); // [equationStateID, parameterID]
		db.execOnResultSet(sql, rs -> {
			while (rs.next()) {
				etaKRows.add(new double[] { rs.getDouble(1), rs.getInt(2) });
				idsRows.add(new int[] { rs.getInt(3), rs.getInt(4) });
			}
			return null;
		});

		// Compute KMAX (sum of K values, sentinel rows with K=0 contribute nothing)
		KMAX = 0;
		for (double[] row : etaKRows) {
			KMAX += (int) row[1];
		}

		eta = new double[KMAX];
		z = new double[KMAX];
		controlVolume = new double[KMAX];
		equationStateID = new int[KMAX];
		parameterID = new int[KMAX];
		etaDual = new double[KMAX + 1];
		zDual = new double[KMAX + 1];
		spaceDelta = new double[KMAX + 1];

		// Build centroid and interface arrays layer by layer
		int globalIdx = 0;
		for (int i = 0; i < etaKRows.size(); i++) {
			double[] etaKRow = etaKRows.get(i);
            double etaTop = etaKRow[0];
			int K = (int) etaKRow[1];
			if (K == 0)
				continue; // k = 0 is bottom layer, already handled with previous interface

			double etaBottom = etaKRows.get(i + 1)[0]; // next row provides the lower boundary
			double cellSize = (etaTop - etaBottom) / K;

			if (globalIdx == 0) {
				etaDual[0] = etaTop; // surface interface
			}

			for (int j = 0; j < K; j++) {
				eta[globalIdx] = etaTop - (j + 0.5) * cellSize;
				controlVolume[globalIdx] = cellSize;
				int[] idsRow = idsRows.get(i);
                equationStateID[globalIdx] = idsRow[0];
				parameterID[globalIdx] = idsRow[1];
				etaDual[globalIdx + 1] = etaTop - (j + 1) * cellSize;
				globalIdx++;
			}
		}

		// Shift to z coordinate
		double zBottom = etaDual[KMAX];
		for (int i = 0; i < KMAX; i++) {
			z[i] = eta[i] - zBottom;
		}
		for (int i = 0; i <= KMAX; i++) {
			zDual[i] = etaDual[i] - zBottom;
		}

		// spaceDelta: distances between consecutive centroids, with half-cell at each boundary
		spaceDelta[0] = etaDual[0] - eta[0];
		for (int i = 1; i < KMAX; i++) {
			spaceDelta[i] = eta[i - 1] - eta[i];
		}
		spaceDelta[KMAX] = eta[KMAX - 1] - etaDual[KMAX];
	}

	private void readSwrcParameters() throws Exception {
		// Build colName → parType map from the dictionary table
		SqlName dictTable = SqlName.m(TABLE_DICTIONARY);
		String dictSql = String.format("""
				SELECT %s, %s FROM %s
				""", COL_DICTIONARY_VAL1, COL_DICTIONARY_VAL2, dictTable.fixedDoubleName);

		final Map<String, String> colToParType = new HashMap<>();
		db.execOnResultSet(dictSql, rs -> {
			while (rs.next()) {
				colToParType.put(rs.getString(1), rs.getString(2));
			}
			return null;
		});

		// column name in swrc_parameters (first match wins)
		List<String> swrcCols = List.of(COL_RICHARDS_N, COL_RICHARDS_ALPHA, COL_RICHARDS_THETAS, COL_RICHARDS_THETAR,
				COL_RICHARDS_ALPHA_SPECIFIC_STORAGE, COL_RICHARDS_BETA_SPECIFIC_STORAGE, COL_RICHARDS_KS);
		final Map<String, String> parTypeToCol = new HashMap<>();
		for (String col : swrcCols) {
			String parType = colToParType.get(col);
			if (parType != null) {
				parTypeToCol.putIfAbsent(parType, col);
			}
		}

		SqlName paramTable = SqlName.m(TABLE_RICHARDS_PARAMETERS);
		int numParams = (int) db.getCount(paramTable);

		thetaS = new double[numParams];
		thetaR = new double[numParams];
		Ks = new double[numParams];
		alphaSS = new double[numParams];
		betaSS = new double[numParams];
		par1SWRC = new double[numParams];
		par2SWRC = new double[numParams];
		par3SWRC = new double[numParams];
		par4SWRC = new double[numParams];
		par5SWRC = new double[numParams];

		// Map column name → 1-based index in the SELECT below
		Map<String, Integer> colIdx = Map.of(COL_RICHARDS_ID, 1, COL_RICHARDS_THETAS, 2, COL_RICHARDS_THETAR, 3,
				COL_RICHARDS_N, 4, COL_RICHARDS_ALPHA, 5, COL_RICHARDS_ALPHA_SPECIFIC_STORAGE, 6,
				COL_RICHARDS_BETA_SPECIFIC_STORAGE, 7, COL_RICHARDS_KS, 8);

		String paramSql = String.format("""
				SELECT %s, %s, %s, %s, %s, %s, %s, %s FROM %s ORDER BY %s
				""", COL_RICHARDS_ID, COL_RICHARDS_THETAS, COL_RICHARDS_THETAR, COL_RICHARDS_N, COL_RICHARDS_ALPHA,
				COL_RICHARDS_ALPHA_SPECIFIC_STORAGE, COL_RICHARDS_BETA_SPECIFIC_STORAGE, COL_RICHARDS_KS,
				paramTable.fixedDoubleName, COL_RICHARDS_ID);

		db.execOnResultSet(paramSql, rs -> {
			while (rs.next()) {
				int idx = rs.getInt(1) - 1; // id is 1-based
				thetaS[idx] = rs.getDouble(2);
				thetaR[idx] = rs.getDouble(3);
				alphaSS[idx] = rs.getDouble(6);
				betaSS[idx] = rs.getDouble(7);
				Ks[idx] = rs.getDouble(8);

				// Fill parXSWRC via dictionary mapping
				for (int p = 1; p <= 5; p++) {
					String colName = parTypeToCol.get("par" + p);
					if (colName == null)
						continue;
					double val = rs.getDouble(colIdx.get(colName));
					switch (p) {
					case 1 -> par1SWRC[idx] = val;
					case 2 -> par2SWRC[idx] = val;
					case 3 -> par3SWRC[idx] = val;
					case 4 -> par4SWRC[idx] = val;
					case 5 -> par5SWRC[idx] = val;
					}
				}
			}
			return null;
		});
	}

	private void readInitialConditions() throws Exception {
		SqlName icTable = SqlName.m(TABLE_IC);
		String sql = String.format("""
				SELECT %s, %s, %s FROM %s ORDER BY %s DESC
				""", COL_IC_ETA, COL_IC_PSI0, COL_IC_T0, icTable.fixedDoubleName, COL_IC_ETA);

		// IC points sorted surface-first (eta DESC)
		final List<double[]> icPoints = new ArrayList<>();
		db.execOnResultSet(sql, rs -> {
			while (rs.next()) {
				icPoints.add(new double[] { rs.getDouble(1), rs.getDouble(2), rs.getDouble(3) });
			}
			return null;
		});

		double etaTop = icPoints.get(0)[0];
		double etaBot = icPoints.get(1)[0];
		double psiTop = icPoints.get(0)[1];
		double psiBot = icPoints.get(1)[1];
		double tTop   = icPoints.get(0)[2];
		double tBot   = icPoints.get(1)[2];

		psi = new double[KMAX];
		temperatureIC = new double[KMAX];

		// TODO check this is correct: linear interpolation in eta coordinate?
		for (int i = 0; i < KMAX; i++) {
			double t = (eta[i] - etaTop) / (etaBot - etaTop);
			psi[i]           = psiTop + t * (psiBot - psiTop);
			temperatureIC[i] = tTop   + t * (tBot   - tTop);
		}
	}

	/** Buffered cursor over the bottom-interface temperature table. Must be closed after use. */
	public WhetgeoTemperatureIterator iterateTemperatureBottomInterface( int bufferSize ) throws Exception {
		return new WhetgeoTemperatureIterator(db, TABLE_TEMPERATURE_BOTTOM_INTERFACE, COL_TEMPERATURE_TIMESTAMP, bufferSize);
	}

	/** Buffered cursor over the top-interface temperature table. Must be closed after use. */
	public WhetgeoTemperatureIterator iterateTemperatureTopInterface( int bufferSize ) throws Exception {
		return new WhetgeoTemperatureIterator(db, TABLE_TEMPERATURE_TOP_INTERFACE, COL_TEMPERATURE_TIMESTAMP, bufferSize);
	}

	/**
	 * Create a database from CSV files in the given folder.
	 *
	 * <p>
	 * For each known table, creates the table in the db if it does not exist, then
	 * populates it from the corresponding CSV file. Timestamps are stored as Unix
	 * epoch seconds (UTC).
	 *
	 * @param csvFolderPath the path to the folder containing the CSV files.
	 * @param db            the database to populate.
	 */
	public static void createDbFromCsv(String csvFolderPath, ADb db) throws Exception {
		populateDictionary(new File(csvFolderPath, TABLE_DICTIONARY + ".csv"), db);
		populateIc(new File(csvFolderPath, TABLE_IC + ".csv"), db);
		populateInputGrid(new File(csvFolderPath, TABLE_GRID + ".csv"), db);
		populateRichardsParameters(new File(csvFolderPath, TABLE_RICHARDS_PARAMETERS + ".csv"), db);
		populateTemperature(new File(csvFolderPath, TABLE_TEMPERATURE_BOTTOM_INTERFACE + ".csv"),
				TABLE_TEMPERATURE_BOTTOM_INTERFACE, db);
		populateTemperature(new File(csvFolderPath, TABLE_TEMPERATURE_TOP_INTERFACE + ".csv"),
				TABLE_TEMPERATURE_TOP_INTERFACE, db);
	}

	private static void populateDictionary(File csv, ADb db) throws Exception {
		SqlName tableName = SqlName.m(TABLE_DICTIONARY);
		if (!db.hasTable(tableName)) {
			db.createTable(tableName, COL_DICTIONARY_VAL1 + " TEXT PRIMARY KEY", COL_DICTIONARY_VAL2 + " TEXT");
		}
		if (db.getCount(tableName) > 0)
			return;

		List<String> lines = Files.readAllLines(csv.toPath());
		String sql = String.format("""
				INSERT INTO %s (%s, %s)
				VALUES (?,?)
				""", tableName.fixedDoubleName, COL_DICTIONARY_VAL1, COL_DICTIONARY_VAL2);
		db.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
				for (int i = 1; i < lines.size(); i++) {
					String line = lines.get(i).trim();
					if (line.isEmpty())
						continue;
					String[] parts = line.split(",", -1);
					pStmt.setString(1, parts[0].trim());
					pStmt.setString(2, parts[1].trim());
					pStmt.addBatch();
				}
				pStmt.executeBatch();
				conn.commit();
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});
	}

	private static void populateIc(File csv, ADb db) throws Exception {
		SqlName tableName = SqlName.m(TABLE_IC);
		if (!db.hasTable(tableName)) {
			db.createTable(tableName, COL_IC_ETA + " REAL PRIMARY KEY", COL_IC_PSI0 + " REAL", COL_IC_T0 + " REAL");
		}
		if (db.getCount(tableName) > 0)
			return;

		List<String> lines = Files.readAllLines(csv.toPath());
		String sql = String.format("""
				INSERT INTO %s (%s, %s, %s)
				VALUES (?,?,?)
				""", tableName.fixedDoubleName, COL_IC_ETA, COL_IC_PSI0, COL_IC_T0);
		db.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
				for (int i = 1; i < lines.size(); i++) {
					String line = lines.get(i).trim();
					if (line.isEmpty())
						continue;
					String[] parts = line.split(",", -1);
					pStmt.setDouble(1, Double.parseDouble(parts[0].trim()));
					pStmt.setDouble(2, Double.parseDouble(parts[1].trim()));
					pStmt.setDouble(3, Double.parseDouble(parts[2].trim()));
					pStmt.addBatch();
				}
				pStmt.executeBatch();
				conn.commit();
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});
	}

	private static void populateInputGrid(File csv, ADb db) throws Exception {
		SqlName tableName = SqlName.m(TABLE_GRID);
		if (!db.hasTable(tableName)) {
			db.createTable(tableName, COL_GRID_ETA + " REAL PRIMARY KEY", COL_GRID_TYPE + " TEXT",
					COL_GRID_K + " INTEGER", COL_GRID_EQUATION_STATE_ID + " INTEGER",
					COL_GRID_PARAMETER_ID + " INTEGER");
		}
		if (db.getCount(tableName) > 0)
			return;

		List<String> lines = Files.readAllLines(csv.toPath());
		String sql = String.format("""
				INSERT INTO %s (%s, %s, %s, %s, %s)
				VALUES (?,?,?,?,?)
				""", tableName.fixedDoubleName, COL_GRID_ETA, COL_GRID_TYPE, COL_GRID_K, COL_GRID_EQUATION_STATE_ID,
				COL_GRID_PARAMETER_ID);
		db.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
				for (int i = 1; i < lines.size(); i++) {
					String line = lines.get(i).trim();
					if (line.isEmpty())
						continue;
					String[] parts = line.split(",", -1);
					pStmt.setDouble(1, Double.parseDouble(parts[1].trim()));
					pStmt.setString(2, parts[0].trim());
					pStmt.setInt(3, Integer.parseInt(parts[2].trim()));
					try {
						pStmt.setInt(4, Integer.parseInt(parts[3].trim()));
						pStmt.setInt(5, Integer.parseInt(parts[4].trim()));
					} catch (NumberFormatException e) {
						if (!parts[3].trim().equals("nan") && !parts[4].trim().equals("nan")) {
							throw new IllegalArgumentException(
									"Error parsing line " + (i + 1) + " in " + csv.getName() + ": " + line, e);
						}
					}
					pStmt.addBatch();
				}
				pStmt.executeBatch();
				conn.commit();
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});
	}

	private static void populateRichardsParameters(File csv, ADb db) throws Exception {
		SqlName tableName = SqlName.m(TABLE_RICHARDS_PARAMETERS);
		if (!db.hasTable(tableName)) {
			db.createTable(tableName, COL_RICHARDS_ID + " INTEGER PRIMARY KEY", COL_RICHARDS_THETAS + " REAL",
					COL_RICHARDS_THETAR + " REAL", COL_RICHARDS_N + " REAL", COL_RICHARDS_ALPHA + " REAL",
					COL_RICHARDS_ALPHA_SPECIFIC_STORAGE + " REAL", COL_RICHARDS_BETA_SPECIFIC_STORAGE + " REAL",
					COL_RICHARDS_KS + " REAL");
		}
		if (db.getCount(tableName) > 0)
			return;

		List<String> lines = Files.readAllLines(csv.toPath());
		String sql = String.format("""
				INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s)
				VALUES (?,?,?,?,?,?,?,?)
				""", tableName.fixedDoubleName, COL_RICHARDS_ID, COL_RICHARDS_THETAS, COL_RICHARDS_THETAR,
				COL_RICHARDS_N, COL_RICHARDS_ALPHA, COL_RICHARDS_ALPHA_SPECIFIC_STORAGE,
				COL_RICHARDS_BETA_SPECIFIC_STORAGE, COL_RICHARDS_KS);
		db.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
				int id = 0;
				for (int i = 1; i < lines.size(); i++) {
					String line = lines.get(i).trim();
					// skip blank lines and comment lines (e.g. "#1 Sand, Bonan 2018 Tab 8.3")
					if (line.isEmpty() || line.startsWith("#"))
						continue;
					id++;
					String[] parts = line.split(",", -1);
					pStmt.setInt(1, id);
					pStmt.setDouble(2, Double.parseDouble(parts[0].trim()));
					pStmt.setDouble(3, Double.parseDouble(parts[1].trim()));
					pStmt.setDouble(4, Double.parseDouble(parts[2].trim()));
					pStmt.setDouble(5, Double.parseDouble(parts[3].trim()));
					pStmt.setDouble(6, Double.parseDouble(parts[4].trim()));
					pStmt.setDouble(7, Double.parseDouble(parts[5].trim()));
					pStmt.setDouble(8, Double.parseDouble(parts[6].trim()));
					pStmt.addBatch();
				}
				pStmt.executeBatch();
				conn.commit();
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});
	}

	/**
	 * Populate a temperature table from an OMS-format CSV.
	 *
	 * <p>
	 * Reads the @H header line to determine column names. The timestamp column is
	 * stored as Unix epoch seconds (UTC); value columns are stored as REAL.
	 */
	private static void populateTemperature(File csv, String tableNameStr, ADb db) throws Exception {
		// Read @H header line to get value column names
		List<String> allLines = Files.readAllLines(csv.toPath());
		String[] headerParts = null;
		for (String line : allLines) {
			if (line.startsWith("@H,")) {
				headerParts = line.split(",", -1);
				break;
			}
		}
		if (headerParts == null) {
			throw new IllegalArgumentException("No @H header line found in " + csv.getName());
		}
		// headerParts: [@H, timestamp, value_0, value_1, ...]
		final int numValueCols = headerParts.length - 2;

		String[] fieldDefs = new String[1 + numValueCols];
		final String[] colNames = new String[1 + numValueCols];
		fieldDefs[0] = COL_TEMPERATURE_TIMESTAMP + " INTEGER PRIMARY KEY";
		colNames[0] = COL_TEMPERATURE_TIMESTAMP;
		for (int i = 0; i < numValueCols; i++) {
			colNames[1 + i] = headerParts[2 + i].trim();
			fieldDefs[1 + i] = colNames[1 + i] + " REAL";
		}

		SqlName tableName = SqlName.m(tableNameStr);
		if (!db.hasTable(tableName)) {
			db.createTable(tableName, fieldDefs);
		}
		if (db.getCount(tableName) > 0)
			return;

		// Build INSERT SQL
		StringBuilder sqlBuilder = new StringBuilder("INSERT INTO " + tableName.fixedDoubleName + " (");
		for (int i = 0; i < colNames.length; i++) {
			if (i > 0)
				sqlBuilder.append(", ");
			sqlBuilder.append(colNames[i]);
		}
		sqlBuilder.append(") VALUES (");
		for (int i = 0; i < colNames.length; i++) {
			if (i > 0)
				sqlBuilder.append(", ");
			sqlBuilder.append("?");
		}
		sqlBuilder.append(")");
		final String sql = sqlBuilder.toString();

		// Read all rows via OmsTimeSeriesReader
		OmsTimeSeriesReader reader = new OmsTimeSeriesReader();
		reader.file = csv.getAbsolutePath();
		reader.read();
		reader.close();

		final List<Long> epochMillis = new ArrayList<>();
		final List<double[]> valueRows = new ArrayList<>();
		for (Map.Entry<DateTime, double[]> entry : reader.outData.entrySet()) {
			epochMillis.add(entry.getKey().getMillis());
			valueRows.add(entry.getValue());
		}

		db.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
				for (int r = 0; r < epochMillis.size(); r++) {
					pStmt.setLong(1, epochMillis.get(r));
					double[] vals = valueRows.get(r);
					for (int i = 0; i < numValueCols; i++) {
						pStmt.setDouble(i + 2, vals[i]);
					}
					pStmt.addBatch();
				}
				pStmt.executeBatch();
				conn.commit();
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});
	}

	public static void main(String[] args) throws Exception {
		try (ADb db = EDb.GEOPACKAGE.getDb()) {
			db.open("/home/hydrologis/pCloudDrive/MYCLOUD/G-ANT/lavori/2026_unitn/geospace/netcdf_conversion/whetgeo_1d_inputs.gpkg");
			// createDbFromCsv("/home/hydrologis/pCloudDrive/MYCLOUD/G-ANT/lavori/2026_unitn/geospace/netcdf_conversion/",
			// db);

			var reader = new Whetgeo1DInputsHandler(db);
			reader.read();

		}

	}

}
