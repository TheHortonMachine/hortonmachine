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
package org.hortonmachine.gears.utils.crs;

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
import org.hortonmachine.dbs.utils.CrsId;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.ReferenceIdentifier;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.datum.Datum;
import org.geotools.api.referencing.operation.MathTransform;

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
     * Get the authority-qualified code string (e.g. {@code "EPSG:4326"} or {@code "ESRI:102700"})
     * from a {@link CoordinateReferenceSystem}.
     *
     * @param crs the crs to get the code from.
     * @return the authority-qualified code, usable with {@link CRS#decode(String)}.
     * @throws Exception
     */
    public static String getCodeFromCrs( CoordinateReferenceSystem crs ) throws Exception {
        Integer epsg = getSrid(crs);
        if (epsg != null) {
            return CrsId.ofEpsg(epsg).toAuthorityCode();
        }
        return CRS.lookupIdentifier(crs, true);
    }

    /**
     * Returns the EPSG integer code for the given CRS, or {@code null} if it is not an EPSG CRS.
     */
    public static Integer getSrid( CoordinateReferenceSystem crs ) throws FactoryException {
        try {
            return CRS.lookupEpsgCode(crs, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the {@link CoordinateReferenceSystem} from a bare EPSG integer code.
     *
     * @param srid the EPSG integer code.
     * @return the crs or null.
     */
    public static CoordinateReferenceSystem getCrsFromSrid( int srid ) {
        if (srid == WGS84_SRID) {
            return WGS84;
        }
        try {
            return HMCrsRegistry.INSTANCE.getCrs(CrsId.ofEpsg(srid).toAuthorityCode());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the {@link CoordinateReferenceSystem} from a bare EPSG integer code with explicit axis order.
     *
     * @param srid            the EPSG integer code.
     * @param doLatitudeFirst {@code true} to put latitude first, {@code false} for longitude first,
     *                        {@code null} for the default axis order.
     * @return the crs or null.
     * @deprecated Use {@link HMCrsRegistry#getCrs(String, boolean)} directly with
     *             {@code longitudeFirst = !doLatitudeFirst}.
     */
    @Deprecated
    public static CoordinateReferenceSystem getCrsFromSrid( int srid, Boolean doLatitudeFirst ) {
        if (srid == WGS84_SRID && doLatitudeFirst == null) {
            return WGS84;
        }
        try {
            if (doLatitudeFirst == null) {
                return HMCrsRegistry.INSTANCE.getCrs(CrsId.ofEpsg(srid).toAuthorityCode());
            }
            return HMCrsRegistry.INSTANCE.getCrs(CrsId.ofEpsg(srid).toAuthorityCode(), !doLatitudeFirst);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the {@link CoordinateReferenceSystem} from an authority-qualified code string such as
     * {@code "EPSG:4326"} or {@code "ESRI:102700"}. Bare integers default to EPSG.
     *
     * @param crsCode the authority-qualified code (or bare integer).
     * @return the crs or null.
     * @deprecated Use {@link HMCrsRegistry#getCrs(String)} directly.
     */
    @Deprecated
    public static CoordinateReferenceSystem getCrsFromCode( String crsCode ) {
        try {
            return HMCrsRegistry.INSTANCE.getCrs(crsCode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the {@link CoordinateReferenceSystem} from an authority-qualified code string with explicit
     * axis order.
     *
     * @param crsCode         the authority-qualified code (or bare integer).
     * @param doLatitudeFirst {@code true} to put latitude first, {@code false} for longitude first,
     *                        {@code null} for the default axis order.
     * @return the crs or null.
     * @deprecated Use {@link HMCrsRegistry#getCrs(String, boolean)} directly with
     *             {@code longitudeFirst = !doLatitudeFirst}.
     */
    @Deprecated
    public static CoordinateReferenceSystem getCrsFromCode( String crsCode, Boolean doLatitudeFirst ) {
        try {
            if (doLatitudeFirst == null) {
                return HMCrsRegistry.INSTANCE.getCrs(crsCode);
            }
            return HMCrsRegistry.INSTANCE.getCrs(crsCode, !doLatitudeFirst);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the {@link CoordinateReferenceSystem} from an EPSG code string such as {@code "EPSG:4326"}.
     *
     * @param doLatitudeFirst {@code true} to put latitude first, {@code false} for longitude first,
     *                        {@code null} for the default axis order.
     * @deprecated Use {@link HMCrsRegistry#getCrs(String, boolean)} directly with
     *             {@code longitudeFirst = !doLatitudeFirst}.
     */
    @Deprecated
    public static CoordinateReferenceSystem getCrsFromEpsg( String epsgPlusCode, Boolean doLatitudeFirst ) {
        return getCrsFromCode(epsgPlusCode, doLatitudeFirst);
    }

    /** @deprecated Use {@link HMCrsRegistry#getCrs(String)} directly. */
    @Deprecated
    public static CoordinateReferenceSystem getCrsFromEpsg( String epsgPlusCode ) {
        return getCrsFromCode(epsgPlusCode);
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
