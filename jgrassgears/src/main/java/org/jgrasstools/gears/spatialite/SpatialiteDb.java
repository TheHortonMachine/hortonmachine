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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jgrasstools.gears.utils.time.EggClock;
import org.sqlite.SQLiteConfig;

import com.vividsolutions.jts.geom.Coordinate;
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

    private static final String PK_UID = "PK_UID";
    private Connection conn = null;
    private String geomFieldName = "the_geom";

    private WKBReader wkbReader = new WKBReader();

    /**
     * Open the connection to a database.
     * 
     * @param dbPath the database path.
     * @throws SQLException
     */
    public void open( String dbPath ) throws SQLException {
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
        sql += "ST_Intersects(" + geomFieldName + ", BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ")) = 1";

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
     * Get the column names of a table.
     * 
     * @param tableName the table to check.
     * @return the list of column names.
     * @throws SQLException
     */
    public List<String> getTableColumns( String tableName ) throws SQLException {
        List<String> tableNames = new ArrayList<String>();
        String sql = "PRAGMA table_info(" + tableName + ")";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            String columnName = rs.getString(2);
            tableNames.add(columnName);
        }
        return tableNames;
    }

    /**
     * Get the column types of a table.
     * 
     * @param tableName the table to check.
     * @return the list of column type.
     * @throws SQLException
     */
    public List<String> getTableColumnTypes( String tableName ) throws SQLException {
        List<String> tableNames = new ArrayList<String>();
        String sql = "PRAGMA table_info(" + tableName + ")";
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            String columnType = rs.getString(3);
            tableNames.add(columnType);
        }
        return tableNames;
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
    public List<TableRecord> geTableRecordsIn( String tableName, Envelope envelope ) throws SQLException, ParseException {
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
        sql += " FROM " + tableName + " WHERE " //
                + "ST_Intersects(" + geomFieldName + ", BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + ")) = 1";

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
     * Get the bounds of a table.
     * 
     * @param tableName the table to query.
     * @return the {@link Envelope} of the table.
     * @throws SQLException
     */
    public Envelope getTableBounds( String tableName ) throws SQLException {
        String sql = "SELECT Min(MbrMinX(" + geomFieldName + ")) AS min_x, Min(MbrMinY(" + geomFieldName + ")) AS min_y,"
                + "Max(MbrMaxX(" + geomFieldName + ")) AS max_x, Max(MbrMaxY(" + geomFieldName + ")) AS max_y " + "FROM "
                + tableName;

        Statement stmt = conn.createStatement();
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

    private String getBboxIntersectingFeaturesQuery( String spatialTable, double n, double s, double e, double w ) {
        StringBuilder sbQ = new StringBuilder();
        sbQ.append("SELECT ");
        sbQ.append("ST_AsBinary(");
        sbQ.append(geomFieldName);
        sbQ.append(")");
        sbQ.append(" FROM ").append(spatialTable);
        sbQ.append(" WHERE ST_Intersects(");
        sbQ.append("BuildMBR(");
        sbQ.append(w);
        sbQ.append(",");
        sbQ.append(s);
        sbQ.append(",");
        sbQ.append(e);
        sbQ.append(",");
        sbQ.append(n);
        sbQ.append("),");
        sbQ.append(geomFieldName);
        sbQ.append(");");
        String query = sbQ.toString();
        return query;
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

    public static String escapeSql( String str ) {
        // ' --> ''
        str = str.replaceAll("'", "''");
        // " --> ""
        str = str.replaceAll("\"", "\"\"");
        // \ --> (remove backslashes)
        str = str.replaceAll("\\\\", "");
        return str;
    }

    public static void main( String[] args ) throws Exception {
        String dbPath = "D:/data/italy.sqlite";

        try (SpatialiteDb db = new SpatialiteDb()) {
            db.open(dbPath);

            EggClock clock = new EggClock("time:", "\n");
            clock.startAndPrint(System.out);

            System.out.println(Arrays.toString(db.getDbInfo()));
            clock.printTimePassedInSeconds(System.out);

            String tableName = "roads";
            List<String> tableColumns = db.getTableColumns(tableName);
            System.out.println(Arrays.toString(tableColumns.toArray()));
            clock.printTimePassedInSeconds(System.out);

            Envelope tableBounds = db.getTableBounds(tableName);
            System.out.println(tableBounds);
            clock.printTimePassedInSeconds(System.out);

            Coordinate c = new Coordinate(11.33134, 46.48275);
            Envelope q = new Envelope(c);
            q.expandBy(0.0001);

            // List<Geometry> geometriesIn = db.getGeometriesIn(tableName, q);
            // System.out.println(geometriesIn.size());
            // clock.printTimePassedInSeconds(System.out);

            List<TableRecord> recordsIn = db.geTableRecordsIn(tableName, q);
            System.out.println(recordsIn.size());
            for( TableRecord tableRecord : recordsIn ) {
                System.out.println(tableRecord.geometry);
                for( Object obj : tableRecord.data ) {
                    System.out.print(obj.toString() + ",");
                }
                System.out.println();
            }
            clock.printTimePassedInSeconds(System.out);

        }

    }
}
