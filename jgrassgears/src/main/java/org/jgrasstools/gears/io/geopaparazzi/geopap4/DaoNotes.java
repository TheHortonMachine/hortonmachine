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


import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.*;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoNotes {

    /**
     * Create the notes tables.
     *
     * @throws IOException if something goes wrong.
     */
    public static void createTables(Connection connection) throws IOException, SQLException {
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_NOTES);
        sB.append(" (");
        sB.append(NotesTableFields.COLUMN_ID.getFieldName());
        sB.append(" INTEGER PRIMARY KEY, ");
        sB.append(NotesTableFields.COLUMN_LON.getFieldName()).append(" REAL NOT NULL, ");
        sB.append(NotesTableFields.COLUMN_LAT.getFieldName()).append(" REAL NOT NULL,");
        sB.append(NotesTableFields.COLUMN_ALTIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(NotesTableFields.COLUMN_TS.getFieldName()).append(" DATE NOT NULL,");
        sB.append(NotesTableFields.COLUMN_DESCRIPTION.getFieldName()).append(" TEXT, ");
        sB.append(NotesTableFields.COLUMN_TEXT.getFieldName()).append(" TEXT NOT NULL, ");
        sB.append(NotesTableFields.COLUMN_FORM.getFieldName()).append(" CLOB, ");
        sB.append(NotesTableFields.COLUMN_STYLE.getFieldName()).append(" TEXT,");
        sB.append(NotesTableFields.COLUMN_ISDIRTY.getFieldName()).append(" INTEGER");
        sB.append(");");
        String CREATE_TABLE_NOTES = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX notes_ts_idx ON ");
        sB.append(TABLE_NOTES);
        sB.append(" ( ");
        sB.append(NotesTableFields.COLUMN_TS.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_NOTES_TS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX notes_x_by_y_idx ON ");
        sB.append(TABLE_NOTES);
        sB.append(" ( ");
        sB.append(NotesTableFields.COLUMN_LON.getFieldName());
        sB.append(", ");
        sB.append(NotesTableFields.COLUMN_LAT.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_NOTES_X_BY_Y = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX notes_isdirty_idx ON ");
        sB.append(TABLE_NOTES);
        sB.append(" ( ");
        sB.append(NotesTableFields.COLUMN_ISDIRTY.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_NOTES_ISDIRTY = sB.toString();

        Statement statement = connection.createStatement();
        try {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            //            System.out.println("CREATE TABLE: " + CREATE_TABLE_NOTES);
            statement.executeUpdate(CREATE_TABLE_NOTES);
            statement.executeUpdate(CREATE_INDEX_NOTES_TS);
            statement.executeUpdate(CREATE_INDEX_NOTES_X_BY_Y);
            statement.executeUpdate(CREATE_INDEX_NOTES_ISDIRTY);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            statement.close();
        }
    }


    /**
     * Add a new note to the database.
     *
     * @param id        the id
     * @param lon       lon
     * @param lat       lat
     * @param altim     elevation
     * @param timestamp the UTC timestamp in millis.
     * @param text      a text
     * @param form      the optional json form.
     *
     * @throws IOException if something goes wrong.
     */
    public static void addNote(Connection connection, long id, double lon, double lat, double altim, long timestamp, String text,
                               String form) throws Exception {
        PreparedStatement writeStatement = null;
        try {
            String insertSQL = "INSERT INTO " + TableDescriptions.TABLE_NOTES
                    + "(" + //
                    TableDescriptions.NotesTableFields.COLUMN_ID.getFieldName() + ", " + //
                    TableDescriptions.NotesTableFields.COLUMN_LAT.getFieldName() + ", " + //
                    TableDescriptions.NotesTableFields.COLUMN_LON.getFieldName() + ", " + //
                    TableDescriptions.NotesTableFields.COLUMN_ALTIM.getFieldName() + ", " + //
                    TableDescriptions.NotesTableFields.COLUMN_TS.getFieldName() + ", " + //
                    TableDescriptions.NotesTableFields.COLUMN_TEXT.getFieldName() + ", " + //
                    TableDescriptions.NotesTableFields.COLUMN_FORM.getFieldName() + ", " + //
                    TableDescriptions.NotesTableFields.COLUMN_ISDIRTY.getFieldName() + //
                    ") VALUES"
                    + "(?,?,?,?,?,?,?,?)";
            writeStatement = connection.prepareStatement(insertSQL);
            writeStatement.setLong(1, id);
            writeStatement.setDouble(2, lat);
            writeStatement.setDouble(3, lon);
            writeStatement.setDouble(4, altim);
            writeStatement.setLong(5, timestamp);
            writeStatement.setString(6, text);
            writeStatement.setString(7, form);
            writeStatement.setInt(8, 1);

            writeStatement.executeUpdate();
        } finally {
            if (writeStatement != null) {
                writeStatement.close();
            }
        }
    }


}
