package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet.Wateroutlet;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Wateroutlet} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestWateroutlet extends HMTestCase {
    public void testWateroutlet() throws Exception {

        double[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
                envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        Wateroutlet wateroutlet = new Wateroutlet();
        wateroutlet.pm = pm;
        wateroutlet.inFlow = flowCoverage;
        wateroutlet.pNorth = 5139885.0;
        wateroutlet.pEast = 1640724.0;

        wateroutlet.process();

        GridCoverage2D basinCoverage = wateroutlet.outBasin;

        checkMatrixEqual(basinCoverage.getRenderedImage(), HMTestMaps.basinWateroutletData, 0);
    }

}
