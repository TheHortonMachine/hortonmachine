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
package org.hortonmachine.gui.console;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Utility to help filter out messages from the console.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ConsoleMessageFilter {

    private static List<String> containsStrings;
    private static List<String> endStrings;
    static {
        containsStrings = new ArrayList<String>();
        containsStrings.add("Kakadu");
        containsStrings.add("no gdaljni in java.library.path");
        containsStrings.add("Error while parsing JAI registry");
        containsStrings.add("Call MapContent dispose() to prevent memory leaks");
        containsStrings.add("org.geotools.map.MapContent finalize");
        containsStrings.add("A descriptor is already registered");
        containsStrings.add("Error in registry file");
        containsStrings.add("Logging initialized");
        containsStrings.add("log4j:WARN");
        containsStrings.add("Error: Could not find mediaLib");
        containsStrings.add("SLF4J: ");
        containsStrings.add("[main] INFO ");
        containsStrings.add(" INFO [main] ");
        containsStrings.add("Occurs in: com.sun.media.jai.mlib.MediaLibAccessor");
        containsStrings.add("java.lang.NoClassDefFoundError: com/sun/medialib/mlib/Image");
        containsStrings.add("Caused by: java.lang.ClassNotFoundException: com.sun.medialib.mlib.Image");
        containsStrings.add("Failed to load the GDAL native libs");//. This is not a problem unless you need to use the GDAL plugins: they won't be enabled.\n" + 
        containsStrings.add("Unable to set ordering between tiff readers spi");
        containsStrings.add("org.geotools.image.ImageWorker <clinit>");
        containsStrings.add("INFO: Warp/affine reduction enabled");
        containsStrings.add("org.geotools.gce.imagemosaic.Utils loadMosaicProperties");
        containsStrings.add("INFO: properties file doesn't exist");
        containsStrings.add("org.geotools.resources.coverage.CoverageUtilities loadPropertiesFromURL");
        containsStrings.add("replaceTIFF");
        containsStrings.add("WARNING: This granule catalog was not properly dispose as it still points to:ServiceInfo");
         
        // "\tat ", //
        // "\t... ", //

        endStrings = new ArrayList<String>();
        endStrings.add("AbstractGTDataStoreGranuleCatalog finalize");
        endStrings.add("description=Features from ShapefileDataStore");
        endStrings.add("factory.epsg.ThreadedEpsgFactory <init>");
        endStrings.add("to a 1800000ms timeout");
        endStrings.add("Native library load failed.");
        endStrings.add("gdalframework.GDALUtilities loadGDAL");
        endStrings.add("org.gdal.gdal.gdalJNI.HasThreadSupport()I");
        endStrings.add("org.gdal.gdal.gdalJNI.VersionInfo__SWIG_0(Ljava/lang/String;)Ljava/lang/String;");
        endStrings.add(".properties (No such file or directory)");

    }

    public static boolean doRemove( final String line ) {
        try {
            Stream<String> endsStream = endStrings.parallelStream();
            boolean isPresent = endsStream.filter(string -> line.endsWith(string)).findFirst().isPresent();
            if (isPresent) {
                return true;
            }
            Stream<String> containsStream = containsStrings.parallelStream();
            Optional<String> findFirst = containsStream.filter(string -> line.contains(string)).findFirst();
            isPresent = findFirst.isPresent();
            if (isPresent) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
