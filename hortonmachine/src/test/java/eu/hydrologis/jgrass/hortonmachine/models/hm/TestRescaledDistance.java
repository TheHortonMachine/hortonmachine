package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.modules.basin.rescaleddistance.RescaledDistance;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.hortonmachine.utils.coverage.CoverageUtilities;

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
