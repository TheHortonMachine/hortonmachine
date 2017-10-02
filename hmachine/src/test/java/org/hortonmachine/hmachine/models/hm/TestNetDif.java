package org.hortonmachine.hmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.network.netdiff.OmsNetDiff;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestNetDif extends HMTestCase {

    public void testNetDif() {
        double[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        double[][] strahlerData = HMTestMaps.strahlerData;
        GridCoverage2D strahlerCoverage = CoverageUtilities.buildCoverage("net", strahlerData, envelopeParams, crs, true);

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pit", pitfillerData, envelopeParams, crs, true);

        OmsNetDiff netDif = new OmsNetDiff();
        netDif.inFlow = flowCoverage;
        netDif.inStream = strahlerCoverage;
        netDif.inRaster = pitfillerCoverage;
        netDif.process();

        GridCoverage2D netDifCoverage = netDif.outDiff;
        checkMatrixEqual(netDifCoverage.getRenderedImage(), HMTestMaps.diff_forPit);

    }

}
