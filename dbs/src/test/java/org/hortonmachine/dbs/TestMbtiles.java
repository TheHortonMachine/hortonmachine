package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.mbtiles.MBTilesDb;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;

/**
 * Tests for log db
 */
public class TestMbtiles {

    private static final EDb DB_TYPE = EDb.H2;
    private static ADb testDb;

    @BeforeClass
    public static void createDb() throws Exception {
        URL dataUrl = TestMbtiles.class.getClassLoader().getResource("italy_h2gis_fortests.mbtiles.mv.db");
        File file = new File(dataUrl.toURI());
        String openPath = file.getAbsolutePath().replaceFirst(".mv.db", "");

        testDb = DB_TYPE.getDb();
        testDb.open(openPath);

    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (testDb != null) {
            testDb.close();
        }
    }

    @Test
    public void testReading() throws Exception {
        testTestDb(testDb);

    }

    private void testTestDb( ADb db ) throws Exception {
        MBTilesDb mdb = new MBTilesDb(db);
        // 6.6027284,35.489243,18.517426,47.085217
        Envelope bounds = mdb.getBounds();
        double delta = 0.00001;
        assertEquals(6.6027284, bounds.getMinX(), delta);
        assertEquals(35.489243, bounds.getMinY(), delta);
        assertEquals(18.517426, bounds.getMaxX(), delta);
        assertEquals(47.085217, bounds.getMaxY(), delta);

        assertEquals("png", mdb.getImageFormat());
        assertEquals(0, mdb.getMinZoom());
        assertEquals(5, mdb.getMaxZoom());
        assertEquals("italy", mdb.getName());
        assertEquals("italy", mdb.getDescription());
        assertEquals("1.1", mdb.getVersion());

        int[] boundsInTileIndex = mdb.getBoundsInTileIndex(0);
        assertEquals(0, boundsInTileIndex[0]);
        assertEquals(0, boundsInTileIndex[1]);
        assertEquals(0, boundsInTileIndex[2]);
        assertEquals(0, boundsInTileIndex[3]);

        boundsInTileIndex = mdb.getBoundsInTileIndex(3);
        assertEquals(4, boundsInTileIndex[0]);
        assertEquals(4, boundsInTileIndex[1]);
        assertEquals(4, boundsInTileIndex[2]);
        assertEquals(5, boundsInTileIndex[3]);

        boundsInTileIndex = mdb.getBoundsInTileIndex(5);
        assertEquals(16, boundsInTileIndex[0]);
        assertEquals(17, boundsInTileIndex[1]);
        assertEquals(19, boundsInTileIndex[2]);
        assertEquals(20, boundsInTileIndex[3]);

        byte[] tileAsBytes = mdb.getTile(16, 19, 5);
        assertNotNull(tileAsBytes);
        tileAsBytes = mdb.getTile(17, 19, 5);
        assertNotNull(tileAsBytes);
        tileAsBytes = mdb.getTile(16, 20, 5);
        assertNotNull(tileAsBytes);
        tileAsBytes = mdb.getTile(17, 20, 5);
        assertNotNull(tileAsBytes);

    }

    @Test
    public void testWriting() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testmbtiles.mbtiles" + DB_TYPE.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, DB_TYPE);

        MBTilesDb fromMdb = new MBTilesDb(testDb);
        Envelope fromBounds = fromMdb.getBounds();

        try (ADb toDb = EDb.H2.getDb()) {
            toDb.open(dbPath);
            MBTilesDb toMdb = new MBTilesDb(toDb);

            toMdb.createTables(false);
            toMdb.fillMetadata((float) fromBounds.getMaxY(), (float) fromBounds.getMinY(), (float) fromBounds.getMinX(),
                    (float) fromBounds.getMaxX(), fromMdb.getName(), fromMdb.getImageFormat(), fromMdb.getMinZoom(),
                    fromMdb.getMaxZoom());

            List<Integer> availableZoomLevels = fromMdb.getAvailableZoomLevels();
            for( int zoomLevel : availableZoomLevels ) {
                int[] boundsInTileIndex = fromMdb.getBoundsInTileIndex(zoomLevel);

                for( int col = boundsInTileIndex[0]; col <= boundsInTileIndex[1]; col++ ) {
                    for( int row = boundsInTileIndex[2]; row <= boundsInTileIndex[3]; row++ ) {
                        byte[] tileData = fromMdb.getTile(col, row, zoomLevel);
                        toMdb.addTile(col, row, zoomLevel, tileData);
                    }
                }
            }

            toMdb.createIndexes();
        }

        try (ADb toDb = EDb.H2.getDb()) {
            toDb.open(dbPath);
            testTestDb(toDb);
        }
        new File(dbPath + "." + DB_TYPE.getExtension()).delete();

    }

}
