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
package org.hortonmachine.gears.modules;


import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.connectivity.OmsDownSlopeConnectivity;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsDownSlopeConnectivity}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDownSlopeConnectivity extends HMTestCase {
    private double NaN = HMConstants.doubleNovalue;
    private GridCoverage2D flowGC;
    private GridCoverage2D netGC;
    private GridCoverage2D slopeGC;

    protected void setUp() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        int[][] flowData = HMTestMaps.flowData;
        flowGC = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        double[][] netData = HMTestMaps.extractNet0Data;
        netGC = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        double[][] slopeData = HMTestMaps.slopeData;
        slopeGC = CoverageUtilities.buildCoverage("slope", slopeData, envelopeParams, crs, true);
    }

    public void testDownSlopeConnectivity() throws Exception {
        OmsDownSlopeConnectivity c = new OmsDownSlopeConnectivity();
        c.inFlow = flowGC;
        c.inNet = netGC;
        c.inSlope = slopeGC;
        c.pWeight = 100.0;
        c.process();

        GridCoverage2D outConnectivity = c.outConnectivity;

        double[][] expected = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 0.15431712, 0.18187551, 0.38108143, 0.40452333, 0.37165276, 0.32985926, 0.11700876, NaN}, //
                {NaN, 0.06423982, 0.06202691, 0.21727291, 0.22475042, 0.25180416, 0.26985020, 0.04497751, 60.03, NaN}, //
                {NaN, 0.0, 0.0375, 0.04497751, 0.07203125, 0.09007729, 0.11984860, 0.0, 60.0, NaN}, //
                {NaN, 0.06423982, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 60.0, NaN}, //
                {NaN, 0.10580151, 0.03333333, 0.03601440, 0.06921110, 0.10580151, 0.11984860, 0.04497751, 60.02570694, NaN}, //
                {NaN, 0.0, 0.22565012, 0.21310624, 0.63356943, 0.66676613, 0.70962327, 0.19187985, 0.11700876, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(outConnectivity.getRenderedImage(), expected, DELTA);
    }

}
