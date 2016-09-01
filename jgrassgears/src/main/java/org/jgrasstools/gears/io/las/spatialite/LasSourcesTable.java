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
package org.jgrasstools.gears.io.las.spatialite;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.gears.spatialite.compat.IJGTConnection;
import org.jgrasstools.gears.spatialite.compat.IJGTPreparedStatement;
import org.jgrasstools.gears.spatialite.compat.IJGTResultSet;
import org.jgrasstools.gears.spatialite.compat.IJGTStatement;
import org.jgrasstools.gears.spatialite.compat.ASpatialDb;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Table to hold all the table sources.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LasSourcesTable {
    public static final String TABLENAME = "lassources";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_GEOM = "the_geom";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_RESOLUTION = "resolution";
    public static final String COLUMN_FACTOR = "factor";
    public static final String COLUMN_LEVELS = "levels";
    public static final String COLUMN_MINZ = "minelev";
    public static final String COLUMN_MAXZ = "maxelev";

    public static void createTable( ASpatialDb db, int srid ) throws Exception {
        if (!db.hasTable(TABLENAME)) {
            String[] creates = {//
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT", //
                    COLUMN_NAME + " TEXT", //
                    COLUMN_RESOLUTION + " REAL", //
                    COLUMN_FACTOR + " REAL", //
                    COLUMN_LEVELS + " INTEGER", //
                    COLUMN_MINZ + " REAL", //
                    COLUMN_MAXZ + " REAL"//
            };
            db.createTable(TABLENAME, creates);
            db.addGeometryXYColumnAndIndex(TABLENAME, COLUMN_GEOM, "POLYGON", String.valueOf(srid));
        }
    }

    /**
     * Insert values in the table
     * 
     * @param db the db to use.
     * @param srid the epsg code.
     * @param levels the levels that are created for this source.
     * @param resolution the resolution of trhe base cells.
     * @param factor the level multiplication factor.
     * @param polygon the polygon geometry bounds of the source.
     * @param name the name of the source.
     * @param minElev the min elevation.
     * @param maxElev the max elevation.
     * @throws SQLException
     */
    /**
     * @param db
     * @param srid
     * @param polygon
     * @param name
     * @param minElev
     * @param maxElev
     * @param factor
     * @return
     * @throws Exception 
     */
    public static long insertLasSource( ASpatialDb db, int srid, int levels, double resolution, double factor, Polygon polygon,
            String name, double minElev, double maxElev ) throws Exception {
        String sql = "INSERT INTO " + TABLENAME//
                + " (" + COLUMN_GEOM + "," + COLUMN_NAME + "," + COLUMN_RESOLUTION + "," //
                + COLUMN_FACTOR + "," + COLUMN_LEVELS + "," + COLUMN_MINZ + "," + COLUMN_MAXZ + //
                ") VALUES (GeomFromText(?, " + srid + "),?,?,?,?,?,?)";

        IJGTConnection conn = db.getConnection();
        try (IJGTPreparedStatement pStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pStmt.setString(1, polygon.toText());
            pStmt.setString(2, name);
            pStmt.setDouble(3, resolution);
            pStmt.setDouble(4, factor);
            pStmt.setInt(5, levels);
            pStmt.setDouble(6, minElev);
            pStmt.setDouble(7, maxElev);

            pStmt.executeUpdate();
            ResultSet rs = pStmt.getGeneratedKeys();
            rs.next();
            long generatedId = rs.getLong(1);

            return generatedId;
        }
    }

    /**
     * Query the las sources table.
     * 
     * @param db the db to use.
     * @return the list of available {@link LasSource}s.
     * @throws Exception
     */
    public static List<LasSource> getLasSources( ASpatialDb db ) throws Exception {
        List<LasSource> sources = new ArrayList<>();
        String sql = "SELECT ST_AsBinary(" + COLUMN_GEOM + ") AS " + COLUMN_GEOM + "," + COLUMN_ID + "," + COLUMN_NAME + ","
                + COLUMN_RESOLUTION + "," + COLUMN_FACTOR + "," + COLUMN_LEVELS + "," + COLUMN_MINZ + "," + COLUMN_MAXZ + " FROM "
                + TABLENAME;

        IJGTConnection conn = db.getConnection();
        WKBReader wkbReader = new WKBReader();
        try (IJGTStatement stmt = conn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                LasSource lasSource = new LasSource();
                int i = 1;
                byte[] geomBytes = rs.getBytes(i++);
                Geometry geometry = wkbReader.read(geomBytes);
                if (geometry instanceof Polygon) {
                    Polygon polygon = (Polygon) geometry;
                    lasSource.polygon = polygon;
                    lasSource.id = rs.getLong(i++);
                    lasSource.name = rs.getString(i++);
                    lasSource.resolution = rs.getDouble(i++);
                    lasSource.levelFactor = rs.getDouble(i++);
                    lasSource.levels = rs.getInt(i++);
                    lasSource.minElev = rs.getDouble(i++);
                    lasSource.maxElev = rs.getDouble(i++);
                    sources.add(lasSource);
                }
            }
            return sources;
        }
    }

    /**
     * Checks if the db is a las database readable by jgrasstools.
     * 
     * @param db the database to check.
     * @return <code>true</code> if the db can be read.
     * @throws Exception
     */
    public static boolean isLasDatabase( ASpatialDb db ) throws Exception {
        if (!db.hasTable(TABLENAME) || !db.hasTable(LasCellsTable.TABLENAME)) {
            return false;
        }
        return true;
    }

}
