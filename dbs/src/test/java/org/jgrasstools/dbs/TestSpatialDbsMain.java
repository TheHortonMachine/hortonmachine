package org.jgrasstools.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.EDb;
import org.jgrasstools.dbs.compat.ISpatialTableNames;
import org.jgrasstools.dbs.compat.objects.ForeignKey;
import org.jgrasstools.dbs.compat.objects.QueryResult;
import org.jgrasstools.dbs.spatialite.jgt.SpatialiteDb;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Main tests for normal dbs
 */
public class TestSpatialDbsMain {

    private static final String MPOLY_TABLE = "multipoly";
    private static final String POLY_TABLE = "poly";
    private static final String MPOINTS_TABLE = "mpoints";
    private static final String POINTS_TABLE = "points";
    private static final String MLINES_TABLE = "mlines";
    private static final String LINES_TABLE = "lines";

    /**
     * The db type to test (set to h2gis for online tests).
     */
    public static final EDb DB_TYPE = EDb.H2GIS;
    private static ASpatialDb db;

    @BeforeClass
    public static void createDb() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testspatialdbsmain" + DB_TYPE.getExtensionOnCreation();
        String dbPathDelete = tempDir + File.separator + "jgt-dbs-testspatialdbsmain." + DB_TYPE.getExtension();
        File file = new File(dbPathDelete);
        file.delete();

        db = DB_TYPE.getSpatialDb();
        db.open(dbPath);
        db.initSpatialMetadata("'WGS84'");

        String[] multiPolygonInserts = {//
                "INSERT INTO " + MPOLY_TABLE + " (id, name, temperature, the_geom) VALUES(1, 'Tscherms', 36.0, "
                        + "ST_GeomFromText('MULTIPOLYGON (((0 10, 10 10, 10 0, 0 0, 0 10)))', 4326));", //
                "INSERT INTO " + MPOLY_TABLE + " (id, name, temperature, the_geom) VALUES(2, 'Meran', 34.0, "
                        + "ST_GeomFromText('MULTIPOLYGON (((0 20, 20 20, 20 0, 0 0, 0 20)),((21 11, 21 6, 25 6, 25 11, 21 11)), ((21 18, 21 16, 23 16, 23 18, 21 18)))', 4326));", //
                "INSERT INTO " + MPOLY_TABLE + " (id, name, temperature, the_geom) VALUES(3, 'Bozen', 42.0, "
                        + "ST_GeomFromText('MULTIPOLYGON (((50 80, 100 80, 100 50, 50 50, 50 80)))', 4326));", //
        };
        String[] polygonInserts = {//
                "INSERT INTO " + POLY_TABLE + " (id, name, temperature, the_geom) VALUES(1, 'Tscherms', 36.0, "
                        + "ST_GeomFromText('POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))', 4326));", //
                "INSERT INTO " + POLY_TABLE + " (id, name, temperature, the_geom) VALUES(2, 'Meran', 34.0, "
                        + "ST_GeomFromText('POLYGON ((0 20, 20 20, 20 0, 0 0, 0 20))', 4326));", //
                "INSERT INTO " + POLY_TABLE + " (id, name, temperature, the_geom) VALUES(3, 'Bozen', 42.0, "
                        + "ST_GeomFromText('POLYGON ((50 80, 100 80, 100 50, 50 50, 50 80))', 4326));", //
        };
        String[] multiPointsInserts = new String[]{//
                "INSERT INTO " + MPOINTS_TABLE
                        + " (id, table1id, the_geom) VALUES(1, 1, ST_GeomFromText('MULTIPOINT ((5 5), (7 7))', 4326));", //
                "INSERT INTO " + MPOINTS_TABLE
                        + " (id, table1id, the_geom) VALUES(2, 2, ST_GeomFromText('MULTIPOINT ((10 10))', 4326));", //
        };
        String[] pointsInserts = new String[]{//
                "INSERT INTO " + POINTS_TABLE + " (id, table1id, the_geom) VALUES(1, 1, ST_GeomFromText('POINT (5 5)', 4326));", //
                "INSERT INTO " + POINTS_TABLE + " (id, table1id, the_geom) VALUES(2, 2, ST_GeomFromText('POINT (10 10)', 4326));", //
                "INSERT INTO " + POINTS_TABLE + " (id, table1id, the_geom) VALUES(3, 3, ST_GeomFromText('POINT (75 75)', 4326));", //
        };
        String[] multiLineInserts = new String[]{//
                "INSERT INTO " + MLINES_TABLE
                        + " (id, table1id, the_geom) VALUES(1, 1, ST_GeomFromText('MULTILINESTRING ((-1 2, 21 2), (-1 4, 23 4, 23 12))', 4326));", //
        };
        String[] linesInserts = new String[]{//
                "INSERT INTO " + LINES_TABLE
                        + " (id, table1id, the_geom) VALUES(1, 1, ST_GeomFromText('LINESTRING (-1 3, 12 3, 12 -0.8)', 4326));", //
                "INSERT INTO " + LINES_TABLE
                        + " (id, table1id, the_geom) VALUES(2, 2, ST_GeomFromText('LINESTRING (20.5 20, 20.5 5)', 4326));", //
        };

        db.createSpatialTable(MPOLY_TABLE, 4326, "the_geom MULTIPOLYGON",
                arr("id INT PRIMARY KEY", "name VARCHAR(255)", "temperature REAL"), null);
        db.createSpatialTable(POLY_TABLE, 4326, "the_geom POLYGON",
                arr("id INT PRIMARY KEY", "name VARCHAR(255)", "temperature REAL"), null);

        db.createSpatialTable(MPOINTS_TABLE, 4326, "the_geom MULTIPOINT",
                arr("id INT PRIMARY KEY", "table1id INT", "FOREIGN KEY (table1id) REFERENCES " + MPOLY_TABLE + "(id)"), null);
        db.createSpatialTable(POINTS_TABLE, 4326, "the_geom POINT",
                arr("id INT PRIMARY KEY", "table1id INT", "FOREIGN KEY (table1id) REFERENCES " + POLY_TABLE + "(id)"), null);

        db.createSpatialTable(MLINES_TABLE, 4326, "the_geom MULTILINESTRING",
                arr("id INT PRIMARY KEY", "table1id INT", "FOREIGN KEY (table1id) REFERENCES " + MPOLY_TABLE + "(id)"), null);
        db.createSpatialTable(LINES_TABLE, 4326, "the_geom LINESTRING",
                arr("id INT PRIMARY KEY", "table1id INT", "FOREIGN KEY (table1id) REFERENCES " + POLY_TABLE + "(id)"), null);

        for( String insert : multiPolygonInserts ) {
            db.executeInsertUpdateDeleteSql(insert);
        }
        for( String insert : polygonInserts ) {
            db.executeInsertUpdateDeleteSql(insert);
        }
        for( String insert : multiPointsInserts ) {
            db.executeInsertUpdateDeleteSql(insert);
        }
        for( String insert : pointsInserts ) {
            db.executeInsertUpdateDeleteSql(insert);
        }
        for( String insert : multiLineInserts ) {
            db.executeInsertUpdateDeleteSql(insert);
        }
        for( String insert : linesInserts ) {
            db.executeInsertUpdateDeleteSql(insert);
        }

        if (DB_TYPE == EDb.SPATIALITE) {
            db.executeInsertUpdateDeleteSql("SELECT UpdateLayerStatistics();");
        }
    }

    private static String[] arr( String... strings ) {
        return strings;
    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (db != null) {
            db.close();
            new File(db.getDatabasePath() + "." + DB_TYPE.getExtension()).delete();
        }
    }

    @Test
    public void testTableOps() throws Exception {
        assertTrue(db.hasTable(MPOLY_TABLE));

        List<String[]> tableColumns = db.getTableColumns(MPOLY_TABLE);
        assertTrue(tableColumns.size() == 4);
        assertEquals("id", tableColumns.get(0)[0].toLowerCase());
        assertEquals("name", tableColumns.get(1)[0].toLowerCase());
        assertEquals("temperature", tableColumns.get(2)[0].toLowerCase());
        assertEquals("the_geom", tableColumns.get(3)[0].toLowerCase());

        HashMap<String, List<String>> tablesMap = db.getTablesMap(false);
        List<String> tables = tablesMap.get(ISpatialTableNames.USERDATA);
        assertTrue(tables.size() == 6);

        List<ForeignKey> foreignKeys = db.getForeignKeys(MPOLY_TABLE);
        assertEquals(0, foreignKeys.size());
        foreignKeys = db.getForeignKeys(MPOINTS_TABLE);
        assertEquals(1, foreignKeys.size());
    }

    @Test
    public void testContents() throws Exception {
        assertEquals(3, db.getCount(MPOLY_TABLE));

        String sql = "select id, name, temperature from " + MPOLY_TABLE + " order by temperature";
        QueryResult result = db.getTableRecordsMapFromRawSql(sql, 2);
        assertEquals(2, result.data.size());

        result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(3, result.data.size());

        assertEquals(-1, result.geometryIndex);
        double temperature = ((Number) result.data.get(0)[2]).doubleValue();
        assertEquals(34.0, temperature, 0.00001);
    }

    @Test
    public void testAllTablesCount() throws Exception {
        assertEquals(3, db.getCount(MPOLY_TABLE));
        String sql = "select * from " + MPOLY_TABLE;
        QueryResult result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(3, result.data.size());
        assertTrue(result.geometryIndex != -1);

        assertEquals(3, db.getCount(POLY_TABLE));
        sql = "select * from " + POLY_TABLE;
        result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(3, result.data.size());
        assertTrue(result.geometryIndex != -1);

        assertEquals(2, db.getCount(MPOINTS_TABLE));
        sql = "select * from " + MPOINTS_TABLE;
        result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(2, result.data.size());
        assertTrue(result.geometryIndex != -1);

        assertEquals(3, db.getCount(POINTS_TABLE));
        sql = "select * from " + POINTS_TABLE;
        result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(3, result.data.size());
        assertTrue(result.geometryIndex != -1);

        assertEquals(1, db.getCount(MLINES_TABLE));
        sql = "select * from " + MLINES_TABLE;
        result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(1, result.data.size());
        assertTrue(result.geometryIndex != -1);

        assertEquals(2, db.getCount(LINES_TABLE));
        sql = "select * from " + LINES_TABLE;
        result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(2, result.data.size());
        assertTrue(result.geometryIndex != -1);
    }

    @Test
    public void testBounds() throws Exception {
        Envelope tableBounds = db.getTableBounds(MPOLY_TABLE);
        Envelope expected = new Envelope(0, 100, 0, 80);
        assertEquals(expected, tableBounds);

        tableBounds = db.getTableBounds(POINTS_TABLE);
        expected = new Envelope(5, 75, 5, 75);
        assertEquals(expected, tableBounds);
    }

    @Test
    public void testGeometries() throws Exception {
        String sql = "select id, name, temperature, the_geom from " + MPOLY_TABLE + " order by temperature";
        QueryResult result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertFalse(result.geometryIndex == -1);
        List<Geometry> geomsList = new ArrayList<>();
        for( Object[] objs : result.data ) {
            assertTrue(objs[result.geometryIndex] instanceof Geometry);
            geomsList.add((Geometry) objs[result.geometryIndex]);
        }
        assertEquals(3, geomsList.size());
    }

    // @Test
    // public void testGetGeometries() throws Exception {
    // List<Geometry> intersecting = db.getGeometriesIn(TABLE1, (Envelope) null);
    // assertEquals(3, intersecting.size());
    // }

    // @Test
    // public void testIntersectsEnvelope() throws Exception {
    // Envelope bounds = new Envelope(5, 80, 5, 80);
    // List<Geometry> intersecting = db.getGeometriesIn(TABLE1, bounds);
    // assertEquals(3, intersecting.size());
    // }
    //
    // @Test
    // public void testIntersectsPolygon() throws Exception {
    // String polygonStr = "POLYGON ((71 70, 40 70, 40 40, 5 40, 5 15, 15 15, 15 4, 50 4, 71 70))";
    // Geometry geom = new WKTReader().read(polygonStr);
    // List<Geometry> intersecting = db.getGeometriesIn(TABLE1, geom);
    // assertEquals(2, intersecting.size());
    // }

}
