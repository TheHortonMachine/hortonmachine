package org.hortonmachine.geoframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

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
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.DynamicDoubleArray;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.chart.TimeSeries;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.optimizers.sceua.CostFunctions;
import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaConfig;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaOptimizer;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaResult;
import org.hortonmachine.geoframe.calibration.WaterBudgetCalibration;
import org.hortonmachine.geoframe.calibration.WaterBudgetParameters;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.core.WaterBudgetSimulation;
import org.hortonmachine.geoframe.core.parameters.RainSnowSeparationParameters;
import org.hortonmachine.geoframe.core.parameters.SnowMeltingParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetCanopyParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetGroundParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRootzoneParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRunoffParameters;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.geoframe.io.GeoframeWaterBudgetSimulationWriter;
import org.hortonmachine.geoframe.utils.IWaterBudgetSimulationRunner;
import org.hortonmachine.geoframe.utils.TopologyUtilities;
import org.hortonmachine.geoframe.utils.WaterSimulationRunner;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;
import org.hortonmachine.hmachine.modules.geomorphology.flow.OmsFlowDirections;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.skyview.OmsSkyview;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsGeoframeInputsBuilder;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;
import org.hortonmachine.hmachine.utils.GeoframeUtils;
import org.jfree.chart.ChartPanel;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import com.google.common.util.concurrent.AtomicDouble;

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

			String fromTS = "2015-10-01 01:00:00";
			String toTS = "2018-10-01 01:00:00";
			var timeStepMinutes = 60; // time step in minutes
			int spinUpDays = 365;
			double[] observedDischarge = getObservedDischarge(envDb, fromTS, toTS);
			boolean doCalibration = false;
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
			
			IWaterBudgetSimulationRunner runner = new WaterSimulationRunner();
			int spinUpTimesteps = (24 * 60 / timeStepMinutes) * spinUpDays;
			if (!doCalibration) {
				double[] params = {
						1.0340263745139564, 0.9070028793148661, -0.47966070577687664, 1.3315168762316274, 0.13977748085156966, 0.1638140752752137, 0.1684748175451627, 0.5990448867883432, 154.20975825107638, 0.15228866553605397, 0.8713004300910122, 1.3727726416540622, 24.582836535502437, 0.6112959748415321, 0.9357673578648056, 508.3183713385732, 0.7348783484874652, 0.9747709636832089
				};
				
				
				WaterBudgetParameters wbParams = WaterBudgetParameters.fromParameterArray(params);				
				
				// run a single simulation with default parameters
				double[] simQ = runner.run(fromTS, toTS, timeStepMinutes, maxBasinId, rootNode.clone(), basinAreas,
						wbParams, 0.6, // TODO handle LAI properly
						db,
						precipReader, tempReader, etpReader,
						null, pm);
				
				double kge = CostFunctions.kge(observedDischarge, simQ, spinUpTimesteps, HMConstants.doubleNovalue);
				System.out.println(kge);
				
				chartResult(simQ, observedDischarge, timeStepMinutes, fromTS, spinUpTimesteps);
			} else {
				WaterBudgetCalibration.psoCalibration(maxBasinId, basinAreas, rootNode, fromTS, toTS, timeStepMinutes, observedDischarge,
						calibrationThreadCount, precipReader, tempReader, etpReader, runner ,spinUpTimesteps, calibrationCounter, pm);

			}

		} finally {
			db.close();
			envDb.close();
		}
	}

	

	private void chartResult(double[] simQ, double[] observedDischarge, int timeStepMinutes, String fromTS,
			int spinUpTimesteps) {
	       	String title = "Simulated vs Observed Discharge";
	        String xLabel = "time";
	        String yLabel = "Q [m3]";
	        int width = 1600;
	        int height = 1000;

	        List<String> series = new ArrayList<>();
	        series.add("Simulated Discharge");
	        series.add("Observed Discharge");
	        List<Boolean> doLines = new ArrayList<>();
	        doLines.add(true);
	        doLines.add(true);
	        
	        long startTS = GeoframeEnvDatabaseIterator.str2ts(fromTS);
	        
	        List<double[]> allValuesList = new ArrayList<>();
	        List<long[]> allTimesList = new ArrayList<>();
	        // simulated
	        double[] simValues = new double[simQ.length];
	        double[] obsValues = new double[simQ.length];
	        long[] simTimes1 = new long[simQ.length];
	        long[] simTimes2 = new long[simQ.length];
	        for (int i = 0; i < simQ.length; i++) {
	        	simValues[i] = simQ[i];
	            simTimes1[i] = startTS + i * timeStepMinutes * 60 * 1000L;
	            
	            if (!HMConstants.isNovalue(observedDischarge[i])) {
	            	obsValues[i] = observedDischarge[i];
	            } else {
	            	obsValues[i] = 0.0;
	            }
	            simTimes2[i] = startTS + i * timeStepMinutes * 60 * 1000L;
			}
			
	        allValuesList.add(simValues);
			allTimesList.add(simTimes1);

			allValuesList.add(obsValues);
			allTimesList.add(simTimes2);

			TimeSeries timeseriesChart = new TimeSeries(title, series, allTimesList, allValuesList);
	        timeseriesChart.setXLabel(xLabel);
	        timeseriesChart.setYLabel(yLabel);
            timeseriesChart.setShowLines(doLines);
            
            timeseriesChart.setColors(new Color[] {Color.BLUE, Color.RED});
//	        if (doShapes != null)
//	            timeseriesChart.setShowShapes(doShapes);

	        ChartPanel chartPanel = new ChartPanel(timeseriesChart.getChart(), true);
	        Dimension preferredSize = new Dimension(width, height);
	        chartPanel.setPreferredSize(preferredSize);

//	        GuiUtilities.openDialogWithPanel(chartPanel, "HM Chart Window", preferredSize, false);
	        JDialog f = new JDialog();
	        f.add(chartPanel, BorderLayout.CENTER);
	        f.setTitle(title);
	        f.setModal(false);
	        f.pack();
//	        if (dimension != null)
//	            f.setSize(dimension);
	        f.setLocationRelativeTo(null); // Center on screen
	        f.setVisible(true);
	        f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	        f.getRootPane().registerKeyboardAction(e -> {
	            f.dispose();
	        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
		
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
