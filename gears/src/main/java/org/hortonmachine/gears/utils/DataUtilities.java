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
package org.hortonmachine.gears.utils;

/**
 * Utilities to handle data.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class DataUtilities {

    public static final String[] supportedVectors = {"shp"};
    public static final String[] supportedRasters = {"asc", "tif", "tiff"};

    /**
     * Checks a given name of a file if it is a supported vector extension.
     * 
     * @param name the name of the file.
     * @return <code>true</code>, if the extension is supported.
     */
    public static boolean isSupportedVectorExtension( String name ) {
        for( String ext : supportedVectors ) {
            if (name.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks a given name of a file if it is a supported raster extension.
     * 
     * @param name the name of the file.
     * @return <code>true</code>, if the extension is supported.
     */
    public static boolean isSupportedRasterExtension( String name ) {
        for( String ext : supportedRasters ) {
            if (name.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        return false;
    }
}
