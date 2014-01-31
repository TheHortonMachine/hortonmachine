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

import java.io.File;
import java.io.IOException;

import org.jgrasstools.gears.io.las.core.liblas.LiblasJNALibrary;
import org.jgrasstools.gears.io.las.core.liblas.LiblasWrapper;
import org.jgrasstools.gears.io.las.core.liblas.LiblasWriter;
import org.jgrasstools.gears.io.las.core.v_1_0.LasWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public abstract class ALasWriter {
    
    private static volatile boolean testedLibLoading = false;
    private static volatile boolean isNativeLibAvailable;

    public static ALasWriter getWriter( File lasFile, CoordinateReferenceSystem crs ) throws Exception {
        ALasWriter writer;
        if (!testedLibLoading) {
            LiblasJNALibrary wrapper = LiblasWrapper.getWrapper();
            if (wrapper != null) {
                isNativeLibAvailable = true;
            }
            testedLibLoading = true;
        }
        if (isNativeLibAvailable) {
            writer = new LiblasWriter(lasFile, crs);
        } else {
            writer = new LasWriter(lasFile, crs);
        }
        return writer;
    }

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

}