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
package org.hortonmachine.gears.io.las.databases;

import org.locationtech.jts.geom.Polygon;

/**
 * A cell of las data.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasCell {
    public long id;
    public long sourceId;
    /**
     * The cell bounds.
     */
    public Polygon polygon;
    
    /**
     * Number of points in the cell.
     */
    public int pointsCount;

    public double avgElev;
    public double minElev;
    public double maxElev;

    /**
     * The byte array containing [x1,y1,z1,x2,y2,z2,...] in amount of pointsCount. 
     */
    public byte[] xyzs;

    public short avgIntensity;
    public short minIntensity;
    public short maxIntensity;

    /**
     * The array containing [intensities1, classification1, ...] in amount of pointsCount. 
     */
    public byte[] intensitiesClassifications;

    /**
     * The array containing [returnNumber1,numberOfReturns1,returnNumber2,numberOfReturns2,...]  in amount of pointsCount. 
     */
    public byte[] returns;

    public double minGpsTime;
    public double maxGpsTime;

    /**
     * The array containing [gpsTime1, gpsTime2, ...] in amount of pointsCount. 
     */
    public byte[] gpsTimes;

    /**
     * The array containing [r1, g1, b1, r2, g2, b2, ...]  rgb info for stylings.
     */
    public byte[] colors;

}
