package org.jgrasstools.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.modules.r.summary.CoverageSummary;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestCoverageSummary extends HMTestCase {
    public void testCoverageSummary() throws Exception {

        double[][] inData = HMTestMaps.extractNet0Data;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams,
                crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        CoverageSummary summary = new CoverageSummary();
        summary.pm = pm;
        summary.inMap = inCoverage;
        summary.pBins = 100;
        summary.process();

        double min = summary.outMin;
        double max = summary.outMax;
        double mean = summary.outMean;
        double sdev = summary.outSdev;
        double range = summary.outRange;
        double sum = summary.outSum;
        double approxMedian = summary.outApproxmedian;

        assertEquals(2.0, min);
        assertEquals(2.0, max);
        assertEquals(2.0, mean);
        assertEquals(0.0, sdev);
        assertEquals(0.0, range);
        assertEquals(18.0, sum);
        assertEquals(2.0, approxMedian);

        double[][] cb = summary.outCb;
        // for( int i = 0; i < cb.length; i++ ) {
        // System.out.println(cb[i][0] + "\t" + cb[i][1] + "\t" + cb[i][2] + "%");
        // }

        assertEquals(cb[0][0], 2.0);
        assertEquals(cb[0][1], 9.0);
        assertEquals(cb[0][2], 11.25);
        assertTrue(JGTConstants.isNovalue(cb[cb.length - 1][0]));
        assertEquals(cb[cb.length - 1][1], 71.00);
        assertEquals(cb[cb.length - 1][2], 88.75);

        // System.out.println();

    }

}
