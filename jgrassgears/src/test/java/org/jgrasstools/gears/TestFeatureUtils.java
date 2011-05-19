package org.jgrasstools.gears;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Point;
/**
 * Test FeatureUtils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestFeatureUtils extends HMTestCase {

    @SuppressWarnings("nls")
    public void testFeatureUtils() throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("typename");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add("AttrName", String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{GeometryUtilities.gf().createPoint(new Coordinate(0, 0)), "test"};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(type.getTypeName());

        Object attr = FeatureUtilities.getAttributeCaseChecked(feature, "attrname");
        assertEquals("test", attr.toString());
        attr = FeatureUtilities.getAttributeCaseChecked(feature, "attrnam");
        assertNull(attr);

    }

}
