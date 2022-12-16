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
package org.hortonmachine.gears.modules.r.interpolation2d.core;

import java.util.List;

import org.hortonmachine.gears.libs.modules.HMConstants;

import org.locationtech.jts.geom.Coordinate;

/**
 * Implementation of a simple averaging Interpolation.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AveragingInterpolator implements ISurfaceInterpolator {

    private double maxDistance;
    private double minDistance;
    private boolean useBuffer = false;

    public AveragingInterpolator() {
    }

    /**
     * @param minDistance min distance allowed. Put to 0 to ignore.
     * @param maxDistance max distance allowed.
     */
    public AveragingInterpolator( double minDistance, double maxDistance ) {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        useBuffer = true;
    }

    public double getValue( Coordinate[] controlPoints, Coordinate interpolated ) {
        if (controlPoints.length == 0) {
            return HMConstants.doubleNovalue;
        }

        double sumdValue = 0;
        int count = 0;
        for( Coordinate coordinate : controlPoints ) {
            double distance = coordinate.distance(interpolated);
            /*
             * the index is built on envelope, we need a radius check.
             * If not near, do not consider it.
             */
            if (useBuffer && (distance > maxDistance || distance < minDistance)) {
                continue;
            }

            sumdValue += coordinate.z;
            count++;
        }

        double value = sumdValue / count;
        return value;
    }

    public double getValue( List<Coordinate> controlPoints, Coordinate interpolated ) {
        if (controlPoints.isEmpty()) {
            return HMConstants.doubleNovalue;
        }

        double sumdValue = 0;
        int count = 0;
        for( Coordinate coordinate : controlPoints ) {
            double distance = coordinate.distance(interpolated);
            /*
             * the index is built on envelope, we need a radius check.
             * If not near, do not consider it.
             */
            if (useBuffer && distance > maxDistance) {
                continue;
            }

            sumdValue += coordinate.z;
            count++;
        }

        double value = sumdValue / count;
        return value;
    }

}
