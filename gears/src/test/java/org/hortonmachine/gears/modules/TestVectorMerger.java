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
package org.hortonmachine.gears.modules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
/**
 * Test for the {@link OmsVectorMerger}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorMerger extends HMTestCase {

    @SuppressWarnings("nls")
    public void testVectorMerger() throws Exception {
        SimpleFeatureCollection testFC1 = HMTestMaps.getTestFC();
        SimpleFeatureCollection testFC2 = HMTestMaps.getTestFC();

        OmsVectorMerger filter = new OmsVectorMerger();
        filter.pm = pm;
        filter.inVectors = Arrays.asList(testFC1, testFC2);
        filter.process();
        SimpleFeatureCollection outFC = filter.outVector;

        assertTrue(outFC.size() == 6);
    }

    /**
     * The features of the first vector come first, in their order.
     */
    public void testOrder() throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        OmsVectorMerger merger = new OmsVectorMerger();
        merger.pm = pm;
        merger.inVectors = Arrays.asList(points(crs, 0, 12), points(crs, 100, 3));
        merger.process();

        List<Integer> ids = new ArrayList<>();
        for( SimpleFeature f : FeatureUtilities.featureCollectionToList(merger.outVector) ) {
            ids.add((Integer) f.getAttribute("id"));
        }
        assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 100, 101, 102), ids);
    }

    public void testDifferentFields() throws Exception {
        OmsVectorMerger merger = new OmsVectorMerger();
        merger.pm = pm;
        merger.inVectors = Arrays.asList(HMTestMaps.getTestFC(), points(HMTestMaps.getCrs(), 0, 2));
        try {
            merger.process();
            fail("Vectors with different fields can't be merged.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
    }

    public void testDifferentCrs() throws Exception {
        OmsVectorMerger merger = new OmsVectorMerger();
        merger.pm = pm;
        merger.inVectors = Arrays.asList(points(HMTestMaps.getCrs(), 0, 2), points(DefaultGeographicCRS.WGS84, 0, 2));
        try {
            merger.process();
            fail("Vectors with different crs can't be merged.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("crs"));
        }
    }

    private static SimpleFeatureCollection points( CoordinateReferenceSystem crs, int firstId, int count ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("points");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("id", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        for( int i = 0; i < count; i++ ) {
            Point point = GeometryUtilities.gf().createPoint(new Coordinate(i, i));
            builder.addAll(new Object[]{point, firstId + i});
            fc.add(builder.buildFeature(String.format("points.%03d", i)));
        }
        return fc;
    }
}
