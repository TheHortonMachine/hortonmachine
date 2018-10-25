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
package org.hortonmachine.gears.utils.coverage;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;

import java.util.List;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

import org.locationtech.jts.geom.Coordinate;

/**
 * A point representing a position in a raster profile.
 * 
 * <p>The point is sortable by its progressive value.</p>
 * 
 * <p>
 * Note that two {@link ProfilePoint}s are meant to be equal
 * if the position and elevation are. This can be used to find touch points
 * of two different profiles.
 * </p> 
 * <p>
 * The sort order of the {@link ProfilePoint} is handled only through 
 * its progressive value.
 * </p> 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ProfilePoint implements Comparable<ProfilePoint> {
    private double progressive = doubleNovalue;
    private double elevation = doubleNovalue;
    private Coordinate position;

    public ProfilePoint( double progressive, double elevation, Coordinate position ) {
        this.progressive = progressive;
        this.elevation = elevation;
        this.position = position;
    }

    public ProfilePoint( double progressive, double elevation, double easting, double northing ) {
        this.progressive = progressive;
        this.elevation = elevation;
        this.position = new Coordinate(easting, northing);
    }

    public double getProgressive() {
        return progressive;
    }

    public double getElevation() {
        return elevation;
    }

    public Coordinate getPosition() {
        return position;
    }

    /**
     * Calculates the mean slope of a given set of profilepoints.
     * 
     * @param points the points of the profile.
     * @return the mean slope.
     */
    public static double getMeanSlope( List<ProfilePoint> points ) {
        double meanSlope = 0;

        int num = 0;
        for( int i = 0; i < points.size() - 1; i++ ) {
            ProfilePoint p1 = points.get(i);
            ProfilePoint p2 = points.get(i + 1);

            double dx = p2.progressive - p1.progressive;
            double dy = p2.elevation - p1.elevation;
            double tmpSlope = dy / dx;
            meanSlope = meanSlope + tmpSlope;
            num++;
        }
        meanSlope = meanSlope / num;
        return meanSlope;
    }

    /**
     * Return last visible point data for a profile points list.
     * 
     * <p>For the profile the min and max angles of "sight" are
     * calculated. The min azimuth angle represents the "upper"
     * line of sight, as thoght from the zenith.
     * <p>The max azimuth angle represents the "below the earth" line
     * of sight (think of a viewer looking in direction nadir).
     * <p>The return values are in an array of doubles containing:
     * <ul>
     * <li>[0] min point elev, </li>
     * <li>[1] min point x, </li>
     * <li>[2] min point y, </li>
     * <li>[3] min point progressive, </li>
     * <li>[4] min point azimuth, </li>
     * <li>[5] max point elev, </li>
     * <li>[6] max point x, </li>
     * <li>[7] max point y, </li>
     * <li>[8] max point progressive, </li>
     * <li>[9] max point azimuth </li>
     * </ul>
     * 
     * @param profile the profile to analize.
     * @return the last visible point parameters.
     */
    public static double[] getLastVisiblePointData( List<ProfilePoint> profile ) {
        if (profile.size() < 2) {
            throw new IllegalArgumentException("A profile needs to have at least 2 points.");
        }
        ProfilePoint first = profile.get(0);
        double baseElev = first.getElevation();
        Coordinate baseCoord = new Coordinate(0, 0);

        double minAzimuthAngle = Double.POSITIVE_INFINITY;
        double maxAzimuthAngle = Double.NEGATIVE_INFINITY;
        ProfilePoint minAzimuthPoint = null;
        ProfilePoint maxAzimuthPoint = null;
        for( int i = 1; i < profile.size(); i++ ) {
            ProfilePoint currentPoint = profile.get(i);
            double currentElev = currentPoint.getElevation();
            if (HMConstants.isNovalue(currentElev)) {
                continue;
            }
            currentElev = currentElev - baseElev;
            double currentProg = currentPoint.getProgressive();
            Coordinate currentCoord = new Coordinate(currentProg, currentElev);

            double azimuth = GeometryUtilities.azimuth(baseCoord, currentCoord);
            if (azimuth <= minAzimuthAngle) {
                minAzimuthAngle = azimuth;
                minAzimuthPoint = currentPoint;
            }
            if (azimuth >= maxAzimuthAngle) {
                maxAzimuthAngle = azimuth;
                maxAzimuthPoint = currentPoint;
            }
        }

        if (minAzimuthPoint == null || maxAzimuthPoint == null) {
            return null;
        }

        return new double[]{//
        /*    */minAzimuthPoint.elevation, //
                minAzimuthPoint.position.x, //
                minAzimuthPoint.position.y, //
                minAzimuthPoint.progressive, //
                minAzimuthAngle, //
                maxAzimuthPoint.elevation, //
                maxAzimuthPoint.position.x, //
                maxAzimuthPoint.position.y, //
                maxAzimuthPoint.progressive, //
                maxAzimuthAngle, //
        };

    }

    public int compareTo( ProfilePoint o ) {
        if (NumericsUtilities.dEq(progressive, o.progressive)) {
            return 0;
        } else if (progressive > o.progressive) {
            return 1;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return progressive + ", " + elevation;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(elevation);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((position == null) ? 0 : position.hashCode());
        temp = Double.doubleToLongBits(progressive);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProfilePoint other = (ProfilePoint) obj;

        /*
         * the progressive point is equal if the elevation, x, y are equal.
         * This can be is used to find intersecting profiles.
         */
        Coordinate otherPosition = other.position;
        if (NumericsUtilities.dEq(elevation, other.elevation) && NumericsUtilities.dEq(position.x, otherPosition.x)
                && NumericsUtilities.dEq(position.y, otherPosition.y)) {
            return true;
        } else {
            return false;
        }
    }

}
