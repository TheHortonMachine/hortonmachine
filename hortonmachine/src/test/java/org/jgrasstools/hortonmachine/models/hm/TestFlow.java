package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.FlowDirections;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link FlowDirections} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestFlow extends HMTestCase {

    public void testFlow() throws Exception {
        double[][] pitfillerData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pitfiller", pitfillerData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        FlowDirections flowDirections = new FlowDirections();
        flowDirections.inPit = pitfillerCoverage;
        flowDirections.pm = pm;

        flowDirections.process();

        GridCoverage2D flowCoverage = flowDirections.outFlow;

        checkMatrixEqual(flowCoverage.getRenderedImage(), HMTestMaps.flowData, 0);
    }

}
