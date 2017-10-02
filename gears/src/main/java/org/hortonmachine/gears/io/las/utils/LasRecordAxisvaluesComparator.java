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
package org.hortonmachine.gears.io.las.utils;

import java.util.Comparator;

import org.hortonmachine.gears.io.las.core.LasRecord;

/**
 * Comparator for x or y values to sort in x or y direction.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasRecordAxisvaluesComparator implements Comparator<LasRecord> {

    private boolean doY;

    public LasRecordAxisvaluesComparator( boolean doY ) {
        this.doY = doY;
    }

    @Override
    public int compare( LasRecord o1, LasRecord o2 ) {
        double v1;
        double v2;
        if (doY) {
            v1 = o1.y;
            v2 = o2.y;
        } else {
            v1 = o1.x;
            v2 = o2.x;
        }
        if (v1 < v2) {
            return -1;
        } else if (v1 > v2) {
            return 1;
        } else {
            return 0;
        }
    }
}
