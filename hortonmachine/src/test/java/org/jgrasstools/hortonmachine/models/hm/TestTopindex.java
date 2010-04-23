package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.basin.topindex.TopIndex;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test topindex
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTopindex extends HMTestCase {

    public void testAb() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);
        double[][] slopeData = HMTestMaps.slopeData;
        GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("plan", slopeData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        TopIndex topindex = new TopIndex();
        topindex.inTca = tcaCoverage;
        topindex.inSlope = slopeCoverage;
        topindex.pm = pm;

        topindex.process();

        GridCoverage2D topindexCoverage = topindex.outTopindex;

        checkMatrixEqual(topindexCoverage.getRenderedImage(), HMTestMaps.topIndexData, 0.01);
    }

}
