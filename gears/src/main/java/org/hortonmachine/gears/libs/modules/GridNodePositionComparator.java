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
 * A simple comparator to sort {@link GridNode}s left to right order.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GridNodePositionComparator implements Comparator<GridNode> {

    @Override
    public int compare( GridNode o1, GridNode o2 ) {
        if (o1.col == o2.col && o1.row == o2.row) {
            return 0;
        } else if (o1.col < o2.col) {
            return -1;
        } else {
            return 1;
        }
    }

}
