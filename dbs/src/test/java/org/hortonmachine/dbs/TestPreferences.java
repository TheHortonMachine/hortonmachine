package org.hortonmachine.dbs;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;

import org.hortonmachine.dbs.log.PreferencesDb;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for log db
 */
public class TestPreferences {

    private static PreferencesDb preferencesDb;

    @BeforeClass
    public static void createDb() throws Exception {
        preferencesDb = PreferencesDb.TESTINSTANCE;
        preferencesDb.clearPreferences();
    }

    @AfterClass
    public static void closeDb() throws Exception {
        if (preferencesDb != null) {
            File dbFile = preferencesDb.getDbFile();
            preferencesDb.close();
            dbFile.delete();
        }
    }

    @Test
    public void testInserts() throws Exception {
        String defaultValue = "test1result";
        String key1 = "test1";
        String preference = preferencesDb.getPreference(key1, defaultValue);
        assertEquals(defaultValue, preference);

        String setValue = "testSet";
        preferencesDb.setPreference(key1, setValue);
        preference = preferencesDb.getPreference(key1, defaultValue);
        assertEquals(setValue, preference);

        String key2 = "test2";
        String[] setValueArray = {"a", "b"};
        preferencesDb.setPreference(key2, setValueArray);
        String[] arrayPreference = preferencesDb.getPreference(key2, new String[0]);
        assertArrayEquals(setValueArray, arrayPreference);

        String key3 = "test3";
        byte[] setByteArray = {1, 2, 3};
        preferencesDb.setPreference(key3, setByteArray);
        byte[] bytesPreference = preferencesDb.getPreference(key3, new byte[0]);
        assertArrayEquals(setByteArray, bytesPreference);

        preferencesDb.setPreference(key3, null);
        byte[] bytesPreference2 = preferencesDb.getPreference(key3, (byte[]) null);
        assertNull(bytesPreference2);

    }

}
