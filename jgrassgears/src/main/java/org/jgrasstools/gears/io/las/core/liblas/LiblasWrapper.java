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
package org.jgrasstools.gears.io.las.core.liblas;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;

/**
 * JNA wrapper for the laslib library.
 * 
 * @see LiblasReader
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LiblasWrapper {
    private static LiblasJNALibrary WRAPPER;

    private LiblasWrapper() {
    }

    /**
     * Getter for the liblas library JNA wrapper. 
     * 
     * @return the JNA wrapper.
     * @throws Exception if no library could be loaded.
     */
    public synchronized static LiblasJNALibrary getWrapper() throws Exception {
        if (WRAPPER == null) {
            // try to find it in the default LD_LIBRARY path
            loadNativeLibrary(null, null);
        }
        if (WRAPPER == null) {
            throw new Exception("No native liblas library loaded.");
        }
        return WRAPPER;
    }

    /**
     * Loads the native libs creating the native wrapper.
     * 
     * @param nativeLibPath the path to add or <code>null</code>.
     * @param libName the lib name or <code>null</code>, in which case "lib_c" is used.
     * @return <code>true</code>, if the lib could be loaded.
     */
    public static boolean loadNativeLibrary( String nativeLibPath, String libName ) {
        try {
            String name = "las_c";
            if (libName == null)
                libName = name;
            if (nativeLibPath != null)
                NativeLibrary.addSearchPath(libName, nativeLibPath);
            WRAPPER = (LiblasJNALibrary) Native.loadLibrary(libName, LiblasJNALibrary.class);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
        return true;
    }

}
