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
package org.hortonmachine.gears.utils;

import java.awt.ComponentOrientation;
import java.io.File;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hortonmachine.dbs.log.PreferencesDb;

public class PreferencesHandler {
    public static final String HM_PREF_PROXYPWD = "hm_pref_proxypwd";
    public static final String HM_PREF_PROXYUSER = "hm_pref_proxyuser";
    public static final String HM_PREF_PROXYPORT = "hm_pref_proxyport";
    public static final String HM_PREF_PROXYHOST = "hm_pref_proxyhost";
    public static final String HM_PREF_PROXYCHECK = "hm_pref_proxycheck";
    
    public static final String HM_PREF_SHP_CHARSET = "hm_pref_shp_charset";

    public static final String PREFS_NODE_NAME = "/org/hortonmachine/gui";

    public static final String LAST_PATH = "KEY_LAST_PATH";
    public static final String PREF_STRING_SEPARATORS = "@@@@";
    public static final String PREF_ORIENTATION = "PREF_ORIENTATION";
    public static final String LEFT_TO_RIGHT = "LEFT_TO_RIGHT";
    public static final String RIGHT_TO_LEFT = "RIGHT_TO_LEFT";

    private static PreferencesDb preferencesDb;
    static {
        preferencesDb = PreferencesDb.INSTANCE;
        if (!preferencesDb.isValid()) {
            preferencesDb = null;
        }
    }

    /**
     * Handle the last set path preference.
     * 
     * @return the last set path or the user home.
     */
    public static File getLastFile() {
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);

        String userHome = System.getProperty("user.home");
        String lastPath = preferences.get(LAST_PATH, userHome);
        File file = new File(lastPath);
        if (!file.exists()) {
            return new File(userHome);
        }
        return file;
    }

    /**
     * Save the passed path as last path available.
     * 
     * @param lastPath
     *            the last path to save.
     */
    public static void setLastPath( String lastPath ) {
        File file = new File(lastPath);
        if (!file.isDirectory()) {
            lastPath = file.getParentFile().getAbsolutePath();
        }
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        preferences.put(LAST_PATH, lastPath);
    }

    /**
     * Get from preference.
     * 
     * @param preferenceKey
     *            the preference key.
     * @param defaultValue
     *            the default value in case of <code>null</code>.
     * @return the string preference asked.
     */
    public static String getPreference( String preferenceKey, String defaultValue ) {
        if (preferencesDb != null) {
            return preferencesDb.getPreference(preferenceKey, defaultValue);
        }
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        String preference = preferences.get(preferenceKey, defaultValue);
        return preference;
    }

    public static String[] getPreference( String preferenceKey, String[] defaultValue ) {
        if (preferencesDb != null) {
            return preferencesDb.getPreference(preferenceKey, defaultValue);
        }
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        String preference = preferences.get(preferenceKey, "");
        String[] split = preference.split(PREF_STRING_SEPARATORS);
        return split;
    }

    public static byte[] getPreference( String preferenceKey, byte[] defaultValue ) {
        if (preferencesDb != null) {
            return preferencesDb.getPreference(preferenceKey, defaultValue);
        }
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        byte[] preference = preferences.getByteArray(preferenceKey, defaultValue);
        return preference;
    }

    /**
     * Set a preference.
     * 
     * @param preferenceKey
     *            the preference key.
     * @param value
     *            the value to set.
     */
    public static void setPreference( String preferenceKey, String value ) {
        if (preferencesDb != null) {
            preferencesDb.setPreference(preferenceKey, value);
            return;
        }

        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        if (value != null) {
            preferences.put(preferenceKey, value);
        } else {
            preferences.remove(preferenceKey);
        }
    }

    public static void setPreference( String preferenceKey, byte[] value ) {
        if (preferencesDb != null) {
            preferencesDb.setPreference(preferenceKey, value);
            return;
        }
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        if (value != null) {
            preferences.putByteArray(preferenceKey, value);
        } else {
            preferences.remove(preferenceKey);
        }
    }

    public static void setPreference( String preferenceKey, String[] valuesArray ) {
        if (preferencesDb != null) {
            preferencesDb.setPreference(preferenceKey, valuesArray);
            return;
        }
        Preferences preferences = Preferences.userRoot().node(PREFS_NODE_NAME);
        if (valuesArray != null) {
            int maxLength = Preferences.MAX_VALUE_LENGTH;
            String arrayToString = Stream.of(valuesArray).collect(Collectors.joining(PREF_STRING_SEPARATORS));

            // remove from last if it is too large
            int remIndex = valuesArray.length - 1;
            while( arrayToString.length() > maxLength ) {
                valuesArray[remIndex--] = "";
                arrayToString = Stream.of(valuesArray).collect(Collectors.joining(PREF_STRING_SEPARATORS));
            }

            preferences.put(preferenceKey, arrayToString);
        } else {
            preferences.remove(preferenceKey);
        }
    }

    public static String getShpCharset() {
        String charset = getPreference(HM_PREF_SHP_CHARSET, "");
        return charset.trim().length() == 0 ? null : charset;
    }

    public static ComponentOrientation getComponentOrientation() {
        String orientationString = getPreference(PREF_ORIENTATION, LEFT_TO_RIGHT);
        if (orientationString.equals(RIGHT_TO_LEFT)) {
            return ComponentOrientation.RIGHT_TO_LEFT;
        } else {
            return ComponentOrientation.LEFT_TO_RIGHT;
        }
    }

    public static void saveComponentOrientation( ComponentOrientation orientation ) {
        setPreference(PREF_ORIENTATION, orientation.isLeftToRight() ? LEFT_TO_RIGHT : RIGHT_TO_LEFT);
    }

    public static void saveComponentOrientation( String orientationString ) {
        setPreference(PREF_ORIENTATION, orientationString);
    }

    public static String getShpDoIndex() {
        // TODO implement this one
        return null;
    }

}
