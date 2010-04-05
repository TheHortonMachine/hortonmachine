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
package eu.hydrologis.edc.utils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

import eu.hydrologis.edc.databases.EdcSessionFactory;
import eu.hydrologis.edc.databases.QueryHandler;

/**
 * Load geometries into the database.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeometryLoader {

    private final EdcSessionFactory edcSessionFactory;

    public GeometryLoader( EdcSessionFactory edcSessionFactory ) {
        this.edcSessionFactory = edcSessionFactory;
    }

    /**
     * Inserts point geometries and ids from a shapefile into a given schema.table.
     * 
     * @param shapeFile the file to load.
     * @param schemaName the name of the schema.
     * @param tableName the name of the table to load the file to.
     * @throws Exception
     */
    public void fromPointShapefile( File shapeFile, String schemaName, String tableName )
            throws Exception {

        Session session = edcSessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        QueryHandler queryHandler = edcSessionFactory.getQueryHandler();
        try {

            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = getFeatureCollectionFromShapefile(shapeFile);
            FeatureIterator<SimpleFeature> iterator = collection.features();

            CoordinateReferenceSystem crs = null;
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                if (crs == null) {
                    crs = feature.getFeatureType().getCoordinateReferenceSystem();
                }
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCoordinate();
                Object attribute = feature.getAttribute(Constants.ID);
                if (attribute == null) {
                    throw new IOException("Could not find attribute: " + Constants.ID);
                }
                long id = ((Number) attribute).longValue();
                queryHandler.insertPointGeometry(session, schemaName, tableName, id, coordinate,
                        crs);
            }

            collection.close(iterator);

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
        } finally {
            session.close();
        }
    }

    /**
     * Inserts line geometries and ids from a shapefile into a given schema.table.
     * 
     * @param shapeFile the file to load.
     * @param schemaName the name of the schema.
     * @param tableName the name of the table to load the file to.
     * @throws Exception
     */
    public void fromLinestringShapefile( File shapeFile, String schemaName, String tableName )
            throws Exception {

        Session session = edcSessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        QueryHandler queryHandler = edcSessionFactory.getQueryHandler();
        try {

            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = getFeatureCollectionFromShapefile(shapeFile);
            FeatureIterator<SimpleFeature> iterator = collection.features();

            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
            CoordinateReferenceSystem crs = null;
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                if (crs == null) {
                    crs = feature.getFeatureType().getCoordinateReferenceSystem();
                }
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate[] coordinates = geometry.getCoordinates();
                LineString lineString = geometryFactory.createLineString(coordinates);

                // ID
                Object idAttribute = feature.getAttribute(Constants.ID);
                if (idAttribute == null) {
                    throw new IOException("Could not find attribute: " + Constants.ID);
                }
                String id = idAttribute.toString();

                // elevation?
                Object zAttribute = feature.getAttribute(Constants.Z);
                if (zAttribute instanceof Double) {
                    Double elevation = (Double) zAttribute;
                    for( Coordinate coordinate : coordinates ) {
                        coordinate.z = elevation;
                    }
                    geometry.geometryChanged();
                }
                if (zAttribute != null) {
                    queryHandler.insertLinestringGeometry3D(session, schemaName, tableName, Long
                            .parseLong(id), lineString, crs);
                } else {
                    queryHandler.insertLinestringGeometry(session, schemaName, tableName, Long
                            .parseLong(id), lineString, crs);
                }
            }

            collection.close(iterator);

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
        } finally {
            session.close();
        }
    }

    /**
     * Inserts polygon geometries and ids from a shapefile into a given schema.table.
     * 
     * @param shapeFile the file to load.
     * @param schemaName the name of the schema.
     * @param tableName the name of the table to load the file to.
     * @throws Exception
     */
    public void fromPolygonShapefile( File shapeFile, String schemaName, String tableName )
            throws Exception {

        Session session = edcSessionFactory.openSession();
        Transaction transaction = session.beginTransaction();
        QueryHandler queryHandler = edcSessionFactory.getQueryHandler();
        try {

            FeatureCollection<SimpleFeatureType, SimpleFeature> collection = getFeatureCollectionFromShapefile(shapeFile);
            FeatureIterator<SimpleFeature> iterator = collection.features();

            GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory(null);
            CoordinateReferenceSystem crs = null;
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                if (crs == null) {
                    crs = feature.getFeatureType().getCoordinateReferenceSystem();
                }
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate[] coordinates = geometry.getCoordinates();
                LinearRing linearRing = geometryFactory.createLinearRing(coordinates);
                Polygon polygon = geometryFactory.createPolygon(linearRing, null);
                Object attribute = feature.getAttribute(Constants.ID);
                if (attribute == null) {
                    throw new IOException("Could not find attribute: " + Constants.ID);
                }
                String id = attribute.toString();
                queryHandler.insertPolygonalGeometry(session, schemaName, tableName, Long
                        .parseLong(id), polygon, crs);
            }

            collection.close(iterator);

            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
        } finally {
            session.close();
        }
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollectionFromShapefile(
            File shapeFile ) throws MalformedURLException, IOException {
        Map<String, Serializable> connectParameters = new HashMap<String, Serializable>();
        connectParameters.put("url", shapeFile.toURI().toURL());
        DataStore dataStore = DataStoreFinder.getDataStore(connectParameters);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = dataStore
                .getFeatureSource(typeName);
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = featureSource
                .getFeatures();
        return collection;
    }
}
