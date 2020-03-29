/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.dbs.log;

import java.io.File;
import java.text.MessageFormat;
import java.util.prefs.Preferences;

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.utils.SerializationUtilities;

/**
 * A preferences database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum PreferencesDb implements AutoCloseable {
    INSTANCE("preferences_hortonmachine.sqlite"), TESTINSTANCE("test_java_preferences_hm.sqlite");

    public static final String PREFS_NODE_NAME = "/org/hortonmachine/dbs";
    public static final String HM_PREF_PREFFOLDER = "hm_pref_preffolder";

    private static final String TABLE_PREFERENCES = "preferences";

    private static final String KEY_NAME = "key";
    private static final String VALUE_NAME = "value";
    private static String selectSql = "select " + VALUE_NAME + " from " + TABLE_PREFERENCES + " where " + KEY_NAME + "=''{0}''";
    private static String insertSql = "replace into " + TABLE_PREFERENCES + "(" + KEY_NAME + "," + VALUE_NAME + ") VALUES (?, ?)";
    private static String deleteSql = "delete from " + TABLE_PREFERENCES + " where " + KEY_NAME + "=''{0}''";

    private ADb prefDb;
    private boolean isValid = true;

    private File prefDbFile;

    private PreferencesDb( String dbName ) {
        try {
            EDb dbType = EDb.SQLITE;
            prefDb = dbType.getDb();

            String folderPath;
            if (dbName.startsWith("test_java_preferences_hm")) {
                folderPath = System.getProperty("java.io.tmpdir");
            } else {
                Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
                File baseFolder = getBaseFolder();
                folderPath = preferences.get(HM_PREF_PREFFOLDER, baseFolder.getAbsolutePath());
            }
            prefDbFile = new File(folderPath + File.separator + dbName);
            boolean open = prefDb.open(prefDbFile.getAbsolutePath());
            if (!open) {
                createTable(dbType);
            }
        } catch (Exception e) {
            e.printStackTrace();
            isValid = false;
        }
    }

    public static File getBaseFolder() {
        String userHomeFolder = System.getProperty("user.home");
        File baseFolder = new File(userHomeFolder);
        if (!baseFolder.canWrite()) {
            // fallback to tmp folder
            String tempDir = System.getProperty("java.io.tmpdir");
            baseFolder = new File(tempDir);
        }
        return baseFolder;
    }

    public File getDbFile() {
        return prefDbFile;
    }

    public boolean isValid() {
        return isValid;
    }

    public void createTable( EDb dbType ) throws Exception {
        if (prefDb != null && !prefDb.hasTable(TABLE_PREFERENCES)) {
            ADatabaseSyntaxHelper helper = dbType.getDatabaseSyntaxHelper();
            String[] fields = { //
                    KEY_NAME + " " + helper.TEXT() + " " + helper.PRIMARYKEY(), //
                    VALUE_NAME + " " + helper.BLOB()};

            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ");
            sb.append(TABLE_PREFERENCES).append("(");
            for( int i = 0; i < fields.length; i++ ) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(fields[i]);
            }
            sb.append(");");

            String sql = prefDb.getType().getDatabaseSyntaxHelper().checkSqlCompatibilityIssues(sb.toString());
            prefDb.execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement()) {
                    stmt.execute(sql);
                }
                return null;
            });

            if (prefDb != null && prefDb.hasTable(TABLE_PREFERENCES)) {
                prefDb.createIndex(TABLE_PREFERENCES, KEY_NAME, true);
            }
        }
    }

    public byte[] getPreference( String preferenceKey ) {
        try {
            return prefDb.execOnConnection(connection -> {
                String sqlStr = MessageFormat.format(selectSql, preferenceKey);
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sqlStr)) {
                    if (rs.next()) {
                        byte[] prefBlob = rs.getBytes(1);
                        return prefBlob;
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setPreference( String preferenceKey, Object value ) {
        try {
            if (value == null) {
                // delete preference
                String sqlStr = MessageFormat.format(deleteSql, preferenceKey);
                prefDb.executeInsertUpdateDeleteSql(sqlStr);
            } else {
                prefDb.execOnConnection(connection -> {
                    byte[] serialized;
                    if (value instanceof byte[]) {
                        serialized = (byte[]) value;
                    } else {
                        serialized = SerializationUtilities.serialize(value);
                    }
                    try (IHMPreparedStatement pStmt = connection.prepareStatement(insertSql);) {
                        pStmt.setString(1, preferenceKey);
                        pStmt.setBytes(2, serialized);
                        pStmt.executeUpdate();
                        return null;
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getPreference( String preferenceKey, String defaultValue ) {
        byte[] result = getPreference(preferenceKey);
        if (result == null) {
            return defaultValue;
        }
        try {
            return SerializationUtilities.deSerialize(result, String.class);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public String[] getPreference( String preferenceKey, String[] defaultValue ) {
        byte[] result = getPreference(preferenceKey);
        if (result == null) {
            return defaultValue;
        }

        try {
            return SerializationUtilities.deSerialize(result, String[].class);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultValue;
        }
    }

    public byte[] getPreference( String preferenceKey, byte[] defaultValue ) {
        byte[] result = getPreference(preferenceKey);
        if (result == null) {
            return defaultValue;
        }
        return result;
    }

    public void clearPreferences() throws Exception {
        prefDb.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.execute("delete from " + TABLE_PREFERENCES);
            }
            return null;
        });
    }

    @Override
    public void close() throws Exception {
        if (prefDb != null)
            prefDb.close();
    }

}
