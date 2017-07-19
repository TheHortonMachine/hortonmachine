package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.Variables;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.gradient.OmsGradient;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * It test the {@link OmsGradient} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGradient extends HMTestCase {

    public void testGradient() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("elevation", pitData, envelopeParams, crs, true);

        OmsGradient gradient = new OmsGradient();
        gradient.inElev = pitfillerCoverage;
        gradient.pm = pm;

        gradient.process();

        GridCoverage2D gradientCoverage = gradient.outSlope;
        checkMatrixEqual(gradientCoverage.getRenderedImage(), HMTestMaps.gradientData, 0.01);
    }

    public void testGradientHorn() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("elevation", pitData, envelopeParams, crs, true);

        OmsGradient gradient = new OmsGradient();
        gradient.inElev = pitfillerCoverage;
        gradient.pm = pm;
        gradient.pMode = Variables.HORN;

        gradient.process();

        GridCoverage2D gradientCoverage = gradient.outSlope;
        checkMatrixEqual(gradientCoverage.getRenderedImage(), HMTestMaps.gradientHornData, 0.01);
    }

    public void testGradientEvans() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("elevation", pitData, envelopeParams, crs, true);

        OmsGradient gradient = new OmsGradient();
        gradient.inElev = pitfillerCoverage;
        gradient.pm = pm;
        gradient.pMode = Variables.EVANS;

        gradient.process();

        GridCoverage2D gradientCoverage = gradient.outSlope;
        checkMatrixEqual(gradientCoverage.getRenderedImage(), HMTestMaps.gradientEvansData, 0.01);
    }

}
