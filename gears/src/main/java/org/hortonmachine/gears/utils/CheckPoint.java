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
package org.hortonmachine.gears.utils;

/**
 * A point with a fast hash.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CheckPoint {
    public final int x;
    public final int y;
    
    public CheckPoint( int col, int row ) {
        this.x = col;
        this.y = row;
    }

    public boolean equals( Object obj ) {
        if (obj instanceof CheckPoint) {
            CheckPoint d = (CheckPoint) obj;
            return (x == d.x) && (y == d.y);
        }
        return false;
    }

    public int hashCode() {
        int sum = x + y;
        return sum * (sum + 1) / 2 + y;
    }

}