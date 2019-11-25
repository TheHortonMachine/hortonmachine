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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.IDbVisitor;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.sqlite.SQLiteConfig;

/**
 * A sqlite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SqliteDb extends ADb {
    private static final String DRIVER_CLASS = "org.sqlite.JDBC";
    private Connection jdbcConn;
    private HMConnection mConn;
    private ConnectionData connectionData;

    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public EDb getType() {
        return EDb.SQLITE;
    }

    public void setCredentials( String user, String password ) {
        this.user = user;
        this.password = password;
    }

    @Override
    public ConnectionData getConnectionData() {
        return connectionData;
    }

    @Override
    public boolean open( String dbPath, String user, String password ) throws Exception {
        setCredentials(user, password);
        return open(dbPath);
    }

    public boolean open( String dbPath ) throws Exception {
        this.mDbPath = dbPath;

        connectionData = new ConnectionData();
        connectionData.connectionLabel = dbPath;
        connectionData.connectionUrl = new String(dbPath);
        connectionData.user = user;
        connectionData.password = password;
        connectionData.dbType = getType().getCode();

        boolean dbExists = false;
        if (dbPath != null) {
            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                if (mPrintInfos)
                    Logger.INSTANCE.insertInfo(null, "Database exists");
                dbExists = true;
            }
        } else {
            dbPath = "file:inmemory?mode=memory";
            dbExists = true;
        }
        // enabling dynamic extension loading
        // absolutely required by SpatiaLite
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        Properties properties = config.toProperties();
        if (user != null && password != null) {
            properties.setProperty("user", user);
            properties.setProperty("password", password);
        }
        jdbcConn = DriverManager.getConnection(EDb.SQLITE.getJdbcPrefix() + dbPath, properties);
        mConn = new HMConnection(jdbcConn, false);
        if (mPrintInfos) {
            String[] dbInfo = getDbInfo();
            Logger.INSTANCE.insertInfo(null, "SQLite Version: " + dbInfo[0]);
        }
        return dbExists;
    }

    @Override
    public String getJdbcUrlPre() {
        return EDb.SQLITE.getJdbcPrefix();
    }

    public Connection getJdbcConnection() {
        return jdbcConn;
    }

    public IHMConnection getConnectionInternal() throws Exception {
        return mConn;
    }

    public void close() throws Exception {
        if (mConn != null) {
            mConn.setAutoCommit(false);
            mConn.commit();
            mConn.close();
            mConn = null;
        }
    }

    public String[] getDbInfo() {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT sqlite_version()";
        try {
            try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                String[] info = new String[1];
                while( rs.next() ) {
                    // read the result set
                    info[0] = rs.getString(1);
                }
                return info;
            }
        } catch (Exception e) {
            return new String[]{"no version info available"};
        }
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

    public List<String> getTables( boolean doOrder ) throws Exception {
        List<String> tableNames = new ArrayList<String>();
        String orderBy = " ORDER BY name";
        if (!doOrder) {
            orderBy = "";
        }
        String sql = "SELECT name FROM sqlite_master WHERE type='table' or type='view'" + orderBy;
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String tabelName = rs.getString(1);
                tableNames.add(tabelName);
            }
            return tableNames;
        }
    }

    public boolean hasTable( String tableName ) throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table'";
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String name = rs.getString(1);
                name = DbsUtilities.fixTableName(name);
                if (name.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public ETableType getTableType( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableType(this, tableName);
    }

    public List<String[]> getTableColumns( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getTableColumns(this, tableName);
    }

    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        String sql = null;
        if (tableName.indexOf('.') != -1) {
            // it is an attached database
            String[] split = tableName.split("\\.");
            String dbName = split[0];
            String tmpTableName = split[1];
            sql = "PRAGMA " + dbName + ".foreign_key_list(" + DbsUtilities.fixTableName(tmpTableName) + ")";
        } else {
            sql = "PRAGMA foreign_key_list(" + DbsUtilities.fixTableName(tableName) + ")";
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
                fKey.fromTable = tableName;
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
        } catch (SQLException e) {
            if (e.getMessage().contains("query does not return ResultSet")) {
                return fKeys;
            } else {
                throw e;
            }
        }
    }

    @Override
    public List<Index> getIndexes( String tableName ) throws Exception {
        return SpatialiteCommonMethods.getIndexes(this, tableName);
    }

    @Override
    public void accept( IDbVisitor visitor ) throws Exception {
        visitor.visit(jdbcConn);
    }

}
