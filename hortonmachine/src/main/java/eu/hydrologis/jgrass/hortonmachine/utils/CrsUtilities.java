/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.jgrass.hortonmachine.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;

public class CrsUtilities {
    /**
     * Fill the prj file with the actual map projection.
     * 
     * @param shapePath the path to the regarding shapefile
     * @throws IOException 
     */
    public static void writeProjectionFile( String shapePath, CoordinateReferenceSystem crs ) throws IOException {
        /*
         * fill a prj file
         */
        String prjPath = null;
        if (shapePath.toLowerCase().endsWith(".shp")) {
            int dotLoc = shapePath.lastIndexOf(".");
            prjPath = shapePath.substring(0, dotLoc);
            prjPath = prjPath + ".prj";
        } else {

            prjPath = shapePath + ".prj";
        }

        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(prjPath));
            bufferedWriter.write(crs.toWKT());
        } finally {
            bufferedWriter.close();
        }

    }

    /**
     * Reproject a geometry
     * 
     * @param from the starting crs
     * @param to the destination crs
     * @param geometries the array of geometries, wrapped into an Object array
     * @throws Exception
     */
    public static void reproject( CoordinateReferenceSystem from, CoordinateReferenceSystem to, Object[] geometries ) throws Exception {
        MathTransform mathTransform = CRS.findMathTransform(from, to);

        for( int i = 0; i < geometries.length; i++ ) {
            geometries[i] = JTS.transform((Geometry) geometries[i], mathTransform);
        }
    }

}
