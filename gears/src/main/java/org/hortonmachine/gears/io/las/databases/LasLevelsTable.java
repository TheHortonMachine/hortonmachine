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
package org.hortonmachine.gears.io.las.databases;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * Table to hold all the table sources.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasLevelsTable {
    public static final String TABLENAME = "laslevels";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_GEOM = ASpatialDb.DEFAULT_GEOM_FIELD_NAME;
    public static final String COLUMN_SOURCE_ID = "sources_id";

    public static final String COLUMN_AVG_ELEV = "avgelev";
    public static final String COLUMN_MIN_ELEV = "minelev";
    public static final String COLUMN_MAX_ELEV = "maxelev";

    public static final String COLUMN_AVG_INTENSITY = "avgintensity";
    public static final String COLUMN_MIN_INTENSITY = "minintensity";
    public static final String COLUMN_MAX_INTENSITY = "maxintensity";

    /**
     * Checks if the given level table exists.
     * 
     * @param db the database.
     * @param levelNum the level number to check.
     * @return <code>true</code> if the level table exists.
     * @throws Exception 
     */
    public static boolean hasLevel( ASpatialDb db, int levelNum ) throws Exception {
        String tablename = TABLENAME + levelNum;
        return db.hasTable(tablename);
    }

    public static void createTable( ASpatialDb db, int srid, int levelNum, boolean avoidIndex ) throws Exception {
        String tablename = TABLENAME + levelNum;
        if (!db.hasTable(tablename)) {
            String[] creates = {//
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTO_INCREMENT", //
                    COLUMN_SOURCE_ID + " INTEGER", //
                    COLUMN_AVG_ELEV + " REAL", //
                    COLUMN_MIN_ELEV + " REAL", //
                    COLUMN_MAX_ELEV + " REAL", //
                    COLUMN_AVG_INTENSITY + " INTEGER", //
                    COLUMN_MIN_INTENSITY + " INTEGER", //
                    COLUMN_MAX_INTENSITY + " INTEGER" //
            };

            db.createSpatialTable(tablename, srid, COLUMN_GEOM + " POLYGON", creates, null, avoidIndex);

            db.createIndex(tablename, COLUMN_SOURCE_ID, false);
            // db.createIndex(TABLENAME, COLUMN_MIN_GPSTIME, false);
            // db.createIndex(TABLENAME, COLUMN_MAX_GPSTIME, false);
            // db.createIndex(TABLENAME, COLUMN_MIN_ELEV, false);
            // db.createIndex(TABLENAME, COLUMN_MAX_ELEV, false);
            // db.createIndex(TABLENAME, COLUMN_MIN_INTENSITY, false);
            // db.createIndex(TABLENAME, COLUMN_MAX_INTENSITY, false);
        }
    }

    /**
     * Insert cell values in the table
     * @throws Exception 
     * 
     */
    public static void insertLasLevel( ASpatialDb db, int srid, LasLevel level ) throws Exception {
        String sql = "INSERT INTO " + TABLENAME + level.level//
                + " (" + //
                COLUMN_GEOM + "," + //
                COLUMN_SOURCE_ID + "," + //
                COLUMN_AVG_ELEV + "," + //
                COLUMN_MIN_ELEV + "," + //
                COLUMN_MAX_ELEV + "," + //
                COLUMN_AVG_INTENSITY + "," + //
                COLUMN_MIN_INTENSITY + "," + //
                COLUMN_MAX_INTENSITY + //
                ") VALUES (ST_GeomFromText(?, " + srid + "),?,?,?,?,?,?,?)";

        db.execOnConnection(conn -> {
            try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
                int i = 1;
                pStmt.setString(i++, level.polygon.toText());
                pStmt.setLong(i++, level.sourceId);
                pStmt.setDouble(i++, level.avgElev);
                pStmt.setDouble(i++, level.minElev);
                pStmt.setDouble(i++, level.maxElev);

                pStmt.setShort(i++, level.avgIntensity);
                pStmt.setShort(i++, level.minIntensity);
                pStmt.setShort(i++, level.maxIntensity);

                pStmt.executeUpdate();
            }
            return null;
        });

    }

    public static void insertLasLevels( ASpatialDb db, int srid, List<LasLevel> levels ) throws Exception {
        if (levels.size() == 0)
            return;
        String sql = "INSERT INTO " + TABLENAME + levels.get(0).level//
                + " (" + //
                COLUMN_GEOM + "," + //
                COLUMN_SOURCE_ID + "," + //
                COLUMN_AVG_ELEV + "," + //
                COLUMN_MIN_ELEV + "," + //
                COLUMN_MAX_ELEV + "," + //
                COLUMN_AVG_INTENSITY + "," + //
                COLUMN_MIN_INTENSITY + "," + //
                COLUMN_MAX_INTENSITY + //
                ") VALUES (ST_GeomFromText(?, " + srid + "),?,?,?,?,?,?,?)";

        db.execOnConnection(conn -> {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
                for( LasLevel level : levels ) {
                    int i = 1;
                    pStmt.setString(i++, level.polygon.toText());
                    pStmt.setLong(i++, level.sourceId);
                    pStmt.setDouble(i++, level.avgElev);
                    pStmt.setDouble(i++, level.minElev);
                    pStmt.setDouble(i++, level.maxElev);

                    pStmt.setShort(i++, level.avgIntensity);
                    pStmt.setShort(i++, level.minIntensity);
                    pStmt.setShort(i++, level.maxIntensity);
                    pStmt.addBatch();
                }
                pStmt.executeBatch();
                conn.commit();
                conn.setAutoCommit(autoCommit);
            }
            return null;
        });

    }

    /**
     * Query the las level table.
     *
     * @param db the db to use.
     * @param levelNum the level to query.
     * @param envelope an optional {@link Envelope} to query spatially.
     * @return the list of extracted level cells.
     * @throws Exception
     */
    public static List<LasLevel> getLasLevels( ASpatialDb db, int levelNum, Envelope envelope ) throws Exception {
        String tableName = TABLENAME + levelNum;
        List<LasLevel> lasLevels = new ArrayList<>();
        String sql = "SELECT " + COLUMN_GEOM + "," + //
                COLUMN_ID + "," + COLUMN_SOURCE_ID + "," + COLUMN_AVG_ELEV + "," + //
                COLUMN_MIN_ELEV + "," + //
                COLUMN_MAX_ELEV + "," + //
                COLUMN_AVG_INTENSITY + "," + //
                COLUMN_MIN_INTENSITY + "," + //
                COLUMN_MAX_INTENSITY;

        sql += " FROM " + tableName;

        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE " + db.getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }

        String _sql = sql;
        IGeometryParser gp = db.getType().getGeometryParser();
        return db.execOnConnection(conn -> {
            try (IHMStatement stmt = conn.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    LasLevel lasLevel = new LasLevel();
                    lasLevel.level = levelNum;
                    int i = 1;
                    Geometry geometry = gp.fromResultSet(rs, i++);
                    if (geometry instanceof Polygon) {
                        Polygon polygon = (Polygon) geometry;
                        lasLevel.polygon = polygon;
                        lasLevel.id = rs.getLong(i++);
                        lasLevel.sourceId = rs.getLong(i++);
                        lasLevel.avgElev = rs.getDouble(i++);
                        lasLevel.minElev = rs.getDouble(i++);
                        lasLevel.maxElev = rs.getDouble(i++);
                        lasLevel.avgIntensity = rs.getShort(i++);
                        lasLevel.minIntensity = rs.getShort(i++);
                        lasLevel.maxIntensity = rs.getShort(i++);
                        lasLevels.add(lasLevel);
                    }
                }
                return lasLevels;
            }
        });

    }

    /**
     * Query the las level table on a geometry intersection.
     *
     * @param db the db to use.
     * @param levelNum the level to query.
     * @param geometry an optional {@link Geometry} to query spatially.
     * @return the list of extracted points
     * @throws Exception
     */
    public static List<LasLevel> getLasLevels( ASpatialDb db, int levelNum, Geometry geometry ) throws Exception {
        String tableName = TABLENAME + levelNum;
        List<LasLevel> lasLevels = new ArrayList<>();
        String sql = "SELECT " + COLUMN_GEOM + "," + //
                COLUMN_ID + "," + COLUMN_SOURCE_ID + "," + COLUMN_AVG_ELEV + "," + //
                COLUMN_MIN_ELEV + "," + //
                COLUMN_MAX_ELEV + "," + //
                COLUMN_AVG_INTENSITY + "," + //
                COLUMN_MIN_INTENSITY + "," + //
                COLUMN_MAX_INTENSITY;

        sql += " FROM " + tableName;

        if (geometry != null) {
            sql += " WHERE " + db.getSpatialindexGeometryWherePiece(tableName, null, geometry);
        }

        String _sql = sql;
        IGeometryParser gp = db.getType().getGeometryParser();
        return db.execOnConnection(conn -> {
            try (IHMStatement stmt = conn.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    LasLevel lasLevel = new LasLevel();
                    lasLevel.level = levelNum;
                    int i = 1;
                    Geometry tmpGeometry = gp.fromResultSet(rs, i++);
                    if (tmpGeometry instanceof Polygon) {
                        Polygon polygon = (Polygon) tmpGeometry;
                        lasLevel.polygon = polygon;
                        lasLevel.id = rs.getLong(i++);
                        lasLevel.sourceId = rs.getLong(i++);
                        lasLevel.avgElev = rs.getDouble(i++);
                        lasLevel.minElev = rs.getDouble(i++);
                        lasLevel.maxElev = rs.getDouble(i++);
                        lasLevel.avgIntensity = rs.getShort(i++);
                        lasLevel.minIntensity = rs.getShort(i++);
                        lasLevel.maxIntensity = rs.getShort(i++);
                        lasLevels.add(lasLevel);
                    }
                }
                return lasLevels;
            }
        });

    }

}
