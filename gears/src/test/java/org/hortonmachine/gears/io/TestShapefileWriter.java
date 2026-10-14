/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.io;

import java.io.File;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureWriter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

/**
 * Test {@link OmsShapefileFeatureWriter}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestShapefileWriter extends HMTestCase {

    /**
     * The shapefile cuts the names of the fields to 10 characters: the values must still be written.
     */
    public void testLongFieldNames() throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("long");
        b.setCRS(HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"));
        b.add("id", Integer.class);
        b.add("the_geom", Point.class);
        b.add("classification", Integer.class);
        b.add("classification_2", String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        Point point = GeometryUtilities.gf().createPoint(new Coordinate(10, 20));
        builder.addAll(new Object[]{7, point, 3, "high vegetation"});
        fc.add(builder.buildFeature(null));

        File shp = File.createTempFile("hm_long_names_", ".shp");
        try {
            OmsShapefileFeatureWriter.writeShapefile(shp.getAbsolutePath(), fc, pm);

            List<SimpleFeature> read = FeatureUtilities
                    .featureCollectionToList(OmsShapefileFeatureReader.readShapefile(shp.getAbsolutePath()));
            assertEquals(1, read.size());
            SimpleFeature feature = read.get(0);
            List<Object> attributes = feature.getAttributes();
            // the geometry first, then the fields in their order
            assertTrue(((Point) attributes.get(0)).equalsExact(point));
            assertEquals(7, ((Number) attributes.get(1)).intValue());
            assertEquals(3, ((Number) attributes.get(2)).intValue());
            assertEquals("high vegetation", attributes.get(3));
        } finally {
            String base = shp.getAbsolutePath().replaceFirst("\\.shp$", "");
            for( String extension : new String[]{".shp", ".shx", ".dbf", ".prj", ".fix", ".qix", ".cpg"} ) {
                new File(base + extension).delete();
            }
        }
    }

}
