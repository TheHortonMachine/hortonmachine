package org.hortonmachine.gears.modules;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
/**
 * Test {@link OmsVectorReader}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorReader extends HMTestCase {

    public void testShapefileReader() throws Exception {

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add("id", Integer.class);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        SimpleFeatureType type = b.buildFeatureType();
        for( int i = 0; i < 2; i++ ) {
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

            Point point = GeometryUtilities.gf().createPoint(new Coordinate(i, i));
            Object[] values = new Object[]{point, i};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + i);
            newCollection.add(feature);
        }

        File tmpShape = File.createTempFile("testshp", ".shp");
        if (tmpShape.exists()) {
            if (!tmpShape.delete())
                throw new IOException();
        }
        OmsVectorWriter writer = new OmsVectorWriter();
        writer.file = tmpShape.getAbsolutePath();
        writer.inVector = newCollection;
        writer.process();

        // now read it again
        OmsVectorReader reader = new OmsVectorReader();
        reader.file = tmpShape.getAbsolutePath();
        reader.process();
        SimpleFeatureCollection readFC = reader.outVector;

        FeatureIterator<SimpleFeature> featureIterator = readFC.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature f = featureIterator.next();

            int id = ((Number) f.getAttribute("id")).intValue();
            Geometry geometry = (Geometry) f.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();

            if (id == 0) {
                assertEquals(coordinate.x, 0.0);
                assertEquals(coordinate.y, 0.0);
            }
            if (id == 1) {
                assertEquals(coordinate.x, 1.0);
                assertEquals(coordinate.y, 1.0);
            }
            if (id == 2) {
                assertEquals(coordinate.x, 2.0);
                assertEquals(coordinate.y, 2.0);
            }
        }

        if (tmpShape.exists()) {
            tmpShape.deleteOnExit();
        }
    }

    public void testPropertiesReader() throws Exception {

        URL dataUrl = this.getClass().getClassLoader().getResource("example.properties");
        String propertiesPath = new File(dataUrl.toURI()).getAbsolutePath();

        // now read it again
        OmsVectorReader reader = new OmsVectorReader();
        reader.file = propertiesPath;
        reader.process();
        SimpleFeatureCollection readFC = reader.outVector;

        FeatureIterator<SimpleFeature> featureIterator = readFC.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature f = featureIterator.next();

            int id = ((Number) f.getAttribute("id")).intValue();
            Geometry geometry = (Geometry) f.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();

            if (id == 1) {
                assertEquals(coordinate.x, 0.0);
                assertEquals(coordinate.y, 0.0);
            }
            if (id == 2) {
                assertEquals(coordinate.x, 10.0);
                assertEquals(coordinate.y, 10.0);
            }
            if (id == 3) {
                assertEquals(coordinate.x, 20.0);
                assertEquals(coordinate.y, 20.0);
            }
            if (id == 4) {
                String attribute = f.getAttribute("name").toString();
                assertEquals(attribute, "justin deolivera");
            }
        }

    }

    public void testPropertiesReader2() throws Exception {

        URL dataUrl = this.getClass().getClassLoader().getResource("example2.properties");
        String propertiesPath = new File(dataUrl.toURI()).getAbsolutePath();

        // now read it again
        OmsVectorReader reader = new OmsVectorReader();
        reader.file = propertiesPath;
        reader.process();
        SimpleFeatureCollection readFC = reader.outVector;

        FeatureIterator<SimpleFeature> featureIterator = readFC.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature f = featureIterator.next();

            int id = ((Number) f.getAttribute("id")).intValue();
            Geometry geometry = (Geometry) f.getDefaultGeometry();

            if (id == 1) {
                Coordinate coordinate = geometry.getCoordinates()[1];
                assertTrue(geometry instanceof LineString);
                assertEquals(coordinate.x, 40.0);
                assertEquals(coordinate.y, 40.0);
            }
            if (id == 2) {
                assertTrue(geometry instanceof LineString);
                Coordinate coordinate = geometry.getCoordinates()[3];
                assertEquals(coordinate.x, 90.0);
                assertEquals(coordinate.y, 0.0);
            }
        }

    }
}
