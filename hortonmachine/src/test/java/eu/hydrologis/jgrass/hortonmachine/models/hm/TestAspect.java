package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.modules.geomorphology.aspect.Aspect;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.hortonmachine.utils.coverage.CoverageUtilities;

/**
 * Test the {@link Aspect} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAspect extends HMTestCase {
    public void testAspectDegrees() throws Exception {
        double[][] pitData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Aspect aspect = new Aspect();
        aspect.inDem = pitCoverage;
        aspect.doRound = true;
        aspect.pm = pm;

        aspect.process();

        GridCoverage2D aspectCoverage = aspect.outAspect;

        checkMatrixEqual(aspectCoverage.getRenderedImage(), HMTestMaps.aspectDataDegrees, 0.01);
    }

    public void testAspectRadiants() throws Exception {
        double[][] pitData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs);
        
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        Aspect aspect = new Aspect();
        aspect.inDem = pitCoverage;
        aspect.doRadiants = true;
        aspect.pm = pm;
        
        aspect.process();
        
        GridCoverage2D aspectCoverage = aspect.outAspect;
        
        checkMatrixEqual(aspectCoverage.getRenderedImage(), HMTestMaps.aspectDataRadiants, 0.01);
    }

}
