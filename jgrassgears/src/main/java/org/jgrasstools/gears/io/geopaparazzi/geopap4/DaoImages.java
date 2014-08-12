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
package org.jgrasstools.gears.io.geopaparazzi.geopap4;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.*;

/**
 * Data access object for images.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoImages {

    /**
     * Create the image tables.
     *
     * @throws IOException if something goes wrong.
     */
    public static void createTables(Connection connection) throws IOException, SQLException {
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_IMAGES);
        sB.append(" (");
        sB.append(ImageTableFields.COLUMN_ID.getFieldName());
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(ImageTableFields.COLUMN_LON.getFieldName()).append(" REAL NOT NULL, ");
        sB.append(ImageTableFields.COLUMN_LAT.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_ALTIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_AZIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName());
        sB.append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT " + ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName() + " REFERENCES ");
        sB.append(TABLE_IMAGE_DATA);
        sB.append("(");
        sB.append(ImageDataTableFields.COLUMN_ID);
        sB.append(") ON DELETE CASCADE,");
        sB.append(ImageTableFields.COLUMN_TS.getFieldName()).append(" DATE NOT NULL,");
        sB.append(ImageTableFields.COLUMN_TEXT.getFieldName()).append(" TEXT NOT NULL,");
        sB.append(ImageTableFields.COLUMN_NOTE_ID.getFieldName()).append(" INTEGER,");
        sB.append(ImageTableFields.COLUMN_ISDIRTY.getFieldName()).append(" INTEGER NOT NULL");
        sB.append(");");
        String CREATE_TABLE_IMAGES = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_ts_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(ImageTableFields.COLUMN_TS.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_IMAGES_TS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_x_by_y_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(ImageTableFields.COLUMN_LON.getFieldName());
        sB.append(", ");
        sB.append(ImageTableFields.COLUMN_LAT.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_IMAGES_X_BY_Y = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_noteid_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(ImageTableFields.COLUMN_NOTE_ID.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_IMAGES_NOTEID = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX images_isdirty_idx ON ");
        sB.append(TABLE_IMAGES);
        sB.append(" ( ");
        sB.append(ImageTableFields.COLUMN_ISDIRTY.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_IMAGES_ISDIRTY = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_IMAGE_DATA);
        sB.append(" (");
        sB.append(ImageDataTableFields.COLUMN_ID.getFieldName());
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(ImageDataTableFields.COLUMN_IMAGE.getFieldName()).append(" BLOB NOT NULL,");
        sB.append(ImageDataTableFields.COLUMN_THUMBNAIL.getFieldName()).append(" BLOB NOT NULL");
        sB.append(");");
        String CREATE_TABLE_IMAGEDATA = sB.toString();

        Statement statement = connection.createStatement();
        try {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE_IMAGES);
            statement.executeUpdate(CREATE_INDEX_IMAGES_TS);
            statement.executeUpdate(CREATE_INDEX_IMAGES_X_BY_Y);
            statement.executeUpdate(CREATE_INDEX_IMAGES_NOTEID);
            statement.executeUpdate(CREATE_INDEX_IMAGES_ISDIRTY);
            statement.executeUpdate(CREATE_TABLE_IMAGEDATA);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            statement.close();
        }
    }


//    public long addImage(double lon, double lat, double altim, double azim, long timestamp, String text, byte[] image, byte[] thumb, long noteId)
//            throws IOException {
//        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
//        sqliteDatabase.beginTransaction();
//        try {
//            // first insert image data
//            ContentValues imageDataValues = new ContentValues();
//            imageDataValues.put(ImageDataTableFields.COLUMN_IMAGE.getFieldName(), image);
//            imageDataValues.put(ImageDataTableFields.COLUMN_THUMBNAIL.getFieldName(), thumb);
//            long imageDataId = sqliteDatabase.insertOrThrow(TABLE_IMAGE_DATA, null, imageDataValues);
//
//            // then insert the image properties and reference to the image itself
//            ContentValues values = new ContentValues();
//            values.put(ImageTableFields.COLUMN_LON.getFieldName(), lon);
//            values.put(ImageTableFields.COLUMN_LAT.getFieldName(), lat);
//            values.put(ImageTableFields.COLUMN_ALTIM.getFieldName(), altim);
//            values.put(ImageTableFields.COLUMN_TS.getFieldName(), timestamp);
//            values.put(ImageTableFields.COLUMN_TEXT.getFieldName(), text);
//            values.put(ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName(), imageDataId);
//            values.put(ImageTableFields.COLUMN_AZIM.getFieldName(), azim);
//            values.put(ImageTableFields.COLUMN_ISDIRTY.getFieldName(), 1);
//            values.put(ImageTableFields.COLUMN_NOTE_ID.getFieldName(), noteId);
//            long imageId = sqliteDatabase.insertOrThrow(TABLE_IMAGES, null, values);
//
//            sqliteDatabase.setTransactionSuccessful();
//
//            return imageId;
//        } catch (Exception e) {
//            GPLog.error("DAOIMAGES", e.getLocalizedMessage(), e);
//            throw new IOException(e.getLocalizedMessage());
//        } finally {
//            sqliteDatabase.endTransaction();
//        }
//    }

}
