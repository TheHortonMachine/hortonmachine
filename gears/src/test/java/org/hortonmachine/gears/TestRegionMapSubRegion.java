/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears;

import java.util.Random;

import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.RegionMap;
import org.locationtech.jts.geom.Envelope;

/**
 * Test {@link RegionMap#toSubRegion(Envelope)}: the sub region must keep the resolution of the
 * original grid, be snapped on it and contain the requested bounds.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestRegionMapSubRegion extends HMTestCase {

    /**
     * The grid of a real case: bounds lying on grid lines got a row less with the full height,
     * i.e. a stretched resolution, so that cutting a flow map skipped rows and created loops.
     */
    public void testBoundsOnGridLinesKeepTheResolution() throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(163705.3410, 198494.5750, 3908778.5322, 3935847.3621, 2778, 2167);
        double xres = region.getXres();
        double yres = region.getYres();
        // 1244 cols and 769 rows on grid lines
        Envelope env = new Envelope(region.getWest() + 694 * xres, region.getWest() + (694 + 1244) * xres,
                region.getSouth() + 640 * yres, region.getSouth() + (640 + 769) * yres);
        RegionMap sub = region.toSubRegion(env);
        assertEquals(1244, sub.getCols());
        assertEquals(769, sub.getRows());
        assertEquals(xres, sub.getXres(), 1e-9);
        assertEquals(yres, sub.getYres(), 1e-9);
    }

    public void testManyBoundsOnGridLines() throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(163705.3410, 198494.5750, 3908778.5322, 3935847.3621, 2778, 2167);
        double xres = region.getXres();
        double yres = region.getYres();
        Random random = new Random(42);
        for( int i = 0; i < 10000; i++ ) {
            int col = random.nextInt(2000);
            int row = random.nextInt(1500);
            int cols = 1 + random.nextInt(2778 - col - 1);
            int rows = 1 + random.nextInt(2167 - row - 1);
            Envelope env = new Envelope(region.getWest() + col * xres, region.getWest() + (col + cols) * xres,
                    region.getSouth() + row * yres, region.getSouth() + (row + rows) * yres);
            RegionMap sub = region.toSubRegion(env);
            String msg = "case " + i + ": " + cols + "x" + rows + " at " + col + "/" + row;
            assertEquals(msg, cols, sub.getCols());
            assertEquals(msg, rows, sub.getRows());
            assertEquals(msg, xres, sub.getXres(), 1e-9);
            assertEquals(msg, yres, sub.getYres(), 1e-9);
        }
    }

    public void testBoundsInsideCellsAreContainedAndSnapped() throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 100, 0, 50, 100, 50);
        // west in the middle of col 10, east in the middle of col 11: both cells are needed
        RegionMap sub = region.toSubRegion(new Envelope(10.6, 11.5, 20.2, 22.7));
        assertEquals(10.0, sub.getWest(), 1e-9);
        assertEquals(12.0, sub.getEast(), 1e-9);
        assertEquals(20.0, sub.getSouth(), 1e-9);
        assertEquals(23.0, sub.getNorth(), 1e-9);
        assertEquals(2, sub.getCols());
        assertEquals(3, sub.getRows());
        assertEquals(1.0, sub.getXres(), 1e-9);
        assertEquals(1.0, sub.getYres(), 1e-9);
    }
}
