package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.modules.basin.topindex.TopIndex;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;
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
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs);
        double[][] slopeData = HMTestMaps.slopeData;
        GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("plan", slopeData, envelopeParams, crs);

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
