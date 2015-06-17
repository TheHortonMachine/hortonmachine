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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public static final String geomFieldName = "the_geom";

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
        stmt.setQueryTimeout(30); // set timeout to 30 sec.
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

    public void deleteTable( String tableName ) throws SQLException {
        Statement stmt = conn.createStatement();

        String sql = "SELECT DisableSpatialIndex('" + tableName + "', '" + geomFieldName + "');";
        stmt.execute(sql);

        sql = "drop table " + tableName + ";";
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
    public void createIndex( String tableName, String column, boolean isUnique ) {
        String unique = "UNIQUE ";
        if (!isUnique) {
            unique = "";
        }
        String indexName = tableName + "__" + column + "_idx";
        String sql = "CREATE " + unique + "INDEX " + indexName + " on " + tableName + "(" + column + ");";

        System.out.println(sql);
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void printIndexSql( String tableName, String column, boolean isUnique ) {
        String unique = "UNIQUE ";
        if (!isUnique) {
            unique = "";
        }
        String indexName = tableName + "__" + column + "_idx";
        String sql = "CREATE " + unique + "INDEX " + indexName + " on " + tableName + "(" + column + ");";

        System.out.println(sql);
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

        String sql = "SELECT AddGeometryColumn('" + tableName + "','" + geomFieldName + "', " + epsgStr + ", '" + geomTypeStr
                + "', 'XY')";
        Statement stmt = conn.createStatement();
        stmt.execute(sql);

        sql = "SELECT CreateSpatialIndex('" + tableName + "', '" + geomFieldName + "');";
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
        String sql = "INSERT INTO " + tableName + " (" + geomFieldName + ") VALUES (GeomFromText(?, " + epsgStr + "))";
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
        String sql = "SELECT name FROM sqlite_master WHERE type='table'" + orderBy;
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            String tabelName = rs.getString(1);
            tableNames.add(tabelName);
        }
        return tableNames;
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
     * Get the column names of a table.
     * 
     * @param tableName the table to check.
     * @return the list of column names.
     * @throws SQLException
     */
    public List<String> getTableColumns( String tableName ) throws SQLException {
        List<String> columnNames = new ArrayList<String>();
        String sql = "PRAGMA table_info(" + tableName + ")";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            String columnName = rs.getString(2);
            columnNames.add(columnName);
        }
        return columnNames;
    }

    /**
     * Get the column types of a table.
     * 
     * @param tableName the table to check.
     * @return the list of column type.
     * @throws SQLException
     */
    public List<String> getTableColumnTypes( String tableName ) throws SQLException {
        List<String> columnTypes = new ArrayList<String>();
        String sql = "PRAGMA table_info(" + tableName + ")";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            String columnType = rs.getString(3);
            columnTypes.add(columnType);
        }
        return columnTypes;
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
     * Get the table records with geometry in the given envelope.
     * 
     * @param tableName the table name.
     * @param envelope the envelope to check.
     * @return the list of found records.
     * @throws SQLException
     * @throws ParseException
     */
    @SuppressWarnings("unused")
    public List<TableRecord> getTableRecordsIn( String tableName, Envelope envelope ) throws SQLException, ParseException {
        List<TableRecord> tableRecords = new ArrayList<TableRecord>();

        List<String> tableColumns = getTableColumns(tableName);
        tableColumns.remove(geomFieldName);
        tableColumns.remove(PK_UID);

        double x1 = envelope.getMinX();
        double y1 = envelope.getMinY();
        double x2 = envelope.getMaxX();
        double y2 = envelope.getMaxY();

        String sql = "SELECT ST_AsBinary(" + geomFieldName + ") AS " + geomFieldName;
        for( int i = 0; i < tableColumns.size(); i++ ) {
            sql += ",";
            sql += tableColumns.get(i);
        }
        sql += " FROM " + tableName + " WHERE "; //
        sql += getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);

        WKBReader wkbReader = new WKBReader();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            int i = 1;
            byte[] geomBytes = rs.getBytes(i++);
            Geometry geometry = wkbReader.read(geomBytes);
            TableRecord rec = new TableRecord();
            rec.geometry = geometry;

            for( String columnName : tableColumns ) {
                Object object = rs.getObject(i++);
                rec.data.add(object);
            }
            tableRecords.add(rec);
        }
        return tableRecords;
    }

    /**
     * Get the table records map with geometry in the given envelope.
     * 
     * @param tableName the table name.
     * @param envelope the envelope to check.
     * @return the list of found records.
     * @throws SQLException
     * @throws ParseException
     */
    public List<TableRecordMap> getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID )
            throws SQLException, ParseException {
        List<TableRecordMap> tableRecords = new ArrayList<TableRecordMap>();

        List<String> tableColumns = getTableColumns(tableName);
        tableColumns.remove(geomFieldName);
        if (!alsoPK_UID)
            tableColumns.remove(PK_UID);

        double x1 = envelope.getMinX();
        double y1 = envelope.getMinY();
        double x2 = envelope.getMaxX();
        double y2 = envelope.getMaxY();

        String sql = "SELECT ST_AsBinary(" + geomFieldName + ") AS " + geomFieldName;
        for( int i = 0; i < tableColumns.size(); i++ ) {
            sql += ",";
            sql += tableColumns.get(i);
        }
        sql += " FROM " + tableName + " WHERE "; //
        sql += getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);

        WKBReader wkbReader = new WKBReader();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            int i = 1;
            byte[] geomBytes = rs.getBytes(i++);
            Geometry geometry = wkbReader.read(geomBytes);
            TableRecordMap rec = new TableRecordMap();
            rec.geometry = geometry;

            for( String columnName : tableColumns ) {
                Object object = rs.getObject(i++);
                rec.data.put(columnName, object);
            }
            tableRecords.add(rec);
        }
        return tableRecords;
    }

    /**
     * Get the geometries of a table inside a given envelope.
     * 
     * @param tableName
     * @param envelope
     * @return
     * @throws SQLException
     * @throws ParseException
     */
    public List<Geometry> getGeometriesIn( String tableName, Envelope envelope ) throws SQLException, ParseException {
        List<Geometry> geoms = new ArrayList<Geometry>();

        double x1 = envelope.getMinX();
        double y1 = envelope.getMinY();
        double x2 = envelope.getMaxX();
        double y2 = envelope.getMaxY();

        String sql = "SELECT ST_AsBinary(" + geomFieldName + ") FROM " + tableName + " WHERE ";
        sql += getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);

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
     */
    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 ) {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }
        String sql = "ST_Intersects(" + alias + geomFieldName + ", BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2
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
     * @param sql 
     * @return
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
     * Compose the formatter for unix timstamps in queries.
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
        String dbPath = "D:/data/italy.sqlite";

        try (SpatialiteDb db = new SpatialiteDb()) {
            db.open(dbPath);

            EggClock clock = new EggClock("time:", "\n");
            clock.startAndPrint(System.out);

            System.out.println(Arrays.toString(db.getDbInfo()));
            clock.printTimePassedInSeconds(System.out);

            List<String> tableNames = db.getTables(true);
            System.out.println("Tables:" + Arrays.toString(tableNames.toArray()));
            clock.printTimePassedInSeconds(System.out);

            System.out.println("Has table roads:" + db.hasTable("roads"));
            clock.printTimePassedInSeconds(System.out);

            String tableName = "roads";
            List<String> tableColumns = db.getTableColumns(tableName);
            System.out.println(Arrays.toString(tableColumns.toArray()));
            clock.printTimePassedInSeconds(System.out);

            Coordinate c = new Coordinate(11.33134, 46.48275);
            Envelope q = new Envelope(c);
            q.expandBy(0.0001);

            List<Geometry> geoms = new ArrayList<Geometry>();
            List<TableRecord> recordsIn = db.getTableRecordsIn(tableName, q);
            System.out.println(recordsIn.size());
            for( TableRecord tableRecord : recordsIn ) {
                geoms.add(tableRecord.geometry);
                // for( Object obj : tableRecord.data ) {
                // System.out.print(obj.toString() + ",");
                // }
                // System.out.println();
            }
            GeometryCollection gc = new GeometryCollection(geoms.toArray(GeometryUtilities.TYPE_GEOMETRY), new GeometryFactory());
            System.out.println(gc.toText());
            clock.printTimePassedInSeconds(System.out);

            List<Geometry> geometriesIn = db.getGeometriesIn(tableName, q);
            gc = new GeometryCollection(geometriesIn.toArray(GeometryUtilities.TYPE_GEOMETRY), new GeometryFactory());
            System.out.println(gc.toText());
            clock.printTimePassedInSeconds(System.out);

            Envelope tableBounds = db.getTableBounds(tableName);
            System.out.println(tableBounds);
            clock.printTimePassedInSeconds(System.out);
        }

    }
}
