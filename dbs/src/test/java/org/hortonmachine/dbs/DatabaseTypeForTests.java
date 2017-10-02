package org.hortonmachine.dbs;

import org.hortonmachine.dbs.compat.EDb;

public class DatabaseTypeForTests {
    public static final boolean SPATIALITE = false;
    /**
     * The db type to test (set to h2 for online tests).
     */
    public static final EDb DB_TYPE = SPATIALITE ? EDb.SQLITE : EDb.H2;
    public static final EDb DB_TYPE_SPATIAL = SPATIALITE ? EDb.SPATIALITE : EDb.H2GIS;
}
