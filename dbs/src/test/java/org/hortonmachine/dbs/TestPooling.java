package org.hortonmachine.dbs;

import static org.hortonmachine.dbs.TestUtilities.POINTS_TABLE;
import static org.hortonmachine.dbs.TestUtilities.arr;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.log.EMessageType;
import org.hortonmachine.dbs.log.LogDb;
import org.hortonmachine.dbs.log.Message;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests pooling 
 */
public class TestPooling {

    private static final EDb DB_TYPE = DatabaseTypeForTests.DB_TYPE;
    private static final EDb DB_TYPE_SPATIAL = DatabaseTypeForTests.DB_TYPE_SPATIAL;
    private static LogDb pooledLogDb;

    @BeforeClass
    public static void createDb() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testpoolingdb" + DB_TYPE.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, DB_TYPE);

        pooledLogDb = new LogDb(DB_TYPE);
        pooledLogDb.open(dbPath);

    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (pooledLogDb != null) {
            pooledLogDb.close();
            new File(pooledLogDb.getDatabasePath() + "." + DB_TYPE.getExtension()).delete();
        }
    }

    @Test
    public void testPooledInserts() throws Exception {

        final AtomicInteger aint = new AtomicInteger(0);
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(4);
        // writers
        for( int i = 0; i < 1000; i++ ) {
            fixedThreadPool.execute(new Runnable(){
                @Override
                public void run() {
                    try {
                        int index = aint.getAndIncrement();
                        // System.out.println("Start " + index);
                        pooledLogDb.insert(EMessageType.INFO, "INFO", "info " + index);
                        pooledLogDb.insert(EMessageType.ACCESS, "ACCESS", "access " + index);
                        pooledLogDb.insert(EMessageType.DEBUG, "DEBUG", "debug " + index);
                        pooledLogDb.insert(EMessageType.WARNING, "WARNING", "warning " + index);
                        // System.out.println("Stop " + index);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        fixedThreadPool.shutdown();
        fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
        fixedThreadPool.shutdownNow();

        List<Message> list = pooledLogDb.getFilteredList(EMessageType.INFO, null, null, -1);
        assertEquals(1000, list.size());
        list = pooledLogDb.getFilteredList(EMessageType.WARNING, null, null, -1);
        assertEquals(1000, list.size());

    }

    @Test
    public void testPooledSpatialInserts() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testpooledspatial" + DB_TYPE_SPATIAL.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, DB_TYPE_SPATIAL);

        ASpatialDb db = DB_TYPE_SPATIAL.getSpatialDb();
        db.open(dbPath);
        db.initSpatialMetadata("'WGS84'");

        ADatabaseSyntaxHelper dt = db.getType().getDatabaseSyntaxHelper();
        db.createSpatialTable(POINTS_TABLE, 4326, "the_geom POINT", arr("id " + dt.INTEGER() + " PRIMARY KEY"));

        long t1 = System.currentTimeMillis();
        // LOOP WITH BATCH
        long count = 1000;
        db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                double delta = 0.001;
                double value = 5;
                for( int i = 0; i < count; i++ ) {
                    value = value + delta;
                    stmt.addBatch("INSERT INTO " + POINTS_TABLE + " (id, the_geom) VALUES(" + i + ", ST_GeomFromText('POINT ("
                            + value + " " + value + ")', 4326));");
//                    if (i > 0 && i % 1000 == 0) {
//                        stmt.executeBatch();
//                    }
                }
                stmt.executeBatch();
            }
            return null;
        });
        long t2 = System.currentTimeMillis();

        // LOOP WITH SINGLE INSERTS
        double delta = 0.001;
        double value = 5;
        for( int i = 0; i < count; i++ ) {
            value = value + delta;
            int i2 = (int) (i + count);
            db.executeInsertUpdateDeleteSql("INSERT INTO " + POINTS_TABLE + " (id, the_geom) VALUES(" + i2
                    + ", ST_GeomFromText('POINT (" + value + " " + value + ")', 4326));");
        }
        long t3 = System.currentTimeMillis();

        // LOOP WITH THREADS
        value = 5;
        final AtomicInteger aint = new AtomicInteger((int) (2 * count));
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(4);
        for( int i = 0; i < count; i++ ) {
            value = value + delta;
            int i2 = aint.getAndIncrement();
            double _value = value;
            fixedThreadPool.execute(() -> {
                try {
                    db.executeInsertUpdateDeleteSql("INSERT INTO " + POINTS_TABLE + " (id, the_geom) VALUES(" + i2
                            + ", ST_GeomFromText('POINT (" + _value + " " + _value + ")', 4326));");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        fixedThreadPool.shutdown();
        fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
        fixedThreadPool.shutdownNow();

        long t4 = System.currentTimeMillis();

        System.out.println((t2 - t1) + " ---- " + (t3 - t2) + "----" + (t4 - t3));

        long count2 = db.getCount(POINTS_TABLE);
        assertEquals(count * 3, count2);

        db.close();
        new File(db.getDatabasePath() + "." + DB_TYPE.getExtension()).delete();
    }
}
