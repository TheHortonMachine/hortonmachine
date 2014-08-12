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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
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
     * @param lon         lon
     * @param lat         lat
     * @param altim       elevation
     * @param timestamp   the UTC timestamp in millis.
     * @param text        a text
     * @param description an optional description for the note.
     * @param form        the optional json form.
     * @param style       the optional style definition.
     *
     * @return the inserted note id.
     *
     * @throws IOException if something goes wrong.
     */
    public static void addNote(Connection connection, double lon, double lat, double altim, long timestamp, String text, String description,
                               String form, String style) throws Exception {
        Statement statement = connection.createStatement();
        try {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            StringBuilder sb = new StringBuilder();
            sb.append("INSERT INTO ").append(TABLE_NOTES).append("(");
            sb.append(NotesTableFields.COLUMN_LON.getFieldName()).append(",");
            sb.append(NotesTableFields.COLUMN_LAT.getFieldName()).append(",");
            sb.append(NotesTableFields.COLUMN_ALTIM.getFieldName()).append(",");
            sb.append(NotesTableFields.COLUMN_TS.getFieldName()).append(",");
            if (description != null)
                sb.append(NotesTableFields.COLUMN_DESCRIPTION.getFieldName()).append(",");
            sb.append(NotesTableFields.COLUMN_TEXT.getFieldName()).append(",");
            if (form != null)
                sb.append(NotesTableFields.COLUMN_FORM.getFieldName()).append(",");
            if (style != null)
                sb.append(NotesTableFields.COLUMN_STYLE.getFieldName()).append(",");
            sb.append(NotesTableFields.COLUMN_ISDIRTY.getFieldName());
            sb.append(") VALUES (");
            sb.append(lon).append(",");
            sb.append(lat).append(",");
            sb.append(altim).append(",");
            sb.append(timestamp).append(",");
            if (description != null)
                sb.append("'").append(description).append("'").append(",");
            sb.append("'").append(text).append("'").append(",");
            if (form != null)
                sb.append("'").append(form).append("'").append(",");
            if (style != null)
                sb.append("'").append(style).append("'").append(",");
            sb.append(1);
            sb.append(")");

            statement.executeUpdate(sb.toString());
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            statement.close();
        }
    }


}
