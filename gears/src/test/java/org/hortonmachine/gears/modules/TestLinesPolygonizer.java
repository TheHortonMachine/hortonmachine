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

import static org.hortonmachine.gears.modules.TestVectorClipper.vector;

import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/**
 * Test {@link OmsLinesPolygonizer}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestLinesPolygonizer extends HMTestCase {

    /**
     * A square of 20 m made of 4 sides and a line crossing it in the middle, without vertexes on
     * the sides: only the noding makes it split the square.
     */
    private static SimpleFeatureCollection squareAndCross() throws Exception {
        return vector(LineString.class, new String[]{"id"}, //
                "LINESTRING (0 0, 20 0)", 1, //
                "LINESTRING (20 0, 20 20)", 2, //
                "LINESTRING (20 20, 0 20)", 3, //
                "LINESTRING (0 20, 0 0)", 4, //
                "LINESTRING (10 -5, 10 25)", 5);
    }

    public void testFaces() throws Exception {
        OmsLinesPolygonizer polygonizer = new OmsLinesPolygonizer();
        polygonizer.inMap = squareAndCross();
        polygonizer.process();

        List<SimpleFeature> polygons = FeatureUtilities.featureCollectionToList(polygonizer.outMap);
        assertEquals(2, polygons.size());
        double area = 0;
        for( SimpleFeature polygon : polygons ) {
            Geometry geometry = (Geometry) polygon.getDefaultGeometry();
            assertEquals(200.0, geometry.getArea(), DELTA);
            area += geometry.getArea();
        }
        assertEquals(400.0, area, DELTA);
        assertEquals(Integer.class, polygonizer.outMap.getSchema().getDescriptor("id").getType().getBinding());
    }

    public void testFacesIdFromPoints() throws Exception {
        OmsLinesPolygonizer polygonizer = new OmsLinesPolygonizer();
        polygonizer.inMap = squareAndCross();
        polygonizer.inPoints = vector(Point.class, new String[]{"name"}, //
                "POINT (5 5)", "west", //
                "POINT (50 50)", "outside");
        polygonizer.fId = "name";
        polygonizer.fNewId = "parcel";
        polygonizer.process();

        List<SimpleFeature> polygons = FeatureUtilities.featureCollectionToList(polygonizer.outMap);
        assertEquals(2, polygons.size());
        for( SimpleFeature polygon : polygons ) {
            Geometry geometry = (Geometry) polygon.getDefaultGeometry();
            Object parcel = polygon.getAttribute("parcel");
            if (geometry.getCentroid().getX() < 10) {
                assertEquals("west", parcel);
            } else {
                assertNull(parcel);
            }
        }
    }

    public void testJoin() throws Exception {
        SimpleFeatureCollection lines = vector(LineString.class, new String[]{"group"}, //
                // a square with a gap and a side drawn backwards
                "LINESTRING (0 0, 10 0)", "a", //
                "LINESTRING (10 0.1, 10 10)", "a", //
                "LINESTRING (0 10, 10 10)", "a", //
                // a bowtie, it crosses itself
                "LINESTRING (30 0, 40 10)", "b", //
                "LINESTRING (40 0, 30 10)", "b");

        OmsLinesPolygonizer polygonizer = new OmsLinesPolygonizer();
        polygonizer.inMap = lines;
        polygonizer.pMode = OmsLinesPolygonizer.MODE_JOIN;
        polygonizer.fGroup = "group";
        polygonizer.process();

        List<SimpleFeature> polygons = FeatureUtilities.featureCollectionToList(polygonizer.outMap);
        assertEquals(1, polygons.size());
        SimpleFeature polygon = polygons.get(0);
        assertEquals("a", polygon.getAttribute("id"));
        Geometry geometry = (Geometry) polygon.getDefaultGeometry();
        assertTrue(geometry.isValid());
        assertEquals(100.0, geometry.getArea(), DELTA);

        // the faces of the same lines: the gap leaves the square open
        polygonizer = new OmsLinesPolygonizer();
        polygonizer.inMap = lines;
        polygonizer.process();
        assertEquals(0, polygonizer.outMap.size());
    }

    public void testWrongInputs() throws Exception {
        OmsLinesPolygonizer polygonizer = new OmsLinesPolygonizer();
        polygonizer.inMap = vector(Point.class, new String[]{"id"}, "POINT (5 5)", 1);
        try {
            polygonizer.process();
            fail("points are not lines");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("line"));
        }

        polygonizer = new OmsLinesPolygonizer();
        polygonizer.inMap = squareAndCross();
        polygonizer.pMode = "rings";
        try {
            polygonizer.process();
            fail("unknown mode");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("mode"));
        }
    }

}
