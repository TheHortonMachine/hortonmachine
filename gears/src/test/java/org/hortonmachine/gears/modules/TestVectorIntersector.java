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
package org.hortonmachine.gears.modules;

import static org.hortonmachine.gears.modules.TestVectorClipper.lines;
import static org.hortonmachine.gears.modules.TestVectorClipper.squares;
import static org.hortonmachine.gears.modules.TestVectorClipper.vector;

import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorIntersector;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Polygon;

/**
 * Test {@link OmsVectorIntersector}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestVectorIntersector extends HMTestCase {

    public void testIntersectLines() throws Exception {
        OmsVectorIntersector intersector = new OmsVectorIntersector();
        intersector.inMap1 = lines();
        intersector.inMap2 = squares();
        intersector.process();

        SimpleFeatureType type = intersector.outMap.getSchema();
        assertEquals(MultiLineString.class, type.getGeometryDescriptor().getType().getBinding());
        // the fields of the squares with the names of those of the lines get the suffix
        assertEquals(5, type.getAttributeCount());
        assertEquals("id", type.getDescriptor(1).getLocalName());
        assertEquals("name", type.getDescriptor(2).getLocalName());
        assertEquals("id_2", type.getDescriptor(3).getLocalName());
        assertEquals("name_2", type.getDescriptor(4).getLocalName());

        // the river once for each square, the line touching a corner left out
        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(intersector.outMap);
        assertEquals(2, features.size());
        for( SimpleFeature feature : features ) {
            assertEquals(7, feature.getAttribute("id"));
            assertEquals("river", feature.getAttribute("name"));
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            assertEquals(10.0, geometry.getLength(), DELTA);
            double minX = geometry.getEnvelopeInternal().getMinX();
            if (feature.getAttribute("id_2").equals(1)) {
                assertEquals("a", feature.getAttribute("name_2"));
                assertEquals(0.0, minX, DELTA);
            } else {
                assertEquals(2, feature.getAttribute("id_2"));
                assertEquals("b", feature.getAttribute("name_2"));
                assertEquals(10.0, minX, DELTA);
            }
        }
    }

    public void testIntersectPolygons() throws Exception {
        OmsVectorIntersector intersector = new OmsVectorIntersector();
        intersector.inMap1 = vector(Polygon.class, new String[]{"code"}, "POLYGON ((5 -5, 15 -5, 15 5, 5 5, 5 -5))", "x");
        intersector.inMap2 = squares();
        intersector.process();
        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(intersector.outMap);
        // a piece for each square, without clashing names
        assertEquals(2, features.size());
        for( SimpleFeature feature : features ) {
            assertEquals("x", feature.getAttribute("code"));
            assertNotNull(feature.getAttribute("id"));
            assertEquals(25.0, ((Geometry) feature.getDefaultGeometry()).getArea(), DELTA);
        }
    }

    public void testNotPolygons() throws Exception {
        OmsVectorIntersector intersector = new OmsVectorIntersector();
        intersector.inMap1 = squares();
        intersector.inMap2 = lines();
        try {
            intersector.process();
            fail("Lines can't be used to intersect.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("polygons"));
        }
    }
}
