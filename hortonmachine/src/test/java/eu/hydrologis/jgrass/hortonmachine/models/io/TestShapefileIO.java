package eu.hydrologis.jgrass.hortonmachine.models.io;

import java.io.File;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import eu.hydrologis.jgrass.hortonmachine.io.shapefile.ShapefileFeatureReader;
import eu.hydrologis.jgrass.hortonmachine.io.shapefile.ShapefileFeatureWriter;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.geometry.GeometryUtilities;
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

        FeatureCollection<SimpleFeatureType, SimpleFeature> newCollection = FeatureCollections.newCollection();
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
        ShapefileFeatureWriter writer = new ShapefileFeatureWriter();
        writer.file = tmpShape.getAbsolutePath();
        writer.geodata = newCollection;
        writer.writeFeatureCollection();
        
        // now read it again
        ShapefileFeatureReader reader = new ShapefileFeatureReader();
        reader.file = tmpShape.getAbsolutePath();
        reader.readFeatureCollection();
        FeatureCollection<SimpleFeatureType, SimpleFeature> readFC = reader.geodata;
        
        FeatureIterator<SimpleFeature> featureIterator = readFC.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature f = featureIterator.next();
            
            int id = ((Number)f.getAttribute("id")).intValue();
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
        
        
    }
}
