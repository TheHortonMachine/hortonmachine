
package org.hortonmachine.utils;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class JavaPreferences {
    private final Preferences preferences;

    public JavaPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public synchronized void clear() {
        try {
            this.preferences.clear();
        } catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }

    public synchronized boolean getBoolean(String key, boolean defaultValue) {
        return this.preferences.getBoolean(key, defaultValue);
    }

    public synchronized byte getByte(String key, byte defaultValue) {
        int intValue = this.preferences.getInt(key, defaultValue);
        if (intValue < Byte.MIN_VALUE || intValue > Byte.MAX_VALUE) {
            throw new IllegalStateException("byte value out of range: " + intValue);
        }
        return (byte) intValue;
    }

    public synchronized double getDouble(String key, double defaultValue) {
        return this.preferences.getDouble(key, defaultValue);
    }

    public synchronized float getFloat(String key, float defaultValue) {
        return this.preferences.getFloat(key, defaultValue);
    }

    public synchronized int getInt(String key, int defaultValue) {
        return this.preferences.getInt(key, defaultValue);
    }

    public synchronized long getLong(String key, long defaultValue) {
        return this.preferences.getLong(key, defaultValue);
    }

    public synchronized String getString(String key, String defaultValue) {
        return this.preferences.get(key, defaultValue);
    }

    public synchronized void putBoolean(String key, boolean value) {
        this.preferences.putBoolean(key, value);
    }

    public synchronized void putByte(String key, byte value) {
        this.preferences.putInt(key, value);
    }

    public synchronized void putDouble(String key, double value) {
        this.preferences.putDouble(key, value);
    }

    public synchronized void putFloat(String key, float value) {
        this.preferences.putFloat(key, value);
    }

    public synchronized void putInt(String key, int value) {
        this.preferences.putInt(key, value);
    }

    public synchronized void putLong(String key, long value) {
        this.preferences.putLong(key, value);
    }

    public synchronized void putString(String key, String value) {
        this.preferences.put(key, value);
    }

    public synchronized void save() {
        try {
            this.preferences.flush();
        } catch (BackingStoreException e) {
            throw new IllegalStateException(e);
        }
    }
}