package org.jgrasstools.hortonmachine.models.hm;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.tca.OmsOldTca;
import org.jgrasstools.hortonmachine.modules.network.magnitudo.Magnitudo;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsOldTca} module.
 * 
 * @author Giuseppe Formetta ()
 */
public class TestMagnitudo extends HMTestCase {

    public void testTca() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

       
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
                envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Magnitudo magnit = new Magnitudo();
        
        magnit.inFlow = flowCoverage;
        magnit.pm = pm;

        magnit.process();
        
        GridCoverage2D magnitudoCoverage = magnit.outMag;

        checkMatrixEqual(magnitudoCoverage.getRenderedImage(), HMTestMaps.magnitudoData);

    }

}