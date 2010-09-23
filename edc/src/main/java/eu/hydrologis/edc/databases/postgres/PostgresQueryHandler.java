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
package eu.hydrologis.edc.databases.postgres;

import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.THE_GEOM;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataStore;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hibernate.SQLQuery;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import eu.hydrologis.edc.databases.DatabaseSessionFactory;
import eu.hydrologis.edc.databases.QueryHandler;

/**
 * Class that handles particular queries.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class PostgresQueryHandler implements QueryHandler {

    private final DatabaseSessionFactory postgresSessionFactory;
    private CoordinateReferenceSystem wgs84CRS;
    private static GeometryFactory gF = new GeometryFactory();

    public PostgresQueryHandler( DatabaseSessionFactory postgresSessionFactory ) {
        this.postgresSessionFactory = postgresSessionFactory;
        wgs84CRS = DefaultGeographicCRS.WGS84;
    }

    public Map<Long, Geometry> getGeometries( String schemaName, String tableName, String epsg,
            Long... ids ) throws Exception {

        Map<Long, Geometry> geometryMap = new HashMap<Long, Geometry>();

        /*
         * filter for the ids
         */
        FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(GeoTools
                .getDefaultHints());
        Set<FeatureId> fids = new HashSet<FeatureId>();
        for( Long id : ids ) {
            FeatureId fid = filterFactory.featureId(String.valueOf(id));
            fids.add(fid);
        }
        Filter filter = filterFactory.id(fids);

        /*
         * reproject
         */
        MathTransform transform = null;
        if (epsg != null) {
            CoordinateReferenceSystem targetCRS = CRS.decode(epsg);
            transform = CRS.findMathTransform(wgs84CRS, targetCRS);
        }

        DataStore spatialDataStore = postgresSessionFactory.getSpatialDataStore();
        SimpleFeatureSource featureSource = spatialDataStore
                .getFeatureSource(tableName);
        SimpleFeatureCollection featureCollection = featureSource
                .getFeatures(filter);
        FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();

        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (transform != null) {
                geometry = JTS.transform(geometry, transform);
            }
            String attribute = feature.getID();
            attribute = attribute.split("\\.")[1].trim();
            geometryMap.put(new Long(attribute), geometry);
        }
        featureCollection.close(featureIterator);

        return geometryMap;
    }

    @SuppressWarnings("unchecked")
    public Map<Long, Geometry> getGeometries3D( String schemaName, String tableName, String epsg,
            Long... ids ) throws Exception {
        Map<Long, Geometry> geometryMap = new HashMap<Long, Geometry>();

        /*
         * reproject
         */
        MathTransform transform = null;
        if (epsg != null) {
            CoordinateReferenceSystem targetCRS = CRS.decode(epsg);
            transform = CRS.findMathTransform(wgs84CRS, targetCRS);
        }

        Session session = postgresSessionFactory.openSession();
        try {
            StringBuilder sB = new StringBuilder();
            for( int i = 0; i < ids.length; i++ ) {
                if (i == 0) {
                    sB.append(ID).append("=").append(ids[i]);
                } else {
                    sB.append(" OR ").append(ID).append("=").append(ids[i]);
                }
            }

            SQLQuery sqlQuery = session.createSQLQuery("SELECT " + ID + ", asewkt(" + THE_GEOM
                    + ") from " + schemaName + "." + tableName + " where " + sB.toString());
            List result = sqlQuery.list();

            for( Object ewkt : result ) {
                Object[] resultArray = (Object[]) ewkt;
                Object id = resultArray[0];
                Object geom = resultArray[1];
                if (id instanceof Number && geom instanceof String) {
                    Number idNUm = (Number) id;
                    String geomString = (String) geom;
                    String[] geomSplit = geomString.split("\\(");
                    String lineStringStr = geomSplit[1].replaceFirst("\\)", "");
                    String[] coordinatesStrings = lineStringStr.split(",");
                    Coordinate[] coordinatesArray = new Coordinate[coordinatesStrings.length];
                    for( int i = 0; i < coordinatesArray.length; i++ ) {
                        String coordinateString = coordinatesStrings[i];
                        String[] coordinatesSplit = coordinateString.split("\\s+");
                        double lon = Double.parseDouble(coordinatesSplit[0]);
                        double lat = Double.parseDouble(coordinatesSplit[1]);
                        double z = Double.parseDouble(coordinatesSplit[2]);
                        Coordinate coord = new Coordinate(lon, lat, z);
                        coordinatesArray[i] = coord;
                    }
                    Geometry lineString = gF.createLineString(coordinatesArray);

                    if (transform != null) {
                        lineString = JTS.transform(lineString, transform);
                    }
                    geometryMap.put(idNUm.longValue(), lineString);
                }
            }
            return geometryMap;
        } finally {
            session.close();
        }

    }

    public void insertPointGeometry( Session session, String schemaName, String tableName, Long id,
            Coordinate pointCoordinate, CoordinateReferenceSystem crs ) throws Exception {
        String epsg = CRS.lookupIdentifier(Citations.EPSG, crs, true);
        insertPointGeometry(session, schemaName, tableName, id, pointCoordinate, epsg);
    }

    public void insertPointGeometry( Session session, String schemaName, String tableName, Long id,
            Coordinate pointCoordinate, String epsgCode ) throws Exception {
        Point point = gF.createPoint(pointCoordinate);
        DefaultTransaction transaction = null;
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(epsgCode);
            MathTransform transform = CRS.findMathTransform(sourceCRS, wgs84CRS);
            Geometry reporjectedPoint = JTS.transform(point, transform);

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName(tableName);
            b.setCRS(wgs84CRS);
            b.add(THE_GEOM, Point.class);
            b.add(ID, Long.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Object[] values = new Object[]{reporjectedPoint, id};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(tableName + "." + id);
            SimpleFeatureCollection newCollection = FeatureCollections
                    .newCollection();
            newCollection.add(feature);

            transaction = new DefaultTransaction();
            DataStore spatialDataStore = postgresSessionFactory.getSpatialDataStore();
            FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) spatialDataStore
                    .getFeatureSource(tableName);
            featureStore.setTransaction(transaction);

            featureStore.addFeatures(newCollection);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (transaction != null)
                transaction.rollback();
            throw new Exception(e);
        } finally {
            if (transaction != null)
                transaction.close();
        }
    }

    public void insertLinestringGeometry( Session session, String schemaName, String tableName,
            Long id, LineString lineString, CoordinateReferenceSystem crs ) throws Exception {
        String epsg = CRS.lookupIdentifier(Citations.EPSG, crs, true);
        insertLinestringGeometry(session, schemaName, tableName, id, lineString, epsg);
    }

    public void insertLinestringGeometry( Session session, String schemaName, String tableName,
            Long id, LineString lineString, String epsgCode ) throws Exception {
        DefaultTransaction transaction = null;
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(epsgCode);
            MathTransform transform = CRS.findMathTransform(sourceCRS, wgs84CRS);
            Geometry reporjectedLinestring = JTS.transform(lineString, transform);

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName(tableName);
            // add a geometry property
            b.setCRS(wgs84CRS);
            b.add(THE_GEOM, LineString.class);
            b.add(ID, Long.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Object[] values = new Object[]{reporjectedLinestring, id};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(tableName + "." + id);
            SimpleFeatureCollection newCollection = FeatureCollections
                    .newCollection();
            newCollection.add(feature);

            transaction = new DefaultTransaction();
            DataStore spatialDataStore = postgresSessionFactory.getSpatialDataStore();
            FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) spatialDataStore
                    .getFeatureSource(tableName);
            featureStore.setTransaction(transaction);

            featureStore.addFeatures(newCollection);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (transaction != null)
                transaction.rollback();
            throw new Exception(e);
        } finally {
            if (transaction != null)
                transaction.close();
        }
    }

    public void insertLinestringGeometry3D( Session session, String schemaName, String tableName,
            Long id, LineString lineString, CoordinateReferenceSystem crs ) throws Exception {
        String epsg = CRS.lookupIdentifier(Citations.EPSG, crs, true);
        insertLinestringGeometry3D(session, schemaName, tableName, id, lineString, epsg);
    }

    public void insertLinestringGeometry3D( Session session, String schemaName, String tableName,
            Long id, LineString lineString, String epsgCode ) throws Exception {
        boolean doClose = false;
        if (session == null) {
            session = postgresSessionFactory.openSession();
            doClose = true;
        }
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(epsgCode);

            MathTransform transform = CRS.findMathTransform(sourceCRS, wgs84CRS);
            Geometry reporjectedLineString = JTS.transform(lineString, transform);

            Coordinate[] coordinates = reporjectedLineString.getCoordinates();
            StringBuilder sB = new StringBuilder();
            for( int i = 0; i < coordinates.length; i++ ) {
                Coordinate coordinate = coordinates[i];
                sB.append(coordinate.x);
                sB.append(" ");
                sB.append(coordinate.y);
                sB.append(" ");
                sB.append(coordinate.z);
                if (i < coordinates.length - 1) {
                    sB.append(",");
                }
            }

            Transaction transaction = session.beginTransaction();
            SQLQuery sqlQuery = session.createSQLQuery("INSERT INTO " + schemaName + "."
                    + tableName + " (" + THE_GEOM + ", " + ID
                    + ") VALUES (GeomFromText('LINESTRING(" + sB.toString() + ")',4326), " + id
                    + ")");
            sqlQuery.executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            throw new Exception(e);
        } finally {
            if (doClose)
                session.close();
        }
    }

    public void insertPolygonalGeometry( Session session, String schemaName, String tableName,
            Long id, Polygon polygon, CoordinateReferenceSystem crs ) throws Exception {
        String epsg = CRS.lookupIdentifier(Citations.EPSG, crs, true);
        insertPolygonalGeometry(session, schemaName, tableName, id, polygon, epsg);
    }

    public void insertPolygonalGeometry( Session session, String schemaName, String tableName,
            Long id, Polygon polygon, String epsgCode ) throws Exception {

        MultiPolygon multiPolygon = gF.createMultiPolygon(new Polygon[]{polygon});
        DefaultTransaction transaction = null;
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(epsgCode);
            MathTransform transform = CRS.findMathTransform(sourceCRS, wgs84CRS);
            Geometry reporjectedPolygon = JTS.transform(multiPolygon, transform);

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName(tableName);
            // add a geometry property
            b.setCRS(wgs84CRS);
            b.add(THE_GEOM, MultiPolygon.class);
            b.add(ID, Long.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Object[] values = new Object[]{reporjectedPolygon, id};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(tableName + "." + id);
            SimpleFeatureCollection newCollection = FeatureCollections
                    .newCollection();
            newCollection.add(feature);

            transaction = new DefaultTransaction();
            DataStore spatialDataStore = postgresSessionFactory.getSpatialDataStore();
            FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) spatialDataStore
                    .getFeatureSource(tableName);
            featureStore.setTransaction(transaction);

            featureStore.addFeatures(newCollection);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (transaction != null)
                transaction.rollback();
            throw new Exception(e);
        } finally {
            if (transaction != null)
                transaction.close();
        }
    }

    public void createPointGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension ) {
        Transaction transaction = session.beginTransaction();

        SQLQuery sqlQuery = session.createSQLQuery("drop table if exists " + schemaName + "."
                + tableName);
        sqlQuery.executeUpdate();

        String createTableQuery = "CREATE TABLE " + schemaName + "." + tableName + " (" + ID
                + " INTEGER )";
        sqlQuery = session.createSQLQuery(createTableQuery);
        sqlQuery.executeUpdate();

        String addPkQuery = "ALTER TABLE " + schemaName + "." + tableName + " ADD PRIMARY KEY ("
                + ID + ")";
        sqlQuery = session.createSQLQuery(addPkQuery);
        sqlQuery.executeUpdate();

        String addFkQuery = "ALTER TABLE " + schemaName + "." + tableName + " ADD CONSTRAINT "
                + tableName + "_" + fkTable + "_id FOREIGN KEY (" + ID + ") REFERENCES "
                + fkSchemaName + "." + fkTable + " (" + ID + ")";
        sqlQuery = session.createSQLQuery(addFkQuery);
        sqlQuery.executeUpdate();

        String addGeomatryQuery = "SELECT AddGeometryColumn('" + schemaName + "', '" + tableName
                + "', '" + THE_GEOM + "', 4326, 'POINT', " + dimension + ")";
        sqlQuery = session.createSQLQuery(addGeomatryQuery);
        sqlQuery.uniqueResult();

        transaction.commit();
    }

    public void createLineGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension ) {
        Transaction transaction = session.beginTransaction();

        SQLQuery sqlQuery = session.createSQLQuery("drop table if exists " + schemaName + "."
                + tableName);
        sqlQuery.executeUpdate();

        String createTableQuery = "CREATE TABLE " + schemaName + "." + tableName + " (" + ID
                + " INTEGER )";
        sqlQuery = session.createSQLQuery(createTableQuery);
        sqlQuery.executeUpdate();

        String addPkQuery = "ALTER TABLE " + schemaName + "." + tableName + " ADD PRIMARY KEY ("
                + ID + ")";
        sqlQuery = session.createSQLQuery(addPkQuery);
        sqlQuery.executeUpdate();

        String addFkQuery = "ALTER TABLE " + schemaName + "." + tableName + " ADD CONSTRAINT "
                + tableName + "_" + fkTable + "_id FOREIGN KEY (" + ID + ") REFERENCES "
                + fkSchemaName + "." + fkTable + " (" + ID + ")";
        sqlQuery = session.createSQLQuery(addFkQuery);
        sqlQuery.executeUpdate();

        String addGeomatryQuery = "SELECT AddGeometryColumn('" + schemaName + "', '" + tableName
                + "', '" + THE_GEOM + "', 4326, 'LINESTRING', " + dimension + ")";
        sqlQuery = session.createSQLQuery(addGeomatryQuery);
        sqlQuery.uniqueResult();

        transaction.commit();
    }

    public void createPolygonGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension ) {
        Transaction transaction = session.beginTransaction();

        SQLQuery sqlQuery = session.createSQLQuery("drop table if exists " + schemaName + "."
                + tableName);
        sqlQuery.executeUpdate();

        String createTableQuery = "CREATE TABLE " + schemaName + "." + tableName + " (" + ID
                + " INTEGER )";
        sqlQuery = session.createSQLQuery(createTableQuery);
        sqlQuery.executeUpdate();

        String addPkQuery = "ALTER TABLE " + schemaName + "." + tableName + " ADD PRIMARY KEY ("
                + ID + ")";
        sqlQuery = session.createSQLQuery(addPkQuery);
        sqlQuery.executeUpdate();

        String addFkQuery = "ALTER TABLE " + schemaName + "." + tableName + " ADD CONSTRAINT "
                + tableName + "_" + fkTable + "_id FOREIGN KEY (" + ID + ") REFERENCES "
                + fkSchemaName + "." + fkTable + " (" + ID + ")";
        sqlQuery = session.createSQLQuery(addFkQuery);
        sqlQuery.executeUpdate();

        String addGeomatryQuery = "SELECT AddGeometryColumn('" + schemaName + "', '" + tableName
                + "', '" + THE_GEOM + "', 4326, 'MULTIPOLYGON', " + dimension + ")";
        sqlQuery = session.createSQLQuery(addGeomatryQuery);
        sqlQuery.uniqueResult();

        transaction.commit();
    }

}
