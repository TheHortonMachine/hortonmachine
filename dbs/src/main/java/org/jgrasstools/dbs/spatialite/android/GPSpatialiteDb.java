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
package org.jgrasstools.dbs.spatialite.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.compat.objects.ForeignKey;
import org.jgrasstools.dbs.compat.objects.QueryResult;
import org.jgrasstools.dbs.spatialite.ESqliteDataType;
import org.jgrasstools.dbs.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.dbs.spatialite.SpatialiteTableNames;
import org.jgrasstools.dbs.spatialite.SpatialiteWKBReader;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import jsqlite.Database;

/**
 * A spatialite database for android.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GPSpatialiteDb extends ASpatialDb {
    public boolean open( String dbPath ) throws Exception {
        this.mDbPath = dbPath;

        boolean dbExists = false;
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            if (mPrintInfos)
                logInfo("Database exists");
            dbExists = true;
        }

        Database database = new Database();
        database.open(dbPath, jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);

        mConn = new GPConnection(database);
        if (mPrintInfos)
            try (IJGTStatement stmt = mConn.createStatement()) {
                stmt.execute("SELECT sqlite_version()");
                IJGTResultSet rs = stmt.executeQuery("SELECT sqlite_version() AS 'SQLite Version';");
                while( rs.next() ) {
                    String sqliteVersion = rs.getString(1);
                    logInfo("SQLite Version: " + sqliteVersion);
                }
            }
        return dbExists;
    }

    public String[] getDbInfo() throws Exception {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT sqlite_version(), spatialite_version(), spatialite_target_cpu()";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            String[] info = new String[3];
            while( rs.next() ) {
                // read the result set
                info[0] = rs.getString(1);
                info[1] = rs.getString(2);
                info[2] = rs.getString(3);
            }
            return info;
        }
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        if (options == null) {
            options = "";
        }
        String sql = "SELECT InitSpatialMetadata(" + options + ")";
        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID, int limit,
            int reprojectSrid ) throws Exception {
        QueryResult queryResult = new QueryResult();

        GeometryColumn gCol = null;
        try {
            gCol = getGeometryColumnsForTable(tableName);
        } catch (Exception e) {
            // ignore
        }
        boolean hasGeom = gCol != null;

        List<String[]> tableColumnsInfo = getTableColumns(tableName);
        List<String> tableColumns = new ArrayList<>();
        HashMap<String, String> name2TypeMap = new HashMap<>();
        for( String[] info : tableColumnsInfo ) {
            tableColumns.add(info[0]);
            if (info[1].length() > 0)
                name2TypeMap.put(info[0], info[1]);
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
            if (!tableColumns.remove(PK_UID)) {
                tableColumns.remove(PKUID);
            }
        }

        String sql = "SELECT ";
        if (hasGeom) {
            if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                sql += gCol.geometryColumnName;
            } else {
                sql += "ST_Transform(" + gCol.geometryColumnName + "," + reprojectSrid + ") AS "
                        + gCol.geometryColumnName;
            }
        }
        for( int i = 0; i < tableColumns.size(); i++ ) {
            if (hasGeom || i != 0)
                sql += ",";
            sql += tableColumns.get(i);
        }
        sql += " FROM " + tableName;
        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE "; //
            sql += getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        SpatialiteWKBReader wkbReader = new SpatialiteWKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                if (hasGeom && columnName.equals(gCol.geometryColumnName)) {
                    queryResult.geometryIndex = i - 1;
                }
                String type = name2TypeMap.get(columnName);
                String columnTypeName = "UNKNOWN";
                if (type != null) {
                    columnTypeName = type;
                }
                queryResult.types.add(columnTypeName);
            }

            String[] typesArray = new String[queryResult.types.size()];
            for( int i = 0; i < queryResult.types.size(); i++ ) {
                typesArray[i] = queryResult.types.get(i).toLowerCase();
            }
            while( rs.next() ) {
                int i = 1;
                Object[] rec = new Object[columnCount];
                if (hasGeom) {
                    byte[] geomBytes = rs.getBytes(i);
                    Geometry geometry = wkbReader.read(geomBytes);
                    rec[i - 1] = geometry;
                    i++;
                }
                for( int j = i; j <= columnCount; j++ ) {
                    Object object;
                    String type = typesArray[j - 1];
                    if (type.equals(ESqliteDataType.TEXT.getLowercaseName())) {
                        object = rs.getString(j);
                    } else if (type.equals(ESqliteDataType.INT.getLowercaseName())) {
                        object = rs.getInt(j);
                    } else if (type.equals(ESqliteDataType.REAL.getLowercaseName())) {
                        object = rs.getDouble(j);
                    } else {
                        object = rs.getObject(j);
                    }
                    rec[j - 1] = object;
                }
                queryResult.data.add(rec);
            }
            return queryResult;
        }
    }

    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    protected void logWarn( String message ) {
        // Log.w("SpatialiteDb", message);
    }

    @Override
    protected void logInfo( String message ) {
        // Log.i("SpatialiteDb", message);
    }

    @Override
    protected void logDebug( String message ) {
        // Log.d("SpatialiteDb", message);
    }

    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
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

    /**
     * Get the geometry column definition for a given table.
     * 
     * @param tableName
     *            the table to check.
     * @return the {@link SpatialiteGeometryColumns column info}.
     * @throws Exception
     */
    public GeometryColumn getGeometryColumnsForTable( String tableName ) throws Exception {
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
                + attachedStr + SpatialiteGeometryColumns.TABLENAME + " where " + SpatialiteGeometryColumns.F_TABLE_NAME + "='"
                + tableName + "'";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
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

    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
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
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
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

    public List<String> getTables( boolean doOrder ) throws Exception {
        List<String> tableNames = new ArrayList<String>();
        String orderBy = " ORDER BY name";
        if (!doOrder) {
            orderBy = "";
        }
        String sql = "SELECT name FROM sqlite_master WHERE type='table' or type='view'" + orderBy;
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String tabelName = rs.getString(1);
                tableNames.add(tabelName);
            }
            return tableNames;
        }
    }

    public boolean hasTable( String tableName ) throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table'";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String name = rs.getString(1);
                if (name.equals(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public List<String[]> getTableColumns( String tableName ) throws Exception {
        String sql;
        if (tableName.indexOf('.') != -1) {
            // it is an attached database
            String[] split = tableName.split("\\.");
            String dbName = split[0];
            String tmpTableName = split[1];
            sql = "PRAGMA " + dbName + ".table_info(" + tmpTableName + ")";
        } else {
            sql = "PRAGMA table_info(" + tableName + ")";
        }

        List<String[]> columnNames = new ArrayList<String[]>();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
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

            while( rs.next() ) {
                String name = rs.getString(nameIndex);
                String type = rs.getString(typeIndex);
                String pk = "0";
                if (pkIndex > 0)
                    pk = rs.getString(pkIndex);
                columnNames.add(new String[]{name, type, pk});
            }
            return columnNames;
        }
    }

    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        String sql = null;
        if (tableName.indexOf('.') != -1) {
            // it is an attached database
            String[] split = tableName.split("\\.");
            String dbName = split[0];
            String tmpTableName = split[1];
            sql = "PRAGMA " + dbName + ".foreign_key_list(" + tmpTableName + ")";
        } else {
            sql = "PRAGMA foreign_key_list(" + tableName + ")";
        }

        List<ForeignKey> fKeys = new ArrayList<ForeignKey>();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int fromIndex = -1;
            int toIndex = -1;
            int toTableIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                if (columnName.equals("from")) {
                    fromIndex = i;
                } else if (columnName.equals("to")) {
                    toIndex = i;
                } else if (columnName.equals("table")) {
                    toTableIndex = i;
                }
            }
            while( rs.next() ) {
                ForeignKey fKey = new ForeignKey();
                Object fromObj = rs.getObject(fromIndex);
                Object toObj = rs.getObject(toIndex);
                Object toTableObj = rs.getObject(toTableIndex);
                if (fromObj != null && toObj != null && toTableObj != null) {
                    fKey.from = fromObj.toString();
                    fKey.to = toObj.toString();
                    fKey.toTable = toTableObj.toString();
                } else {
                    continue;
                }
                fKeys.add(fKey);
            }
            return fKeys;
        }
    }

    @Override
    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = SpatialiteTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }
}
