package org.jgrasstools.gears.spatialite;
import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import org.sqlite.SQLiteConfig;

public class SpatialiteSample {
    public static String dbPath = "D:/TMP/spatialite-test64.sqlite";

    public SpatialiteSample() {
        System.setProperty("java.io.tmpdir", "D:/TMP/");
        File dbFile = new File(dbPath);
        dbFile.delete();

        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");

            // enabling dynamic extension loading
            // absolutely required by SpatiaLite
            SQLiteConfig config = new SQLiteConfig();
            config.enableLoadExtension(true);
            // create a database connection
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath, config.toProperties());

            Statement stmt = conn.createStatement();
            stmt.setQueryTimeout(30); // set timeout to 30 sec.

            // loading SpatiaLite
            stmt.execute("SELECT load_extension('mod_spatialite')");

            printInfo(stmt);

            // // createDummyTable(stmt);
            // // conn.setAutoCommit(false);
            // // insertDummyAndCheck(conn, stmt);
            // // conn.setAutoCommit(true);
            //
            // enabling Spatial Metadata
            // this automatically initializes SPATIAL_REF_SYS and GEOMETRY_COLUMNS
//            String sql = "SELECT InitSpatialMetadata()";
             String sql = "SELECT InitSpatialMetadata(0, 'WGS84')";
            stmt.execute(sql);
            //
            // //
            // // naturalmente poi lo sviluppatore saggio andra' a recuperarsi il
            // // valore di ritorno dal resultset (finezze forse ignote ai javaroli):
            // //
            // // 1 = ok, spatial index creato con pieno successo
            // // 0 = oh cazzo, e' andata male
            // // -1 = guarda meglio coglione, hai creato una colonna fisica che
            // // si chiama ROWID: chi ti credevi di prendere per il culo ?
            //
            // createPointTable(stmt);
            //
            createLineTable(stmt);
            stmt.close();
            //
            // createPolygonTable(stmt);

            // inserting some POINTs
            // please note well: SQLite is ACID and Transactional,
            // so (to get best performance) the whole insert cycle
            // will be handled as a single TRANSACTION

            // conn.setAutoCommit(false);
            // insertPointsAndCheck(conn, stmt);

            insertLinesAndCheck(conn);
            //
            // insertPolygonsAndCheck(conn, stmt);
        } catch (Exception e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                e.printStackTrace();
            }
        }
    }
    private void insertPolygonsAndCheck( Connection conn, Statement stmt ) throws SQLException {
        String sql;
        ResultSet rs;
        // inserting some POLYGONs
        // this time too we'll use a Prepared Statement
        sql = "INSERT INTO test_pg (id, name, geom) ";
        sql += "VALUES (?, ?, GeomFromText(?, 4326))";
        final PreparedStatement ins_stmt = conn.prepareStatement(sql);
        conn.setAutoCommit(false);
        for( int i = 0; i < 100000; i++ ) {
            // setting up values / binding
            String name = "test POLYGON #";
            name += i + 1;
            ins_stmt.setInt(1, i + 1);
            ins_stmt.setString(2, name);
            String geom = "POLYGON((";
            geom += -10.0 - (i / 1000.0);
            geom += " ";
            geom += -10.0 - (i / 1000.0);
            geom += ", ";
            geom += 10.0 + (i / 1000.0);
            geom += " ";
            geom += -10.0 - (i / 1000.0);
            geom += ", ";
            geom += 10.0 + (i / 1000.0);
            geom += " ";
            geom += 10.0 + (i / 1000.0);
            geom += ", ";
            geom += -10.0 - (i / 1000.0);
            geom += " ";
            geom += 10.0 + (i / 1000.0);
            geom += ", ";
            geom += -10.0 - (i / 1000.0);
            geom += " ";
            geom += -10.0 - (i / 1000.0);
            geom += "))";
            ins_stmt.setInt(1, i + 1);
            ins_stmt.setString(2, name);
            ins_stmt.setString(3, geom);
            ins_stmt.executeUpdate();
        }
        conn.commit();

        // checking POLYGONs
        sql = "SELECT DISTINCT Count(*), ST_GeometryType(geom), ";
        sql += "ST_Srid(geom) FROM test_pg";
        rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            // read the result set
            String msg = "> Inserted ";
            msg += rs.getInt(1);
            msg += " entities of type ";
            msg += rs.getString(2);
            msg += " SRID=";
            msg += rs.getInt(3);
            System.out.println(msg);
        }
    }
    private void insertLinesAndCheck( Connection conn) throws SQLException {
        String sql;
        ResultSet rs;
        int i;
        // inserting some LINESTRINGs
        // this time we'll use a Prepared Statement
        sql = "INSERT INTO test_ln (id, name, geom) ";
        sql += "VALUES (?, ?, GeomFromText(?, 4326))";
        PreparedStatement ins_stmt = conn.prepareStatement(sql);
        // conn.setAutoCommit(false);
        int index = 1;
        for( i = 0; i < 1000; i++ ) {
            // setting up values / binding
            String name = "test LINESTRING #";
            name += i + 1;
            String geom = "LINESTRING (";
            if ((i % 2) == 1) {
                // odd row: five points
                geom += "-180.0 -90.0, ";
                geom += -10.0 - (i / 1000.0);
                geom += " ";
                geom += -10.0 - (i / 1000.0);
                geom += ", ";
                geom += -10.0 - (i / 1000.0);
                geom += " ";
                geom += 10.0 + (i / 1000.0);
                geom += ", ";
                geom += 10.0 + (i / 1000.0);
                geom += " ";
                geom += 10.0 + (i / 1000.0);
                geom += ", 180.0 90.0";
            } else {
                // even row: two points
                geom += -10.0 - (i / 1000.0);
                geom += " ";
                geom += -10.0 - (i / 1000.0);
                geom += ", ";
                geom += 10.0 + (i / 1000.0);
                geom += " ";
                geom += 10.0 + (i / 1000.0);
            }
            geom += ")";
            ins_stmt.setInt(1, i + 1);
            ins_stmt.setString(2, name);
            ins_stmt.setString(3, geom);
//            ins_stmt.addBatch();
//            if (i % 400 == 0){
//                System.out.println("Exec: " + i);
//                ins_stmt.executeBatch();
//            }
             ins_stmt.executeUpdate();
        }
//        ins_stmt.executeBatch();
        
//        conn.commit();
        ins_stmt.close();

        // checking LINESTRINGs
        sql = "SELECT DISTINCT Count(*), ST_GeometryType(geom), ";
        sql += "ST_Srid(geom) FROM test_ln";
        Statement stmt = conn.createStatement();
        rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            // read the result set
            String msg = "> Inserted ";
            msg += rs.getInt(1);
            msg += " entities of type ";
            msg += rs.getString(2);
            msg += " SRID=";
            msg += rs.getInt(3);
            System.out.println(msg);
        }
    }
    private void insertPointsAndCheck( Connection conn, Statement stmt ) throws SQLException {
        // conn.setAutoCommit(false);

        String sql = "INSERT INTO test_pt (id, name, geom) VALUES (?, '?', ?)";

        PreparedStatement pStmt = conn.prepareStatement(sql);

        ResultSet rs;
        int i;
        for( i = 0; i < 10000; i++ ) {
            // for POINTs we'll use full text sql statements

            sql = "INSERT INTO test_pt (id, name, geom) VALUES (";
            sql += i + 1;
            sql += ", 'test POINT #";
            sql += i + 1;
            sql += "', GeomFromText('POINT(";
            sql += i / 1000.0;
            sql += " ";
            sql += i / 1000.0;
            sql += ")', 4326))";
            // System.out.println(sql);
            stmt.addBatch(sql);
        }
        stmt.executeBatch();
        conn.commit();
        conn.setAutoCommit(true);

        // checking POINTs
        sql = "SELECT DISTINCT Count(*), ST_GeometryType(geom), ";
        sql += "ST_Srid(geom) FROM test_pt";
        rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            // read the result set
            String msg = "> Inserted ";
            msg += rs.getInt(1);
            msg += " entities of type ";
            msg += rs.getString(2);
            msg += " SRID=";
            msg += rs.getInt(3);
            System.out.println(msg);
        }
    }
    private void createPolygonTable( Statement stmt ) throws SQLException {
        String sql;
        // creating a POLYGON table
        sql = "CREATE TABLE test_pg (";
        sql += "id INTEGER NOT NULL PRIMARY KEY,";
        sql += "name TEXT NOT NULL)";
        stmt.execute(sql);
        // creating a POLYGON Geometry column
        sql = "SELECT AddGeometryColumn('test_pg', ";
        sql += "'geom', 4326, 'POLYGON', 'XY')";
        stmt.execute(sql);

        sql = "SELECT CreateSpatialIndex('test_pg', 'geom');";
        stmt.execute(sql);

    }
    private void createLineTable( Statement stmt ) throws SQLException {
        String sql;
        // creating a LINESTRING table
        sql = "CREATE TABLE test_ln (";
        sql += "id INTEGER NOT NULL PRIMARY KEY,";
        sql += "name TEXT NOT NULL)";
        stmt.execute(sql);
        // creating a LINESTRING Geometry column
        sql = "SELECT AddGeometryColumn('test_ln', ";
        sql += "'geom', 4326, 'LINESTRING', 'XY')";
        stmt.execute(sql);

        sql = "SELECT CreateSpatialIndex('test_ln', 'geom');";
        stmt.execute(sql);
    }
    private void createPointTable( Statement stmt ) throws SQLException {
        String sql;
        // creating a POINT table
        sql = "CREATE TABLE test_pt (";
        sql += "id INTEGER NOT NULL PRIMARY KEY,";
        sql += "name TEXT NOT NULL)";
        stmt.execute(sql);
        // creating a POINT Geometry column
        sql = "SELECT AddGeometryColumn('test_pt', ";
        sql += "'geom', 4326, 'POINT', 'XY')";
        stmt.execute(sql);

        sql = "SELECT CreateSpatialIndex('test_pt', 'geom');";
        stmt.execute(sql);

        sql = "SELECT id, name FROM test_pt";
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            // read the result set
            String msg = "> id= ";
            msg += rs.getInt(1);
            msg += "  name=";
            msg += rs.getString(2);
            System.out.println(msg);
        }
        System.out.println("DONE");
    }

    private void createDummyTable( Statement stmt ) throws SQLException {
        String sql;
        sql = "CREATE TABLE dummy (";
        sql += "id INTEGER NOT NULL PRIMARY KEY,";
        sql += "name TEXT NOT NULL)";
        stmt.execute(sql);
    }

    private void insertDummyAndCheck( Connection conn, Statement stmt ) throws SQLException {
        String sql;
        ResultSet rs;
        int i;
        for( i = 0; i < 100; i++ ) {
            // for POINTs we'll use full text sql statements
            sql = "INSERT INTO dummy (id, name) VALUES (";
            sql += i + 1;
            sql += ", 'test #";
            sql += i + 1;
            sql += "')";
            stmt.executeUpdate(sql);
        }
        conn.commit();

        // checking POINTs
        sql = "SELECT id, name FROM dummy";
        rs = stmt.executeQuery(sql);
        i = 0;
        while( rs.next() ) {
            // read the result set
            String msg = "> id= ";
            msg += rs.getInt(1);
            msg += "  name=";
            msg += rs.getString(2);
            if (i++ < 10)
                System.out.println(msg);
        }
    }

    private void printInfo( Statement stmt ) throws SQLException {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT sqlite_version(), spatialite_version(), spatialite_target_cpu()";
        ResultSet rs = stmt.executeQuery(sql);
        while( rs.next() ) {
            // read the result set
            String msg = "SQLite version: ";
            msg += rs.getString(1);
            System.out.println(msg);
            msg = "SpatiaLite version: ";
            msg += rs.getString(2);
            System.out.println(msg);
            msg = "target CPU: ";
            msg += rs.getString(3);
            System.out.println(msg);
        }
    }
    public static void main( String[] args ) throws ClassNotFoundException {
        new SpatialiteSample();
    }
}