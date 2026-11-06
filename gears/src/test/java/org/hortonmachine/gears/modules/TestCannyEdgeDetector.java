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
package org.hortonmachine.gears.modules;

import java.awt.image.Raster;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * Tests the {@link OmsCannyEdgeDetector} module on synthetic steps.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestCannyEdgeDetector extends HMTestCase {

    private static final int SIZE = 30;

    private interface Values {
        double value( int col, int row );
    }

    private GridCoverage2D map( Values values ) {
        double[][] data = new double[SIZE][SIZE];
        for( int r = 0; r < SIZE; r++ ) {
            for( int c = 0; c < SIZE; c++ ) {
                data[r][c] = values.value(c, r);
            }
        }
        RegionMap region = RegionMap.fromBoundsAndGrid(0, SIZE * 10, 0, SIZE * 10, SIZE, SIZE);
        return CoverageUtilities.buildCoverage("map", data, region, HMTestMaps.getCrs(), true);
    }

    private boolean[][] edges( GridCoverage2D map, double low, double high ) throws Exception {
        OmsCannyEdgeDetector canny = new OmsCannyEdgeDetector();
        canny.inMap = map;
        canny.pLowthres = low;
        canny.pHighthres = high;
        canny.pm = pm;
        canny.process();

        Raster raster = canny.outMap.getRenderedImage().getData();
        boolean[][] edges = new boolean[SIZE][SIZE];
        for( int r = 0; r < SIZE; r++ ) {
            for( int c = 0; c < SIZE; c++ ) {
                double v = raster.getSampleDouble(c, r, 0);
                if (!HMConstants.isNovalue(v)) {
                    assertEquals(1.0, v, 0);
                    edges[r][c] = true;
                }
            }
        }
        return edges;
    }

    private int edgesInRow( boolean[][] edges, int row, int fromCol, int toCol ) {
        int count = 0;
        for( int c = fromCol; c <= toCol; c++ ) {
            if (edges[row][c]) {
                count++;
            }
        }
        return count;
    }

    public void testStep() throws Exception {
        boolean[][] edges = edges(map(( c, r ) -> c < 15 ? 0 : 10), 0.1, 0.2);
        // a single, one cell thin, line along the step, also near the borders of the map
        for( int r = 1; r < SIZE - 1; r++ ) {
            assertEquals("row " + r, 1, edgesInRow(edges, r, 0, SIZE - 1));
            assertTrue("row " + r, edges[r][14] || edges[r][15]);
        }
    }

    public void testWeakChangesLeftOut() throws Exception {
        // a strong step and, apart, a step 20 times weaker
        boolean[][] edges = edges(map(( c, r ) -> (c < 8 ? 0 : 10) + (c < 22 ? 0 : 0.5)), 0.1, 0.2);
        for( int r = 1; r < SIZE - 1; r++ ) {
            assertEquals(1, edgesInRow(edges, r, 0, 14));
            assertEquals(0, edgesInRow(edges, r, 15, SIZE - 1));
        }
    }

    public void testHysteresis() throws Exception {
        // the step fades from 10 at the top to 1.2 at the bottom
        Values values = ( c, r ) -> c < 15 ? 0 : 10 - 8.8 * r / (SIZE - 1);
        boolean[][] onlyStrong = edges(map(values), 0.3, 0.3);
        boolean[][] followed = edges(map(values), 0.1, 0.3);
        // with a single threshold the weaker part is missing
        assertEquals(0, edgesInRow(onlyStrong, 27, 0, SIZE - 1));
        // with the low threshold the edge continues from the strong part into the weaker one (the
        // last row before the border is left out, there the ramp along the rows adds a cell)
        for( int r = 1; r < SIZE - 2; r++ ) {
            assertEquals("row " + r, 1, edgesInRow(followed, r, 0, SIZE - 1));
            assertTrue("row " + r, followed[r][14] || followed[r][15]);
        }
    }

    public void testNovalueIsNoEdge() throws Exception {
        // a flat map with a hole of no-data: the border of the data is not an edge
        boolean[][] edges = edges(map(( c, r ) -> c > 8 && c < 20 && r > 8 && r < 20 ? HMConstants.doubleNovalue : 5),
                0.1, 0.2);
        for( int r = 0; r < SIZE; r++ ) {
            assertEquals(0, edgesInRow(edges, r, 0, SIZE - 1));
        }

        // a step crossing the hole is still found outside of it
        edges = edges(map(( c, r ) -> r > 10 && r < 18 && c > 5 && c < 25 ? HMConstants.doubleNovalue : (c < 15 ? 0 : 10)),
                0.1, 0.2);
        assertTrue(edges[3][14] || edges[3][15]);
        assertTrue(edges[25][14] || edges[25][15]);
        for( int r = 11; r < 18; r++ ) {
            assertEquals(0, edgesInRow(edges, r, 6, 24));
        }
    }

    public void testThresholds() throws Exception {
        GridCoverage2D map = map(( c, r ) -> c);
        try {
            edges(map, 0.3, 0.2);
            fail("The low threshold can't be above the high one.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
        try {
            edges(map, 0.1, 2.0);
            fail("The thresholds are fractions.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
    }
}
