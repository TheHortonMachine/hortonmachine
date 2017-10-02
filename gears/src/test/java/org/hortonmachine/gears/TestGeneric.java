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
package org.hortonmachine.gears;

import org.hortonmachine.gears.utils.BitMatrix;
import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Generic tests.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGeneric extends HMTestCase {

    public void testBitMatrix() throws Exception {

        BitMatrix m = new BitMatrix(5, 5);

        m.mark(0, 0);
        m.mark(1, 1);
        m.mark(2, 2);
        m.mark(3, 3);
        m.mark(4, 4);

        for( int i = 0; i < 5; i++ ) {
            for( int j = 0; j < 5; j++ ) {
                boolean isMarked = m.isMarked(i, j);
                if (i == j) {
                    assertTrue(isMarked);
                } else {
                    assertFalse(isMarked);
                }
            }
        }
    }

}
