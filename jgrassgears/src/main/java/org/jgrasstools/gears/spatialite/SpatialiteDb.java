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
package org.jgrasstools.gears.spatialite;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.utils.OsCheck;
import org.jgrasstools.gears.utils.OsCheck.OSType;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.sqlite.SQLiteConfig;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * A spatialite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SpatialiteDb implements AutoCloseable {

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static final String PK_UID = "PK_UID";

    public static final String defaultGeomFieldName = "the_geom";

    protected Connection conn = null;

    private String dbPath;

    public boolean printInfos = true;

    /**
     * Open the connection to a database.
     * 
     * @param dbPath the database path.
     * @return <code>true</code> if the database did already exist.
     * @throws SQLException
     */
    public boolean open( String dbPath ) throws Exception {
        this.dbPath = dbPath;

        boolean dbExists = false;
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            if (printInfos)
                System.out.println("Database exists");
            dbExists = true;
        }
        // enabling dynamic extension loading
        // absolutely required by SpatiaLite
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        // create a database connection
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath, config.toProperties());
        if (printInfos)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SELECT sqlite_version()");
                ResultSet rs = stmt.executeQuery("SELECT sqlite_version() AS 'SQLite Version';");
                while( rs.next() ) {
                    String sqliteVersion = rs.getString(1);
                    System.out.println("SQLite Version: " + sqliteVersion);
                }
            }
        try (Statement stmt = conn.createStatement()) {
            // set timeout to 30 sec.
            stmt.setQueryTimeout(30);
            // load SpatiaLite
            try {
                OSType operatingSystemType = OsCheck.getOperatingSystemType();
                switch( operatingSystemType ) {
                case Linux:
                case MacOS:
                    try {
                        stmt.execute("SELECT load_extension('mod_rasterlite2.so', 'sqlite3_modrasterlite_init')");
                    } catch (Exception e) {
                        if (printInfos) {
                            System.out.println("Unable to load mod_rasterlite2.so: " + e.getMessage());
                        }
                    }
                    try {
                        stmt.execute("SELECT load_extension('mod_spatialite.so', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (printInfos) {
                            System.out.println("Unable to load mod_spatialite.so: " + e.getMessage());
                        }
                        throw e;
                    }
                    break;
                default:
                    try {
                        stmt.execute("SELECT load_extension('mod_rasterlite2', 'sqlite3_modrasterlite_init')");
                    } catch (Exception e) {
                        if (printInfos) {
                            System.out.println("Unable to load mod_rasterlite2: " + e.getMessage());
                        }
                    }
                    try {
                        stmt.execute("SELECT load_extension('mod_spatialite', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (printInfos) {
                            System.out.println("Unable to load mod_spatialite: " + e.getMessage());
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
        return dbExists;
    }

    /**
     * @return the path to the database. 
     */
    public String getDatabasePath() {
        return dbPath;
    }

    /**
     * Create Spatial Metadata initialize SPATIAL_REF_SYS and GEOMETRY_COLUMNS.
     * 
     * @param options optional tweaks.
     * @throws SQLException
     */
    public void initSpatialMetadata( String options ) throws SQLException {
        if (options == null) {
            options = "";
        }
        enableAutocommit(false);
        String sql = "SELECT InitSpatialMetadata(" + options + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        enableAutocommit(true);
    }

    /**
     * Toggle autocommit mode.
     * 
     * @param enable if <code>true</code>, autocommit is enabled if not already enabled.
     *          Vice versa if <code>false</code>.
     * @throws SQLException
     */
    public void enableAutocommit( boolean enable ) throws SQLException {
        boolean autoCommitEnabled = conn.getAutoCommit();
        if (enable && !autoCommitEnabled) {
            // do enable if not already enabled
            conn.setAutoCommit(true);
        } else if (!enable && autoCommitEnabled) {
            // disable if not already disabled
            conn.setAutoCommit(false);
        }
    }

    /**
     * Get database infos.
     * 
     * @return the string array of [sqlite_version, spatialite_version, spatialite_target_cpu]
     * @throws SQLException
     */
    public String[] getDbInfo() throws SQLException {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT sqlite_version(), spatialite_version(), spatialite_target_cpu()";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
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

    /**
     * Create a new table.
     * 
     * @param tableName the table name.
     * @param fieldData the data for each the field (ex. id INTEGER NOT NULL PRIMARY KEY).
     * @throws SQLException
     */
    public void createTable( String tableName, String... fieldData ) throws SQLException {
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

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sb.toString());
        }
    }

    /**
     * Delete a geo-table with all attached indexes and stuff.
     * 
     * @param tableName
     * @throws SQLException
     */
    public void deleteGeoTable( String tableName ) throws SQLException {
        String sql = "SELECT DropGeoTable('" + tableName + "');";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Create an single column index.
     * 
     * @param tableName the table.
     * @param column the column. 
     * @param isUnique if <code>true</code>, a unique index will be created.
     * @throws SQLException
     */
    public void createIndex( String tableName, String column, boolean isUnique ) throws SQLException {
        String sql = getIndexSql(tableName, column, isUnique);
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    /**
     * Get the sql to create an index.
     * 
     * @param tableName the table.
     * @param column the column. 
     * @param isUnique if <code>true</code>, a unique index will be created.
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
     * Adds a geometry column to a table. 
     * 
     * @param tableName the table name.
     * @param geomColName the geometry column name.
     * @param geomType the geometry type (ex. LINESTRING);
     * @param epsg the optional epsg code (default is 4326);
     * @throws SQLException
     */
    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg )
            throws SQLException {
        String epsgStr = "4326";
        if (epsg != null) {
            epsgStr = epsg;
        }
        String geomTypeStr = "LINESTRING";
        if (geomType != null) {
            geomTypeStr = geomType;
        }

        if (geomColName == null) {
            geomColName = defaultGeomFieldName;
        }

        try (Statement stmt = conn.createStatement()) {
            String sql = "SELECT AddGeometryColumn('" + tableName + "','" + geomColName + "', " + epsgStr + ", '" + geomTypeStr
                    + "', 'XY')";
            stmt.execute(sql);

            sql = "SELECT CreateSpatialIndex('" + tableName + "', '" + geomColName + "');";
            stmt.execute(sql);
        }
    }

    /**
     * Insert a geometry into a table.
     * 
     * @param tableName the table to use.
     * @param geometry the geometry to insert.
     * @param epsg the optional epsg.
     * @throws SQLException
     */
    public void insertGeometry( String tableName, Geometry geometry, String epsg ) throws SQLException {
        String epsgStr = "4326";
        if (epsg == null) {
            epsgStr = epsg;
        }

        SpatialiteGeometryColumns gc = getGeometryColumnsForTable(tableName);
        String sql = "INSERT INTO " + tableName + " (" + gc.f_geometry_column + ") VALUES (GeomFromText(?, " + epsgStr + "))";
        try (PreparedStatement pStmt = conn.prepareStatement(sql)) {
            pStmt.setString(1, geometry.toText());
            pStmt.executeUpdate();
        }
    }

    /**
     * Get the list of available tables.
     * 
     * @param doOrder if <code>true</code>, the names are ordered.
     * @return the list of names.
     * @throws SQLException
     */
    public List<String> getTables( boolean doOrder ) throws SQLException {
        List<String> tableNames = new ArrayList<String>();
        String orderBy = " ORDER BY name";
        if (!doOrder) {
            orderBy = "";
        }
        String sql = "SELECT name FROM sqlite_master WHERE type='table' or type='view'" + orderBy;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while( rs.next() ) {
                String tabelName = rs.getString(1);
                tableNames.add(tabelName);
            }
            return tableNames;
        }
    }

    /**
     * Get the list of available tables, mapped by type.
     * 
     * <p>
     * Supported types are:
     * <ul>
     * <li>{@value SpatialiteTableNames#INTERNALDATA} </li>
     * <li>{@value SpatialiteTableNames#METADATA} </li>
     * <li>{@value SpatialiteTableNames#SPATIALINDEX} </li>
     * <li>{@value SpatialiteTableNames#STYLE} </li>
     * <li>{@value SpatialiteTableNames#USERDATA} </li>
     * <li></li>
     * <li></li>
     * <li></li>
     * </ul>
     * 
     * @param doOrder
     * @return the map of tables sorted by aggregated type:
     * @throws SQLException
     */
    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws SQLException {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = SpatialiteTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    /**
     * Checks if the table is available.
     * 
     * @param tableName the name of the table.
     * @return <code>true</code> if the table exists.
     * @throws SQLException
     */
    public boolean hasTable( String tableName ) throws SQLException {
        String sql = "SELECT name FROM sqlite_master WHERE type='table'";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while( rs.next() ) {
                String name = rs.getString(1);
                if (name.equals(tableName)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the column [name, type, pk] values of a table.
     * 
     * @param tableName the table to check.
     * @return the list of column [name, type, pk].
     * @throws SQLException
     */
    public List<String[]> getTableColumns( String tableName ) throws SQLException {
        List<String[]> columnNames = new ArrayList<String[]>();
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
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

    /**
     * Get the geometry column definition for a given table.
     * 
     * @param tableName the table to check.
     * @return the {@link SpatialiteGeometryColumns column info}.
     * @throws Exception
     */
    public SpatialiteGeometryColumns getGeometryColumnsForTable( String tableName ) throws SQLException {
        String sql = "select " + SpatialiteGeometryColumns.F_TABLE_NAME + ", " //
                + SpatialiteGeometryColumns.F_GEOMETRY_COLUMN + ", " //
                + SpatialiteGeometryColumns.GEOMETRY_TYPE + "," //
                + SpatialiteGeometryColumns.COORD_DIMENSION + ", " //
                + SpatialiteGeometryColumns.SRID + ", " //
                + SpatialiteGeometryColumns.SPATIAL_INDEX_ENABLED + " from " //
                + SpatialiteGeometryColumns.TABLENAME + " where " + SpatialiteGeometryColumns.F_TABLE_NAME + "='" + tableName
                + "'";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                SpatialiteGeometryColumns gc = new SpatialiteGeometryColumns();
                gc.f_table_name = rs.getString(1);
                gc.f_geometry_column = rs.getString(2);
                gc.geometry_type = rs.getInt(3);
                gc.coord_dimension = rs.getInt(4);
                gc.srid = rs.getInt(5);
                gc.spatial_index_enabled = rs.getInt(6);
                return gc;
            }
            return null;
        }
    }

    /**
     * Checks if a table is spatial.
     * 
     * @param tableName the table to check.
     * @return <code>true</code> if a geometry column is present.
     * @throws SQLException
     */
    public boolean isTableSpatial( String tableName ) throws SQLException {
        SpatialiteGeometryColumns geometryColumns = getGeometryColumnsForTable(tableName);
        return geometryColumns != null;
    }

    /**
     * Get the foreign keys from a table.
     * 
     * @param tableName the table to check on.
     * @return the list of keys.
     * @throws SQLException
     */
    public List<ForeignKey> getForeignKeys( String tableName ) throws SQLException {
        List<ForeignKey> fKeys = new ArrayList<ForeignKey>();
        String sql = "PRAGMA foreign_key_list(" + tableName + ")";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
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
                    fKey.table = toTableObj.toString();
                } else {
                    continue;
                }
                fKeys.add(fKey);
            }
            return fKeys;
        }
    }

    /**
     * Get the record count of a table.
     * 
     * @param tableName the name of the table.
     * @return the record count or -1.
     * @throws SQLException
     */
    public long getCount( String tableName ) throws SQLException {
        String sql = "select count(*) from " + tableName;
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while( rs.next() ) {
                long count = rs.getLong(1);
                return count;
            }
            return -1;
        }
    }

    /**
     * Get the table records map with geometry in the given envelope.
     * 
     * <p>If the table is not geometric, the geom is set to null.
     * 
     * @param tableName the table name.
     * @param envelope the envelope to check.
     * @param limit if > 0 a limit is set.
     * @param alsoPK_UID if <code>true</code>, also the PK_UID column is considered.
     * @return the list of found records.
     * @throws SQLException
     * @throws ParseException
     */
    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID, int limit )
            throws SQLException, ParseException {
        QueryResult queryResult = new QueryResult();

        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        boolean hasGeom = gCol != null;

        List<String[]> tableColumnsInfo = getTableColumns(tableName);
        List<String> tableColumns = new ArrayList<>();
        for( String[] info : tableColumnsInfo ) {
            tableColumns.add(info[0]);
        }
        if (hasGeom) {
            tableColumns.remove(gCol.f_geometry_column);
        }
        if (!alsoPK_UID)
            tableColumns.remove(PK_UID);

        String sql = "SELECT ";
        if (hasGeom) {
            sql += "ST_AsBinary(" + gCol.f_geometry_column + ") AS " + gCol.f_geometry_column;
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
        WKBReader wkbReader = new WKBReader();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (hasGeom && columnName.equals(gCol.f_geometry_column)) {
                    queryResult.geometryIndex = i - 1;
                }
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
                    Object object = rs.getObject(j);
                    rec[j - 1] = object;
                }
                queryResult.data.add(rec);
            }
            return queryResult;
        }
    }

    /**
     * Execute a query from raw sql.
     * 
     * @param sql the sql to run.
     * @param limit a limit, ignored if < 1
     * @return the resulting records.
     * @throws SQLException
     * @throws ParseException
     */
    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws SQLException, ParseException {
        QueryResult queryResult = new QueryResult();
        WKBReader wkbReader = new WKBReader();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                int columnType = rsmd.getColumnType(i);
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (columnTypeName.equals("BLOB") && SpatialiteGeometryType.forValue(columnType) != null) {
                    geometryIndex = i;
                    queryResult.geometryIndex = i - 1;
                }
            }
            int count = 0;
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (j == geometryIndex) {
                        byte[] geomBytes = rs.getBytes(j);
                        try {
                            Geometry geometry = wkbReader.read(geomBytes);
                            rec[j - 1] = geometry;
                        } catch (Exception e) {
                            // ignore this, it could be missing ST_AsBinary() in the sql
                        }
                    } else {
                        Object object = rs.getObject(j);
                        rec[j - 1] = object;
                    }
                }
                queryResult.data.add(rec);
                if (limit > 0 && ++count > (limit - 1)) {
                    break;
                }
            }
            return queryResult;
        }
    }

    /**
     * Execute a query from raw sql and put the result in a csv file.
     * 
     * @param sql the sql to run.
     * @param csvFile the output file.
     * @param doHeader if <code>true</code>, the header is written.
     * @param separator the separator (if null, ";" is used).
     * @throws SQLException
     * @throws ParseException
     * @throws IOException 
     */
    public void runRawSqlToCsv( String sql, File csvFile, boolean doHeader, String separator )
            throws SQLException, ParseException, IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
            WKBReader wkbReader = new WKBReader();
            try (Statement stmt = conn.createStatement()) {
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int geometryIndex = -1;
                for( int i = 1; i <= columnCount; i++ ) {
                    if (i > 1) {
                        bw.write(separator);
                    }
                    int columnType = rsmd.getColumnType(i);
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    String columnName = rsmd.getColumnName(i);
                    bw.write(columnName);
                    if (columnTypeName.equals("BLOB") && SpatialiteGeometryType.forValue(columnType) != null) {
                        geometryIndex = i;
                    }
                }
                bw.write("\n");
                while( rs.next() ) {
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (j > 1) {
                            bw.write(separator);
                        }
                        byte[] geomBytes = null;
                        if (j == geometryIndex) {
                            geomBytes = rs.getBytes(j);
                        }
                        if (geomBytes != null) {
                            try {
                                Geometry geometry = wkbReader.read(geomBytes);
                                bw.write(geometry.toText());
                            } catch (Exception e) {
                                // write it as it comes
                                Object object = rs.getObject(j);
                                if (object != null) {
                                    bw.write(object.toString());
                                } else {
                                    bw.write("");
                                }
                            }
                        } else {
                            Object object = rs.getObject(j);
                            if (object != null) {
                                bw.write(object.toString());
                            } else {
                                bw.write("");
                            }
                        }
                    }
                    bw.write("\n");
                }
            }
        }
    }

    /**
     * Extractes a featurecollection from an sql statement.
     * 
     * <p>The assumption is made that the first string after the FROM
     * keyword in the select statement is the table that contains the geometry.
     * 
     * @param simpleSql the sql.
     * @return the features.
     * @throws Exception
     */
    public DefaultFeatureCollection runRawSqlToFeatureCollection( String simpleSql ) throws Exception {
        String[] split = simpleSql.split("\\s+");
        String tableName = null;
        for( int i = 0; i < split.length; i++ ) {
            if (split[i].toLowerCase().equals("from")) {
                tableName = split[i + 1];
            }
        }

        if (tableName == null) {
            throw new RuntimeException("The geometry table name needs to be the first after the FROM keyword.");
        }

        SpatialiteGeometryColumns geometryColumns = getGeometryColumnsForTable(tableName);
        if (geometryColumns == null) {
            throw new IllegalArgumentException("The supplied table name doesn't seem to be spatial: " + tableName);
        }

        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        WKBReader wkbReader = new WKBReader();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(simpleSql);
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;

            CoordinateReferenceSystem crs = CRS.decode("EPSG:" + geometryColumns.srid);
            SpatialiteGeometryType geomType = SpatialiteGeometryType.forValue(geometryColumns.geometry_type);

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("sql");
            b.setCRS(crs);

            for( int i = 1; i <= columnCount; i++ ) {
                int columnType = rsmd.getColumnType(i);
                String columnTypeName = rsmd.getColumnTypeName(i);
                String columnName = rsmd.getColumnName(i);

                if (geomType != null && columnType > 999 && columnTypeName.toLowerCase().equals("blob")) {
                    geometryIndex = i;
                    b.add("the_geom", geomType.getGeometryClass());
                } else {
                    // Class< ? > forName = Class.forName(columnClassName);
                    switch( columnTypeName ) {
                    case "INTEGER":
                        b.add(columnName, Integer.class);
                        break;
                    case "DOUBLE":
                    case "FLOAT":
                    case "REAL":
                        b.add(columnName, Double.class);
                        break;
                    case "DATE":
                        b.add(columnName, Date.class);
                        break;
                    case "TEXT":
                    default:
                        b.add(columnName, String.class);
                        break;
                    }
                }
            }

            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            while( rs.next() ) {
                Object[] values = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (j == geometryIndex) {
                        byte[] geomBytes = rs.getBytes(j);
                        Geometry geometry = wkbReader.read(geomBytes);
                        values[j - 1] = geometry;
                    } else {
                        Object object = rs.getObject(j);
                        if (object != null) {
                            values[j - 1] = object;
                        }
                    }
                }
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                fc.add(feature);
            }
        }
        return fc;
    }

    /**
     * Execute a insert/update sql file. 
     * 
     * @param file the file to run on this db.
     * @param chunks commit interval.
     * @throws Exception 
     */
    public void executeSqlFile( File file, int chunks, boolean eachLineAnSql ) throws Exception {
        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        Predicate<String> validSqlLine = s -> s.length() != 0 //
                && !s.startsWith("BEGIN") //
                && !s.startsWith("COMMIT") //
                ;
        Predicate<String> commentPredicate = s -> !s.startsWith("--");

        try (Statement pStmt = conn.createStatement()) {
            final int[] counter = {1};
            Stream<String> linesStream = null;
            if (eachLineAnSql) {
                linesStream = Files.lines(Paths.get(file.getAbsolutePath())).map(s -> s.trim()).filter(commentPredicate)
                        .filter(validSqlLine);
            } else {
                linesStream = Arrays.stream(Files.lines(Paths.get(file.getAbsolutePath())).filter(commentPredicate)
                        .collect(Collectors.joining()).split(";")).filter(validSqlLine);
            }

            Consumer<String> executeAction = s -> {
                try {
                    pStmt.executeUpdate(s);
                    counter[0]++;
                    if (counter[0] % chunks == 0) {
                        conn.commit();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            linesStream.forEach(executeAction);
            conn.commit();
        }
        conn.setAutoCommit(autoCommit);
    }

    /**
     * Execute an update, insert or delete by sql.
     * 
     * @param sql the sql to run.
     * @return the result code of the update.
     * @throws SQLException
     */
    public int executeInsertUpdateDeleteSql( String sql ) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            int executeUpdate = stmt.executeUpdate(sql);
            return executeUpdate;
        }
    }

    /**
     * Get the geometries of a table inside a given envelope.
     * 
     * @param tableName the table name.
     * @param envelope the envelope to check.
     * @return The list of geometries intersecting the envelope.
     * @throws SQLException
     * @throws ParseException
     */
    public List<Geometry> getGeometriesIn( String tableName, Envelope envelope ) throws SQLException, ParseException {
        List<Geometry> geoms = new ArrayList<Geometry>();

        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        String sql = "SELECT ST_AsBinary(" + gCol.f_geometry_column + ") FROM " + tableName;

        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE " + getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        WKBReader wkbReader = new WKBReader();
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while( rs.next() ) {
                byte[] geomBytes = rs.getBytes(1);
                Geometry geometry = wkbReader.read(geomBytes);
                geoms.add(geometry);
            }
            return geoms;
        }
    }

    /**
     * Get the where cause of a Spatialindex based BBOX query.
     * 
     * @param tableName the name of the table.
     * @param x1 west bound.
     * @param y1 south bound.
     * @param x2 east bound.
     * @param y2 north bound.
     * @return the sql piece.
     * @throws SQLException 
     */
    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws SQLException {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }
        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        String sql = "ST_Intersects(" + alias + gCol.f_geometry_column + ", BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2
                + ")) = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "f_table_name = '" + tableName + "' AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }

    /**
     * Get the where query piece based on a geometry intersection.
     * 
     * @param tableName the table to query.
     * @param alias optinal alias.
     * @param geometry the geometry to intersect.
     * @return the query piece.
     * @throws SQLException
     */
    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws SQLException {
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

        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        String sql = "ST_Intersects(" + alias + gCol.f_geometry_column + ", " + "GeomFromText('" + geometry.toText() + "')"
                + ") = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "f_table_name = '" + tableName + "' AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }

    /**
     * Get the bounds of a table.
     * 
     * @param tableName the table to query.
     * @return the {@link Envelope} of the table.
     * @throws SQLException
     */
    public Envelope getTableBounds( String tableName ) throws SQLException {
        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        String geomFieldName = gCol.f_geometry_column;

        String trySql = "SELECT extent_min_x, extent_min_y, extent_max_x, extent_max_y FROM vector_layers_statistics WHERE table_name='"
                + tableName + "' AND geometry_column='" + geomFieldName + "'";
        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(trySql);
            if (rs.next()) {
                double minX = rs.getDouble(1);
                double minY = rs.getDouble(2);
                double maxX = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                Envelope env = new Envelope(minX, maxX, minY, maxY);
                if (env.getWidth() != 0.0 && env.getHeight() != 0.0) {
                    return env;
                }
            }
        }

        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT Min(MbrMinX(" + geomFieldName + ")) AS min_x, Min(MbrMinY(" + geomFieldName + ")) AS min_y,"
                + "Max(MbrMaxX(" + geomFieldName + ")) AS max_x, Max(MbrMaxY(" + geomFieldName + ")) AS max_y " + "FROM "
                + tableName;

        try (Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while( rs.next() ) {
                double minX = rs.getDouble(1);
                double minY = rs.getDouble(2);
                double maxX = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                Envelope env = new Envelope(minX, maxX, minY, maxY);
                return env;
            }
            return null;
        }
    }

    /**
     * @return the connection to the database.
     */
    public Connection getConnection() {
        return conn;
    }

    @Override
    public void close() throws Exception {
        if (conn != null) {
            conn.close();
        }
    }

    /**
     * Escape sql.
     * 
     * @param sql the sql code to escape. 
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
     * <p>The default format is: <b>2015-06-11 03:14:51</b>, as
     * given by pattern: <b>%Y-%m-%d %H:%M:%S</b>.</p>
     * 
     * @param columnName the timestamp column in the db.
     * @param datePattern the datepattern.
     * @return the query piece.
     */
    public static String getTimestampQuery( String columnName, String datePattern ) {
        if (datePattern == null)
            datePattern = "%Y-%m-%d %H:%M:%S";
        String sql = "strftime('" + datePattern + "', " + columnName + " / 1000, 'unixepoch')";
        return sql;
    }

}
