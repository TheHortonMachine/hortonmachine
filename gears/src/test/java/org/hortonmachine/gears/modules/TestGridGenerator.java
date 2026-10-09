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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * Test for {@link OmsGridsGenerator}
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGridGenerator extends HMTestCase {

    /**
     * Without cell size, the raster is divided in rows x cols cells.
     */
    public void testRasterDividedInCells() throws Exception {
        OmsGridsGenerator grid = new OmsGridsGenerator();
        grid.pm = pm;
        grid.inRaster = raster();
        grid.pRows = 2;
        grid.pCols = 4;
        grid.process();

        assertEquals(8, grid.outMap.size());
        assertBounds(grid.outMap.getBounds(), 0, 400, 0, 100);
    }

    /**
     * With cell size, the cells start from the lower left corner of the raster: the width goes
     * with the columns, the height with the rows.
     */
    public void testRasterWithCellSize() throws Exception {
        OmsGridsGenerator grid = new OmsGridsGenerator();
        grid.pm = pm;
        grid.inRaster = raster();
        grid.pWidth = 30.0;
        grid.pHeight = 10.0;
        grid.pRows = 3;
        grid.pCols = 2;
        grid.process();

        assertEquals(6, grid.outMap.size());
        assertBounds(grid.outMap.getBounds(), 0, 60, 0, 30);
    }

    public void testFromCoordinates() throws Exception {
        OmsGridsGenerator grid = new OmsGridsGenerator();
        grid.pm = pm;
        grid.pCode = "EPSG:32632";
        grid.pLon = 653000.0;
        grid.pLat = 5100000.0;
        grid.pWidth = 20.0;
        grid.pHeight = 10.0;
        grid.pRows = 5;
        grid.pCols = 3;
        grid.process();

        assertEquals(15, grid.outMap.size());
        assertBounds(grid.outMap.getBounds(), 653000, 653060, 5100000, 5100050);
    }

    /**
     * @return a raster of 400 x 100 m, from 0,0.
     */
    private static GridCoverage2D raster() {
        double[][] data = new double[10][40];
        return CoverageUtilities.buildCoverage("raster", data,
                CoverageUtilities.makeRegionParamsMap(100, 0, 0, 400, 10, 10, 40, 10), HMTestMaps.getCrs(), true);
    }

    private static void assertBounds( ReferencedEnvelope bounds, double w, double e, double s, double n ) {
        assertEquals(w, bounds.getMinX(), 1E-6);
        assertEquals(e, bounds.getMaxX(), 1E-6);
        assertEquals(s, bounds.getMinY(), 1E-6);
        assertEquals(n, bounds.getMaxY(), 1E-6);
    }
}
