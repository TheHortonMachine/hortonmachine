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
 * Comparator for elevation in {@link LasRecord}s.
 * 
 * <p>The default use in {@link Collections#sort(java.util.List)} orders 
 * the points from the lowest to the highest elevation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasRecordGroundElevationComparator implements Comparator<LasRecord> {

    private boolean doReverse;

    public LasRecordGroundElevationComparator() {
        this(false);
    }

    public LasRecordGroundElevationComparator( boolean doReverse ) {
        this.doReverse = doReverse;
    }

    @Override
    public int compare( LasRecord o1, LasRecord o2 ) {
        if (doReverse) {
            if (o1.groundElevation < o2.groundElevation) {
                return 1;
            } else if (o1.groundElevation > o2.groundElevation) {
                return -1;
            } else {
                return 0;
            }
        } else {
            if (o1.groundElevation < o2.groundElevation) {
                return -1;
            } else if (o1.groundElevation > o2.groundElevation) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
