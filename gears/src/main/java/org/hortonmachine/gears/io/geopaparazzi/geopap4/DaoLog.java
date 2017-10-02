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
import java.sql.Statement;

/**
 * The class that handles logging to the database.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoLog {

    /**
     *
     */
    public static final String TABLE_LOG = "log";
    /**
     *
     */
    public static final String COLUMN_ID = "_id";
    /**
     *
     */
    public static final String COLUMN_DATAORA = "dataora";
    /**
     *
     */
    public static final String COLUMN_LOGMSG = "logmsg";


    /**
     * Create the default log table.
     *
     * @param connection the db connection to use.
     *
     * @throws Exception if something goes wrong.
     */
    public static void createTables(Connection connection) throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_LOG);
        sB.append(" (");
        sB.append(COLUMN_ID);
        sB.append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
        sB.append(COLUMN_DATAORA).append(" INTEGER NOT NULL, ");
        sB.append(COLUMN_LOGMSG).append(" TEXT ");
        sB.append(");");
        String CREATE_TABLE = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX " + TABLE_LOG + "_" + COLUMN_ID + " ON ");
        sB.append(TABLE_LOG);
        sB.append(" ( ");
        sB.append(COLUMN_ID);
        sB.append(" );");
        String CREATE_INDEX = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX " + TABLE_LOG + "_" + COLUMN_DATAORA + " ON ");
        sB.append(TABLE_LOG);
        sB.append(" ( ");
        sB.append(COLUMN_DATAORA);
        sB.append(" );");
        String CREATE_INDEX_DATE = sB.toString();


        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE);
            statement.executeUpdate(CREATE_INDEX);
            statement.executeUpdate(CREATE_INDEX_DATE);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        }
    }

}
