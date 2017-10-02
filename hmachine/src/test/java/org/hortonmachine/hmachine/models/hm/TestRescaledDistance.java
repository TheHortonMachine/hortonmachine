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

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.basin.rescaleddistance.OmsRescaledDistance;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test {@link OmsRescaledDistance}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRescaledDistance extends HMTestCase {

    public void testRescaledDistance() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet0Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        OmsRescaledDistance rescaledDistance = new OmsRescaledDistance();
        rescaledDistance.inFlow = flowCoverage;
        rescaledDistance.inNet = netCoverage;
        rescaledDistance.pRatio = 0.3f;
        rescaledDistance.pm = pm;

        rescaledDistance.process();

        GridCoverage2D rescaledDistanceCoverage = rescaledDistance.outRescaled;
        checkMatrixEqual(rescaledDistanceCoverage.getRenderedImage(), HMTestMaps.rescaledDistanceData, 0.1);
    }

    public void testRescaledDistance3D() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet0Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);
        double[][] elevData = HMTestMaps.mapData;
        GridCoverage2D elevCoverage = CoverageUtilities.buildCoverage("elev", elevData, envelopeParams, crs, true);

        OmsRescaledDistance rescaledDistance = new OmsRescaledDistance();
        rescaledDistance.inFlow = flowCoverage;
        rescaledDistance.inNet = netCoverage;
        rescaledDistance.inElev = elevCoverage;
        rescaledDistance.pRatio = 0.3f;
        rescaledDistance.pm = pm;

        rescaledDistance.process();

        // GridCoverage2D rescaledDistanceCoverage = rescaledDistance.outRescaled;
        // PrintUtilities.printCoverageData(rescaledDistanceCoverage);
        // checkMatrixEqual(rescaledDistanceCoverage.getRenderedImage(),
        // HMTestMaps.rescaledDistanceData, 0.1);
    }

}
