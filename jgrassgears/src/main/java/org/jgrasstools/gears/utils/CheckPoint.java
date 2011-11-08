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
package org.jgrasstools.gears.utils;

/**
 * A point that can checked by comparison.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CheckPoint implements Comparable<CheckPoint> {
    public int col;
    public int row;
    public int index;

    public CheckPoint( int col, int row, int index ) {
        this.col = col;
        this.row = row;
        this.index = index;
    }

    public int compareTo( CheckPoint o ) {
        /*
         * if row and col are equal, return 0, which will 
         * anyways trigger and exception
         */
        if (col == o.col && row == o.row) {
            return 0;
        }

        /*
         * in the case of non equal row/col, we need to make the normal sort
         */
        if (index < o.index) {
            return -1;
        } else if (index > o.index) {
            return 1;
        } else {
            return 0;
        }

    }

}