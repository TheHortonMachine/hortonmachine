package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageDb;
import org.junit.Test;

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
            
            assertTrue(db.hasSpatialIndex("point2d"));
            
            FeatureEntry feature = db.feature("point2d");
            
            System.out.println(feature.getGeometryColumn());
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
