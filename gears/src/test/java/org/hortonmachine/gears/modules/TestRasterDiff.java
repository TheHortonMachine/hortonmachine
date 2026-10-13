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

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * Test for {@link OmsRasterDiff}
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterDiff extends HMTestCase {
    private static final double NV = HMConstants.doubleNovalue;

    private GridCoverage2D dsm;
    private GridCoverage2D dtm;

    protected void setUp() throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 3, 0, 2, 3, 2);
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        dsm = CoverageUtilities.buildCoverage("surface", new double[][]{//
                {110, 101, NV}, //
                {99.5, 103, 104}}, region, crs, true);
        dtm = CoverageUtilities.buildCoverage("terrain", new double[][]{//
                {100, 100, 100}, //
                {100, NV, 100}}, region, crs, true);
    }

    public void testDiff() throws Exception {
        OmsRasterDiff diff = new OmsRasterDiff();
        diff.pm = pm;
        diff.inRaster1 = dsm;
        diff.inRaster2 = dtm;
        diff.process();

        // a novalue in either raster gives a novalue
        double[][] expected = new double[][]{//
                {10, 1, NV}, //
                {-0.5, NV, 4}};
        checkMatrixEqual(diff.outRaster.getRenderedImage(), expected, 1E-9);
    }

    public void testNoNegatives() throws Exception {
        OmsRasterDiff diff = new OmsRasterDiff();
        diff.pm = pm;
        diff.inRaster1 = dsm;
        diff.inRaster2 = dtm;
        diff.doNegatives = false;
        diff.process();

        double[][] expected = new double[][]{//
                {10, 1, NV}, //
                {0, NV, 4}};
        checkMatrixEqual(diff.outRaster.getRenderedImage(), expected, 1E-9);
    }

    public void testThreshold() throws Exception {
        OmsRasterDiff diff = new OmsRasterDiff();
        diff.pm = pm;
        diff.inRaster1 = dsm;
        diff.inRaster2 = dtm;
        diff.doNegatives = false;
        diff.pThreshold = 2.0;
        diff.process();

        // the differences below the threshold become novalue, the clamped negative included
        double[][] expected = new double[][]{//
                {10, NV, NV}, //
                {NV, NV, 4}};
        checkMatrixEqual(diff.outRaster.getRenderedImage(), expected, 1E-9);
    }

    public void testDifferentGrids() throws Exception {
        GridCoverage2D other = CoverageUtilities.buildCoverage("other", new double[][]{{1, 2}},
                RegionMap.fromBoundsAndGrid(0, 2, 0, 1, 2, 1), HMTestMaps.getCrs(), true);
        OmsRasterDiff diff = new OmsRasterDiff();
        diff.pm = pm;
        diff.inRaster1 = dsm;
        diff.inRaster2 = other;
        try {
            diff.process();
            fail("Different grids should not be accepted.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
    }

}
