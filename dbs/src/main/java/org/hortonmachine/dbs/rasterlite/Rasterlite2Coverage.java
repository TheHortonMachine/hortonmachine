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
package org.hortonmachine.dbs.rasterlite;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Class representing a raster_coverages record.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Rasterlite2Coverage {

    // COLUMN NAMES
    public static final String TABLENAME = "raster_coverages";
    public static final String COVERAGE_NAME = "coverage_name";
    public static final String TITLE = "title";
    public static final String SRID = "srid";
    public static final String COMPRESSION = "compression";
    public static final String EXTENT_MINX = "extent_minx";
    public static final String EXTENT_MINY = "extent_miny";
    public static final String EXTENT_MAXX = "extent_maxx";
    public static final String EXTENT_MAXY = "extent_maxy";

    // VARIABLES
    private String coverageName;
    private String title;
    private int srid;
    private String compression;
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private ASpatialDb database;

    public Rasterlite2Coverage( ASpatialDb database, String coverageName, String title, int srid, String compression, double minX,
            double miny, double maxX, double maxY ) {
        this.database = database;
        this.coverageName = coverageName;
        this.title = title;
        this.srid = srid;
        this.compression = compression;
        this.minX = minX;
        this.minY = miny;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public String getName() {
        return coverageName;
    }

    public String getDatabasePath() {
        return database.getDatabasePath();
    }

    public int getSrid() {
        return srid;
    }

    public Envelope getBounds() {
        return new Envelope(minX, maxX, minY, maxY);
    }

    public String getImageFormat() {
        String imageFormat = null;
        try {
            if (compression.equals("JPEG")) {
                imageFormat = "jpg";
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        if (imageFormat == null) {
            imageFormat = "png";
        }
        return imageFormat;
    }

    /**
     * Extract an image from the database.
     * 
     * @param geom the image bounding box geometry.
     * @param width the pixel width of the expected image.
     * @param height the pixel height of the expected image.
     * @return the image bytes.
     * @throws Exception
     */
    public byte[] getRL2Image( Geometry geom, String geomEpsg, int width, int height ) throws Exception {
        String sql;
        String rasterName = getName();
        if (geomEpsg != null) {
            sql = "select GetMapImageFromRaster('" + rasterName + "', ST_Transform(ST_GeomFromText('" + geom.toText() + "', "
                    + geomEpsg + "), " + srid + ") , " + width + " , " + height
                    + ", 'default', 'image/png', '#ffffff', 0, 80, 1 )";
        } else {
            sql = "select GetMapImageFromRaster('" + rasterName + "', ST_GeomFromText('" + geom.toText() + "') , " + width + " , "
                    + height + ", 'default', 'image/png', '#ffffff', 0, 80, 1 )";
        }

        return database.execOnConnection(mConn -> {
            try (IHMStatement stmt = mConn.createStatement()) {
                IHMResultSet resultSet = stmt.executeQuery(sql);
                if (resultSet.next()) {
                    byte[] bytes = resultSet.getBytes(1);
                    return bytes;
                }
            }
            return null;
        });
    }

    @Override
    public String toString() {
        return "RasterCoverage [\n\tcoverage_name=" + coverageName //
                + ", \n\ttitle=" + title //
                + ", \n\tsrid=" + srid //
                + ", \n\tcompression=" + compression //
                + ", \n\textent_minx=" + minX //
                + ", \n\textent_miny=" + minY //
                + ", \n\textent_maxx=" + maxX //
                + ", \n\textent_maxy=" + maxY //
                + "\n]";
    }

}
