package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.modules.geomorphology.ab.Ab;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;
/**
 * Test ab.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAb extends HMTestCase {

    public void testAb() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData,
                envelopeParams, crs);
        double[][] planData = HMTestMaps.planData;
        GridCoverage2D planCoverage = CoverageUtilities.buildCoverage("plan", planData,
                envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Ab ab = new Ab();
        ab.inTca = tcaCoverage;
        ab.inPlan = planCoverage;
        ab.pm = pm;

        ab.process();

        GridCoverage2D alungCoverage = ab.outAb;
        GridCoverage2D bCoverage = ab.outB;

        checkMatrixEqual(alungCoverage.getRenderedImage(), HMTestMaps.abData, 0.01);
        checkMatrixEqual(bCoverage.getRenderedImage(), HMTestMaps.bData, 0.01);
    }

}
