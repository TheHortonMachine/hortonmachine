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
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.*;
import static java.lang.Math.abs;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class DaoGpsLog  {

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

//
//    public long addGpsLog(long startTs, long endTs, double lengthm, String text, float width, String color, boolean visible)
//            throws IOException {
//        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
//        sqliteDatabase.beginTransaction();
//        long rowId;
//        try {
//            // add new log
//            ContentValues values = new ContentValues();
//            values.put(GpsLogsTableFields.COLUMN_LOG_STARTTS.getFieldName(), startTs);
//            values.put(GpsLogsTableFields.COLUMN_LOG_ENDTS.getFieldName(), endTs);
//            if (text == null) {
//                text = "log_" + dateFormatterForLabelInLocalTime.format(new java.util.Date(startTs));
//            }
//            values.put(GpsLogsTableFields.COLUMN_LOG_LENGTHM.getFieldName(), lengthm);
//            values.put(GpsLogsTableFields.COLUMN_LOG_TEXT.getFieldName(), text);
//            values.put(GpsLogsTableFields.COLUMN_LOG_ISDIRTY.getFieldName(), 1);
//            rowId = sqliteDatabase.insertOrThrow(TABLE_GPSLOGS, null, values);
//
//            // and some default properties
//            ContentValues propValues = new ContentValues();
//            propValues.put(GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName(), rowId);
//            propValues.put(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName(), color);
//            propValues.put(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName(), width);
//            propValues.put(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName(), visible ? 1 : 0);
//            sqliteDatabase.insertOrThrow(TABLE_GPSLOG_PROPERTIES, null, propValues);
//
//            sqliteDatabase.setTransactionSuccessful();
//        } catch (Exception e) {
//            GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
//            throw new IOException(e.getLocalizedMessage());
//        } finally {
//            sqliteDatabase.endTransaction();
//        }
//        return rowId;
//    }
//
//    /**
//     * Adds a new XY entry to the gps table.
//     *
//     * @param gpslogId the ID from the GPS log table.
//     * @param lon      longitude.
//     * @param lat      latitude
//     * @param altim    altitude/elevation
//     * @throws IOException if something goes wrong
//     */
//    public void addGpsLogDataPoint(SQLiteDatabase sqliteDatabase, long gpslogId, double lon, double lat, double altim,
//                                   long timestamp) throws IOException {
//        ContentValues values = new ContentValues();
//        values.put(GpsLogsDataTableFields.COLUMN_LOGID.getFieldName(), (int) gpslogId);
//        values.put(GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName(), lon);
//        values.put(GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName(), lat);
//        values.put(GpsLogsDataTableFields.COLUMN_DATA_ALTIM.getFieldName(), altim);
//        values.put(GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName(), timestamp);
//        sqliteDatabase.insertOrThrow(TABLE_GPSLOG_DATA, null, values);
//    }
//
//
//
//    /**
//     * Update the properties of a log.
//     *
//     * @param logid   the id of the log.
//     * @param color   color
//     * @param width   width
//     * @param visible whether it is visible.
//     * @param name    the name.
//     * @throws IOException if something goes wrong.
//     */
//    public static void updateLogProperties(long logid, String color, float width, boolean visible, String name)
//            throws IOException {
//        SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
//        sqliteDatabase.beginTransaction();
//        try {
//
//            StringBuilder sb = new StringBuilder();
//            sb.append("UPDATE ");
//            sb.append(TABLE_GPSLOG_PROPERTIES);
//            sb.append(" SET ");
//            sb.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName()).append("='").append(color).append("', ");
//            sb.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName()).append("=").append(width).append(", ");
//            sb.append(GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_VISIBLE.getFieldName()).append("=").append(visible ? 1 : 0).append(" ");
//            sb.append("WHERE ").append(GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName()).append("=").append(logid);
//
//            String query = sb.toString();
//            if (GPLog.LOG_HEAVY)
//                GPLog.addLogEntry("DAOGPSLOG", query);
//            // sqliteDatabase.execSQL(query);
//            SQLiteStatement sqlUpdate = sqliteDatabase.compileStatement(query);
//            sqlUpdate.execute();
//            sqlUpdate.close();
//
//            if (name != null && name.length() > 0) {
//                sb = new StringBuilder();
//                sb.append("UPDATE ");
//                sb.append(TABLE_GPSLOGS);
//                sb.append(" SET ");
//                sb.append(GpsLogsTableFields.COLUMN_LOG_TEXT.getFieldName()).append("='").append(name).append("' ");
//                sb.append("WHERE ").append(GpsLogsTableFields.COLUMN_ID.getFieldName()).append("=").append(logid);
//
//                query = sb.toString();
//                if (GPLog.LOG_HEAVY)
//                    GPLog.addLogEntry("DAOGPSLOG", query);
//                sqlUpdate = sqliteDatabase.compileStatement(query);
//                sqlUpdate.execute();
//                sqlUpdate.close();
//            }
//
//            sqliteDatabase.setTransactionSuccessful();
//        } catch (Exception e) {
//            GPLog.error("DAOGPSLOG", e.getLocalizedMessage(), e);
//            throw new IOException(e.getLocalizedMessage());
//        } finally {
//            sqliteDatabase.endTransaction();
//        }
//    }
}
