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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.hm.HMConnection;

/**
 * An H2 database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class H2Db extends ADb {
    private Connection jdbcConn;

    static {
        try {
            Class.forName("org.h2.Driver");
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

    public boolean open( String dbPath ) throws Exception {
        this.mDbPath = dbPath;

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
            }
        } else {
            dbPath = "mem:syntax";
            dbExists = false;
        }

        String jdbcUrl = "jdbc:h2:" + dbPath;
        if (user != null && password != null) {
            jdbcConn = DriverManager.getConnection(jdbcUrl, user, password);
        } else {
            jdbcConn = DriverManager.getConnection(jdbcUrl);
        }
        mConn = new HMConnection(jdbcConn);
        if (mPrintInfos) {
            String[] dbInfo = getDbInfo();
            Logger.INSTANCE.insertDebug(null, "H2 Version: " + dbInfo[0] + "(" + dbPath + ")");
        }
        return dbExists;
    }

    public Connection getJdbcConnection() {
        return jdbcConn;
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

    public String[] getDbInfo() throws Exception {
        // checking h2 version
        String sql = "SELECT H2VERSION();";
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            String[] info = new String[1];
            while( rs.next() ) {
                // read the result set
                info[0] = rs.getString(1);
            }
            return info;
        }
    }

    public String checkSqlCompatibilityIssues( String sql ) {

        String lowerCase = sql.toLowerCase().trim();
        if (lowerCase.startsWith("create table")) {
            sql = sql.replaceAll("AUTOINCREMENT", "AUTO_INCREMENT");
            sql = sql.replaceAll("TEXT", "varchar(255)");
        }

        return sql;
    }

    @Override
    public List<String> getTables( boolean doOrder ) throws Exception {
        List<String> tableNames = new ArrayList<String>();
        String orderBy = " ORDER BY TABLE_NAME";
        if (!doOrder) {
            orderBy = "";
        }
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='TABLE' or TABLE_TYPE='VIEW' or TABLE_TYPE='EXTERNAL'"
                + orderBy;
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String tabelName = rs.getString(1);
                tableNames.add(tabelName);
            }
            return tableNames;
        }
    }

    @Override
    public boolean hasTable( String tableName ) throws Exception {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE='TABLE' or TABLE_TYPE='VIEW' or TABLE_TYPE='EXTERNAL'";
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                String name = rs.getString(1);
                if (name.equalsIgnoreCase(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public ETableType getTableType( String tableName ) throws Exception {
        String sql = "SELECT TABLE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE Lower(TABLE_NAME)=Lower('" + tableName + "')";
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {

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
    }

    @Override
    public List<String[]> getTableColumns( String tableName ) throws Exception {
        // select * from information_schema.columns where table_name = 'TEST';
        // [name, type, primarykey]
        String tableNameUpper = tableName.toUpperCase();
        String pkSql = "select c.COLUMN_NAME from information_schema.columns c , information_schema.indexes i"
                + " where  upper(c.table_name) = '" + tableNameUpper + "' and upper(i.table_name) = '" + tableNameUpper + "'"
                + " and c.COLUMN_NAME=i.COLUMN_NAME and i.PRIMARY_KEY = true";
        String pkName = null;
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(pkSql)) {
            if (rs.next()) {
                pkName = rs.getString(1);
            }
        }

        List<String[]> colInfo = new ArrayList<>();
        String sql = "select COLUMN_NAME, TYPE_NAME from information_schema.columns where upper(table_name) = '" + tableNameUpper
                + "' and TABLE_SCHEMA != 'INFORMATION_SCHEMA'";
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
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
    }

    @Override
    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        List<ForeignKey> fKeys = new ArrayList<ForeignKey>();

        String sql = "SELECT PKTABLE_NAME, PKCOLUMN_NAME, FKCOLUMN_NAME FROM INFORMATION_SCHEMA.CROSS_REFERENCES where upper(FKTABLE_NAME)='"
                + tableName.toUpperCase() + "'";
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                ForeignKey fKey = new ForeignKey();
                fKey.fromTable = tableName;
                fKey.toTable = rs.getString(1);
                fKey.to = rs.getString(2);
                fKey.from = rs.getString(3);
                fKeys.add(fKey);
            }
        }
        return fKeys;
    }

    @Override
    public List<Index> getIndexes( String tableName ) throws Exception {

        String sql = "SELECT INDEX_NAME, sql FROM information_schema.indexes where upper(TABLE_NAME)='" + tableName.toUpperCase()
                + "'";

        List<Index> indexes = new ArrayList<Index>();
        try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                Index index = new Index();

                String indexName = rs.getString(1);

                index.table = tableName;
                index.name = indexName;

                String createSql = rs.getString(2);
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
    }

}
