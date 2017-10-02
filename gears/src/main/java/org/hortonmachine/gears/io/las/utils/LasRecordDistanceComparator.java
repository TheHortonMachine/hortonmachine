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

import java.util.Collections;
import java.util.Comparator;

import org.hortonmachine.gears.io.las.core.LasRecord;

/**
 * Comparator for distance {@link LasRecord}s.
 * 
 * <p>The default use in {@link Collections#sort(java.util.List)} orders 
 * the points from the nearest to the farest from a given point.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasRecordDistanceComparator implements Comparator<LasRecord> {

    private boolean doReverse;
    private LasRecord center;

    public LasRecordDistanceComparator( LasRecord center ) {
        this(center, false);
    }

    public LasRecordDistanceComparator( LasRecord center, boolean doReverse ) {
        this.center = center;
        this.doReverse = doReverse;
    }

    @Override
    public int compare( LasRecord o1, LasRecord o2 ) {
        double d1 = LasUtils.distance(center, o1);
        double d2 = LasUtils.distance(center, o2);
        if (doReverse) {
            if (d1 < d2) {
                return 1;
            } else if (d1 > d2) {
                return -1;
            } else {
                return 0;
            }
        } else {
            if (d1 < d2) {
                return -1;
            } else if (d1 > d2) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
