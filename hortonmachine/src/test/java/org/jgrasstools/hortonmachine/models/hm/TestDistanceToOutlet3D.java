package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.distancetooutlet3d.DistanceToOutlet3D;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test for the {@link DistanceToOutlet3D} models.
 * 
 * @author Daniele Andreis.
 *
 */

public class TestDistanceToOutlet3D extends HMTestCase {

    public void testDistanceToOutlet3D() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        //get the flow direction map.
        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        //get the pit map.
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs, true);
        DistanceToOutlet3D distanceToOutlet = new DistanceToOutlet3D();
        //set the needed input.
        distanceToOutlet.inFlow = flowCoverage;
        distanceToOutlet.inPit = pitCoverage;
        distanceToOutlet.process();
        GridCoverage2D distanceCoverage = distanceToOutlet.outDistance;
        checkMatrixEqual(distanceCoverage.getRenderedImage(), HMTestMaps.d2o3dData);
    }
}
