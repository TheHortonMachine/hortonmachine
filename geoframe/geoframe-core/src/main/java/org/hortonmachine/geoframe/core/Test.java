package org.hortonmachine.geoframe.core;

import java.io.File;
import java.util.Arrays;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
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
		int drainThres = 5000;
		double desiredArea = 1_000_000.0;
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
			for(int i = 0; i < queryResult.data.size(); i++) {
				Object[] row = queryResult.data.get(i);
				int basinId = (int) row[idIndex];
				Geometry basinGeom = (Geometry) row[queryResult.geometryIndex];
				double area = basinGeom.getArea() / 1_000_000.0; // in km2
				basinAreas[basinId] = area;
			}
			
			// get the topology from the db
			TopologyNode rootNode = TopologyUtilities.getRootNodeFromDb(db);
			pm.message("Topology:\n" + TopologyNode.toAsciiTree(rootNode));
			// create a map of basinId -> TopologyNode
			TopologyNode[] basinid2nodeMap = new TopologyNode[maxBasinId + 1];
			rootNode.visitUpstream(node -> {
				basinid2nodeMap[node.basinId] = node;
			});

			String fromTS = "2005-01-01 01:00:00";
			String toTS = "2006-01-01 01:00:00";
			var timeStepMinutes = 60; // time step in minutes

			var precipReader = new GeoframeEnvDatabaseIterator(maxBasinId);
			precipReader.db = envDb;
			precipReader.pParameterId = 2; // precip
			precipReader.tStart = fromTS;
			precipReader.tEnd = toTS;

			var tempReader = new GeoframeEnvDatabaseIterator(maxBasinId);
			tempReader.db = envDb;
			tempReader.pParameterId = 4; // temperature
			tempReader.tStart = fromTS;
			tempReader.tEnd = toTS;

			var etpReader = new GeoframeEnvDatabaseIterator(maxBasinId);
			etpReader.db = envDb;
			etpReader.pParameterId = 1; // etp
			etpReader.tStart = fromTS;
			etpReader.tEnd = toTS;


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
			
			// CALIRATION PARAMETERS
			RainSnowSeparationParameters rssepParam = RainSnowSeparationParameters.CALIBRATION_DEFAULT;
			SnowMeltingParameters snowMParams = SnowMeltingParameters.CALIBRATION_DEFAULT;
			WaterBudgetCanopyParameters wbCanopyParams = WaterBudgetCanopyParameters.CALIBRATION_DEFAULT;
			WaterBudgetRootzoneParameters wbRootzoneParams = WaterBudgetRootzoneParameters.CALIBRATION_DEFAULT;
			WaterBudgetRunoffParameters wbRunoffParams = WaterBudgetRunoffParameters.CALIBRATION_DEFAULT;
			WaterBudgetGroundParameters wbGroundParams = WaterBudgetGroundParameters.CALIBRATION_DEFAULT;
			
			var lai = 0.6; // TODO handle LAI properly
			
			WaterBudgetSimulation wbSim = new WaterBudgetSimulation();
			wbSim.pm = pm;
			wbSim.basinAreas = basinAreas;
			wbSim.rootNode = rootNode;
			wbSim.basinid2nodeMap = basinid2nodeMap;
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
			wbSim.process();
			
			TopologyNode nodeWithResults = wbSim.rootNode;
			
//			pm.message(TopologyNode.toAsciiTree(nodeWithResults));

		} finally {
			db.close();
			envDb.close();
		}
	}


	private boolean toDo(String filepath) {
		return new File(filepath).exists() == false;
	}
	
    public static void makeQgisStyleForRaster( String tableName, String rasterPath, int labelDecimals ) throws Exception {
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
