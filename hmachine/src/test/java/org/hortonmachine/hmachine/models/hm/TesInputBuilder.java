package org.hortonmachine.hmachine.models.hm;

import java.io.File;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.geoframe.io.database.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsGeoframeInputsBuilder;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;
import org.hortonmachine.hmachine.utils.HMTestCase;

public class TesInputBuilder extends HMModel {

	public TesInputBuilder() throws Exception {

		// gura
//				String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/gura/";
//				String ext = ".tif";
//				int thres = 500;
//				double desiredArea = 500.0;
//				double easting = 265340.845;
//				double northing = 9934464.184;

		// flanginec
//				String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/flanginec/";
//				String ext = ".tif";
//				int thres = 100;
//				double desiredArea = 100.0;
//				double easting = 1637993.497;
//				double northing = 5111925.950;

		// NOCE SMALL
//				String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce_mini/";
//				String ext = ".tif";
//				int drainThres = 5000;
//				double desiredArea = 10000.0;
//				double desiredAreaDelta = 200.0;
//				double easting = 623519.2969;
//				double northing = 5128704.4571;

		// NOCE

		double desiredArea = 1_000_000.0;
		double desiredAreaDelta = 20.0;
		String folder = "/home/andreisd/Documents/project/data_hm/vermiglio_dtm/";
		String ext = ".tif";
		String basinnetbasins = folder + "outputs/basin_netnumbasins" + ext;
		String basinpit = folder + "outputs/basin_pit" + ext;
		String basindrain = folder + "outputs/basin_drain" + ext;
		String basintca = folder + "outputs/basin_tca" + ext;
		String basinnet = folder + "outputs/basin_net" + ext;
		String basinnetnum = folder + "outputs/basin_netnum" + ext;
		String basinnetbasinsdesired = folder + "outputs/basin_netnumbasins_desired" + ext;
		String topology = folder + "outputs/topology.txt";
		String streamGaugePath = folder + "inputs/idrometri.shp";
		String basinskyview = folder + "outputs/basin_skyview" + ext;

		String geoframeGpkg = folder + "outputs/gf.gpkg";

		String geoframeFolder;

		File outFolder = new File(folder + "outputs/");
		if (!outFolder.exists()) {
			outFolder.mkdirs();
		}
//				if (!toDo(geoframeGpkg)) {
//					new File(geoframeGpkg).delete();
//				}
		ASpatialDb db = null;
		try {
			db = EDb.GEOPACKAGE.getSpatialDb();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			db.open(geoframeGpkg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {

			try {

				if (!db.hasTable(SqlName.m(GeoFrameSimpleTable.TOPOLOGY.tableName()))) {
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

				}

				if (!db.hasTable(SqlName.m(GeoFrameGeoTable.BASIN.tableName()))
						|| !db.hasTable(SqlName.m(GeoFrameGeoTable.NET.tableName()))) {
					OmsGeoframeInputsBuilder builder = new OmsGeoframeInputsBuilder();
					builder.inPitfiller = basinpit;
					builder.inDrain = basindrain;
					builder.inTca = basintca;
					builder.inNet = basinnet;
					builder.inSkyview = basinskyview;
					builder.inBasins = basinnetbasinsdesired;
					builder.inGeoframeTopology = topology;
					// builder.outFolder = geoframeFolder;
					builder.inStreamGauge = streamGaugePath;
					builder.inIDStreamGaugeFieldName = "idstazione";
					builder.inGeoframeDb = db;
					builder.process();
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} finally {

		}
	}

	public static void main(String[] args) throws Exception {
		new TesInputBuilder();
	}

}
