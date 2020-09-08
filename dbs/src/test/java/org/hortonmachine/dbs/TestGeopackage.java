package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.geopackage.Entry;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.GeopackageTableNames;
import org.hortonmachine.dbs.geopackage.TileEntry;
import org.hortonmachine.dbs.geopackage.TileMatrix;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Main tests for geopackage
 */
public class TestGeopackage {

    @Test
    public void testReading() throws Exception {

        URL dataUrl = TestGeopackage.class.getClassLoader().getResource("gdal_sample.gpkg");
        File gpkgFile = new File(dataUrl.toURI());
        try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgFile.getAbsolutePath());
            db.initSpatialMetadata(null);

            HashMap<String, List<String>> tablesMap = db.getTablesMap(false);
            List<String> tables = tablesMap.get(GeopackageTableNames.USERDATA);
            assertEquals(16, tables.size());

            String point2DTable = "point2d";
            assertTrue(db.hasSpatialIndex(point2DTable));
            GeometryColumn geometryColumn = db.getGeometryColumnsForTable(point2DTable);
            assertEquals("geom", geometryColumn.geometryColumnName);
            assertEquals(0, geometryColumn.srid);
            List<Geometry> geometries = db.getGeometriesIn(point2DTable, (Envelope) null);
            geometries.removeIf(g -> g == null);
            assertEquals(1, geometries.size());
            assertEquals("POINT (1 2)", geometries.get(0).toText());

            String line2DTable = "linestring2d";
            assertTrue(db.hasSpatialIndex(line2DTable));
            geometryColumn = db.getGeometryColumnsForTable(line2DTable);
            assertEquals("geom", geometryColumn.geometryColumnName);
            assertEquals(4326, geometryColumn.srid);
            geometries = db.getGeometriesIn(line2DTable, (Envelope) null);
            geometries.removeIf(g -> g == null);
            assertEquals(1, geometries.size());
            assertEquals("LINESTRING (1 2, 3 4)", geometries.get(0).toText());

            // with spatial index
            String polygon2DTable = "polygon2d";
            assertTrue(db.hasSpatialIndex(polygon2DTable));
            geometryColumn = db.getGeometryColumnsForTable(polygon2DTable);
            assertEquals("geom", geometryColumn.geometryColumnName);
            assertEquals(32631, geometryColumn.srid);
            geometries = db.getGeometriesIn(polygon2DTable, new Envelope(-1, 11, -1, 11));
            geometries.removeIf(g -> g == null);
            assertEquals(1, geometries.size());
            assertEquals("POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0), (1 1, 1 9, 9 9, 9 1, 1 1))", geometries.get(0).toText());

            // has no spatial index
            String multipoint2DTable = "multipoint2d";
            assertFalse(db.hasSpatialIndex(multipoint2DTable));
            geometries = db.getGeometriesIn(multipoint2DTable, (Envelope) null);
            geometries.removeIf(g -> g == null);
            assertEquals(1, geometries.size());
            assertEquals("MULTIPOINT ((0 1), (2 3))", geometries.get(0).toText());

            String geomcollection2DTable = "geomcollection2d";
            assertTrue(db.hasSpatialIndex(geomcollection2DTable));
            geometries = db.getGeometriesIn(geomcollection2DTable, (Envelope) null);
            geometries.removeIf(g -> g == null);
            assertEquals(4, geometries.size());

            // with spatial index
            geometries = db.getGeometriesIn(geomcollection2DTable, new Envelope(9, 11, 9, 11));
            assertEquals(2, geometries.size());

            String point3DTable = "point3d";
            assertTrue(db.hasSpatialIndex(point3DTable));
            FeatureEntry feature = db.feature(point3DTable);
            assertEquals("POINT".toLowerCase(), feature.getGeometryType().getTypeName().toLowerCase());
            geometries = db.getGeometriesIn(point3DTable, (Envelope) null);
            geometries.removeIf(g -> g == null);
            assertEquals(1, geometries.size());

            // 3D geoms not supported by JTS WKBReader at the time being
            assertEquals("POINT (1 2)", geometries.get(0).toText());

        }
    }

    @Test
    public void testTilesGeotools() throws Exception {

        URL dataUrl = TestGeopackage.class.getClassLoader().getResource("test_tiles_srid.gpkg");
        File gpkgFile = new File(dataUrl.toURI());
        try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgFile.getAbsolutePath());
            db.initSpatialMetadata(null);

            HashMap<String, List<String>> tablesMap = db.getTablesMap(false);
            List<String> tables = tablesMap.get(GeopackageTableNames.USERDATA);
            assertEquals(1, tables.size());

            List<Entry> contents = db.contents();
            assertEquals(1, contents.size());

            TileEntry tileEntry = db.tile("test");
            assertNotNull(tileEntry);

            int srid = tileEntry.getSrid();
            assertEquals(3857, srid);

            Envelope bounds = tileEntry.getBounds();
            double delta = 0.000001;
            assertEquals(-1.50672670155739E7, bounds.getMinX(), delta);
            assertEquals(8570731.107560242, bounds.getMinY(), delta);
            assertEquals(-1.502813125709188E7, bounds.getMaxX(), delta);
            assertEquals(8609866.86604225, bounds.getMaxY(), delta);

            Envelope tileMatrixSetBounds = tileEntry.getTileMatrixSetBounds();
            assertEquals(-1.50672670155739E7, tileMatrixSetBounds.getMinX(), delta);
            assertEquals(8548683.634461217, tileMatrixSetBounds.getMinY(), delta);
            assertEquals(-1.500608378399286E7, tileMatrixSetBounds.getMaxX(), delta);
            assertEquals(8609866.86604225, tileMatrixSetBounds.getMaxY(), delta);

            List<TileMatrix> tileMatricies = tileEntry.getTileMatricies();
            assertEquals(3, tileMatricies.size());
        }
    }

    @Test
    public void testTilesGdaltranslate() throws Exception {

        URL dataUrl = TestGeopackage.class.getClassLoader().getResource("test_3857.gpkg");
        File gpkgFile = new File(dataUrl.toURI());
        try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgFile.getAbsolutePath());
            db.initSpatialMetadata(null);

            HashMap<String, List<String>> tablesMap = db.getTablesMap(false);
            List<String> tables = tablesMap.get(GeopackageTableNames.USERDATA);
            assertEquals(1, tables.size());

            List<Entry> contents = db.contents();
            assertEquals(1, contents.size());

            String tableName = "gboverviewplus";
            TileEntry tileEntry = db.tile(tableName);
            assertNotNull(tileEntry);

            int srid = tileEntry.getSrid();
            assertEquals(3857, srid);

            Envelope bounds = tileEntry.getBounds();
            double delta = 0.000001;

            assertEquals(-1001968.845655086, bounds.getMinX(), delta);
            assertEquals(6276897.685170724, bounds.getMinY(), delta);
            assertEquals(222246.5993602969, bounds.getMaxX(), delta);
            assertEquals(8625654.690317621, bounds.getMaxY(), delta);

            Envelope tileMatrixSetBounds = tileEntry.getTileMatrixSetBounds();
            assertEquals(-2.003750834278924E7, tileMatrixSetBounds.getMinX(), delta);
            assertEquals(-2.003750834278924E7, tileMatrixSetBounds.getMinY(), delta);
            assertEquals(2.003750834278924E7, tileMatrixSetBounds.getMaxX(), delta);
            assertEquals(2.003750834278924E7, tileMatrixSetBounds.getMaxY(), delta);

            List<TileMatrix> tileMatricies = tileEntry.getTileMatricies();
            assertEquals(9, tileMatricies.size());

            // 8/125/79.png Edinburg
            byte[] tile = db.getTile(tableName, 125, 79, 8);
            // ByteArrayInputStream bais = new ByteArrayInputStream(tile);
            // BufferedImage bufferedImage = ImageIO.read(bais);
            // ImageIO.write(bufferedImage, "png", new
            // File("test.png"));
            String encodeToString = Base64.getEncoder().encodeToString(tile);
            String expected = "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";
            assertEquals(expected, encodeToString);

            // 56.029/-3.594
            byte[] tile2 = db.getTile(tableName, -3.594, 56.029, 8);
            encodeToString = Base64.getEncoder().encodeToString(tile2);
            assertEquals(expected, encodeToString);

        }
    }

    @Test
    public void testCreation() throws Exception {

        File gpkgFile = TestUtilities.createTmpFile(".gpkg");
        gpkgFile.delete();
        try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgFile.getAbsolutePath());
            db.initSpatialMetadata(null);

            TestUtilities.createGeomTablesAndPopulate(db, false);
        }
        // reopen
        try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgFile.getAbsolutePath());
            db.initSpatialMetadata(null);

            HashMap<String, List<String>> tablesMap = db.getTablesMap(false);
            List<String> tables = tablesMap.get(GeopackageTableNames.USERDATA);
            assertEquals(6, tables.size());

            List<Geometry> geometries = db.getGeometriesIn(TestUtilities.MPOLY_TABLE, (Envelope) null);
            assertEquals(3, geometries.size());

            geometries = db.getGeometriesIn(TestUtilities.MPOLY_TABLE, new Envelope(0, 5, 0, 5));
            assertEquals(2, geometries.size());
        } finally {
            gpkgFile.delete();
        }
    }

}
