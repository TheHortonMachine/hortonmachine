package org.hortonmachine.hmachine.geoframe;

import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
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
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameGeoTable;
import org.hortonmachine.hmachine.geoframe.io.database.tables.GeoFrameSimpleTable;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;
import org.hortonmachine.hmachine.modules.geomorphology.flow.OmsFlowDirections;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.skyview.OmsSkyview;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsGeoframeInputsBuilder;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description("Prepares raster and topological data for the ERM/GeoFrame water budget model pipeline.")
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("ERM, GeoFrame, DEM, basin, network, data preparation")
@Label("GeoFrame")
@Name("ermDataPreparator")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmDataPreparator extends HMModel {

	@Description("Input dtm.")
	@UI(HMConstants.FILEIN_UI_HINT_RASTER)
	@In
	public String inDtm;

	@Description("GeoPackage output name.")
	@In
	public String outGeopackageName = "geoframe_data.gpkg";

	@Description("Execute also basin cutout (needs the outlet coordinate to be defined.")
	@In
	public boolean doBasinCutout = false;

	@Description("Drainage area threshold for network extraction.")
	@Unit("cells")
	@In
	public int pDrainThreshold = 2000;

	@Description("Target sub-basin area.")
	@Unit("m²")
	@In
	public double pDesiredArea = 1_000_000.0;

	@Description("Acceptable deviation from the target area.")
	@Unit("%")
	@In
	public double pDesiredAreaDelta = 20.0;

	@Description("Outlet point easting coordinate.")
	@In
	public double pOutletEasting;

	@Description("Outlet point northing coordinate.")
	@In
	public double pOutletNorthing;

	@Description("If true, existing output files are overwritten.")
	@In
	public boolean doOverwrite = false;
	@Description("Stream Gauge vector layer path.")
	@In
	public String inStreamGauge;
	@Description("Vector layer field for the station ID.")
	@In
	public String pStreamGaugeIDField;

	@Execute
	public void process() throws Exception {
		if (doBasinCutout && (pOutletEasting == 0 || pOutletNorthing == 0)) {
			throw new IllegalArgumentException("Outlet coordinates must be defined for basin cutout.");
		}

		Paths p = new Paths(inDtm, doOverwrite);
		new File(p.outputsDir).mkdirs();

		String gpkgPath = p.getOutputsDir() + outGeopackageName;

		ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb();
		db.open(gpkgPath);

		try {
			if (p.shouldRun(p.pit)) {
				pm.message("Running PitFiller...");
				OmsPitfiller op = new OmsPitfiller();
				op.pm = pm;
				op.inElev = OmsRasterReader.readRaster(p.dtm);
				op.process();
				OmsRasterWriter.writeRaster(p.pit, op.outPit);
				makeQgisStyleForRaster(EColorTables.elev.name(), p.pit, 0);
			} else {
				pm.message("Not overwriting existing pit: " + p.pit);
			}

			if (p.shouldRun(p.flow)) {
				pm.message("Running FlowDirections...");
				OmsFlowDirections fd = new OmsFlowDirections();
				fd.pm = pm;
				fd.inPit = OmsRasterReader.readRaster(p.pit);
				fd.pMinElev = 0;
				fd.process();
				OmsRasterWriter.writeRaster(p.flow, fd.outFlow);
				makeQgisStyleForRaster(EColorTables.flow.name(), p.flow, 0);
			} else {
				pm.message("Not overwriting existing flow: " + p.flow);
			}

			if (p.shouldRun(p.drain)) {
				pm.message("Running DrainDir...");
				OmsDrainDir dd = new OmsDrainDir();
				dd.pm = pm;
				dd.inPit = OmsRasterReader.readRaster(p.pit);
				dd.inFlow = OmsRasterReader.readRaster(p.flow);
				dd.pLambda = 1.0;
				dd.doLad = true;
				dd.process();
				OmsRasterWriter.writeRaster(p.drain, dd.outFlow);
				OmsRasterWriter.writeRaster(p.tca, dd.outTca);
				makeQgisStyleForRaster(EColorTables.flow.name(), p.drain, 0);
				makeQgisStyleForRaster(EColorTables.logarithmic.name(), p.tca, 0);
			} else {
				pm.message("Not overwriting existing drain: " + p.drain);
				pm.message("Not overwriting existing tca: " + p.tca);
			}

			if (p.shouldRun(p.net)) {
				pm.message("Running ExtractNetwork...");
				OmsExtractNetwork en = new OmsExtractNetwork();
				en.pm = pm;
				en.inTca = OmsRasterReader.readRaster(p.tca);
				en.inFlow = OmsRasterReader.readRaster(p.flow);
				en.pThres = pDrainThreshold;
				en.process();
				OmsRasterWriter.writeRaster(p.net, en.outNet);
				makeQgisStyleForRaster(EColorTables.net.name(), p.net, 0);
			} else {
				pm.message("Not overwriting existing network: " + p.net);
			}

			if (p.shouldRun(p.skyview)) {
				pm.message("Running Skyview...");
				OmsSkyview sv = new OmsSkyview();
				sv.pm = pm;
				sv.inElev = OmsRasterReader.readRaster(p.pit);
				sv.process();
				OmsRasterWriter.writeRaster(p.skyview, sv.outSky);
				makeQgisStyleForRaster(EColorTables.slope.name(), p.skyview, 0);
			} else {
				pm.message("Not overwriting existing skyview: " + p.skyview);
			}

			if (doBasinCutout) {
				if (p.shouldRun(p.basin)) {
					pm.message("Running ExtractBasin...");
					OmsExtractBasin eb = new OmsExtractBasin();
					eb.pm = pm;
					eb.inFlow = OmsRasterReader.readRaster(p.drain);
					eb.pEast = pOutletEasting;
					eb.pNorth = pOutletNorthing;
					eb.process();
					OmsRasterWriter.writeRaster(p.basin, eb.outBasin);
					makeQgisStyleForRaster(EColorTables.net.name(), p.basin, 0);
				} else {
					pm.message("Not overwriting existing basin cutout: " + p.basin);
				}

				if (p.shouldRun(p.basinResized)) {
					pm.message("Resizing basin...");
					OmsRasterResizer rr = new OmsRasterResizer();
					rr.pm = pm;
					GridCoverage2D basinGC = OmsRasterReader.readRaster(p.basin);
					rr.inRaster = basinGC;
					RegionMap rm = HMRaster.fromGridCoverage(basinGC).getDataRegionMap();
					Envelope env = rm.toEnvelope();
					Polygon poly = GeometryUtilities.createPolygonFromEnvelope(env);
					SimpleFeatureCollection fc = FeatureUtilities
							.featureCollectionFromGeometry(basinGC.getCoordinateReferenceSystem(), poly);
					rr.inVector = fc;
					rr.process();
					OmsRasterWriter.writeRaster(p.basinResized, rr.outRaster);
				} else {
					pm.message("Not overwriting existing resized basin: " + p.basinResized);
				}

				if (p.shouldRun(p.basinPit)) {
					pm.message("Cutting basin pit...");
					OmsCutOut co = new OmsCutOut();
					co.pm = pm;
					co.inRaster = OmsRasterReader.readRaster(p.pit);
					co.inMask = OmsRasterReader.readRaster(p.basinResized);
					co.process();
					OmsRasterWriter.writeRaster(p.basinPit, co.outRaster);
					makeQgisStyleForRaster(EColorTables.elev.name(), p.basinPit, 0);
				} else {
					pm.message("Not overwriting existing basin pit: " + p.basinPit);
				}

				if (p.shouldRun(p.basinDrain)) {
					pm.message("Cutting basin drain...");
					OmsCutOut co = new OmsCutOut();
					co.pm = pm;
					co.inRaster = OmsRasterReader.readRaster(p.drain);
					co.inMask = OmsRasterReader.readRaster(p.basinResized);
					co.process();
					OmsRasterWriter.writeRaster(p.basinDrain, co.outRaster);
					makeQgisStyleForRaster(EColorTables.flow.name(), p.basinDrain, 0);
				} else {
					pm.message("Not overwriting existing basin drain: " + p.basinDrain);
				}

				if (p.shouldRun(p.basinTca)) {
					pm.message("Cutting basin TCA...");
					OmsCutOut co = new OmsCutOut();
					co.pm = pm;
					co.inRaster = OmsRasterReader.readRaster(p.tca);
					co.inMask = OmsRasterReader.readRaster(p.basinResized);
					co.process();
					OmsRasterWriter.writeRaster(p.basinTca, co.outRaster);
					makeQgisStyleForRaster(EColorTables.logarithmic.name(), p.basinTca, 0);
				} else {
					pm.message("Not overwriting existing basin TCA: " + p.basinTca);
				}

				if (p.shouldRun(p.basinNet)) {
					pm.message("Cutting basin network...");
					OmsCutOut co = new OmsCutOut();
					co.pm = pm;
					co.inRaster = OmsRasterReader.readRaster(p.net);
					co.inMask = OmsRasterReader.readRaster(p.basinResized);
					co.process();
					OmsRasterWriter.writeRaster(p.basinNet, co.outRaster);
					makeQgisStyleForRaster(EColorTables.net.name(), p.basinNet, 0);
				} else {
					pm.message("Not overwriting existing basin network: " + p.basinNet);
				}

				if (p.shouldRun(p.basinSkyview)) {
					pm.message("Cutting basin skyview...");
					OmsCutOut co = new OmsCutOut();
					co.pm = pm;
					co.inRaster = OmsRasterReader.readRaster(p.skyview);
					co.inMask = OmsRasterReader.readRaster(p.basinResized);
					co.process();
					OmsRasterWriter.writeRaster(p.basinSkyview, co.outRaster);
					makeQgisStyleForRaster(EColorTables.slope.name(), p.basinSkyview, 0);
				} else {
					pm.message("Not overwriting existing basin skyview: " + p.basinSkyview);
				}

				if (p.shouldRun(p.basinNetnum) || !db.hasTable(SqlName.m(GeoFrameSimpleTable.TOPOLOGY.tableName()))) {
					pm.message("Running NetNumbering...");
					OmsNetNumbering nn = new OmsNetNumbering();
					nn.pm = pm;
					nn.inFlow = OmsRasterReader.readRaster(p.basinDrain);
					nn.inNet = OmsRasterReader.readRaster(p.basinNet);
					nn.inTca = OmsRasterReader.readRaster(p.basinTca);
					nn.pDesiredArea = pDesiredArea;
					nn.pDesiredAreaDelta = pDesiredAreaDelta;
					nn.inGeoframeDb = db;
					nn.process();
					OmsRasterWriter.writeRaster(p.basinNetnum, nn.outNetnum);
					OmsRasterWriter.writeRaster(p.basinNetbasins, nn.outBasins);
					OmsRasterWriter.writeRaster(p.basinNetbasinsDesired, nn.outDesiredBasins);
					makeQgisStyleForRaster(EColorTables.contrasting.name(), p.basinNetnum, 0);
					makeQgisStyleForRaster(EColorTables.contrasting.name(), p.basinNetbasins, 0);
					makeQgisStyleForRaster(EColorTables.contrasting.name(), p.basinNetbasinsDesired, 0);
				}

				if (!db.hasTable(SqlName.m(GeoFrameGeoTable.BASIN.tableName()))
						|| !db.hasTable(GeoFrameGeoTable.NET.tableName())) {
					pm.message("Building GeoframeInputs...");
					OmsGeoframeInputsBuilder b = new OmsGeoframeInputsBuilder();
					b.pm = pm;
					b.inPitfiller = p.basinPit;
					b.inDrain = p.basinDrain;
					b.inTca = p.basinTca;
					b.inNet = p.basinNet;
					b.inSkyview = p.basinSkyview;
					b.inBasins = p.basinNetbasinsDesired;
					b.inStreamGauge = inStreamGauge;
					b.inIDStreamGaugeFieldName = pStreamGaugeIDField;
					b.inGeoframeDb = db;

					b.process();
				}
			}

			pm.message("Data preparation complete.");
		} finally {
			db.close();
		}
	}

	public void makeQgisStyleForRaster(String colorTable, String rasterPath, int labelDecimals) throws Exception {
		OmsRasterSummary s = new OmsRasterSummary();
		s.pm = new DummyProgressMonitor();
		s.inRaster = OmsRasterReader.readRaster(rasterPath);
		s.process();
		String qml = RasterStyleUtilities.createQGISRasterStyle(colorTable, s.outMin, s.outMax, null, labelDecimals);
		FileUtilities.writeFile(qml, FileUtilities.substituteExtention(new File(rasterPath), "qml"));
	}

	private static final class Paths {
		final String outputsDir;

		final String dtm;
		final String pit;
		final String flow;
		final String drain;
		final String tca;
		final String net;
		final String basin;
		final String basinResized;
		final String basinPit;
		final String basinDrain;
		final String basinTca;
		final String basinNet;
		final String skyview;
		final String basinSkyview;
		final String basinNetnum;
		final String basinNetbasins;
		final String basinNetbasinsDesired;

		final boolean overwrite;
		final String ext = ".tif";

		Paths(String inDtm, boolean overwrite) {
			this.overwrite = overwrite;
			dtm = inDtm;
			if (!new File(dtm).exists()) {
				throw new IllegalArgumentException("Input DTM not found at: " + dtm);
			}
			File folder = new File(dtm).getParentFile();
			outputsDir = new File(folder, "outputs").getAbsolutePath() + File.separator;
			if (!new File(outputsDir).exists()) {
				new File(outputsDir).mkdirs();
			}
			pit = outputsDir + "pit" + ext;
			flow = outputsDir + "flow" + ext;
			drain = outputsDir + "drain" + ext;
			tca = outputsDir + "tca" + ext;
			net = outputsDir + "net" + ext;
			skyview = outputsDir + "skyview" + ext;
			basin = outputsDir + "basin" + ext;
			basinResized = outputsDir + "basin_resized" + ext;
			basinPit = outputsDir + "basin_pit" + ext;
			basinDrain = outputsDir + "basin_drain" + ext;
			basinTca = outputsDir + "basin_tca" + ext;
			basinNet = outputsDir + "basin_net" + ext;
			basinSkyview = outputsDir + "basin_skyview" + ext;
			basinNetnum = outputsDir + "basin_netnum" + ext;
			basinNetbasins = outputsDir + "basin_netnumbasins" + ext;
			basinNetbasinsDesired = outputsDir + "basin_netnumbasins_desired" + ext;

		}

		public String getOutputsDir() {
			return outputsDir;
		}

		boolean shouldRun(String path) {
			return overwrite || !new File(path).exists();
		}
	}

	public static void main(String[] args) throws Exception {
		String workspacePath = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/workspace/";
		ErmDataPreparator prep = new ErmDataPreparator();
		prep.inDtm = workspacePath + "dtm.tif";
		prep.outGeopackageName = "geoframe_data.gpkg";
		prep.doBasinCutout = true;
		prep.pDrainThreshold = 2000;
		prep.pDesiredArea = 1_000_000.0;
		prep.pDesiredAreaDelta = 20.0;
		prep.pOutletEasting = 629720;
		prep.pOutletNorthing = 5127690;
		prep.pStreamGaugeIDField = "idstazione";
		prep.inStreamGauge = workspacePath + "idrometri.shp";
		prep.process();
	}
}
