package org.jgrasstools.dbs;

import static org.jgrasstools.dbs.TestUtilities.createGeomTables;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.h2.tools.Server;
import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.EDb;
import org.jgrasstools.dbs.h2gis.H2GisDb;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import static org.jgrasstools.dbs.TestUtilities.*;

/**
 * Main tests for h2gis server db
 */
public class TestH2GisServer {

    private static final String TCP_LOCALHOST = "tcp://localhost:9092/";

    private static Server server;

    @BeforeClass
    public static void createDb() throws Exception {
        server = H2GisDb.startTcpServerMode("9092", false, null, false, null);
    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testGetGeometriesFromServer() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testspatialdbsservermain" + EDb.H2GIS.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, EDb.H2GIS);

        String tcpServerUrl = TCP_LOCALHOST + dbPath;

        try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
            db.open(tcpServerUrl);
            db.initSpatialMetadata("'WGS84'");

            createGeomTables(db);

            List<Geometry> intersecting = db.getGeometriesIn(MPOLY_TABLE, (Envelope) null);
            assertEquals(3, intersecting.size());
        }
    }

}
