package org.hortonmachine.hmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.multitca.OmsMultiTca;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestMultiTca extends HMTestCase {
    public void testMultiTca() throws Exception {
        double[][] pitfillerData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pit", pitfillerData, envelopeParams, crs, true);


        double[][] flowData = HMTestMaps.drainData1;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        envelopeParams = HMTestMaps.getEnvelopeparams();
        crs = HMTestMaps.getCrs();
        double[][] cp9Data = HMTestMaps.cp9Data;
        GridCoverage2D cp3Coverage = CoverageUtilities.buildCoverage("cp9", cp9Data, envelopeParams, crs, true);

        OmsMultiTca tca = new OmsMultiTca();
        tca.inPit = pitfillerCoverage;
        tca.inFlow = flowCoverage;
        tca.inCp9 = cp3Coverage;
        tca.pm = pm;
        tca.process();
        GridCoverage2D tcaCoverage = tca.outMultiTca;
 //       checkMatrixEqual(tcaCoverage.getRenderedImage(), HMTestMaps.multiTcaData);
    }
}
