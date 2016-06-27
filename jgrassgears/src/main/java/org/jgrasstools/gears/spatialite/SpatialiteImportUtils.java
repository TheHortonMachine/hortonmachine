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
package org.jgrasstools.gears.spatialite;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Import utilities.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteImportUtils {

    /**
     * Create a spatial table using a shapefile as schema.
     * 
     * @param db the database to use.
     * @param shapeFile the shapefile to use.
     * @return the name of the created table.
     * @throws Exception
     */
    public static String createTableFromShp( SpatialiteDb db, File shapeFile ) throws Exception {
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureType schema = featureSource.getSchema();
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();

        String shpName = FileUtilities.getNameWithoutExtention(shapeFile);

        List<String> attrSql = new ArrayList<String>();
        List<AttributeDescriptor> attributeDescriptors = schema.getAttributeDescriptors();
        for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
            if (attributeDescriptor instanceof GeometryDescriptor) {
                continue;
            }
            String attrName = attributeDescriptor.getLocalName();
            Class< ? > binding = attributeDescriptor.getType().getBinding();
            if (binding.isAssignableFrom(Double.class) || binding.isAssignableFrom(Float.class)) {
                attrSql.add(attrName + " REAL");
            } else if (binding.isAssignableFrom(Long.class) || binding.isAssignableFrom(Integer.class)) {
                attrSql.add(attrName + " INTEGER");
            } else if (binding.isAssignableFrom(String.class)) {
                attrSql.add(attrName + " TEXT");
            } else {
                attrSql.add(attrName + " TEXT");
            }
        }

        db.createTable(shpName, attrSql.toArray(new String[0]));

        String typeString = null;
        org.opengis.feature.type.GeometryType type = geometryDescriptor.getType();
        Class< ? > binding = type.getBinding();
        if (binding.isAssignableFrom(MultiPolygon.class)) {
            typeString = "MULTIPOLYGON";
        } else if (binding.isAssignableFrom(Polygon.class)) {
            typeString = "POLYGON";
        } else if (binding.isAssignableFrom(MultiLineString.class)) {
            typeString = "MULTILINESTRING";
        } else if (binding.isAssignableFrom(LineString.class)) {
            typeString = "LINESTRING";
        } else if (binding.isAssignableFrom(MultiPoint.class)) {
            typeString = "MULTIPOINT";
        } else if (binding.isAssignableFrom(Point.class)) {
            typeString = "POINT";
        }
        if (typeString != null) {
            String codeFromCrs = CrsUtilities.getCodeFromCrs(schema.getCoordinateReferenceSystem());
            if (codeFromCrs == null || codeFromCrs.toLowerCase().contains("null")) {
                codeFromCrs = "4326"; // fallback on 4326
            }
            codeFromCrs = codeFromCrs.replaceFirst("EPSG:", "");
            db.addGeometryXYColumnAndIndex(shpName, null, typeString, codeFromCrs);
        }

        return shpName;
    }

    /**
     * Import a shapefile into a table.
     * 
     * @param db the database to use.
     * @param shapeFile the shapefile to import.
     * @param tableName the name of the table to import to.
     * @param limit if > 0, a limit to the imported features is applied.
     * @throws Exception
     */
    public static void importShapefile( SpatialiteDb db, File shapeFile, String tableName, int limit ) throws Exception {
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureType schema = featureSource.getSchema();
        List<AttributeDescriptor> attributeDescriptors = schema.getAttributeDescriptors();

        SimpleFeatureCollection features = featureSource.getFeatures();

        List<String[]> tableInfo = db.getTableColumns(tableName);
        List<String> tableColumns = new ArrayList<>();
        for( String[] item : tableInfo ) {
            tableColumns.add(item[0]);
        }
        SpatialiteGeometryColumns geometryColumns = db.getGeometryColumnsForTable(tableName);
        String gCol = geometryColumns.f_geometry_column;

        int epsg = geometryColumns.srid;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:" + epsg);
        ReprojectingFeatureCollection repFeatures = new ReprojectingFeatureCollection(features, crs);
        SimpleFeatureIterator featureIterator = repFeatures.features();

        String valueNames = "";
        String qMarks = "";
        for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
            String attrName = attributeDescriptor.getLocalName();
            if (attrName.equals(SpatialiteDb.PK_UID)) {
                continue;
            }
            if (attributeDescriptor instanceof GeometryDescriptor) {
                valueNames += "," + gCol;
                qMarks += ",GeomFromText(?, " + epsg + ")";
            } else {
                if (!tableColumns.contains(attrName)) {
                    throw new IllegalArgumentException("The imported shapefile doesn't seem to match the table's schema.");
                }
                valueNames += "," + attrName;
                qMarks += ",?";
            }
        }
        valueNames = valueNames.substring(1);
        qMarks = qMarks.substring(1);
        String sql = "INSERT INTO " + tableName + " (" + valueNames + ") VALUES (" + qMarks + ")";

        Connection conn = db.getConnection();
        try (PreparedStatement pStmt = conn.prepareStatement(sql)) {
            int count = 0;
            while( featureIterator.hasNext() ) {
                SimpleFeature f = (SimpleFeature) featureIterator.next();
                List<Object> attributes = f.getAttributes();
                for( int i = 0; i < attributes.size(); i++ ) {
                    Object object = attributes.get(i);
                    if (object == null) {
                        continue;
                    }
                    int iPlus = i + 1;
                    if (object instanceof Double) {
                        pStmt.setDouble(iPlus, (Double) object);
                    } else if (object instanceof Float) {
                        pStmt.setFloat(iPlus, (Float) object);
                    } else if (object instanceof Integer) {
                        pStmt.setInt(iPlus, (Integer) object);
                    } else if (object instanceof String) {
                        pStmt.setString(iPlus, (String) object);
                    } else if (object instanceof Geometry) {
                        pStmt.setString(iPlus, ((Geometry) object).toText());
                    } else {
                        pStmt.setString(iPlus, object.toString());
                    }
                }
                pStmt.executeUpdate();

                count++;
                if (limit > 0 && count >= limit) {
                    break;
                }
            }
            featureIterator.close();
        }
    }

    /**
     * get a table as featurecollection.
     * 
     * @param db the database.
     * @param tableName the table to use.
     * @param featureLimit limit in feature or -1.
     * @param forceSrid a srid to force to or -1.
     * @return the extracted featurecollection.
     * @throws SQLException
     * @throws Exception
     */
    public static DefaultFeatureCollection tableToFeatureFCollection( SpatialiteDb db, String tableName, int featureLimit,
            int forceSrid ) throws SQLException, Exception {
        DefaultFeatureCollection fc = new DefaultFeatureCollection();

        SpatialiteGeometryColumns geometryColumn = db.getGeometryColumnsForTable(tableName);
        CoordinateReferenceSystem crs;
        if (forceSrid == -1) {
            forceSrid = geometryColumn.srid;
        }
        if (forceSrid == 4326) {
            crs = DefaultGeographicCRS.WGS84;
        } else {
            crs = CRS.decode("EPSG:" + geometryColumn.srid);
        }
        QueryResult tableRecords = db.getTableRecordsMapIn(tableName, null, false, featureLimit, forceSrid);
        int geometryIndex = tableRecords.geometryIndex;
        if (geometryIndex == -1) {
            throw new IllegalArgumentException("Not a geometric layer.");
        }
        List<String> names = tableRecords.names;
        List<String> types = tableRecords.types;

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(tableName);
        b.setCRS(crs);

        for( int i = 0; i < names.size(); i++ ) {
            if (i == geometryIndex) {
                SpatialiteGeometryType geomType = SpatialiteGeometryType.forValue(geometryColumn.geometry_type);
                Class< ? > geometryClass = geomType.getGeometryClass();
                b.add(geometryColumn.f_geometry_column, geometryClass);
                continue;
            }
            Class< ? > fieldClass = null;
            String typeStr = types.get(i);
            switch( typeStr ) {
            case "DOUBLE":
            case "REAL":
                fieldClass = Double.class;
                break;
            case "FLOAT":
                fieldClass = Float.class;
                break;
            case "INTEGER":
                fieldClass = Integer.class;
                break;
            case "TEXT":
                fieldClass = String.class;
                break;
            default:
                fieldClass = String.class;
                break;
            }
            b.add(names.get(i), fieldClass);
        }
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        int count = tableRecords.data.size();
        for( int i = 0; i < count; i++ ) {
            Object[] objects = tableRecords.data.get(i);

            builder.addAll(objects);
            SimpleFeature feature = builder.buildFeature(null);

            fc.add(feature);
        }
        return fc;
    }
}
