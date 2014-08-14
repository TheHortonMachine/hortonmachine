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


import org.jgrasstools.gears.io.geopaparazzi.OmsGeopaparazziProject3To4Converter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.*;
import static java.lang.Math.abs;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoGpsLog {

    private static SimpleDateFormat dateFormatter = TimeUtilities.INSTANCE.TIME_FORMATTER_SQLITE_UTC;
    private static SimpleDateFormat dateFormatterForLabelInLocalTime = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL;

    /**
     * Create log tables.
     *
     * @throws IOException if something goes wrong.
     */
    public static void createTables(Connection connection) throws IOException, SQLException {

        /*
         * gps log table
         */
        StringBuilder sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_GPSLOGS);
        sB.append(" (");
        sB.append(GpsLogsTableFields.COLUMN_ID.getFieldName() + " INTEGER PRIMARY KEY, ");
        sB.append(GpsLogsTableFields.COLUMN_LOG_STARTTS.getFieldName()).append(" LONG NOT NULL,");
        sB.append(GpsLogsTableFields.COLUMN_LOG_ENDTS.getFieldName()).append(" LONG NOT NULL,");
        sB.append(GpsLogsTableFields.COLUMN_LOG_LENGTHM.getFieldName()).append(" REAL NOT NULL, ");
        sB.append(GpsLogsTableFields.COLUMN_LOG_ISDIRTY.getFieldName()).append(" INTEGER NOT NULL, ");
        sB.append(GpsLogsTableFields.COLUMN_LOG_TEXT.getFieldName()).append(" TEXT NOT NULL ");
        sB.append(");");
        String CREATE_TABLE_GPSLOGS = sB.toString();

        /*
         * gps log data table
         */
        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_GPSLOG_DATA);
        sB.append(" (");
        sB.append(GpsLogsDataTableFields.COLUMN_ID.getFieldName() + " INTEGER PRIMARY KEY, ");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName()).append(" REAL NOT NULL, ");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName()).append(" REAL NOT NULL,");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_ALTIM.getFieldName()).append(" REAL NOT NULL,");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName()).append(" DATE NOT NULL,");
        sB.append(GpsLogsDataTableFields.COLUMN_LOGID.getFieldName()).append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT ");
        sB.append(GpsLogsDataTableFields.COLUMN_LOGID.getFieldName());
        sB.append(" REFERENCES ");
        sB.append(TABLE_GPSLOGS);
        sB.append("(" + GpsLogsTableFields.COLUMN_ID.getFieldName() + ") ON DELETE CASCADE");
        sB.append(");");
        String CREATE_TABLE_GPSLOG_DATA = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX gpslog_id_idx ON ");
        sB.append(TABLE_GPSLOG_DATA);
        sB.append(" ( ");
        sB.append(GpsLogsDataTableFields.COLUMN_LOGID.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_GPSLOG_ID = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX gpslog_ts_idx ON ");
        sB.append(TABLE_GPSLOG_DATA);
        sB.append(" ( ");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_GPSLOG_TS = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX gpslog_x_by_y_idx ON ");
        sB.append(TABLE_GPSLOG_DATA);
        sB.append(" ( ");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName());
        sB.append(", ");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_GPSLOG_X_BY_Y = sB.toString();

        sB = new StringBuilder();
        sB.append("CREATE INDEX gpslog_logid_x_y_idx ON ");
        sB.append(TABLE_GPSLOG_DATA);
        sB.append(" ( ");
        sB.append(GpsLogsDataTableFields.COLUMN_LOGID.getFieldName());
        sB.append(", ");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName());
        sB.append(", ");
        sB.append(GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName());
        sB.append(" );");
        String CREATE_INDEX_GPSLOG_LOGID_X_Y = sB.toString();

        /*
         * properties table
         */
        sB = new StringBuilder();
        sB.append("CREATE TABLE ");
        sB.append(TABLE_GPSLOG_PROPERTIES);
        sB.append(" (");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_ID.getFieldName());
        sB.append(" INTEGER PRIMARY KEY, ");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName());
        sB.append(" INTEGER NOT NULL ");
        sB.append("CONSTRAINT " + GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName() + " REFERENCES ");
        sB.append(TABLE_GPSLOGS);
        sB.append("(");
        sB.append(GpsLogsTableFields.COLUMN_ID);
        sB.append(") ON DELETE CASCADE,");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName()).append(" TEXT NOT NULL, ");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName()).append(" REAL NOT NULL, ");
        sB.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName()).append(" INTEGER NOT NULL");
        sB.append(");");
        String CREATE_TABLE_GPSLOGS_PROPERTIES = sB.toString();

        Statement statement = connection.createStatement();
        try {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            statement.executeUpdate(CREATE_TABLE_GPSLOGS);

            statement.executeUpdate(CREATE_TABLE_GPSLOG_DATA);
            statement.executeUpdate(CREATE_INDEX_GPSLOG_ID);
            statement.executeUpdate(CREATE_INDEX_GPSLOG_TS);
            statement.executeUpdate(CREATE_INDEX_GPSLOG_X_BY_Y);
            statement.executeUpdate(CREATE_INDEX_GPSLOG_LOGID_X_Y);

            statement.executeUpdate(CREATE_TABLE_GPSLOGS_PROPERTIES);
        } catch (Exception e) {
            throw new IOException(e.getLocalizedMessage());
        } finally {
            statement.close();
        }

    }


    public static void addGpsLog(Connection connection, OmsGeopaparazziProject3To4Converter.GpsLog log, float width, String color, boolean visible)
            throws Exception {
        Date startTS = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.parse(log.startTime);
        Date endTS = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.parse(log.endTime);

        PreparedStatement writeStatement = null;
        try {
            String insertSQL = "INSERT INTO " + TableDescriptions.TABLE_GPSLOGS
                    + "(" + //
                    TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsTableFields.COLUMN_LOG_STARTTS.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsTableFields.COLUMN_LOG_ENDTS.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsTableFields.COLUMN_LOG_LENGTHM.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsTableFields.COLUMN_LOG_TEXT.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsTableFields.COLUMN_LOG_ISDIRTY.getFieldName() + //
                    ") VALUES"
                    + "(?,?,?,?,?,?)";

            writeStatement = connection.prepareStatement(insertSQL);
            writeStatement.setLong(1, log.id);
            writeStatement.setLong(2, startTS.getTime());
            writeStatement.setLong(3, endTS.getTime());
            writeStatement.setDouble(4, 0.0);
            writeStatement.setString(5, log.text);
            writeStatement.setInt(6, 1);

            writeStatement.executeUpdate();

        } finally {
            if (writeStatement != null) {
                writeStatement.close();
            }
        }

        try {
            String insertSQL = "INSERT INTO " + TableDescriptions.TABLE_GPSLOG_PROPERTIES
                    + "(" + //
                    GpsLogsPropertiesTableFields.COLUMN_ID.getFieldName() + ", " + //
                    GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName() + ", " + //
                    GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName() + ", " + //
                    GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName() + ", " + //
                    GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName() + //
                    ") VALUES"
                    + "(?,?,?,?,?)";

            writeStatement = connection.prepareStatement(insertSQL);
            writeStatement.setLong(1, log.id);
            writeStatement.setLong(2, log.id);
            writeStatement.setString(3, color);
            writeStatement.setFloat(4, width);
            writeStatement.setInt(5, visible ? 1 : 0);

            writeStatement.executeUpdate();

        } finally {
            if (writeStatement != null) {
                writeStatement.close();
            }
        }

        for (OmsGeopaparazziProject3To4Converter.GpsPoint point : log.points) {
            addGpsLogDataPoint(connection, point, log.id);
        }


    }

    /**
     * Adds a new XY entry to the gps table.
     *
     * @param connection the db connection.
     * @param point      the point to add.
     * @param gpslogId   the id of the log the point is part of.
     *
     * @throws IOException if something goes wrong
     */
    public static void addGpsLogDataPoint(Connection connection, OmsGeopaparazziProject3To4Converter.GpsPoint point, long gpslogId) throws Exception {
        Date timestamp = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.parse(point.utctime);

        PreparedStatement writeStatement = null;
        try {
            String insertSQL = "INSERT INTO " + TableDescriptions.TABLE_GPSLOG_DATA
                    + "(" + //
                    TableDescriptions.GpsLogsDataTableFields.COLUMN_ID.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsDataTableFields.COLUMN_LOGID.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_ALTIM.getFieldName() + ", " + //
                    TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName() + //
                    ") VALUES"
                    + "(?,?,?,?,?,?)";

            writeStatement = connection.prepareStatement(insertSQL);
            writeStatement.setLong(1, point.id);
            writeStatement.setLong(2, gpslogId);
            writeStatement.setDouble(3, point.lon);
            writeStatement.setDouble(4, point.lat);
            writeStatement.setDouble(5, point.altim);
            writeStatement.setLong(6, timestamp.getTime());

            writeStatement.executeUpdate();

        } finally {
            if (writeStatement != null) {
                writeStatement.close();
            }
        }
    }
}
