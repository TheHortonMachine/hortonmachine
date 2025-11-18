package org.hortonmachine.geoframe.core;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.io.File;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.HM;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.gears.modules.r.rasteronvectorresizer.OmsRasterResizer;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;
import org.hortonmachine.hmachine.modules.geomorphology.flow.OmsFlowDirections;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.skyview.OmsSkyview;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;
import org.hortonmachine.hmachine.utils.GeoframeUtils;
import org.hortonmachine.modules.GeoframeInputsBuilder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import canopyOut.WaterBudgetCanopyOUT;
import canopyOut.WaterBudgetCanopyOUT.WaterBudgetCanopyStepResult;
import it.geoframe.blogspot.snowmelting.pointcase.SnowMeltingPointCaseDegreeDay;
import it.geoframe.blogspot.snowmelting.pointcase.SnowMeltingPointCaseDegreeDay.SnowStepResult;
import rainSnowSperataion.RainSnowSeparationPointCase;
import rootZone.WaterBudgetRootZone;
import rootZone.WaterBudgetRootZone.WaterBudgetRootZoneStepResult;
import simpleBucket.WaterBudget;
import simpleBucket.WaterBudget.WaterBudgetStepResult;

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
				HM.makeQgisStyleForRaster(EColorTables.elev.name(), pit, 0);
			}

			if (toDo(flow)) {
				OmsFlowDirections flowdirections = new OmsFlowDirections();
				flowdirections.inPit = getRaster(pit);
				flowdirections.pMinElev = 0;
				flowdirections.process();
				dumpRaster(flowdirections.outFlow, flow);
				HM.makeQgisStyleForRaster(EColorTables.flow.name(), flow, 0);
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
				HM.makeQgisStyleForRaster(EColorTables.flow.name(), drain, 0);
				HM.makeQgisStyleForRaster(EColorTables.logarithmic.name(), tca, 0);
			}

			if (toDo(net)) {
				OmsExtractNetwork extractnetwork = new OmsExtractNetwork();
				extractnetwork.inTca = getRaster(tca);
				extractnetwork.inFlow = getRaster(flow);
				extractnetwork.pThres = drainThres;
				extractnetwork.process();
				dumpRaster(extractnetwork.outNet, net);
				HM.makeQgisStyleForRaster(EColorTables.net.name(), net, 0);
			}

			if (toDo(skyview)) {
				OmsSkyview sv = new OmsSkyview();
				sv.inElev = getRaster(pit);
				sv.process();
				dumpRaster(sv.outSky, skyview);
				HM.makeQgisStyleForRaster(EColorTables.slope.name(), skyview, 0);
			}

			if (toDo(basin)) {
				OmsExtractBasin extractbasin = new OmsExtractBasin();
				extractbasin.inFlow = getRaster(drain);
				extractbasin.pEast = easting;
				extractbasin.pNorth = northing;
				extractbasin.process();
				dumpRaster(extractbasin.outBasin, basin);
				HM.makeQgisStyleForRaster(EColorTables.net.name(), basin, 0);
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
				HM.makeQgisStyleForRaster(EColorTables.elev.name(), basinpit, 0);
			}

			if (toDo(basindrain)) {
				cutout = new OmsCutOut();
				cutout.inRaster = getRaster(drain);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basindrain);
				HM.makeQgisStyleForRaster(EColorTables.flow.name(), basindrain, 0);
			}

			if (toDo(basintca)) {
				cutout = new OmsCutOut();
				cutout.inRaster = getRaster(tca);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basintca);
				HM.makeQgisStyleForRaster(EColorTables.logarithmic.name(), basintca, 0);
			}

			if (toDo(basinnet)) {
				cutout = new OmsCutOut();
				cutout.inRaster = getRaster(net);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basinnet);
				HM.makeQgisStyleForRaster(EColorTables.net.name(), basinnet, 0);
			}

			if (toDo(basinskyview)) {
				cutout = new OmsCutOut();
				cutout.inRaster = getRaster(skyview);
				cutout.inMask = getRaster(basin_resized);
				cutout.process();
				dumpRaster(cutout.outRaster, basinskyview);
				HM.makeQgisStyleForRaster(EColorTables.slope.name(), basinskyview, 0);
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
				HM.makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetnum, 0);
				HM.makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetbasins, 0);
				HM.makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetbasinsdesired, 0);
			}

			if (!db.hasTable(SqlName.m(GeoframeUtils.GEOFRAME_BASIN_TABLE))
					|| !db.hasTable(SqlName.m(GeoframeUtils.GEOFRAME_NETWORK_TABLE))) {
				GeoframeInputsBuilder builder = new GeoframeInputsBuilder();
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
			
			// get the basins from the db and their areas
			QueryResult queryResult = db.getTableRecordsMapIn(GeoframeUtils.GEOFRAME_BASIN_TABLE, null, -1, -1, null);
			HashMap<Integer, Double> basinAreas = new HashMap<>();
			int idIndex = queryResult.names.indexOf("basinid");
			for(int i = 0; i < queryResult.data.size(); i++) {
				Object[] row = queryResult.data.get(i);
				int basinId = (int) row[idIndex];
				Geometry basinGeom = queryResult.geometries.get(i);
				double area = basinGeom.getArea() / 1_000_000.0; // in km2
				basinAreas.put(basinId, area);
			}
			

			String fromTS = "2005-01-01 01:00:00";
			String toTS = "2005-01-01 02:00:00";

			var precipReader = new GeoframeEnvDatabaseIterator();
			precipReader.db = envDb;
			precipReader.pParameterId = 2; // precip
			precipReader.tStart = fromTS;
			precipReader.tEnd = toTS;

			var tempReader = new GeoframeEnvDatabaseIterator();
			tempReader.db = envDb;
			tempReader.pParameterId = 4; // temperature
			tempReader.tStart = fromTS;
			tempReader.tEnd = toTS;

			var etpReader = new GeoframeEnvDatabaseIterator();
			etpReader.db = envDb;
			etpReader.pParameterId = 1; // etp
			etpReader.tStart = fromTS;
			etpReader.tEnd = toTS;

			var timeStepMinutes = 60; // time step in minutes

			HashMap<Integer, Double> initialConditionSolidWater = new HashMap<>();
			HashMap<Integer, Double> initialConditionLiquidWater = new HashMap<>();
			HashMap<Integer, Double> initalConditionsCanopyMap = new HashMap<>();
			HashMap<Integer, Double> initalConditionsRootzoneMap = new HashMap<>();
			HashMap<Integer, Double> initalConditionsRunoffMap = new HashMap<>();

			while (precipReader.next() && tempReader.next() && etpReader.next()) {
				HashMap<Integer, Double> precipMap = precipReader.outData;
				HashMap<Integer, Double> tempMap = tempReader.outData;
				HashMap<Integer, Double> etpMap = etpReader.outData;

				String pTs = precipReader.tCurrent;
				String tTs = tempReader.tCurrent;
				// check that time steps are the same
				if (!pTs.equals(tTs)) {
					throw new Exception("Time steps do not match: " + pTs + " <> " + tTs);
				}

				pm.message("Processing time step: " + pTs);

				for (Integer basinId : precipMap.keySet()) {
					// FOR EACH BASIN
					double precipitation = precipMap.get(basinId);
					double temperature = tempMap.get(basinId);
					double etp = etpMap.get(basinId);
					double basinAreaKm2 = basinAreas.get(basinId);

					// RAIN SNOW SEPARATION
					double m1 = 1.0;
					var alfa_r = 1.0; // Adjustment coefficient for the rainfall measurements errors [-]
					var alfa_s = 1.0; // Adjustment coefficient for the snow measurements errors [-]
					var meltingTemperature = 0.0; // Melting temperature (equal for snow model) [°C]

					double[] rainSnow = RainSnowSeparationPointCase.calculateRSSeparation(//
							precipitation, //
							temperature, //
							meltingTemperature, //
							alfa_r, //
							alfa_s, //
							m1 //
					);

					// SNOW MELTING
					var combinedMeltingFactor = 1.5; // Melting factor [mm/°C/day]
					var freezingFactor = 0.8; // Freezing factor [mm/°C/day]
					var alfa_l = 0.2; // Coefficient for the computation of the maximum liquid water [-]

					freezingFactor = freezingFactor / 1440 * timeStepMinutes;
					combinedMeltingFactor = combinedMeltingFactor / 1440 * timeStepMinutes;

					double initialSolidWater = 0.0;
					double initialLiquidWater = 0.0;
					if (initialConditionSolidWater.containsKey(basinId)) {
						initialSolidWater = initialConditionSolidWater.get(basinId);
					}
					if (initialConditionLiquidWater.containsKey(basinId)) {
						initialLiquidWater = initialConditionLiquidWater.get(basinId);
					}

					SnowStepResult resultSM = SnowMeltingPointCaseDegreeDay.computeSnowStep( //
							temperature, //
							rainSnow[0], //
							rainSnow[1], //
							meltingTemperature, //
							combinedMeltingFactor, //
							freezingFactor, //
							alfa_l, //
							initialSolidWater, //
							initialLiquidWater //
					);

					initialConditionSolidWater.put(basinId, resultSM.solidWater());
					initialConditionLiquidWater.put(basinId, resultSM.liquidWater());

					// CANOPY
					var kc = 0.6;
					var p = 0.4;
					var lai = 0.6; // TODO handle LAI properly

					double ci = kc * lai / 2.0;
					if (initalConditionsCanopyMap.containsKey(basinId)) {
						ci = initalConditionsCanopyMap.get(basinId);
					}
					double meltingDischarge = resultSM.meltingDischarge();
					if (isNovalue(meltingDischarge))
						meltingDischarge = 0.0;
					if (isNovalue(etp) || etp < 0)
						etp = 0.0;
					WaterBudgetCanopyStepResult resultWBC = WaterBudgetCanopyOUT.calculateWaterBudgetCanopy(//
							meltingDischarge, //
							lai, //
							etp, //
							ci, //
							kc, //
							p //
					);
					initalConditionsCanopyMap.put(basinId, resultWBC.waterStorage());
					
					// ROOTZONE
					var s_RootZoneMax = 150.0; // Maximum value of the rootzone water storage [mm]
					var g = 0.05; // Maximum percolation rate [-]
					var h = 1.5; // Exponential of non-linear reservoir model [-]
					var pB_soil = 2.0; // Degree of spatial variability of the soil moisture capacity [-]
					var sat_degree = 0.5;
					
					ci = s_RootZoneMax * sat_degree;
					if (initalConditionsRootzoneMap.containsKey(basinId)) {
						ci = initalConditionsRootzoneMap.get(basinId);
					}
					var throughFall = resultWBC.throughfall();
					if(isNovalue(throughFall)) {
						throughFall = 0.0;
					}
					var aet = resultWBC.AET();
					if(isNovalue(aet)) {
						aet = 0.0;
					}
					WaterBudgetRootZoneStepResult resultRZ = WaterBudgetRootZone.calculateWaterBudgetRootZone(//
							throughFall, //
							etp, //
							aet, //
							ci, //
							pB_soil, //
							s_RootZoneMax, //
							g, //
							h, //
							basinAreaKm2, //
							timeStepMinutes//
					);
					initalConditionsRootzoneMap.put(basinId, resultRZ.waterStorage());
					
					// RUNOFF
					var s_RunoffMax = 60.0; // Maximum runoff storage [mm]
					var c = 0.4; // Coefficient of the non-linear reservoir model [-]
					var d = 2.0; // Exponent of the non-linear reservoir model [-]
					
					ci = 0.5 * s_RunoffMax;
					if (initalConditionsRunoffMap.containsKey(basinId)) {
						ci = initalConditionsRunoffMap.get(basinId);
					}
					
					double quick_mm = resultRZ.quick_mm();
					if (isNovalue(quick_mm)) {
						quick_mm = 0.0;
					}
					WaterBudgetStepResult resultWB = WaterBudget.calculateWaterBudget(quick_mm, ci, c, d, s_RunoffMax, basinAreaKm2, timeStepMinutes);
					initalConditionsRunoffMap.put(basinId, resultWB.waterStorage());
					
					
				}
			}

		} finally {
			db.close();
			envDb.close();
		}
	}

	private boolean toDo(String filepath) {
		return new File(filepath).exists() == false;
	}

	public static void main(String[] args) throws Exception {
		new Test();
	}

}
