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


import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.Exception;
import java.sql.*;
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
        sB.append(" INTEGER PRIMARY KEY, ");
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
        sB.append(" INTEGER PRIMARY KEY, ");
        sB.append(ImageDataTableFields.COLUMN_IMAGE.getFieldName()).append(" BLOB NOT NULL,");
        sB.append(ImageDataTableFields.COLUMN_THUMBNAIL.getFieldName()).append(" BLOB NOT NULL");
        sB.append(");");
        String CREATE_TABLE_IMAGEDATA = sB.toString();

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE_IMAGES);
            statement.executeUpdate(CREATE_INDEX_IMAGES_TS);
            statement.executeUpdate(CREATE_INDEX_IMAGES_X_BY_Y);
            statement.executeUpdate(CREATE_INDEX_IMAGES_NOTEID);
            statement.executeUpdate(CREATE_INDEX_IMAGES_ISDIRTY);
            statement.executeUpdate(CREATE_TABLE_IMAGEDATA);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }


    public static void addImage(Connection connection, long id, double lon, double lat, double altim, double azim, long timestamp, String text, byte[] image, byte[] thumb, long noteId)
            throws IOException, SQLException {

        String insertSQL1 = "INSERT INTO " + TableDescriptions.TABLE_IMAGE_DATA
                + "(" + //
                ImageDataTableFields.COLUMN_ID.getFieldName() + ", " + //
                ImageDataTableFields.COLUMN_IMAGE.getFieldName() + ", " + //
                ImageDataTableFields.COLUMN_THUMBNAIL.getFieldName() + //
                ") VALUES"
                + "(?,?,?)";
        try (PreparedStatement writeImageDataStatement = connection.prepareStatement(insertSQL1)) {
            writeImageDataStatement.setLong(1, id);
            writeImageDataStatement.setBytes(2, image);
            writeImageDataStatement.setBytes(3, thumb);

            writeImageDataStatement.executeUpdate();
        }


        String insertSQL2 = "INSERT INTO " + TableDescriptions.TABLE_IMAGES
                + "(" + //
                ImageTableFields.COLUMN_ID.getFieldName() + ", " + //
                ImageTableFields.COLUMN_LON.getFieldName() + ", " + //
                ImageTableFields.COLUMN_LAT.getFieldName() + ", " + //
                ImageTableFields.COLUMN_ALTIM.getFieldName() + ", " + //
                ImageTableFields.COLUMN_TS.getFieldName() + ", " + //
                ImageTableFields.COLUMN_AZIM.getFieldName() + ", " + //
                ImageTableFields.COLUMN_TEXT.getFieldName() + ", " + //
                ImageTableFields.COLUMN_ISDIRTY.getFieldName() + ", " + //
                ImageTableFields.COLUMN_NOTE_ID.getFieldName() + ", " + //
                ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName() +//
                ") VALUES"
                + "(?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement writeImageStatement = connection.prepareStatement(insertSQL2)) {
            writeImageStatement.setLong(1, id);
            writeImageStatement.setDouble(2, lon);
            writeImageStatement.setDouble(3, lat);
            writeImageStatement.setDouble(4, altim);
            writeImageStatement.setLong(5, timestamp);
            writeImageStatement.setDouble(6, azim);
            writeImageStatement.setString(7, text);
            writeImageStatement.setInt(8, 1);
            writeImageStatement.setLong(9, noteId);
            writeImageStatement.setLong(10, id);

            writeImageStatement.executeUpdate();
        }
    }


    public static String getImageName(Connection connection, long imageId) throws Exception {
        String sql = "select " +
                ImageTableFields.COLUMN_TEXT.getFieldName() + //
                " from " + TABLE_IMAGES + //
                " where " + ImageTableFields.COLUMN_ID.getFieldName() + " = " + imageId;

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                String text = rs.getString(ImageTableFields.COLUMN_TEXT.getFieldName());
                if (text != null && text.trim().length() != 0) {
                    return text;
                }
            }
            return null;
        }
    }


    /**
     * Get the list of Images from the db.
     *
     * @return list of notes.
     *
     * @throws IOException if something goes wrong.
     */
    public static List<Image> getImagesList(Connection connection) throws Exception {
        List<Image> images = new ArrayList<Image>();
        String sql = "select " + //
                ImageTableFields.COLUMN_ID.getFieldName() + "," +//
                ImageTableFields.COLUMN_LON.getFieldName() + "," +//
                ImageTableFields.COLUMN_LAT.getFieldName() + "," +//
                ImageTableFields.COLUMN_ALTIM.getFieldName() + "," +//
                ImageTableFields.COLUMN_TS.getFieldName() + "," +//
                ImageTableFields.COLUMN_AZIM.getFieldName() + "," +//
                ImageTableFields.COLUMN_TEXT.getFieldName() + "," +//
                ImageTableFields.COLUMN_NOTE_ID.getFieldName() + "," +//
                ImageTableFields.COLUMN_IMAGEDATA_ID.getFieldName() +//
                " from " + TABLE_IMAGES;

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                long id = rs.getLong(1);
                double lon = rs.getDouble(2);
                double lat = rs.getDouble(3);
                double altim = rs.getDouble(4);
                long ts = rs.getLong(5);
                double azim = rs.getDouble(6);
                String text = rs.getString(7);
                long noteId = rs.getLong(8);
                long imageDataId = rs.getLong(9);

                Image image = new Image(id, text, lon, lat, altim, azim, imageDataId, noteId, ts);
                images.add(image);
            }
        }
        return images;
    }


    /**
     * Get Image data from data id.
     *
     * @param connection
     * @param imageDataId
     * @return
     * @throws Exception
     */
    public static byte[] getImageData(Connection connection, long imageDataId) throws Exception {
        String sql = "select " + //
                ImageDataTableFields.COLUMN_IMAGE.getFieldName() +//
                " from " + TABLE_IMAGE_DATA + " where " +//
                ImageDataTableFields.COLUMN_ID.getFieldName() + " = " + imageDataId;

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery(sql);
            if (rs.next()) {
                byte[] bytes = rs.getBytes(1);
                return bytes;
            }
        }
        return null;
    }

}
