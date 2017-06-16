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
package org.jgrasstools.dbs.spatialite.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.spatialite.ESqliteDataType;
import org.jgrasstools.dbs.spatialite.QueryResult;
import org.jgrasstools.dbs.spatialite.SpatialiteGeometryColumns;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBReader;

import jsqlite.Database;

/**
 * A spatialite database for android.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GPSpatialiteDb extends ASpatialDb {
    public boolean open( String dbPath ) throws Exception {
        this.mDbPath = dbPath;

        boolean dbExists = false;
        File dbFile = new File(dbPath);
        if (dbFile.exists()) {
            if (mPrintInfos)
                logInfo("Database exists");
            dbExists = true;
        }

        Database database = new Database();
        database.open(dbPath, jsqlite.Constants.SQLITE_OPEN_READWRITE | jsqlite.Constants.SQLITE_OPEN_CREATE);

        mConn = new GPConnection(database);
        if (mPrintInfos)
            try (IJGTStatement stmt = mConn.createStatement()) {
                stmt.execute("SELECT sqlite_version()");
                IJGTResultSet rs = stmt.executeQuery("SELECT sqlite_version() AS 'SQLite Version';");
                while( rs.next() ) {
                    String sqliteVersion = rs.getString(1);
                    logInfo("SQLite Version: " + sqliteVersion);
                }
            }
        return dbExists;
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        if (options == null) {
            options = "";
        }
        String sql = "SELECT InitSpatialMetadata(" + options + ")";
        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID, int limit,
            int reprojectSrid ) throws Exception {
        QueryResult queryResult = new QueryResult();

        SpatialiteGeometryColumns gCol = null;
        try {
            gCol = getGeometryColumnsForTable(tableName);
        } catch (Exception e) {
            // ignore
        }
        boolean hasGeom = gCol != null;

        List<String[]> tableColumnsInfo = getTableColumns(tableName);
        List<String> tableColumns = new ArrayList<>();
        HashMap<String, String> name2TypeMap = new HashMap<>();
        for( String[] info : tableColumnsInfo ) {
            tableColumns.add(info[0]);
            if (info[1].length() > 0)
                name2TypeMap.put(info[0], info[1]);
        }
        if (hasGeom) {
            if (!tableColumns.remove(gCol.f_geometry_column)) {
                String gColLower = gCol.f_geometry_column.toLowerCase();
                int index = -1;
                for( int i = 0; i < tableColumns.size(); i++ ) {
                    String tableColumn = tableColumns.get(i);
                    if (tableColumn.toLowerCase().equals(gColLower)) {
                        index = i;
                        break;
                    }
                }
                if (index != -1) {
                    tableColumns.remove(index);
                }
            }
        }
        if (!alsoPK_UID) {
            if (!tableColumns.remove(PK_UID)) {
                tableColumns.remove(PKUID);
            }
        }

        String sql = "SELECT ";
        if (hasGeom) {
            if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                sql += "ST_AsBinary(" + gCol.f_geometry_column + ") AS " + gCol.f_geometry_column;
            } else {
                sql += "ST_AsBinary(ST_Transform(" + gCol.f_geometry_column + "," + reprojectSrid + ")) AS "
                        + gCol.f_geometry_column;
            }
        }
        for( int i = 0; i < tableColumns.size(); i++ ) {
            if (hasGeom || i != 0)
                sql += ",";
            sql += tableColumns.get(i);
        }
        sql += " FROM " + tableName;
        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE "; //
            sql += getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        WKBReader wkbReader = new WKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                if (hasGeom && columnName.equals(gCol.f_geometry_column)) {
                    queryResult.geometryIndex = i - 1;
                }
                String type = name2TypeMap.get(columnName);
                String columnTypeName = "UNKNOWN";
                if (type != null) {
                    columnTypeName = type;
                }
                queryResult.types.add(columnTypeName);
            }

            String[] typesArray = new String[queryResult.types.size()];
            for( int i = 0; i < queryResult.types.size(); i++ ) {
                typesArray[i] = queryResult.types.get(i).toLowerCase();
            }
            while( rs.next() ) {
                int i = 1;
                Object[] rec = new Object[columnCount];
                if (hasGeom) {
                    byte[] geomBytes = rs.getBytes(i);
                    Geometry geometry = wkbReader.read(geomBytes);
                    rec[i - 1] = geometry;
                    i++;
                }
                for( int j = i; j <= columnCount; j++ ) {
                    Object object;
                    String type = typesArray[j-1];
                    if (type.equals(ESqliteDataType.TEXT.getLowercaseName())) {
                        object = rs.getString(j);
                    } else if (type.equals(ESqliteDataType.INT.getLowercaseName())) {
                        object = rs.getInt(j);
                    } else if (type.equals(ESqliteDataType.REAL.getLowercaseName())) {
                        object = rs.getDouble(j);
                    } else {
                        object = rs.getObject(j);
                    }
                    rec[j - 1] = object;
                }
                queryResult.data.add(rec);
            }
            return queryResult;
        }
    }

    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    protected void logWarn( String message ) {
        // Log.w("SpatialiteDb", message);
    }

    @Override
    protected void logInfo( String message ) {
        // Log.i("SpatialiteDb", message);
    }

    @Override
    protected void logDebug( String message ) {
        // Log.d("SpatialiteDb", message);
    }

}
