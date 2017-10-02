package org.hortonmachine.dbs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.log.EMessageType;
import org.hortonmachine.dbs.log.LogDb;
import org.hortonmachine.dbs.log.Message;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for log db
 */
public class TestLogDb {

    private static final EDb DB_TYPE = DatabaseTypeForTests.DB_TYPE;
    private static LogDb logDb;

    @BeforeClass
    public static void createDb() throws Exception {
        String tempDir = System.getProperty("java.io.tmpdir");
        String dbPath = tempDir + File.separator + "jgt-dbs-testlogdb" + DB_TYPE.getExtensionOnCreation();
        TestUtilities.deletePrevious(tempDir, dbPath, DB_TYPE);

        logDb = new LogDb(DB_TYPE);
        logDb.open(dbPath);
    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (logDb != null) {
            logDb.close();
            new File(logDb.getDatabasePath() + "." + DB_TYPE.getExtension()).delete();
        }
    }

    @Test
    public void testInserts() throws Exception {
        logDb.insert(EMessageType.INFO, "INFO", "info 1");
        logDb.insert(EMessageType.ACCESS, "ACCESS", "access 1");
        logDb.insert(EMessageType.DEBUG, "DEBUG", "debug 1");
        logDb.insert(EMessageType.WARNING, "WARNING", "warning 1");
        logDb.insert(EMessageType.ERROR, "ERROR", "error 1");
        logDb.insert(EMessageType.INFO, "INFO", "info 2");
        logDb.insert(EMessageType.ACCESS, "ACCESS", "access 2");
        logDb.insert(EMessageType.DEBUG, "DEBUG", "debug 2");
        logDb.insert(EMessageType.WARNING, "WARNING", "warning 2");
        logDb.insert(EMessageType.ERROR, "ERROR", "error 2");
        logDb.insert(EMessageType.ERROR, "ERROR", "error 3");

        List<Message> list = logDb.getFilteredList(EMessageType.INFO, null, null, -1);
        assertEquals(2, list.size());
        list = logDb.getFilteredList(EMessageType.ERROR, null, null, -1);
        assertEquals(3, list.size());

        Date from = new Date();
        long fromTs = from.getTime() + 3000;
        list = logDb.getFilteredList(EMessageType.ALL, fromTs, null, -1);
        assertEquals(0, list.size());

        from = new Date();
        fromTs = from.getTime() - 100000;
        long toTs = from.getTime() + 100000;
        list = logDb.getFilteredList(null, fromTs, toTs, -1);
        assertEquals(11, list.size());
    }

}
