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
package org.hortonmachine.dbs.compat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.QueryResult;

/**
 * Abstract non spatial db class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public abstract class ADb implements AutoCloseable {

    protected IHMConnection mConn = null;

    protected String mDbPath;
    
    protected String user = "sa";
    protected String password = "";

    public boolean mPrintInfos = true;

    /**
     * Get the database type.
     * 
     * @return the database type.
     */
    public abstract EDb getType();

    /**
     * Open the connection to a database.
     * 
     * <b>Make sure the connection object is created here.</b>
     * 
     * @param dbPath
     *            the database path. If <code>null</code>, an in-memory db is
     *            created.
     * @return <code>true</code> if the database did already exist.
     * @throws Exception
     */
    public abstract boolean open( String dbPath ) throws Exception;

    /**
     * Set credentials if supported.
     * 
     * <p>To be called before the {@link #open(String)} method.</p>
     * 
     * @param user the username to set or use.
     * @param password the password to set or use.
     */
    public abstract void setCredentials( String user, String password );

    /**
     * @return the path to the database.
     */
    public String getDatabasePath() {
        return mDbPath;
    }

    /**
     * Get the original jdbc connection.
     * 
     * @return the jdbc connection.
     */
    public abstract Connection getJdbcConnection();

    /**
     * Toggle autocommit mode.
     * 
     * @param enable
     *            if <code>true</code>, autocommit is enabled if not already
     *            enabled. Vice versa if <code>false</code>.
     * @throws SQLException
     */
    public void enableAutocommit( boolean enable ) throws Exception {
        boolean autoCommitEnabled = mConn.getAutoCommit();
        if (enable && !autoCommitEnabled) {
            // do enable if not already enabled
            mConn.setAutoCommit(true);
        } else if (!enable && autoCommitEnabled) {
            // disable if not already disabled
            mConn.setAutoCommit(false);
        }
    }

    /**
     * Get database infos.
     * 
     * @return the string array of database version information.
     * @throws SQLException
     */
    public abstract String[] getDbInfo() throws Exception;

    /**
     * Create a new table.
     * 
     * @param tableName
     *            the table name.
     * @param fieldData
     *            the data for each the field (ex. id INTEGER NOT NULL PRIMARY
     *            KEY).
     * @throws SQLException
     */
    public void createTable( String tableName, String... fieldData ) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(tableName).append("(");
        for( int i = 0; i < fieldData.length; i++ ) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(fieldData[i]);
        }
        sb.append(")");

        String sql = sb.toString();
        sql = checkSqlCompatibilityIssues(sql);

        try (IHMStatement stmt = mConn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Check for compatibility issues with different databases.
     * 
     * @param sql the original sql.
     * @return the fixed sql.
     */
    public abstract String checkSqlCompatibilityIssues( String sql );

    /**
     * Create a spatial index.
     * 
     * @param tableName the table name.
     * @param geomColumnName the geometry column name.
     * @throws Exception
     */
    public void createSpatialIndex( String tableName, String geomColumnName ) throws Exception {
        if (geomColumnName == null) {
            geomColumnName = "the_geom";
        }
        String sql = "CREATE SPATIAL INDEX ON " + tableName + "(" + geomColumnName + ");";
        try (IHMStatement stmt = mConn.createStatement()) {
            stmt.execute(sql.toString());
        }
    }

    /**
     * Create an single column index.
     * 
     * @param tableName
     *            the table.
     * @param column
     *            the column.
     * @param isUnique
     *            if <code>true</code>, a unique index will be created.
     * @throws Exception
     */
    public void createIndex( String tableName, String column, boolean isUnique ) throws Exception {
        String sql = getIndexSql(tableName, column, isUnique);
        try (IHMStatement stmt = mConn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            String message = e.getMessage();
            if (message.contains("index") && message.contains("already exists")) {
                logWarn(message);
            } else {
                e.printStackTrace();
            }
        }
    }

    /**
     * Get the sql to create an index.
     * 
     * @param tableName
     *            the table.
     * @param column
     *            the column.
     * @param isUnique
     *            if <code>true</code>, a unique index will be created.
     * @return the index sql.
     */
    public String getIndexSql( String tableName, String column, boolean isUnique ) {
        String unique = "UNIQUE ";
        if (!isUnique) {
            unique = "";
        }
        String indexName = tableName + "__" + column + "_idx";
        String sql = "CREATE " + unique + "INDEX " + indexName + " on " + tableName + "(" + column + ");";
        return sql;
    }

    /**
     * Get the list of available tables.
     * 
     * @param doOrder
     *            if <code>true</code>, the names are ordered.
     * @return the list of names.
     * @throws Exception
     */
    public abstract List<String> getTables( boolean doOrder ) throws Exception;

    /**
     * Checks if the table is available.
     * 
     * @param tableName
     *            the name of the table.
     * @return <code>true</code> if the table exists.
     * @throws Exception
     */
    public abstract boolean hasTable( String tableName ) throws Exception;

    /**
     * Gets the table type.
     * 
     * @param tableName
     *            the name of the table.
     * @return the table type.
     * @throws Exception
     */
    public abstract ETableType getTableType( String tableName ) throws Exception;

    /**
     * Get the column [name, type, primarykey] values of a table.
     * 
     * <p>pk = 0 -> false</p>
     * 
     * @param tableName
     *            the table to check.
     * @return the list of column [name, type, pk].
     * @throws SQLException
     */
    public abstract List<String[]> getTableColumns( String tableName ) throws Exception;

    /**
     * Get the foreign keys from a table.
     * 
     * @param tableName
     *            the table to check on.
     * @return the list of keys.
     * @throws Exception
     */
    public abstract List<ForeignKey> getForeignKeys( String tableName ) throws Exception;

    /**
     * Get the record count of a table.
     * 
     * @param tableName
     *            the name of the table.
     * @return the record count or -1.
     * @throws Exception
     */
    public long getCount( String tableName ) throws Exception {
        String sql = "select count(*) from " + tableName;
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                long count = rs.getLong(1);
                return count;
            }
            return -1;
        }
    }

    /**
     * Execute a query from raw sql.
     * 
     * @param sql
     *            the sql to run.
     * @param limit
     *            a limit, ignored if < 1
     * @return the resulting records.
     * @throws Exception
     */
    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws Exception {
        QueryResult queryResult = new QueryResult();
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            IHMResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
            }
            long start = System.currentTimeMillis();
            int count = 0;
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    Object object = rs.getObject(j);
                    rec[j - 1] = object;
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
            try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for( int i = 1; i <= columnCount; i++ ) {
                    if (i > 1) {
                        bw.write(separator);
                    }
                    String columnName = rsmd.getColumnName(i);
                    bw.write(columnName);
                }
                bw.write("\n");
                while( rs.next() ) {
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (j > 1) {
                            bw.write(separator);
                        }
                        Object object = rs.getObject(j);
                        if (object != null) {
                            bw.write(object.toString());
                        } else {
                            bw.write("");
                        }
                    }
                    bw.write("\n");
                }
            }
        }
    }

    /**
     * Execute an update, insert or delete by sql.
     * 
     * @param sql
     *            the sql to run.
     * @return the result code of the update.
     * @throws Exception
     */
    public int executeInsertUpdateDeleteSql( String sql ) throws Exception {
        try (IHMStatement stmt = mConn.createStatement()) {
            int executeUpdate = stmt.executeUpdate(sql);
            return executeUpdate;
        }
    }

    public int executeInsertUpdateDeletePreparedSql( String sql, Object[] objects ) throws Exception {
        try (IHMPreparedStatement stmt = mConn.prepareStatement(sql)) {
            for( int i = 0; i < objects.length; i++ ) {
                if (objects[i] instanceof Boolean) {
                    stmt.setBoolean(i + 1, (boolean) objects[i]);
                } else if (objects[i] instanceof byte[]) {
                    stmt.setBytes(i + 1, (byte[]) objects[i]);
                } else if (objects[i] instanceof Double) {
                    stmt.setDouble(i + 1, (double) objects[i]);
                } else if (objects[i] instanceof Float) {
                    stmt.setFloat(i + 1, (float) objects[i]);
                } else if (objects[i] instanceof Integer) {
                    stmt.setInt(i + 1, (int) objects[i]);
                } else if (objects[i] instanceof Long) {
                    stmt.setLong(i + 1, (long) objects[i]);
                } else if (objects[i] instanceof Short) {
                    stmt.setShort(i + 1, (short) objects[i]);
                } else if (objects[i] instanceof String) {
                    stmt.setString(i + 1, (String) objects[i]);
                } else {
                    stmt.setString(i + 1, objects[i].toString());
                }
            }
            int executeUpdate = stmt.executeUpdate();
            return executeUpdate;
        }
    }

    /**
     * @return the connection to the database.
     */
    public IHMConnection getConnection() {
        return mConn;
    }

    public void close() throws Exception {
        if (mConn != null) {
            mConn.setAutoCommit(false);
            mConn.commit();
            mConn.close();
        }
    }

    /**
     * Escape sql.
     * 
     * @param sql
     *            the sql code to escape.
     * @return the escaped sql.
     */
    public static String escapeSql( String sql ) {
        // ' --> ''
        sql = sql.replaceAll("'", "''");
        // " --> ""
        sql = sql.replaceAll("\"", "\"\"");
        // \ --> (remove backslashes)
        sql = sql.replaceAll("\\\\", "");
        return sql;
    }

    /**
     * Composes the formatter for unix timstamps in queries.
     * 
     * <p>
     * The default format is: <b>2015-06-11 03:14:51</b>, as given by pattern:
     * <b>%Y-%m-%d %H:%M:%S</b>.
     * </p>
     * 
     * @param columnName
     *            the timestamp column in the db.
     * @param datePattern
     *            the datepattern.
     * @return the query piece.
     */
    public static String getTimestampQuery( String columnName, String datePattern ) {
        if (datePattern == null)
            datePattern = "%Y-%m-%d %H:%M:%S";
        String sql = "strftime('" + datePattern + "', " + columnName + " / 1000, 'unixepoch')";
        return sql;
    }

    protected abstract void logWarn( String message );

    protected abstract void logInfo( String message );

    protected abstract void logDebug( String message );

}