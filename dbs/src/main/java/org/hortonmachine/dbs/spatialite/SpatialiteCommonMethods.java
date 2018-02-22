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

import java.io.File;
import java.io.FileInputStream;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.utils.DbsUtilities;

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

    public static boolean isSqliteFile( File file ) throws Exception {
        /*
         * https://www.sqlite.org/fileformat.html
         * 
         * 53 51 4c 69 74 65 20 66 6f 72 6d 61 74 20 33 00
         */
        String hexHeader = "53514c69746520666f726d6174203300";
        byte[] headerBytes = DbsUtilities.hexStringToByteArray(hexHeader);
        try (FileInputStream fis = new FileInputStream(file)) {
            for( int i = 0; i < 16; i++ ) {
                int read = fis.read();
                if (headerBytes[i] != read) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void main( String[] args ) throws Exception {
        System.out.println(isSqliteFile(new File("/tmp/jgt-dbs-testdbsmain.sqlite")));
    }

    /**
     * Check for compatibility issues with other databases.
     * 
     * @param sql the original sql.
     * @return the fixed sql.
     */
    public static String checkCompatibilityIssues( String sql ) {
        sql = sql.replaceAll("AUTO_INCREMENT", "AUTOINCREMENT");
        sql = sql.replaceAll("LONG PRIMARY KEY AUTOINCREMENT", "INTEGER PRIMARY KEY AUTOINCREMENT");
        return sql;
    }

    public static QueryResult getTableRecordsMapIn( ASpatialDb db, String tableName, Envelope envelope, boolean alsoPK_UID,
            int limit, int reprojectSrid, String whereStr ) throws Exception, ParseException {
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
            tableColumns.add(info[0].toLowerCase());
        }
        if (hasGeom) {
            String gColLower = gCol.geometryColumnName.toLowerCase();
            if (!tableColumns.remove(gColLower)) {
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
            String gColLower = gCol.geometryColumnName.toLowerCase();
            if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                items.add(gColLower);
            } else {
                items.add("ST_Transform(" + gColLower + "," + reprojectSrid + ") AS " + gColLower);
            }
        }
        String itemsWithComma = DbsUtilities.joinByComma(items);
        sql += itemsWithComma;
        sql += " FROM " + tableName;

        List<String> whereStrings = new ArrayList<>();
        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            whereStrings.add(db.getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2));
        }
        if (whereStr != null) {
            whereStrings.add(whereStr);
        }

        if (whereStrings.size() > 0) {
            sql += " WHERE "; //
            sql += DbsUtilities.joinBySeparator(whereStrings, " AND ");
        }

        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
        try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            IHMResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (hasGeom && columnName.toLowerCase().equals(gCol.geometryColumnName.toLowerCase())) {
                    queryResult.geometryIndex = i - 1;
                }
            }

            long start = System.currentTimeMillis();
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (hasGeom && queryResult.geometryIndex == j - 1) {
                        byte[] geomBytes = rs.getBytes(j);
                        if (geomBytes != null) {
                            Geometry geometry = wkbReader.read(geomBytes);
                            rec[j - 1] = geometry;
                        }
                    } else {
                        Object object = rs.getObject(j);
                        if (object instanceof Clob) {
                            object = rs.getString(j);
                        }
                        rec[j - 1] = object;
                    }
                }
                queryResult.data.add(rec);
            }
            long end = System.currentTimeMillis();
            queryResult.queryTimeMillis = end - start;
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
        try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
        try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                byte[] geomBytes = rs.getBytes(1);
                Geometry geometry = wkbReader.read(geomBytes);
                geoms.add(geometry);
            }
            return geoms;
        }
    }

    public static GeometryColumn getGeometryColumnsForTable( IHMConnection connection, String tableName ) throws Exception {
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
        try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
        }

        // check in virtual tables
        sql = "select " + SpatialiteGeometryColumns.VIRT_F_TABLE_NAME + ", " //
                + SpatialiteGeometryColumns.VIRT_F_GEOMETRY_COLUMN + ", " //
                + SpatialiteGeometryColumns.VIRT_GEOMETRY_TYPE + "," //
                + SpatialiteGeometryColumns.VIRT_COORD_DIMENSION + ", " //
                + SpatialiteGeometryColumns.VIRT_SRID + " from " //
                + attachedStr + SpatialiteGeometryColumns.VIRT_TABLENAME + " where Lower("
                + SpatialiteGeometryColumns.VIRT_F_TABLE_NAME + ")=Lower('" + tableName + "')";
        try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                SpatialiteGeometryColumns gc = new SpatialiteGeometryColumns();
                gc.tableName = rs.getString(1);
                gc.geometryColumnName = rs.getString(2);
                gc.geometryType = rs.getInt(3);
                gc.coordinatesDimension = rs.getInt(4);
                gc.srid = rs.getInt(5);
                gc.isSpatialIndexEnabled = 0;
                return gc;
            }
        }

        return null;
    }

    public static void initSpatialMetadata( ASpatialDb db, String options ) throws Exception {
        if (options == null) {
            options = "";
        }
        db.enableAutocommit(false);
        String sql = "SELECT InitSpatialMetadata(" + options + ")";
        try (IHMStatement stmt = db.getConnection().createStatement()) {
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
            try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs = stmt.executeQuery(trySql)) {
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

        try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
                + "f_table_name = '" + tableName + "' AND " //
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
        String sql = "ST_Intersects(" + alias + gCol.geometryColumnName + ", " + "ST_GeomFromText('" + geometry.toText() + "')"
                + ") = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "f_table_name = '" + tableName + "' AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }

    /**
     * Adds a geometry column to a table.
     * 
     * @param tableName
     *            the table name.
     * @param geomColName
     *            the geometry column name.
     * @param geomType
     *            the geometry type (ex. LINESTRING);
     * @param epsg
     *            the optional epsg code (default is 4326);
     * @param avoidIndex if <code>true</code>, the index is not created.
     * @throws Exception
     */
    public static void addGeometryXYColumnAndIndex( ASpatialDb db, String tableName, String geomColName, String geomType,
            String epsg, boolean avoidIndex ) throws Exception {
        String epsgStr = "4326";
        if (epsg != null) {
            epsgStr = epsg;
        }
        String geomTypeStr = "LINESTRING";
        if (geomType != null) {
            geomTypeStr = geomType;
        }

        if (geomColName == null) {
            geomColName = ASpatialDb.DEFAULT_GEOM_FIELD_NAME;
        }

        try (IHMStatement stmt = db.getConnection().createStatement()) {
            String sql = "SELECT AddGeometryColumn('" + tableName + "','" + geomColName + "', " + epsgStr + ", '" + geomTypeStr
                    + "', 'XY')";
            stmt.execute(sql);

            if (!avoidIndex) {
                sql = "SELECT CreateSpatialIndex('" + tableName + "', '" + geomColName + "');";
                stmt.execute(sql);
            }
        }
    }

    public static void createSpatialTable( ASpatialDb db, String tableName, int tableSrid, String geometryFieldData,
            String[] fieldData, String[] foreignKeys, boolean avoidIndex ) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(tableName).append("(");
        for( int i = 0; i < fieldData.length; i++ ) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(fieldData[i]);
        }
        if (foreignKeys != null) {
            for( int i = 0; i < foreignKeys.length; i++ ) {
                sb.append(",");
                sb.append(foreignKeys[i]);
            }
        }
        sb.append(")");

        String sql = sb.toString();
        sql = checkCompatibilityIssues(sql);

        try (IHMStatement stmt = db.getConnection().createStatement()) {
            stmt.execute(sql);
        }

        String[] split = geometryFieldData.trim().split("\\s+");
        String geomColName = split[0];
        String type = split[1];
        addGeometryXYColumnAndIndex(db, tableName, geomColName, type, String.valueOf(tableSrid), avoidIndex);
    }

    public static String getGeojsonIn( ASpatialDb db, String tableName, String[] fields, String wherePiece, Integer precision )
            throws Exception {
        if (precision == 0) {
            precision = 6;
        }
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);

        String sql;
        if (fields == null || fields.length == 0) {
            sql = "SELECT AsGeoJson(ST_Collect(ST_Transform(" + gCol.geometryColumnName + ",4326)), " + precision + ",0) FROM "
                    + tableName;
            if (wherePiece != null) {
                sql += " WHERE " + wherePiece;
            }
        } else {
            sql = "SELECT \"{\"\"type\"\":\"\"FeatureCollection\"\",\"\"features\"\":[\" || group_concat(\"{\"\"type\"\":\"\"Feature\"\",\"\"geometry\"\":\" || AsGeoJson("
                    + gCol.geometryColumnName + ", " + precision + ", 0) || \",\"\"properties\"\": {\" || ";
            List<String> fieldsList = new ArrayList<>();
            for( String field : fields ) {
                String string = "\"\"\"" + field + "\"\":\"\"\" || " + field + " || \"\"\"\"";
                fieldsList.add(string);
            }
            StringBuilder sb = new StringBuilder();
            for( int i = 0; i < fieldsList.size(); i++ ) {
                if (i > 0) {
                    sb.append(" || \",\" ||");
                }
                sb.append("\n").append(fieldsList.get(i));
            }
            sql += sb.toString() + " || \"}}\") || \"]}\"";
            sql += " FROM " + tableName;
            if (wherePiece != null) {
                sql += " WHERE " + wherePiece;
            }
        }
        try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String geoJson = rs.getString(1);
                return geoJson;
            }
        }
        return "";
    }

    public static ETableType getTableType( ADb db, String tableName ) throws Exception {
        String sql = "SELECT type, sql FROM sqlite_master WHERE Lower(tbl_name)=Lower('" + tableName + "')";
        try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String typeStr = rs.getString(1);
                String sqlStr = rs.getString(2);
                ETableType type = ETableType.fromType(typeStr);
                if (type == ETableType.TABLE) {
                    // check if it is virtual shp
                    if (sqlStr.contains("USING VirtualShape")) {
                        return ETableType.EXTERNAL;
                    }
                }
                return type;
            }
        }
        return ETableType.OTHER;
    }

    public static List<Index> getIndexes( ADb db, String tableName ) throws Exception {
        String sql = "SELECT name, sql FROM sqlite_master WHERE type='index' and tbl_name='" + tableName + "'";

        List<Index> indexes = new ArrayList<Index>();
        try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                Index index = new Index();

                String indexName = rs.getString(1);

                index.table = tableName;
                index.name = indexName;

                String createSql = rs.getString(2);
                if (createSql != null) {
                    String lower = createSql.toLowerCase();
                    if (lower.startsWith("create index") || lower.startsWith("create unique index")) {
                        String[] split = createSql.split("\\(|\\)");
                        String columns = split[1];
                        String[] colSplit = columns.split(",");
                        for( String col : colSplit ) {
                            col = col.trim();
                            if (col.length() > 0) {
                                index.columns.add(col);
                            }
                        }

                        if (lower.startsWith("create unique index")) {
                            index.isUnique = true;
                        }

                        indexes.add(index);
                    }
                }
            }
            return indexes;
        } catch (SQLException e) {
            if (e.getMessage().contains("query does not return ResultSet")) {
                return indexes;
            } else {
                throw e;
            }
        }
    }
}
