/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.utils.features;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gce.grassraster.JGrassConstants;
import org.geotools.geometry.Envelope2D;
import org.jaitools.media.jai.vectorize.VectorizeDescriptor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.util.AffineTransformation;

public class FeatureUtilities {

    /**
     * Order the geometries of a list to be all directed in the same direction
     * 
     * @param geometryList the list of geometries to be ordered
     * @param thresHold a scalar value that defines the max distance between two points to be the
     *        same
     * @return a list of ordered coordinates
     */
    @SuppressWarnings("unchecked")
    public static CoordinateList orderLineGeometries( List<Geometry> geometryList, double thresHold ) {
        /*
         * first search the feature that is one of the two external points
         */
        Geometry firstFeature = null;
        boolean foundFirst = true;
        boolean foundSecond = true;
        for( Geometry feature : geometryList ) {
            foundFirst = true;
            foundSecond = true;

            Coordinate[] coords = feature.getCoordinates();

            Coordinate first = coords[0];
            Coordinate last = coords[coords.length - 1];

            for( Geometry compareFeature : geometryList ) {
                if (compareFeature.equals(feature))
                    continue;
                Coordinate[] compareCoords = compareFeature.getCoordinates();

                Coordinate comparefirst = compareCoords[0];
                Coordinate comparelast = compareCoords[compareCoords.length - 1];

                /*
                 * check if the next point is far away
                 */
                if (first.distance(comparefirst) < thresHold || first.distance(comparelast) < thresHold) {
                    foundFirst = false;
                }
                if (last.distance(comparefirst) < thresHold || last.distance(comparelast) < thresHold) {
                    foundSecond = false;
                }

            }
            if (foundFirst || foundSecond) {
                firstFeature = feature;
                break;
            }

        }
        if (firstFeature == null) {
            throw new RuntimeException();
        }
        CoordinateList coordinateList = new CoordinateList();
        Coordinate[] coords = firstFeature.getCoordinates();
        if (foundSecond) {
            for( int i = 0; i < coords.length; i++ ) {
                coordinateList.add(coords[coords.length - i - 1]);
            }
        } else {
            for( int i = 0; i < coords.length; i++ ) {
                coordinateList.add(coords[i]);
            }
        }

        // if (foundFirst) {
        // addCoordsInProperDirection(foundFirst, coordinateList, coords, true,
        // 0);
        // }else{
        // addCoordsInProperDirection(foundSecond, coordinateList, coords, true,
        // 0);
        // }

        geometryList.remove(firstFeature);

        Coordinate currentCoordinate = coordinateList.getCoordinate(coordinateList.size() - 1);
        while( geometryList.size() != 0 ) {

            for( int j = 0; j < geometryList.size(); j++ ) {
                System.out.println(j);
                Geometry compareGeom = geometryList.get(j);
                Coordinate[] compareCoords = compareGeom.getCoordinates();

                Coordinate comparefirst = compareCoords[0];
                Coordinate comparelast = compareCoords[compareCoords.length - 1];

                // System.out.println(j + " "
                // + currentCoordinate.distance(comparefirst) + " "
                // + currentCoordinate.distance(comparelast));

                /*
                 * check if the next point is far away
                 */
                if (currentCoordinate.distance(comparefirst) < thresHold) {
                    for( int i = 0; i < compareCoords.length; i++ ) {
                        coordinateList.add(compareCoords[i]);
                    }
                    currentCoordinate = new Coordinate(comparelast);
                    geometryList.remove(compareGeom);
                    break;
                } else if (currentCoordinate.distance(comparelast) < thresHold) {
                    for( int i = 0; i < compareCoords.length; i++ ) {
                        coordinateList.add(compareCoords[compareCoords.length - i - 1]);
                    }
                    currentCoordinate = new Coordinate(comparefirst);
                    geometryList.remove(compareGeom);
                    break;
                }

            }

        }

        return coordinateList;
    }

    /**
     * Create a featurecollection from a vector of features
     * 
     * @param features - the vectore of features
     * @return the created featurecollection
     */
    public static SimpleFeatureCollection createFeatureCollection( SimpleFeature... features ) {
        SimpleFeatureCollection fcollection = FeatureCollections.newCollection();

        for( SimpleFeature feature : features ) {
            fcollection.add(feature);
        }
        return fcollection;
    }

    /**
     * <p>
     * Convert a csv file to a FeatureCollection. 
     * <b>This for now supports only point geometries</b>.<br>
     * For different crs it also performs coor transformation.
     * </p>
     * <p>
     * <b>NOTE: this doesn't support date attributes</b>
     * </p>
     * 
     * @param csvFile the csv file.
     * @param crs the crs to use.
     * @param fieldsAndTypes the {@link Map} of filed names and {@link JGrassConstants#CSVTYPESARRAY types}.
     * @param pm progress monitor.
     * @param separatorthe separator to use, if null, comma is used.
     * @return the created {@link FeatureCollection}
     * @throws Exception
     */
    @SuppressWarnings("nls")
    public static SimpleFeatureCollection csvFileToFeatureCollection( File csvFile, CoordinateReferenceSystem crs,
            LinkedHashMap<String, Integer> fieldsAndTypesIndex, String separator, IJGTProgressMonitor pm ) throws Exception {
        GeometryFactory gf = new GeometryFactory();
        Map<String, Class< ? >> typesMap = JGrassConstants.CSVTYPESCLASSESMAP;
        String[] typesArray = JGrassConstants.CSVTYPESARRAY;

        if (separator == null) {
            separator = ",";
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("csvimport");
        b.setCRS(crs);
        b.add("the_geom", Point.class);

        int xIndex = -1;
        int yIndex = -1;
        Set<String> fieldNames = fieldsAndTypesIndex.keySet();
        String[] fieldNamesArray = (String[]) fieldNames.toArray(new String[fieldNames.size()]);
        for( int i = 0; i < fieldNamesArray.length; i++ ) {
            String fieldName = fieldNamesArray[i];
            Integer typeIndex = fieldsAndTypesIndex.get(fieldName);

            if (typeIndex == 0) {
                xIndex = i;
            } else if (typeIndex == 1) {
                yIndex = i;
            } else {
                Class class1 = typesMap.get(typesArray[typeIndex]);
                b.add(fieldName, class1);
            }
        }
        SimpleFeatureType featureType = b.buildFeatureType();

        SimpleFeatureCollection newCollection = FeatureCollections.newCollection();
        Collection<Integer> orderedTypeIndexes = fieldsAndTypesIndex.values();
        Integer[] orderedTypeIndexesArray = (Integer[]) orderedTypeIndexes.toArray(new Integer[orderedTypeIndexes.size()]);

        BufferedReader bR = new BufferedReader(new FileReader(csvFile));
        String line = null;
        int featureId = 0;
        pm.beginTask("Importing raw data", -1);
        while( (line = bR.readLine()) != null ) {
            pm.worked(1);
            if (line.startsWith("#")) {
                continue;
            }

            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
            Object[] values = new Object[fieldNames.size() - 1];

            String[] lineSplit = line.split(separator);
            double x = Double.parseDouble(lineSplit[xIndex]);
            double y = Double.parseDouble(lineSplit[yIndex]);
            Point point = gf.createPoint(new Coordinate(x, y));
            values[0] = point;

            int objIndex = 1;
            for( int i = 0; i < lineSplit.length; i++ ) {
                if (i == xIndex || i == yIndex) {
                    continue;
                }

                String value = lineSplit[i];
                int typeIndex = orderedTypeIndexesArray[i];
                String typeName = typesArray[typeIndex];
                if (typeName.equals(typesArray[3])) {
                    values[objIndex] = value;
                } else if (typeName.equals(typesArray[4])) {
                    values[objIndex] = new Double(value);
                } else if (typeName.equals(typesArray[5])) {
                    values[objIndex] = new Integer(value);
                } else {
                    throw new IllegalArgumentException("An undefined value type was found");
                }
                objIndex++;
            }
            builder.addAll(values);

            SimpleFeature feature = builder.buildFeature(featureType.getTypeName() + "." + featureId);
            featureId++;
            newCollection.add(feature);
        }
        bR.close();
        pm.done();

        return newCollection;
    }

    /**
     * <p>
     * The easy way to create a shapefile from attributes and geometries
     * </p>
     * <p>
     * <b>NOTE: this doesn't support date attributes</b>
     * </p>
     * 
     * @param shapeFilePath the shapefile name
     * @param crs the destination crs
     * @param fet the featurecollection
     * @throws IOException 
     */
    public static synchronized boolean collectionToShapeFile( String shapeFilePath, CoordinateReferenceSystem crs,
            SimpleFeatureCollection fet ) throws IOException {

        // Create the file you want to write to
        File file = null;
        if (shapeFilePath.toLowerCase().endsWith(".shp")) { //$NON-NLS-1$
            file = new File(shapeFilePath);
        } else {
            file = new File(shapeFilePath + ".shp"); //$NON-NLS-1$
        }

        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();

        Map<String, Serializable> create = new HashMap<String, Serializable>();
        create.put("url", file.toURI().toURL());
        ShapefileDataStore newDataStore = (ShapefileDataStore) factory.createNewDataStore(create);

        newDataStore.createSchema(fet.getSchema());
        if (crs != null)
            newDataStore.forceSchemaCRS(crs);
        Transaction transaction = new DefaultTransaction();
        FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore
                .getFeatureSource();
        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(fet);
            transaction.commit();
        } catch (Exception problem) {
            problem.printStackTrace();
            transaction.rollback();
        } finally {
            transaction.close();
        }
        return true;
    }
    /**
     * @param name the shapefile name
     * @param fieldsSpec to create other fields you can use a string like : <br>
     *        "geom:MultiLineString,FieldName:java.lang.Integer" <br>
     *        field name can not be over 10 characters use a ',' between each field <br>
     *        field types can be : java.lang.Integer, java.lang.Long, // java.lang.Double,
     *        java.lang.String or java.util.Date
     * @return
     * @throws Exception 
     */
    @SuppressWarnings("nls")
    public static synchronized ShapefileDataStore createShapeFileDatastore( String name, String fieldsSpec,
            CoordinateReferenceSystem crs ) throws Exception {
        // Create the file you want to write to
        File file = null;
        if (name.toLowerCase().endsWith(".shp")) {
            file = new File(name);
        } else {
            file = new File(name + ".shp");
        }

        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        Map<String, Serializable> create = new HashMap<String, Serializable>();
        create.put("url", file.toURI().toURL());
        ShapefileDataStore myData = (ShapefileDataStore) factory.createNewDataStore(create);

        // Tell this shapefile what type of data it will store
        // Shapefile handle only : Point, MultiPoint, MultiLineString,
        // MultiPolygon
        SimpleFeatureType featureType = DataUtilities.createType(name, fieldsSpec);

        // Create the Shapefile (empty at this point)
        myData.createSchema(featureType);

        // Tell the DataStore what type of Coordinate Reference System (CRS)
        // to use
        myData.forceSchemaCRS(crs);

        return myData;

    }

    /**
     * Writes a featurecollection to a shapefile
     * 
     * @param data the datastore
     * @param collection the featurecollection
     * @throws IOException 
     */
    private static synchronized boolean writeToShapefile( ShapefileDataStore data, SimpleFeatureCollection collection )
            throws IOException {
        String featureName = data.getTypeNames()[0]; // there is only one in
        // a shapefile
        FeatureStore<SimpleFeatureType, SimpleFeature> store = null;

        Transaction transaction = null;
        try {

            // Create the DefaultTransaction Object
            transaction = Transaction.AUTO_COMMIT;

            // Tell it the name of the shapefile it should look for in our
            // DataStore
            SimpleFeatureSource source = data.getFeatureSource(featureName);
            store = (FeatureStore<SimpleFeatureType, SimpleFeature>) source;
            store.addFeatures(collection);
            data.getFeatureWriter(transaction);

            // TODO is this needed transaction.commit();
            return true;
        } catch (Exception eek) {
            eek.printStackTrace();
            transaction.rollback();
            return false;
        } finally {
            transaction.close();
        }
    }

    /**
     * Extracts features from a {@link FeatureCollection} into an {@link ArrayList}.
     * 
     * @param collection the feature collection.
     * @return the list with the features or an empty list if no features present.
     */
    public static List<SimpleFeature> featureCollectionToList( SimpleFeatureCollection collection ) {
        List<SimpleFeature> featuresList = new ArrayList<SimpleFeature>();
        if (collection == null) {
            return featuresList;
        }
        SimpleFeatureIterator featureIterator = collection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            featuresList.add(feature);
        }
        featureIterator.close();
        return featuresList;
    }

    /**
     * Extracts features from a {@link FeatureCollection} into an {@link ArrayList} of {@link FeatureMate}s.
     * 
     * @param collection the feature collection.
     * @return the list with the features or an empty list if no features present.
     */
    public static List<FeatureMate> featureCollectionToMatesList( SimpleFeatureCollection collection ) {
        List<FeatureMate> featuresList = new ArrayList<FeatureMate>();
        if (collection == null) {
            return featuresList;
        }
        SimpleFeatureIterator featureIterator = collection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            featuresList.add(new FeatureMate(feature));
        }
        featureIterator.close();
        return featuresList;
    }

    /**
     * Extracts features from a {@link FeatureCollection} into an {@link ArrayList} of its geometries.
     * 
     * @param collection the feature collection.
     * @param doSubGeoms split the geometries in single geometries (ex. MultiLines in Lines).
     * @param userDataField if not <code>null</code>, the data in the field are put in the userData
     *                  field of the geometry.
     * @return the list with the geometries or an empty list if no features present.
     */
    public static List<Geometry> featureCollectionToGeometriesList( SimpleFeatureCollection collection, boolean doSubGeoms,
            String userDataField ) {
        List<Geometry> geometriesList = new ArrayList<Geometry>();
        if (collection == null) {
            return geometriesList;
        }
        SimpleFeatureIterator featureIterator = collection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (geometry != null)
                if (doSubGeoms) {
                    int numGeometries = geometry.getNumGeometries();
                    for( int i = 0; i < numGeometries; i++ ) {
                        Geometry geometryN = geometry.getGeometryN(i);
                        geometriesList.add(geometryN);
                        if (userDataField != null) {
                            Object attribute = feature.getAttribute(userDataField);
                            geometryN.setUserData(attribute);
                        }
                    }
                } else {
                    geometriesList.add(geometry);
                    if (userDataField != null) {
                        Object attribute = feature.getAttribute(userDataField);
                        geometry.setUserData(attribute);
                    }
                }
        }
        featureIterator.close();
        return geometriesList;
    }

    /**
     * Make a {@link SimpleFeatureCollection} from a set of {@link Geometry}.
     * 
     * <p>This is a fast utility and adds no attributes.</p>
     * 
     * @param crs The {@link CoordinateReferenceSystem}.
     * @param geometries the set of {@link Geometry} to add.
     * @return the features wrapping the geoms.
     */
    public static SimpleFeatureCollection featureCollectionFromGeometry( CoordinateReferenceSystem crs, Geometry... geometries ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("simplegeom");
        b.setCRS(crs);

        SimpleFeatureCollection newCollection = FeatureCollections.newCollection();

        GEOMETRYTYPE geometryType = GeometryUtilities.getGeometryType(geometries[0]);
        switch( geometryType ) {
        case LINE:
            b.add("the_geom", LineString.class);
            break;
        case MULTILINE:
            b.add("the_geom", MultiLineString.class);
            break;
        case POINT:
            b.add("the_geom", Point.class);
            break;
        case MULTIPOINT:
            b.add("the_geom", MultiPoint.class);
            break;
        case POLYGON:
            b.add("the_geom", Polygon.class);
            break;
        case MULTIPOLYGON:
            b.add("the_geom", MultiPolygon.class);
            break;
        default:
            break;
        }
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for( Geometry g : geometries ) {
            Object[] values = new Object[]{g};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            newCollection.add(feature);
        }
        return newCollection;
    }

    /**
     * Getter for attributes of a feature.
     * 
     * <p>If the attribute is not found, checks are done in non
     * case sensitive mode.
     * 
     * @param feature the feature from which to get the attribute.
     * @param field the name of the field.
     * @return the attribute or null if none found.
     */
    public static Object getAttributeCaseChecked( SimpleFeature feature, String field ) {
        Object attribute = feature.getAttribute(field);
        if (attribute == null) {
            attribute = feature.getAttribute(field.toLowerCase());
            if (attribute != null)
                return attribute;
            attribute = feature.getAttribute(field.toUpperCase());
            if (attribute != null)
                return attribute;

            // alright, last try, search for it
            SimpleFeatureType featureType = feature.getFeatureType();
            field = findAttributeName(featureType, field);
            if (field != null) {
                return feature.getAttribute(field);
            }
        }
        return attribute;
    }

    /**
     * Find the name of an attribute, case insensitive.
     * 
     * @param featureType te feature type to check.
     * @param field the case insensitive field name.
     * @return the real name of the field, or <code>null</code>, if none found.
     */
    public static String findAttributeName( SimpleFeatureType featureType, String field ) {
        List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
        for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
            String name = attributeDescriptor.getLocalName();
            if (name.toLowerCase().equals(field.toLowerCase())) {
                return name;
            }
        }
        return null;
    }

    /**
     * Create a {@link Polygon} from an {@link Envelope}.
     * 
     * @param envelope the envelope to convert.
     * @return the created polygon.
     */
    public static Polygon envelopeToPolygon( Envelope2D envelope ) {
        double w = envelope.getMinX();
        double e = envelope.getMaxX();
        double s = envelope.getMinY();
        double n = envelope.getMaxY();

        Coordinate[] coords = new Coordinate[5];
        coords[0] = new Coordinate(w, n);
        coords[1] = new Coordinate(e, n);
        coords[2] = new Coordinate(e, s);
        coords[3] = new Coordinate(w, s);
        coords[4] = new Coordinate(w, n);

        GeometryFactory gf = GeometryUtilities.gf();
        LinearRing linearRing = gf.createLinearRing(coords);
        Polygon polygon = gf.createPolygon(linearRing, null);
        return polygon;
    }
    
    /**
     * Create a {@link Polygon} from an {@link Envelope}.
     * 
     * @param envelope the envelope to convert.
     * @return the created polygon.
     */
    public static Polygon envelopeToPolygon( Envelope envelope ) {
    	double w = envelope.getMinX();
    	double e = envelope.getMaxX();
    	double s = envelope.getMinY();
    	double n = envelope.getMaxY();
    	
    	Coordinate[] coords = new Coordinate[5];
    	coords[0] = new Coordinate(w, n);
    	coords[1] = new Coordinate(e, n);
    	coords[2] = new Coordinate(e, s);
    	coords[3] = new Coordinate(w, s);
    	coords[4] = new Coordinate(w, n);
    	
    	GeometryFactory gf = GeometryUtilities.gf();
    	LinearRing linearRing = gf.createLinearRing(coords);
    	Polygon polygon = gf.createPolygon(linearRing, null);
    	return polygon;
    }

    /**
     * Helper function to run the Vectorize operation with given parameters and
     * retrieve the vectors.
     * 
     * @param src the source {@link GridCoverage2D}.
     * @param args a {@code Map} of parameter names and values or <code>null</code>.
     * 
     * @return the generated vectors as JTS Polygons
     */
    @SuppressWarnings("unchecked")
    public static Collection<Polygon> doVectorize( GridCoverage2D src, Map<String, Object> args ) {
        if (args == null) {
            args = new HashMap<String, Object>();
        }

        ParameterBlockJAI pb = new ParameterBlockJAI("Vectorize");
        pb.setSource("source0", src.getRenderedImage());

        // Set any parameters that were passed in
        for( Entry<String, Object> e : args.entrySet() ) {
            pb.setParameter(e.getKey(), e.getValue());
        }

        // Get the desintation image: this is the unmodified source image data
        // plus a property for the generated vectors
        RenderedOp dest = JAI.create("Vectorize", pb);

        // Get the vectors
        Collection<Polygon> polygons = (Collection<Polygon>) dest.getProperty(VectorizeDescriptor.VECTOR_PROPERTY_NAME);

        RegionMap regionParams = CoverageUtilities.getRegionParamsFromGridCoverage(src);
        double xRes = regionParams.getXres();
        double yRes = regionParams.getYres();
        final AffineTransform mt2D = (AffineTransform) src.getGridGeometry().getGridToCRS2D(PixelOrientation.CENTER);
        final AffineTransformation jtsTransformation = new AffineTransformation(mt2D.getScaleX(), mt2D.getShearX(),
                mt2D.getTranslateX() - xRes / 2.0, mt2D.getShearY(), mt2D.getScaleY(), mt2D.getTranslateY() + yRes / 2.0);
        for( Polygon polygon : polygons ) {
            polygon.apply(jtsTransformation);
        }
        return polygons;
    }

    public static SimpleFeature toDummyFeature( Geometry geom, CoordinateReferenceSystem crs ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("dummy");
        if (crs != null)
            b.setCRS(crs);
        b.add("the_geom", geom.getClass());
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{geom};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(null);
        return feature;
    }

}
