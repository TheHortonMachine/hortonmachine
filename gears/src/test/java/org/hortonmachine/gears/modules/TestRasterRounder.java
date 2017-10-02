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
import org.hortonmachine.gears.modules.r.rastervaluerounder.OmsRasterValueRounder;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link OmsRasterTransformer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterRounder extends HMTestCase {
    private double NaN = HMConstants.doubleNovalue;

    public void testRasterRounder() throws Exception {
        double[][] flowData = new double[][]{//
        {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 6.34589, 6.34589, 6.34589, 6.34589, 6.34589, 6.34589, 6.34589, NaN}, //
                {NaN, 7, 6.34589, 6.34589, 6.34589, 6.34589, 6.34589, 7, 7, NaN}, //
                {NaN, 5, 5, 7, 6.34589, 6.34589, 6.34589, 6.34589, 5, NaN}, //
                {NaN, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
                {NaN, 2, 3, 3, 4, 4, 4, 3, 3, NaN}, //
                {NaN, 4, 4, 4, 4, 4, 5, 4, 4, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsRasterValueRounder transformer = new OmsRasterValueRounder();
        transformer.inRaster = inCoverage;
        transformer.pPattern = ".000";
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;

        double value = CoverageUtilities.getValue(outCoverage, 3, 2);
        String expected = "6.346";
        String valueStr = String.valueOf(value).replace(',', '.');
        assertEquals(expected, valueStr);

        transformer = new OmsRasterValueRounder();
        transformer.inRaster = inCoverage;
        transformer.pPattern = "##.";
        transformer.process();
        outCoverage = transformer.outRaster;

        value = CoverageUtilities.getValue(outCoverage, 3, 2);
        expected = "6.0";
        valueStr = String.valueOf(value).replace(',', '.');
        assertEquals(expected, valueStr);

    }
}
