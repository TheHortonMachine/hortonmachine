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
package org.hortonmachine.gears.io.geopaparazzi.geopap4;


import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoBookmarks {

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_LON = "lon";
    private static final String COLUMN_LAT = "lat";
    private static final String COLUMN_TEXT = "text";
    private static final String COLUMN_ZOOM = "zoom";
    private static final String COLUMN_NORTHBOUND = "bnorth";
    private static final String COLUMN_SOUTHBOUND = "bsouth";
    private static final String COLUMN_WESTBOUND = "bwest";
    private static final String COLUMN_EASTBOUND = "beast";

    /**
     * Bookmarks table name.
     */
    public static final String TABLE_BOOKMARKS = "bookmarks";

    /**
     * Create bookmarks tables.
     *
     * @throws IOException  if something goes wrong.
     */
    public static void createTables(Connection connection) throws IOException, SQLException {
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_BOOKMARKS);
        sB.append(" (");
        sB.append(COLUMN_ID);
        sB.append(" INTEGER PRIMARY KEY, ");
        sB.append(COLUMN_LON).append(" REAL NOT NULL, ");
        sB.append(COLUMN_LAT).append(" REAL NOT NULL,");
        sB.append(COLUMN_ZOOM).append(" REAL NOT NULL,");
        sB.append(COLUMN_NORTHBOUND).append(" REAL NOT NULL,");
        sB.append(COLUMN_SOUTHBOUND).append(" REAL NOT NULL,");
        sB.append(COLUMN_WESTBOUND).append(" REAL NOT NULL,");
        sB.append(COLUMN_EASTBOUND).append(" REAL NOT NULL,");
        sB.append(COLUMN_TEXT).append(" TEXT NOT NULL ");
        sB.append(");");
        String CREATE_TABLE_BOOKMARKS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX bookmarks_x_by_y_idx ON ");
        sB.append(TABLE_BOOKMARKS);
        sB.append(" ( ");
        sB.append(COLUMN_LON);
        sB.append(", ");
        sB.append(COLUMN_LAT);
        sB.append(" );");
        String CREATE_INDEX_BOOKMARKS_X_BY_Y = sB.toString();

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE_BOOKMARKS);
            statement.executeUpdate(CREATE_INDEX_BOOKMARKS_X_BY_Y);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }

//    /**
//     * Add a bookmark.
//     *
//     * @param lon lon
//     * @param lat lat
//     * @param text a text
//     * @param zoom zoom level
//     * @param north north
//     * @param south south
//     * @param west west
//     * @param east east
//     * @throws IOException if something goes wrong.
//     */
//    public static void addBookmark( double lon, double lat, String text, double zoom, double north, double south, double west,
//            double east ) throws IOException {
//        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
//        sqliteDatabase.beginTransaction();
//        try {
//            ContentValues values = new ContentValues();
//            values.put(COLUMN_LON, lon);
//            values.put(COLUMN_LAT, lat);
//            values.put(COLUMN_TEXT, text);
//            values.put(COLUMN_ZOOM, zoom);
//            values.put(COLUMN_NORTHBOUND, north);
//            values.put(COLUMN_SOUTHBOUND, south);
//            values.put(COLUMN_WESTBOUND, west);
//            values.put(COLUMN_EASTBOUND, east);
//            sqliteDatabase.insertOrThrow(TABLE_BOOKMARKS, null, values);
//
//            sqliteDatabase.setTransactionSuccessful();
//        } catch (Exception e) {
//            DaoLog.error("DAOBOOKMARKS", e.getLocalizedMessage(), e);
//            throw new IOException(e.getLocalizedMessage());
//        } finally {
//            sqliteDatabase.endTransaction();
//        }
//    }





}
