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
import com.vividsolutions.jts.io.WKTReader;

/**
 * Main tests for normal dbs
 */
public class TestSpatialDbsMain {

    private static final String TABLE1 = "table1";
    private static final String TABLE2 = "table2";
    /**
     * The db type to test (set to h2 for online tests).
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

        if (DB_TYPE == EDb.SPATIALITE) {
            SpatialiteDb spatialiteDb = (SpatialiteDb) db;
            db.createTable(TABLE1, "id INT PRIMARY KEY", "name VARCHAR(255)", "temperature REAL");
            spatialiteDb.addGeometryXYColumnAndIndex(TABLE1, "the_geom", "POLYGON", "4326");

            String[] inserts = {//
                    "INSERT INTO " + TABLE1
                            + " (id, name, temperature, the_geom) VALUES(1, 'Tscherms', 36.0, ST_GeomFromText('POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))', 4326));", //
                    "INSERT INTO " + TABLE1
                            + " (id, name, temperature, the_geom) VALUES(2, 'Meran', 34.0, ST_GeomFromText('POLYGON ((0 20, 20 20, 20 0, 0 0, 0 20))', 4326));", //
                    "INSERT INTO " + TABLE1
                            + " (id, name, temperature, the_geom) VALUES(3, 'Bozen', 42.0, ST_GeomFromText('POLYGON ((50 80, 100 80, 100 50, 50 50, 50 80))', 4326));", //
            };

            for( String insert : inserts ) {
                db.executeInsertUpdateDeleteSql(insert);
            }

            db.createTable(TABLE2, "id INT PRIMARY KEY", "table1id INT", "FOREIGN KEY (table1id) REFERENCES " + TABLE1 + "(id)");
            spatialiteDb.addGeometryXYColumnAndIndex(TABLE2, "the_geom", "POINT", "4326");

            inserts = new String[]{//
                    "INSERT INTO " + TABLE2 + " (id, table1id, the_geom) VALUES(1, 1, ST_GeomFromText('POINT (5 5)', 4326));", //
                    "INSERT INTO " + TABLE2 + " (id, table1id, the_geom) VALUES(2, 2, ST_GeomFromText('POINT (10 10)', 4326));", //
                    "INSERT INTO " + TABLE2 + " (id, table1id, the_geom) VALUES(3, 3, ST_GeomFromText('POINT (75 75)', 4326));", //
            };
            for( String insert : inserts ) {
                db.executeInsertUpdateDeleteSql(insert);
            }

            db.executeInsertUpdateDeleteSql("SELECT UpdateLayerStatistics();");
        } else if (DB_TYPE == EDb.H2GIS) {

            db.createTable(TABLE1, "id INT PRIMARY KEY", "name VARCHAR(255)", "temperature REAL", "the_geom POLYGON");

            String indexSql = "CREATE SPATIAL INDEX ON " + TABLE1 + "(the_geom)";
            db.executeInsertUpdateDeleteSql(indexSql);

            String[] inserts = {//
                    "INSERT INTO " + TABLE1
                            + " (id, name, temperature, the_geom) VALUES(1, 'Tscherms', 36.0, 'POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))');", //
                    "INSERT INTO " + TABLE1
                            + " (id, name, temperature, the_geom) VALUES(2, 'Meran', 34.0, 'POLYGON ((0 20, 20 20, 20 0, 0 0, 0 20))');", //
                    "INSERT INTO " + TABLE1
                            + " (id, name, temperature, the_geom) VALUES(3, 'Bozen', 42.0, 'POLYGON ((50 80, 100 80, 100 50, 50 50, 50 80))');", //
            };

            for( String insert : inserts ) {
                db.executeInsertUpdateDeleteSql(insert);
            }

            db.createTable(TABLE2, "id INT PRIMARY KEY", "table1id INT", "FOREIGN KEY (table1id) REFERENCES " + TABLE1 + "(id)",
                    "the_geom POINT");
            indexSql = "CREATE SPATIAL INDEX ON " + TABLE2 + "(the_geom)";
            db.executeInsertUpdateDeleteSql(indexSql);

            inserts = new String[]{//
                    "INSERT INTO " + TABLE2 + " (id, table1id, the_geom) VALUES(1, 1, 'POINT (5 5)');", //
                    "INSERT INTO " + TABLE2 + " (id, table1id, the_geom) VALUES(2, 2, 'POINT (10 10)');", //
                    "INSERT INTO " + TABLE2 + " (id, table1id, the_geom) VALUES(3, 3, 'POINT (75 75)');", //
            };
            for( String insert : inserts ) {
                db.executeInsertUpdateDeleteSql(insert);
            }
        } else {
            throw new IllegalArgumentException();
        }
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
        assertTrue(db.hasTable(TABLE1));

        List<String[]> tableColumns = db.getTableColumns(TABLE1);
        assertTrue(tableColumns.size() == 4);
        assertEquals("id", tableColumns.get(0)[0].toLowerCase());
        assertEquals("name", tableColumns.get(1)[0].toLowerCase());
        assertEquals("temperature", tableColumns.get(2)[0].toLowerCase());
        assertEquals("the_geom", tableColumns.get(3)[0].toLowerCase());

        HashMap<String, List<String>> tablesMap = db.getTablesMap(false);
        List<String> tables = tablesMap.get(ISpatialTableNames.USERDATA);
        assertTrue(tables.size() == 2);

        List<ForeignKey> foreignKeys = db.getForeignKeys(TABLE1);
        assertEquals(0, foreignKeys.size());
        foreignKeys = db.getForeignKeys(TABLE2);
        assertEquals(1, foreignKeys.size());
    }

    @Test
    public void testContents() throws Exception {
        assertEquals(3, db.getCount(TABLE1));

        String sql = "select id, name, temperature from " + TABLE1 + " order by temperature";
        QueryResult result = db.getTableRecordsMapFromRawSql(sql, 2);
        assertEquals(2, result.data.size());

        result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertEquals(3, result.data.size());

        assertEquals(-1, result.geometryIndex);
        double temperature = ((Number) result.data.get(0)[2]).doubleValue();
        assertEquals(34.0, temperature, 0.00001);
    }

    @Test
    public void testBounds() throws Exception {
        Envelope tableBounds = db.getTableBounds(TABLE1);
        Envelope expected = new Envelope(0, 100, 0, 80);
        assertEquals(expected, tableBounds);

        tableBounds = db.getTableBounds(TABLE2);
        expected = new Envelope(5, 75, 5, 75);
        assertEquals(expected, tableBounds);
    }

    @Test
    public void testGeometries() throws Exception {
        String sql = "select id, name, temperature, the_geom from " + TABLE1 + " order by temperature";
        QueryResult result = db.getTableRecordsMapFromRawSql(sql, -1);
        assertFalse(result.geometryIndex == -1);
        List<Geometry> geomsList = new ArrayList<>();
        for( Object[] objs : result.data ) {
            assertTrue(objs[result.geometryIndex] instanceof Geometry);
            geomsList.add((Geometry) objs[result.geometryIndex]);
        }
        assertEquals(3, geomsList.size());
    }

    @Test
    public void testIntersectsEnvelope() throws Exception {
        Envelope bounds = new Envelope(5, 80, 5, 80);
        List<Geometry> intersecting = db.getGeometriesIn(TABLE1, bounds);
        assertEquals(3, intersecting.size());
    }

    @Test
    public void testIntersectsPolygon() throws Exception {
        String polygonStr = "POLYGON ((71 70, 40 70, 40 40, 5 40, 5 15, 15 15, 15 4, 50 4, 71 70))";
        Geometry geom = new WKTReader().read(polygonStr);
        List<Geometry> intersecting = db.getGeometriesIn(TABLE1, geom);
        assertEquals(2, intersecting.size());
    }

}
