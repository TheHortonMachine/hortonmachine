/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.io.las.core;

import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.joda.time.DateTime;

/**
 * Object containing the las record content plus some additional info.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasRecord {
    /**
     * The x coordinate, with scale and offset already applied.
     */
    public double x = Double.NaN;

    /**
     * The y coordinate, with scale and offset already applied.
     */
    public double y = Double.NaN;

    /**
     * The z coordinate, with scale and offset already applied.
     */
    public double z = Double.NaN;

    /**
     * The intensity value.
     */
    public short intensity = -1;

    /**
     * The return number.
     */
    public int returnNumber = -1;

    /**
     * Number of Returns (given pulse).
     */
    public int numberOfReturns = -1;

    /**
     * Classification.
     */
    public int classification = -1;

    /**
     * Gps timestamp
     */
    public double gpsTime = -1;

    /**
     * Color rgb info for styling (defaults to gray).
     */
    public short[] color = new short[]{100, 100, 100};

    /**
     * Information about the ground elevation in the current point position (not contained in record and optional).
     */
    public double groundElevation = Double.NaN;

    /**
     * Density of points around the current point (not contained in record and optional).
     */
    public int pointsDensity = -1;

    public String toString() {
        final String CR = "\n";
        final String TAB = "\t";

        StringBuilder retValue = new StringBuilder();

        retValue.append("Dot ( \n").append(TAB).append("x = ").append(this.x).append(CR).append(TAB).append("y = ")
                .append(this.y).append(CR).append(TAB).append("z = ").append(this.z).append(CR).append(TAB)
                .append("intensity = ").append(this.intensity).append(CR).append(TAB).append("impulse = ")
                .append(this.returnNumber).append(CR).append(TAB).append("impulseNum = ").append(this.numberOfReturns).append(CR)
                .append(TAB).append("classification = ").append(this.classification).append(CR).append(TAB).append("gpsTime = ")
                .append(this.gpsTime).append(CR).append(" )");

        return retValue.toString();
    }

    /**
     * Projected distance between two points.
     * 
     * @param other the other point.
     * @return the 2D distance.
     */
    public double distance( LasRecord other ) {
        double distance = NumericsUtilities.pythagoras(x - other.x, y - other.y);
        return distance;
    }

    /**
     * Distance between two points.
     * 
     * @param other the other point.
     * @return the 3D distance.
     */
    public double distance3D( LasRecord other ) {
        double deltaElev = Math.abs(z - other.z);
        double projectedDistance = NumericsUtilities.pythagoras(x - other.x, y - other.y);
        double distance = NumericsUtilities.pythagoras(projectedDistance, deltaElev);
        return distance;
    }

    public boolean equals( Object obj ) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LasRecord)) {
            return false;
        }
        LasRecord r = (LasRecord) obj;
        double delta = 0.000001;
        boolean check = NumericsUtilities.dEq(x, r.x, delta);
        if (!check) {
            return false;
        }
        check = NumericsUtilities.dEq(y, r.y, delta);
        if (!check) {
            return false;
        }
        check = NumericsUtilities.dEq(z, r.z, delta);
        if (!check) {
            return false;
        }
        check = intensity == r.intensity;
        if (!check) {
            return false;
        }
        check = classification == r.classification;
        if (!check) {
            return false;
        }
        check = returnNumber == r.returnNumber;
        if (!check) {
            return false;
        }
        check = numberOfReturns == r.numberOfReturns;
        if (!check) {
            return false;
        }
        return true;
    }

}
