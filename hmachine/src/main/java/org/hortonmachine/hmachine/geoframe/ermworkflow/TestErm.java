package org.hortonmachine.hmachine.geoframe.ermworkflow;

import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.optimizers.CostFunctions;
import org.hortonmachine.gears.utils.optimizers.particleswarm.PSConfig;
import org.hortonmachine.hmachine.geoframe.calibration.WaterBudgetCalibration;
import org.hortonmachine.hmachine.geoframe.calibration.WaterBudgetParameters;
import org.hortonmachine.hmachine.geoframe.core.TopologyNode;
import org.hortonmachine.hmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.hmachine.geoframe.utils.TopologyUtilities;
import org.hortonmachine.hmachine.geoframe.utils.WaterSimulationRunner;

public class TestErm extends HMModel {

	public TestErm() throws Exception {
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
		String geoframeGpkg = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/workspace/outputs/geoframe_data.gpkg";

		try(ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb()){
		db.open(geoframeGpkg);

			// TODO here a potential evapotrans comes in

			// get the max basin id from the db
			int maxBasinId = IWaterBudgetSimulationRunner.getMaxBasinId(db);
			double[] basinAreas = IWaterBudgetSimulationRunner.getBasinAreas(db, maxBasinId);

			// get the topology from the db
			TopologyNode rootNode = TopologyUtilities.getRootNodeFromDb(db);

			//////////////////////////////////////////////////
			/// PARAMETERS
			//////////////////////////////////////////////////
//			public static String START_TIMESTAMP = "2018-01-01 01:00";
//			public static String END_TIMESTAMP = "2022-12-31 01:00";
			String fromTS = "2018-10-01 01:00:00";
			String toTS = "2022-12-31 01:00:00";
			var timeStepMinutes = 60; // time step in minutes
			int spinUpDays = 365;
			double[] observedDischarge = IWaterBudgetSimulationRunner.getObservedDischarge(db, fromTS, toTS);
			boolean doCalibration = false;
			int psoIterations = 300;
			boolean writeState = false;
			int calibrationThreadCount = 20;
			CostFunctions costFunction = CostFunctions.KGE;

			var precipReader = new GeoframeEnvDatabaseIterator();
			precipReader.db = db;
			precipReader.pMaxId = maxBasinId;
			precipReader.pParameterId = 2; // precip
			precipReader.tStart = fromTS;
			precipReader.tEnd = toTS;
			if (doCalibration) {
				precipReader.preCacheData();
			}

			var tempReader = new GeoframeEnvDatabaseIterator();
			tempReader.db = db;
			tempReader.pParameterId = 4; // temperature
			tempReader.pMaxId = maxBasinId;
			tempReader.tStart = fromTS;
			tempReader.tEnd = toTS;
			if (doCalibration) {
				tempReader.preCacheData();
			}

			var etpReader = new GeoframeEnvDatabaseIterator();
			etpReader.db = db;
			etpReader.pParameterId = 1; // etp
			etpReader.pMaxId = maxBasinId;
			etpReader.tStart = fromTS;
			etpReader.tEnd = toTS;
			if (doCalibration) {
				etpReader.preCacheData();
			}

			IWaterBudgetSimulationRunner runner = new WaterSimulationRunner();
			int spinUpTimesteps = (24 * 60 / timeStepMinutes) * spinUpDays;
			if (!doCalibration) {

				double[] params = {
//						0.940739667840607, 0.96075954632785, -0.681919048298992, 1.3971214752557832, 0.33951109114109224, 0.09437578309755729, 0.3755301937475191, 0.6209768892258773, 166.8213039282626, 0.12887303016873772, 0.8868181607935712, 1.8827257470886904, 48.43029773696331, 1.0623972142583038, 0.9188401606071446, 745.4366397791149, 0.9812359783377442, 0.9798892148598694
						0.8000000002105654, 0.8792250568698996, 0.05108695411643222, 0.11239951741563044, 9.877935737098644E-4, 0.1515957937664112, 0.2588546932492243, 0.6021034732391685, 88.53249714981247, 0.7689827947151374, 0.8750362719004473, 1.4969090358990143, 26.00755896651049, 1.9027073830044883, 0.9506803591951801, 149.6608142417257, 0.7089454206868994, 0.9653872588350276
						};

				runSimulationOnParams(db, maxBasinId, basinAreas, rootNode, fromTS, timeStepMinutes, observedDischarge,
						precipReader, tempReader, etpReader, runner, spinUpTimesteps, params, writeState);
			} else {
				PSConfig psConfig = new PSConfig();
				psConfig.particlesNum = 20;
				psConfig.maxIterations = psoIterations;
				psConfig.c1 = 2.0;
				psConfig.c2 = 2.0;
				psConfig.w0 = 0.9;
				psConfig.decay = 0.4;

//				double[] bestParams = 
						WaterBudgetCalibration.psoCalibration(psConfig, maxBasinId, basinAreas, rootNode,
						timeStepMinutes, observedDischarge, costFunction, calibrationThreadCount, precipReader,
						tempReader, etpReader, runner, spinUpTimesteps, writeState, pm);

				
//				runSimulationOnParams(db, maxBasinId, basinAreas, rootNode, fromTS, timeStepMinutes, observedDischarge,
//						precipReader, tempReader, etpReader, runner, spinUpTimesteps, bestParams, writeState);
			}

		}
	}

	private void runSimulationOnParams(ASpatialDb db, int maxBasinId, double[] basinAreas, TopologyNode rootNode,
			String fromTS, int timeStepMinutes, double[] observedDischarge, GeoframeEnvDatabaseIterator precipReader,
			GeoframeEnvDatabaseIterator tempReader, GeoframeEnvDatabaseIterator etpReader,
			IWaterBudgetSimulationRunner runner, int spinUpTimesteps, double[] params, boolean writeState) throws Exception {
		runner.configure(timeStepMinutes, maxBasinId, rootNode, basinAreas, true, true, writeState, db, pm);
		WaterBudgetParameters wbParams = WaterBudgetParameters.fromParameterArray(params);

		// run a single simulation with default parameters
		double[] simQ = runner.run(wbParams, 0.6, // TODO handle LAI properly
				precipReader, tempReader, etpReader, null);

		double cost = -CostFunctions.KGE.evaluateCost(observedDischarge, simQ, spinUpTimesteps,
				HMConstants.doubleNovalue);
		// format cost ##,00
		cost = Math.round(cost * 100.0) / 100.0;
		String title = "Simulated vs Observed Discharge ( KGE: " + cost + " )";
		IWaterBudgetSimulationRunner.quickChartResult(title, simQ, observedDischarge, timeStepMinutes, fromTS, spinUpTimesteps);
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
		new TestErm();
	}

}