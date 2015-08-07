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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.time.EggClock;
import org.sqlite.SQLiteConfig;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
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
        // System.setProperty("java.io.tmpdir", "D:/TMP/");
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

    /**
     * Open the connection to a database.
     * 
     * @param dbPath the database path.
     * @throws SQLException
     */
    public void open( String dbPath ) throws SQLException {
        this.dbPath = dbPath;

        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            System.out.println("Database exists");
        }
        // enabling dynamic extension loading
        // absolutely required by SpatiaLite
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        // create a database connection
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath, config.toProperties());
        Statement stmt = conn.createStatement();
        // set timeout to 30 sec.
        stmt.setQueryTimeout(30);
        // load SpatiaLite
        stmt.execute("SELECT load_extension('mod_spatialite')");
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
        String sql = "SELECT InitSpatialMetadata(" + options + ")";
        Statement stmt = conn.createStatement();
        stmt.execute(sql);
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
        ResultSet rs = conn.createStatement().executeQuery(sql);
        String[] info = new String[3];
        while( rs.next() ) {
            // read the result set
            info[0] = rs.getString(1);
            info[1] = rs.getString(2);
            info[2] = rs.getString(3);
        }
        return info;
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

        Statement stmt = conn.createStatement();
        stmt.execute(sb.toString());
    }

    /**
     * Delete a geo-table with all attached indexes and stuff.
     * 
     * @param tableName
     * @throws SQLException
     */
    public void deleteGeoTable( String tableName ) throws SQLException {
        Statement stmt = conn.createStatement();

        String sql = "SELECT DropGeoTable('" + tableName + "');";
        stmt.execute(sql);
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
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(sql);
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
     * @param geomType the geometry type (ex. LINESTRING);
     * @param epsg the optional epsg code (default is 4326);
     * @throws SQLException
     */
    public void addGeometryXYColumnAndIndex( String tableName, String geomType, String epsg ) throws SQLException {
        String epsgStr = "4326";
        if (epsg != null) {
            epsgStr = epsg;
        }
        String geomTypeStr = "LINESTRING";
        if (geomType != null) {
            geomTypeStr = geomType;
        }

        String sql = "SELECT AddGeometryColumn('" + tableName + "','" + defaultGeomFieldName + "', " + epsgStr + ", '"
                + geomTypeStr + "', 'XY')";
        Statement stmt = conn.createStatement();
        stmt.execute(sql);

        sql = "SELECT CreateSpatialIndex('" + tableName + "', '" + defaultGeomFieldName + "');";
        stmt.execute(sql);
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
        PreparedStatement pStmt = conn.prepareStatement(sql);
        pStmt.setString(1, geometry.toText());
        pStmt.executeUpdate();
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
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            String tabelName = rs.getString(1);
            tableNames.add(tabelName);
        }
        return tableNames;
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
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            String name = rs.getString(1);
            if (name.equals(tableName)) {
                return true;
            }
        }
        return false;
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
        Statement stmt = conn.createStatement();
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
        Statement stmt = conn.createStatement();
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
        Statement stmt = conn.createStatement();
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

    /**
     * Get the record count of a table.
     * 
     * @param tableName the name of the table.
     * @return the record count or -1.
     * @throws SQLException
     */
    public long getCount( String tableName ) throws SQLException {
        String sql = "select count(*) from " + tableName;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            long count = rs.getLong(1);
            return count;
        }
        return -1;
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
        Statement stmt = conn.createStatement();
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
        Statement stmt = conn.createStatement();
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

    /**
     * Execute an update, insert or delete by sql.
     * 
     * @param sql the sql to run.
     * @return the result code of the update.
     * @throws SQLException
     */
    public int executeInsertUpdateDeleteSql( String sql ) throws SQLException {
        Statement stmt = conn.createStatement();
        int executeUpdate = stmt.executeUpdate(sql);
        return executeUpdate;
    }

    /**
     * Get the geometries of a table inside a given envelope.
     * 
     * @param tableName the table name.
     * @param envelope the envelope to check.
     * @return
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
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            byte[] geomBytes = rs.getBytes(1);
            Geometry geometry = wkbReader.read(geomBytes);
            geoms.add(geometry);
        }
        return geoms;
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
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(trySql);
        while( rs.next() ) {
            double minX = rs.getDouble(1);
            double minY = rs.getDouble(2);
            double maxX = rs.getDouble(3);
            double maxY = rs.getDouble(4);

            Envelope env = new Envelope(minX, maxX, minY, maxY);
            return env;
        }

        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT Min(MbrMinX(" + geomFieldName + ")) AS min_x, Min(MbrMinY(" + geomFieldName + ")) AS min_y,"
                + "Max(MbrMaxX(" + geomFieldName + ")) AS max_x, Max(MbrMaxY(" + geomFieldName + ")) AS max_y " + "FROM "
                + tableName;

        stmt = conn.createStatement();
        rs = stmt.executeQuery(sql);
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

    public static void main( String[] args ) throws Exception {
        String dbPath = "/media/hydrologis/HYDRO/huberg/ITALGAS_SICILIA/2015_Trapani_export/dataindex_v3_2015.sqlite";

        try (SpatialiteDb db = new SpatialiteDb()) {
            db.open(dbPath);

            EggClock clock = new EggClock("time:", "\n");
            clock.startAndPrint(System.out);

            System.out.println(Arrays.toString(db.getDbInfo()));
            clock.printTimePassedInSeconds(System.out);

            List<String> tableNames = db.getTables(true);
            System.out.println("Tables:");
            for( String tableName : tableNames ) {
                System.out.println(tableName);
            }
            clock.printTimePassedInSeconds(System.out);
            HashMap<String, List<String>> tablesMap = db.getTablesMap(true);
            for( Entry<String, List<String>> entry : tablesMap.entrySet() ) {
                System.out.println(entry.getKey() + ": ");
                for( String tableName : entry.getValue() ) {
                    SpatialiteGeometryColumns gc = db.getGeometryColumnsForTable(tableName);
                    if (gc == null) {
                        System.out.println("\t->" + tableName);
                    } else {
                        System.out.println("\t->" + tableName + " / geom col: " + gc.f_geometry_column + " / srid: " + gc.srid
                                + " / geom type: " + SpatialiteGeometryType.forValue(gc.geometry_type));
                    }
                }
            }
            clock.printTimePassedInSeconds(System.out);

            System.out.println("Has table roads:" + db.hasTable("roads"));
            clock.printTimePassedInSeconds(System.out);

            String tableName = "roads";
            List<String[]> tableColumns = db.getTableColumns(tableName);
            System.out.println(Arrays.toString(tableColumns.toArray()));
            clock.printTimePassedInSeconds(System.out);

            // Coordinate c = new Coordinate(11.33134, 46.48275);
            // Envelope q = new Envelope(c);
            // q.expandBy(0.1);

            List<Geometry> geoms = new ArrayList<Geometry>();
            QueryResult recordsIn = db.getTableRecordsMapIn(tableName, null, false, 10);
            System.out.println(recordsIn.data.size());
            if (recordsIn.geometryIndex != -1)
                for( Object[] tableRecord : recordsIn.data ) {
                    geoms.add((Geometry) tableRecord[recordsIn.geometryIndex]);
                }
            GeometryCollection gc = new GeometryCollection(geoms.toArray(GeometryUtilities.TYPE_GEOMETRY), new GeometryFactory());
            System.out.println(gc.toText());
            clock.printTimePassedInSeconds(System.out);

            String sql = "SELECT ST_AsBinary(the_geom) AS the_geom,osm_id,name,ref,type,oneway,bridge,tunnel,maxspeed FROM roads LIMIT 10";
            QueryResult recordsIn1 = db.getTableRecordsMapFromRawSql(sql, 10);
            System.out.println(recordsIn1.data.size());
            if (recordsIn1.geometryIndex != -1)
                for( Object[] tableRecord : recordsIn1.data ) {
                    geoms.add((Geometry) tableRecord[recordsIn1.geometryIndex]);
                }
            gc = new GeometryCollection(geoms.toArray(GeometryUtilities.TYPE_GEOMETRY), new GeometryFactory());
            System.out.println(gc.toText());
            clock.printTimePassedInSeconds(System.out);

            List<Geometry> geometriesIn = db.getGeometriesIn(tableName, null);
            gc = new GeometryCollection(geometriesIn.toArray(GeometryUtilities.TYPE_GEOMETRY), new GeometryFactory());
            System.out.println(gc.toText());
            clock.printTimePassedInSeconds(System.out);

            Envelope tableBounds = db.getTableBounds(tableName);
            System.out.println(tableBounds);
            clock.printTimePassedInSeconds(System.out);

            QueryResult tableRecordsMapFromRawSql = db.getTableRecordsMapFromRawSql("SELECT *  FROM sql_statements_log", -1);
            System.out.println("sql history");
            int indexOf = tableRecordsMapFromRawSql.names.indexOf("sql_statement");
            if (indexOf != -1)
                for( Object[] tableRecordMap : tableRecordsMapFromRawSql.data ) {
                    System.out.println(tableRecordMap[indexOf]);
                }
            clock.printTimePassedInSeconds(System.out);

            // db.deleteGeoTable("ne_10m_admin_1_states_provinces");
            // db.createTableFromShp(new
            // File("D:/data/naturalearth/ne_10m_admin_1_states_provinces.shp"));
            // SpatialiteImportUtils.importShapefile(db, new
            // File("D:/data/naturalearth/ne_10m_admin_0_countries.shp"),
            // "ne_10m_admin_0_countries", -1);
            // clock.printTimePassedInSeconds(System.out);
        }

    }
}
