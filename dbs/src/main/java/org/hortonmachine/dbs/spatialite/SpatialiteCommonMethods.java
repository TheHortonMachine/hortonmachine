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
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.ResultSetToObjectFunction;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;

/**
 * Common methods for spatialite (android and java).
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SpatialiteCommonMethods {

    private static final String PK_UID = "pk_uid";
    private static final String PKUID = "pkuid";

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

    /**
     * Check for compatibility issues with other databases.
     * 
     * @param sql the original sql.
     * @return the fixed sql.
     */
    public static String checkCompatibilityIssues( String sql ) {
        sql = sql.replaceAll("LONG PRIMARY KEY AUTOINCREMENT", "INTEGER PRIMARY KEY AUTOINCREMENT");
        sql = sql.replaceAll("AUTO_INCREMENT", "AUTOINCREMENT");
        return sql;
    }

    public static QueryResult getTableRecordsMapIn( ASpatialDb db, String tableName, Envelope envelope, int limit,
            int reprojectSrid, String whereStr ) throws Exception, ParseException {
        QueryResult queryResult = new QueryResult();
        GeometryColumn gCol = null;
        String geomColLower = null;
        try {
            gCol = db.getGeometryColumnsForTable(tableName);
            // TODO check if it is a virtual table
            if (gCol != null)
                geomColLower = gCol.geometryColumnName.toLowerCase();
        } catch (Exception e) {
            // ignore
        }

        List<String[]> tableColumnsInfo = db.getTableColumns(tableName);
        int columnCount = tableColumnsInfo.size();

        int index = 0;
        List<String> items = new ArrayList<>();
        List<ResultSetToObjectFunction> funct = new ArrayList<>();
        for( String[] columnInfo : tableColumnsInfo ) {
            String columnName = columnInfo[0];
            if (DbsUtilities.isReservedName(columnName)) {
                columnName = DbsUtilities.fixReservedNameForQuery(columnName);
            }

            String columnTypeName = columnInfo[1];

            queryResult.names.add(columnName);
            queryResult.types.add(columnTypeName);

            String isPk = columnInfo[2];
            if (isPk.equals("1")) {
                queryResult.pkIndex = index;
            }
            if (geomColLower != null && columnName.toLowerCase().equals(geomColLower)) {
                queryResult.geometryIndex = index;

                if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                    items.add("ST_AsBinary(" + geomColLower + ")");
                } else {
                    items.add("ST_AsBinary(ST_Transform(" + geomColLower + "," + reprojectSrid + ")) AS " + geomColLower);
                }
            } else {
                items.add(columnName);
            }
            index++;

            EDataType type = EDataType.getType4Name(columnTypeName);
            switch( type ) {
            case TEXT: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getString(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case INTEGER: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getInt(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case BOOLEAN: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getInt(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case FLOAT: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getFloat(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case DOUBLE: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getDouble(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case LONG: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getLong(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case BLOB: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            return resultSet.getBytes(index);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            case DATETIME:
            case DATE: {
                funct.add(new ResultSetToObjectFunction(){
                    @Override
                    public Object getObject( IHMResultSet resultSet, int index ) {
                        try {
                            String date = resultSet.getString(index);
                            return date;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                });
                break;
            }
            default:
                funct.add(null);
                break;
            }
        }

        String sql = "SELECT ";
        sql += DbsUtilities.joinByComma(items);
        sql += " FROM " + DbsUtilities.fixTableName(tableName);

        List<String> whereStrings = new ArrayList<>();
        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            String spatialindexBBoxWherePiece = db.getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
            if (spatialindexBBoxWherePiece != null)
                whereStrings.add(spatialindexBBoxWherePiece);
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
        IGeometryParser gp = db.getType().getGeometryParser();

        String _sql = sql;
        return db.execOnConnection(connection -> {
            long start = System.currentTimeMillis();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    Object[] rec = new Object[columnCount];
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (queryResult.geometryIndex == j - 1) {
                            Geometry geometry = gp.fromResultSet(rs, j);
                            if (geometry != null) {
                                rec[j - 1] = geometry;
                            }
                        } else {
                            ResultSetToObjectFunction function = funct.get(j - 1);
                            Object object = function.getObject(rs, j);
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
        });

    }

    /**
     * Get the table columns from a non spatial db.
     * 
     * @param db the db.
     * @param tableName the name of the table to get the columns for.
     * @return the list of table column information. See {@link ADb#getTableColumns(String)}
     * @throws Exception
     */
    public static List<String[]> getTableColumns( ADb db, String tableName ) throws Exception {
        String sql;
        tableName = DbsUtilities.fixTableName(tableName);
        if (tableName.indexOf('.') != -1) {
            // it is an attached database
            String[] split = tableName.split("\\.");
            String dbName = split[0];
            String tmpTableName = split[1];
            sql = "PRAGMA " + dbName + ".table_info(" + tmpTableName + ")";
        } else {
            sql = "PRAGMA table_info(" + tableName + ")";
        }

        return db.execOnConnection(connection -> {
            List<String[]> columnsInfo = new ArrayList<String[]>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int nameIndex = -1;
                int typeIndex = -1;
                int pkIndex = -1;
                for( int i = 1; i <= columnCount; i++ ) {
                    String columnName = rsmd.getColumnName(i);
                    if (columnName.equals("name")) {
                        nameIndex = i;
                    } else if (columnName.equals("type")) {
                        typeIndex = i;
                    } else if (columnName.equals("pk")) {
                        pkIndex = i;
                    }
                }

                boolean hasPk = false;
                while( rs.next() ) {
                    String name = rs.getString(nameIndex);
                    String type = rs.getString(typeIndex);
                    String pk = "0";
                    if (pkIndex > 0) {
                        pk = rs.getString(pkIndex);
                        if (pk.equals("1")) {
                            hasPk = true;
                        }

                    }
                    columnsInfo.add(new String[]{name, type, pk});
                }

                // if no pk is available in the table, check if there is a field pkuid and
                // in case set that as pk
                if (!hasPk) {
                    for( String[] colInfo : columnsInfo ) {
                        String name = colInfo[0].toLowerCase();
                        if (name.equals(PKUID) || name.equals(PK_UID)) {
                            colInfo[2] = "1";
                        }
                    }
                }
                return columnsInfo;
            }
        });
    }

    /**
     * Get the primary key from a non spatial db.
     * 
     * @param db the db.
     * @param tableName the name of the table to get the pk for.
     * @return the pk name
     * @throws Exception
     */
    public static String getPrimaryKey( ADb db, String tableName ) throws Exception {
        String sql;
        tableName = DbsUtilities.fixTableName(tableName);
        if (tableName.indexOf('.') != -1) {
            // it is an attached database
            String[] split = tableName.split("\\.");
            String dbName = split[0];
            String tmpTableName = split[1];
            sql = "PRAGMA " + dbName + ".table_info(" + tmpTableName + ")";
        } else {
            sql = "PRAGMA table_info(" + tableName + ")";
        }

        return db.execOnConnection(connection -> {
            List<String[]> primaryKey = new ArrayList<>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int nameIndex = -1;
                int typeIndex = -1;
                int pkIndex = -1;
                for( int i = 1; i <= columnCount; i++ ) {
                    String columnName = rsmd.getColumnName(i);
                    if (columnName.equals("name")) {
                        nameIndex = i;
                    } else if (columnName.equals("type")) {
                        typeIndex = i;
                    } else if (columnName.equals("pk")) {
                        pkIndex = i;
                    }
                }

                boolean hasPk = false;
                while( rs.next() ) {
                    String name = rs.getString(nameIndex);
                    String type = rs.getString(typeIndex);
                    String pk = "0";
                    if (pkIndex > 0) {
                        pk = rs.getString(pkIndex);
                        if (pk.equals("1")) {
                            return name;
                        }
                    }
                    primaryKey.add(new String[]{name, type, pk});
                }

                // if no pk is available in the table, check if there is a field pkuid and
                // in case set that as pk
                if (!hasPk) {
                    for( String[] colInfo : primaryKey ) {
                        String name = colInfo[0].toLowerCase();
                        if (name.equals(PKUID) || name.equals(PK_UID)) {
                            colInfo[2] = "1";
                            return name;
                        }
                    }
                }
                return null;
            }
        });
    }

    /**
     * Get the table columns from a spatial db. Some checks are done on non recognised columns.
     * 
     * @param db the db.
     * @param tableName the name of the table to get the columns for.
     * @return the list of table column information. See {@link ADb#getTableColumns(String)}
     * @throws Exception
     */
    public static List<String[]> getTableColumns( ASpatialDb db, String tableName ) throws Exception {
        List<String[]> tableColumns = getTableColumns((ADb) db, tableName);
        for( String[] cols : tableColumns ) {
            if (cols[1].trim().length() == 0) {
                // might be a non understood geom type
                GeometryColumn gcol = db.getGeometryColumnsForTable(tableName);
                if (gcol != null && cols[0].equals(gcol.geometryColumnName)) {
                    cols[1] = gcol.geometryType.getTypeName();
                }
            }
        }
        return tableColumns;
    }

    // public static List<Geometry> getGeometriesIn( ASpatialDb db, String tableName, Envelope
    // envelope, String... prePostWhere )
    // throws Exception, ParseException {
    // List<Geometry> geoms = new ArrayList<Geometry>();
    //
    // List<String> wheres = new ArrayList<>();
    // String pre = "";
    // String post = "";
    // String where = "";
    // if (prePostWhere != null) {
    // pre = prePostWhere[0];
    // post = prePostWhere[1];
    // where = prePostWhere[2];
    // wheres.add(where);
    // }
    //
    // GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
    // String sql = "SELECT " + pre + gCol.geometryColumnName + post + " FROM " + tableName;
    //
    // if (envelope != null) {
    // double x1 = envelope.getMinX();
    // double y1 = envelope.getMinY();
    // double x2 = envelope.getMaxX();
    // double y2 = envelope.getMaxY();
    // wheres.add(db.getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2));
    // }
    //
    // if (wheres.size() > 0) {
    // sql += " WHERE " + DbsUtilities.joinBySeparator(wheres, " AND ");
    // }
    //
    // SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
    // try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs =
    // stmt.executeQuery(sql)) {
    // while( rs.next() ) {
    // byte[] geomBytes = rs.getBytes(1);
    // Geometry geometry = wkbReader.read(geomBytes);
    // geoms.add(geometry);
    // }
    // return geoms;
    // }
    // }
    //
    // public static List<Geometry> getGeometriesIn( ASpatialDb db, String tableName, Geometry
    // intersectionGeometry,
    // String... prePostWhere ) throws Exception, ParseException {
    // List<Geometry> geoms = new ArrayList<Geometry>();
    //
    // List<String> wheres = new ArrayList<>();
    // String pre = "";
    // String post = "";
    // String where = "";
    // if (prePostWhere != null) {
    // pre = prePostWhere[0];
    // post = prePostWhere[1];
    // where = prePostWhere[2];
    // wheres.add(where);
    // }
    //
    // GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
    // String sql = "SELECT " + pre + gCol.geometryColumnName + post + " FROM " + tableName;
    //
    // if (intersectionGeometry != null) {
    // wheres.add(db.getSpatialindexGeometryWherePiece(tableName, null, intersectionGeometry));
    // }
    //
    // if (wheres.size() > 0) {
    // sql += " WHERE " + DbsUtilities.joinBySeparator(wheres, " AND ");
    // }
    //
    // SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
    // try (IHMStatement stmt = db.getConnection().createStatement(); IHMResultSet rs =
    // stmt.executeQuery(sql)) {
    // while( rs.next() ) {
    // byte[] geomBytes = rs.getBytes(1);
    // Geometry geometry = wkbReader.read(geomBytes);
    // geoms.add(geometry);
    // }
    // return geoms;
    // }
    // }

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

        String tableNameNoApex = tableName.replaceAll("'", ""); // in case added due to special name
        String sql = "select " + SpatialiteGeometryColumns.F_TABLE_NAME + ", " //
                + SpatialiteGeometryColumns.F_GEOMETRY_COLUMN + ", " //
                + SpatialiteGeometryColumns.GEOMETRY_TYPE + "," //
                + SpatialiteGeometryColumns.COORD_DIMENSION + ", " //
                + SpatialiteGeometryColumns.SRID + ", " //
                + SpatialiteGeometryColumns.SPATIAL_INDEX_ENABLED + " from " //
                + attachedStr + SpatialiteGeometryColumns.TABLENAME + " where Lower(" + SpatialiteGeometryColumns.F_TABLE_NAME
                + ")=Lower('" + tableNameNoApex + "')";
        try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                SpatialiteGeometryColumns gc = new SpatialiteGeometryColumns();
                gc.tableName = rs.getString(1);
                gc.geometryColumnName = rs.getString(2);
                gc.geometryType = EGeometryType.fromGeometryTypeCode(rs.getInt(3));
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
                gc.geometryType = EGeometryType.fromGeometryTypeCode(rs.getInt(3));
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
        String sql = "SELECT InitSpatialMetadata(" + options + ")";

        db.execOnConnection(connection -> {
            connection.enableAutocommit(false);
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
            connection.enableAutocommit(true);
            return null;
        });

    }

    public static Envelope getTableBounds( ASpatialDb db, String tableName ) throws Exception {
        GeometryColumn gCol = db.getGeometryColumnsForTable(tableName);
        String geomFieldName;
        if (gCol != null) {
            geomFieldName = gCol.geometryColumnName;
            String trySql = "SELECT extent_min_x, extent_min_y, extent_max_x, extent_max_y FROM vector_layers_statistics WHERE Lower(table_name)=Lower('"
                    + tableName + "') AND Lower(geometry_column)=Lower('" + geomFieldName + "')";

            Envelope resEnv = db.execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(trySql)) {
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
                    return null;
                }
            });
            if (resEnv != null)
                return resEnv;
        } else {
            // try geometry if virtual table
            geomFieldName = "geometry";
        }

        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT Min(MbrMinX(" + geomFieldName + ")) AS min_x, Min(MbrMinY(" + geomFieldName + ")) AS min_y,"
                + "Max(MbrMaxX(" + geomFieldName + ")) AS max_x, Max(MbrMaxY(" + geomFieldName + ")) AS max_y " + "FROM "
                + tableName;

        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
        });
    }

    public static String getSpatialindexBBoxWherePiece( ASpatialDb db, String tableName, String alias, double x1, double y1,
            double x2, double y2 ) throws Exception {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = DbsUtilities.fixTableName(tableName) + ".ROWID";
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

        String _geomColName = geomColName;
        String _epsgStr = epsgStr;
        String _geomTypeStr = geomTypeStr;
        db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                String sql = "SELECT AddGeometryColumn('" + tableName + "','" + _geomColName + "', " + _epsgStr + ", '"
                        + _geomTypeStr + "', 'XY')";
                stmt.execute(sql);

                if (!avoidIndex) {
                    sql = "SELECT CreateSpatialIndex('" + tableName + "', '" + _geomColName + "');";
                    stmt.execute(sql);
                }
            }
            return null;
        });

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

        String _sql = sql;
        db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.execute(_sql);
            }
            return null;
        });

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
                    + DbsUtilities.fixTableName(tableName);
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
            sql += " FROM " + DbsUtilities.fixTableName(tableName);
            if (wherePiece != null) {
                sql += " WHERE " + wherePiece;
            }
        }

        String _sql = sql;
        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                if (rs.next()) {
                    String geoJson = rs.getString(1);
                    return geoJson;
                }
            }
            return "";
        });
    }

    public static ETableType getTableType( ADb db, String tableName ) throws Exception {
        String sql = "SELECT type, sql FROM sqlite_master WHERE Lower(tbl_name)=Lower('" + tableName + "')";

        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
        });

    }

    public static List<Index> getIndexes( ADb db, String tableName ) throws Exception {
        String sql = "SELECT name, sql FROM sqlite_master WHERE type='index' and tbl_name='" + tableName + "'";

        return db.execOnConnection(connection -> {
            List<Index> indexes = new ArrayList<Index>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
        });
    }
}
