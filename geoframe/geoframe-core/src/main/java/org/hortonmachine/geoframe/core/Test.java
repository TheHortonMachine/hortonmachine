package org.hortonmachine.geoframe.core;

import java.io.File;

import org.hortonmachine.HM;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;
import org.hortonmachine.hmachine.modules.geomorphology.flow.OmsFlowDirections;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;

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
		
		// NOCE
		String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/";
		String ext = ".tif";
		int drainThres = 5000;
		double desiredArea = 10000.0;
		double desiredAreaDelta = 200.0;
		double easting = 623519.2969;
		double northing = 5128704.4571;
		
		String dtm = folder + "inputs/dtm" + ext;
		String pit = folder + "outputs/pit" + ext;
		String flow = folder + "outputs/flow" + ext;
		String drain = folder + "outputs/drain" + ext;
		String tca = folder + "outputs/tca" + ext;
		String net = folder + "outputs/net" + ext;
		String basin = folder + "outputs/basin" + ext;
		
		String basinpit = folder + "outputs/basin_pit" + ext;
		String basindrain = folder + "outputs/basin_drain" + ext;
		String basintca = folder + "outputs/basin_tca" + ext;
		String basinnet = folder + "outputs/basin_net" + ext;
		
		String basinnetnum = folder + "outputs/basin_netnum" + ext;
		String basinnetbasins = folder + "outputs/basin_netnumbasins" + ext;
		String basinnetbasinsdesired = folder + "outputs/basin_netnumbasins_desired" + ext;

		File outFolder = new File(folder + "outputs/");
		if (!outFolder.exists()) {
			outFolder.mkdirs();
		}

		OmsPitfiller pitfiller = new OmsPitfiller();
		pitfiller.inElev = getRaster(dtm);
		pitfiller.process();
		dumpRaster(pitfiller.outPit, pit);
		HM.makeQgisStyleForRaster(EColorTables.elev.name(), pit, 0);

		OmsFlowDirections flowdirections = new OmsFlowDirections();
		flowdirections.inPit = getRaster(pit);
		flowdirections.pMinElev = 0;
		flowdirections.process();
		dumpRaster(flowdirections.outFlow, flow);
		HM.makeQgisStyleForRaster(EColorTables.flow.name(), flow, 0);

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

		OmsExtractNetwork extractnetwork = new OmsExtractNetwork();
		extractnetwork.inTca = getRaster(tca);
		extractnetwork.inFlow = getRaster(flow);
		extractnetwork.pThres = drainThres;
		extractnetwork.process();
		dumpRaster(extractnetwork.outNet, net);
		HM.makeQgisStyleForRaster(EColorTables.net.name(), net, 0);
		
		OmsExtractBasin extractbasin = new OmsExtractBasin();
		extractbasin.inFlow = getRaster(drain);
		extractbasin.pEast = easting;
		extractbasin.pNorth = northing;
		extractbasin.process();
		dumpRaster(extractbasin.outBasin, basin);
		HM.makeQgisStyleForRaster(EColorTables.net.name(), basin, 0);
		
		// cutout pit, drain and tca for the basin only
		OmsCutOut cutout = new OmsCutOut();
		cutout.inRaster = getRaster(pit);
		cutout.inMask = getRaster(basin);
		cutout.process();
		dumpRaster(cutout.outRaster, basinpit);
		HM.makeQgisStyleForRaster(EColorTables.elev.name(), basinpit, 0);
		
		cutout = new OmsCutOut();
		cutout.inRaster = getRaster(drain);
		cutout.inMask = getRaster(basin);
		cutout.process();
		dumpRaster(cutout.outRaster, basindrain);
		HM.makeQgisStyleForRaster(EColorTables.flow.name(), basindrain, 0);
		
		cutout = new OmsCutOut();
		cutout.inRaster = getRaster(tca);
		cutout.inMask = getRaster(basin);
		cutout.process();
		dumpRaster(cutout.outRaster, basintca);
		HM.makeQgisStyleForRaster(EColorTables.logarithmic.name(), basintca, 0);
		
		cutout = new OmsCutOut();
		cutout.inRaster = getRaster(net);
		cutout.inMask = getRaster(basin);
		cutout.process();
		dumpRaster(cutout.outRaster, basinnet);
		HM.makeQgisStyleForRaster(EColorTables.net.name(), basinnet, 0);
		
		
		OmsNetNumbering nn = new OmsNetNumbering();
		nn.inFlow = getRaster(basindrain);
		nn.inNet = getRaster(basinnet);
		nn.inTca = getRaster(basintca);
		nn.pDesiredArea = desiredArea;
		nn.pDesiredAreaDelta = desiredAreaDelta;
		nn.process();
		dumpRaster(nn.outNetnum, basinnetnum);
		dumpRaster(nn.outBasins, basinnetbasins);
		dumpRaster(nn.outDesiredBasins, basinnetbasinsdesired);
		HM.makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetnum, 0);
		HM.makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetbasins, 0);
		HM.makeQgisStyleForRaster(EColorTables.contrasting.name(), basinnetbasinsdesired, 0);
		
		
		

	}

	public static void main(String[] args) throws Exception {
		new Test();
	}

}
