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

import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_GPSLOGS;
import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_GPSLOG_DATA;
import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_GPSLOG_PROPERTIES;
import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_IMAGES;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.gears.io.geopaparazzi.OmsGeopaparazziProject3To4Converter;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.GpsLogsDataTableFields;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.GpsLogsPropertiesTableFields;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.GpsLogsTableFields;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.ImageTableFields;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoGpsLog {

    // private static SimpleDateFormat dateFormatter =
    // TimeUtilities.INSTANCE.TIME_FORMATTER_SQLITE_UTC;
    // private static SimpleDateFormat dateFormatterForLabelInLocalTime =
    // TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL;

    /**
     * Create log tables.
     *
     * @throws IOException if something goes wrong.
     */
    public static void createTables( Connection connection ) throws IOException, SQLException {

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

        try (Statement statement = connection.createStatement()) {
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
        }

    }

    public static void addGpsLog( Connection connection, OmsGeopaparazziProject3To4Converter.GpsLog log, float width,
            String color, boolean visible ) throws Exception {
        Date startTS = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.parse(log.startTime);
        Date endTS = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.parse(log.endTime);

        String insertSQL1 = "INSERT INTO " + TableDescriptions.TABLE_GPSLOGS + "(" + //
                TableDescriptions.GpsLogsTableFields.COLUMN_ID.getFieldName() + ", " + //
                TableDescriptions.GpsLogsTableFields.COLUMN_LOG_STARTTS.getFieldName() + ", " + //
                TableDescriptions.GpsLogsTableFields.COLUMN_LOG_ENDTS.getFieldName() + ", " + //
                TableDescriptions.GpsLogsTableFields.COLUMN_LOG_LENGTHM.getFieldName() + ", " + //
                TableDescriptions.GpsLogsTableFields.COLUMN_LOG_TEXT.getFieldName() + ", " + //
                TableDescriptions.GpsLogsTableFields.COLUMN_LOG_ISDIRTY.getFieldName() + //
                ") VALUES" + "(?,?,?,?,?,?)";
        try (PreparedStatement writeStatement = connection.prepareStatement(insertSQL1)) {
            writeStatement.setLong(1, log.id);
            writeStatement.setLong(2, startTS.getTime());
            writeStatement.setLong(3, endTS.getTime());
            writeStatement.setDouble(4, 0.0);
            writeStatement.setString(5, log.text);
            writeStatement.setInt(6, 1);

            writeStatement.executeUpdate();
        }

        String insertSQL2 = "INSERT INTO " + TableDescriptions.TABLE_GPSLOG_PROPERTIES + "(" + //
                GpsLogsPropertiesTableFields.COLUMN_ID.getFieldName() + ", " + //
                GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName() + ", " + //
                GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName() + ", " + //
                GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName() + ", " + //
                GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName() + //
                ") VALUES" + "(?,?,?,?,?)";
        try (PreparedStatement writeStatement = connection.prepareStatement(insertSQL2)) {
            writeStatement.setLong(1, log.id);
            writeStatement.setLong(2, log.id);
            writeStatement.setString(3, color);
            writeStatement.setFloat(4, width);
            writeStatement.setInt(5, visible ? 1 : 0);

            writeStatement.executeUpdate();
        }

        for( OmsGeopaparazziProject3To4Converter.GpsPoint point : log.points ) {
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
    public static void addGpsLogDataPoint( Connection connection, OmsGeopaparazziProject3To4Converter.GpsPoint point,
            long gpslogId ) throws Exception {
        Date timestamp = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.parse(point.utctime);

        String insertSQL = "INSERT INTO " + TableDescriptions.TABLE_GPSLOG_DATA + "(" + //
                TableDescriptions.GpsLogsDataTableFields.COLUMN_ID.getFieldName() + ", " + //
                TableDescriptions.GpsLogsDataTableFields.COLUMN_LOGID.getFieldName() + ", " + //
                TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName() + ", " + //
                TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName() + ", " + //
                TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_ALTIM.getFieldName() + ", " + //
                TableDescriptions.GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName() + //
                ") VALUES" + "(?,?,?,?,?,?)";
        try (PreparedStatement writeStatement = connection.prepareStatement(insertSQL)) {
            writeStatement.setLong(1, point.id);
            writeStatement.setLong(2, gpslogId);
            writeStatement.setDouble(3, point.lon);
            writeStatement.setDouble(4, point.lat);
            writeStatement.setDouble(5, point.altim);
            writeStatement.setLong(6, timestamp.getTime());

            writeStatement.executeUpdate();
        }
    }

    /**
     * Get the list of available logs.
     * 
     * @param connection the connection to use.
     * @return the list of logs.
     * @throws SQLException
     */
    public static List<GpsLog> getLogsList( IHMConnection connection ) throws Exception {
        List<GpsLog> logsList = new ArrayList<>();
        String sql = "select " + //
                GpsLogsTableFields.COLUMN_ID.getFieldName() + "," + //
                GpsLogsTableFields.COLUMN_LOG_STARTTS.getFieldName() + "," + //
                GpsLogsTableFields.COLUMN_LOG_ENDTS.getFieldName() + "," + //
                GpsLogsTableFields.COLUMN_LOG_TEXT.getFieldName() + //
                " from " + TABLE_GPSLOGS; //
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(sql);) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.


            // first get the logs
            while( rs.next() ) {
                long id = rs.getLong(1);

                long startDateTimeString = rs.getLong(2);
                long endDateTimeString = rs.getLong(3);
                String text = rs.getString(4);

                GpsLog log = new GpsLog();
                log.id = id;
                log.startTime = startDateTimeString;
                log.endTime = endDateTimeString;
                log.text = text;
                logsList.add(log);
            }
        }
        return logsList;
    }

    /**
     * Gather gps points data for a supplied log.
     * 
     * @param connection the connection to use.
     * @param log the log.
     * @throws Exception 
     */
    public static void collectDataForLog( IHMConnection connection, GpsLog log ) throws Exception {
        long logId = log.id;

        String query = "select "
                + //
                GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName() + ","
                + //
                GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName() + ","
                + //
                GpsLogsDataTableFields.COLUMN_DATA_ALTIM.getFieldName() + ","
                + //
                GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName()
                + //
                " from " + TABLE_GPSLOG_DATA + " where "
                + //
                GpsLogsDataTableFields.COLUMN_LOGID.getFieldName() + " = " + logId + " order by "
                + GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName();

        try (IHMStatement newStatement = connection.createStatement(); IHMResultSet result = newStatement.executeQuery(query);) {
            newStatement.setQueryTimeout(30);

            while( result.next() ) {
                double lat = result.getDouble(1);
                double lon = result.getDouble(2);
                double altim = result.getDouble(3);
                long ts = result.getLong(4);

                GpsPoint gPoint = new GpsPoint();
                gPoint.lon = lon;
                gPoint.lat = lat;
                gPoint.altim = altim;
                gPoint.utctime = ts;
                log.points.add(gPoint);
            }
        }
    }

    /**
     * A class representing a gps point.
     */
    public static class GpsPoint {
        public double lat;
        public double lon;
        public double altim;
        public long utctime;
    }

    /**
     * A gps log with an empty holder for gps data.
     */
    public static class GpsLog {
        public long id;
        public long startTime;
        public long endTime;
        public String text;
        public List<GpsPoint> points = new ArrayList<>();
        
        @Override
        public String toString() {
            return text;
        }
    }
    
    
    /**
     * Get the current data envelope.
     * 
     * @param connection the db connection.
     * @return the envelope.
     * @throws Exception
     */
    public static ReferencedEnvelope getEnvelope( IHMConnection connection ) throws Exception {
        String query = "SELECT min(" + //
                GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName() + "), max(" + //
                GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName() + "), min(" + //
                GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName() + "), max(" + //
                GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName() + ") " + //
                " FROM " + TABLE_GPSLOG_DATA;
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(query);) {
            if (rs.next()) {
                double minX = rs.getDouble(1);
                double maxX = rs.getDouble(2);
                double minY = rs.getDouble(3);
                double maxY = rs.getDouble(4);

                ReferencedEnvelope env = new ReferencedEnvelope(minX, maxX, minY, maxY, DefaultGeographicCRS.WGS84);
                return env;
            }
        }

        return null;
    }
}
