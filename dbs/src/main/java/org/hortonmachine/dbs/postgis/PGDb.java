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
package org.hortonmachine.dbs.postgis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.hm.HMConnection;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * An postgresql database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PGDb extends ADb {
    private static final String WORKING_SCHEMA = "public"; // TODO add schema support in future
    private static final String DRIVER_CLASS = "org.postgresql.Driver";
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
        return EDb.POSTGRES;
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

        boolean dbExists = true;

        String jdbcUrl = EDb.POSTGRES.getJdbcPrefix() + dbPath;

        if (makePooled) {
            Properties p = new Properties(System.getProperties());
            p.put("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
            p.put("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF"); // Off or any
                                                                                  // other level
            System.setProperties(p);

//            testConnectionOnCheckin validates the connection when it is returned to the pool. 
//            idleConnectionTestPeriod sets a limit to how long a connection will stay idle before testing it.
//            maxIdleTimeExcessConnections will bring back the connectionCount back down to minPoolSize after a spike in activity.

            comboPooledDataSource = new ComboPooledDataSource();
            comboPooledDataSource.setDriverClass(DRIVER_CLASS);
            comboPooledDataSource.setJdbcUrl(jdbcUrl);
            if (user != null && password != null) {
                comboPooledDataSource.setUser(user);
                comboPooledDataSource.setPassword(password);
            }
            comboPooledDataSource.setMinPoolSize(5);
            comboPooledDataSource.setMaxPoolSize(30);
            comboPooledDataSource.setAcquireIncrement(1);
            comboPooledDataSource.setInitialPoolSize(10);
            comboPooledDataSource.setMaxStatements(100);
            comboPooledDataSource.setTestConnectionOnCheckin(true);
            comboPooledDataSource.setIdleConnectionTestPeriod(300);
            comboPooledDataSource.setMaxIdleTimeExcessConnections(240);

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
            Logger.INSTANCE.insertDebug(null, "Postgresql Version: " + dbInfo[0] + "(" + dbPath + ")");
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
        return EDb.POSTGRES.getJdbcPrefix();
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
        // checking postgresql version
        String sql = "SELECT version();";
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
    public List<String> getTables( boolean doOrder ) throws Exception {
        List<String> tableNames = new ArrayList<String>();
        String orderBy = " ORDER BY TABLE_NAME";
        if (!doOrder) {
            orderBy = "";
        }
        String sql = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES  " + //
                "WHERE (TABLE_TYPE='BASE TABLE' or TABLE_TYPE='VIEW' or TABLE_TYPE='EXTERNAL') " + //
                "and table_schema='public' " + //
                "and table_name != 'geography_columns' " + //
                "and table_name != 'geometry_columns' " + //
                "and table_name != 'spatial_ref_sys' " + //
                "and table_name != 'raster_columns' " + //
                "and table_name != 'raster_overviews'" + orderBy;

        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String tabelName = rs.getString(1);
                    tableNames.add(tabelName);
                }
                return tableNames;
            }
        });
    }

    @Override
    public boolean hasTable( String tableName ) throws Exception {
        String sql = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE (TABLE_TYPE='BASE TABLE' or TABLE_TYPE='VIEW' or TABLE_TYPE='EXTERNAL') " + "and table_schema='"
                + WORKING_SCHEMA + "' and upper(table_name) = upper('" + tableName + "')";
        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String name = rs.getString(1);
                    if (name.equalsIgnoreCase(tableName)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    public ETableType getTableType( String tableName ) throws Exception {
        String sql = "SELECT TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE Lower(TABLE_NAME)=Lower('" + tableName
                + "') and table_Schema!='information_schema'";
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
    public List<String[]> getTableColumns( String tableName ) throws Exception {
        // [name, type, primarykey]
        String pkSql = getIndexSql(tableName);

        String pkName = execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(pkSql)) {
                while( rs.next() ) {
                    String indexName = rs.getString(3);
                    if (indexName.toLowerCase().contains("pkey")) {
                        String columnDef = rs.getString(4);
                        String column = columnDef.split("\\s+")[0];
                        return column;
                    }
                }
                return null;
            }
        });

        String sql = "select column_name, data_type from information_schema.columns where upper(table_name)=upper('" + tableName
                + "') and table_Schema!='information_schema'";
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
    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        String sql = "SELECT " + //
                "    tc.table_name, kcu.column_name, " + //
                "    ccu.table_name AS foreign_table_name, " + //
                "    ccu.column_name AS foreign_column_name " + //
                "FROM " + //
                "    information_schema.table_constraints AS tc " + //
                "    JOIN information_schema.key_column_usage  " + //
                "        AS kcu ON tc.constraint_name = kcu.constraint_name " + //
                "    JOIN information_schema.constraint_column_usage  " + //
                "        AS ccu ON ccu.constraint_name = tc.constraint_name " + //
                "WHERE constraint_type = 'FOREIGN KEY' and upper(tc.table_name)=upper('" + tableName + "')";

        return execOnConnection(connection -> {
            List<ForeignKey> fKeys = new ArrayList<ForeignKey>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    ForeignKey fKey = new ForeignKey();
                    fKey.fromTable = tableName;
                    fKey.from = rs.getString(2);
                    fKey.toTable = rs.getString(3);
                    fKey.to = rs.getString(4);
                    fKeys.add(fKey);
                }
            }
            return fKeys;
        });
    }

    @Override
    public List<Index> getIndexes( String tableName ) throws Exception {

        String sql = getIndexSql(tableName);

        return execOnConnection(connection -> {
            List<Index> indexes = new ArrayList<Index>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    Index index = new Index();

//                    String schemaName = rs.getString(1);
//                    String tableName = rs.getString(2);
                    String indexName = rs.getString(3);
                    String createSql = rs.getString(5);

                    index.table = tableName;
                    index.name = indexName;

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

    private String getIndexSql( String tableName ) {
        return "SELECT  tnsp.nspname AS schema_name,   trel.relname AS table_name,   irel.relname AS index_name,   " + //
                " a.attname    || ' ' || CASE o.option & 1 WHEN 1 THEN 'DESC' ELSE 'ASC' END   || ' ' || CASE  " + //
                " o.option & 2 WHEN 2 THEN 'NULLS FIRST' ELSE 'NULLS LAST' END   AS column, " + //
                " pi.indexdef " + //
                " FROM pg_index AS i JOIN pg_class AS trel ON trel.oid = i.indrelid JOIN pg_namespace AS tnsp  " + //
                " ON trel.relnamespace = tnsp.oid JOIN pg_class AS irel ON irel.oid = i.indexrelid CROSS JOIN LATERAL " + //
                "  unnest (i.indkey) WITH ORDINALITY AS c (colnum, ordinality) LEFT JOIN LATERAL unnest (i.indoption)  " + //
                "  WITH ORDINALITY AS o (option, ordinality)   ON c.ordinality = o.ordinality JOIN  " + //
                "  pg_attribute AS a ON trel.oid = a.attrelid AND  " + //
                "  a.attnum = c.colnum , " + //
                "  pg_indexes pi " + //
                "  where pi.indexname=irel.relname " + //
                "  and upper(trel.relname)=upper('" + tableName + "')";
    }

    @Override
    public void accept( IDbVisitor visitor ) throws Exception {
        visitor.visit(comboPooledDataSource);
        visitor.visit(singleJdbcConn);
    }

    /**
     * Get the list of databases.
     * 
     * @param host the host.
     * @param port the port.
     * @param existingDb an existing db to lean on. If null, "postgres" will be used.
     * @param user optional user.
     * @param pwd optional pwd.
     * @return the list of available databases.
     * @throws Exception
     */
    public static List<String> getDatabases( String host, String port, String existingDb, String user, String pwd )
            throws Exception {
        if (existingDb == null) {
            existingDb = "postgres";
        }
        String url = EDb.POSTGRES.getJdbcPrefix() + host + ":" + port + "/" + existingDb;
        try (Connection connection = DriverManager.getConnection(url, user, pwd)) {

            String sql = "SELECT datname FROM pg_database WHERE datistemplate = false;";

            List<String> dbs = new ArrayList<>();
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String dbName = rs.getString(1);
                    dbs.add(dbName);
                }
                return dbs;
            }
        }
    }
    
    /**
     * Get the list of databases.
     * 
     * @param db the db to use as query base.
     * @return the list of dbs.
     * @throws Exception
     */
    public static List<String> getDatabases( ADb db ) throws Exception {
        List<String> dbs = new ArrayList<>();
        String sql = "SELECT datname FROM pg_database WHERE datistemplate = false;";
        db.execOnResultSet(sql, resSet -> {
            while (resSet.next()) {
                String dbName = resSet.getString(1);
                dbs.add(dbName);
            }
            return null;
        });

        return dbs;
    }

}
