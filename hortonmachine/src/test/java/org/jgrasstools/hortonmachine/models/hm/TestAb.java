package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.ab.Ab;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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
                envelopeParams, crs, true);
        double[][] planData = HMTestMaps.planData;
        GridCoverage2D planCoverage = CoverageUtilities.buildCoverage("plan", planData,
                envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

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
