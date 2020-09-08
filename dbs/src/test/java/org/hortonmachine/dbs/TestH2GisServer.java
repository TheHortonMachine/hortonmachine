package org.hortonmachine.dbs;

import static org.hortonmachine.dbs.TestUtilities.MPOLY_TABLE;
import static org.hortonmachine.dbs.TestUtilities.createGeomTablesAndPopulate;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.h2.tools.Server;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.h2gis.H2GisServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Main tests for h2gis server db
 */
public class TestH2GisServer {

    private static final String TCP_LOCALHOST = "tcp://localhost:9092/";

    private static Server server;

    private static String dbPath;

    @BeforeClass
    public static void createDb() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        server = H2GisServer.startTcpServerMode("9092", false, null, true, tempDir);

        dbPath = tempDir + File.separator + "jgt-dbs-testspatialdbsservermain" + EDb.H2GIS.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, EDb.H2GIS);

        // create the local db to connect to
        try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
            db.open(dbPath);
            db.initSpatialMetadata("'WGS84'");
        }
    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (server != null) {
            Thread.sleep(1000);
            server.stop();
            new File(dbPath + "." + EDb.H2GIS.getExtension()).delete();
        }
    }

    @Test
    public void testGetGeometriesFromServer() throws Exception {

        String tcpServerUrl = TCP_LOCALHOST + dbPath;

        try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
            db.open(tcpServerUrl);
            db.initSpatialMetadata("'WGS84'");

            createGeomTablesAndPopulate(db, true);

            List<Geometry> intersecting = db.getGeometriesIn(MPOLY_TABLE, (Envelope) null);
            assertEquals(3, intersecting.size());
        }
    }

}
