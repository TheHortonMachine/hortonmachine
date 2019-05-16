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
package org.hortonmachine.gears.io.las.core;


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
    public short returnNumber = -1;

    /**
     * Number of Returns (given pulse).
     */
    public short numberOfReturns = -1;

    /**
     * Classification.
     */
    public byte classification = -1;

    /**
     * Gps timestamp
     */
    public double gpsTime = 0;

    /**
     * Color rgb info for styling (defaults to black).
     */
    public short[] color = new short[]{0, 0, 0};

    /**
     * Information about the ground elevation in the current point position (not contained in record and optional).
     */
    public double groundElevation = Double.NaN;

    /**
     * Density of points around the current point (not contained in record and optional).
     */
    public int pointsDensity = -1;

}
