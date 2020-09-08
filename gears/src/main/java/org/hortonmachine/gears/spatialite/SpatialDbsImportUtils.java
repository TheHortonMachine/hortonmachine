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
package org.hortonmachine.gears.spatialite;

import java.io.File;
import java.sql.Clob;
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
import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.h2gis.H2GisDb;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.postgis.PostgisDb;
import org.hortonmachine.dbs.spatialite.hm.SpatialiteDb;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Import utilities.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialDbsImportUtils {
    private static final String FID = "fid";

    private static final String PK_UID = "PK_UID";

    private static final Logger logger = Logger.INSTANCE;

    public static final String GEOMFIELD_FOR_SHAPEFILE = ASpatialDb.DEFAULT_GEOM_FIELD_NAME;

    /**
     * Create a spatial table using a shapefile as schema.
     * 
     * @param db the database to use.
     * @param shapeFile the shapefile to use.
     * @param newTableName the new name of the table. If null, the shp name is used.
     * @param forceSrid an optional srid to force the table to.
     * @return the name of the created table.
     * @param avoidSpatialIndex if <code>true</code>, no spatial index will be created. This is useful if many records 
     *          have to be inserted and the index will be created later manually.
     * @return the name of the created table.
     * @throws Exception
     */
    public static String createTableFromShp( ASpatialDb db, File shapeFile, String newTableName, String forceSrid,
            boolean avoidSpatialIndex ) throws Exception {
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureType schema = featureSource.getSchema();
        if (newTableName == null) {
            newTableName = FileUtilities.getNameWithoutExtention(shapeFile);
        }
        return createTableFromSchema(db, schema, newTableName, forceSrid, avoidSpatialIndex);
    }

    /**
     * Create a spatial table using a schema.
     * 
     * @param db the database to use.
     * @param schema the schema to use.
     * @param newTableName the new name of the table. If null, the shp name is used.
     * @param forceSrid an optional srid to force the table to.
     * @return the name of the created table.
     * @param avoidSpatialIndex if <code>true</code>, no spatial index will be created. This is useful if many records 
     *          have to be inserted and the index will be created later manually.
     * @throws Exception
     */
    public static String createTableFromSchema( ASpatialDb db, SimpleFeatureType schema, String newTableName, String forceSrid,
            boolean avoidSpatialIndex ) throws Exception {
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();

        ADatabaseSyntaxHelper dsh = db.getType().getDatabaseSyntaxHelper();

        List<String> attrSql = new ArrayList<String>();
        attrSql.add(FID + " " + dsh.LONG_PRIMARYKEY_AUTOINCREMENT());

        String fidField = FeatureUtilities.findAttributeName(schema, FID);
        List<AttributeDescriptor> attributeDescriptors = schema.getAttributeDescriptors();
        for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
            String attrName = attributeDescriptor.getLocalName();

            if (fidField != null && fidField.equals(attrName)) {
                continue;
            } else if (attributeDescriptor instanceof GeometryDescriptor) {
                continue;
            } else if (attrName.equalsIgnoreCase(PK_UID)) {
                continue;
            }

            if (DbsUtilities.isReservedName(attrName)) {
                attrName = DbsUtilities.fixReservedNameForQuery(attrName);
            }
            Class< ? > binding = attributeDescriptor.getType().getBinding();
            if (binding.isAssignableFrom(Double.class) || binding.isAssignableFrom(Float.class)) {
                attrSql.add(attrName + " " + dsh.REAL());
            } else if (binding.isAssignableFrom(Long.class) || binding.isAssignableFrom(Integer.class)) {
                attrSql.add(attrName + " " + dsh.INTEGER());
            } else if (binding.isAssignableFrom(String.class)) {
                attrSql.add(attrName + " " + dsh.TEXT());
            } else {
                attrSql.add(attrName + " " + dsh.TEXT());
            }
        }

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
            String sridString = forceSrid; // forced srid rules, if available
            if (sridString == null) {
                String codeFromCrs = CrsUtilities.getCodeFromCrs(schema.getCoordinateReferenceSystem());
                if (codeFromCrs == null || codeFromCrs.toLowerCase().contains("null")) {
                    codeFromCrs = "4326"; // fallback on 4326
                }
                sridString = codeFromCrs.replaceFirst("EPSG:", "");
            }
            if (db instanceof SpatialiteDb) {
                SpatialiteDb spatialiteDb = (SpatialiteDb) db;
                spatialiteDb.createTable(newTableName, attrSql.toArray(new String[0]));
                spatialiteDb.addGeometryXYColumnAndIndex(newTableName, GEOMFIELD_FOR_SHAPEFILE, typeString, sridString,
                        avoidSpatialIndex);
            } else if (db instanceof PostgisDb) {
                PostgisDb postgisDb = (PostgisDb) db;
                postgisDb.createTable(newTableName, attrSql.toArray(new String[0]));
                postgisDb.addGeometryXYColumnAndIndex(newTableName, GEOMFIELD_FOR_SHAPEFILE, typeString, sridString,
                        avoidSpatialIndex);
            } else if (db instanceof H2GisDb) {
                H2GisDb h2gisDb = (H2GisDb) db;
                String typeStringExtra = typeString;
                // String typeStringExtra = "GEOMETRY(" + typeString + "," + codeFromCrs + ")";
                attrSql.add(GEOMFIELD_FOR_SHAPEFILE + " " + typeStringExtra);
                String[] array = attrSql.toArray(new String[0]);
                h2gisDb.createTable(newTableName, array);
                h2gisDb.addSrid(newTableName, sridString, GEOMFIELD_FOR_SHAPEFILE);
                if (!avoidSpatialIndex)
                    h2gisDb.createSpatialIndex(newTableName, GEOMFIELD_FOR_SHAPEFILE);
            } else if (db instanceof GeopackageDb) {
                GeopackageCommonDb gpkgDb = (GeopackageCommonDb) db;

                int srid = Integer.parseInt(sridString);
                CoordinateReferenceSystem crs = CrsUtilities.getCrsFromSrid(srid);
                gpkgDb.addCRS("EPSG", srid, crs.toWKT());

                String[] array = attrSql.toArray(new String[0]);
                gpkgDb.createSpatialTable(newTableName, Integer.parseInt(sridString), GEOMFIELD_FOR_SHAPEFILE + " " + typeString,
                        array, null, avoidSpatialIndex);
            }
        } else {
            db.createTable(newTableName, attrSql.toArray(new String[0]));
        }

        return newTableName;
    }

    /**
     * Import a shapefile into a table.
     * 
     * @param db the database to use.
     * @param shapeFile the shapefile to import.
     * @param tableName the name of the table to import to.
     * @param limit if > 0, a limit to the imported features is applied.
     * @param useFromTextForGeom if true, the wkt form is used to insert geometries.
     * @param pm the progress monitor.
     * @return <code>false</code>, is an error occurred. 
     * @throws Exception
     */
    public static boolean importShapefile( ASpatialDb db, File shapeFile, String tableName, int limit, boolean useFromTextForGeom,
            IHMProgressMonitor pm ) throws Exception {
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        SimpleFeatureCollection features = featureSource.getFeatures();

        return importFeatureCollection(db, features, tableName, limit, useFromTextForGeom, pm);

    }

    /**
     * Import a featureCollection into a table.
     * 
     * @param db the database to use.
     * @param featureCollection the featureCollection to import.
     * @param tableName the name of the table to import to.
     * @param limit if > 0, a limit to the imported features is applied.
     * @param useFromTextForGeom if true, the wkt form is used to insert geometries.
     * @param pm the progress monitor.
     * @return <code>false</code>, is an error occurred. 
     * @throws Exception
     */
    public static boolean importFeatureCollection( ASpatialDb db, SimpleFeatureCollection featureCollection, String tableName,
            int limit, boolean useFromTextForGeom, IHMProgressMonitor pm ) throws Exception {
        SimpleFeatureType schema = featureCollection.getSchema();
        List<AttributeDescriptor> attributeDescriptors = schema.getAttributeDescriptors();

        int featureCount = featureCollection.size();

        List<String[]> tableInfo = db.getTableColumns(tableName);
        List<String> tableColumns = new ArrayList<>();
        boolean hasFid = false;
        for( String[] item : tableInfo ) {
            String colName = item[0].toUpperCase();
            tableColumns.add(colName);
            if (colName.equalsIgnoreCase(FID)) {
                hasFid = true;
            }
        }
        GeometryColumn geometryColumns = db.getGeometryColumnsForTable(tableName);
        String gCol = geometryColumns.geometryColumnName;

        int epsg = geometryColumns.srid;
        CoordinateReferenceSystem crs = null;
        try {
            crs = CrsUtilities.getCrsFromEpsg("EPSG:" + epsg);
        } catch (Exception e1) {
            // ignore and try without
        }
        SimpleFeatureIterator featureIterator;
        if (crs != null) {
            ReprojectingFeatureCollection repFeatures = new ReprojectingFeatureCollection(featureCollection, crs);
            featureIterator = repFeatures.features();
        } else {
            featureIterator = featureCollection.features();
        }

        List<String> attrNames = new ArrayList<>();
        String valueNames = hasFid ? FID : "";
        String qMarks = hasFid ? "?" : "";
        for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
            String attrName = attributeDescriptor.getLocalName();
            String fidField = FeatureUtilities.findAttributeName(schema, FID);
            if (fidField != null && fidField.equals(attrName)) {
                continue;
            } else if (attrName.equalsIgnoreCase("PK_UID")) {
                continue;
            }
            attrNames.add(attrName);
            if (attributeDescriptor instanceof GeometryDescriptor) {
                valueNames += "," + gCol;
                if (useFromTextForGeom) {
                    qMarks += ",ST_GeomFromText(?, " + epsg + ")";
                } else {
                    qMarks += ",?";
                }
            } else {
                if (!tableColumns.contains(attrName.toUpperCase())) {
                    pm.errorMessage(
                            "The imported shapefile doesn't seem to match the table's schema. Doesn't exist: " + attrName);
                    return false;
                }
                if (DbsUtilities.isReservedName(attrName)) {
                    attrName = DbsUtilities.fixReservedNameForQuery(attrName);
                }
                valueNames += "," + attrName;
                qMarks += ",?";
            }
        }
        if (valueNames.startsWith(",")) {
            valueNames = valueNames.substring(1);
            qMarks = qMarks.substring(1);
        }
        String sql = "INSERT INTO " + tableName + " (" + valueNames + ") VALUES (" + qMarks + ")";

        IGeometryParser gp = db.getType().getGeometryParser();

        boolean _hasFid = hasFid;
        return db.execOnConnection(conn -> {
            boolean noErrors = true;
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (IHMPreparedStatement pStmt = conn.prepareStatement(sql)) {
                int count = 0;
                int batchCount = 0;
                try {
                    while( featureIterator.hasNext() ) {
                        SimpleFeature f = (SimpleFeature) featureIterator.next();

                        int shift = 1;
                        if (_hasFid) {
                            long featureId = FeatureUtilities.getFeatureId(f);
                            pStmt.setLong(1, featureId);
                            shift = 2;
                        }

                        for( int i = 0; i < attrNames.size(); i++ ) {
                            Object object = f.getAttribute(attrNames.get(i));

                            int iPlus = i + shift;
                            if (object == null) {
                                pStmt.setObject(iPlus, null);
                            } else if (object instanceof Double) {
                                pStmt.setDouble(iPlus, (Double) object);
                            } else if (object instanceof Float) {
                                pStmt.setFloat(iPlus, (Float) object);
                            } else if (object instanceof Integer) {
                                pStmt.setInt(iPlus, (Integer) object);
                            } else if (object instanceof Long) {
                                pStmt.setLong(iPlus, (Long) object);
                            } else if (object instanceof String) {
                                pStmt.setString(iPlus, (String) object);
                            } else if (object instanceof Geometry) {
                                Geometry geom = (Geometry) object;
                                if (useFromTextForGeom) {
                                    pStmt.setString(iPlus, geom.toText());
                                } else {
                                    geom.setSRID(epsg);
                                    pStmt.setObject(iPlus, gp.toSqlObject(geom));
                                }
                            } else if (object instanceof Clob) {
                                String string = ((Clob) object).toString();
                                pStmt.setString(iPlus, string);
                            } else {
                                pStmt.setString(iPlus, object.toString());
                            }
                        }
                        pStmt.addBatch();
                        count++;
                        batchCount++;
                        if (batchCount % DbsUtilities.DEFAULT_BULK_INSERT_CHUNK_SIZE == 0) {
                            pm.beginTask("Batch import " + batchCount + " features. ( " + count + " of " + featureCount + " )",
                                    IHMProgressMonitor.UNKNOWN);
                            pStmt.executeBatch();
                            pm.done();
                            batchCount = 0;
                        }
                        if (limit > 0 && count >= limit) {
                            break;
                        }
                    }
                    if (batchCount > 0) {
                        pm.beginTask("Batch import " + batchCount + " features. ( " + count + " of " + featureCount + " )",
                                IHMProgressMonitor.UNKNOWN);
                        pStmt.executeBatch();
                        pm.done();
                    }
                } catch (Exception e) {
                    logger.insertError("SpatialDbsImportUtils", "error", e);
                    noErrors = false;
                } finally {
                    featureIterator.close();
                }
            }
            conn.setAutoCommit(autoCommit);
            try (IHMStatement pStmt = conn.createStatement()) {
                try {
                    pStmt.executeQuery("Select updateLayerStatistics();");
                } catch (Exception e) {
                    // ignore
                }
            }
            return noErrors;
        });
    }

    /**
     * Get a table as featurecollection.
     * 
     * @param db the database.
     * @param tableName the table to use.
     * @param featureLimit limit in feature or -1.
     * @param forceSrid a srid to force to or -1.
     * @param whereStr an optional where condition string.
     * @return the extracted featurecollection.
     * @throws SQLException
     * @throws Exception
     */
    public static DefaultFeatureCollection tableToFeatureFCollection( ASpatialDb db, String tableName, int featureLimit,
            int forceSrid, String whereStr ) throws SQLException, Exception {
        DefaultFeatureCollection fc = new DefaultFeatureCollection();

        GeometryColumn geometryColumn = db.getGeometryColumnsForTable(tableName);
        CoordinateReferenceSystem forceCrs = null;
        CoordinateReferenceSystem crs;
        if (geometryColumn != null) {
            if (forceSrid == -1) {
                forceSrid = geometryColumn.srid;
            } else {
                forceCrs = CrsUtilities.getCrsFromEpsg("EPSG:" + forceSrid);
            }
            crs = CrsUtilities.getCrsFromEpsg("EPSG:" + geometryColumn.srid);
        } else {
            crs = CrsUtilities.getCrsFromEpsg("EPSG:" + forceSrid);
        }

        QueryResult tableRecords = db.getTableRecordsMapIn(tableName, null, featureLimit, forceSrid, whereStr);
        if (tableRecords.data.size() == 0) {
            return fc;
        }

        int geometryIndex = tableRecords.geometryIndex;
        int latIndex = -1;
        int lonIndex = -1;
        Geometry sampleGeom = null;
        if (geometryIndex == -1) {
            for( String fieldName : tableRecords.names ) {
                if (fieldName.toLowerCase().startsWith("lat")) {
                    latIndex = tableRecords.names.indexOf(fieldName);
                } else if (fieldName.toLowerCase().startsWith("lon")) {
                    lonIndex = tableRecords.names.indexOf(fieldName);
                }
            }
            if (latIndex == -1 || lonIndex == -1)
                throw new IllegalArgumentException("Not a geometric layer.");
        } else {
            sampleGeom = (Geometry) tableRecords.data.get(0)[geometryIndex];
        }

        List<String> names = tableRecords.names;
        List<String> types = tableRecords.types;

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(tableName);
        if (forceCrs != null) {
            b.setCRS(forceCrs);
        } else {
            b.setCRS(crs);
        }

        if (latIndex != -1 && lonIndex != -1) {
            b.add(GEOMFIELD_FOR_SHAPEFILE, Point.class);
        }
        for( int i = 0; i < names.size(); i++ ) {
            if (geometryIndex != -1 && i == geometryIndex) {
                Class< ? > geometryClass = sampleGeom.getClass();
                b.add(geometryColumn.geometryColumnName, geometryClass);
                continue;
            }
            Class< ? > fieldClass = null;
            String typeStr = types.get(i);
            EDataType type = EDataType.getType4Name(typeStr);
            switch( type ) {
            case DOUBLE:
                fieldClass = Double.class;
                break;
            case FLOAT:
                fieldClass = Float.class;
                break;
            case INTEGER:
                fieldClass = Integer.class;
                break;
            case LONG:
                fieldClass = Long.class;
                break;
            case BOOLEAN:
                fieldClass = Integer.class;
                break;
            case DATE:
            case DATETIME:
                fieldClass = String.class;
                break;
            case TEXT:
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

            if (latIndex != -1 && lonIndex != -1) {
                double lat = ((Number) objects[latIndex]).doubleValue();
                double lon = ((Number) objects[lonIndex]).doubleValue();
                Point point = GeometryUtilities.gf().createPoint(new Coordinate(lon, lat));
                Object[] newObjects = new Object[objects.length + 1];
                System.arraycopy(objects, 0, newObjects, 1, objects.length);
                newObjects[0] = point;
                builder.addAll(newObjects);
            } else {
                builder.addAll(objects);
            }

            SimpleFeature feature = builder.buildFeature(null);
            fc.add(feature);
        }
        return fc;
    }

    public static DefaultFeatureCollection tableGeomsToFeatureFCollection( ASpatialDb db, String tableName, int forceSrid,
            String geomPrefix, String geomPostfix, String whereStr ) throws SQLException, Exception {

        GeometryColumn geometryColumn = db.getGeometryColumnsForTable(tableName);
        CoordinateReferenceSystem crs;
        if (geometryColumn != null) {
            if (forceSrid == -1) {
                forceSrid = geometryColumn.srid;
            }
            crs = CrsUtilities.getCrsFromEpsg("EPSG:" + geometryColumn.srid);
        } else {
            crs = CrsUtilities.getCrsFromEpsg("EPSG:" + forceSrid);
        }

        List<Geometry> tableRecords = db.getGeometriesIn(tableName, (Envelope) null, geomPrefix, geomPostfix, whereStr);

        SimpleFeatureCollection fc;
        if (tableRecords.size() > 0) {
            fc = FeatureUtilities.featureCollectionFromGeometry(crs, tableRecords.toArray(new Geometry[tableRecords.size()]));
        } else {
            fc = new DefaultFeatureCollection();
        }
        return (DefaultFeatureCollection) fc;
    }
}
