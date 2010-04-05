/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
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
package eu.hydrologis.edc.databases;

import java.util.Map;

import org.hibernate.classic.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A handler class that has to take care for particular queries not dealt by hibernate.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface QueryHandler {

    /**
     * Get geometries by their ids from a table.
     * 
     * <p>
     * Note that in edc the geometry tables have always a field ID to
     * link to the id of the table that contains their attributes.
     * </p>
     * 
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param ids the ids of the records to extract.
     * @param epsg the epsg code to which to convert the geometries to.
     * @return the map of the geometries found bound to the given id.
     * @throws Exception
     */
    public Map<Long, Geometry> getGeometries( String schemaName, String tableName, String epsg,
            Long... ids ) throws Exception;

    /**
     * Get 3D geometries by their ids from a table.
     * 
     * <p>
     * Note that in edc the geometry tables have always a field ID to
     * link to the id of the table that contains their attributes.
     * </p>
     * 
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param ids the ids of the records to extract.
     * @param epsg the epsg code to which to convert the geometries to.
     * @return the map of the geometries found bound to the given id.
     * @throws Exception
     */
    public Map<Long, Geometry> getGeometries3D( String schemaName, String tableName, String epsg,
            Long... ids ) throws Exception;

    /**
     * Insert a point geometry into the database.
     * 
     * @param session the session to use for the insert or null, in which case a new one is created
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param id the id the geometry is connected to.
     * @param pointCoordinate the {@link Coordinate} of the point to add.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @throws Exception
     */
    public void insertPointGeometry( Session session, String schemaName, String tableName, Long id,
            Coordinate pointCoordinate, CoordinateReferenceSystem crs ) throws Exception;

    /**
     * Insert a point geometry into the database.
     * 
     * @param session the session to use for the insert or null, in which case a new one is created
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param id the id the geometry is connected to.
     * @param pointCoordinate the {@link Coordinate} of the point to add.
     * @param epsgCode the EPSG code number (format = EPSG:4326).
     * @throws Exception
     */
    public void insertPointGeometry( Session session, String schemaName, String tableName, Long id,
            Coordinate pointCoordinate, String epsgCode ) throws Exception;

    /**
     * Insert a line geometry into the database.
     * 
     * @param session the session to use for the insert or null, in which case a new one is created
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param id the id the geometry is connected to.
     * @param lineString the {@link LineString} to be inserted.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @throws Exception
     */
    public void insertLinestringGeometry( Session session, String schemaName, String tableName,
            Long id, LineString lineString, CoordinateReferenceSystem crs ) throws Exception;

    /**
     * Insert a line geometry into the database.
     * 
     * @param session the session to use for the insert or null, in which case a new one is created
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param id the id the geometry is connected to.
     * @param lineString the {@link LineString} to be inserted.
     * @param epsgCode the EPSG code number.
     * @throws Exception
     */
    public void insertLinestringGeometry( Session session, String schemaName, String tableName,
            Long id, LineString lineString, String epsgCode ) throws Exception;

    /**
     * Insert a 3D line geometry into the database.
     * 
     * @param session the session to use for the insert or null, in which case a new one is created
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param id the id the geometry is connected to.
     * @param lineString the 3D {@link LineString} to be inserted.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @throws Exception
     */
    public void insertLinestringGeometry3D( Session session, String schemaName, String tableName,
            Long id, LineString lineString, CoordinateReferenceSystem crs ) throws Exception;

    /**
     * Insert a 3d line geometry into the database.
     * 
     * @param session the session to use for the insert or null, in which case a new one is created
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param id the id the geometry is connected to.
     * @param lineString the 3D {@link LineString} to be inserted.
     * @param epsgCode the EPSG code number.
     * @throws Exception
     */
    public void insertLinestringGeometry3D( Session session, String schemaName, String tableName,
            Long id, LineString lineString, String epsgCode ) throws Exception;

    /**
     * Insert a polygon geometry into the database.
     * 
     * @param session the session to use for the insert or null, in which case a new one is created
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param id the id the geometry is connected to.
     * @param polygon the {@link Polygon} to be inserted.
     * @param crs the {@link CoordinateReferenceSystem}.
     * @throws Exception
     */
    public void insertPolygonalGeometry( Session session, String schemaName, String tableName,
            Long id, Polygon polygon, CoordinateReferenceSystem crs ) throws Exception;

    /**
     * Insert a polygon geometry into the database.
     * 
     * @param session the session to use for the insert or null, in which case a new one is created
     * @param schemaName name of the schema where the table lives in.
     * @param tableName name of the geometry table.
     * @param id the id the geometry is connected to.
     * @param polygon the {@link Polygon} to be inserted.
     * @param epsgCode the EPSG code number.
     * @throws Exception
     */
    public void insertPolygonalGeometry( Session session, String schemaName, String tableName,
            Long id, Polygon polygon, String epsgCode ) throws Exception;

    /**
     * Create a table holding a point geometry and an id.
     * 
     * @param session the db session to use.
     * @param schemaName the name of the schema of the new table.
     * @param tableName the name of the new table.
     * @param fkSchemaName the name of the schema where the referred foreign key table is.
     * @param fkTable the referred foreign table name.
     * @param dimension dimension of the geometry.
     */
    public void createPointGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension );

    /**
     * Create a table holding a line geometry and an id.
     * 
     * @param session the db session to use.
     * @param schemaName the name of the schema of the new table.
     * @param tableName the name of the new table.
     * @param fkSchemaName the name of the schema where the referred foreign key table is.
     * @param fkTable the referred foreign table name.
     * @param dimension dimension of the geometry.
     */
    public void createLineGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension );

    /**
     * Create a table holding a polygon geometry and an id.
     * 
     * @param session the db session to use.
     * @param schemaName the name of the schema of the new table.
     * @param tableName the name of the new table.
     * @param fkSchemaName the name of the schema where the referred foreign key table is.
     * @param fkTable the referred foreign table name.
     * @param dimension dimension of the geometry.
     */
    public void createPolygonGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension );

}