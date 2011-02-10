package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.FlowDirections;
import org.jgrasstools.hortonmachine.modules.geomorphology.slope.Slope;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Tests the {@link Slope} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestSlope extends HMTestCase {

    public void testSlope() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("elevation", pitData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        // first create the needed map of flowdirections
        FlowDirections flow = new FlowDirections();
        flow.pm = pm;
        flow.inPit = pitfillerCoverage;
        flow.process();

        // then create the slope map
        Slope slope = new Slope();
        slope.inDem = pitfillerCoverage;
        slope.inFlow = flow.outFlow;
        slope.pm = pm;

        slope.process();

        GridCoverage2D slopeCoverage = slope.outSlope;
        checkMatrixEqual(slopeCoverage.getRenderedImage(), HMTestMaps.slopeData, 0.01);
    }

}
