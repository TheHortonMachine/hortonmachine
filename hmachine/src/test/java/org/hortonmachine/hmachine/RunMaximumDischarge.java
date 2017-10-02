package org.hortonmachine.hmachine;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance;
import org.hortonmachine.hmachine.modules.basin.topindex.OmsTopIndex;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;
import org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb;
import org.hortonmachine.hmachine.modules.geomorphology.aspect.OmsAspect;
import org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures;
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;
import org.hortonmachine.hmachine.modules.geomorphology.gradient.OmsGradient;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;

public class RunMaximumDischarge {

    public static void main( String[] args ) throws Exception {

        String dtm = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/dtm.tiff";
        String aspect = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/aspect.tiff";
        String pit = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/pit.tiff";
        String flow = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/flow.tiff";
        String drain = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/drain.tiff";
        String tca = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/tca.tiff";
        String net = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/net300.tiff";
        String basin = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/basin.tiff";
        String basinShp = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/basin_vect.shp";
        String basinOutletShp = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/basin_outlet.shp";
        String basinPit = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/basin_cutout.tiff";
        String slope = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/slope.tiff";
        String topindex = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/topindex.tiff";
        String curvaturesPlan = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/curvatures_plan.tiff";
        String curvaturesProf = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/curvatures_prof.tiff";
        String curvaturesTan = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/curvatures_tan.tiff";
        String alung = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/alung.tiff";
        String alungB = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/alungB.tiff";
        String rescaled = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/flanginec/rescaled.tiff";

        // String dtm = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/dtm_toblino.tiff";
        // String pit = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/pit.tiff";
        // String flow = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/flow.tiff";
        // String drain = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/drain.tiff";
        // String tca = "/media/hydrologis/Samsung_T3/MAZONE/DTM/dtm_toblino/tca.tiff";

        // String dtm =
        // "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/dtm_all_float.tiff";
        // String pit = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/pit.tiff";
        // String flow = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/flow.tiff";
        // String drain = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/drain.tiff";
        // String tca = "/media/hydrologis/Samsung_T3/MAZONE/PITFILLE/DTM_calvello/tca.tiff";

//        OmsAspect aspectMod = new OmsAspect();
//        aspectMod.inElev = OmsRasterReader.readRaster(dtm);
//        // aspectMod.doRadiants = doRadiants;
//        aspectMod.doRound = true;
//        aspectMod.process();
//        OmsRasterWriter.writeRaster(aspect, aspectMod.outAspect);

        OmsDePitter pitfiller = new OmsDePitter();
        pitfiller.inElev = OmsRasterReader.readRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/dtm_test2.tiff");
        pitfiller.process();
        OmsRasterWriter.writeRaster(pit, pitfiller.outPit);
        OmsRasterWriter.writeRaster(flow, pitfiller.outFlow);
        // OmsVectorWriter.writeVector(pitPoints, pitfiller.outPitPoints);
        
        if (true) {
            System.exit(0);
        }

        OmsDrainDir draindir = new OmsDrainDir();
        draindir.inPit = OmsRasterReader.readRaster(pit);
        draindir.inFlow = OmsRasterReader.readRaster(flow);
        // draindir.inFlownet = OmsRasterReader.readRaster(inFlownet);
        draindir.pLambda = 1f;
        draindir.process();
        OmsRasterWriter.writeRaster(drain, draindir.outFlow);
        OmsRasterWriter.writeRaster(tca, draindir.outTca);

        OmsExtractNetwork extractnetwork = new OmsExtractNetwork();
        extractnetwork.inTca = OmsRasterReader.readRaster(tca);
        extractnetwork.inFlow = OmsRasterReader.readRaster(drain);
        // extractnetwork.inSlope = OmsRasterReader.readRaster(inSlope);
        // extractnetwork.inTc3 = OmsRasterReader.readRaster(inTc3);
        extractnetwork.pThres = 300;
        extractnetwork.pMode = Variables.TCA;
        // extractnetwork.pExp = pExp;
        // extractnetwork.pm = pm;
        extractnetwork.process();
        OmsRasterWriter.writeRaster(net, extractnetwork.outNet);

        OmsExtractBasin extractbasin = new OmsExtractBasin();
        extractbasin.pNorth = 5112705.635839384;
        extractbasin.pEast = 1638565.1058415675;
        extractbasin.inFlow = OmsRasterReader.readRaster(drain);
        // extractbasin.inNetwork = getVector(inNetwork);
        // extractbasin.pSnapbuffer = pSnapbuffer;
        extractbasin.doVector = true;
        // extractbasin.doSmoothing = doSmoothing;
        extractbasin.process();
        OmsRasterWriter.writeRaster(basin, extractbasin.outBasin);
        OmsVectorWriter.writeVector(basinOutletShp, extractbasin.outOutlet);
        OmsVectorWriter.writeVector(basinShp, extractbasin.outVectorBasin);
        System.out.println("Basin Area = " + extractbasin.outArea);

        OmsCutOut cutPit = new OmsCutOut();
        cutPit.inRaster = OmsRasterReader.readRaster(pit);
        cutPit.inMask = OmsRasterReader.readRaster(basin);
        // c.pMax = pMax;
        // c.pMin = pMin;
        // c.doInverse = doInverse;
        cutPit.process();
        OmsRasterWriter.writeRaster(basinPit, cutPit.outRaster);

        OmsGradient gradient = new OmsGradient();
        gradient.inElev = OmsRasterReader.readRaster(basinPit);
        gradient.pMode = Variables.FINITE_DIFFERENCES;
        gradient.doDegrees = true;
        gradient.process();
        OmsRasterWriter.writeRaster(slope, gradient.outSlope);

        OmsTopIndex topindexMod = new OmsTopIndex();
        topindexMod.inTca = OmsRasterReader.readRaster(tca);
        topindexMod.inSlope = OmsRasterReader.readRaster(slope);
        topindexMod.process();
        OmsRasterWriter.writeRaster(topindex, topindexMod.outTopindex);

        OmsCurvatures curv = new OmsCurvatures();
        curv.inElev = OmsRasterReader.readRaster(basinPit);
        curv.process();
        OmsRasterWriter.writeRaster(curvaturesProf, curv.outProf);
        OmsRasterWriter.writeRaster(curvaturesPlan, curv.outPlan);
        OmsRasterWriter.writeRaster(curvaturesTan, curv.outTang);

        OmsAb ab = new OmsAb();
        ab.inTca = OmsRasterReader.readRaster(tca);
        ab.inPlan = OmsRasterReader.readRaster(curvaturesPlan);
        ab.process();
        OmsRasterWriter.writeRaster(alung, ab.outAb);
        OmsRasterWriter.writeRaster(alungB, ab.outB);

        GridCoverage2D cutDrain = OmsCutOut.cut(OmsRasterReader.readRaster(drain), OmsRasterReader.readRaster(basin));
        GridCoverage2D cutNet = OmsCutOut.cut(OmsRasterReader.readRaster(net), OmsRasterReader.readRaster(basin));

        OmsRescaledDistance rescaleddistance = new OmsRescaledDistance();
        rescaleddistance.inFlow = cutDrain;
        rescaleddistance.inNet = cutNet;
        rescaleddistance.inElev = OmsRasterReader.readRaster(basinPit);
        rescaleddistance.pRatio = 0.3;
        rescaleddistance.process();
        OmsRasterWriter.writeRaster(rescaled, rescaleddistance.outRescaled);
    }

}
