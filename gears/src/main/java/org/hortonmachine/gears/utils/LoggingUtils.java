package org.hortonmachine.gears.utils;

import java.util.ServiceConfigurationError;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public enum LoggingUtils {
    INSTANCE;

    /**
     * The logger of the GeoTools plugin registry, kept referenced so that its filter is not lost
     * if the logging framework drops unreferenced loggers.
     */
    private static Logger factoryRegistryLogger;

    /**
     * Hide the warnings of the plugin registry of GeoTools that are known to be harmless, leaving the others.
     *
     * <p>At the moment: the coverage operations of GeoScript that wrap JAI operations not available
     * in ImageN (e.g. <code>geoscript.layer.Raster$Divide</code>, since ImageN has no "Divide" operation).
     * GeoTools skips them and goes on, but logs the failure with its stacktrace the first time the
     * coverage operations are looked up.</p>
     *
     * <p>Can be called more than once.</p>
     */
    public static synchronized void hideKnownPluginWarnings() {
        if (factoryRegistryLogger != null)
            return;
        Logger logger = Logger.getLogger("org.geotools.util.factory");
        Filter previous = logger.getFilter();
        logger.setFilter(record -> !isKnownPluginWarning(record) && (previous == null || previous.isLoggable(record)));
        factoryRegistryLogger = logger;
    }

    private static boolean isKnownPluginWarning( LogRecord record ) {
        Throwable thrown = record.getThrown();
        if (!(thrown instanceof ServiceConfigurationError) || thrown.getMessage() == null)
            return false;
        String message = thrown.getMessage();
        return message.contains("Provider geoscript.layer.Raster$") && message.contains("could not be instantiated");
    }

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
