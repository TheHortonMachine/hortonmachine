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

import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_NOTES;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.NotesTableFields;

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
    public static void createTables( Connection connection ) throws IOException, SQLException {
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

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE_NOTES);
            statement.executeUpdate(CREATE_INDEX_NOTES_TS);
            statement.executeUpdate(CREATE_INDEX_NOTES_X_BY_Y);
            statement.executeUpdate(CREATE_INDEX_NOTES_ISDIRTY);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
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
    public static void addNote( Connection connection, long id, double lon, double lat, double altim, long timestamp, String text,
            String form ) throws Exception {
        String insertSQL = "INSERT INTO " + TableDescriptions.TABLE_NOTES + "(" + //
                TableDescriptions.NotesTableFields.COLUMN_ID.getFieldName() + ", " + //
                TableDescriptions.NotesTableFields.COLUMN_LAT.getFieldName() + ", " + //
                TableDescriptions.NotesTableFields.COLUMN_LON.getFieldName() + ", " + //
                TableDescriptions.NotesTableFields.COLUMN_ALTIM.getFieldName() + ", " + //
                TableDescriptions.NotesTableFields.COLUMN_TS.getFieldName() + ", " + //
                TableDescriptions.NotesTableFields.COLUMN_TEXT.getFieldName() + ", " + //
                TableDescriptions.NotesTableFields.COLUMN_FORM.getFieldName() + ", " + //
                TableDescriptions.NotesTableFields.COLUMN_ISDIRTY.getFieldName() + //
                ") VALUES" + "(?,?,?,?,?,?,?,?)";
        try (PreparedStatement writeStatement = connection.prepareStatement(insertSQL)) {
            writeStatement.setLong(1, id);
            writeStatement.setDouble(2, lat);
            writeStatement.setDouble(3, lon);
            writeStatement.setDouble(4, altim);
            writeStatement.setLong(5, timestamp);
            writeStatement.setString(6, text);
            writeStatement.setString(7, form);
            writeStatement.setInt(8, 1);

            writeStatement.executeUpdate();
        }
    }

    /**
     * Get the collected notes from the database inside a given bound.
     *
     * @param connection the db to take from .
     * @param nswe      optional bounds as [n, s, w, e].
     * @return the list of notes inside the bounds.
     * @throws Exception 
     */
    public static List<Note> getNotesList( IHMConnection connection, float[] nswe ) throws Exception {

        String query = "SELECT " + //
                NotesTableFields.COLUMN_ID.getFieldName() + ", " + //
                NotesTableFields.COLUMN_LON.getFieldName() + ", " + //
                NotesTableFields.COLUMN_LAT.getFieldName() + ", " + //
                NotesTableFields.COLUMN_ALTIM.getFieldName() + ", " + //
                NotesTableFields.COLUMN_TEXT.getFieldName() + ", " + //
                NotesTableFields.COLUMN_TS.getFieldName() + ", " + //
                NotesTableFields.COLUMN_DESCRIPTION.getFieldName() + //
                " FROM " + TABLE_NOTES;
        if (nswe != null) {
            query = query + " WHERE (lon BETWEEN XXX AND XXX) AND (lat BETWEEN XXX AND XXX)";
            query = query.replaceFirst("XXX", String.valueOf(nswe[2]));
            query = query.replaceFirst("XXX", String.valueOf(nswe[3]));
            query = query.replaceFirst("XXX", String.valueOf(nswe[1]));
            query = query.replaceFirst("XXX", String.valueOf(nswe[0]));
        }

        List<Note> notes = new ArrayList<>();
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(query);) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            while( rs.next() ) {
                Note note = new Note();
                note.id = rs.getLong(1);
                note.lon = rs.getDouble(2);
                note.lat = rs.getDouble(3);
                note.altim = rs.getDouble(4);
                note.simpleText = rs.getString(5);
                note.timeStamp = rs.getLong(6);
                note.description = rs.getString(7);
                notes.add(note);
            }
        }
        return notes;
    }

    /**
     * Get the current data envelope.
     * 
     * @param connection the db connection.
     * @param noteTypeName if <code>null</code>, simple notes are handled. Else this 
     *          is taken as name for the note type.
     * @return the envelope.
     * @throws Exception
     */
    public static ReferencedEnvelope getEnvelope( IHMConnection connection, String noteTypeName ) throws Exception {
        String query = "SELECT min(" + //
                NotesTableFields.COLUMN_LON.getFieldName() + "), max(" + //
                NotesTableFields.COLUMN_LON.getFieldName() + "), min(" + //
                NotesTableFields.COLUMN_LAT.getFieldName() + "), max(" + //
                NotesTableFields.COLUMN_LAT.getFieldName() + ") " + //
                " FROM " + TABLE_NOTES;

        if (noteTypeName != null) {
            query += " where " + NotesTableFields.COLUMN_TEXT.getFieldName() + "='" + noteTypeName + "'";
        } else {
            String formFN = NotesTableFields.COLUMN_FORM.getFieldName();
            query += " where " + formFN + " is null OR " + formFN + "=''";
        }

        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(query);) {
            if (rs.next()) {
                double minX = rs.getDouble(1);
                double maxX = rs.getDouble(2);
                double minY = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                ReferencedEnvelope env = new ReferencedEnvelope(minX, maxX, minY, maxY, DefaultGeographicCRS.WGS84);
                return env;
            }
        } catch (Exception e) {
            // print trace but return null
            e.printStackTrace();
        }

        return null;
    }

}
