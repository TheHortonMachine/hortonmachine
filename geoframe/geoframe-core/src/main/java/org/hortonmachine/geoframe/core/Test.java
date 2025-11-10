package org.hortonmachine.geoframe.core;

import java.io.File;

import org.hortonmachine.HM;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;
import org.hortonmachine.hmachine.modules.geomorphology.flow.OmsFlowDirections;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;

public class Test extends HMModel {

	public Test() throws Exception {
		String folder = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/flanginec/";
		String dtm = folder + "inputs/dtm.asc";
		String pit = folder + "outputs/pit.asc";
		String flow = folder + "outputs/flow.asc";
		String drain = folder + "outputs/drain.asc";
		String tca = folder + "outputs/tca.asc";
		String net = folder + "outputs/net.asc";
		String netnum = folder + "outputs/netnum.asc";
		String netbasins = folder + "outputs/netnumbasins.asc";
		int thres = 100;
		double desiredArea = 5000.0;
		
		// create output folder if not existing
		File outFolder = new File(folder + "outputs/");
		if (!outFolder.exists()) {
			outFolder.mkdirs();
		}

		OmsPitfiller pitfiller = new OmsPitfiller();
		pitfiller.inElev = getRaster(dtm);
		pitfiller.process();
		dumpRaster(pitfiller.outPit, pit);

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
		extractnetwork.pThres = thres;
		extractnetwork.process();
		dumpRaster(extractnetwork.outNet, net);
		HM.makeQgisStyleForRaster(EColorTables.net.name(), net, 0);
		
//		OmsNetNumbering nn = new OmsNetNumbering();
//		nn.inFlow = getRaster(drain);
//		nn.inTca = getRaster(tca);
//		nn.inNet = getRaster(net);
//		nn.pDesiredArea = desiredArea;
//		nn.pDesiredAreaDelta = 10.0;
//		nn.process();
//		dumpRaster(nn.outNetnum, netnum);
//		dumpRaster(nn.outBasins, netbasins);
//		HM.makeQgisStyleForRaster(EColorTables.contrasting.name(), netnum, 0);
//		HM.makeQgisStyleForRaster(EColorTables.contrasting.name(), netbasins, 0);
		
		
		

	}

	public static void main(String[] args) throws Exception {
		new Test();
	}

}
