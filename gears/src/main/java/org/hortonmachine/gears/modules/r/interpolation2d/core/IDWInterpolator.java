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

import org.hortonmachine.gears.libs.modules.HMConstants;

import org.locationtech.jts.geom.Coordinate;

/**
 * Implementation of IDW Interpolation.
 *
 * @author jezekjan
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class IDWInterpolator implements ISurfaceInterpolator {

    private final double buffer;

    public IDWInterpolator( double buffer ) {
        this.buffer = buffer;
    }

    public double getValue( Coordinate[] controlPoints, Coordinate interpolated ) {
        if (controlPoints.length == 0) {
            return HMConstants.doubleNovalue;
        }

        double sumdValue = 0;
        double sumweight = 0;

        for( Coordinate coordinate : controlPoints ) {
            double distance = coordinate.distance(interpolated);
            /*
             * the index if built on envelope, we need a radius check.
             * If not near, do not consider it.
             */
            if (distance > buffer) {
                continue;
            }
            if (distance < 0.00001) {
                distance = 0.00001;
            }
            double weight = (1 / Math.pow(distance, 2));

            sumdValue = sumdValue + coordinate.z * weight;

            sumweight = sumweight + weight;
        }

        double value = sumdValue / sumweight;
        return value;

    }

    @Override
    public double getBuffer() {
        return buffer;
    }

}
