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
package org.hortonmachine.dbs.spatialite.hm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Clob;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IDbVisitor;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.ESpatialiteGeometryType;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.dbs.spatialite.SpatialiteGeometryColumns;
import org.hortonmachine.dbs.spatialite.SpatialiteTableNames;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.OsCheck;
import org.hortonmachine.dbs.utils.OsCheck.OSType;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * A spatialite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteDb extends ASpatialDb {
    private SqliteDb sqliteDb;

    public SpatialiteDb() {
        sqliteDb = new SqliteDb();
    }

    @Override
    public EDb getType() {
        return EDb.SPATIALITE;
    }

    public void setCredentials( String user, String password ) {
        this.user = user;
        this.password = password;
    }

    @Override
    public ConnectionData getConnectionData() {
        return sqliteDb.getConnectionData();
    }

    @Override
    public boolean open( String dbPath, String user, String password ) throws Exception {
        setCredentials(user, password);
        return open(dbPath);
    }

    public boolean open( String dbPath ) throws Exception {
        sqliteDb.setCredentials(user, password);
        boolean dbExists = sqliteDb.open(dbPath);

        sqliteDb.getConnectionData().dbType = getType().getCode();

        this.mDbPath = sqliteDb.getDatabasePath();
        IHMConnection mConn = sqliteDb.getConnectionInternal();
        try (IHMStatement stmt = mConn.createStatement()) {
            // set timeout to 30 sec.
            stmt.setQueryTimeout(30);
            // load SpatiaLite
            try {
                String spatialiteLibsFolder = DbsUtilities.getPreference(DbsUtilities.SPATIALITE_DYLIB_FOLDER, "").trim();
                if (spatialiteLibsFolder.length() > 0 && !spatialiteLibsFolder.endsWith("/")) {
                    spatialiteLibsFolder += "/";
                }
                OSType operatingSystemType = OsCheck.getOperatingSystemType();
                switch( operatingSystemType ) {
                case Linux:
                    try {
                        stmt.execute("SELECT load_extension('mod_rasterlite2.so', 'sqlite3_modrasterlite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            Logger.INSTANCE.insertInfo(null, "Unable to load mod_rasterlite2.so: " + e.getMessage());
                        }
                        try {
                            stmt.execute("SELECT load_extension('mod_rasterlite2', 'sqlite3_modrasterlite_init')");
                        } catch (Exception e1) {
                            Logger.INSTANCE.insertInfo(null, "Unable to load mod_rasterlite2: " + e1.getMessage());
                        }
                    }
                    try {
                        stmt.execute("SELECT load_extension('mod_spatialite.so', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            Logger.INSTANCE.insertInfo(null, "Unable to load mod_spatialite.so: " + e.getMessage());
                        }
                        try {
                            stmt.execute("SELECT load_extension('mod_spatialite', 'sqlite3_modspatialite_init')");
                        } catch (Exception e1) {
                            Logger.INSTANCE.insertInfo(null, "Unable to load mod_spatialite: " + e1.getMessage());
                        }
                        throw e;
                    }
                    break;
                case MacOS:
                    try {
                        stmt.execute("SELECT load_extension('" + spatialiteLibsFolder
                                + "mod_spatialite.so', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            Logger.INSTANCE.insertInfo(null,
                                    "Unable to load " + spatialiteLibsFolder + "mod_spatialite.so: " + e.getMessage());
                        }
                        try {
                            stmt.execute("SELECT load_extension('mod_spatialite', 'sqlite3_modspatialite_init')");
                        } catch (Exception e1) {
                            Logger.INSTANCE.insertInfo(null, "Unable to load mod_spatialite: " + e1.getMessage());
                        }
                        throw e;
                    }
                    break;
                default:
                    try {
                        stmt.execute("SELECT load_extension('mod_rasterlite2', 'sqlite3_modrasterlite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            Logger.INSTANCE.insertInfo(null, "Unable to load mod_rasterlite2: " + e.getMessage());
                        }
                    }
                    try {
                        stmt.execute("SELECT load_extension('mod_spatialite', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            Logger.INSTANCE.insertInfo(null, "Unable to load mod_spatialite: " + e.getMessage());
                        }
                        throw e;
                    }
                    break;
                }
            } catch (Exception e) {
                throw e;
                // Map<String, String> getenv = System.getenv();
                // for( Entry<String, String> entry : getenv.entrySet() ) {
                // System.out.println(entry.getKey() + ": " + entry.getValue());
                // }
            }
        }
        if (mPrintInfos) {
            String[] dbInfo = getDbInfo();
            Logger.INSTANCE.insertInfo(null, "Spatialite Version: " + dbInfo[0]);
            Logger.INSTANCE.insertInfo(null, "Spatialite Target CPU: " + dbInfo[1]);
        }
        return dbExists;
    }

    @Override
    public String getJdbcUrlPre() {
        return sqliteDb.getJdbcUrlPre();
    }

    public Connection getJdbcConnection() {
        return sqliteDb.getJdbcConnection();
    }

    @Override
    public IHMConnection getConnectionInternal() throws Exception {
        return sqliteDb.getConnectionInternal();
    }

    public void close() throws Exception {
        sqliteDb.close();
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        SpatialiteCommonMethods.initSpatialMetadata(this, options);
    }

    public String[] getDbInfo() {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT spatialite_version(), spatialite_target_cpu()";
        try {
            IHMConnection mConn = sqliteDb.getConnectionInternal();
            try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                String[] info = new String[2];
                while( rs.next() ) {
                    // read the result set
                    info[0] = rs.getString(1);
                    info[1] = rs.getString(2);
                }
                return info;
            }
        } catch (Exception e) {
            return new String[]{"no version info available", "no version info available"};
        }
    }

    public void createSpatialTable( String tableName, int tableSrid, String geometryFieldData, String[] fieldData,
            String[] foreignKeys, boolean avoidIndex ) throws Exception {
        SpatialiteCommonMethods.createSpatialTable(this, tableName, tableSrid, geometryFieldData, fieldData, foreignKeys,
                avoidIndex);
    }

    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableBounds(this, tableName);
    }

    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, int limit, int reprojectSrid, String whereStr )
            throws Exception {
        return SpatialiteCommonMethods.getTableRecordsMapIn(this, tableName, envelope, limit, reprojectSrid, whereStr);
    }

    @Override
    protected void logWarn( String message ) {
        Logger.INSTANCE.insertWarning(null, message);
    }

    @Override
    protected void logInfo( String message ) {
        Logger.INSTANCE.insertInfo(null, message);
    }

    @Override
    protected void logDebug( String message ) {
        Logger.INSTANCE.insertDebug(null, message);
    }

    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = SpatialiteTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        return SpatialiteCommonMethods.getSpatialindexBBoxWherePiece(this, tableName, alias, x1, y1, x2, y2);
    }

    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
        return SpatialiteCommonMethods.getSpatialindexGeometryWherePiece(this, tableName, alias, geometry);
    }

    public GeometryColumn getGeometryColumnsForTable( String tableName ) throws Exception {
        if (!hasTable(SpatialiteGeometryColumns.TABLENAME))
            return null;
        return SpatialiteCommonMethods.getGeometryColumnsForTable(sqliteDb.getConnectionInternal(), tableName);
    }

    @Override
    public List<String> getTables( boolean doOrder ) throws Exception {
        return sqliteDb.getTables(doOrder);
    }

    @Override
    public boolean hasTable( String tableName ) throws Exception {
        return sqliteDb.hasTable(tableName);
    }

    public ETableType getTableType( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableType(this, tableName);
    }

    @Override
    public List<String[]> getTableColumns( String tableName ) throws Exception {
        List<String[]> tableColumns = SpatialiteCommonMethods.getTableColumns(this, tableName);
        return tableColumns;
    }

    @Override
    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        return sqliteDb.getForeignKeys(tableName);
    }

    public String getGeojsonIn( String tableName, String[] fields, String wherePiece, Integer precision ) throws Exception {
        return SpatialiteCommonMethods.getGeojsonIn(this, tableName, fields, wherePiece, precision);
    }

    /**
     * Delete a geo-table with all attached indexes and stuff.
     * 
     * @param tableName
     * @throws Exception
     */
    public void deleteGeoTable( String tableName ) throws Exception {
        String sql = "SELECT DropGeoTable('" + tableName + "');";

        try (IHMStatement stmt = sqliteDb.getConnectionInternal().createStatement()) {
            stmt.execute(sql);
        }
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg,
            boolean avoidIndex ) throws Exception {
        SpatialiteCommonMethods.addGeometryXYColumnAndIndex(this, tableName, geomColName, geomType, epsg, avoidIndex);
    }

    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg )
            throws Exception {
        addGeometryXYColumnAndIndex(tableName, geomColName, geomType, epsg, false);
    }

    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws Exception {
        QueryResult queryResult = new QueryResult();
        try (IHMStatement stmt = sqliteDb.getConnectionInternal().createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            IHMResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (columnName.toLowerCase().contains("st_asbinary") || ESpatialiteGeometryType.isGeometryName(columnTypeName)) {
                    geometryIndex = i;
                    queryResult.geometryIndex = i - 1;
                }
            }
            int count = 0;
            IGeometryParser gp = getType().getGeometryParser();
            long start = System.currentTimeMillis();
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (j == geometryIndex) {
                        Geometry geometry = gp.fromResultSet(rs, j);
                        if (geometry != null) {
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
                if (limit > 0 && ++count > (limit - 1)) {
                    break;
                }
            }
            long end = System.currentTimeMillis();
            queryResult.queryTimeMillis = end - start;
            return queryResult;
        }
    }

    /**
     * Execute a query from raw sql and put the result in a csv file.
     * 
     * @param sql
     *            the sql to run.
     * @param csvFile
     *            the output file.
     * @param doHeader
     *            if <code>true</code>, the header is written.
     * @param separator
     *            the separator (if null, ";" is used).
     * @throws Exception
     */
    public void runRawSqlToCsv( String sql, File csvFile, boolean doHeader, String separator ) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
            IGeometryParser gp = getType().getGeometryParser();
            try (IHMStatement stmt = sqliteDb.getConnectionInternal().createStatement();
                    IHMResultSet rs = stmt.executeQuery(sql)) {
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int geometryIndex = -1;
                for( int i = 1; i <= columnCount; i++ ) {
                    if (i > 1) {
                        bw.write(separator);
                    }
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    String columnName = rsmd.getColumnName(i);
                    bw.write(columnName);
                    if (columnName.toLowerCase().contains("st_asbinary")
                            || ESpatialiteGeometryType.isGeometryName(columnTypeName)) {
                        geometryIndex = i;
                    }
                }
                bw.write("\n");
                while( rs.next() ) {
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (j > 1) {
                            bw.write(separator);
                        }
                        if (j == geometryIndex) {
                            try {
                                Geometry geometry = gp.fromResultSet(rs, j);
                                if (geometry == null) {
                                    Object object = rs.getObject(j);
                                    if (object instanceof Clob) {
                                        object = rs.getString(j);
                                    }
                                    if (object != null) {
                                        bw.write(object.toString());
                                    } else {
                                        bw.write("");
                                    }
                                }
                            } catch (Exception e) {
                                // write it as it comes
                                Object object = rs.getObject(j);
                                if (object instanceof Clob) {
                                    object = rs.getString(j);
                                }
                                if (object != null) {
                                    bw.write(object.toString());
                                } else {
                                    bw.write("");
                                }
                            }
                        }
                    }
                    bw.write("\n");
                }
            }
        }
    }

    @Override
    public List<Index> getIndexes( String tableName ) throws Exception {
        return sqliteDb.getIndexes(tableName);
    }

    @Override
    public void accept( IDbVisitor visitor ) throws Exception {
        sqliteDb.accept(visitor);
    }

}
