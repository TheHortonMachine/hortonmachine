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
package org.hortonmachine.lesto.modules.raster.adaptivetinfilter;

import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.distance3d;

import java.util.Comparator;

import org.locationtech.jts.geom.Coordinate;

/**
 * A comparator for 3d distance from a given coordinate.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PointsToCoordinateComparator implements Comparator<Coordinate> {
    private final Coordinate coordinate;

    public PointsToCoordinateComparator( Coordinate coordinate ) {
        this.coordinate = coordinate;
    }

    @Override
    public int compare( Coordinate o1, Coordinate o2 ) {
        double d1 = distance3d(o1, coordinate, null);
        double d2 = distance3d(o2, coordinate, null);

        if (d1 < d2) {
            return -1;
        } else if (d1 > d2) {
            return 1;
        } else {
            return 0;
        }
    }

}
