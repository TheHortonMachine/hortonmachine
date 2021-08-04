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
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterResolutionResampler;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link OmsRasterResolutionResampler}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterResolutionResampler extends HMTestCase {

    public void testRasterResolutionResampler() throws Exception {

        int[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsRasterResolutionResampler transformer = new OmsRasterResolutionResampler();
        transformer.inGeodata = flowCoverage;
        transformer.pInterpolation = Variables.NEAREST_NEIGHTBOUR;
        transformer.pXres = 60.0;
        transformer.pYres = 60.0;
        transformer.process();
        GridCoverage2D outCoverage = transformer.outGeodata;
        double value = CoverageUtilities.getValue(outCoverage, 1, 1);

        assertEquals(7.0, value, 0.000000001);
    }
}
