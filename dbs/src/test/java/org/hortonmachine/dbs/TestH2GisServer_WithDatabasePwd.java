package org.hortonmachine.dbs;

import static org.hortonmachine.dbs.TestUtilities.*;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.h2.jdbc.JdbcSQLException;
import org.h2.tools.Server;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.h2gis.H2GisDb;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Main tests for h2gis server db
 */
public class TestH2GisServer_WithDatabasePwd {

    private static final String TCP_LOCALHOST = "tcp://localhost:9093/";

    private static Server server;

    private static String dbPath;

    @BeforeClass
    public static void createDb() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        server = H2GisDb.startTcpServerMode("9093", false, null, true, tempDir);

        dbPath = tempDir + File.separator + "jgt-dbs-testspatialdbsserverpwdmain" + EDb.H2GIS.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, EDb.H2GIS);

        // create the local db to connect to
        try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
            db.setCredentials("dbuser", "dbpwd");
            db.open(dbPath);
            db.initSpatialMetadata("'WGS84'");
        }
    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (server != null) {
            server.stop();
            new File(dbPath + "." + EDb.H2GIS.getExtension()).delete();
        }
    }

    @Test(expected = JdbcSQLException.class)
    public void testGetGeometriesFromServerWithoutPwd() throws Exception {
        connect(false);
    }

    @Test
    public void testGetGeometriesFromServerWithPwd() throws Exception {
        connect(true);
    }

    private void connect( boolean withDbPwd ) throws Exception {
        String tcpServerUrl = TCP_LOCALHOST + dbPath;

        try (ASpatialDb db = EDb.H2GIS.getSpatialDb()) {
            if (withDbPwd) {
                db.setCredentials("dbuser", "dbpwd");
            }
            db.open(tcpServerUrl);
            db.initSpatialMetadata("'WGS84'");

            createGeomTables(db);

            List<Geometry> intersecting = db.getGeometriesIn(MPOLY_TABLE, (Envelope) null);
            assertEquals(3, intersecting.size());
        }
    }

}
