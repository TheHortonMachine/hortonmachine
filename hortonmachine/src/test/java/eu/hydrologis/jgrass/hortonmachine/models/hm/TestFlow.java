package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.modules.geomorphology.flow.FlowDirections;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.hortonmachine.utils.coverage.CoverageUtilities;

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
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pitfiller", pitfillerData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        FlowDirections flowDirections = new FlowDirections();
        flowDirections.inPit = pitfillerCoverage;
        flowDirections.pm = pm;

        flowDirections.process();

        GridCoverage2D flowCoverage = flowDirections.outFlow;

        checkMatrixEqual(flowCoverage.getRenderedImage(), HMTestMaps.flowData, 0);
    }

}
