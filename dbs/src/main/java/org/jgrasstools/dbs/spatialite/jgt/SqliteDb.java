/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.dbs.spatialite.jgt;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import org.jgrasstools.dbs.compat.ADb;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

/**
 * A sqlite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SqliteDb extends ADb {
    private static final Logger logger = LoggerFactory.getLogger(SqliteDb.class);

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean open( String dbPath ) throws Exception {
        this.mDbPath = dbPath;

        boolean dbExists = false;
        if (dbPath != null) {
            File dbFile = new File(dbPath);
            if (dbFile.exists()) {
                if (mPrintInfos)
                    logger.info("Database exists");
                dbExists = true;
            }
        } else {
            dbPath = "file:inmemory?mode=memory";
            dbExists = true;
        }
        // enabling dynamic extension loading
        // absolutely required by SpatiaLite
        SQLiteConfig config = new SQLiteConfig();
        config.enableLoadExtension(true);
        // create a database connection
        Connection tmpConn = DriverManager.getConnection("jdbc:sqlite:" + dbPath, config.toProperties());
        mConn = new JGTConnection(tmpConn);
        if (mPrintInfos)
            try (IJGTStatement stmt = mConn.createStatement();
                    IJGTResultSet rs = stmt.executeQuery("SELECT sqlite_version() AS 'SQLite Version';")) {
                while( rs.next() ) {
                    String sqliteVersion = rs.getString(1);
                    logger.info("SQLite Version: " + sqliteVersion);
                }
            }
        return dbExists;
    }

    @Override
    protected void logWarn( String message ) {
        logger.warn(message);
    }

    @Override
    protected void logInfo( String message ) {
        logger.info(message);
    }

    @Override
    protected void logDebug( String message ) {
        logger.debug(message);
    }

}
