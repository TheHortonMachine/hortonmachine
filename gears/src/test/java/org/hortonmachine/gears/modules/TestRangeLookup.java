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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.rangelookup.OmsRangeLookup;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsRangeLookup}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRangeLookup extends HMTestCase {

    public void testRangeLookup() throws Exception {
        double[][] inData = HMTestMaps.rangeLookupInData;
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        OmsRangeLookup range = new OmsRangeLookup();
        range.pm = pm;
        range.inRaster = inCoverage;
        range.pRanges = "[0 90),[90 180),[180 270),[270 360)";
        range.pClasses = "1,2,3,4";
        range.process();
        GridCoverage2D out = range.outRaster;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.rangeLookupOutData, 0);
    }

    /**
     * A range open downwards contains the novalue -9999, whose cells must stay novalue.
     */
    public void testNovalueInOpenRange() throws Exception {
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("dtm", HMTestMaps.mapData, HMTestMaps.getEnvelopeparams(),
                HMTestMaps.getCrs(), true);

        OmsRangeLookup range = new OmsRangeLookup();
        range.pm = pm;
        range.inRaster = inCoverage;
        range.pRanges = "[null 1000),[1000 null)";
        range.pClasses = "1,2";
        range.process();

        double[][] data = HMTestMaps.mapData;
        double[][] expected = new double[data.length][data[0].length];
        for( int r = 0; r < data.length; r++ ) {
            for( int c = 0; c < data[0].length; c++ ) {
                double v = data[r][c];
                expected[r][c] = HMConstants.isNovalue(v) ? HMConstants.doubleNovalue : v < 1000 ? 1 : 2;
            }
        }
        checkMatrixEqual(range.outRaster.getRenderedImage(), expected, 0);
        // the expected novalue is checked as novalue by checkMatrixEqual only in one direction
        assertTrue(HMConstants.isNovalue(CoverageUtilities.getValue(range.outRaster, 1, 1)));
    }

}
