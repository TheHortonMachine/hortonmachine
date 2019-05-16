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

import java.io.File;
import java.io.IOException;

/**
 * Interface for las readers.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class ALasReader extends Las implements AutoCloseable{

    /**
     * Open the las data source.
     * 
     * @throws Exception
     */
    public abstract void open() throws Exception;

    /**
     * Close the las data source and free resources. 
     * 
     * @throws Exception
     */
    public abstract void close() throws Exception;

    /**
     * Get a reference to the las file.
     * 
     * @return the las file. 
     */
    public abstract File getLasFile();

    /**
     * Get the las header.
     * 
     * @return the las header.
     */
    public abstract ILasHeader getHeader();

    /**
     * Check if there are still data available.
     * 
     * @return <code>true</code> if there are still data to read.
     */
    public abstract boolean hasNextPoint() throws IOException;

    /**
     * Read the next record into a {@link LasRecord} object.
     * 
     * @return the read object or <code>null</code> if none available.
     * @throws IOException 
     */
    public abstract LasRecord getNextPoint() throws IOException;

    /**
     * Rewind the iterator to start from scratch.
     * 
     * @throws IOException
     */
    public abstract void rewind() throws IOException;

    /**
     * Reads a dot at a given address.
     * 
     * @param address the file address of the record to read.
     * @return the read record.
     * @throws IOException
     */
    public abstract LasRecord getPointAtAddress( long address ) throws IOException;

    /**
     * Reads a dot at a given point position.
     * 
     * @param pointPosition the point position.
     * @return the read record.
     * @throws IOException
     */
    public abstract LasRecord getPointAt( long pointPosition ) throws IOException;

    /**
     * Reads the position and the record address in the file of the next point.
     * 
     * @return the array containing [x, y, z, address].
     * @throws IOException
     */
    public abstract double[] readNextLasXYZAddress() throws IOException;

    /**
     * Move to a given point in the file.
     * 
     * <p>The position starts with 0 at the first point position.</p>
     * <p>Note that not {@link #hasNextPoint()} can be called after seek,
     * {@link #getNextPoint()} must be called directly instead.</p> 
     * 
     * @param pointNumber the number of records to skip.
     * @throws IOException
     */
    public abstract void seek( long pointNumber ) throws IOException;

    /**
     * Overrides the time type.
     * 
     * <p>The time type is sometimes found set wrong by certain softwares.
     * This gives the possibility to override it and allow for proper time
     * conversion.
     * 
     * @param type the type to set.
     */
    public abstract void setOverrideGpsTimeType( int type );

}