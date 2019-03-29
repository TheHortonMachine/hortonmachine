package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.rasterlite.Rasterlite2Coverage;
import org.hortonmachine.dbs.rasterlite.Rasterlite2Db;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

/**
 * Tests for log db
 */
public class TestRasterlite2 {

    //@Test this can't be run serverside - plus right now it works only on android
    public void testReading() throws Exception {
        URL dataUrl = TestRasterlite2.class.getClassLoader().getResource("1873.berlin_stadt_postgrenzen_rasterlite2.rl2");
        File file = new File(dataUrl.toURI());

        try (ASpatialDb db = EDb.SPATIALITE.getSpatialDb()) {
            db.open(file.getAbsolutePath());
            Rasterlite2Db rdb = new Rasterlite2Db(db);
            List<Rasterlite2Coverage> rasterCoverages = rdb.getRasterCoverages(true);

            Rasterlite2Coverage raster = rasterCoverages.get(2);
            assertEquals("berlin_stadtteilgrenzen.1880", raster.getName());

            Envelope bounds = raster.getBounds();
            
        }

    }

}
