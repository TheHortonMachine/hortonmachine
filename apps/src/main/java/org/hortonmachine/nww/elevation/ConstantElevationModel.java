/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.nww.elevation;

import java.util.List;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.terrain.ZeroElevationModel;

/**
 * A flat terrain at a given elevation, everywhere.
 *
 * <p>It declares a very coarse resolution, so that in a compound elevation model it is the base that
 * all the other models override where they have data.</p>
 */
public class ConstantElevationModel extends ZeroElevationModel {
    /** About 60 km on Earth, coarser than any real elevation model. */
    private static final double RESOLUTION = 0.01;

    private final double elevation;

    /**
     * @param elevation the elevation of the terrain.
     */
    public ConstantElevationModel( double elevation ) {
        this.elevation = elevation;
    }

    @Override
    public double getMaxElevation() {
        return elevation + 1;
    }

    @Override
    public double getMinElevation() {
        return elevation;
    }

    @Override
    public double[] getExtremeElevations( Angle latitude, Angle longitude ) {
        return new double[]{elevation, elevation + 1};
    }

    @Override
    public double[] getExtremeElevations( Sector sector ) {
        return new double[]{elevation, elevation + 1};
    }

    @Override
    public double getElevations( Sector sector, List< ? extends LatLon> latlons, double targetResolution, double[] buffer ) {
        for( int i = 0; i < latlons.size(); i++ ) {
            buffer[i] = elevation;
        }
        this.setValue(AVKey.FRAME_TIMESTAMP, System.currentTimeMillis());
        return RESOLUTION;
    }

    @Override
    public double getBestResolution( Sector sector ) {
        return RESOLUTION;
    }

    @Override
    public double getUnmappedElevation( Angle latitude, Angle longitude ) {
        return elevation;
    }
}
