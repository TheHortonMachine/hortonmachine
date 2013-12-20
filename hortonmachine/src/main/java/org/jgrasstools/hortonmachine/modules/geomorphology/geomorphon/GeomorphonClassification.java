/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.hortonmachine.modules.geomorphology.geomorphon;

/**
 * Class representing the Geomorphon classification.
 * 
 * <p>
 * <ul>
 * <li>flat = 1000</li>
 * <li>peak = 1001</li>
 * <li>ridge = 1002</li>
 * <li>shoulder = 1003</li>
 * <li>spur = 1004</li>
 * <li>slope = 1005</li>
 * <li>hollow = 1006</li>
 * <li>footslope = 1007</li>
 * <li>valley = 1008</li>
 * <li>pit = 1009</li>
 * </ul>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeomorphonClassification {

    /**
     * The geomorphon classification matrix.
     * 
     * <p>cols = pluses count
     * <p>rows = minuses count
     */
    private static int[][] categories = new int[][]{//
    /*    */{1000, 1000, 1000, 1007, 1007, 1008, 1008, 1008, 1009},// 0
            {1000, 1000, 1007, 1007, 1007, 1008, 1008, 1008, -1},// 1
            {1000, 1003, 1005, 1005, 1006, 1006, 1008, -1, -1},// 2
            {1003, 1003, 1005, 1005, 1005, 1006, -1, -1, -1},// 3
            {1003, 1003, 1004, 1005, 1005, -1, -1, -1, -1},// 4
            {1002, 1002, 1004, 1004, -1, -1, -1, -1, -1},// 5
            {1002, 1002, 1002, -1, -1, -1, -1, -1, -1},// 6
            {1002, 1002, -1, -1, -1, -1, -1, -1, -1},// 7
            {1001, -1, -1, -1, -1, -1, -1, -1, -1},// 8
    };

    /**
     * Returns the right classification value for the plus and minus counts.
     * 
     * @param plusCount
     * @param minusCount
     * @return the classification value.
     */
    public static int getClassification( int plusCount, int minusCount ) {
        return categories[minusCount][plusCount];
    }

}
