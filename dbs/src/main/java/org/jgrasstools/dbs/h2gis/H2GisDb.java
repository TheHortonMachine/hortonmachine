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
package org.jgrasstools.dbs.h2gis;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.h2.tools.Server;
import org.h2gis.ext.H2GISExtension;
import org.h2gis.functions.factory.H2GISFunctions;
import org.h2gis.utilities.SFSUtilities;
import org.h2gis.utilities.TableLocation;
import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.EDb;
import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.dbs.compat.objects.ForeignKey;
import org.jgrasstools.dbs.compat.objects.QueryResult;
import org.jgrasstools.dbs.utils.DbsUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

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
    private static final Logger logger = LoggerFactory.getLogger(H2GisDb.class);
    private String user = "sa";
    private String password = "";
    private Connection jdbcConn;
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

    /**
     * Start the server mode.
     * 
     *<pre>
     * Server server = Server.createTcpServer(
     *     "-tcpPort", "9123", "-tcpAllowOthers").start();
     * </pre>
     * Supported options are:
     * -tcpPort, -tcpSSL, -tcpPassword, -tcpAllowOthers, -tcpDaemon,
     * -trace, -ifExists, -baseDir, -key.
     * See the main method for details.
     * <p>
     * 
     * @return
     * @throws SQLException
     */
    public static Server startServerMode( String... args ) throws SQLException {
        Server server = Server.createTcpServer(args).start();
        return server;
    }

    public boolean open( String dbPath ) throws Exception {
        h2Db.setCredentials(user, password);

        if (dbPath.endsWith(EDb.H2GIS.getExtension())) {
            dbPath = dbPath.substring(0, dbPath.length() - (EDb.H2GIS.getExtension().length() + 1));
        }
        boolean dbExists = h2Db.open(dbPath);
        if (dbExists) {
            wasInitialized = true;
        }

        jdbcConn = SFSUtilities.wrapConnection(h2Db.getJdbcConnection());
        if (!dbExists)
            initSpatialMetadata(null);

        this.mDbPath = h2Db.getDatabasePath();
        mConn = h2Db.getConnection();
        if (mPrintInfos) {
            String[] dbInfo = getDbInfo();
            logger.info("H2 Version: " + dbInfo[0]);
            logger.info("H2GIS Version: " + dbInfo[1]);
        }
        return dbExists;
    }

    public Connection getJdbcConnection() {
        return jdbcConn;
    }

    @Override
    public void initSpatialMetadata( String options ) throws Exception {
        if (!wasInitialized) {
            H2GISExtension.load(jdbcConn);
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
        // try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs =
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

        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
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
    }

    public String[] getDbInfo() throws Exception {
        // checking h2 version
        String sql = "SELECT H2VERSION(), H2GISVERSION();";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            String[] info = new String[2];
            while( rs.next() ) {
                // read the result set
                info[0] = rs.getString(1);
                info[1] = rs.getString(2);
            }
            return info;
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

        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute(sb.toString());
        }

        addSrid(tableName, String.valueOf(srid));

        if (!avoidIndex) {
            String[] split = geometryFieldData.trim().split("\\s+");
            String geomColName = split[0];

            String indexSql = "CREATE SPATIAL INDEX ON " + tableName + "(" + geomColName + ")";
            executeInsertUpdateDeleteSql(indexSql);
        }
    }

    @Override
    public List<String> getTables( boolean doOrder ) throws Exception {
        return h2Db.getTables(doOrder);
    }

    public String checkSqlCompatibilityIssues( String sql ) {
        return h2Db.checkSqlCompatibilityIssues(sql);
    }
    
    @Override
    public boolean hasTable( String tableName ) throws Exception {
        return h2Db.hasTable(tableName);
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
    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = H2GisTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws Exception {
        QueryResult queryResult = new QueryResult();

        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql);) {

            int geomIndex = -1;
            IJGTResultSetMetaData rsmd = rs.getMetaData();
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
    }

    @Override
    protected void logWarn( String message ) {
        logger.warn(message);
    }

    @Override
    protected void logInfo( String message ) {
        logger.info(message);
    }

    @Override
    protected void logDebug( String message ) {
        logger.debug(message);
    }
    
    public Geometry getGeometryFromResultSet( IJGTResultSet resultSet, int position ) throws Exception {
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

        String sql = "select " + H2GisGeometryColumns.F_TABLE_NAME + ", " //
                + H2GisGeometryColumns.F_GEOMETRY_COLUMN + ", " //
                + H2GisGeometryColumns.GEOMETRY_TYPE + "," //
                + H2GisGeometryColumns.COORD_DIMENSION + ", " //
                + H2GisGeometryColumns.SRID + " from " //
                + attachedStr + H2GisGeometryColumns.TABLENAME + " where Lower(" + H2GisGeometryColumns.F_TABLE_NAME + ")=Lower('"
                + tableName + "')";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                H2GisGeometryColumns gc = new H2GisGeometryColumns();
                gc.tableName = rs.getString(1);
                gc.geometryColumnName = rs.getString(2);
                gc.geometryType = rs.getInt(3);
                gc.coordinatesDimension = rs.getInt(4);
                gc.srid = rs.getInt(5);
                // gc.isSpatialIndexEnabled = rs.getInt(6);
                return gc;
            }
            return null;
        }
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

    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID, int limit,
            int reprojectSrid ) throws Exception {
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
        if (!alsoPK_UID) {
            if (!tableColumns.remove(PK_UID)) {
                tableColumns.remove(PKUID);
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
        sql += itemsWithComma;
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
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (hasGeom && columnName.equals(gCol.geometryColumnName)) {
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
    }

    public List<Geometry> getGeometriesIn( String tableName, Envelope envelope ) throws Exception {
        List<Geometry> geoms = new ArrayList<Geometry>();

        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        String sql = "SELECT " + gCol.geometryColumnName + " FROM " + tableName;

        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE " + getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                Geometry geometry = (Geometry) rs.getObject(1);
                geoms.add(geometry);
            }
            return geoms;
        }
    }

    public List<Geometry> getGeometriesIn( String tableName, Geometry intersectionGeometry ) throws Exception {
        List<Geometry> geoms = new ArrayList<Geometry>();

        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        String sql = "SELECT " + gCol.geometryColumnName + " FROM " + tableName;

        if (intersectionGeometry != null) {
            sql += " WHERE " + getSpatialindexGeometryWherePiece(tableName, null, intersectionGeometry);
        }
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                Geometry geometry = (Geometry) rs.getObject(1);
                geoms.add(geometry);
            }
            return geoms;
        }
    }

    public String getGeojsonIn( String tableName, String[] fields, String wherePiece, Integer precision ) throws Exception {
        if (precision == 0) {
            precision = 6;
        }
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);

        if (fields != null) {
            logger.warn("H2Gis does not support geojson export with fields.");
        }

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
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String geoJson = rs.getString(1);
                return geoJson;
            }
        }
        return "";
    }

    public static void main( String[] args ) throws Exception {
        try (H2GisDb db = new H2GisDb()) {
            // db.setCredentials("asd", "asd");
            boolean existed = db.open("/home/hydrologis/TMP/H2GIS/h2_test1");
            if (!existed)
                db.initSpatialMetadata(null);

            db.createTable("ROADS", "the_geom MULTILINESTRING", "speed_limit INT");
            db.createIndex(PK_UID, PKUID, existed);

        }
    }

    public void addSrid( String tableName, String codeFromCrs ) throws Exception {
        int srid = Integer.parseInt(codeFromCrs);
        try {
            TableLocation tableLocation = TableLocation.parse(tableName);
            SFSUtilities.addTableSRIDConstraint(jdbcConn, tableLocation, srid);
        } catch (Exception e) {
            TableLocation tableLocation = TableLocation.parse(tableName.toUpperCase());
            SFSUtilities.addTableSRIDConstraint(jdbcConn, tableLocation, srid);
        }
    }
}
