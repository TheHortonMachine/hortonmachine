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
package org.hortonmachine.dbs.spatialite;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.datatypes.ESpatialiteGeometryType;

/**
 * Common import and export utilities.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportExportUtils {

    private static final String MULTI = "multi";

    /**
     * Import a shapefile into the database using a temporary virtual table.
     * 
     * @param db the database.
     * @param tableName the name for the new table.
     * @param shpPath the shp to import.
     * @param encoding the encoding. If <code>null</code>, UTF-8 is used.
     * @param srid the epsg code for the file.
     * @param geometryType the geometry type of the file.
     * @throws Exception
     */
    public static void executeShapefileImportQueries( final ASpatialDb db, String tableName, String shpPath, String encoding,
            int srid, ESpatialiteGeometryType geometryType ) throws Exception {
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");

        if (encoding == null || encoding.trim().length() == 0) {
            encoding = "UTF-8";
        }
        String geomType = geometryType.getDescription();
        if (geomType.contains("_")) {
            geomType = geomType.split("_")[0];
        }

        if (shpPath.endsWith(".shp")) {
            shpPath = shpPath.substring(0, shpPath.length() - 4);
        }

        String tmptable = "virtualTmp" + dateFormatter.format(new Date()) + tableName;

        final String sql1 = "CREATE VIRTUAL TABLE " + tmptable + " using virtualshape('" + shpPath + "','" + encoding + "',"
                + srid + ");";
        final String sql2 = "select RegisterVirtualGeometry('" + tmptable + "');";
        final String sql3 = "create table " + tableName + " as select * from " + tmptable + ";";
        String sql4 = "select recovergeometrycolumn('" + tableName + "','Geometry'," + srid + ",'" + geomType + "');";
        final String sql5 = "select CreateSpatialIndex('" + tableName + "','Geometry');";
        final String sql6 = "drop table '" + tmptable + "';";
        final String sql7 = "select updateLayerStatistics('" + tableName + "');";
        db.executeInsertUpdateDeleteSql(sql1);
        db.executeInsertUpdateDeleteSql(sql2);
        db.executeInsertUpdateDeleteSql(sql3);

        int executeInsertUpdateDeleteSql = db.executeInsertUpdateDeleteSql(sql4);
        if (executeInsertUpdateDeleteSql == 0) {
            // try also the multi toggle, since the virtualtable might have choosen a different one
            // than geotools
            if (geomType.contains(MULTI)) {
                geomType = geomType.replaceFirst(MULTI, "");
            } else {
                geomType = MULTI + geomType;
            }
            sql4 = "select recovergeometrycolumn('" + tableName + "','Geometry'," + srid + ",'" + geomType + "');";
            db.executeInsertUpdateDeleteSql(sql4);
        }
        db.executeInsertUpdateDeleteSql(sql5);
        db.executeInsertUpdateDeleteSql(sql6);
        db.executeInsertUpdateDeleteSql(sql7);
    }

    /**
     * Attach a shapefile into the database using a temporary virtual table.
     * 
     * @param db the database.
     * @param tableName the name for the new table.
     * @param shpPath the shp to import.
     * @param encoding the encoding. If <code>null</code>, UTF-8 is used.
     * @param srid the epsg code for the file.
     * @throws Exception
     */
    public static void executeShapefileAttachQueries( final ASpatialDb db, String tableName, String shpPath, String encoding,
            int srid ) throws Exception {
        if (encoding == null || encoding.trim().length() == 0) {
            encoding = "UTF-8";
        }

        if (shpPath.endsWith(".shp")) {
            shpPath = shpPath.substring(0, shpPath.length() - 4);
        }

        final String sql1 = "CREATE VIRTUAL TABLE " + tableName + " using virtualshape('" + shpPath + "','" + encoding + "',"
                + srid + ");";
        final String sql2 = "select RegisterVirtualGeometry('" + tableName + "');";
        db.executeInsertUpdateDeleteSql(sql1);
        db.executeInsertUpdateDeleteSql(sql2);
    }

}
