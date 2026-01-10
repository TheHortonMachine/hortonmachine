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
import java.util.HashMap;
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
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * An postgresql database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PGDb extends ADb {
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

            comboPooledDataSource = new ComboPooledDataSource();
            comboPooledDataSource.setDriverClass(DRIVER_CLASS);
            comboPooledDataSource.setJdbcUrl(jdbcUrl);
            if (user != null && password != null) {
                comboPooledDataSource.setUser(user);
                comboPooledDataSource.setPassword(password);
            }
            comboPooledDataSource.setMinPoolSize(minSize);
            comboPooledDataSource.setMaxPoolSize(maxSize);
            comboPooledDataSource.setInitialPoolSize(5);
            // The pool will acquire one connection at a time when it 
            // needs more connections.
            comboPooledDataSource.setAcquireIncrement(1);
            // This sets the size of the global PreparedStatement cache. 
            // This setting is useful for caching prepared statements.
            comboPooledDataSource.setMaxStatements(100);
            // This setting ensures that connections are tested for 
            // validity when they are returned to the pool.
            comboPooledDataSource.setTestConnectionOnCheckin(true);
            
            // Specifies the maximum time (in seconds) a connection can 
            // sit idle in the pool before being discarded. 
            // This helps in closing idle connections.
            comboPooledDataSource.setMaxIdleTime(300);
            // Specifies the maximum time (in seconds) excess connections 
            // (connections above the minimum pool size) can sit idle in 
            // the pool before being discarded.
            comboPooledDataSource.setMaxIdleTimeExcessConnections(180);
            // Specifies how frequently (in seconds) idle connections are 
            // tested to ensure they are still valid.
            comboPooledDataSource.setIdleConnectionTestPeriod(60);
            // specifies that the connection pool should attempt to acquire a new 
            // connection only once if the initial attempt fails.
            comboPooledDataSource.setAcquireRetryAttempts(3);
            // This setting specifies the maximum number of milliseconds that a client
            // will wait for a connection to be checked out from the pool. If a connection
            // is not available within this time, an exception will be thrown.
            // Setting a checkout timeout can help prevent your application from hanging
            // indefinitely if a connection cannot be acquired. If the timeout is set too low,
            // you might encounter exceptions during high load periods.
            comboPooledDataSource.setCheckoutTimeout(2000);
            // This setting specifies whether the pool should continue to attempt to acquire
            // a new connection after a failure.
            comboPooledDataSource.setBreakAfterAcquireFailure(false);
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
    public List<TableName> getTables() throws Exception {
        List<TableName> tableNames = new ArrayList<TableName>();
        String sql = """
                    SELECT table_name, table_schema, table_type 
                    FROM INFORMATION_SCHEMA.TABLES  
                    WHERE (TABLE_TYPE='BASE TABLE' OR TABLE_TYPE='VIEW' OR TABLE_TYPE='EXTERNAL')
                    AND table_schema NOT IN ('pg_catalog', 'information_schema') 
                    ORDER BY table_schema, table_name;
                """;

        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String tabelName = rs.getString(1);
                    var schemaName = rs.getString(2);
                    var tableType = rs.getString(3);
                    var tt = ETableType.fromType(tableType);
                    if("spatial_ref_sys".equalsIgnoreCase(tabelName)) {
                        continue;
                    }
                    tableNames.add(new TableName(tabelName, schemaName, tt));
                }
                return tableNames;
            }
        });
    }

    @Override
    public boolean hasTable( SqlName tableName ) throws Exception {
        String sql = "SELECT table_name FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE (TABLE_TYPE='BASE TABLE' or TABLE_TYPE='VIEW' or TABLE_TYPE='EXTERNAL') "+
                " and upper(table_name) = upper('" + tableName.name + "')";
        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    String name = rs.getString(1);
                    if (name.equalsIgnoreCase(tableName.name)) {
                        return true;
                    }
                }
            }
            return false;
        });
    }

    public ETableType getTableType( SqlName tableName ) throws Exception {
        String sql = "SELECT TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE Lower(TABLE_NAME)=Lower('" + tableName.name
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
    public List<String[]> getTableColumns( SqlName tableName ) throws Exception {
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

        String sql = "select column_name, data_type from information_schema.columns where upper(table_name)=upper('"
                + tableName.name + "') and table_Schema!='information_schema'";
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
                "WHERE constraint_type = 'FOREIGN KEY' and upper(tc.table_name)=upper('" + tableName.name + "')";

        return execOnConnection(connection -> {
            List<ForeignKey> fKeys = new ArrayList<ForeignKey>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    ForeignKey fKey = new ForeignKey();
                    fKey.fromTable = tableName.name;
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
    public List<Index> getIndexes( SqlName tableName ) throws Exception {

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

                    index.table = tableName.name;
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

    private String getIndexSql( SqlName tableName ) {
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
                "  and upper(trel.relname)=upper('" + tableName.name + "')";
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
            while( resSet.next() ) {
                String dbName = resSet.getString(1);
                dbs.add(dbName);
            }
            return null;
        });

        return dbs;
    }

}
