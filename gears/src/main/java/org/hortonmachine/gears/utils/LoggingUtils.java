package org.hortonmachine.gears.utils;

import java.util.logging.*;

public enum LoggingUtils {
    INSTANCE;

    public static void silenceLogging() {
        // Remove every handler any library might have installed
        LogManager.getLogManager().reset();

        Logger root = Logger.getLogger("");
        root.setLevel(Level.OFF);
        root.setFilter(record -> false);

        // Disable geotools loggings
        for (String name : new String[] {
                "org.geotools",
                "org.geotools.gce",
                "org.geotools.gce.imagemosaic"
        }) {
            Logger l = Logger.getLogger(name);
            l.setLevel(Level.OFF);
            l.setUseParentHandlers(false);
            for (Handler h : l.getHandlers()) {
                l.removeHandler(h);
            }
            l.setFilter(record -> false);
        }
    }
}
