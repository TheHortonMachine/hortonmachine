package org.hortonmachine.dbs;

import java.io.File;
import java.sql.SQLException;

import org.h2.jdbc.JdbcSQLException;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.junit.Test;

/**
 * Test pwd support.
 * 
 * <b>WORKS ONLY FOR H2</b>
 */
public class TestDbsPwdMain {

    // works only for H2
    private static final EDb DB_TYPE = EDb.H2;

    @Test(expected = SQLException.class)
    public void testWrongPassword() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testdbsmain1" + DB_TYPE.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, DB_TYPE);

        try (ADb db = DB_TYPE.getDb()) {
            db.setCredentials("testuser", "testpwd");
            db.open(dbPath);
        }

        try (ADb db = DB_TYPE.getDb()) {
            db.setCredentials("testuser", "testpwd1");
            // to avoid slow test when pooling
            // we force single connection
            db.setMakePooled(false);
            db.open(dbPath);
        }
    }

    @Test(expected = SQLException.class)
    public void testEmptyPassword() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testdbsmain1" + DB_TYPE.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, DB_TYPE);

        try (ADb db = DB_TYPE.getDb()) {
            db.setCredentials("testuser", "testpwd");
            db.open(dbPath);
        }

        try (ADb db = DB_TYPE.getDb()) {
            // to avoid slow test when pooling
            // we force single connection
            db.setMakePooled(false);
            db.open(dbPath);
        }
    }

    @Test(expected = SQLException.class)
    public void testPasswordOnEmpty() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testdbsmain3" + DB_TYPE.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, DB_TYPE);

        try (ADb db = DB_TYPE.getDb()) {
            db.open(dbPath);
        }

        try (ADb db = DB_TYPE.getDb()) {
            db.setCredentials("testuser", "testpwd");
            // to avoid slow test when pooling
            // we force single connection
            db.setMakePooled(false);
            db.open(dbPath);
        }
    }

    @Test
    public void testRightPassword() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testdbsmain3" + DB_TYPE.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, DB_TYPE);

        try (ADb db = DB_TYPE.getDb()) {
            db.setCredentials("testuser", "testpwd");
            db.open(dbPath);
        }

        try (ADb db = DB_TYPE.getDb()) {
            db.setCredentials("testuser", "testpwd");
            db.open(dbPath);
        }
    }

}
