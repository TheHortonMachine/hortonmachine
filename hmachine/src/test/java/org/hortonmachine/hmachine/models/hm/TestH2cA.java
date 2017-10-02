package org.hortonmachine.hmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.hillslopeanalyses.h2ca.OmsH2cA;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * test the {@link OmsH2cA} model.
 * 
 * @author Daniele Andreis
 *
 */
public class TestH2cA extends HMTestCase {

    public void testH2cA() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowGC = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet0Data;
        GridCoverage2D netGC = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);
        double[][] gradientData = HMTestMaps.gradientData;
        GridCoverage2D gradientGC = CoverageUtilities.buildCoverage("gradient", gradientData, envelopeParams, crs, true);
        OmsH2cA h2cA = new OmsH2cA();
        h2cA.inFlow = flowGC;
        h2cA.inNet = netGC;
        h2cA.inAttribute = gradientGC;
        h2cA.process();
        GridCoverage2D outH2cA = h2cA.outAttribute;
        checkMatrixEqual(outH2cA.getRenderedImage(), HMTestMaps.h2caForGradient, 0.05);
    }
}
