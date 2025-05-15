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
package org.hortonmachine.dbs.spatialite.android;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IDbVisitor;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.compat.objects.SchemaLevel;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.dbs.spatialite.SpatialiteGeometryColumns;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import jsqlite.Database;

/**
 * A spatialite database for android.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GPSpatialiteDb extends ASpatialDb {
    private GPConnection mConn;
    private ConnectionData connectionData;
    private Database database;

    @Override
    public EDb getType() {
        return EDb.SPATIALITE4ANDROID;
    }

    public void setCredentials( String user, String password ) {
        // not supported on android
    }

    @Override
    public boolean open( String dbPath, String user, String password ) throws Exception {
        return open(dbPath);
    }

    @Override
    public ConnectionData getConnectionData() {
        return connectionData;
    }

    public Database getDatabase() {
        return database;
    }

    public boolean open( String dbPath ) throws Exception {
        this.mDbPath = dbPath;

        connectionData = new ConnectionData();
        connectionData.connectionLabel = dbPath;
        connectionData.connectionUrl = new String(dbPath);
        connectionData.dbType = getType().getCode();

        boolean dbExists = false;
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            if (mPrintInfos)
                logInfo("Database exists");
            dbExists = true;
        }

        database = new Database();
        database.open(dbPath, jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);

        mConn = new GPConnection(database);
        if (mPrintInfos)
            try (IHMStatement stmt = mConn.createStatement()) {
                stmt.execute("SELECT sqlite_version()");
                IHMResultSet rs = stmt.executeQuery("SELECT sqlite_version() AS 'SQLite Version';");
                while( rs.next() ) {
                    String sqliteVersion = rs.getString(1);
                    logInfo("SQLite Version: " + sqliteVersion);
                }
            }
        return dbExists;
    }

    @Override
    public void close() throws Exception {
        mConn.close();
    }

    @Override
    public Connection getJdbcConnection() {
        throw new IllegalArgumentException("Android drivers do not support this method.");
    }

    @Override
    public IHMConnection getConnectionInternal() throws Exception {
        return mConn;
    }

    @Override
    public String getJdbcUrlPre() {
        throw new IllegalArgumentException("Android drivers do not support this method.");
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        SpatialiteCommonMethods.initSpatialMetadata(this, options);
    }

    public String[] getDbInfo() {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT sqlite_version(), spatialite_version(), spatialite_target_cpu()";
        try {
            try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                String[] info = new String[3];
                while( rs.next() ) {
                    // read the result set
                    info[0] = rs.getString(1);
                    info[1] = rs.getString(2);
                    info[2] = rs.getString(3);
                }
                return info;
            }
        } catch (Exception e) {
            return new String[]{"no version info available", "no version info available", "no version info available"};
        }
    }

    public void createSpatialTable( SqlName tableName, int tableSrid, String geometryFieldData, String[] fieldData,
            String[] foreignKeys, boolean avoidIndex ) throws Exception {
        SpatialiteCommonMethods.createSpatialTable(this, tableName, tableSrid, geometryFieldData, fieldData, foreignKeys,
                avoidIndex);
    }

    public String checkSqlCompatibilityIssues( String sql ) {
        return SpatialiteCommonMethods.checkCompatibilityIssues(sql);
    }

    @Override
    public Envelope getTableBounds( SqlName tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableBounds(this, tableName);
    }

    public QueryResult getTableRecordsMapIn( SqlName tableName, Envelope envelope, int limit, int reprojectSrid, String whereStr )
            throws Exception {
        return SpatialiteCommonMethods.getTableRecordsMapIn(this, tableName, envelope, limit, reprojectSrid, whereStr);
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

    @Override
    public HashMap<String, HashMap<String,List<String>>> getTablesMap() throws Exception {
        List<TableName> tableNames = getTables();
        HashMap<String, HashMap<String, List<String>>> schema2types2tableMap = new HashMap<>();
        for (TableName tableName : tableNames) {
            String schema = tableName.getSchema();
            HashMap<String,List<String>> type2tableMap = schema2types2tableMap.get(schema);
            if (type2tableMap == null) {
                type2tableMap = new HashMap<>();
                schema2types2tableMap.put(schema, type2tableMap);
            }
            List<String> tablesList = type2tableMap.get(tableName.getTableType().name());
            if (tablesList == null) {
                tablesList = new ArrayList<>();
                type2tableMap.put(tableName.getTableType().name(), tablesList);
            }
            tablesList.add(tableName.getName());
        }
        return schema2types2tableMap;
    }

    public String getSpatialindexBBoxWherePiece( SqlName tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        return SpatialiteCommonMethods.getSpatialindexBBoxWherePiece(this, tableName, alias, x1, y1, x2, y2);
    }

    public String getSpatialindexGeometryWherePiece( SqlName tableName, String alias, Geometry geometry ) throws Exception {
        return SpatialiteCommonMethods.getSpatialindexGeometryWherePiece(this, tableName, alias, geometry);
    }

    public GeometryColumn getGeometryColumnsForTable( SqlName tableName ) throws Exception {
        if (!hasTable(SqlName.m(SpatialiteGeometryColumns.TABLENAME)))
            return null;
        return SpatialiteCommonMethods.getGeometryColumnsForTable(mConn, tableName);
    }

    public List<TableName> getTables( ) throws Exception {
        List<TableName> tableNames = new ArrayList<TableName>();
        String sql = """
            SELECT name, type 
            FROM sqlite_master 
            WHERE type IN ('table', 'view')
            ORDER BY name;
        """;
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String tabelName = rs.getString(1);
                String type = rs.getString(2);
                var tt = ETableType.fromType(type);
                tableNames.add(new TableName(tabelName, SchemaLevel.FALLBACK_SCHEMA, tt));
            }
            return tableNames;
        }
    }

    public boolean hasTable( SqlName tableName ) throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table'";
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String name = rs.getString(1);
                if (name.equals(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public ETableType getTableType( SqlName tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableType(this, tableName);
    }

    public List<String[]> getTableColumns( SqlName tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableColumns(this, tableName);
    }

    public List<ForeignKey> getForeignKeys( SqlName tableName ) throws Exception {
        String sql = null;
        if (tableName.name.indexOf('.') != -1) {
            // it is an attached database
            String[] split = tableName.name.split("\\.");
            String dbName = split[0];
            String tmpTableName = split[1];
            sql = "PRAGMA " + dbName + ".foreign_key_list(" + tmpTableName + ")";
        } else {
            sql = "PRAGMA foreign_key_list(" + tableName.name + ")";
        }

        List<ForeignKey> fKeys = new ArrayList<ForeignKey>();
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            IHMResultSetMetaData rsmd = rs.getMetaData();
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

    public String getGeojsonIn( SqlName tableName, String[] fields, String wherePiece, Integer precision ) throws Exception {
        return SpatialiteCommonMethods.getGeojsonIn(this, tableName, fields, wherePiece, precision);
    }

    public void addGeometryXYColumnAndIndex( SqlName tableName, String geomColName, String geomType, String epsg,
            boolean avoidIndex ) throws Exception {
        SpatialiteCommonMethods.addGeometryXYColumnAndIndex(this, tableName, geomColName, geomType, epsg, avoidIndex);
    }

    public void addGeometryXYColumnAndIndex( SqlName tableName, String geomColName, String geomType, String epsg )
            throws Exception {
        addGeometryXYColumnAndIndex(tableName, geomColName, geomType, epsg, false);
    }

    @Override
    public List<Index> getIndexes( SqlName tableName ) throws Exception {
        return SpatialiteCommonMethods.getIndexes(this, tableName);
    }

    @Override
    public void accept( IDbVisitor visitor ) {
        // not supported
    }

}
