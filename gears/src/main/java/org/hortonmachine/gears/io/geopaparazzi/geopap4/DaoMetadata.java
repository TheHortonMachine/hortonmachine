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


import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoMetadata {

    public static final String EMPTY_VALUE = " - ";

    /**
     * Create the notes tables.
     *
     * @throws java.io.IOException if something goes wrong.
     */
    public static void createTables(Connection connection) throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_METADATA);
        sB.append(" (");
        sB.append(MetadataTableFields.COLUMN_KEY.getFieldName()).append(" TEXT NOT NULL, ");
        sB.append(MetadataTableFields.COLUMN_VALUE.getFieldName()).append(" TEXT NOT NULL ");
        sB.append(");");
        String CREATE_TABLE_PROJECT = sB.toString();

        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE_PROJECT);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }


    /**
     * Populate the project metadata table.
     *
     * @param name         the project name
     * @param description  an optional description.
     * @param notes        optional notes.
     * @param creationUser the user creating the project.
     *
     * @throws java.io.IOException if something goes wrong.
     */
    public static void fillProjectMetadata(Connection connection, String name, String description, String notes, String creationUser) throws Exception {
        Date creationDate = new Date();
        if (name == null) {
            name = "project-" + ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(creationDate);
        }
        if (description == null) {
            description = EMPTY_VALUE;
        }
        if (notes == null) {
            notes = EMPTY_VALUE;
        }
        if (creationUser == null) {
            creationUser = "dummy user";
        }

        insertPair(connection, MetadataTableFields.KEY_NAME.getFieldName(), name);
        insertPair(connection, MetadataTableFields.KEY_DESCRIPTION.getFieldName(), description);
        insertPair(connection, MetadataTableFields.KEY_NOTES.getFieldName(), notes);
        insertPair(connection, MetadataTableFields.KEY_CREATIONTS.getFieldName(), String.valueOf(creationDate.getTime()));
        insertPair(connection, MetadataTableFields.KEY_LASTTS.getFieldName(), EMPTY_VALUE);
        insertPair(connection, MetadataTableFields.KEY_CREATIONUSER.getFieldName(), creationUser);
        insertPair(connection, MetadataTableFields.KEY_LASTUSER.getFieldName(), EMPTY_VALUE);

    }

    private static void insertPair(Connection connection, String key, String value) throws SQLException {
        String insertSQL = "INSERT INTO " + TableDescriptions.TABLE_METADATA
                + "(" + //
                MetadataTableFields.COLUMN_KEY.getFieldName() + ", " + //
                MetadataTableFields.COLUMN_VALUE.getFieldName() + //
                ") VALUES"
                + "(?,?)";
        try (PreparedStatement writeImageDataStatement = connection.prepareStatement(insertSQL)) {
            writeImageDataStatement.setString(1, key);
            writeImageDataStatement.setString(2, value);

            writeImageDataStatement.executeUpdate();
        }
    }


}
