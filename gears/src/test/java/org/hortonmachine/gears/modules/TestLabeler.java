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

import org.eclipse.imagen.iterator.RandomIter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.labeler.OmsLabeler;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * Test for {@link OmsLabeler}
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestLabeler extends HMTestCase {
    private static final double NV = HMConstants.doubleNovalue;
    private static final double INV = HMConstants.intNovalue;

    public void testLabels() throws Exception {
        // a U whose arms meet only at the bottom, a diagonal line, a single cell
        double[][] data = new double[][]{//
                {1, NV, 5, NV, NV, 7}, //
                {1, NV, 5, NV, 7, NV}, //
                {3, 3, 3, NV, NV, NV}, //
                {NV, NV, NV, NV, 2, NV}};
        int[][] expected = new int[][]{//
                {1, 0, 1, 0, 0, 2}, //
                {1, 0, 1, 0, 2, 0}, //
                {1, 1, 1, 0, 0, 0}, //
                {0, 0, 0, 0, 3, 0}};

        GridCoverage2D labeled = label(data);
        double[][] expectedValues = new double[expected.length][expected[0].length];
        for( int r = 0; r < expected.length; r++ ) {
            for( int c = 0; c < expected[0].length; c++ ) {
                expectedValues[r][c] = expected[r][c] == 0 ? INV : expected[r][c];
            }
        }
        checkMatrixEqual(labeled.getRenderedImage(), expectedValues, 0);
        assertEquals(INV, HMConstants.getNovalue(labeled), 0);
    }

    public void testSingleCell() throws Exception {
        GridCoverage2D labeled = label(new double[][]{{4}});
        checkMatrixEqual(labeled.getRenderedImage(), new double[][]{{1}}, 0);
    }

    public void testManyComponents() throws Exception {
        // a checkerboard of isolated cells on every other row: every cell is a component
        int size = 101;
        double[][] data = new double[size][size];
        int count = 0;
        for( int r = 0; r < size; r++ ) {
            for( int c = 0; c < size; c++ ) {
                boolean isValid = r % 2 == 0 && c % 2 == 0;
                data[r][c] = isValid ? 1 : NV;
                if (isValid) {
                    count++;
                }
            }
        }
        GridCoverage2D labeled = label(data);
        // the last cell is the last component met
        RandomIter iter = CoverageUtilities.getRandomIterator(labeled);
        assertEquals(count, iter.getSampleDouble(size - 1, size - 1, 0), 0);
        iter.done();
    }

    private GridCoverage2D label( double[][] data ) throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(0, data[0].length, 0, data.length, data[0].length, data.length);
        GridCoverage2D raster = CoverageUtilities.buildCoverage("mask", data, region, HMTestMaps.getCrs(), true);
        OmsLabeler labeler = new OmsLabeler();
        labeler.pm = pm;
        labeler.inMap = raster;
        labeler.process();
        return labeler.outMap;
    }

}
