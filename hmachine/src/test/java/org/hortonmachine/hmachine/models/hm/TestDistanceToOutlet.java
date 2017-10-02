/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.network.distancetooutlet.OmsDistanceToOutlet;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestDistanceToOutlet extends HMTestCase {

    /**
     * test {@link OmsDistanceToOutlet} in the topological mode.
     * 
     */
    public void testDistanceToOutletTopological() {

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        OmsDistanceToOutlet distanceToOutlet = new OmsDistanceToOutlet();
        distanceToOutlet.inFlow = flowCoverage;
        distanceToOutlet.pMode = 1;
        distanceToOutlet.process();
        GridCoverage2D distanceCoverage = distanceToOutlet.outDistance;
        checkMatrixEqual(distanceCoverage.getRenderedImage(), HMTestMaps.d2oPixelData);
    }

    /**
     * test {@link OmsDistanceToOutlet} in the simple mode.
     * 
     */
    public void testDistanceToOutletMetere() {

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        OmsDistanceToOutlet distanceToOutlet = new OmsDistanceToOutlet();
        distanceToOutlet.inFlow = flowCoverage;
        distanceToOutlet.pMode = 0;
        distanceToOutlet.process();
        GridCoverage2D distanceCoverage = distanceToOutlet.outDistance;
        checkMatrixEqual(distanceCoverage.getRenderedImage(), HMTestMaps.d2oMeterData, 0.01);
    }
    
    /**
     * test {@link OmsDistanceToOutlet} in 3d.
     * 
     */
    public void testDistanceToOutlet3D() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        //get the flow direction map.
        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        //get the pit map.
        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs, true);
        OmsDistanceToOutlet distanceToOutlet = new OmsDistanceToOutlet();
        //set the needed input. 
        distanceToOutlet.pMode = 0;
        distanceToOutlet.inFlow = flowCoverage;
        distanceToOutlet.inPit = pitCoverage;
        distanceToOutlet.process();
        GridCoverage2D distanceCoverage = distanceToOutlet.outDistance;
        checkMatrixEqual(distanceCoverage.getRenderedImage(), HMTestMaps.d2o3dData);
    }
    
}
