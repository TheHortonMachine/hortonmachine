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
package org.jgrasstools.dbs.spatialite;

import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.compat.objects.QueryResult;
import org.jgrasstools.dbs.utils.DbsUtilities;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

/**
 * Common methods for spatialite (android and java).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SpatialiteCommonMethods {

    public static QueryResult getTableRecordsMapIn( ASpatialDb db, String tableName, Envelope envelope, boolean alsoPK_UID,
            int limit, int reprojectSrid ) throws Exception, ParseException {
        QueryResult queryResult = new QueryResult();

        GeometryColumn gCol = null;
        try {
            gCol = db.getGeometryColumnsForTable(tableName);
            // TODO check if it is a virtual table
        } catch (Exception e) {
            // ignore
        }
        boolean hasGeom = gCol != null;

        List<String[]> tableColumnsInfo = db.getTableColumns(tableName);
        List<String> tableColumns = new ArrayList<>();
        for( String[] info : tableColumnsInfo ) {
            tableColumns.add(info[0]);
        }
        if (hasGeom) {
            if (!tableColumns.remove(gCol.geometryColumnName)) {
                String gColLower = gCol.geometryColumnName.toLowerCase();
                int index = -1;
                for( int i = 0; i < tableColumns.size(); i++ ) {
                    String tableColumn = tableColumns.get(i);
                    if (tableColumn.toLowerCase().equals(gColLower)) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    tableColumns.remove(index);
                }
            }
        }
        if (!alsoPK_UID) {
            if (!tableColumns.remove(ASpatialDb.PK_UID)) {
                tableColumns.remove(ASpatialDb.PKUID);
            }
        }

        String sql = "SELECT ";
        List<String> items = new ArrayList<>();
        for( int i = 0; i < tableColumns.size(); i++ ) {
            items.add(tableColumns.get(i));
        }
        if (hasGeom) {
            if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                items.add(gCol.geometryColumnName);
            } else {
                items.add("ST_Transform(" + gCol.geometryColumnName + "," + reprojectSrid + ") AS " + gCol.geometryColumnName);
            }
        }
        String itemsWithComma = DbsUtilities.joinByComma(items);
        sql += itemsWithComma;
        sql += " FROM " + tableName;
        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE "; //
            sql += db.getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
        try (IJGTStatement stmt = db.getConnection().createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (hasGeom && columnName.equals(gCol.geometryColumnName)) {
                    queryResult.geometryIndex = i - 1;
                }
            }

            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (hasGeom && queryResult.geometryIndex == j - 1) {
                        byte[] geomBytes = rs.getBytes(j);
                        Geometry geometry = wkbReader.read(geomBytes);
                        rec[j - 1] = geometry;
                    } else {
                        Object object = rs.getObject(j);
                        rec[j - 1] = object;
                    }
                }
                queryResult.data.add(rec);
            }
            return queryResult;
        }
    }

    public static List<Geometry> getGeometriesIn( ASpatialDb db, String tableName, Envelope envelope )
            throws Exception, ParseException {
        List<Geometry> geoms = new ArrayList<Geometry>();

        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        String sql = "SELECT " + gCol.geometryColumnName + " FROM " + tableName;

        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE " + db.getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
        try (IJGTStatement stmt = db.getConnection().createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                byte[] geomBytes = rs.getBytes(1);
                Geometry geometry = wkbReader.read(geomBytes);
                geoms.add(geometry);
            }
            return geoms;
        }
    }

    public static List<Geometry> getGeometriesIn( ASpatialDb db, String tableName, Geometry intersectionGeometry )
            throws Exception, ParseException {
        List<Geometry> geoms = new ArrayList<Geometry>();

        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        String sql = "SELECT " + gCol.geometryColumnName + " FROM " + tableName;

        if (intersectionGeometry != null) {
            sql += " WHERE " + db.getSpatialindexGeometryWherePiece(tableName, null, intersectionGeometry);
        }
        SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
        try (IJGTStatement stmt = db.getConnection().createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                byte[] geomBytes = rs.getBytes(1);
                Geometry geometry = wkbReader.read(geomBytes);
                geoms.add(geometry);
            }
            return geoms;
        }
    }

    public static GeometryColumn getGeometryColumnsForTable( IJGTConnection connection, String tableName ) throws Exception {
        String attachedStr = "";
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached
            // database

            // get the database name
            String[] split = tableName.split("\\.");
            attachedStr = split[0] + ".";
            tableName = split[1];
            // logger.debug(MessageFormat.format("Considering attached database:
            // {0}", attachedStr));
        }

        String sql = "select " + SpatialiteGeometryColumns.F_TABLE_NAME + ", " //
                + SpatialiteGeometryColumns.F_GEOMETRY_COLUMN + ", " //
                + SpatialiteGeometryColumns.GEOMETRY_TYPE + "," //
                + SpatialiteGeometryColumns.COORD_DIMENSION + ", " //
                + SpatialiteGeometryColumns.SRID + ", " //
                + SpatialiteGeometryColumns.SPATIAL_INDEX_ENABLED + " from " //
                + attachedStr + SpatialiteGeometryColumns.TABLENAME + " where Lower(" + SpatialiteGeometryColumns.F_TABLE_NAME
                + ")=Lower('" + tableName + "')";
        try (IJGTStatement stmt = connection.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                SpatialiteGeometryColumns gc = new SpatialiteGeometryColumns();
                gc.tableName = rs.getString(1);
                gc.geometryColumnName = rs.getString(2);
                gc.geometryType = rs.getInt(3);
                gc.coordinatesDimension = rs.getInt(4);
                gc.srid = rs.getInt(5);
                gc.isSpatialIndexEnabled = rs.getInt(6);
                return gc;
            }
            return null;
        }
    }

    public static void initSpatialMetadata( ASpatialDb db, String options ) throws Exception {
        if (options == null) {
            options = "";
        }
        db.enableAutocommit(false);
        String sql = "SELECT InitSpatialMetadata(" + options + ")";
        try (IJGTStatement stmt = db.getConnection().createStatement()) {
            stmt.execute(sql);
        }
        db.enableAutocommit(true);
    }

    public static Envelope getTableBounds( ASpatialDb db, String tableName ) throws Exception {
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        String geomFieldName;
        if (gCol != null) {
            geomFieldName = gCol.geometryColumnName;
            String trySql = "SELECT extent_min_x, extent_min_y, extent_max_x, extent_max_y FROM vector_layers_statistics WHERE Lower(table_name)=Lower('"
                    + tableName + "') AND Lower(geometry_column)=Lower('" + geomFieldName + "')";
            try (IJGTStatement stmt = db.getConnection().createStatement(); IJGTResultSet rs = stmt.executeQuery(trySql)) {
                if (rs.next()) {
                    double minX = rs.getDouble(1);
                    double minY = rs.getDouble(2);
                    double maxX = rs.getDouble(3);
                    double maxY = rs.getDouble(4);

                    Envelope env = new Envelope(minX, maxX, minY, maxY);
                    if (env.getWidth() != 0.0 && env.getHeight() != 0.0) {
                        return env;
                    }
                }
            }
        } else {
            // try geometry if virtual table
            geomFieldName = "geometry";
        }

        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT Min(MbrMinX(" + geomFieldName + ")) AS min_x, Min(MbrMinY(" + geomFieldName + ")) AS min_y,"
                + "Max(MbrMaxX(" + geomFieldName + ")) AS max_x, Max(MbrMaxY(" + geomFieldName + ")) AS max_y " + "FROM "
                + tableName;

        try (IJGTStatement stmt = db.getConnection().createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                double minX = rs.getDouble(1);
                double minY = rs.getDouble(2);
                double maxX = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                Envelope env = new Envelope(minX, maxX, minY, maxY);
                return env;
            }
            return null;
        }
    }

    public static String getSpatialindexBBoxWherePiece( ASpatialDb db, String tableName, String alias, double x1, double y1,
            double x2, double y2 ) throws Exception {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached
            // database
            tableName = "DB=" + tableName;
        }

        String sql = "ST_Intersects(" + alias + gCol.geometryColumnName + ", BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2
                + ")) = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "Lower(f_table_name) = Lower('" + tableName + "') AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }

    public static String getSpatialindexGeometryWherePiece( ASpatialDb db, String tableName, String alias, Geometry geometry )
            throws Exception {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }

        Envelope envelope = geometry.getEnvelopeInternal();
        double x1 = envelope.getMinX();
        double x2 = envelope.getMaxX();
        double y1 = envelope.getMinY();
        double y2 = envelope.getMaxY();
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached
            // database
            tableName = "DB=" + tableName;
        }
        String sql = "ST_Intersects(" + alias + gCol.geometryColumnName + ", " + "GeomFromText('" + geometry.toText() + "')"
                + ") = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "Lower(f_table_name) = Lower('" + tableName + "') AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }
}
