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
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.HMConnectionConsumer;
import org.hortonmachine.dbs.utils.HMResultSetConsumer;

/**
 * Abstract non spatial db class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public abstract class ADb implements AutoCloseable, IVisitableDb {

    protected String mDbPath;

    protected String user = null;
    protected String password = null;

    /**
     * Defines if the connection should be pooled. True by default.
     */
    protected boolean makePooled = true;
    public boolean mPrintInfos = false;

    /**
     * Get the database type.
     * 
     * @return the database type.
     */
    public abstract EDb getType();

    /**
     * Set the pooled behavior.
     * 
     * <p>To be called before the {@link #open(String)} method.</p>
     * 
     * @param makePooled if false, the connection will not be pooled.
     */
    public void setMakePooled( boolean makePooled ) {
        this.makePooled = makePooled;
    }

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
     * Open the connection to a database.
     * 
     * <b>Make sure the connection object is created here.</b>
     * 
     * @param dbPath
     *            the database path. If <code>null</code>, an in-memory db is
     *            created.
     * @param user the user to use.
     * @param password the passord to use.
     * @return <code>true</code> if the database did already exist.
     * @throws Exception
     */
    public abstract boolean open( String dbPath, String user, String password ) throws Exception;

    /**
     * Getter for the {@link ConnectionData} that can be used to connect to the database.
     * 
     * @return the connection data.
     */
    public abstract ConnectionData getConnectionData();

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
     * Returns the path to the database. 
     * 
     * <p>In case of non file based databases, this will be the part after the {@link #getJdbcUrlPre()}.
     * 
     * @return the path to the database.
     */
    public String getDatabasePath() {
        return mDbPath;
    }

    /**
     * Get the jdbc url pre string for the database (ex.  jdbc:h2:).
     * 
     * @return the jdbc url pre string..
     */
    public abstract String getJdbcUrlPre();

    /**
     * Get the original jdbc connection.
     * 
     * @return the jdbc connection.
     * @throws Exception 
     */
    public abstract Connection getJdbcConnection() throws Exception;

    /**
     * @return the connection to the database.
     * @throws Exception 
     */
    public abstract IHMConnection getConnectionInternal() throws Exception;

    /**
     * Get database infos.
     * 
     * @return the string array of database version information.
     */
    public abstract String[] getDbInfo();

    /**
     * Execute an operation on a database connection. This handles proper releasing of the connection.
     * 
     * @param consumer the operation to perform.
     * @throws Exception
     */
    public <T> T execOnConnection( HMConnectionConsumer<IHMConnection, Exception, T> consumer ) throws Exception {
        IHMConnection connection = getConnectionInternal();
        if (connection == null) {
            return null;
        }
        try {
            return consumer.execOnConnection(connection);
        } finally {
            connection.release();
        }
    }

    /**
     * Execute an operation to get a resultset. This handles proper releasing of the resultset.
     * 
     * @param consumer the operation to perform.
     * @throws Exception
     */
    public <T> T execOnResultSet( String sql, HMResultSetConsumer<IHMResultSet, Exception, T> consumer ) throws Exception {
        IHMConnection connection = getConnectionInternal();
        if (connection == null) {
            return null;
        }

        try (IHMStatement statement = connection.createStatement()) {
            IHMResultSet resultSet = statement.executeQuery(sql);
            return consumer.execOnResultSet(resultSet);
        } finally {
            connection.release();
        }
    }

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

        String sql = getType().getDatabaseSyntaxHelper().checkSqlCompatibilityIssues(sb.toString());

        execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
            return null;
        });
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

        execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.executeUpdate(sql);
            } catch (SQLException e) {
                String message = e.getMessage();
                if (message.contains("index") && message.contains("already exists")) {
                    logWarn(message);
                } else {
                    e.printStackTrace();
                }
            }
            return null;
        });
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
     * Get the indexes of a table (no primary keys and no foreign keys).
     * 
     * @param tableName
     *            the table to check on.
     * @return the list of indexes.
     * @throws Exception
     */
    public abstract List<Index> getIndexes( String tableName ) throws Exception;

    /**
     * Get the record count of a table.
     * 
     * @param tableName
     *            the name of the table.
     * @return the record count or -1.
     * @throws Exception
     */
    public long getCount( String tableName ) throws Exception {
        String sql = "select count(*) from " + DbsUtilities.fixTableName(tableName);
        Long count = execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return -1l;
        });
        return count;
    }

    /**
     * Get the max value of a long/int field.
     * 
     * @param tableName the name of the table.
     * @param fieldName the name of the field.
     * @return the current max value.
     * @throws Exception
     */
    public long getMax( String tableName, String fieldName ) throws Exception {
        String sql = "select max(" + fieldName + ") from " + DbsUtilities.fixTableName(tableName);
        Long max = execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return -1l;
        });
        return max;
    }

    /**
     * Get a single long field as query result.
     * 
     * @param sql the query to run.
     * @return the long.
     * @throws Exception if the query doesn't return a long
     */
    public Long getLong( String sql ) throws Exception {
        Long resultingLong = execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            return null;
        });
        return resultingLong;
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
        QueryResult queryResult = execOnConnection(connection -> {
            QueryResult res = new QueryResult();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for( int i = 1; i <= columnCount; i++ ) {
                    String columnName = rsmd.getColumnName(i);
                    res.names.add(columnName);
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    res.types.add(columnTypeName);
                }
                long start = System.currentTimeMillis();
                int count = 0;
                while( rs.next() ) {
                    Object[] rec = new Object[columnCount];
                    for( int j = 1; j <= columnCount; j++ ) {
                        Object object = rs.getObject(j);
                        rec[j - 1] = object;
                    }
                    res.data.add(rec);
                    if (limit > 0 && ++count > (limit - 1)) {
                        break;
                    }
                }
                long end = System.currentTimeMillis();
                res.queryTimeMillis = end - start;
            }
            return res;
        });
        return queryResult;
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
            execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
                return null;
            });
        }
    }

    /**
     * Execute a select sql without any return.
     * 
     * @param sql
     *            the sql to run.
     * @throws Exception
     */
    public void executeSelect( String sql ) throws Exception {
        execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                return stmt.executeQuery(sql);
            }
        });
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
        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                return stmt.executeUpdate(sql);
            }
        });
    }

    public int executeInsertUpdateDeletePreparedSql( String sql, Object[] objects ) throws Exception {
        return execOnConnection(connection -> {
            try (IHMPreparedStatement stmt = connection.prepareStatement(sql)) {
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
                        stmt.setObject(i + 1, objects[i]);
                    }
                }
                return stmt.executeUpdate();
            }
        });
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
     * Get the name of a column in the proper case.
     * 
     * @param tableName the table to check.
     * @param columnName the column name to check. 
     * @return the name found in the columns list of the table.
     * @throws Exception
     */
    public String getProperColumnNameCase( String tableName, String columnName ) throws Exception {
        List<String[]> tableColumns = getTableColumns(tableName);
        String realName = columnName;
        for( String[] cols : tableColumns ) {
            if (cols[0].equalsIgnoreCase(columnName)) {
                realName = cols[0];
                break;
            }
        }
        return realName;
    }

    /**
     * Get the table name in the proper case.
     * 
     * @param tableName the table to check.
     * @return
     * @throws Exception
     */
    public String getProperTableNameCase( String tableName ) throws Exception {
        List<String> tables = getTables(false);
        String realName = tableName;
        for( String name : tables ) {
            if (name.equalsIgnoreCase(tableName)) {
                realName = name;
                break;
            }
        }
        return realName;
    }

    /**
     * Add new columns to a table if they are not there.
     * 
     * @param tableName
     *            the name of the table.
     * @param columnToAdd
     *            the column name to add.
     * @param typeToAdd
     *            the type of the column to add.
     * @throws Exception 
     */
    public void addNewColumn( String tableName, String columnToAdd, String typeToAdd ) throws Exception {
        if (hasTable(tableName)) {
            List<String[]> tableColumns = getTableColumns(tableName);
            List<String> tableColumnsFirst = new ArrayList<String>();
            for( String[] tc : tableColumns ) {
                tableColumnsFirst.add(tc[0].toLowerCase());
            }

            if (!tableColumnsFirst.contains(columnToAdd.toLowerCase())) {
                String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnToAdd + " " + typeToAdd + ";";
                executeInsertUpdateDeleteSql(sql);
            }
        }

    }

    protected abstract void logWarn( String message );

    protected abstract void logInfo( String message );

    protected abstract void logDebug( String message );

}