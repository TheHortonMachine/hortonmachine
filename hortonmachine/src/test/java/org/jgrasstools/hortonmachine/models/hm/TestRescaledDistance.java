package org.jgrasstools.hortonmachine.models.hm;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.basin.rescaleddistance.RescaledDistance;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test RescaledDistance.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRescaledDistance extends HMTestCase {

    public void testRescaledDistance() throws IOException {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs);
        double[][] netData = HMTestMaps.extractNet0Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        RescaledDistance rescaledDistance = new RescaledDistance();
        rescaledDistance.inFlow = flowCoverage;
        rescaledDistance.inNet = netCoverage;
        rescaledDistance.pRatio = 0.3;
        rescaledDistance.pm = pm;

        rescaledDistance.process();

        GridCoverage2D rescaledDistanceCoverage = rescaledDistance.outRescaled;
        checkMatrixEqual(rescaledDistanceCoverage.getRenderedImage(), HMTestMaps.rescaledDistanceData, 0.1);
    }

}
