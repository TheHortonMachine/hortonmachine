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
package org.hortonmachine.dbs.compat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * Abstract spatial db class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public abstract class ASpatialDb extends ADb implements AutoCloseable {

    public final static String DEFAULT_GEOM_FIELD_NAME = "the_geom";

    protected boolean supportsSpatialIndex = true;

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
     * Create a new spatial table.
     * 
     * @param tableName
     *            the table name.
     * @param tableSrid the table's epsg code.
     * @param geometryFieldData the data for the geometry column, ex. the_geom MULTIPOLYGON
     * @param fieldData
     *            the data for each the field (ex. id INTEGER NOT NULL PRIMARY
     *            KEY).
     * @param foreignKeys
     *            foreign keys definitions, if available (ex. FOREIGN KEY (table1id) REFERENCES table1(id)).
     * @param avoidIndex if <code>true</code>, no spatial index is created.
     * @throws SQLException
     */
    public abstract void createSpatialTable( String tableName, int tableSrid, String geometryFieldData, String[] fieldData,
            String[] foreignKeys, boolean avoidIndex ) throws Exception;

    /**
     * Creates a spatial table with default values for foreign keys and index.
     * 
     * @param tableName
     *            the table name.
     * @param tableSrid the table's epsg code.
     * @param geometryFieldData the data for the geometry column, ex. the_geom MULTIPOLYGON
     * @param fieldData
     *            the data for each the field (ex. id INTEGER NOT NULL PRIMARY
     *            KEY).
     * @throws Exception
     */
    public void createSpatialTable( String tableName, int tableSrid, String geometryFieldData, String[] fieldData )
            throws Exception {
        createSpatialTable(tableName, tableSrid, geometryFieldData, fieldData, null, false);
    }

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
    public abstract String getSpatialindexGeometryWherePiece( String tableName, String alias, Geometry geometry )
            throws Exception;

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
    public abstract String getSpatialindexBBoxWherePiece( String tableName, String alias, double x1, double y1, double x2,
            double y2 ) throws Exception;

    /**
     * Create a spatial index.
     * 
     * @param tableName the table name.
     * @param geomColumnName the geometry column name.
     * @throws Exception
     */
    public void createSpatialIndex( String tableName, String geomColumnName ) throws Exception {
        if (geomColumnName == null) {
            geomColumnName = "the_geom";
        }
        String realColumnName = getProperColumnNameCase(tableName, geomColumnName);
        String realTableName = getProperTableNameCase(tableName);
        String sql = "CREATE SPATIAL INDEX ON " + realTableName + "(" + realColumnName + ");";

        execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.execute(sql.toString());
            }
            return null;
        });

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
     * @param where an optional where string (without the where keyword)
     * @throws Exception
     */
    public void insertGeometry( String tableName, Geometry geometry, String epsg , String where) throws Exception {
        String epsgStr = "4326";
        if (epsg == null) {
            epsgStr = epsg;
        }

        GeometryColumn gc = getGeometryColumnsForTable(tableName);
        String sql = "INSERT INTO " + tableName + " (" + gc.geometryColumnName + ") VALUES (ST_GeomFromText(?, " + epsgStr + "))";
        if(where != null) {
            sql += " WHERE " + where;
        }
        String _sql = sql;
        execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(_sql)) {
                pStmt.setString(1, geometry.toText());
                pStmt.executeUpdate();
            }
            return null;
        });

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
     * Get the list of available tables, mapped by type.
     * 
     * <p>
     * Supported types are:
     * <ul>
     * <li>{@value ISpatialTableNames#INTERNALDATA}</li>
     * <li>{@value ISpatialTableNames#SYSTEM}</li>
     * <li></li>
     * <li></li>
     * <li></li>
     * </ul>
     * 
     * @param doOrder
     * @return the map of tables sorted by aggregated type:
     * @throws Exception
     */
    public abstract HashMap<String, List<String>> getTablesMap( boolean doOrder ) throws Exception;

    /**
     * Get the table records map with geometry in the given envelope.
     * 
     * <p>
     * If the table is not geometric, the geom is set to null.
     * 
     * @param tableName
     *            the table name.
     * @param envelope
     *            the envelope to check, in the table SRS.
     * @param limit
     *            if > 0 a limit is set.
     * @param reprojectSrid an optional srid to require reprojection (-1 is disabled).
     * @param whereStr an optional where condition string to apply.
     * @return the result object.
     * @throws Exception
     */
    public abstract QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, int limit, int reprojectSrid,
            String whereStr ) throws Exception;

    /**
     * Get the geometries of a table inside a given envelope.
     * 
     * @param tableName
     *            the table name.
     * @param envelope
     *            the envelope to check.
     * @param prePostWhere an optional set of 3 parameters. The parameters are: a 
     *          prefix wrapper for geom, a postfix for the same and a where string 
     *          to apply. They all need to be existing if the parameter is passed.
     * @return The list of geometries intersecting the envelope.
     * @throws Exception
     */
    public List<Geometry> getGeometriesIn( String tableName, Envelope envelope, String... prePostWhere ) throws Exception {
        List<String> wheres = new ArrayList<>();
        String pre = "";
        String post = "";
        String where = "";
        if (prePostWhere != null && prePostWhere.length == 3) {
            if (prePostWhere[0] != null)
                pre = prePostWhere[0];
            if (prePostWhere[1] != null)
                post = prePostWhere[1];
            if (prePostWhere[2] != null) {
                where = prePostWhere[2];
                wheres.add(where);
            }
        }

        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        String sql = "SELECT " + pre + gCol.geometryColumnName + post + " FROM " + DbsUtilities.fixTableName(tableName);

        if (envelope != null && supportsSpatialIndex) {
            double x1 = envelope.getMinX();
            double y1 = envelope.getMinY();
            double x2 = envelope.getMaxX();
            double y2 = envelope.getMaxY();
            String spatialindexBBoxWherePiece = getSpatialindexBBoxWherePiece(tableName, null, x1, y1, x2, y2);
            if (spatialindexBBoxWherePiece != null)
                wheres.add(spatialindexBBoxWherePiece);
        }

        if (wheres.size() > 0) {
            sql += " WHERE " + DbsUtilities.joinBySeparator(wheres, " AND ");
        }

        String _sql = sql;
        IGeometryParser geometryParser = getType().getGeometryParser();
        return execOnConnection(connection -> {
            List<Geometry> geoms = new ArrayList<Geometry>();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    Geometry geometry = geometryParser.fromResultSet(rs, 1);
                    if (!supportsSpatialIndex && envelope != null) {
                        // need to check manually
                        if (!geometry.getEnvelopeInternal().intersects(envelope)) {
                            continue;
                        }
                    }
                    geoms.add(geometry);
                }
            }
            return geoms;
        });
    }

    /**
     * Get the geometries of a table intersecting a given geometry.
     * 
     * @param tableName
     *            the table name.
     * @param intersectionGeometry
     *            the geometry to check, assumed in the same srid of the table geometry.
     * @param prePostWhere an optional set of 3 parameters. The parameters are: a 
     *          prefix wrapper for geom, a postfix for the same and a where string 
     *          to apply. They all need to be existing if the parameter is passed.
     * @return The list of geometries intersecting the geometry.
     * @throws Exception
     */
    public List<Geometry> getGeometriesIn( String tableName, Geometry intersectionGeometry, String... prePostWhere )
            throws Exception {
        List<Geometry> geoms = new ArrayList<Geometry>();

        List<String> wheres = new ArrayList<>();
        String pre = "";
        String post = "";
        String where = "";
        if (prePostWhere != null && prePostWhere.length == 3) {
            if (prePostWhere[0] != null)
                pre = prePostWhere[0];
            if (prePostWhere[1] != null)
                post = prePostWhere[1];
            if (prePostWhere[2] != null) {
                where = prePostWhere[2];
                wheres.add(where);
            }
        }
        GeometryColumn gCol = getGeometryColumnsForTable(tableName);
        String sql = "SELECT " + pre + gCol.geometryColumnName + post + " FROM " + DbsUtilities.fixTableName(tableName);

        if (intersectionGeometry != null && supportsSpatialIndex) {
            intersectionGeometry.setSRID(gCol.srid);
            String spatialindexGeometryWherePiece = getSpatialindexGeometryWherePiece(tableName, null, intersectionGeometry);
            if (spatialindexGeometryWherePiece != null)
                wheres.add(spatialindexGeometryWherePiece);
        }

        if (wheres.size() > 0) {
            sql += " WHERE " + DbsUtilities.joinBySeparator(wheres, " AND ");
        }

        IGeometryParser geometryParser = getType().getGeometryParser();
        String _sql = sql;
        return execOnConnection(connection -> {
            PreparedGeometry prepGeom = null;
            if (!supportsSpatialIndex) {
                prepGeom = PreparedGeometryFactory.prepare(intersectionGeometry);
            }
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                while( rs.next() ) {
                    Geometry geometry = geometryParser.fromResultSet(rs, 1);
                    if (prepGeom != null && !prepGeom.intersects(geometry)) {
                        continue;
                    }
                    geoms.add(geometry);
                }
                return geoms;
            }
        });

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
    public abstract String getGeojsonIn( String tableName, String[] fields, String wherePiece, Integer precision )
            throws Exception;

    /**
     * Get the bounds of a table.
     * 
     * @param tableName
     *            the table to query.
     * @return the {@link Envelope} of the table.
     * @throws Exception
     */
    public abstract Envelope getTableBounds( String tableName ) throws Exception;

    /**
     * Get the column [name, type, primarykey] values of a table.
     * 
     * <p>pk = 0 -> false</p>
     * 
     * @param tableName
     *            the table to check.
     * @return the list of column [name, type, pk].
     * @throws SQLException
     */
    public abstract List<String[]> getTableColumns( String tableName ) throws Exception;

    protected abstract void logWarn( String message );

    protected abstract void logInfo( String message );

    protected abstract void logDebug( String message );

    /**
     * Reproject an envelope.
     * 
     * @param fromEnvelope the original envelope.
     * @param fromSrid the original srid.
     * @param toSrid the destination srid.
     * @return the reprojected Envelope.
     * @throws Exception
     */
    public Envelope reproject( Envelope fromEnvelope, int fromSrid, int toSrid ) throws Exception {
        double w = fromEnvelope.getMinX();
        double e = fromEnvelope.getMaxX();
        double s = fromEnvelope.getMinY();
        double n = fromEnvelope.getMaxY();
        String sql = "select ST_Transform( ST_PointFromText('POINT( " + w + " " + s + ")', " + fromSrid + ") , " + toSrid
                + "), ST_Transform( ST_PointFromText('POINT( " + e + " " + n + ")', " + fromSrid + ") , " + toSrid + ")";
        return execOnConnection(connection -> {
            IGeometryParser gp = getType().getGeometryParser();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    Geometry llPoint = gp.fromResultSet(rs, 1);
                    Geometry urPoint = gp.fromResultSet(rs, 2);
                    if (llPoint instanceof Point) {
                        Point ll = (Point) llPoint;
                        Point ur = (Point) urPoint;
                        Envelope newEnv = new Envelope(ll.getX(), ur.getX(), ll.getY(), ur.getY());
                        return newEnv;
                    }
                }
                return null;
            }
        });
    }

    /**
     * Reproject an geometry.
     * 
     * @param fromGeometry the original geometry.
     * @param fromSrid the original srid.
     * @param toSrid the destination srid.
     * @return the reprojected Geometry.
     * @throws Exception
     */
    public Geometry reproject( Geometry fromGeometry, int fromSrid, int toSrid ) throws Exception {
        String sql = "select ST_Transform( ST_GeomFromText('" + fromGeometry.toText() + "', " + fromSrid + ") , " + toSrid + ")";
        return execOnConnection(connection -> {
            IGeometryParser gp = getType().getGeometryParser();
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    Geometry repGeom = gp.fromResultSet(rs, 1);
                    return repGeom;
                }
                return null;
            }
        });
    }

    /**
     * Run a generic sql string (also multiline).
     * 
     * @param sql the sql to run.
     * @throws Exception
     */
    public void runSql( String sql ) throws Exception {
        String[] sqlSplit = sql.split("\n");
        StringBuilder sb = new StringBuilder();
        for( String string : sqlSplit ) {
            String trim = string.trim();
            if (trim.length() == 0) {
                continue;
            }
            if (trim.startsWith("--")) {
                continue;
            }
            sb.append(trim + " ");
        }

        String[] sqls = sb.toString().split(";");
        for( String single : sqls ) {
            executeInsertUpdateDeleteSql(single);
        }
    }

}