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
import java.sql.SQLException;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.dbs.utils.OsCheck;
import org.jgrasstools.dbs.utils.OsCheck.OSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

import com.vividsolutions.jts.geom.Envelope;

/**
 * A spatialite database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteDb extends ASpatialDb {
    private static final Logger logger = LoggerFactory.getLogger(SpatialiteDb.class);

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
        try (IJGTStatement stmt = mConn.createStatement()) {
            // set timeout to 30 sec.
            stmt.setQueryTimeout(30);
            // load SpatiaLite
            try {
                OSType operatingSystemType = OsCheck.getOperatingSystemType();
                switch( operatingSystemType ) {
                case Linux:
                case MacOS:
                    try {
                        stmt.execute("SELECT load_extension('mod_rasterlite2.so', 'sqlite3_modrasterlite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            logger.info("Unable to load mod_rasterlite2.so: " + e.getMessage());
                        }
                        try {
                            stmt.execute("SELECT load_extension('mod_rasterlite2', 'sqlite3_modrasterlite_init')");
                        } catch (Exception e1) {
                            logger.info("Unable to load mod_rasterlite2: " + e1.getMessage());
                        }
                    }
                    try {
                        stmt.execute("SELECT load_extension('mod_spatialite.so', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            logger.info("Unable to load mod_spatialite.so: " + e.getMessage());
                        }
                        try {
                            stmt.execute("SELECT load_extension('mod_spatialite', 'sqlite3_modspatialite_init')");
                        } catch (Exception e1) {
                            logger.info("Unable to load mod_spatialite: " + e1.getMessage());
                        }
                        throw e;
                    }
                    break;
                default:
                    try {
                        stmt.execute("SELECT load_extension('mod_rasterlite2', 'sqlite3_modrasterlite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            logger.info("Unable to load mod_rasterlite2: " + e.getMessage());
                        }
                    }
                    try {
                        stmt.execute("SELECT load_extension('mod_spatialite', 'sqlite3_modspatialite_init')");
                    } catch (Exception e) {
                        if (mPrintInfos) {
                            logger.info("Unable to load mod_spatialite: " + e.getMessage());
                        }
                        throw e;
                    }
                    break;
                }
            } catch (Exception e) {
                throw e;
                // Map<String, String> getenv = System.getenv();
                // for( Entry<String, String> entry : getenv.entrySet() ) {
                // System.out.println(entry.getKey() + ": " + entry.getValue());
                // }
            }
        }
        return dbExists;
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        if (options == null) {
            options = "";
        }
        enableAutocommit(false);
        String sql = "SELECT InitSpatialMetadata(" + options + ")";
        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute(sql);
        }
        enableAutocommit(true);
    }

    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        String geomFieldName = gCol.f_geometry_column;

        String trySql = "SELECT extent_min_x, extent_min_y, extent_max_x, extent_max_y FROM vector_layers_statistics WHERE table_name='"
                + tableName + "' AND geometry_column='" + geomFieldName + "'";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(trySql)) {
            if (rs.next()) {
                double minX = rs.getDouble(1);
                double minY = rs.getDouble(2);
                double maxX = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                Envelope env = new Envelope(minX, maxX, minY, maxY);
                if (env.getWidth() != 0.0 && env.getHeight() != 0.0) {
                    return env;
                }
            }
        }

        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT Min(MbrMinX(" + geomFieldName + ")) AS min_x, Min(MbrMinY(" + geomFieldName + ")) AS min_y,"
                + "Max(MbrMaxX(" + geomFieldName + ")) AS max_x, Max(MbrMaxY(" + geomFieldName + ")) AS max_y " + "FROM "
                + tableName;

        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                double minX = rs.getDouble(1);
                double minY = rs.getDouble(2);
                double maxX = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                Envelope env = new Envelope(minX, maxX, minY, maxY);
                return env;
            }
            return null;
        }
    }

    /**
     * Get database infos.
     * 
     * @return the string array of [sqlite_version, spatialite_version,
     *         spatialite_target_cpu]
     * @throws SQLException
     */
    public String[] getDbInfo() throws Exception {
        // checking SQLite and SpatiaLite version + target CPU
        String sql = "SELECT sqlite_version(), spatialite_version(), spatialite_target_cpu()";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            String[] info = new String[3];
            while( rs.next() ) {
                // read the result set
                info[0] = rs.getString(1);
                info[1] = rs.getString(2);
                info[2] = rs.getString(3);
            }
            return info;
        }
    }

    /**
     * Delete a geo-table with all attached indexes and stuff.
     * 
     * @param tableName
     * @throws Exception
     */
    public void deleteGeoTable( String tableName ) throws Exception {
        String sql = "SELECT DropGeoTable('" + tableName + "');";

        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute(sql);
        }
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
