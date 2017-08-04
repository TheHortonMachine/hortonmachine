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
package org.jgrasstools.dbs.compat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.dbs.spatialite.ESpatialiteGeometryType;
import org.jgrasstools.dbs.spatialite.QueryResult;
import org.jgrasstools.dbs.spatialite.RasterCoverage;
import org.jgrasstools.dbs.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.dbs.spatialite.SpatialiteTableNames;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * Abstract spatial db class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public abstract class ASpatialDb extends ADb implements AutoCloseable {

    public static String PK_UID = "PK_UID";
    public static String PKUID = "PKUID";
    public static String defaultGeomFieldName = "the_geom";

    /**
     * Open the connection to a database.
     * 
     * <b>Make sure the connection object is created here.</b>
     * 
     * @param dbPath
     *            the database path. If <code>null</code>, an in-memory db is
     *            created.
     * @return <code>true</code> if the database did already exist.
     * @throws Exception
     */
    public abstract boolean open( String dbPath ) throws Exception;

    /**
     * Create Spatial Metadata initialize SPATIAL_REF_SYS and GEOMETRY_COLUMNS.
     * 
     * <p>
     * If the optional argument mode is not specified then any possible ESPG
     * SRID definition will be inserted into the spatial_ref_sys table.
     * </p>
     * <p>
     * If the mode arg 'WGS84' (alias 'WGS84_ONLY') is specified, then only
     * WGS84-related EPSG SRIDs will be inserted
     * </p>
     * <p>
     * If the mode arg 'NONE' (alias 'EMPTY') is specified, no EPSG SRID will be
     * inserted at all
     * </p>
     * 
     * @param options
     *            optional tweaks.
     * @throws Exception
     */
    public abstract void initSpatialMetadata( String options ) throws Exception;

    /**
     * Adds a geometry column to a table.
     * 
     * @param tableName
     *            the table name.
     * @param geomColName
     *            the geometry column name.
     * @param geomType
     *            the geometry type (ex. LINESTRING);
     * @param epsg
     *            the optional epsg code (default is 4326);
     * @param avoidIndex if <code>true</code>, the index is not created.
     * @throws Exception
     */
    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg,
            boolean avoidIndex ) throws Exception {
        String epsgStr = "4326";
        if (epsg != null) {
            epsgStr = epsg;
        }
        String geomTypeStr = "LINESTRING";
        if (geomType != null) {
            geomTypeStr = geomType;
        }

        if (geomColName == null) {
            geomColName = defaultGeomFieldName;
        }

        try (IJGTStatement stmt = mConn.createStatement()) {
            String sql = "SELECT AddGeometryColumn('" + tableName + "','" + geomColName + "', " + epsgStr + ", '" + geomTypeStr
                    + "', 'XY')";
            stmt.execute(sql);

            if (!avoidIndex) {
                sql = "SELECT CreateSpatialIndex('" + tableName + "', '" + geomColName + "');";
                stmt.execute(sql);
            }
        }
    }

    /**
     * Adds a geometry column to a table.
     * 
     * @param tableName
     *            the table name.
     * @param geomColName
     *            the geometry column name.
     * @param geomType
     *            the geometry type (ex. LINESTRING);
     * @param epsg
     *            the optional epsg code (default is 4326);
     * @throws Exception
     */
    public void addGeometryXYColumnAndIndex( String tableName, String geomColName, String geomType, String epsg )
            throws Exception {
        addGeometryXYColumnAndIndex(tableName, geomColName, geomType, epsg, false);
    }

    /**
     * Insert a geometry into a table.
     * 
     * @param tableName
     *            the table to use.
     * @param geometry
     *            the geometry to insert.
     * @param epsg
     *            the optional epsg.
     * @throws Exception
     */
    public void insertGeometry( String tableName, Geometry geometry, String epsg ) throws Exception {
        String epsgStr = "4326";
        if (epsg == null) {
            epsgStr = epsg;
        }

        SpatialiteGeometryColumns gc = getGeometryColumnsForTable(tableName);
        String sql = "INSERT INTO " + tableName + " (" + gc.f_geometry_column + ") VALUES (GeomFromText(?, " + epsgStr + "))";
        try (IJGTPreparedStatement pStmt = mConn.prepareStatement(sql)) {
            pStmt.setString(1, geometry.toText());
            pStmt.executeUpdate();
        }
    }

    /**
     * Get the list of available raster coverages.
     * 
     * @param doOrder
     *            if <code>true</code>, the names are ordered.
     * @return the list of raster coverages.
     * @throws Exception
     */
    public List<RasterCoverage> getRasterCoverages( boolean doOrder ) throws Exception {
        List<RasterCoverage> rasterCoverages = new ArrayList<RasterCoverage>();
        String orderBy = " ORDER BY name";
        if (!doOrder) {
            orderBy = "";
        }

        String sql = "SELECT " + RasterCoverage.COVERAGE_NAME + ", " + RasterCoverage.TITLE + ", " + RasterCoverage.SRID + ", "
                + RasterCoverage.COMPRESSION + ", " + RasterCoverage.EXTENT_MINX + ", " + RasterCoverage.EXTENT_MINY + ", "
                + RasterCoverage.EXTENT_MAXX + ", " + RasterCoverage.EXTENT_MAXY + " FROM " + RasterCoverage.TABLENAME + orderBy;
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                RasterCoverage rc = new RasterCoverage();
                int i = 1;
                rc.coverage_name = rs.getString(i++);
                rc.title = rs.getString(i++);
                rc.srid = rs.getInt(i++);
                rc.compression = rs.getString(i++);
                rc.extent_minx = rs.getDouble(i++);
                rc.extent_miny = rs.getDouble(i++);
                rc.extent_maxx = rs.getDouble(i++);
                rc.extent_maxy = rs.getDouble(i++);
                rasterCoverages.add(rc);
            }
            return rasterCoverages;
        }
    }

    /**
     * Get the list of available tables, mapped by type.
     * 
     * <p>
     * Supported types are:
     * <ul>
     * <li>{@value SpatialiteTableNames#INTERNALDATA}</li>
     * <li>{@value SpatialiteTableNames#METADATA}</li>
     * <li>{@value SpatialiteTableNames#SPATIALINDEX}</li>
     * <li>{@value SpatialiteTableNames#STYLE}</li>
     * <li>{@value SpatialiteTableNames#USERDATA}</li>
     * <li></li>
     * <li></li>
     * <li></li>
     * </ul>
     * 
     * @param doOrder
     * @return the map of tables sorted by aggregated type:
     * @throws Exception
     */
    public HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception {
        List<String> tableNames = getTables(doOrder);
        HashMap<String, List<String>> tablesMap = SpatialiteTableNames.getTablesSorted(tableNames, doOrder);
        return tablesMap;
    }

    /**
     * Get the geometry column definition for a given table.
     * 
     * @param tableName
     *            the table to check.
     * @return the {@link SpatialiteGeometryColumns column info}.
     * @throws Exception
     */
    public SpatialiteGeometryColumns getGeometryColumnsForTable( String tableName ) throws Exception {
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

        String sql = "select " + SpatialiteGeometryColumns.F_TABLE_NAME + ", " //
                + SpatialiteGeometryColumns.F_GEOMETRY_COLUMN + ", " //
                + SpatialiteGeometryColumns.GEOMETRY_TYPE + "," //
                + SpatialiteGeometryColumns.COORD_DIMENSION + ", " //
                + SpatialiteGeometryColumns.SRID + ", " //
                + SpatialiteGeometryColumns.SPATIAL_INDEX_ENABLED + " from " //
                + attachedStr + SpatialiteGeometryColumns.TABLENAME + " where " + SpatialiteGeometryColumns.F_TABLE_NAME + "='"
                + tableName + "'";
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                SpatialiteGeometryColumns gc = new SpatialiteGeometryColumns();
                gc.f_table_name = rs.getString(1);
                gc.f_geometry_column = rs.getString(2);
                gc.geometry_type = rs.getInt(3);
                gc.coord_dimension = rs.getInt(4);
                gc.srid = rs.getInt(5);
                gc.spatial_index_enabled = rs.getInt(6);
                return gc;
            }
            return null;
        }
    }

    /**
     * Checks if a table is spatial.
     * 
     * @param tableName
     *            the table to check.
     * @return <code>true</code> if a geometry column is present.
     * @throws Exception
     */
    public boolean isTableSpatial( String tableName ) throws Exception {
        SpatialiteGeometryColumns geometryColumns = getGeometryColumnsForTable(tableName);
        return geometryColumns != null;
    }

    /**
     * Get the table records map with geometry in the given envelope.
     * 
     * <p>
     * If the table is not geometric, the geom is set to null.
     * 
     * @param tableName
     *            the table name.
     * @param envelope
     *            the envelope to check.
     * @param limit
     *            if > 0 a limit is set.
     * @param alsoPK_UID
     *            if <code>true</code>, also the PK_UID column is considered.
     * @return the list of found records.
     * @throws SQLException
     * @throws ParseException
     */
    public QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID, int limit,
            int reprojectSrid ) throws Exception {
        QueryResult queryResult = new QueryResult();

        SpatialiteGeometryColumns gCol = null;
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
        List<String> items = new ArrayList<>();
        for( int i = 0; i < tableColumns.size(); i++ ) {
            items.add(tableColumns.get(i));
        }
        if (hasGeom) {
            if (reprojectSrid == -1 || reprojectSrid == gCol.srid) {
                items.add("ST_AsBinary(" + gCol.f_geometry_column + ") AS " + gCol.f_geometry_column);
            } else {
                items.add("ST_AsBinary(ST_Transform(" + gCol.f_geometry_column + "," + reprojectSrid + ")) AS "
                        + gCol.f_geometry_column);
            }
        }
        String itemsWithComma = join(items);
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
        WKBReader wkbReader = new WKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (hasGeom && columnName.equals(gCol.f_geometry_column)) {
                    queryResult.geometryIndex = i - 1;
                }
            }

            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (hasGeom && queryResult.geometryIndex == j - 1) {
                        byte[] geomBytes = rs.getBytes(j);
                        Geometry geometry = wkbReader.read(geomBytes);
                        rec[j - 1] = geometry;
                    } else {
                        Object object = rs.getObject(j);
                        rec[j - 1] = object;
                    }
                }
                queryResult.data.add(rec);
            }
            return queryResult;
        }
    }

    private String join( List<String> items ) {
        StringBuilder sb = new StringBuilder();
        for( String item : items ) {
            sb.append(",").append(item);
        }
        return sb.substring(1);
    }

    /**
     * Execute a query from raw sql.
     * 
     * @param sql
     *            the sql to run.
     * @param limit
     *            a limit, ignored if < 1
     * @return the resulting records.
     * @throws Exception
     */
    public QueryResult getTableRecordsMapFromRawSql( String sql, int limit ) throws Exception {
        QueryResult queryResult = new QueryResult();
        WKBReader wkbReader = new WKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            IJGTResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            int geometryIndex = -1;
            for( int i = 1; i <= columnCount; i++ ) {
                int columnType = rsmd.getColumnType(i);
                String columnName = rsmd.getColumnName(i);
                queryResult.names.add(columnName);
                String columnTypeName = rsmd.getColumnTypeName(i);
                queryResult.types.add(columnTypeName);
                if (columnTypeName.equals("BLOB") && ESpatialiteGeometryType.forValue(columnType) != null) {
                    geometryIndex = i;
                    queryResult.geometryIndex = i - 1;
                }
            }
            int count = 0;
            while( rs.next() ) {
                Object[] rec = new Object[columnCount];
                for( int j = 1; j <= columnCount; j++ ) {
                    if (j == geometryIndex) {
                        byte[] geomBytes = rs.getBytes(j);
                        try {
                            Geometry geometry = wkbReader.read(geomBytes);
                            rec[j - 1] = geometry;
                        } catch (Exception e) {
                            // ignore this, it could be missing ST_AsBinary() in
                            // the sql
                        }
                    } else {
                        Object object = rs.getObject(j);
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

    /**
     * Execute a query from raw sql and put the result in a csv file.
     * 
     * @param sql
     *            the sql to run.
     * @param csvFile
     *            the output file.
     * @param doHeader
     *            if <code>true</code>, the header is written.
     * @param separator
     *            the separator (if null, ";" is used).
     * @throws Exception
     */
    public void runRawSqlToCsv( String sql, File csvFile, boolean doHeader, String separator ) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {
            WKBReader wkbReader = new WKBReader();
            try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
                IJGTResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount();
                int geometryIndex = -1;
                for( int i = 1; i <= columnCount; i++ ) {
                    if (i > 1) {
                        bw.write(separator);
                    }
                    int columnType = rsmd.getColumnType(i);
                    String columnTypeName = rsmd.getColumnTypeName(i);
                    String columnName = rsmd.getColumnName(i);
                    bw.write(columnName);
                    if (columnTypeName.equals("BLOB") && ESpatialiteGeometryType.forValue(columnType) != null) {
                        geometryIndex = i;
                    }
                }
                bw.write("\n");
                while( rs.next() ) {
                    for( int j = 1; j <= columnCount; j++ ) {
                        if (j > 1) {
                            bw.write(separator);
                        }
                        byte[] geomBytes = null;
                        if (j == geometryIndex) {
                            geomBytes = rs.getBytes(j);
                        }
                        if (geomBytes != null) {
                            try {
                                Geometry geometry = wkbReader.read(geomBytes);
                                bw.write(geometry.toText());
                            } catch (Exception e) {
                                // write it as it comes
                                Object object = rs.getObject(j);
                                if (object != null) {
                                    bw.write(object.toString());
                                } else {
                                    bw.write("");
                                }
                            }
                        } else {
                            Object object = rs.getObject(j);
                            if (object != null) {
                                bw.write(object.toString());
                            } else {
                                bw.write("");
                            }
                        }
                    }
                    bw.write("\n");
                }
            }
        }
    }

    /**
     * Get the geometries of a table inside a given envelope.
     * 
     * @param tableName
     *            the table name.
     * @param envelope
     *            the envelope to check.
     * @return The list of geometries intersecting the envelope.
     * @throws Exception
     */
    public List<Geometry> getGeometriesIn( String tableName, Envelope envelope ) throws Exception {
        List<Geometry> geoms = new ArrayList<Geometry>();

        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        String sql = "SELECT ST_AsBinary(" + gCol.f_geometry_column + ") FROM " + tableName;

        if (envelope != null) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            sql += " WHERE " + getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
        }
        WKBReader wkbReader = new WKBReader();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql)) {
            while( rs.next() ) {
                byte[] geomBytes = rs.getBytes(1);
                Geometry geometry = wkbReader.read(geomBytes);
                geoms.add(geometry);
            }
            return geoms;
        }
    }

    /**
     * Get the geojson of a table inside a given envelope.
     * 
     * @param tableName
     *            the table name.
     * @param wherePiece the where string (can be constructed for example with {@link #getSpatialindexBBoxWherePiece(String, String, double, double, double, double)}
     * @return The resulting geojson.
     * @throws Exception
     */
    public String getGeojsonIn( String tableName, String[] fields, String wherePiece, Integer precision ) throws Exception {
        if (precision == 0) {
            precision = 6;
        }
        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);

        String sql;
        if (fields == null || fields.length == 0) {
            sql = "SELECT asGeoJSON(ST_Collect(ST_Transform(" + gCol.f_geometry_column + ",4326)), " + precision + ",0) FROM "
                    + tableName;
            if (wherePiece != null) {
                sql += " WHERE " + wherePiece;
            }
        } else {
            sql = "SELECT \"{\"\"type\"\":\"\"FeatureCollection\"\",\"\"features\"\":[\" || group_concat(\"{\"\"type\"\":\"\"Feature\"\",\"\"geometry\"\":\" || asGeoJSON("
                    + gCol.f_geometry_column + ", " + precision + ", 0) || \",\"\"properties\"\": {\" || ";
            List<String> fieldsList = new ArrayList<>();
            for( String field : fields ) {
                String string = "\"\"\"" + field + "\"\":\"\"\" || " + field + " || \"\"\"\"";
                fieldsList.add(string);
            }
            StringBuilder sb = new StringBuilder();
            for( int i = 0; i < fieldsList.size(); i++ ) {
                if (i > 0) {
                    sb.append(" || \",\" ||");
                }
                sb.append("\n").append(fieldsList.get(i));
            }
            sql += sb.toString() + " || \"}}\") || \"]}\"";
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

    /**
     * Get the where cause of a Spatialindex based BBOX query.
     * 
     * @param tableName
     *            the name of the table.
     * @param x1
     *            west bound.
     * @param y1
     *            south bound.
     * @param x2
     *            east bound.
     * @param y2
     *            north bound.
     * @return the sql piece.
     * @throws Exception
     */
    public String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }
        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached
            // database
            tableName = "DB=" + tableName;
        }

        String sql = "ST_Intersects(" + alias + gCol.f_geometry_column + ", BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2
                + ")) = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "f_table_name = '" + tableName + "' AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }

    /**
     * Get the where query piece based on a geometry intersection.
     * 
     * @param tableName
     *            the table to query.
     * @param alias
     *            optinal alias.
     * @param geometry
     *            the geometry to intersect.
     * @return the query piece.
     * @throws Exception
     */
    public String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception {
        String rowid = "";
        if (alias == null) {
            alias = "";
            rowid = tableName + ".ROWID";
        } else {
            rowid = alias + ".ROWID";
            alias = alias + ".";
        }

        Envelope envelope = geometry.getEnvelopeInternal();
        double x1 = envelope.getMinX();
        double x2 = envelope.getMaxX();
        double y1 = envelope.getMinY();
        double y2 = envelope.getMaxY();
        SpatialiteGeometryColumns gCol = getGeometryColumnsForTable(tableName);
        if (tableName.indexOf('.') != -1) {
            // if the tablename contains a dot, then it comes from an attached
            // database
            tableName = "DB=" + tableName;
        }
        String sql = "ST_Intersects(" + alias + gCol.f_geometry_column + ", " + "GeomFromText('" + geometry.toText() + "')"
                + ") = 1 AND " + rowid + " IN ( SELECT ROWID FROM SpatialIndex WHERE "//
                + "f_table_name = '" + tableName + "' AND " //
                + "search_frame = BuildMbr(" + x1 + ", " + y1 + ", " + x2 + ", " + y2 + "))";
        return sql;
    }

    /**
     * Get the bounds of a table.
     * 
     * @param tableName
     *            the table to query.
     * @return the {@link Envelope} of the table.
     * @throws Exception
     */
    public abstract Envelope getTableBounds( String tableName ) throws Exception;

    protected abstract void logWarn( String message );

    protected abstract void logInfo( String message );

    protected abstract void logDebug( String message );

}