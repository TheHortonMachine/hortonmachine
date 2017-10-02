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
package org.hortonmachine.gears.libs.modules;

import java.util.Comparator;

/**
 * Comparator to sort {@link GridNode}s in ascending order.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GridNodeElevationToLeastComparator implements Comparator<GridNode> {

    @Override
    public int compare( GridNode o1, GridNode o2 ) {
        if (o1.elevation < o2.elevation) {
            return -1;
        } else if (o1.elevation > o2.elevation) {
            return 1;
        } else {
            /*
             * if they are equal, check if they are in the same place.
             * If they are in the same place, ok, else order them and handle them as different.
             */
            if (o1.col == o2.col && o1.row == o2.row) {
                return 0;
            } else {
                return -1;
            }
        }
    }

}
