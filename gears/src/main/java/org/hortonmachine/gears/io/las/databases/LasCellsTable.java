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

import java.nio.ByteBuffer;
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
import org.locationtech.jts.io.ParseException;

/**
 * Table to hold all the table sources.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasCellsTable {
    public static final String TABLENAME = "lascells";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_GEOM = ASpatialDb.DEFAULT_GEOM_FIELD_NAME;
    public static final String COLUMN_SOURCE_ID = "sources_id";
    public static final String COLUMN_POINTS_COUNT = "pointscount";

    public static final String COLUMN_AVG_ELEV = "avgelev";
    public static final String COLUMN_MIN_ELEV = "minelev";
    public static final String COLUMN_MAX_ELEV = "maxelev";
    public static final String COLUMN_POSITION_BLOB = "position_blob";

    public static final String COLUMN_AVG_INTENSITY = "avgintensity";
    public static final String COLUMN_MIN_INTENSITY = "minintensity";
    public static final String COLUMN_MAX_INTENSITY = "maxintensity";
    public static final String COLUMN_INTENS_CLASS_BLOB = "intens_class_blob";

    public static final String COLUMN_RETURNS_BLOB = "returns_blob";

    public static final String COLUMN_MIN_GPSTIME = "mingpstime";
    public static final String COLUMN_MAX_GPSTIME = "maxgpstime";
    public static final String COLUMN_GPSTIME_BLOB = "gpstime_blob";

    public static final String COLUMN_COLORS_BLOB = "colors_blob";

    public static void createTable( ASpatialDb db, int srid, boolean avoidIndex ) throws Exception {
        if (!db.hasTable(TABLENAME)) {
            String[] creates = {//
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTO_INCREMENT", //
                    COLUMN_SOURCE_ID + " INTEGER", //
                    COLUMN_POINTS_COUNT + " INTEGER", //
                    COLUMN_AVG_ELEV + " REAL", //
                    COLUMN_MIN_ELEV + " REAL", //
                    COLUMN_MAX_ELEV + " REAL", //
                    COLUMN_POSITION_BLOB + " BLOB", //
                    COLUMN_AVG_INTENSITY + " INTEGER", //
                    COLUMN_MIN_INTENSITY + " INTEGER", //
                    COLUMN_MAX_INTENSITY + " INTEGER", //
                    COLUMN_INTENS_CLASS_BLOB + " BLOB", //
                    COLUMN_RETURNS_BLOB + " BLOB", //
                    COLUMN_MIN_GPSTIME + " REAL", //
                    COLUMN_MAX_GPSTIME + " REAL", //
                    COLUMN_GPSTIME_BLOB + " BLOB", //
                    COLUMN_COLORS_BLOB + " BLOB"//
            };
            db.createSpatialTable(TABLENAME, srid, COLUMN_GEOM + " POLYGON", creates, null, avoidIndex);

            db.createIndex(TABLENAME, COLUMN_SOURCE_ID, false);
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
    public static void insertLasCell( ASpatialDb db, int srid, LasCell cell ) throws Exception {
        String sql = "INSERT INTO " + TABLENAME//
                + " (" + //
                COLUMN_GEOM + "," + //
                COLUMN_SOURCE_ID + "," + //
                COLUMN_POINTS_COUNT + "," + //
                COLUMN_AVG_ELEV + "," + //
                COLUMN_MIN_ELEV + "," + //
                COLUMN_MAX_ELEV + "," + //
                COLUMN_POSITION_BLOB + "," + //
                COLUMN_AVG_INTENSITY + "," + //
                COLUMN_MIN_INTENSITY + "," + //
                COLUMN_MAX_INTENSITY + "," + //
                COLUMN_INTENS_CLASS_BLOB + "," + //
                COLUMN_RETURNS_BLOB + "," + //
                COLUMN_MIN_GPSTIME + "," + //
                COLUMN_MAX_GPSTIME + "," + //
                COLUMN_GPSTIME_BLOB + "," + //
                COLUMN_COLORS_BLOB + //
                ") VALUES (ST_GeomFromText(?, " + srid + "),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        db.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                int i = 1;
                pStmt.setString(i++, cell.polygon.toText());
                pStmt.setLong(i++, cell.sourceId);
                pStmt.setInt(i++, cell.pointsCount);
                pStmt.setDouble(i++, cell.avgElev);
                pStmt.setDouble(i++, cell.minElev);
                pStmt.setDouble(i++, cell.maxElev);
                pStmt.setBytes(i++, cell.xyzs);

                pStmt.setShort(i++, cell.avgIntensity);
                pStmt.setShort(i++, cell.minIntensity);
                pStmt.setShort(i++, cell.maxIntensity);
                pStmt.setBytes(i++, cell.intensitiesClassifications);

                pStmt.setBytes(i++, cell.returns);

                pStmt.setDouble(i++, cell.minGpsTime);
                pStmt.setDouble(i++, cell.maxGpsTime);
                pStmt.setBytes(i++, cell.gpsTimes);

                pStmt.setBytes(i++, cell.colors);

                pStmt.executeUpdate();
            }
            return null;
        });

    }

    public static void insertLasCells( ASpatialDb db, int srid, List<LasCell> cells ) throws Exception {
        String sql = "INSERT INTO " + TABLENAME//
                + " (" + //
                COLUMN_GEOM + "," + //
                COLUMN_SOURCE_ID + "," + //
                COLUMN_POINTS_COUNT + "," + //
                COLUMN_AVG_ELEV + "," + //
                COLUMN_MIN_ELEV + "," + //
                COLUMN_MAX_ELEV + "," + //
                COLUMN_POSITION_BLOB + "," + //
                COLUMN_AVG_INTENSITY + "," + //
                COLUMN_MIN_INTENSITY + "," + //
                COLUMN_MAX_INTENSITY + "," + //
                COLUMN_INTENS_CLASS_BLOB + "," + //
                COLUMN_RETURNS_BLOB + "," + //
                COLUMN_MIN_GPSTIME + "," + //
                COLUMN_MAX_GPSTIME + "," + //
                COLUMN_GPSTIME_BLOB + "," + //
                COLUMN_COLORS_BLOB + //
                ") VALUES (ST_GeomFromText(?, " + srid + "),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        db.execOnConnection(conn -> {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
                for( LasCell cell : cells ) {
                    int i = 1;
                    pStmt.setString(i++, cell.polygon.toText());
                    pStmt.setLong(i++, cell.sourceId);
                    pStmt.setInt(i++, cell.pointsCount);
                    pStmt.setDouble(i++, cell.avgElev);
                    pStmt.setDouble(i++, cell.minElev);
                    pStmt.setDouble(i++, cell.maxElev);
                    pStmt.setBytes(i++, cell.xyzs);

                    pStmt.setShort(i++, cell.avgIntensity);
                    pStmt.setShort(i++, cell.minIntensity);
                    pStmt.setShort(i++, cell.maxIntensity);
                    pStmt.setBytes(i++, cell.intensitiesClassifications);

                    pStmt.setBytes(i++, cell.returns);

                    pStmt.setDouble(i++, cell.minGpsTime);
                    pStmt.setDouble(i++, cell.maxGpsTime);
                    pStmt.setBytes(i++, cell.gpsTimes);

                    pStmt.setBytes(i++, cell.colors);
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
     * Query the las cell table.
     *
     * @param db the db to use.
     * @param envelope an optional {@link Envelope} to query spatially.
     * @param exactGeometry an optional exact geometry. If available it is used instead of the envelope.
     * @param doPosition if <code>true</code> position info is extracted.
     * @param doIntensity if <code>true</code> intensity and classification info is extracted.
     * @param doReturns  if <code>true</code> return info is extracted.
     * @param doTime  if <code>true</code> time info is extracted.
     * @param doColor if <code>true</code> color info is extracted.
     * @param limitTo limit the cells to a value if != -1
     * @return the list of extracted points
     * @throws Exception
     */
    public static List<LasCell> getLasCells( ASpatialDb db, Envelope envelope, Geometry exactGeometry, boolean doPosition,
            boolean doIntensity, boolean doReturns, boolean doTime, boolean doColor, int limitTo ) throws Exception {
        List<LasCell> lasCells = new ArrayList<>();
        String sql = "SELECT " + COLUMN_GEOM + "," + COLUMN_ID + "," + COLUMN_SOURCE_ID + "," + COLUMN_POINTS_COUNT;

        if (doPosition)
            sql += "," + COLUMN_AVG_ELEV + "," + //
                    COLUMN_MIN_ELEV + "," + //
                    COLUMN_MAX_ELEV + "," + //
                    COLUMN_POSITION_BLOB;//

        if (doIntensity)
            sql += "," + COLUMN_AVG_INTENSITY + "," + //
                    COLUMN_MIN_INTENSITY + "," + //
                    COLUMN_MAX_INTENSITY + "," + //
                    COLUMN_INTENS_CLASS_BLOB;//

        if (doReturns)
            sql += "," + COLUMN_RETURNS_BLOB;

        if (doTime)
            sql += "," + COLUMN_MIN_GPSTIME + "," + //
                    COLUMN_MAX_GPSTIME + "," + //
                    COLUMN_GPSTIME_BLOB;
        if (doColor)
            sql += "," + COLUMN_COLORS_BLOB;

        sql += " FROM " + TABLENAME;

        if (exactGeometry != null) {
            sql += " WHERE " + db.getSpatialindexGeometryWherePiece(TABLENAME, null, exactGeometry);
        } else if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE " + db.getSpatialindexBBoxWherePiece(TABLENAME, null, x1, y1, x2, y2);
        }

        if (limitTo > 0) {
            sql += " LIMIT " + limitTo;
        }

        String _sql = sql;
        IGeometryParser gp = db.getType().getGeometryParser();
        return db.execOnConnection(conn -> {
            try (IHMStatement stmt = conn.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    LasCell lasCell = resultSetToCell(db, gp, doPosition, doIntensity, doReturns, doTime, doColor, rs);
                    lasCells.add(lasCell);
                }
                return lasCells;
            }
        });

    }

    /**
     * Query the las cell table on a geometry intersection.
     *
     * @param db the db to use.
     * @param geometry an optional {@link Geometry} to query spatially.
     * @param doPosition if <code>true</code> position info is extracted.
     * @param doIntensity if <code>true</code> intensity and classification info is extracted.
     * @param doReturns  if <code>true</code> return info is extracted.
     * @param doTime  if <code>true</code> time info is extracted.
     * @param doColor if <code>true</code> color info is extracted.
     * @return the list of extracted points
     * @throws Exception
     */
    public static List<LasCell> getLasCells( ASpatialDb db, Geometry geometry, boolean doPosition, boolean doIntensity,
            boolean doReturns, boolean doTime, boolean doColor ) throws Exception {
        List<LasCell> lasCells = new ArrayList<>();
        String sql = "SELECT " + COLUMN_GEOM + "," + COLUMN_ID + "," + COLUMN_SOURCE_ID + "," + COLUMN_POINTS_COUNT;

        if (doPosition)
            sql += "," + COLUMN_AVG_ELEV + "," + //
                    COLUMN_MIN_ELEV + "," + //
                    COLUMN_MAX_ELEV + "," + //
                    COLUMN_POSITION_BLOB;//

        if (doIntensity)
            sql += "," + COLUMN_AVG_INTENSITY + "," + //
                    COLUMN_MIN_INTENSITY + "," + //
                    COLUMN_MAX_INTENSITY + "," + //
                    COLUMN_INTENS_CLASS_BLOB;//

        if (doReturns)
            sql += "," + COLUMN_RETURNS_BLOB;

        if (doTime)
            sql += "," + COLUMN_MIN_GPSTIME + "," + //
                    COLUMN_MAX_GPSTIME + "," + //
                    COLUMN_GPSTIME_BLOB;
        if (doColor)
            sql += "," + COLUMN_COLORS_BLOB;

        sql += " FROM " + TABLENAME;

        if (geometry != null) {
            sql += " WHERE " + db.getSpatialindexGeometryWherePiece(TABLENAME, null, geometry);
        }

        String _sql = sql;
        IGeometryParser gp = db.getType().getGeometryParser();
        return db.execOnConnection(conn -> {
            try (IHMStatement stmt = conn.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    LasCell lasCell = resultSetToCell(db, gp, doPosition, doIntensity, doReturns, doTime, doColor, rs);
                    lasCells.add(lasCell);
                }
                return lasCells;
            }
        });

    }

    /**
     * Query the las cell table based on source.
     *
     * @param db the db to use.
     * @param geometry an optional {@link Geometry} to query spatially.
     * @param doPosition if <code>true</code> position info is extracted.
     * @param doIntensity if <code>true</code> intensity and classification info is extracted.
     * @param doReturns  if <code>true</code> return info is extracted.
     * @param doTime  if <code>true</code> time info is extracted.
     * @param doColor if <code>true</code> color info is extracted.
     * @return the list of extracted points
     * @throws Exception
     */
    public static List<LasCell> getLasCellsBySource( ASpatialDb db, long sourceId, boolean doPosition, boolean doIntensity,
            boolean doReturns, boolean doTime, boolean doColor ) throws Exception {
        List<LasCell> lasCells = new ArrayList<>();
        String sql = "SELECT " + COLUMN_GEOM + "," + COLUMN_ID + "," + COLUMN_SOURCE_ID + "," + COLUMN_POINTS_COUNT;

        if (doPosition)
            sql += "," + COLUMN_AVG_ELEV + "," + //
                    COLUMN_MIN_ELEV + "," + //
                    COLUMN_MAX_ELEV + "," + //
                    COLUMN_POSITION_BLOB;//

        if (doIntensity)
            sql += "," + COLUMN_AVG_INTENSITY + "," + //
                    COLUMN_MIN_INTENSITY + "," + //
                    COLUMN_MAX_INTENSITY + "," + //
                    COLUMN_INTENS_CLASS_BLOB;//

        if (doReturns)
            sql += "," + COLUMN_RETURNS_BLOB;

        if (doTime)
            sql += "," + COLUMN_MIN_GPSTIME + "," + //
                    COLUMN_MAX_GPSTIME + "," + //
                    COLUMN_GPSTIME_BLOB;
        if (doColor)
            sql += "," + COLUMN_COLORS_BLOB;

        sql += " FROM " + TABLENAME;
        sql += " WHERE " + COLUMN_SOURCE_ID + "=" + sourceId;

        String _sql = sql;
        IGeometryParser gp = db.getType().getGeometryParser();
        return db.execOnConnection(conn -> {
            try (IHMStatement stmt = conn.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    LasCell lasCell = resultSetToCell(db, gp, doPosition, doIntensity, doReturns, doTime, doColor, rs);
                    lasCells.add(lasCell);
                }
                return lasCells;
            }
        });
    }

    private static LasCell resultSetToCell( ASpatialDb db, IGeometryParser geometryParser, boolean doPosition,
            boolean doIntensity, boolean doReturns, boolean doTime, boolean doColor, IHMResultSet rs )
            throws Exception, ParseException {
        LasCell lasCell = new LasCell();
        int i = 1;

        Geometry tmpGeometry = geometryParser.fromResultSet(rs, i++);
        if (tmpGeometry instanceof Polygon) {
            Polygon polygon = (Polygon) tmpGeometry;
            lasCell.polygon = polygon;
            lasCell.id = rs.getLong(i++);
            lasCell.sourceId = rs.getLong(i++);
            lasCell.pointsCount = rs.getInt(i++);

            if (doPosition) {
                lasCell.avgElev = rs.getDouble(i++);
                lasCell.minElev = rs.getDouble(i++);
                lasCell.maxElev = rs.getDouble(i++);
                lasCell.xyzs = rs.getBytes(i++);
            }

            if (doIntensity) {
                lasCell.avgIntensity = rs.getShort(i++);
                lasCell.minIntensity = rs.getShort(i++);
                lasCell.maxIntensity = rs.getShort(i++);
                lasCell.intensitiesClassifications = rs.getBytes(i++);
            }

            if (doReturns)
                lasCell.returns = rs.getBytes(i++);

            if (doTime) {
                lasCell.minGpsTime = rs.getDouble(i++);
                lasCell.maxGpsTime = rs.getDouble(i++);
                lasCell.gpsTimes = rs.getBytes(i++);
            }
            if (doColor)
                lasCell.colors = rs.getBytes(i++);
            return lasCell;
        }
        return null;
    }

    public static double[][] getCellPositions( LasCell cell ) {
        int points = cell.pointsCount;
        if (points == 0) {
            return null;
        }
        double[][] xyzPoints = new double[points][3];
        ByteBuffer buffer = ByteBuffer.wrap(cell.xyzs);

        for( int i = 0; i < points; i++ ) {
            xyzPoints[i][0] = buffer.getDouble();
            xyzPoints[i][1] = buffer.getDouble();
            xyzPoints[i][2] = buffer.getDouble();
        }
        return xyzPoints;
    }

    public static short[][] getCellIntensityClass( LasCell cell ) {
        int points = cell.pointsCount;
        if (points == 0) {
            return null;
        }
        short[][] intensClassPoints = new short[points][2];
        ByteBuffer buffer = ByteBuffer.wrap(cell.intensitiesClassifications);

        for( int i = 0; i < points; i++ ) {
            intensClassPoints[i][0] = buffer.getShort();
            intensClassPoints[i][1] = buffer.getShort();
        }
        return intensClassPoints;
    }

    public static short[][] getCellColors( LasCell cell ) {
        int points = cell.pointsCount;
        if (points == 0) {
            return null;
        }
        short[][] colorPoints = new short[points][3];
        ByteBuffer buffer = ByteBuffer.wrap(cell.colors);

        for( int i = 0; i < points; i++ ) {
            colorPoints[i][0] = buffer.getShort();
            colorPoints[i][1] = buffer.getShort();
            colorPoints[i][2] = buffer.getShort();
        }
        return colorPoints;
    }

}
