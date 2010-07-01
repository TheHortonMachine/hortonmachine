package org.jgrasstools.hortonmachine.models.hm;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.hacklength.HackLength;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test Hacklength.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestHacklength extends HMTestCase {

    public void testHacklength() throws IOException {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.mflowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        HackLength hackLength = new HackLength();
        hackLength.inFlow = flowCoverage;
        hackLength.inTca = tcaCoverage;
        hackLength.pm = pm;

        hackLength.process();

        GridCoverage2D hackLengthCoverage = hackLength.outHacklength;
        checkMatrixEqual(hackLengthCoverage.getRenderedImage(), HMTestMaps.hacklengthData, 0.01);
    }

}
