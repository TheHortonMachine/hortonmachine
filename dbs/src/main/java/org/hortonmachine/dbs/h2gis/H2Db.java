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
package org.hortonmachine.dbs.h2gis;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.IDbVisitor;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.SpatialiteTableNames;
import org.hortonmachine.dbs.spatialite.hm.HMConnection;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * An H2 database.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class H2Db extends ADb {
    private static final String DRIVER_CLASS = "org.h2.Driver";
    /**
     * Keywords of H2 2 that are allowed as unquoted identifiers.
     */
    public static final String NON_KEYWORDS_SETTING = ";NON_KEYWORDS=KEY,VALUE";
    /**
     * Connection use in non pooled mode.
     */
    private Connection singleJdbcConn;

    /**
     * Connection source used in pooled mode.
     */
    private ComboPooledDataSource comboPooledDataSource;
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
        return EDb.H2;
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
            File dbFile = new File(dbPath + "." + EDb.H2.getExtension());
            if (dbFile.exists()) {
                if (mPrintInfos)
                    Logger.INSTANCE.insertInfo(null, "Database exists");
                dbExists = true;
            }
            if (dbPath.toLowerCase().startsWith("tcp")) {
                // no way to check, assume it exists
                dbExists = true;

                // also cleanup path
                int first = dbPath.indexOf('/');
                int second = dbPath.indexOf('/', first + 1);
                int third = dbPath.indexOf('/', second + 1);
                int lastSlash = dbPath.indexOf('/', third + 1);
                if (lastSlash != -1) {
                    mDbPath = dbPath.substring(lastSlash, dbPath.length());
                }
            }
        } else {
            dbPath = "mem:syntax";
            dbExists = false;
        }

        String jdbcUrl = EDb.H2.getJdbcPrefix() + dbPath;
        if (!jdbcUrl.toUpperCase().contains("NON_KEYWORDS")) {
            // KEY and VALUE are keywords since H2 2, but used as column names (mbtiles metadata, preferences)
            jdbcUrl += NON_KEYWORDS_SETTING;
        }

        if (makePooled) {
            Properties p = new Properties(System.getProperties());
            p.put("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
            p.put("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
            System.setProperties(p);

            comboPooledDataSource = new ComboPooledDataSource();
            comboPooledDataSource.setDriverClass(DRIVER_CLASS);
            comboPooledDataSource.setJdbcUrl(jdbcUrl);
            if (user != null && password != null) {
                comboPooledDataSource.setUser(user);
                comboPooledDataSource.setPassword(password);
            }
            comboPooledDataSource.setInitialPoolSize(10);
            comboPooledDataSource.setMinPoolSize(5);
            comboPooledDataSource.setAcquireIncrement(5);
            comboPooledDataSource.setMaxPoolSize(30);
            comboPooledDataSource.setMaxStatements(100);
            comboPooledDataSource.setMaxIdleTime(14400); // 4 hours by default

            // comboPooledDataSource.setCheckoutTimeout(2000);
            comboPooledDataSource.setAcquireRetryAttempts(1);
            // comboPooledDataSource.setBreakAfterAcquireFailure(false);
            // TODO remove after debug
            // comboPooledDataSource.setUnreturnedConnectionTimeout(180);

        } else {
            if (user != null && password != null) {
                singleJdbcConn = DriverManager.getConnection(jdbcUrl, user, password);
            } else {
                singleJdbcConn = DriverManager.getConnection(jdbcUrl);
            }
        }
        if (mPrintInfos) {
            String[] dbInfo = getDbInfo();
            Logger.INSTANCE.insertDebug(null, "H2 Version: " + dbInfo[0] + "(" + dbPath + ")");
        }
        return dbExists;
    }

    public Connection getJdbcConnection() throws Exception {
        if (makePooled) {
            if (comboPooledDataSource == null) {
                return null;
            }
            return comboPooledDataSource.getConnection();
        } else {
            return singleJdbcConn;
        }
    }

    public IHMConnection getConnectionInternal() throws Exception {
        Connection jdbcConnection = getJdbcConnection();
        if (jdbcConnection == null) {
            return null;
        }
        return new HMConnection(jdbcConnection, makePooled);
    }

    public void close() throws Exception {
        if (!makePooled) {
            if (singleJdbcConn != null) {
                singleJdbcConn.setAutoCommit(false);
                singleJdbcConn.commit();
                singleJdbcConn.close();
                singleJdbcConn = null;
            }
        } else if (comboPooledDataSource != null) {
            comboPooledDataSource.close();
            comboPooledDataSource = null;
        }
    }

    @Override
    public String getJdbcUrlPre() {
        return EDb.H2.getJdbcPrefix();
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

    public String[] getDbInfo() {
        // checking h2 version
        String sql = "SELECT H2VERSION();";
        try {
            return execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                    String[] info = new String[1];
                    while( rs.next() ) {
                        // read the result set
                        info[0] = rs.getString(1);
                    }
                    return info;
                }
            });
        } catch (Exception e) {
            return new String[]{"no version info available"};
        }
    }

    @Override
    public List<TableName> getTables( ) throws Exception {
        List<TableName> tableNames = new ArrayList<TableName>();
        String sql = """
                SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_TYPE IN ('BASE TABLE', 'VIEW', 'TABLE LINK')
                AND TABLE_SCHEMA <> 'INFORMATION_SCHEMA'
                ORDER BY TABLE_SCHEMA, TABLE_NAME;
                """;

        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String tabelSchema = rs.getString(1);
                    String tableName = rs.getString(2);
                    String tableType = rs.getString(3);
                    ETableType type = ETableType.fromType(tableType);
                    TableName t = new TableName(tableName, tabelSchema, type);
                    tableNames.add(t);
                }
                var metadataTables = H2GisTableNames.metadataTables;
                var internalDataTables = H2GisTableNames.internalDataTables;
                var startsWithIndexTables = H2GisTableNames.startsWithIndexTables;
        
                List<TableName> result = new ArrayList<>();
                for( TableName tableName : tableNames ) {
                    String name = tableName.getName().toLowerCase();
                    if (metadataTables.contains(name)) {
                        continue;
                    }
                    if (internalDataTables.contains(name)) {
                        continue;
                    }
                    if (name.startsWith(startsWithIndexTables)) {
                        continue;
                    }
                    result.add(tableName);
                }
                return result;
            }
        });

    }


    @Override
    public boolean hasTable( SqlName tableName ) throws Exception {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE IN ('BASE TABLE', 'VIEW', 'TABLE LINK')"
                + " AND TABLE_SCHEMA <> 'INFORMATION_SCHEMA'";
        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String name = rs.getString(1);
                    if (name.equalsIgnoreCase(tableName.name)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public ETableType getTableType( SqlName tableName ) throws Exception {
        String sql = "SELECT TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE Lower(TABLE_NAME)=Lower('" + tableName
                + "') AND TABLE_SCHEMA <> 'INFORMATION_SCHEMA'";
        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {

                ETableType type = null;
                while( rs.next() ) {
                    String typeStr = rs.getString(1);
                    ETableType tmp = ETableType.fromType(typeStr);
                    if (type == null || type == ETableType.OTHER) {
                        type = tmp;
                    }
                }
                if (type != null)
                    return type;
            }
            return ETableType.OTHER;
        });
    }

    @Override
    public List<String[]> getTableColumns( SqlName tableName ) throws Exception {
        // select * from information_schema.columns where table_name = 'TEST';
        // [name, type, primarykey]
        String tableNameUpper = tableName.name.toUpperCase();
        String pkSql = "SELECT ic.COLUMN_NAME FROM INFORMATION_SCHEMA.INDEXES i JOIN INFORMATION_SCHEMA.INDEX_COLUMNS ic"
                + " ON ic.INDEX_SCHEMA=i.INDEX_SCHEMA AND ic.INDEX_NAME=i.INDEX_NAME"
                + " WHERE i.INDEX_TYPE_NAME='PRIMARY KEY' AND upper(i.TABLE_NAME) = '" + tableNameUpper + "'";
        String pkName = execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(pkSql)) {
                if (rs.next()) {
                    return rs.getString(1);
                }
                return null;
            }
        });

        String sql = "select COLUMN_NAME, DATA_TYPE from information_schema.columns where upper(table_name) = '" + tableNameUpper
                + "' and TABLE_SCHEMA != 'INFORMATION_SCHEMA' ORDER BY ORDINAL_POSITION";
        return execOnConnection(connection -> {
            List<String[]> colInfo = new ArrayList<>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String colName = rs.getString(1);
                    String typeName = rs.getString(2);
                    String pk = "0";
                    if (pkName != null && colName.equals(pkName)) {
                        pk = "1";
                    }
                    colInfo.add(new String[]{colName, typeName, pk});
                }
                return colInfo;
            }
        });
    }

    @Override
    public List<ForeignKey> getForeignKeys( SqlName tableName ) throws Exception {

        String sql = "SELECT pk.TABLE_NAME, pk.COLUMN_NAME, fk.COLUMN_NAME FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc" //
                + " JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE fk" //
                + " ON fk.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND fk.CONSTRAINT_NAME=rc.CONSTRAINT_NAME" //
                + " JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE pk" //
                + " ON pk.CONSTRAINT_SCHEMA=rc.UNIQUE_CONSTRAINT_SCHEMA AND pk.CONSTRAINT_NAME=rc.UNIQUE_CONSTRAINT_NAME" //
                + " AND pk.ORDINAL_POSITION=fk.POSITION_IN_UNIQUE_CONSTRAINT" //
                + " WHERE upper(fk.TABLE_NAME)='" + tableName.name.toUpperCase() + "'";
        return execOnConnection(connection -> {
            List<ForeignKey> fKeys = new ArrayList<ForeignKey>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    ForeignKey fKey = new ForeignKey();
                    fKey.fromTable = tableName.name;
                    fKey.toTable = rs.getString(1);
                    fKey.to = rs.getString(2);
                    fKey.from = rs.getString(3);
                    fKeys.add(fKey);
                }
            }
            return fKeys;
        });
    }

    @Override
    public List<Index> getIndexes( SqlName tableName ) throws Exception {

        // primary keys and the indexes backing constraints (CONSTRAINT_INDEX_*) are not reported
        String sql = "SELECT i.INDEX_NAME, i.INDEX_TYPE_NAME, ic.COLUMN_NAME FROM INFORMATION_SCHEMA.INDEXES i" //
                + " JOIN INFORMATION_SCHEMA.INDEX_COLUMNS ic ON ic.INDEX_SCHEMA=i.INDEX_SCHEMA AND ic.INDEX_NAME=i.INDEX_NAME" //
                + " WHERE upper(i.TABLE_NAME)='" + tableName.name.toUpperCase() + "'" //
                + " AND i.INDEX_TYPE_NAME <> 'PRIMARY KEY' AND i.INDEX_NAME NOT LIKE 'CONSTRAINT_INDEX%'" //
                + " ORDER BY i.INDEX_NAME, ic.ORDINAL_POSITION";

        return execOnConnection(connection -> {
            Map<String, Index> indexesMap = new LinkedHashMap<>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String indexName = rs.getString(1);
                    String indexType = rs.getString(2);
                    String column = rs.getString(3);

                    Index index = indexesMap.get(indexName);
                    if (index == null) {
                        index = new Index();
                        index.table = tableName.name;
                        index.name = indexName;
                        index.isUnique = "UNIQUE INDEX".equalsIgnoreCase(indexType);
                        indexesMap.put(indexName, index);
                    }
                    index.columns.add(column);
                }
            }
            return new ArrayList<Index>(indexesMap.values());
        });

    }

    @Override
    public void accept( IDbVisitor visitor ) throws Exception {
        visitor.visit(comboPooledDataSource);
        visitor.visit(singleJdbcConn);
    }

}
