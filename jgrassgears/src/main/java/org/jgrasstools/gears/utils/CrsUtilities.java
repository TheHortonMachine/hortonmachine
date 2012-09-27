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
package org.jgrasstools.gears.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Utilities for CRS.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
public class CrsUtilities {
    /**
     * Fill the prj file with the actual map projection.
     * 
     * @param filePath the path to the regarding data or prj file.
     * @param extention the extention of the data file. If <code>null</code>, the crs is written to filePath directly.
     * @param crs the {@link CoordinateReferenceSystem} to write.
     * @throws IOException
     */
    @SuppressWarnings("nls")
    public static void writeProjectionFile( String filePath, String extention, CoordinateReferenceSystem crs ) throws IOException {
        /*
         * fill a prj file
         */
        String prjPath = null;
        if (extention != null && filePath.toLowerCase().endsWith("." + extention)) {
            int dotLoc = filePath.lastIndexOf(".");
            prjPath = filePath.substring(0, dotLoc);
            prjPath = prjPath + ".prj";
        } else {
            prjPath = filePath + ".prj";
        }

        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(prjPath));
            bufferedWriter.write(crs.toWKT());
        } finally {
            if (bufferedWriter != null)
                bufferedWriter.close();
        }
    }

    /**
     * Reads a {@link CoordinateReferenceSystem} from a prj file.
     * 
     * @param filePath the path to the regarding data or prj file.
     * @param extention the extention of the data file. If <code>null</code>, the crs is written to filePath directly.
     * @return the read {@link CoordinateReferenceSystem}. 
     * @throws Exception
     */
    @SuppressWarnings("nls")
    public static CoordinateReferenceSystem readProjectionFile( String filePath, String extention ) throws Exception {
        CoordinateReferenceSystem crs = null;
        /*
         * fill a prj file
         */
        String prjPath = null;
        if (extention != null && filePath.toLowerCase().endsWith("." + extention)) {
            int dotLoc = filePath.lastIndexOf(".");
            prjPath = filePath.substring(0, dotLoc);
            prjPath = prjPath + ".prj";
        } else {
            prjPath = filePath + ".prj";
        }

        File prjFile = new File(prjPath);
        if (!prjFile.exists()) {
            throw new ModelsIOException("The prj file doesn't exist: " + prjPath, "CRSUTILITIES");
        }
        try {
            String wkt = FileUtilities.readFile(prjFile);
            crs = CRS.parseWKT(wkt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return crs;
    }

    /**
     * Reproject a set of geometries
     * 
     * @param from the starting crs
     * @param to the destination crs
     * @param geometries the array of geometries, wrapped into an Object array
     * @throws Exception
     */
    public static void reproject( CoordinateReferenceSystem from, CoordinateReferenceSystem to, Object[] geometries )
            throws Exception {
        MathTransform mathTransform = CRS.findMathTransform(from, to);

        for( int i = 0; i < geometries.length; i++ ) {
            geometries[i] = JTS.transform((Geometry) geometries[i], mathTransform);
        }
    }

    /**
     * Reproject a set of coordinates.
     * 
     * @param from the starting crs
     * @param to the destination crs
     * @param coordinates the array of coordinates, wrapped into an Object array
     * @throws Exception
     */
    public static void reproject( CoordinateReferenceSystem from, CoordinateReferenceSystem to, Coordinate[] coordinates )
            throws Exception {
        MathTransform mathTransform = CRS.findMathTransform(from, to);

        for( int i = 0; i < coordinates.length; i++ ) {
            coordinates[i] = JTS.transform(coordinates[i], coordinates[i], mathTransform);
        }
    }

    /**
     * Get the code from a {@link CoordinateReferenceSystem}.
     * 
     * @param crs the crs to get the code from.
     * @return the code, that can be used with {@link CRS#decode(String)}
     *              to recreate the crs.
     * @throws Exception
     */
    public static String getCodeFromCrs( CoordinateReferenceSystem crs ) throws Exception {
        String code = null;
        try {
            Integer epsg = CRS.lookupEpsgCode(crs, true);
            code = "EPSG:" + epsg; //$NON-NLS-1$
        } catch (Exception e) {
            // try non epsg
            code = CRS.lookupIdentifier(crs, true);
        }
        return code;
    }

}
