package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.distancetooutlet.DistanceToOutlet;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestDistanceToOutlet extends HMTestCase {
    
    public void testDistanceToOutlet() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        DistanceToOutlet distanceToOutlet = new DistanceToOutlet();
        distanceToOutlet.inFlow = flowCoverage;
        distanceToOutlet.pMode = 0;
        distanceToOutlet.process();
        GridCoverage2D distanceCoverage = distanceToOutlet.outDistance;
        checkMatrixEqual(distanceCoverage.getRenderedImage(), HMTestMaps.d2oPixelData);
    }
}
