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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.dbs.compat.objects.QueryResult;

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
     * <p>Possible options for spatialite are:
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
     * </p>
     * 
     * @param options
     *            optional tweaks.
     * @throws Exception
     */
    public abstract void initSpatialMetadata( String options ) throws Exception;
    
    /**
     * Get the geometry column for the given table.
     * 
     * @param tableName the table.
     * @return the geometry column or <code>null</code>.
     * @throws Exception
     */
    public abstract GeometryColumn getGeometryColumnsForTable( String tableName ) throws Exception;

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
    public abstract String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry ) throws Exception;
    
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
    public abstract String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2, double y2 )
            throws Exception;

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

        GeometryColumn gc = getGeometryColumnsForTable(tableName);
        String sql = "INSERT INTO " + tableName + " (" + gc.geometryColumnName + ") VALUES (ST_GeomFromText(?, " + epsgStr + "))";
        try (IJGTPreparedStatement pStmt = mConn.prepareStatement(sql)) {
            pStmt.setString(1, geometry.toText());
            pStmt.executeUpdate();
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
        GeometryColumn geometryColumns = getGeometryColumnsForTable(tableName);
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
                items.add("ST_AsBinary(" + gCol.geometryColumnName + ") AS " + gCol.geometryColumnName);
            } else {
                items.add("ST_AsBinary(ST_Transform(" + gCol.geometryColumnName + "," + reprojectSrid + ")) AS "
                        + gCol.geometryColumnName);
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
                if (hasGeom && columnName.equals(gCol.geometryColumnName)) {
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

        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        String sql = "SELECT ST_AsBinary(" + gCol.geometryColumnName + ") FROM " + tableName;

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
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);

        String sql;
        if (fields == null || fields.length == 0) {
            sql = "SELECT ST_asGeoJSON(ST_Collect(ST_Transform(" + gCol.geometryColumnName + ",4326)), " + precision + ",0) FROM "
                    + tableName;
            if (wherePiece != null) {
                sql += " WHERE " + wherePiece;
            }
        } else {
            sql = "SELECT \"{\"\"type\"\":\"\"FeatureCollection\"\",\"\"features\"\":[\" || group_concat(\"{\"\"type\"\":\"\"Feature\"\",\"\"geometry\"\":\" || ST_asGeoJSON("
                    + gCol.geometryColumnName + ", " + precision + ", 0) || \",\"\"properties\"\": {\" || ";
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