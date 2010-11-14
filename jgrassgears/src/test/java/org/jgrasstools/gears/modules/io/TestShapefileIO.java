package org.jgrasstools.gears.modules.io;

import java.io.File;
import java.io.IOException;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureReader;
import org.jgrasstools.gears.io.shapefile.ShapefileFeatureWriter;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestShapefileIO extends HMTestCase {

    public void testShapefileIO() throws Exception {

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add("id", Integer.class);

        SimpleFeatureCollection newCollection = FeatureCollections
                .newCollection();
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
        ShapefileFeatureWriter writer = new ShapefileFeatureWriter();
        writer.file = tmpShape.getAbsolutePath();
        writer.geodata = newCollection;
        writer.writeFeatureCollection();

        // now read it again
        ShapefileFeatureReader reader = new ShapefileFeatureReader();
        reader.file = tmpShape.getAbsolutePath();
        reader.readFeatureCollection();
        SimpleFeatureCollection readFC = reader.geodata;

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
            if (!tmpShape.delete())
                throw new IOException();
        }
    }
}
