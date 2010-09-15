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
package eu.hydrologis.edc.databases.h2;

import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.THE_GEOM;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.geotools.geometry.jts.JTS;
import org.geotools.metadata.iso.citation.Citations;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hibernate.SQLQuery;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.InputStreamInStream;
import com.vividsolutions.jts.io.WKBReader;

import eu.hydrologis.edc.databases.QueryHandler;

/**
 * Class that handles particular queries.
 * 
 * <p>
 * This is for example the case of spatial queries, 
 * until the H2 database is not better dealable throught datastores
 * of geotools.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class H2QueryHandler implements QueryHandler {

    private final H2SessionFactory h2SessionFactory;
    private CoordinateReferenceSystem wgs84CRS;

    public H2QueryHandler( H2SessionFactory h2SessionFactory ) {
        this.h2SessionFactory = h2SessionFactory;
        wgs84CRS = DefaultGeographicCRS.WGS84;
    }

    public Map<Long, Geometry> getGeometries( String schemaName, String tableName, String epsg,
            Long... ids ) throws Exception {

        Map<Long, Geometry> geometryMap = new HashMap<Long, Geometry>();

        // /*
        // * filter for the ids
        // */
        // FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(GeoTools
        // .getDefaultHints());
        // Set<FeatureId> fids = new HashSet<FeatureId>();
        // for( Long id : ids ) {
        // FeatureId fid = filterFactory.featureId(String.valueOf(id));
        // fids.add(fid);
        // }
        // Filter filter = filterFactory.id(fids);
        //
        // /*
        // * reproject
        // */
        // MathTransform transform = null;
        // if (epsg != null) {
        // CoordinateReferenceSystem sourceCRS = DefaultGeographicCRS.WGS84;
        // CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:" + epsg);
        // transform = CRS.findMathTransform(sourceCRS, targetCRS);
        // }
        //
        // DataStore spatialDataStore = h2SessionFactory.getSpatialDataStore();
        // SimpleFeatureSource featureSource = spatialDataStore
        // .getFeatureSource(tableName);
        // SimpleFeatureCollection featureCollection = featureSource
        // .getFeatures(filter);
        // FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        //
        // while( featureIterator.hasNext() ) {
        // SimpleFeature feature = featureIterator.next();
        //
        // Geometry geometry = (Geometry) feature.getDefaultGeometry();
        // if (transform != null) {
        // geometry = JTS.transform(geometry, transform);
        // }
        // String attribute = feature.getID();
        // attribute = attribute.split("\\.")[1].trim();
        // geometryMap.put(new Long(attribute), geometry);
        // }
        //
        // return geometryMap;

        Session session = h2SessionFactory.openSession();
        try {
            for( Long id : ids ) {
                SQLQuery sqlQuery = session.createSQLQuery("SELECT GeomFromWKB(t.\""
                        + THE_GEOM.toUpperCase() + "\") from \"" + schemaName.toUpperCase()
                        + "\".\"" + tableName.toUpperCase() + "\" t where t." + ID + "=" + id);
                Object result = sqlQuery.uniqueResult();
                Geometry geometry = fromWKB((byte[]) result);

                if (epsg != null) {
                    CoordinateReferenceSystem sourceCRS = DefaultGeographicCRS.WGS84;
                    CoordinateReferenceSystem targetCRS = CRS.decode(epsg);
                    MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
                    if (!transform.isIdentity()) {
                        geometry = JTS.transform(geometry, transform);
                    }
                }
                geometryMap.put(id, geometry);
            }

            return geometryMap;
        } finally {
            session.close();
        }
    }

    public Map<Long, Geometry> getGeometries3D( String schemaName, String tableName, String epsg,
            Long... ids ) throws Exception {
        return getGeometries(schemaName, tableName, epsg, ids);
    }

    public void insertPointGeometry( Session session, String schemaName, String tableName, Long id,
            Coordinate pointCoordinate, CoordinateReferenceSystem crs ) throws Exception {
        String epsg = CRS.lookupIdentifier(Citations.EPSG, crs, true);
        insertPointGeometry(session, schemaName, tableName, id, pointCoordinate, epsg);
    }

    public void insertPointGeometry( Session session, String schemaName, String tableName, Long id,
            Coordinate pointCoordinate, String epsgCode ) throws Exception {
        boolean doClose = false;
        if (session == null) {
            session = h2SessionFactory.openSession();
            doClose = true;
        }
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(epsgCode);

            MathTransform transform = CRS.findMathTransform(sourceCRS, wgs84CRS);
            Coordinate targetCoordinate = JTS.transform(pointCoordinate, null, transform);

            SQLQuery sqlQuery = session.createSQLQuery("INSERT INTO " + schemaName + "."
                    + tableName + " (" + THE_GEOM + ", " + ID + ") VALUES (GeomFromText('POINT("
                    + targetCoordinate.x + " " + targetCoordinate.y + ")', 4326), " + id + ")");
            sqlQuery.executeUpdate();
        } finally {
            if (doClose)
                session.close();
        }

        // Point point = gF.createPoint(pointCoordinate);
        // DefaultTransaction transaction = null;
        // try {
        // CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:" + epsgCode);
        // MathTransform transform = CRS.findMathTransform(sourceCRS, wgs84CRS);
        // Geometry reporjectedPoint = JTS.transform(point, transform);
        //
        // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        // b.setName(tableName);
        // b.setCRS(wgs84CRS);
        // b.add(THE_GEOM, Point.class);
        // b.add(ID, Long.class);
        // SimpleFeatureType type = b.buildFeatureType();
        // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        // Object[] values = new Object[]{reporjectedPoint, id};
        // builder.addAll(values);
        // SimpleFeature feature = builder.buildFeature(tableName + "." + id);
        // SimpleFeatureCollection newCollection = FeatureCollections
        // .newCollection();
        // newCollection.add(feature);
        //
        // transaction = new DefaultTransaction();
        // DataStore spatialDataStore = h2SessionFactory.getSpatialDataStore();
        // FeatureStore<SimpleFeatureType, SimpleFeature> featureStore =
        // (FeatureStore<SimpleFeatureType, SimpleFeature>) spatialDataStore
        // .getFeatureSource(tableName);
        // featureStore.setTransaction(transaction);
        //
        // featureStore.addFeatures(newCollection);
        // transaction.commit();
        // } catch (Exception e) {
        // e.printStackTrace();
        // if (transaction != null)
        // transaction.rollback();
        // } finally {
        // if (transaction != null)
        // transaction.close();
        // }
    }

    public void insertLinestringGeometry( Session session, String schemaName, String tableName,
            Long id, LineString lineString, CoordinateReferenceSystem crs ) throws Exception {
        String epsg = CRS.lookupIdentifier(Citations.EPSG, crs, true);
        insertLinestringGeometry(session, schemaName, tableName, id, lineString, epsg);
    }

    public void insertLinestringGeometry( Session session, String schemaName, String tableName,
            Long id, LineString lineString, String epsgCode ) throws Exception {
        boolean doClose = false;
        if (session == null) {
            session = h2SessionFactory.openSession();
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
                if (i < coordinates.length - 1) {
                    sB.append(",");
                }
            }

            SQLQuery sqlQuery = session.createSQLQuery("INSERT INTO " + schemaName + "."
                    + tableName + " (" + THE_GEOM + ", " + ID
                    + ") VALUES (GeomFromText('LINESTRING(" + sB.toString() + ")',4326), " + id
                    + ")");
            sqlQuery.executeUpdate();
        } finally {
            if (doClose)
                session.close();
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
            session = h2SessionFactory.openSession();
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

            SQLQuery sqlQuery = session.createSQLQuery("INSERT INTO " + schemaName + "."
                    + tableName + " (" + THE_GEOM + ", " + ID
                    + ") VALUES (GeomFromText('LINESTRING(" + sB.toString() + ")',4326), " + id
                    + ")");
            sqlQuery.executeUpdate();
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
        boolean doClose = false;
        if (session == null) {
            session = h2SessionFactory.openSession();
            doClose = true;
        }
        try {
            CoordinateReferenceSystem sourceCRS = CRS.decode(epsgCode);

            MathTransform transform = CRS.findMathTransform(sourceCRS, wgs84CRS);
            Geometry reporjectedPolygon = JTS.transform(polygon, transform);

            Coordinate[] coordinates = reporjectedPolygon.getCoordinates();
            StringBuilder sB = new StringBuilder();
            for( int i = 0; i < coordinates.length; i++ ) {
                Coordinate coordinate = coordinates[i];
                sB.append(coordinate.x);
                sB.append(" ");
                sB.append(coordinate.y);
                if (i < coordinates.length - 1) {
                    sB.append(",");
                }
            }

            Transaction transaction = session.beginTransaction();
            SQLQuery sqlQuery = session.createSQLQuery("INSERT INTO " + schemaName + "."
                    + tableName + " (" + THE_GEOM + ", " + ID + ") VALUES (GeomFromText('POLYGON(("
                    + sB.toString() + "))', 4326), " + id + ")");
            sqlQuery.executeUpdate();
            transaction.commit();
        } finally {
            if (doClose)
                session.close();
        }
    }

    private Geometry fromWKB( byte[] wkb ) throws Exception {
        ByteArrayInputStream bytes = new ByteArrayInputStream(wkb, 0, wkb.length - 4);
        // read the geometry
        Geometry g = new WKBReader().read(new InputStreamInStream(bytes));
        // read the srid
        int srid = 0;
        srid |= wkb[wkb.length - 4] & 0xFF;
        srid <<= 8;
        srid |= wkb[wkb.length - 3] & 0xFF;
        srid <<= 8;
        srid |= wkb[wkb.length - 2] & 0xFF;
        srid <<= 8;
        srid |= wkb[wkb.length - 1] & 0xFF;
        g.setSRID(srid);
        return g;
    }

    public void createPointGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension ) {
        Transaction transaction = session.beginTransaction();

        SQLQuery sqlQuery = session.createSQLQuery("drop table " + schemaName + "." + tableName
                + " if exists");
        sqlQuery.executeUpdate();
        sqlQuery = session.createSQLQuery("CREATE TABLE " + schemaName + "." + tableName + " ( "
                + THE_GEOM + " BLOB COMMENT 'POINT', " + ID + " BIGINT NOT NULL)");
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

        transaction.commit();
    }

    public void createLineGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension ) {
        Transaction transaction = session.beginTransaction();

        SQLQuery sqlQuery = session.createSQLQuery("drop table " + schemaName + "." + tableName
                + " if exists");
        sqlQuery.executeUpdate();
        sqlQuery = session.createSQLQuery("CREATE TABLE " + schemaName + "." + tableName + " ( "
                + THE_GEOM + " BLOB COMMENT 'LINESTRING', " + ID + " BIGINT NOT NULL)");
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

        transaction.commit();
    }

    public void createPolygonGeometryTable( Session session, String schemaName, String tableName,
            String fkSchemaName, String fkTable, int dimension ) {
        Transaction transaction = session.beginTransaction();

        SQLQuery sqlQuery = session.createSQLQuery("drop table " + schemaName + "." + tableName
                + " if exists");
        sqlQuery.executeUpdate();
        sqlQuery = session.createSQLQuery("CREATE TABLE " + schemaName + "." + tableName + " ( "
                + THE_GEOM + " BLOB COMMENT 'MULTIPOLYGON', " + ID + " BIGINT NOT NULL)");
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

        transaction.commit();
    }

}
