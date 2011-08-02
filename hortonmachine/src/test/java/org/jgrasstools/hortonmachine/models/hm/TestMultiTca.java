package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.multitca.MultiTca;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestMultiTca extends HMTestCase {
    public void testMultiTca() throws Exception {
        double[][] pitfillerData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pit", pitfillerData, envelopeParams, crs, true);


        double[][] flowData = HMTestMaps.drainData1;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        envelopeParams = HMTestMaps.envelopeParams;
        crs = HMTestMaps.crs;
        double[][] cp9Data = HMTestMaps.cp9Data;
        GridCoverage2D cp3Coverage = CoverageUtilities.buildCoverage("cp9", cp9Data, envelopeParams, crs, true);

        MultiTca tca = new MultiTca();
        tca.inPit = pitfillerCoverage;
        tca.inFlow = flowCoverage;
        tca.inCp9 = cp3Coverage;
        tca.pm = pm;
        tca.process();
        GridCoverage2D tcaCoverage = tca.outMultiTca;
 //       checkMatrixEqual(tcaCoverage.getRenderedImage(), HMTestMaps.multiTcaData);
    }
}
