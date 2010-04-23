package org.jgrasstools.hortonmachine.models.hm;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.gradient.Gradient;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * It test the {@link Gradient} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGradient extends HMTestCase {

    public void testGradient() throws IOException {

        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("elevation", pitData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        Gradient gradient = new Gradient();
        gradient.inDem = pitfillerCoverage;
        gradient.pm = pm;

        gradient.process();

        GridCoverage2D gradientCoverage = gradient.outSlope;
        checkMatrixEqual(gradientCoverage.getRenderedImage(), HMTestMaps.gradientData, 0.01);
    }

}
