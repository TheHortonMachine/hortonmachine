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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hortonmachine.gears.io.las.core.liblas.LiblasJNALibrary;
import org.hortonmachine.gears.io.las.core.liblas.LiblasReader;
import org.hortonmachine.gears.io.las.core.liblas.LiblasWrapper;
import org.hortonmachine.gears.io.las.core.liblas.LiblasWriter;
import org.hortonmachine.gears.io.las.core.v_1_0.LasReaderBuffered;
import org.hortonmachine.gears.io.las.core.v_1_0.LasWriterBuffered;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Las superclass.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class Las {
    private static volatile boolean testedLibLoading = false;
    private static volatile boolean isNativeLibAvailable;

    /**
     * Checks of nativ libs are available. 
     * 
     * @return <code>true</code>, if native liblas connection is available. 
     */
    public static boolean supportsNative() {
        if (!testedLibLoading) {
            LiblasJNALibrary wrapper = LiblasWrapper.getWrapper();
            if (wrapper != null) {
                isNativeLibAvailable = true;
            }
            testedLibLoading = true;
        }
        return isNativeLibAvailable;
    }

    public static String[] getLibraryPaths() {
        String path = System.getProperty("java.library.path");
        String[] split = path.trim().split(File.pathSeparator);
        List<String> pathList = new ArrayList<String>();
        for( String pathItem : split ) {
            pathItem = pathItem.trim();
            if (pathItem.length() == 0) {
                continue;
            }
            if (!pathList.contains(pathItem)) {
                pathList.add(pathItem);
            }
        }
        Collections.sort(pathList);
        return pathList.toArray(new String[0]);
    }
    /**
     * Get a las reader.
     * 
     * <p>If available, a native reader is created.
     * 
     * @param lasFile the file to read.
     * @param crs the {@link CoordinateReferenceSystem} or <code>null</code> if the file has one.
     * @return the las reader.
     * @throws Exception if something goes wrong.
     */
    public static ALasReader getReader( File lasFile, CoordinateReferenceSystem crs ) throws Exception {
        if (supportsNative()) {
            return new LiblasReader(lasFile, crs);
        } else {
            return new LasReaderBuffered(lasFile, crs);
        }
    }

    /**
     * Get a las writer.
     * 
     * <p>If available, a native writer is created.
     * 
     * @param lasFile the file to write.
     * @param crs the {@link CoordinateReferenceSystem} to be written in the prj.
     * @return the las writer.
     * @throws Exception if something goes wrong.
     */
    public static ALasWriter getWriter( File lasFile, CoordinateReferenceSystem crs ) throws Exception {
        if (supportsNative()) {
            return new LiblasWriter(lasFile, crs);
        } else {
            return new LasWriterBuffered(lasFile, crs);
        }
    }

}
