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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.transitionclasses.OmsRasterTransitionClasses;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * Test for {@link OmsRasterTransitionClasses}
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestRasterTransitionClasses extends HMTestCase {
    private static final double NV = HMConstants.doubleNovalue;
    private static final double INV = HMConstants.intNovalue;

    public void testTransitions() throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 3, 0, 2, 3, 2);
        // a NaN novalue, that used to become the class 0 of the integer output
        GridCoverage2D previous = CoverageUtilities.buildCoverageWithNovalue("previous", new double[][]{//
                {1, 1, 2}, //
                {Double.NaN, 2, Double.NaN}}, region, HMTestMaps.getCrs(), true, Double.NaN);
        GridCoverage2D next = CoverageUtilities.buildCoverage("next", new double[][]{//
                {1, 3, 3}, //
                {3, NV, NV}}, region, HMTestMaps.getCrs(), true);

        OmsRasterTransitionClasses transitions = new OmsRasterTransitionClasses();
        transitions.pm = pm;
        transitions.inPreviousRaster = previous;
        transitions.inNextRaster = next;
        transitions.process();

        // 1->1 unchanged, 1->3 and 2->3 changes, nothing->3 and 2->nothing appear/disappear
        double[][] expected = new double[][]{//
                {10001, 1, 2}, //
                {-1, -2, INV}};
        checkMatrixEqual(transitions.outRaster.getRenderedImage(), expected, 0);
        assertEquals(INV, HMConstants.getNovalue(transitions.outRaster), 0);

        String table = transitions.getTransitionsTable();
        assertEquals("code;previous;next;count\n" + //
                "-2;2;unaccounted;1\n" + //
                "-1;unaccounted;3;1\n" + //
                "1;1;3;1\n" + //
                "2;2;3;1\n" + //
                "10001;1;1;1\n", table);
    }

}
