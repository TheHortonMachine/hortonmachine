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

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.AbstractSingleCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Utilities for CRS.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
public class CrsUtilities {

    public static final CoordinateReferenceSystem WGS84 = DefaultGeographicCRS.WGS84;
    public static final int WGS84_SRID = 4326;

    public static final ReferencedEnvelope WORLD = new ReferencedEnvelope(-180, 180, -90, 90, WGS84);

    /**
     * Checks if a crs is valid, i.e. if it is not a wildcard default one.
     * 
     * @param crs the crs to check.
     */
    public static boolean isCrsValid( CoordinateReferenceSystem crs ) {
        if (crs instanceof AbstractSingleCRS) {
            AbstractSingleCRS aCrs = (AbstractSingleCRS) crs;
            Datum datum = aCrs.getDatum();
            ReferenceIdentifier name = datum.getName();
            String code = name.getCode();
            if (code.equalsIgnoreCase("Unknown")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Fill the prj file with the actual map projection.
     *
     * @param filePath  the path to the regarding data or prj file.
     * @param extention the extention of the data file. If <code>null</code>, the crs is written to filePath directly.
     * @param crs       the {@link CoordinateReferenceSystem} to write.
     *
     * @throws IOException
     */
    @SuppressWarnings("nls")
    public static void writeProjectionFile( String filePath, String extention, CoordinateReferenceSystem crs )
            throws IOException {
        /*
         * fill a prj file
         */
        String prjPath = null;
        if (extention != null && filePath.toLowerCase().endsWith("." + extention)) {
            int dotLoc = filePath.lastIndexOf(".");
            prjPath = filePath.substring(0, dotLoc);
            prjPath = prjPath + ".prj";
        } else {
            if (!filePath.endsWith(".prj")) {
                prjPath = filePath + ".prj";
            } else {
                prjPath = filePath;
            }
        }

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(prjPath))) {
            bufferedWriter.write(crs.toWKT());
        }
    }

    /**
     * Reads a {@link CoordinateReferenceSystem} from a prj file.
     *
     * @param filePath  the path to the prj file or the connected datafile it sidecar file for.
     * @param extension the extension of the data file. If <code>null</code> it is assumed to be prj.
     *
     * @return the read {@link CoordinateReferenceSystem}.
     *
     * @throws Exception
     */
    @SuppressWarnings("nls")
    public static CoordinateReferenceSystem readProjectionFile( String filePath, String extension ) throws Exception {
        CoordinateReferenceSystem crs = null;
        String prjPath = null;
        String filePathLower = filePath.trim().toLowerCase();
        if (filePathLower.endsWith(".prj")) {
            // it is the prj file
            prjPath = filePath;
        } else if (extension != null && filePathLower.endsWith("." + extension)) {
            // datafile was supplied (substitute extension)
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
        String wkt = FileUtilities.readFile(prjFile);
        crs = CRS.parseWKT(wkt);
        return crs;
    }

    /**
     * Reproject a set of geometries
     *
     * @param from       the starting crs
     * @param to         the destination crs
     * @param geometries the array of geometries, wrapped into an Object array
     *
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
     * @param from        the starting crs
     * @param to          the destination crs
     * @param coordinates the array of coordinates, wrapped into an Object array
     *
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
     *
     * @return the code, that can be used with {@link CRS#decode(String)} to recreate the crs.
     *
     * @throws Exception
     */
    public static String getCodeFromCrs( CoordinateReferenceSystem crs ) throws Exception {
        String code = null;
        try {
            Integer epsg = getSrid(crs);
            code = "EPSG:" + epsg; //$NON-NLS-1$
        } catch (Exception e) {
            // try non epsg
            code = CRS.lookupIdentifier(crs, true);
        }
        return code;
    }

    public static int getSrid( CoordinateReferenceSystem crs ) throws FactoryException {
        Integer epsg = CRS.lookupEpsgCode(crs, true);
        return epsg;
    }

    /**
     * Get the {@link CoordinateReferenceSystem} from a epsg srid number. 
     * 
     * @param srid the srid number.
     * @return the crs or null.
     */
    public static CoordinateReferenceSystem getCrsFromSrid( int srid ) {
        return getCrsFromSrid(srid, null);
    }

    public static CoordinateReferenceSystem getCrsFromSrid( int srid, Boolean doLatitudeFirst ) {
        if (srid == 4326 && doLatitudeFirst == null) {
            return WGS84;
        }
        try {
            if (doLatitudeFirst == null) {
                return CRS.decode("EPSG:" + srid);
            } else {
                return CRS.decode("EPSG:" + srid, doLatitudeFirst);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the {@link CoordinateReferenceSystem} from an epsg definition. 
     * 
     * @param epsgPlusCode the code as EPSG:4326
     * @param doLatitudeFirst see {@link CRS#decode(String, boolean)}
     * @return the crs.
     */
    public static CoordinateReferenceSystem getCrsFromEpsg( String epsgPlusCode, Boolean doLatitudeFirst ) {
        String sridString = epsgPlusCode.replaceFirst("EPSG:", "").replaceFirst("epsg:", "");
        int srid = Integer.parseInt(sridString);
        return getCrsFromSrid(srid, doLatitudeFirst);
    }

    public static CoordinateReferenceSystem getCrsFromEpsg( String epsgPlusCode ) {
        return getCrsFromEpsg(epsgPlusCode, null);
    }

    /**
     * Converts meters to degrees, based on a given coordinate in 90 degrees direction.
     * 
     * @param meters the meters to convert.
     * @param c the position to consider.
     * @return the converted degrees.
     */
    public static double getMetersAsWGS84( double meters, Coordinate c ) {
        GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
        gc.setStartingGeographicPoint(c.x, c.y);
        gc.setDirection(90, meters);
        Point2D destinationGeographicPoint = gc.getDestinationGeographicPoint();
        double degrees = Math.abs(destinationGeographicPoint.getX() - c.x);
        return degrees;
    }

}
