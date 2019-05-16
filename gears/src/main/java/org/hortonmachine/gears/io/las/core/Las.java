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

import org.hortonmachine.gears.io.las.core.laszip4j.LaszipReader;
import org.hortonmachine.gears.io.las.core.v_1_0.LasReaderBuffered;
import org.hortonmachine.gears.io.las.core.v_1_0.LasWriterBuffered;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Las superclass.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class Las {
    private static boolean isLaz( String name ) {
        return name.toLowerCase().endsWith(".laz");
    }

    /**
     * Get a las reader.
     * 
     * @param lasFile the file to read.
     * @return the las reader.
     * @throws Exception if something goes wrong.
     */
    public static ALasReader getReader( File lasFile ) throws Exception {
        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(lasFile);
        File prjFile = new File(lasFile.getParentFile(), nameWithoutExtention + ".prj");
        CoordinateReferenceSystem crs = null;
        if (prjFile.exists()) {
            try {
                crs = CrsUtilities.readProjectionFile(prjFile.getAbsolutePath(), null);
            } catch (Exception e) {
                // try to go on without crs.
            }
        }
        return getReader(lasFile, crs);
    }

    /**
     * Get a las reader.
     * 
     * @param lasFile the file to read.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @return the las reader.
     * @throws Exception if something goes wrong.
     */
    public static ALasReader getReader( File lasFile, CoordinateReferenceSystem crs ) throws Exception {
        if (isLaz(lasFile.getName())) {
            return new LaszipReader(lasFile, crs);
        } else {
            return new LasReaderBuffered(lasFile, crs);
        }
    }

    /**
     * Get a las writer.
     * 
     * @param lasFile the file to write.
     * @param crs the {@link CoordinateReferenceSystem} to be written in the prj.
     * @return the las writer.
     * @throws Exception if something goes wrong.
     */
    public static ALasWriter getWriter( File lasFile, CoordinateReferenceSystem crs ) throws Exception {
        return new LasWriterBuffered(lasFile, crs);
    }

}
