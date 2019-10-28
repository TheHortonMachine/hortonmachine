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
package org.hortonmachine.dbs.h2gis;

import java.sql.Clob;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.h2gis.functions.factory.H2GISFunctions;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IDbVisitor;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.Index;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.EGeometryType;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * A spatialite database.
 * 
 * <p>Notes:</p>
 * <p>To create a spatial table you need to do:
 * <pre>
 * {@link H2GisDb#createTable(String, String...)};
 * {@link H2GisDb#addSrid(String, String)});
 * {@link H2GisDb#createSpatialIndex(String, String)};
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class H2GisDb extends ASpatialDb {
    private H2Db h2Db;
    private boolean wasInitialized = false;

    public H2GisDb() {
        h2Db = new H2Db();
    }

    @Override
    public EDb getType() {
        return EDb.H2GIS;
    }

    public void setCredentials( String user, String password ) {
        this.user = user;
        this.password = password;
    }

    @Override
    public ConnectionData getConnectionData() {
        return h2Db.getConnectionData();
    }

    @Override
    public boolean open( String dbPath, String user, String password ) throws Exception {
        setCredentials(user, password);
        return open(dbPath);
    }

    public boolean open( String dbPath ) throws Exception {
        h2Db.setCredentials(user, password);
        h2Db.setMakePooled(makePooled);

        if (dbPath != null && dbPath.endsWith(EDb.H2GIS.getExtension())) {
            dbPath = dbPath.substring(0, dbPath.length() - (EDb.H2GIS.getExtension().length() + 1));
        }
        boolean dbExists = h2Db.open(dbPath);
        if (dbExists) {
            wasInitialized = true;
        }
        h2Db.getConnectionData().dbType = getType().getCode();

        if (!dbExists)
            initSpatialMetadata(null);

        this.mDbPath = h2Db.getDatabasePath();
        if (mPrintInfos) {
            if (!wasInitialized) {
                initSpatialMetadata(null);
            }
            String[] dbInfo = getDbInfo();
            Logger.INSTANCE.insertDebug(null, "H2GIS Version: " + dbInfo[1]);
        }
        return dbExists;
    }

    public void close() throws Exception {
        h2Db.close();
    }

    @Override
    public String getJdbcUrlPre() {
        return h2Db.getJdbcUrlPre();
    }

    public Connection getJdbcConnection() throws Exception {
        Connection jdbcConn = SFSUtilities.wrapConnection(h2Db.getJdbcConnection());
        return jdbcConn;
    }

    public IHMConnection getConnectionInternal() throws Exception {
        return h2Db.getConnectionInternal();
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        if (!wasInitialized) {
            H2GISFunctions.load(getJdbcConnection());
            wasInitialized = true;
        }
    }

    @Override
    public Envelope getTableBounds( String tableName ) throws Exception {
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        if (gCol == null)
            return null;
        String geomFieldName = gCol.geometryColumnName;
        // String geomFieldName;
        // if (gCol != null) {
        // geomFieldName = gCol.f_geometry_column;
        // String trySql = "SELECT extent_min_x, extent_min_y, extent_max_x, extent_max_y FROM
        // vector_layers_statistics WHERE table_name='"
        // + tableName + "' AND geometry_column='" + geomFieldName + "'";
        // try (IHMStatement stmt = mConn.createStatement(); IHMResultSet rs =
        // stmt.executeQuery(trySql)) {
        // if (rs.next()) {
        // double minX = rs.getDouble(1);
        // double minY = rs.getDouble(2);
        // double maxX = rs.getDouble(3);
        // double maxY = rs.getDouble(4);
        //
        // Envelope env = new Envelope(minX, maxX, minY, maxY);
        // if (env.getWidth() != 0.0 && env.getHeight() != 0.0) {
        // return env;
        // }
        // }
        // }
        // } else {
        // // try geometry if virtual table
        // geomFieldName = "geometry";
        // }

        // OR DO FULL GEOMETRIES SCAN

        String sql = "SELECT ST_XMin(ST_collect(" + geomFieldName + ")) , ST_YMin(ST_collect(" + geomFieldName + ")),"
                + "ST_XMax(ST_collect(" + geomFieldName + ")), ST_YMax(ST_collect(" + geomFieldName + ")) " + "FROM " + tableName;

        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    double minX = rs.getDouble(1);
                    double minY = rs.getDouble(2);
                    double maxX = rs.getDouble(3);
                    double maxY = rs.getDouble(4);

                    Envelope env = new Envelope(minX, maxX, minY, maxY);
                    return env;
                }
                return null;
            }
        });
    }

    public String[] getDbInfo() {
        // checking h2 version
        String sql = "SELECT H2VERSION(), H2GISVERSION();";
        try {
            return execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                    String[] info = new String[2];
                    while( rs.next() ) {
                        // read the result set
                        info[0] = rs.getString(1);
                        info[1] = rs.getString(2);
                    }
                    return info;
                }
            });
        } catch (Exception e) {
            return new String[]{"no version info available", "no version info available"};
        }
    }

    public void createSpatialTable( String tableName, int srid, String geometryFieldData, String[] fieldData,
            String[] foreignKeys, boolean avoidIndex ) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(tableName).append("(");
        for( int i = 0; i < fieldData.length; i++ ) {
            if (i != 0)
                sb.append(",");
            sb.append(fieldData[i]);
        }
        sb.append(",").append(geometryFieldData);
        if (foreignKeys != null) {
            for( int i = 0; i < foreignKeys.length; i++ ) {
                sb.append(",");
                sb.append(foreignKeys[i]);
            }
        }
        sb.append(")");

        execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.execute(sb.toString());
            }
            return null;
        });

        addSrid(tableName, String.valueOf(srid), null);

        if (!avoidIndex) {
            String[] split = geometryFieldData.trim().split("\\s+");
            String geomColName = split[0];

            createSpatialIndex(tableName, geomColName);
        }
    }

    @Override
    public List<String> getTables( boolean doOrder ) throws Exception {
        return h2Db.getTables(doOrder);
    }

    @Override
    public boolean hasTable( String tableName ) throws Exception {
        return h2Db.hasTable(tableName);
    }

    public ETableType getTableType( String tableName ) throws Exception {
        return h2Db.getTableType(tableName);
    }

    @Override
    public List<String[]> getTableColumns( String tableName ) throws Exception {
        return h2Db.getTableColumns(tableName);
    }

    @Override
    public List<ForeignKey> getForeignKeys( String tableName ) throws Exception {
        return h2Db.getForeignKeys(tableName);
    }

    @Override
    public List<Index> getIndexes( String tableName ) throws Exception {
        return h2Db.getIndexes(tableName);
    }

    @Override
    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = H2GisTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws Exception {
        return execOnConnection(connection -> {
            QueryResult queryResult = new QueryResult();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {

                int geomIndex = -1;
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                for( int i = 1; i <= columnCount; i++ ) {
                    String columnName = rsmd.getColumnName(i);
                    queryResult.names.add(columnName);
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    queryResult.types.add(columnTypeName);
                    if (columnTypeName.equalsIgnoreCase(H2GISFunctions.GEOMETRY_BASE_TYPE)) {
                        geomIndex = i;
                        queryResult.geometryIndex = i - 1;
                    }
                }

                int count = 0;
                while( rs.next() ) {
                    Object[] rec = new Object[columnCount];
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (j == geomIndex) {
                            Geometry geometry = (Geometry) rs.getObject(j);
                            rec[j - 1] = geometry;
                        } else {
                            Object object = rs.getObject(j);
                            if (object instanceof Clob) {
                                object = rs.getString(j);
                            }
                            rec[j - 1] = object;
                        }
                    }
                    queryResult.data.add(rec);
                    if (limit > 0 && ++count > (limit - 1)) {
                        break;
                    }
                }
                return queryResult;

            }
        });

    }

    @Override
    protected void logWarn( String message ) {
        Logger.INSTANCE.insertWarning(null, message);
    }

    @Override
    protected void logInfo( String message ) {
        Logger.INSTANCE.insertInfo(null, message);
    }

    @Override
    protected void logDebug( String message ) {
        Logger.INSTANCE.insertDebug(null, message);
    }

    public Geometry getGeometryFromResultSet( IHMResultSet resultSet, int position ) throws Exception {
        Object object = resultSet.getObject(position);
        if (object instanceof Geometry) {
            return (Geometry) object;
        }
        return null;
    }

    @Override
    public GeometryColumn getGeometryColumnsForTable( String tableName ) throws Exception {
        String attachedStr = "";
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached
            // database

            // get the database name
            String[] split = tableName.split("\\.");
            attachedStr = split[0] + ".";
            tableName = split[1];
            // logger.debug(MessageFormat.format("Considering attached database:
            // {0}", attachedStr));
        }

        // int srid = SFSUtilities.getSRID(jdbcConn, new TableLocation(tableName));

        String indexSql = "SELECT " + H2GisGeometryColumns.INDEX_TABLENAME_FIELD + " FROM " + H2GisGeometryColumns.INDEX_TABLENAME
                + " where " + H2GisGeometryColumns.INDEX_TYPE_NAME + "='SPATIAL INDEX'";
        List<String> tablesWithIndex = new ArrayList<>();
        execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(indexSql)) {
                while( rs.next() ) {
                    String name = rs.getString(1);
                    tablesWithIndex.add(name);
                }
                return null;
            }
        });

        String sql = "select " + H2GisGeometryColumns.F_TABLE_NAME + ", " //
                + H2GisGeometryColumns.F_GEOMETRY_COLUMN + ", " //
                + H2GisGeometryColumns.GEOMETRY_TYPE + "," //
                + H2GisGeometryColumns.COORD_DIMENSION + ", " //
                + H2GisGeometryColumns.SRID + " from " //
                + attachedStr + H2GisGeometryColumns.TABLENAME + " where Lower(" + H2GisGeometryColumns.F_TABLE_NAME + ")=Lower('"
                + tableName + "')";

        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    H2GisGeometryColumns gc = new H2GisGeometryColumns();
                    String name = rs.getString(1);
                    gc.tableName = name;
                    gc.geometryColumnName = rs.getString(2);
                    gc.geometryType = EGeometryType.fromGeometryTypeCode(rs.getInt(3));
                    gc.coordinatesDimension = rs.getInt(4);
                    gc.srid = rs.getInt(5);

                    if (tablesWithIndex.contains(name)) {
                        gc.isSpatialIndexEnabled = 1;
                    }
                    return gc;
                }
                return null;
            }
        });
    }

    @Override
    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        if (alias == null) {
            alias = "";
        } else {
            alias = alias + ".";
        }

        Envelope envelopeInternal = geometry.getEnvelopeInternal();
        Polygon bounds = DbsUtilities.createPolygonFromEnvelope(envelopeInternal);
        String sql = alias + gCol.geometryColumnName + " && ST_GeomFromText('" + bounds.toText() + "') AND ST_Intersects(" + alias
                + gCol.geometryColumnName + ",ST_GeomFromText('" + geometry.toText() + "'))";
        return sql;
    }

    @Override
    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        Polygon bounds = DbsUtilities.createPolygonFromBounds(x1, y1, x2, y2);
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        if (alias == null) {
            alias = "";
        } else {
            alias = alias + ".";
        }
        String sql = alias + gCol.geometryColumnName + " && ST_GeomFromText('" + bounds.toText() + "') AND ST_Intersects(" + alias
                + gCol.geometryColumnName + ",ST_GeomFromText('" + bounds.toText() + "'))";
        return sql;

    }

    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, int limit, int reprojectSrid, String whereStr )
            throws Exception {
        QueryResult queryResult = new QueryResult();

        GeometryColumn gCol = null;
        try {
            gCol = getGeometryColumnsForTable(tableName);
            // TODO check if it is a virtual table
        } catch (Exception e) {
            // ignore
        }
        boolean hasGeom = gCol != null;

        List<String[]> tableColumnsInfo = getTableColumns(tableName);
        List<String> tableColumns = new ArrayList<>();
        for( String[] info : tableColumnsInfo ) {
            if (DbsUtilities.isReservedName(info[0])) {
                info[0] = DbsUtilities.fixReservedNameForQuery(info[0]);
            }
            tableColumns.add(info[0]);
        }
        if (hasGeom) {
            if (!tableColumns.remove(gCol.geometryColumnName)) {
                String gColLower = gCol.geometryColumnName.toLowerCase();
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

        String sql = "SELECT ";
        List<String> items = new ArrayList<>();
        for( int i = 0; i < tableColumns.size(); i++ ) {
            items.add(tableColumns.get(i));
        }
        if (hasGeom) {
            if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                items.add(gCol.geometryColumnName);
            } else {
                items.add("ST_Transform(" + gCol.geometryColumnName + "," + reprojectSrid + ") AS " + gCol.geometryColumnName);
            }
        }
        String itemsWithComma = DbsUtilities.joinByComma(items);
        if (itemsWithComma.trim().length() == 0) {
            itemsWithComma = "*";
        }
        sql += itemsWithComma;
        sql += " FROM " + tableName;

        List<String> whereStrings = new ArrayList<>();
        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            String spatialindexBBoxWherePiece = getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
            if (spatialindexBBoxWherePiece != null)
                whereStrings.add(spatialindexBBoxWherePiece);
        }
        if (whereStr != null) {
            whereStrings.add(whereStr);
        }

        if (whereStrings.size() > 0) {
            sql += " WHERE "; //
            sql += DbsUtilities.joinBySeparator(whereStrings, " AND ");
        }

        if (limit > 0) {
            sql += " LIMIT " + limit;
        }
        String _sql = sql;
        GeometryColumn _gCol = gCol;
        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                IHMResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();

                for( int i = 1; i <= columnCount; i++ ) {
                    String columnName = rsmd.getColumnName(i);
                    queryResult.names.add(columnName);
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    queryResult.types.add(columnTypeName);
                    if (hasGeom && columnName.equals(_gCol.geometryColumnName)) {
                        queryResult.geometryIndex = i - 1;
                    }
                }

                long start = System.currentTimeMillis();
                while( rs.next() ) {
                    Object[] rec = new Object[columnCount];
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (hasGeom && queryResult.geometryIndex == j - 1) {
                            Geometry geometry = (Geometry) rs.getObject(j);
                            rec[j - 1] = geometry;
                        } else {
                            Object object = rs.getObject(j);
                            if (object instanceof Clob) {
                                object = rs.getString(j);
                            }
                            rec[j - 1] = object;
                        }
                    }
                    queryResult.data.add(rec);
                }
                long end = System.currentTimeMillis();
                queryResult.queryTimeMillis = end - start;
                return queryResult;
            }
        });

    }

    public String getGeojsonIn( String tableName, String[] fields, String wherePiece, Integer precision ) throws Exception {
        if (precision == 0) {
            precision = 6;
        }
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);

        String sql;
        if (fields == null || fields.length == 0) {
            sql = "SELECT ST_AsGeoJson(ST_Collect(ST_Transform(" + gCol.geometryColumnName + ",4326))) FROM " + tableName;
            if (wherePiece != null) {
                sql += " WHERE " + wherePiece;
            }
        } else {
            sql = "SELECT '{\"type\":\"FeatureCollection\",\"features\":['"
                    + " || group_concat('{\"type\":\"Feature\",\"geometry\":' || ST_AsGeoJson(" + gCol.geometryColumnName
                    + ") || ',\"properties\": {' || ";
            List<String> fieldsList = new ArrayList<>();
            for( String field : fields ) {
                String string = "'\"" + field + "\":\"' || " + field + " || '\"'";
                fieldsList.add(string);
            }
            StringBuilder sb = new StringBuilder();
            for( int i = 0; i < fieldsList.size(); i++ ) {
                if (i > 0) {
                    sb.append(" || ',' ||");
                }
                sb.append("\n").append(fieldsList.get(i));
            }
            sql += sb.toString() + " || '}}') || ']}'";
            sql += " FROM " + tableName;
            if (wherePiece != null) {
                sql += " WHERE " + wherePiece;
            }
        }

        String _sql = sql;
        return execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                if (rs.next()) {
                    String geoJson = rs.getString(1);
                    return geoJson;
                }
            }
            return "";
        });

    }

    public void addSrid( String tableName, String codeFromCrs, String geometryColumnName ) throws Exception {
        int srid = Integer.parseInt(codeFromCrs);
        if (geometryColumnName == null)
            geometryColumnName = "the_geom";
        String realName = getProperColumnNameCase(tableName, geometryColumnName);
        String realTableName = getProperTableNameCase(tableName);
        TableLocation tableLocation = TableLocation.parse(realTableName);
        if (srid > 0) {
            getJdbcConnection().createStatement().execute(
                    String.format("ALTER TABLE %s ADD CHECK ST_SRID(" + realName + ")=%d", tableLocation.toString(), srid));
        }
    }

    @Override
    public void accept( IDbVisitor visitor ) throws Exception {
        h2Db.accept(visitor);
    }

}
