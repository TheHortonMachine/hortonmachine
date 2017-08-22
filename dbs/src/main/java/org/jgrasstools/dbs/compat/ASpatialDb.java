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
import java.util.HashMap;
import java.util.List;

import org.jgrasstools.dbs.compat.objects.QueryResult;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;

/**
 * Abstract spatial db class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public abstract class ASpatialDb extends ADb implements AutoCloseable {

    public static String PK_UID = "PK_UID";
    public static String PKUID = "PKUID";
    public final static String DEFAULT_GEOM_FIELD_NAME = "the_geom";

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
     * Get the list of available tables, mapped by type.
     * 
     * <p>
     * Supported types are:
     * <ul>
     * <li>{@value ISpatialTableNames#INTERNALDATA}</li>
     * <li>{@value ISpatialTableNames#METADATA}</li>
     * <li>{@value ISpatialTableNames#SPATIALINDEX}</li>
     * <li>{@value ISpatialTableNames#STYLE}</li>
     * <li>{@value ISpatialTableNames#USERDATA}</li>
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
     *            the envelope to check.
     * @param limit
     *            if > 0 a limit is set.
     * @param alsoPK_UID
     *            if <code>true</code>, also the PK_UID column is considered.
     * @return the list of found records.
     * @throws SQLException
     * @throws ParseException
     */
    public abstract QueryResult getTableRecordsMapIn( String tableName, Envelope envelope, boolean alsoPK_UID, int limit,
            int reprojectSrid ) throws Exception;

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
    public abstract List<Geometry> getGeometriesIn( String tableName, Envelope envelope ) throws Exception;

    /**
     * Get the geometries of a table intersecting a given geometry.
     * 
     * @param tableName
     *            the table name.
     * @param geometry
     *            the geometry to check.
     * @return The list of geometries intersecting the geometry.
     * @throws Exception
     */
    public abstract List<Geometry> getGeometriesIn( String tableName, Geometry geometry ) throws Exception;

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

    protected abstract void logWarn( String message );

    protected abstract void logInfo( String message );

    protected abstract void logDebug( String message );

    /**
     * Extract the geometry from a resultset.
     * 
     * @param resultSet the resultset to get the {@link Geometry} from.
     * @param position the position of the geometry object in the resultset.
     * @return the extracted geometry.
     * @throws Exception 
     */
    public abstract Geometry getGeometryFromResultSet( IJGTResultSet resultSet, int position ) throws Exception;

}