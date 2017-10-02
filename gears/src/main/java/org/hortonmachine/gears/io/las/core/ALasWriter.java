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

import java.io.IOException;

public abstract class ALasWriter extends Las implements AutoCloseable {

    /**
     * Possibility to define the scale for the data.
     * 
     * <p>If not set it defaults to 0.01,0.01,0.001.</p>
     * 
     * @param xScale the x scaling value.
     * @param yScale the y scaling value.
     * @param zScale the z scaling value.
     */
    public abstract void setScales( double xScale, double yScale, double zScale );

    /**
     * Possibility to define the offset for the data.
     * 
     * <p>If not set it defaults to 0.0,0.0,0.0.</p>
     * 
     * @param xOffset the x scaling value.
     * @param yOffset the y scaling value.
     * @param zOffset the z scaling value.
     */
    public abstract void setOffset( double xOffset, double yOffset, double zOffset );

    /**
     * Possibility to set the min and max bounds.
     * 
     * <p>If not set they all default to 0.</p>
     * 
     * @param xMin
     * @param xMax
     * @param yMin
     * @param yMax
     * @param zMin
     * @param zMax
     */
    public abstract void setBounds( double xMin, double xMax, double yMin, double yMax, double zMin, double zMax );

    /**
     * Possibility to set the min and max bounds.
     * 
     * @param header the las header (as read by the reader).
     */
    public abstract void setBounds( ILasHeader header );

    /**
     * Opens the file and write the header info.
     * 
     * @throws Exception
     */
    public abstract void open() throws Exception;

    /**
     * Writes a point to file.
     * 
     * @param record the point record.
     * @throws IOException
     */
    public abstract void addPoint( LasRecord record ) throws IOException;

    /**
     * Close the writer and release resources.
     * 
     * @throws Exception
     */
    public abstract void close() throws Exception;

    public abstract void setWriteGroundElevation( boolean doWriteGroundElevation );

    public abstract void setPointFormat( int pointFormat );
    
    /**
     * Defines whether GPS Week Time (0) or Adjusted Standard GPS
     * time (1) is used on the GPSTime field of LAS records.
     *  
     * @param timeType 0 to set GPS Week Time or 1 to set Adjusted Standard
     * GPS time.
     */
    public abstract void setGpsTimeType(int timeType);

}