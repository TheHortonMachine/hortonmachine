package org.hortonmachine.hmachine.models.hm;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.network.magnitudo.OmsMagnitudo;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsOldTca} module.
 * 
 * @author Giuseppe Formetta ()
 */
public class TestMagnitudo extends HMTestCase {

    public void testTca() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

       
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
                envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        OmsMagnitudo magnit = new OmsMagnitudo();
        
        magnit.inFlow = flowCoverage;
        magnit.pm = pm;

        magnit.process();
        
        GridCoverage2D magnitudoCoverage = magnit.outMag;

        checkMatrixEqual(magnitudoCoverage.getRenderedImage(), HMTestMaps.magnitudoData);

    }

}