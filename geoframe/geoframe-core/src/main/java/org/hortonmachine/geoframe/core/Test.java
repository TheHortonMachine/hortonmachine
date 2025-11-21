package org.hortonmachine.geoframe.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.DynamicDoubleArray;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.optimizers.sceua.KGE;
import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaConfig;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaOptimizer;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaResult;
import org.hortonmachine.geoframe.core.parameters.RainSnowSeparationParameters;
import org.hortonmachine.geoframe.core.parameters.SnowMeltingParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetCanopyParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetGroundParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRootzoneParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRunoffParameters;
import org.hortonmachine.geoframe.core.utils.TopologyNode;
import org.hortonmachine.geoframe.core.utils.TopologyUtilities;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;
import org.hortonmachine.hmachine.modules.geomorphology.flow.OmsFlowDirections;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.skyview.OmsSkyview;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsGeoframeInputsBuilder;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;
import org.hortonmachine.hmachine.utils.GeoframeUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

public class Test extends HMModel {
	
	private AtomicInteger calibrationCounter = new AtomicInteger(0);

	public Test() throws Exception {
		// gura
//		String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/gura/";
//		String ext = ".tif";
//		int thres = 500;
//		double desiredArea = 500.0;
//		double easting = 265340.845;
//		double northing = 9934464.184;

		// flanginec
//		String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/flanginec/";
//		String ext = ".tif";
//		int thres = 100;
//		double desiredArea = 100.0;
//		double easting = 1637993.497;
//		double northing = 5111925.950;

		// NOCE SMALL
//		String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce_mini/";
//		String ext = ".tif";
//		int drainThres = 5000;
//		double desiredArea = 10000.0;
//		double desiredAreaDelta = 200.0;
//		double easting = 623519.2969;
//		double northing = 5128704.4571;

		// NOCE
		String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/";
		String ext = ".tif";
		int drainThres = 2000;
		double desiredArea = 30_000; // 1_000_000.0; 
		double desiredAreaDelta = 20.0;
		double easting = 629720;
		double northing = 5127690;

		String dtm = folder + "inputs/dtm" + ext;
		String pit = folder + "outputs/pit" + ext;
		String flow = folder + "outputs/flow" + ext;
		String drain = folder + "outputs/drain" + ext;
		String tca = folder + "outputs/tca" + ext;
		String net = folder + "outputs/net" + ext;
		String basin = folder + "outputs/basin" + ext;
		String basin_resized = folder + "outputs/basin_resized" + ext;

		String basinpit = folder + "outputs/basin_pit" + ext;
		String basindrain = folder + "outputs/basin_drain" + ext;
		String basintca = folder + "outputs/basin_tca" + ext;
		String basinnet = folder + "outputs/basin_net" + ext;

		String basinnetnum = folder + "outputs/basin_netnum" + ext;
		String basinnetbasins = folder + "outputs/basin_netnumbasins" + ext;
		String basinnetbasinsdesired = folder + "outputs/basin_netnumbasins_desired" + ext;
		String topology = folder + "outputs/topology.txt";

		String skyview = folder + "outputs/skyview" + ext;
		String basinskyview = folder + "outputs/basin_skyview" + ext;

		String geoframeFolder = folder + "outputs/geoframe_inputs/";

		String geoframeGpkg = folder + "outputs/geoframe_data.gpkg";

		String envDataPath = "/home/hydrologis/storage/lavori_tmp/GEOFRAME/env_data.sqlite";

		File outFolder = new File(folder + "outputs/");
		if (!outFolder.exists()) {
			outFolder.mkdirs();
		}
//		if (!toDo(geoframeGpkg)) {
//			new File(geoframeGpkg).delete();
//		}
		ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();
		db.open(geoframeGpkg);

		ADb envDb = EDb.SQLITE.getDb();
		envDb.open(envDataPath);
		
		try {

			if (toDo(pit)) {
				OmsPitfiller pitfiller = new OmsPitfiller();
				pitfiller.inElev = getRaster(dtm);
				pitfiller.process();
				dumpRaster(pitfiller.outPit, pit);
				makeQgisStyleForRaster(EColorTables.elev.name(), pit, 0);
			}

			if (toDo(flow)) {
				OmsFlowDirections flowdirections = new OmsFlowDirections();
				flowdirections.inPit = getRaster(pit);
				flowdirections.pMinElev = 0;
				flowdirections.process();
				dumpRaster(flowdirections.outFlow, flow);
				makeQgisStyleForRaster(EColorTables.flow.name(), flow, 0);
			}

			if (toDo(drain)) {
				OmsDrainDir draindir = new OmsDrainDir();
				draindir.inPit = getRaster(pit);
				draindir.inFlow = getRaster(flow);
				draindir.pLambda = 1.0;
				draindir.doLad = true;
				draindir.process();
				dumpRaster(draindir.outFlow, drain);
				dumpRaster(draindir.outTca, tca);
				makeQgisStyleForRaster(EColorTables.flow.name(), drain, 0);
				makeQgisStyleForRaster(EColorTables.logarithmic.name(), tca, 0);
			}

			if (toDo(net)) {
				OmsExtractNetwork extractnetwork = new OmsExtractNetwork();
				extractnetwork.inTca = getRaster(tca);
				extractnetwork.inFlow = getRaster(flow);
				extractnetwork.pThres = drainThres;
				extractnetwork.process();
				dumpRaster(extractnetwork.outNet, net);
				makeQgisStyleForRaster(EColorTables.net.name(), net, 0);
			}

			if (toDo(skyview)) {
				OmsSkyview sv = new OmsSkyview();
				sv.inElev = getRaster(pit);
				sv.process();
				dumpRaster(sv.outSky, skyview);
				makeQgisStyleForRaster(EColorTables.slope.name(), skyview, 0);
			}

			if (toDo(basin)) {
				OmsExtractBasin extractbasin = new OmsExtractBasin();
				extractbasin.inFlow = getRaster(drain);
				extractbasin.pEast = easting;
				extractbasin.pNorth = northing;
				extractbasin.process();
				dumpRaster(extractbasin.outBasin, basin);
				makeQgisStyleForRaster(EColorTables.net.name(), basin, 0);
			}

			if (toDo(basin_resized)) {
				OmsRasterResizer resizer = new OmsRasterResizer();
				GridCoverage2D basinGC = getRaster(basin);
				resizer.inRaster = basinGC;
				RegionMap dataRegionMap = HMRaster.fromGridCoverage(basinGC).getDataRegionMap();
				Envelope dataEnvelope = dataRegionMap.toEnvelope();
				Polygon dataPolygon = GeometryUtilities.createPolygonFromEnvelope(dataEnvelope);
				SimpleFeatureCollection fc = FeatureUtilities
						.featureCollectionFromGeometry(basinGC.getCoordinateReferenceSystem(), dataPolygon);
				resizer.inVector = fc;
				resizer.process();
				dumpRaster(resizer.outRaster, basin_resized);
			}

			// cutout for the basin only
			OmsCutOut cutout = new OmsCutOut();
			if (toDo(basinpit)) {
				cutout.inRaster = getRaster(pit);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basinpit);
				makeQgisStyleForRaster(EColorTables.elev.name(), basinpit, 0);
			}

			if (toDo(basindrain)) {
				cutout = new OmsCutOut();
				cutout.inRaster = getRaster(drain);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basindrain);
				makeQgisStyleForRaster(EColorTables.flow.name(), basindrain, 0);
			}

			if (toDo(basintca)) {
				cutout = new OmsCutOut();
				cutout.inRaster = getRaster(tca);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basintca);
				makeQgisStyleForRaster(EColorTables.logarithmic.name(), basintca, 0);
			}

			if (toDo(basinnet)) {
				cutout = new OmsCutOut();
				cutout.inRaster = getRaster(net);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basinnet);
				makeQgisStyleForRaster(EColorTables.net.name(), basinnet, 0);
			}

			if (toDo(basinskyview)) {
				cutout = new OmsCutOut();
				cutout.inRaster = getRaster(skyview);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basinskyview);
				makeQgisStyleForRaster(EColorTables.slope.name(), basinskyview, 0);
			}

			if (toDo(basinnetnum) || !db.hasTable(SqlName.m(GeoframeUtils.GEOFRAME_TOPOLOGY_TABLE))) {
				OmsNetNumbering nn = new OmsNetNumbering();
				nn.inFlow = getRaster(basindrain);
				nn.inNet = getRaster(basinnet);
				nn.inTca = getRaster(basintca);
				nn.pDesiredArea = desiredArea;
				nn.pDesiredAreaDelta = desiredAreaDelta;
				nn.inGeoframeDb = db;
				nn.process();
				dumpRaster(nn.outNetnum, basinnetnum);
				dumpRaster(nn.outBasins, basinnetbasins);
				dumpRaster(nn.outDesiredBasins, basinnetbasinsdesired);
//			FileUtilities.writeFile(nn.outGeoframeTopology, new File(topology));
				makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetnum, 0);
				makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetbasins, 0);
				makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetbasinsdesired, 0);
			}

			if (!db.hasTable(SqlName.m(GeoframeUtils.GEOFRAME_BASIN_TABLE))
					|| !db.hasTable(SqlName.m(GeoframeUtils.GEOFRAME_NETWORK_TABLE))) {
				OmsGeoframeInputsBuilder builder = new OmsGeoframeInputsBuilder();
				builder.inPitfiller = basinpit;
				builder.inDrain = basindrain;
				builder.inTca = basintca;
				builder.inNet = basinnet;
				builder.inSkyview = basinskyview;
				builder.inBasins = basinnetbasinsdesired;
				// builder.inGeoframeTopology = topology;
				// builder.outFolder = geoframeFolder;
				builder.inGeoframeDb = db;
				builder.process();
			}

			// TODO here a potential evapotrans comes in

			// TODO part in which rain, temperature, radiation and evapotransp are handled

			// get the max basin id from the db
			int maxBasinId = db.getLong("select max(basinid) from " + GeoframeUtils.GEOFRAME_BASIN_TABLE).intValue();

			// get the basins from the db and their areas
			QueryResult queryResult = db.getTableRecordsMapIn(GeoframeUtils.GEOFRAME_BASIN_TABLE, null, -1, -1, null);
			double[] basinAreas = new double[maxBasinId + 1];
			int idIndex = queryResult.names.indexOf("basinid");
			for (int i = 0; i < queryResult.data.size(); i++) {
				Object[] row = queryResult.data.get(i);
				int basinId = (int) row[idIndex];
				Geometry basinGeom = (Geometry) row[queryResult.geometryIndex];
				double area = basinGeom.getArea() / 1_000_000.0; // in km2
				basinAreas[basinId] = area;
			}

			// get the topology from the db
			TopologyNode rootNode = TopologyUtilities.getRootNodeFromDb(db);

			String fromTS = "2001-01-01 01:00:00";
			String toTS = "2004-01-01 01:00:00";
			var timeStepMinutes = 60; // time step in minutes
			int spinUpDays = 180;
			double[] observedDischarge = getObservedDischarge(envDb, fromTS, toTS);
			boolean doCalibration = true;
			int calibrationThreadCount = 20;
			
			var precipReader = new GeoframeEnvDatabaseIterator(maxBasinId);
			precipReader.db = envDb;
			precipReader.pParameterId = 2; // precip
			precipReader.tStart = fromTS;
			precipReader.tEnd = toTS;
			if (doCalibration) {
				precipReader.preCacheData();
			}

			var tempReader = new GeoframeEnvDatabaseIterator(maxBasinId);
			tempReader.db = envDb;
			tempReader.pParameterId = 4; // temperature
			tempReader.tStart = fromTS;
			tempReader.tEnd = toTS;
			if (doCalibration) {
				tempReader.preCacheData();
			}

			var etpReader = new GeoframeEnvDatabaseIterator(maxBasinId);
			etpReader.db = envDb;
			etpReader.pParameterId = 1; // etp
			etpReader.tStart = fromTS;
			etpReader.tEnd = toTS;
			if (doCalibration) {
				etpReader.preCacheData();
			}
			
			int spinUpTimesteps = (24 * 60 / timeStepMinutes) * spinUpDays;
			if (!doCalibration) {
//				Best objective (cost) = 0.7175186988286195
//				Best params = [0.8, 1.2569136394087654, 1.932469419839689, 
//								0.0022958543552148054, 1.0E-4, 0.012081897205468567, 
//								0.8827651473577803, 0.8281084929808293, 
//								145.44367883355892, 0.22833855129992142, 2.256007161307445, 0.506427242953237, 
//								15.250391376469448, 0.4007918724273609, 1.001258062874522, 
//								988.7809685222537, 2.8950706950524037E-4, 3.0]
//				Best KGE = -0.7175186988286195
				RainSnowSeparationParameters rssepP = new RainSnowSeparationParameters(1.0, 0.8, 1.2569, 1.9325);
				SnowMeltingParameters snowmP = new SnowMeltingParameters(0.00229585, 1.0E-4, 0.0120819);
				WaterBudgetCanopyParameters wbcP = new WaterBudgetCanopyParameters(0.882765, 0.828108);
				WaterBudgetRootzoneParameters wbRzP = new WaterBudgetRootzoneParameters(145.4437, 0.228339, 2.256007, 0.506427);
				WaterBudgetRunoffParameters wbrP = new WaterBudgetRunoffParameters(15.25039, 0.400792, 1.001258);
				WaterBudgetGroundParameters wbgP = new WaterBudgetGroundParameters(988.7810, 2.89507E-4, 3.0);
				
				// run a single simulation with default parameters
				double[] simQ = runSimulation(fromTS, toTS, timeStepMinutes, maxBasinId, rootNode.clone(), basinAreas,
						rssepP, snowmP,
						wbcP,
						wbRzP,
						wbrP,
						wbgP, 0.6, // TODO handle LAI properly
						db,
						precipReader, tempReader, etpReader);
				double kge = KGE.kge(simQ, observedDischarge, spinUpTimesteps, HMConstants.doubleNovalue);
				System.out.println("KGE with default parameters = " + kge);
			} else {
				List<ParameterBounds> allParameterBounds = new ArrayList<>();
				allParameterBounds.addAll(RainSnowSeparationParameters.calibrationParameterBounds());
				allParameterBounds.addAll(SnowMeltingParameters.calibrationParameterBounds());
				allParameterBounds.addAll(WaterBudgetCanopyParameters.calibrationParameterBounds());
				allParameterBounds.addAll(WaterBudgetRootzoneParameters.calibrationParameterBounds());
				allParameterBounds.addAll(WaterBudgetRunoffParameters.calibrationParameterBounds());
				allParameterBounds.addAll(WaterBudgetGroundParameters.calibrationParameterBounds());

				SceUaOptimizer.ObjectiveFunction objFn = params -> {
					var lai = 0.6; // TODO handle LAI properly

					int i = 0;
					double alpha_r = params[i++];
					double alpha_s = params[i++];
					double meltingTemperature = params[i++];
					var m1 = 1.0; // TODO handle m1 properly
					RainSnowSeparationParameters rssepParamCalib = new RainSnowSeparationParameters(m1, alpha_r,
							alpha_s, meltingTemperature);

					double combinedMeltingFactor = params[i++];
					double freezingFactor = params[i++];
					double alfa_l = params[i++];
					SnowMeltingParameters snowMParamsCalib = new SnowMeltingParameters(combinedMeltingFactor,
							freezingFactor, alfa_l);

					double kc = params[i++];
					double p = params[i++];
					WaterBudgetCanopyParameters wbCanopyParamsCalib = new WaterBudgetCanopyParameters(kc, p);

					double s_RootZoneMax = params[i++];
					double g = params[i++];
					double h = params[i++];
					double pB_soil = params[i++];
					WaterBudgetRootzoneParameters wbRootzoneParamsCalib = new WaterBudgetRootzoneParameters(
							s_RootZoneMax, g, h, pB_soil);

					double sRunoffMax = params[i++];
					double c = params[i++];
					double d = params[i++];
					WaterBudgetRunoffParameters wbRunoffParamsCalib = new WaterBudgetRunoffParameters(sRunoffMax, c, d);

					double s_GroundWaterMax = params[i++];
					double e = params[i++];
					double f = params[i++];
					WaterBudgetGroundParameters wbGroundParamsCalib = new WaterBudgetGroundParameters(s_GroundWaterMax,
							e, f);

					double[] simQ = runSimulation(fromTS, toTS, timeStepMinutes, maxBasinId, rootNode.clone(),
							basinAreas, rssepParamCalib, snowMParamsCalib, wbCanopyParamsCalib, wbRootzoneParamsCalib,
							wbRunoffParamsCalib, wbGroundParamsCalib, lai, null,
							precipReader, tempReader, etpReader);
					return KGE.kgeCost(simQ, observedDischarge, spinUpTimesteps, HMConstants.doubleNovalue); // minimize -KGE
				};
				SceUaConfig config = SceUaConfig.builder().maxIterations(1200).maxEvaluations(1200).complexCount(5)
						.objectiveStdTolerance(1e-4).random(new Random(42L)) // deterministic
						.verbose(false).build();

				SceUaOptimizer optimizer = new SceUaOptimizer(allParameterBounds, objFn, config);
				SceUaResult result = optimizer.optimizeParallel(calibrationThreadCount);

				double[] best = result.getBestParameters();
				double bestObj = result.getBestObjective();

				System.out.println("Best objective (cost) = " + bestObj);
				System.out.println("Best params = " + Arrays.toString(best));

				// Convert cost back to KGE for interpretation
				double bestKGE = -bestObj;
				System.out.println("Best KGE = " + bestKGE);

			}

		} finally {
			db.close();
			envDb.close();
		}
	}

	private double[] runSimulation(String fromTS, String toTS, int timeStepMinutes, int maxBasinId, //
			TopologyNode rootNode, double[] basinAreas, //
			RainSnowSeparationParameters rssepParam, SnowMeltingParameters snowMParams, WaterBudgetCanopyParameters wbCanopyParams, 
			WaterBudgetRootzoneParameters wbRootzoneParams, WaterBudgetRunoffParameters wbRunoffParams, 
			WaterBudgetGroundParameters wbGroundParams, double lai, //
			ADb outputDb, //
			GeoframeEnvDatabaseIterator precipReader, GeoframeEnvDatabaseIterator tempReader, GeoframeEnvDatabaseIterator etpReader
			) throws Exception {
		int calibRun = calibrationCounter.incrementAndGet();

		long t1 = System.currentTimeMillis();
		System.out.println("Running simulation " +  calibRun);
		
		

		GeoframeWaterBudgetSimulationWriter resultsWriter = null;
		if (outputDb != null) {
			resultsWriter = new GeoframeWaterBudgetSimulationWriter();
			resultsWriter.db = outputDb;
			resultsWriter.rootNode = rootNode;
		}

		double[] initialConditionSolidWater = new double[maxBasinId + 1]; // ok init with 0s
		double[] initialConditionLiquidWater = new double[maxBasinId + 1]; // ok init with 0s
		double[] initalConditionsCanopyMap = new double[maxBasinId + 1];
		Arrays.fill(initalConditionsCanopyMap, Double.NaN);
		double[] initalConditionsRootzoneMap = new double[maxBasinId + 1];
		Arrays.fill(initalConditionsRootzoneMap, Double.NaN);
		double[] initalConditionsRunoffMap = new double[maxBasinId + 1];
		Arrays.fill(initalConditionsRunoffMap, Double.NaN);
		double[] initalConditionsGroundMap = new double[maxBasinId + 1];
		Arrays.fill(initalConditionsGroundMap, Double.NaN);
		
		
		WaterBudgetSimulation wbSim = new WaterBudgetSimulation();
		wbSim.pm = pm;
		wbSim.rootNode = rootNode;
		wbSim.basinAreas = basinAreas;
		wbSim.timeStepMinutes = timeStepMinutes;
		wbSim.precipReader = precipReader;
		wbSim.tempReader = tempReader;
		wbSim.etpReader = etpReader;
		wbSim.initialConditionSolidWater = initialConditionSolidWater;
		wbSim.initialConditionLiquidWater = initialConditionLiquidWater;
		wbSim.initalConditionsCanopyMap = initalConditionsCanopyMap;
		wbSim.initalConditionsRootzoneMap = initalConditionsRootzoneMap;
		wbSim.initalConditionsRunoffMap = initalConditionsRunoffMap;
		wbSim.initalConditionsGroundMap = initalConditionsGroundMap;
		wbSim.rssepParam = rssepParam;
		wbSim.snowMParams = snowMParams;
		wbSim.wbCanopyParams = wbCanopyParams;
		wbSim.wbRootzoneParams = wbRootzoneParams;
		wbSim.wbRunoffParams = wbRunoffParams;
		wbSim.wbGroundParams = wbGroundParams;
		wbSim.lai = lai;
		wbSim.resultsWriter = resultsWriter;
		wbSim.doParallel = false;
		wbSim.doTopologically = false;
		wbSim.doDebugMessages = outputDb != null ;

		wbSim.init();
		wbSim.process();
		
		var lastNodeDischargeArray = wbSim.outRootNodeDischargeInTime;
		var sim = lastNodeDischargeArray.getTrimmedInternalArray();
		
		long t2 = System.currentTimeMillis();
		System.out.println("Simulation run " + calibRun + " completed in " + (t2 - t1) / 1000.0 + " seconds.");
		return sim;
	}

	private double[] getObservedDischarge(ADb envDb, String fromTS, String toTS) throws Exception {
		long from = GeoframeEnvDatabaseIterator.str2ts(fromTS);
		long to = GeoframeEnvDatabaseIterator.str2ts(toTS);
		String sql = "select ts, value from observed_discharge where ts >= " + from + " " + "and ts <= " + to + " order by ts asc";
		QueryResult qr = envDb.getTableRecordsMapFromRawSql(sql, -1);
		DynamicDoubleArray dda = new DynamicDoubleArray(10000, 10000);
		int valueIndex = qr.names.indexOf("value");
		for (Object[] row : qr.data) {
			double value = ((Number) row[valueIndex]).doubleValue();
			dda.addValue(value);
		}
		return dda.getTrimmedInternalArray();
	}

	private boolean toDo(String filepath) {
		return new File(filepath).exists() == false;
	}

	public static void makeQgisStyleForRaster(String tableName, String rasterPath, int labelDecimals) throws Exception {
		OmsRasterSummary s = new OmsRasterSummary();
		s.pm = new DummyProgressMonitor();
		s.inRaster = OmsRasterReader.readRaster(rasterPath);
		s.process();
		double min = s.outMin;
		double max = s.outMax;

		String style = RasterStyleUtilities.createQGISRasterStyle(tableName, min, max, null, labelDecimals);
		File styleFile = FileUtilities.substituteExtention(new File(rasterPath), "qml");
		FileUtilities.writeFile(style, styleFile);
	}

	public static void main(String[] args) throws Exception {
		new Test();
	}

}
