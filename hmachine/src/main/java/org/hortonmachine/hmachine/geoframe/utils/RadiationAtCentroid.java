package org.hortonmachine.hmachine.geoframe.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.geotools.api.data.DataSourceException;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.time.UtcTimeUtilities;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.io.database.TableUtils;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.BasinPolygonSchema.BasinMultiPolygonField;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.EnvironmentalVariableType;
import org.hortonmachine.hmachine.geoframe.io.database.tables.implementation.VarSchema.TimeResolution;
import org.hortonmachine.hmachine.geoframe.utils.radiation.NetRadiationPointCase;
import org.hortonmachine.hmachine.geoframe.utils.radiation.LwrbPointCase.Lwrb;
import org.hortonmachine.hmachine.geoframe.utils.radiation.swrbPointCase.ShortwaveRadiationBalancePointCase;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description("Populate the db with radiation data at centroid")
@Author(name = "Daniele Andreis")
@Keywords("time series, iterator, basin, value, database")
@Name("VariableEvaluatorAtCentroid")
@Status(40)
@UI(HMConstants.HIDE_UI_HINT)
@License("General Public License Version 3 (GPLv3)")

//TODO this is a simple example to improve
public class RadiationAtCentroid extends HMModel {
	@Description("Input geoframe database")
	@In
	public ASpatialDb inGeoframeDb = null;
	@Description("X parameter of the literature formulation")
	@In
	public double x = 0.7;
	@Description("Y parameter of the literature formulation")
	@In
	public double y = 5.95;
	// TODO check
	@Description("Y parameter of the literature formulation")
	@In
	public double z = 0;

	@Description("Soil emissivity")
	@Unit("-")
	@In
	public double epsilonS = 0.98;
	
	@Description("Coefficient to take into account the cloud cover, set equal to 0 for clear sky conditions ")
	@In
	public double aCloud = 0;
	
	@Description("Exponent  to take into account the cloud cover, set equal to 1 for clear sky conditions")
	@In
	public double bCloud = 1;
	
	@Description("The expected time resolution of the data. Daily and hourly (default) is supported.")
	@In
	public TimeResolution pTimeResolution = TimeResolution.HOURLY;

	@Description("Number of sun-position samples used to average net radiation over a day if "
			+ "pTimeResolution is DAILY. 24 (one per hour) is the most accurate but also slowest.")
	@In
	public int pDailySubSamples = 24;

	public double alpha = 0.26;
	
	@Description("Ozone layer thickness in cm")
	@In
	public double pCmO3 = 0.6;
	
	@Description("The soil albedo.")
	@In
	public double pAlphagp = 0.9;

	@Description(" For aerosol attenuation (5 < vis < 180 Km) [km].")
	@In
	@Unit("km")
	public double visibility = 80;

	@Description("String containing the number of the model: " + " 1: Angstrom [1918];" + " 2: Brunt's [1932];"
			+ " 3: Swinbank [1963];" + " 4: Idso and Jackson [1969];" + " 5: Brutsaert [1975];" + " 6: Idso [1981];"
			+ " 7: Monteith and Unsworth [1990];" + " 8: Konzelman [1994];" + " 9: Prata [1996];"
			+ " 10: Dilley and O'Brien [1998];" + " 11: To be implemented")
	@In
	public String lwrvModeel;

	@Description("The path to the map of the Digital Elevation Model")
	@In
	public GridCoverage2D dem;
	@Description("The path to the skyviewfactor")
	@In
	public GridCoverage2D inSkyview;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inTemperatureReader;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inHumifidtyReader = null;
	@Description("reader")
	@In
	public GeoframeEnvDatabaseIterator inClearSkyReader;

	private Lwrb lwrb;
	private ShortwaveRadiationBalancePointCase swrb;
	private NetRadiationPointCase nrpc;
	private int timestepIndex = 0;
	private HashMap<Integer, double[]> inNan;
	private SimpleFeatureCollection inBasinsFC;

	private static final long MILLIS_PER_HOUR = 3_600_000L;
	private static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

	@Initialize
	public void init() throws Exception {
		if (lwrvModeel == null || lwrvModeel.isEmpty()) {
			throw new IllegalArgumentException();
		}
		if (pTimeResolution == TimeResolution.MONTHLY || pTimeResolution == TimeResolution.YEARLY) {
			throw new UnsupportedOperationException(
					"ErmRadiation only supports HOURLY and DAILY resolutions, got " + pTimeResolution);
		}
		if (pDailySubSamples < 1) {
			throw new IllegalArgumentException("pDailySubSamples must be >= 1, got " + pDailySubSamples);
		}
		checkNull(inGeoframeDb);

		if (!(inGeoframeDb.hasTable(GeoFrameGeoTable.BASIN.tableName())
				&& inGeoframeDb.hasTable(GeoFrameSimpleTable.BASINDATA.tableName())
				&& inGeoframeDb.hasTable(GeoFrameSimpleTable.STATIONDATA.tableName()))) {
			throw new DataSourceException("no suitable tables are present in db check");
		}

		if (!inGeoframeDb.hasTable(GeoFrameSimpleTable.BASINDATA.getSchema().getSQLName())) {
			String sql = GeoFrameSimpleTable.BASINDATA.getSchema().createTableSql();
			inGeoframeDb.executeInsertUpdateDeleteSql(sql);
		}

		inBasinsFC = SpatialDbsImportUtils.tableToFeatureFCollection(inGeoframeDb,
				GeoFrameGeoTable.BASIN.getSchema().getSQLName(), -1, -1, null, null);

		List<Integer> ids = new ArrayList<>();

		try (SimpleFeatureIterator it = inBasinsFC.features()) {
			while (it.hasNext()) {
				SimpleFeature feature = it.next();

				Integer id = ((Number) feature.getAttribute(BasinMultiPolygonField.ID.columnName())).intValue();
				ids.add(id);
			}
		}

		int[] basinsId = ids.stream().mapToInt(Integer::intValue).toArray();

		inNan = TableUtils.getLegacyHMInputNaN(basinsId);

		lwrb = createLwrb();
		swrb = createSwrb();
		nrpc = createNrpc();
	}

	/**
	 * Builds a freshly configured {@link Lwrb} instance.
	 *
	 * <p>
	 * Used both for the single sequential instance and, one per worker
	 * thread, for the parallel pre-caching path in {@link #process()}, since
	 * {@link Lwrb} keeps per-call mutable state and is not safe to share
	 * across threads.
	 */
	private Lwrb createLwrb() {
		Lwrb l = new Lwrb();
		l.X = x;
		l.Y = y;
		l.Z = z;
		l.epsilonS = epsilonS;
		l.aCloud = aCloud;
		l.bCloud = bCloud;
		l.model = lwrvModeel;
		l.fStationsID = BasinMultiPolygonField.ID.columnName();
		l.inStationsFC = inBasinsFC;
		l.inSkyviewGC = inSkyview;
		return l;
	}

	/**
	 * Builds a freshly configured {@link ShortwaveRadiationBalancePointCase}
	 * instance. See {@link #createLwrb()} for why this needs to be per-thread
	 * in the parallel path.
	 */
	private ShortwaveRadiationBalancePointCase createSwrb() {
		ShortwaveRadiationBalancePointCase s = new ShortwaveRadiationBalancePointCase();
		s.doHourly = pTimeResolution == TimeResolution.HOURLY;
		s.fStationsid = BasinMultiPolygonField.ID.columnName();
		s.inStationsFC = inBasinsFC;
		s.inDem = dem;
		s.inSkyview = inSkyview;
		s.pAlphag = pAlphagp;
		s.pCmO3 = pCmO3;
		s.pVisibility = visibility;
		return s;
	}

	private NetRadiationPointCase createNrpc() {
		NetRadiationPointCase n = new NetRadiationPointCase();
		n.alfa = alpha;
		return n;
	}

	@Execute
	public void process() throws Exception {
		int[] ids = TableUtils.getIntIdArray(inGeoframeDb, GeoFrameGeoTable.BASIN.tableName(),
				BasinPolygonSchema.BasinMultiPolygonField.ID.columnName(), null);
		int iteration = 0;
		if (inTemperatureReader.isPreCachingMode()) {
			pm.beginTask("Processing radiation data...", inTemperatureReader.getCachedSize());
			// We can do parallel processing of timesteps only in cached mode
			// because each the timesteps are indexed and independent 
			int nThreads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
			ExecutorService executor = Executors.newFixedThreadPool(nThreads);
			ThreadLocal<Lwrb> tlLwrb = ThreadLocal.withInitial(this::createLwrb);
			ThreadLocal<ShortwaveRadiationBalancePointCase> tlSwrb = ThreadLocal.withInitial(this::createSwrb);
			ThreadLocal<NetRadiationPointCase> tlNrpc = ThreadLocal.withInitial(this::createNrpc);
			try {
				int chunkSize = nThreads * 4;
				String insertSql = GeoFrameSimpleTable.BASINDATA.getSchema().buildInsertAll();
				List<Callable<TimestepResult>> chunkTasks = new ArrayList<>(chunkSize);

				double[] variableData = inTemperatureReader.getCached(timestepIndex);
				while (variableData != null) {
					final int idx = timestepIndex;
					chunkTasks.add(() -> computeTimestepParallel(idx, ids, tlLwrb, tlSwrb, tlNrpc));
					timestepIndex++;
					variableData = inTemperatureReader.getCached(timestepIndex);

					if (chunkTasks.size() == chunkSize || variableData == null) {
						computeAndWriteChunk(executor, chunkTasks, insertSql);
						int size = chunkTasks.size();
						pm.worked(size);
						iteration += size;
						pm.message("Processed " + iteration + " timesteps...");
						chunkTasks.clear();
					}
				}
			} finally {
				executor.shutdown();
			}
		} else {
			pm.beginTask("Processing radiation data...", -1);
			while (inTemperatureReader.next()) {
				double[] variableData = inTemperatureReader.outData;
				var temperature = TableUtils.getLegacyHMInput(variableData, ids);

				HashMap<Integer, double[]> humidity = inNan;
				if (inHumifidtyReader != null) {
					double[] humidityData = inHumifidtyReader.outData;
					humidity = TableUtils.getLegacyHMInput(humidityData, ids);
				}
				HashMap<Integer, double[]> clearSky = inNan;
				if (inClearSkyReader != null) {
					double[] clearSkyData = inClearSkyReader.outData;
					clearSky = TableUtils.getLegacyHMInput(clearSkyData, ids);
				}
				lwrb.inHumidityValuesHM = inNan;
				swrb.inHumidityValues = inNan;

				if (iteration++ % 1000 == 0) {
					pm.message("Processed timestep " + UtcTimeUtilities.quickToString(inTemperatureReader.currentT)
							+ "...");
				}
				processTimestep(temperature, humidity, clearSky, inTemperatureReader.currentT);

			}
		}
		pm.done();

	}

	private void processTimestep(HashMap<Integer, double[]> temperature, HashMap<Integer, double[]> humidity,
			HashMap<Integer, double[]> clearSky, long t) throws Exception {
		HashMap<Integer, double[]> out = computeNetRadiation(lwrb, swrb, nrpc, temperature, humidity, clearSky, t);

		String insertSql = GeoFrameSimpleTable.BASINDATA.getSchema().buildInsertAll();
		inGeoframeDb.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(insertSql)) {
				for (Map.Entry<Integer, double[]> entry : out.entrySet()) {
					int basinId = entry.getKey();
					double value = entry.getValue()[0];
					pStmt.setLong(1, t);
					pStmt.setInt(2, basinId);
					pStmt.setInt(3, EnvironmentalVariableType.RADIATION.getId());
					pStmt.setDouble(4, value);
					pStmt.addBatch();
				}
				pStmt.executeBatch();
				conn.commit();
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});

	}

	/** Result of one timestep's computation, ready to be written to the db. */
	private static class TimestepResult {
		final long timestep;
		final HashMap<Integer, double[]> out;

		TimestepResult(long timestep, HashMap<Integer, double[]> out) {
			this.timestep = timestep;
			this.out = out;
		}
	}

	/**
	 * Computes one timestep using thread-confined {@link Lwrb}/
	 * {@link ShortwaveRadiationBalancePointCase}/{@link NetRadiationPointCase}
	 * instances (one per worker thread, lazily built on first use so the
	 * expensive one-time raster setup in each of them only happens once per
	 * thread, not once per timestep).
	 *
	 * <p>
	 * The result map is copied out of {@code nrpc.outHMnetRad} because that
	 * field is reused and overwritten in place on every {@code process()}
	 * call on the same instance.
	 */
	private TimestepResult computeTimestepParallel(int idx, int[] ids, ThreadLocal<Lwrb> tlLwrb,
			ThreadLocal<ShortwaveRadiationBalancePointCase> tlSwrb, ThreadLocal<NetRadiationPointCase> tlNrpc)
			throws Exception {
		double[] variableData = inTemperatureReader.getCached(idx);
		long timestamp = inTemperatureReader.getCachedTimestamp(idx);
		var temperature = TableUtils.getLegacyHMInput(variableData, ids);

		HashMap<Integer, double[]> humidity = inNan;
		if (inHumifidtyReader != null && inHumifidtyReader.isPreCachingMode()) {
			double[] humidityData = inHumifidtyReader.getCached(idx);
			humidity = TableUtils.getLegacyHMInput(humidityData, ids);
		}
		HashMap<Integer, double[]> clearSky = inNan;
		if (inClearSkyReader != null && inClearSkyReader.isPreCachingMode()) {
			double[] clearSkyData = inClearSkyReader.getCached(idx);
			clearSky = TableUtils.getLegacyHMInput(clearSkyData, ids);
		}

		Lwrb lwrbLocal = tlLwrb.get();
		ShortwaveRadiationBalancePointCase swrbLocal = tlSwrb.get();
		NetRadiationPointCase nrpcLocal = tlNrpc.get();

		HashMap<Integer, double[]> out = computeNetRadiation(lwrbLocal, swrbLocal, nrpcLocal, temperature, humidity,
				clearSky, timestamp);
		return new TimestepResult(timestamp, out);
	}

	/**
	 * Computes net radiation (W/m2) for one timestep, dispatching to
	 * either a single hourly sample or a daily average of 24 hourly samples
	 * depending on the time resolution.
	 *
	 * <p>
	 * The returned map is always a fresh copy, independent of
	 * {@code nrpc.outHMnetRad}, which is reused and overwritten in place on
	 * every {@code process()} call - without a copy, a thread going on to
	 * compute another timestep before this result is consumed would leave the
	 * caller holding a reference to that later timestep's data instead.
	 */
	private HashMap<Integer, double[]> computeNetRadiation(Lwrb lwrbLocal, ShortwaveRadiationBalancePointCase swrbLocal,
			NetRadiationPointCase nrpcLocal, HashMap<Integer, double[]> temperature, HashMap<Integer, double[]> humidity,
			HashMap<Integer, double[]> clearSky, long t) throws Exception {
		lwrbLocal.inAirTemperatureValuesHM = temperature;
		lwrbLocal.inSoilTempratureValuesHM = temperature;
		lwrbLocal.inHumidityValuesHM = humidity;
		lwrbLocal.inClearnessIndexValuesHM = clearSky;
		// Longwave depends only on temperature/humidity/clearness - not on time of day -
		// so one process() call covers the whole timestep, hourly or daily alike.
		lwrbLocal.process();

		swrbLocal.inHumidityValues = humidity;
		swrbLocal.inTemperatureValues = temperature;

		if (pTimeResolution == TimeResolution.HOURLY) {
			swrbLocal.doHourly = true;
			swrbLocal.tCurrentDateString = GeoframeEnvDatabaseIterator.ts2str(t);
			swrbLocal.process();

			nrpcLocal.inShortwaveValues = swrbLocal.outHMtotal;
			nrpcLocal.inDownwellingValues = lwrbLocal.outHMlongwaveDownwellingHM;
			nrpcLocal.inUpwellingValues = lwrbLocal.outHMlongwaveUpwellingHM;
			nrpcLocal.process();
			return new HashMap<>(nrpcLocal.outHMnetRad);
		} else {
			return averageDailyNetRadiation(lwrbLocal, swrbLocal, nrpcLocal, t);
		}
	}

	/**
	 * Averages net radiation over a full day into a daily-mean flux.
	 *
	 * <p>
	 * {@link ShortwaveRadiationBalancePointCase} only ever evaluates the sun's
	 * position (and the DEM-derived shading/skyview weighting that goes with
	 * it) at one instant, and that DEM-wide shadow recomputation is what
	 * dominates runtime. With daily-resolution input there is a single
	 * temperature/humidity/clearness reading for the whole day, so this
	 * processes that same daily reading at {@link #pDailySubSamples} sun
	 * positions evenly spaced across the day and averages the results. At the
	 * default of 24 this means running hourly resolution for the day and
	 * averaging the 24 outputs; lower values are cheaper but coarser
	 * approximations, useful for quick test runs.
	 */
	private HashMap<Integer, double[]> averageDailyNetRadiation(Lwrb lwrbLocal,
			ShortwaveRadiationBalancePointCase swrbLocal, NetRadiationPointCase nrpcLocal, long dayTimestamp)
			throws Exception {
		long dayStartMillis = Math.floorDiv(dayTimestamp, MILLIS_PER_DAY) * MILLIS_PER_DAY;
		long stepMillis = MILLIS_PER_DAY / pDailySubSamples;

		HashMap<Integer, Double> sumWattsPerM2 = new HashMap<>();
		swrbLocal.doHourly = true; // sample the actual sun position each time, rather than one fixed instant
		for (int sample = 0; sample < pDailySubSamples; sample++) {
			long sampleMillis = dayStartMillis + sample * stepMillis;
			swrbLocal.tCurrentDateString = GeoframeEnvDatabaseIterator.ts2str(sampleMillis);
			swrbLocal.process();

			nrpcLocal.inShortwaveValues = swrbLocal.outHMtotal;
			nrpcLocal.inDownwellingValues = lwrbLocal.outHMlongwaveDownwellingHM;
			nrpcLocal.inUpwellingValues = lwrbLocal.outHMlongwaveUpwellingHM;
			nrpcLocal.process();

			for (Map.Entry<Integer, double[]> entry : nrpcLocal.outHMnetRad.entrySet()) {
				sumWattsPerM2.merge(entry.getKey(), entry.getValue()[0], Double::sum);
			}
		}

		HashMap<Integer, double[]> dailyMean = new HashMap<>();
		for (Map.Entry<Integer, Double> entry : sumWattsPerM2.entrySet()) {
			dailyMean.put(entry.getKey(), new double[] { entry.getValue() / pDailySubSamples });
		}
		return dailyMean;
	}

	/**
	 * Runs a chunk of timestep tasks on the executor. Mind that this is where the
	 * actual radiation computation is done, blocking until the whole chunk
	 * is done. Because then all the results are written to db sequentially.
	 * Keeping the db write single-threaded is necessary with GeoPackage/SQLite.
	 */
	private void computeAndWriteChunk(ExecutorService executor, List<Callable<TimestepResult>> tasks, String insertSql)
			throws Exception {
		List<Future<TimestepResult>> futures = executor.invokeAll(tasks);
		inGeoframeDb.execOnConnection(conn -> {
			boolean autoCommit = conn.getAutoCommit();
			conn.setAutoCommit(false);
			try (IHMPreparedStatement pStmt = conn.prepareStatement(insertSql)) {
				for (Future<TimestepResult> f : futures) {
					TimestepResult res = f.get();
					for (Map.Entry<Integer, double[]> entry : res.out.entrySet()) {
						int basinId = entry.getKey();
						double value = entry.getValue()[0];
						pStmt.setLong(1, res.timestep);
						pStmt.setInt(2, basinId);
						pStmt.setInt(3, EnvironmentalVariableType.RADIATION.getId());
						pStmt.setDouble(4, value);
						pStmt.addBatch();
					}
				}
				pStmt.executeBatch();
				conn.commit();
				conn.setAutoCommit(autoCommit);
			}
			return null;
		});
	}

}
