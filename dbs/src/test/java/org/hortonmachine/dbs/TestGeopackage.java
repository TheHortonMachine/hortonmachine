package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageDb;
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
        try (GeopackageDb db = (GeopackageDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgFile.getAbsolutePath());
            db.initSpatialMetadata(null);

            List<String> tables = db.getTables(false);
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
//    @Test
//    public void testCreation() throws Exception {
//        
//        File gpkgFile = TestUtilities.createTmpFile(".gpkg");
//        
//        try (ASpatialDb db = EDb.GEOPACKAGE.getSpatialDb()) {
//            db.open(gpkgFile.getAbsolutePath());
//            db.initSpatialMetadata(null);
//            
//            List<String> tables = db.getTables(false);
//            assertEquals(10, tables.size());
//        } finally {
//            gpkgFile.delete();
//        }
//    }

}
