/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.dbs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.h2gis.H2GisDb;
import org.hortonmachine.dbs.spatialite.hm.SpatialiteDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.junit.Assume;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

/**
 * Tests for {@link ASpatialDb#setTableSrid(SqlName, int)}: the srid changes, the coordinates don't.
 */
public class TestSetTableSrid {

    private static final String POLYGON_WKT = "POLYGON ((0 0, 0 10, 10 10, 10 0, 0 0), (1 1, 1 9, 9 9, 9 1, 1 1))";

    @Test
    public void testBlobHeaderPatch() throws Exception {
        // big endian header, srid 300001
        byte[] be = {0x47, 0x50, 0x00, 0x00, 0x00, 0x04, (byte) 0x93, (byte) 0xE1, 0x01};
        assertTrue(GeopackageCommonDb.setGeometryBlobSrid(be, 5070));
        assertArrayEquals(new byte[]{0x47, 0x50, 0x00, 0x00, 0x00, 0x00, 0x13, (byte) 0xCE, 0x01}, be);
        // little endian header
        byte[] le = {0x47, 0x50, 0x00, 0x01, (byte) 0xE1, (byte) 0x93, 0x04, 0x00, 0x01};
        assertTrue(GeopackageCommonDb.setGeometryBlobSrid(le, 5070));
        assertArrayEquals(new byte[]{0x47, 0x50, 0x00, 0x01, (byte) 0xCE, 0x13, 0x00, 0x00, 0x01}, le);
        // already set, not a geopackage blob
        assertFalse(GeopackageCommonDb.setGeometryBlobSrid(le, 5070));
        assertFalse(GeopackageCommonDb.setGeometryBlobSrid(new byte[]{0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00}, 5070));
        assertFalse(GeopackageCommonDb.setGeometryBlobSrid(null, 5070));
    }

    /**
     * GDAL writes little endian headers.
     */
    @Test
    public void testGeopackageGdalSample() throws Exception {
        URL dataUrl = TestSetTableSrid.class.getClassLoader().getResource("gdal_sample.gpkg");
        File gpkgFile = TestUtilities.createTmpFile(".gpkg");
        Files.copy(new File(dataUrl.toURI()).toPath(), gpkgFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgFile.getAbsolutePath());
            db.initSpatialMetadata(null);

            SqlName table = SqlName.m("polygon2d");
            assertEquals(32631, db.getGeometryColumnsForTable(table).srid);
            byte[] before = firstBlob(db, "polygon2d", "geom");
            assertEquals("GDAL headers are little endian", 1, before[3] & 0x01);
            int triggersBefore = countTriggers(db, "polygon2d");

            db.setTableSrid(table, 4326);

            checkGeopackage(db, table, 4326, triggersBefore);
            byte[] after = firstBlob(db, "polygon2d", "geom");
            assertEquals(4326, littleEndianInt(after, 4));
            // only the srid bytes changed
            for( int i = 0; i < before.length; i++ ) {
                if (i < 4 || i > 7) {
                    assertEquals(before[i], after[i]);
                }
            }
        } finally {
            gpkgFile.delete();
        }
    }

    /**
     * HM writes big endian headers. The NWI case: a custom srid that is in fact EPSG:5070.
     */
    @Test
    public void testGeopackageHmCreated() throws Exception {
        File gpkgFile = TestUtilities.createTmpFile(".gpkg");
        gpkgFile.delete();
        try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
            db.open(gpkgFile.getAbsolutePath());
            db.initSpatialMetadata(null);
            db.addCRS(300001, "USA_Contiguous_Albers_Equal_Area_Conic_USGS_version", "NONE", 300001, "PROJCS[\"custom\"]",
                    "custom albers");

            SqlName table = SqlName.m("wetlands");
            db.createSpatialTable(table, 300001, "geom POLYGON", new String[]{"fid INTEGER PRIMARY KEY AUTOINCREMENT"});
            Geometry polygon = new WKTReader().read(POLYGON_WKT);
            db.insertGeometry(table, polygon, "300001", null);
            db.insertGeometry(table, polygon, "300001", null);
            db.insertGeometry(table, polygon, "300001", null);
            int triggersBefore = countTriggers(db, "wetlands");
            assertTrue(triggersBefore > 0);

            // the srid has to be registered first
            try {
                db.setTableSrid(table, 5070);
                fail("5070 is not registered");
            } catch (IllegalArgumentException e) {
                // expected
            }
            assertEquals(300001, db.getGeometryColumnsForTable(table).srid);

            db.addCRS("EPSG", 5070, "PROJCS[\"NAD83 / Conus Albers\"]");
            db.setTableSrid(table, 5070);

            checkGeopackage(db, table, 5070, triggersBefore);
            byte[] blob = firstBlob(db, "wetlands", "geom");
            assertEquals("HM headers are big endian", 0, blob[3] & 0x01);
            // the old srs is kept
            assertTrue(db.hasCrs(300001));

            // the restored triggers keep the spatial index in sync
            db.insertGeometry(table, new WKTReader().read("POLYGON ((100 100, 100 110, 110 110, 110 100, 100 100))"), "5070",
                    null);
            assertEquals(1, db.getGeometriesIn(table, new Envelope(99, 111, 99, 111)).size());
        } finally {
            gpkgFile.delete();
        }
    }

    private static void checkGeopackage( GeopackageCommonDb db, SqlName table, int srid, int triggersBefore ) throws Exception {
        GeometryColumn gc = db.getGeometryColumnsForTable(table);
        assertEquals(srid, gc.srid);
        assertEquals(1, gc.isSpatialIndexEnabled);
        assertEquals(srid, db.feature(table).getSrid().intValue());
        assertEquals(triggersBefore, countTriggers(db, table.name));

        List<Geometry> geometries = db.getGeometriesIn(table, new Envelope(-1, 11, -1, 11));
        geometries.removeIf(g -> g == null);
        assertFalse(geometries.isEmpty());
        for( Geometry geometry : geometries ) {
            assertEquals(srid, geometry.getSRID());
            assertEquals(POLYGON_WKT, geometry.toText());
        }
    }

    @Test
    public void testH2Gis() throws Exception {
        File tmpFile = File.createTempFile("hm-h2gis-setsrid", "");
        tmpFile.delete();
        String dbPath = tmpFile.getAbsolutePath();
        try (H2GisDb db = new H2GisDb()) {
            db.open(dbPath);
            db.initSpatialMetadata(null);

            SqlName table = SqlName.m("wetlands");
            db.createSpatialTable(table, 4269, "the_geom POLYGON", new String[]{"id INT PRIMARY KEY"});
            insertWithSql(db, "INSERT INTO " + table.fixedDoubleName + " VALUES (1, ST_GeomFromText('" + POLYGON_WKT + "', 4269))");

            db.setTableSrid(table, 5070);

            GeometryColumn gc = db.getGeometryColumnsForTable(table);
            assertEquals(5070, gc.srid);
            assertEquals(1, gc.isSpatialIndexEnabled);
            List<Geometry> geometries = db.getGeometriesIn(table, new Envelope(-1, 11, -1, 11));
            assertEquals(1, geometries.size());
            assertEquals(5070, geometries.get(0).getSRID());
            assertEquals(POLYGON_WKT, geometries.get(0).toText());
        } finally {
            new File(dbPath + "." + EDb.H2GIS.getExtension()).delete();
        }
    }

    /**
     * Runs only where mod_spatialite can be loaded.
     */
    @Test
    public void testSpatialite() throws Exception {
        File dbFile = TestUtilities.createTmpFile(".sqlite");
        dbFile.delete();
        try (SpatialiteDb db = new SpatialiteDb()) {
            try {
                db.open(dbFile.getAbsolutePath());
            } catch (Exception e) {
                Assume.assumeNoException("mod_spatialite not available", e);
            }
            db.initSpatialMetadata(null);

            SqlName table = SqlName.m("wetlands");
            db.createSpatialTable(table, 4269, "the_geom POLYGON", new String[]{"id INTEGER PRIMARY KEY"});
            insertWithSql(db, "INSERT INTO " + table.fixedDoubleName + " VALUES (1, ST_GeomFromText('" + POLYGON_WKT + "', 4269))");
            int triggersBefore = countTriggers(db, "wetlands");

            db.setTableSrid(table, 5070);

            GeometryColumn gc = db.getGeometryColumnsForTable(table);
            assertEquals(5070, gc.srid);
            assertEquals(1, gc.isSpatialIndexEnabled);
            assertEquals(triggersBefore, countTriggers(db, "wetlands"));
            List<Geometry> geometries = db.getGeometriesIn(table, new Envelope(-1, 11, -1, 11));
            assertEquals(1, geometries.size());
            assertEquals(5070, geometries.get(0).getSRID());
            assertEquals(POLYGON_WKT, geometries.get(0).toText());

            // the restored constraint triggers check the new srid
            insertWithSql(db, "INSERT INTO " + table.fixedDoubleName + " VALUES (2, ST_GeomFromText('" + POLYGON_WKT + "', 5070))");
            try {
                insertWithSql(db,
                        "INSERT INTO " + table.fixedDoubleName + " VALUES (3, ST_GeomFromText('" + POLYGON_WKT + "', 4269))");
                fail("the old srid violates the geometry constraint");
            } catch (Exception e) {
                // expected
            }
        } finally {
            dbFile.delete();
        }
    }

    private static void insertWithSql( ADb db, String sql ) throws Exception {
        db.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                pStmt.executeUpdate();
            }
            return null;
        });
    }

    private static int countTriggers( ADb db, String table ) throws Exception {
        return db.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection
                    .prepareStatement("SELECT count(*) FROM sqlite_master WHERE type = 'trigger' AND lower(tbl_name) = lower(?)")) {
                pStmt.setString(1, table);
                try (IHMResultSet rs = pStmt.executeQuery()) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        });
    }

    private static byte[] firstBlob( ADb db, String table, String column ) throws Exception {
        return db.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection
                    .prepareStatement("SELECT \"" + column + "\" FROM \"" + table + "\" WHERE \"" + column + "\" IS NOT NULL")) {
                try (IHMResultSet rs = pStmt.executeQuery()) {
                    rs.next();
                    return rs.getBytes(1);
                }
            }
        });
    }

    private static int littleEndianInt( byte[] bytes, int offset ) {
        return (bytes[offset] & 0xFF) | (bytes[offset + 1] & 0xFF) << 8 | (bytes[offset + 2] & 0xFF) << 16
                | (bytes[offset + 3] & 0xFF) << 24;
    }
}
