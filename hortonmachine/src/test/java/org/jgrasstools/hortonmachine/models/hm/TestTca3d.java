package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.tca.Tca;
import org.jgrasstools.hortonmachine.modules.geomorphology.tca3d.Tca3d;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test the {@link Tca} module.
 * 
 * @author Giuseppe Formetta ()
 */
public class TestTca3d extends HMTestCase{
	public void testTca3d() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pitfiller",
                pitfillerData, envelopeParams, crs, true);
        //double[][] flowData = HMTestMaps.flowData;
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
                envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Tca3d tca3d = new Tca3d();
        
        tca3d.inPit = pitCoverage;
        tca3d.inFlow = flowCoverage;
        tca3d.pm = pm;

        tca3d.process();
        
        GridCoverage2D tca3dCoverage = tca3d.outTca3d;

        checkMatrixEqual(tca3dCoverage.getRenderedImage(), HMTestMaps.tca3DData,0.02);

    }

	
	
}
