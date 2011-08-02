package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.netdif.NetDif;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestNetDif extends HMTestCase {

    public void testNetDif() {
        double[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        double[][] strahlerData = HMTestMaps.strahlerData;
        GridCoverage2D strahlerCoverage = CoverageUtilities.buildCoverage("net", strahlerData, envelopeParams, crs, true);

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pit", pitfillerData, envelopeParams, crs, true);

        NetDif netDif = new NetDif();
        netDif.inFlow = flowCoverage;
        netDif.inStream = strahlerCoverage;
        netDif.inRaster = pitfillerCoverage;
        netDif.process();

        GridCoverage2D netDifCoverage = netDif.outDiff;
        checkMatrixEqual(netDifCoverage.getRenderedImage(), HMTestMaps.diff_forPit);

    }

}
