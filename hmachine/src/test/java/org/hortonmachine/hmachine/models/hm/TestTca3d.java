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
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.tca3d.OmsTca3d;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test the {@link OmsTca3d} module.
 * 
 * @author Giuseppe Formetta ()
 */
public class TestTca3d extends HMTestCase {
    private double N = HMConstants.doubleNovalue;
    public void testTca3d() throws Exception {
        
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pitfiller", pitfillerData, envelopeParams, crs, true);
        // double[][] flowData = HMTestMaps.flowData;
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsTca3d tca3d = new OmsTca3d();
        tca3d.inPit = pitCoverage;
        tca3d.inFlow = flowCoverage;
        tca3d.pm = pm;
        tca3d.process();

        GridCoverage2D tca3dCoverage = tca3d.outTca;
        printImage(tca3dCoverage.getRenderedImage());

        double[][] tca3DData = new double[][]{ //
            /*    */{N, N, N, N, N, N, N, N, N, N}, //
                    {N, N, 5438.94, 5370.95, 5963.77, 6552.17, 7082.91, 6568.44, 7191.73, N}, //
                    {N, 10320.83, 9568.38, 8653.17, 8973.7, 9682.18, 10800.86, 13431.92, 10391.83, N}, //
                    {N, 257338.23, 14529.45, 13231.44, 13420.35, 13928.84, 3096.91, 39495.19, 22327, N}, //
                    {N, 5382.74, 212067.61, 185732.11, 123898.49, 101011.35, 82961.85, 40031.45, 22377.36, N}, //
                    {N, 9234.82, 9916.27, 10455.59, 17929.35, 3652.57, 11112.19, 13567.34, 10219.8, N}, //
                    {N, 5384.03, 4828.31, 4947.56, 5864.58, 14606.37, 7294.38, 6426.99, 7298.45, N}, //
                    {N, N, N, N, N, N, N, N, N, N} //
            };
        checkMatrixEqual(tca3dCoverage.getRenderedImage(), tca3DData, 0.02);

    }

}
