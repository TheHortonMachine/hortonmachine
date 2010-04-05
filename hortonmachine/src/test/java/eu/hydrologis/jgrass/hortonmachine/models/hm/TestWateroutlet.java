package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.modules.demmanipulation.wateroutlet.Wateroutlet;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;

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
