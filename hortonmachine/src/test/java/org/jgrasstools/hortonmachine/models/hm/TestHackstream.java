package org.jgrasstools.hortonmachine.models.hm;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.hackstream.HackStream;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test Hacklength.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestHackstream extends HMTestCase {

    @SuppressWarnings("nls")
    public void testHacklength() throws IOException {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.mflowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);
        double[][] hacklengthData = HMTestMaps.hacklengthData;
        GridCoverage2D hacklengthCoverage = CoverageUtilities.buildCoverage("hacklength", hacklengthData, envelopeParams, crs,
                true);
        double[][] netData = HMTestMaps.extractNet1Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        HackStream hackStream = new HackStream();
        hackStream.pm = pm;
        hackStream.inFlow = flowCoverage;
        hackStream.inTca = tcaCoverage;
        hackStream.inNet = netCoverage;
        hackStream.inHacklength = hacklengthCoverage;

        hackStream.process();

        GridCoverage2D hackStreamCoverage = hackStream.outHackstream;
        checkMatrixEqual(hackStreamCoverage.getRenderedImage(), HMTestMaps.hackstream, 0.01);
    }

}
