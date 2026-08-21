package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.utils.DbTimeseriesIterator;
import org.hortonmachine.dbs.utils.SqlName;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link DbTimeseriesIterator}.
 */
public class TestTimeseries {

    private static final String SINGLE_TABLE = "timeseries_single";
    private static final String MULTI_TABLE = "timeseries_multi";
    private static final int ROWS = 5;

    private static File gpkgFile;
    private static ADb db;

    @BeforeClass
    public static void createDb() throws Exception {
        gpkgFile = TestUtilities.createTmpFile(".gpkg");
        gpkgFile.delete();

        db = EDb.GEOPACKAGE.getDb();
        db.open(gpkgFile.getAbsolutePath());

        SqlName singleTable = SqlName.m(SINGLE_TABLE);
        db.createTable(singleTable, "timestamp INTEGER PRIMARY KEY", "value REAL");
        for (int i = 0; i < ROWS; i++) {
            db.executeInsertUpdateDeleteSql("INSERT INTO " + singleTable.fixedDoubleName + " VALUES(" + (1000 + i * 100)
                    + ", " + (i * 1.5) + ")");
        }

        SqlName multiTable = SqlName.m(MULTI_TABLE);
        db.createTable(multiTable, "timestamp INTEGER PRIMARY KEY", "valueA REAL", "valueB REAL", "valueC REAL");
        for (int i = 0; i < ROWS; i++) {
            db.executeInsertUpdateDeleteSql("INSERT INTO " + multiTable.fixedDoubleName + " VALUES(" + (1000 + i * 100)
                    + ", " + (i * 1.5) + ", " + (i * 10.0) + ", " + (i * 100.0) + ")");
        }
    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (db != null) {
            db.close();
        }
        if (gpkgFile != null) {
            gpkgFile.delete();
        }
    }

    @Test
    public void testSingleValue() throws Exception {
        // bufferSize smaller than the row count, to exercise refill() across
        // multiple buffer fills
        try (DbTimeseriesIterator it = new DbTimeseriesIterator(db, SINGLE_TABLE, "timestamp", "value", 2)) {
            int i = 0;
            while (it.next()) {
                assertEquals(1000 + i * 100, it.timestamp());
                assertEquals(i * 1.5, it.value(), 1e-9);

                double[] values = it.values();
                assertEquals(1, values.length);
                assertEquals(i * 1.5, values[0], 1e-9);
                i++;
            }
            assertEquals(ROWS, i);
        }
    }

    @Test
    public void testMultiValue() throws Exception {
        try (DbTimeseriesIterator it = new DbTimeseriesIterator(db, MULTI_TABLE, "timestamp",
                new String[]{"valueA", "valueB", "valueC"}, 2)) {
            int i = 0;
            while (it.next()) {
                assertEquals(1000 + i * 100, it.timestamp());
                assertEquals(i * 1.5, it.value(0), 1e-9);
                assertEquals(i * 10.0, it.value(1), 1e-9);
                assertEquals(i * 100.0, it.value(2), 1e-9);

                double[] values = it.values();
                assertEquals(3, values.length);
                assertEquals(i * 1.5, values[0], 1e-9);
                assertEquals(i * 10.0, values[1], 1e-9);
                assertEquals(i * 100.0, values[2], 1e-9);

                assertEquals(it.value(0), it.value(), 1e-9);
                i++;
            }
            assertEquals(ROWS, i);
        }
    }
}
