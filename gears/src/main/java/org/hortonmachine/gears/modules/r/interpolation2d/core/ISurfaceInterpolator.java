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

import org.locationtech.jts.geom.Coordinate;

/**
 * Simple interface for surface interpolation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface ISurfaceInterpolator {
    /**
     * Gets an interpolated value in a agiven position, from a set of control values.
     * 
     * @param controlPoints all the controlpoints to consider to evaluate the interpolated point.
     * @param interpolated the coordinate in which to interpolate.
     * @return the interpolated z value.
     */
    public double getValue( Coordinate[] controlPoints, Coordinate interpolated );
    
    
    public double getBuffer();
}