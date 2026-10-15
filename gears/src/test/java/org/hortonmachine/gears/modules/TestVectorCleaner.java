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
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.FIELD_AREA;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.FIELD_ORIGID;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.FIELD_TYPE;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.TYPE_CUT;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.TYPE_FILL;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.TYPE_HOLE;
import static org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner.TYPE_INVALID;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.modules.v.vectorcleaner.OmsVectorCleaner;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Test {@link OmsVectorCleaner}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestVectorCleaner extends HMTestCase {

    private static final double AREA_DELTA = 0.01;

    public void testInvalidAndEmpty() throws Exception {
        SimpleFeatureCollection polygons = vector(Polygon.class, new String[]{"id"}, //
                "POLYGON ((0 0, 10 10, 10 0, 0 10, 0 0))", 1, // a bow-tie
                "POLYGON EMPTY", 2, //
                "POLYGON ((20 0, 30 0, 30 10, 20 10, 20 0))", 3);
        OmsVectorCleaner cleaner = new OmsVectorCleaner();
        cleaner.inMap = polygons;
        cleaner.process();

        assertEquals(MultiPolygon.class, cleaner.outMap.getSchema().getGeometryDescriptor().getType().getBinding());
        Map<Integer, SimpleFeature> byId = byId(cleaner.outMap);
        assertEquals(2, byId.size());
        Geometry bowTie = (Geometry) byId.get(1).getDefaultGeometry();
        assertTrue(bowTie.isValid());
        assertEquals(2, bowTie.getNumGeometries());
        assertEquals(50.0, bowTie.getArea(), DELTA);
        // the valid one is not touched
        assertTrue(((Geometry) byId.get(3).getDefaultGeometry()).getGeometryN(0)
                .equalsExact((Geometry) byId(polygons).get(3).getDefaultGeometry()));

        List<SimpleFeature> changes = FeatureUtilities.featureCollectionToList(cleaner.outChanges);
        assertEquals(1, changes.size());
        SimpleFeature change = changes.get(0);
        assertEquals(TYPE_INVALID, change.getAttribute(FIELD_TYPE));
        assertEquals(byId(polygons).get(1).getID(), change.getAttribute(FIELD_ORIGID));
        // the outline of the bow-tie, as it was
        Geometry outline = (Geometry) change.getDefaultGeometry();
        assertTrue(outline instanceof MultiLineString);
        assertEquals(2 * Math.sqrt(200) + 20, outline.getLength(), DELTA);
    }

    public void testHoles() throws Exception {
        SimpleFeatureCollection polygons = vector(Polygon.class, new String[]{"id"}, //
                "POLYGON ((0 0, 200 0, 200 200, 0 200, 0 0), " //
                        + "(20 20, 30 20, 30 30, 20 30, 20 20), " // small
                        + "(50 20, 52 20, 52 180, 50 180, 50 20), " // narrow
                        + "(100 100, 150 100, 150 150, 100 150, 100 100), " // big
                        + "(100 20, 130 20, 130 34, 160 34, 160 36, 130 36, 130 50, 100 50, 100 20))", // with a narrow tail
                1);
        OmsVectorCleaner cleaner = new OmsVectorCleaner();
        cleaner.inMap = polygons;
        cleaner.pMaxHoleArea = 150;
        cleaner.pMinWidth = 5;
        cleaner.process();

        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(cleaner.outMap);
        assertEquals(1, features.size());
        Geometry cleaned = (Geometry) features.get(0).getDefaultGeometry();
        assertTrue(cleaned.isValid());
        assertEquals(1, cleaned.getNumGeometries());
        Polygon polygon = (Polygon) cleaned.getGeometryN(0);
        // the big hole and the square of the one with the tail stay
        assertEquals(2, polygon.getNumInteriorRing());
        assertEquals(40000 - 2500 - 900, polygon.getArea(), AREA_DELTA);

        List<SimpleFeature> changes = FeatureUtilities.featureCollectionToList(cleaner.outChanges);
        assertEquals(3, changes.size());
        double changedArea = 0;
        for( SimpleFeature change : changes ) {
            assertEquals(TYPE_HOLE, change.getAttribute(FIELD_TYPE));
            changedArea += (Double) change.getAttribute(FIELD_AREA);
        }
        // the small, the narrow and the tail
        assertEquals(100 + 320 + 60, changedArea, AREA_DELTA);

        // without a width only the small hole is filled, the other rings keep their vertices
        cleaner = new OmsVectorCleaner();
        cleaner.inMap = polygons;
        cleaner.pMaxHoleArea = 150;
        cleaner.process();
        polygon = (Polygon) ((Geometry) FeatureUtilities.featureCollectionToList(cleaner.outMap).get(0).getDefaultGeometry())
                .getGeometryN(0);
        assertEquals(3, polygon.getNumInteriorRing());
        Polygon original = (Polygon) byId(polygons).get(1).getDefaultGeometry();
        assertTrue(polygon.getInteriorRingN(0).equalsExact(original.getInteriorRingN(1)));
        assertEquals(1, cleaner.outChanges.size());
    }

    public void testOuterSlivers() throws Exception {
        SimpleFeatureCollection polygons = vector(MultiPolygon.class, new String[]{"id"}, //
                "MULTIPOLYGON (((0 0, 50 0, 50 20, 90 20, 90 21, 50 21, 50 50, 0 50, 0 0)))", 1, // a spike
                "MULTIPOLYGON (((0 0, 50 0, 50 20, 10 20, 10 21, 50 21, 50 50, 0 50, 0 0)))", 2, // a notch
                "MULTIPOLYGON (((0 0, 50 0, 50 20, 40 20, 40 21, 50 21, 50 50, 0 50, 0 0)))", 3, // a short notch
                "MULTIPOLYGON (((0 0, 100 0, 100 1, 0 1, 0 0)))", 4, // a sliver itself
                "MULTIPOLYGON (((0 0, 50 0, 50 50, 0 50, 0 0)))", 5, //
                // two squares with a narrow gap between them
                "MULTIPOLYGON (((0 0, 50 0, 50 50, 0 50, 0 0)), ((51 0, 101 0, 101 50, 51 50, 51 0)))", 6);

        OmsVectorCleaner cleaner = new OmsVectorCleaner();
        cleaner.inMap = polygons;
        cleaner.pMinWidth = 5;
        cleaner.pMinSliverLength = 30;
        cleaner.process();

        Map<Integer, SimpleFeature> byId = byId(cleaner.outMap);
        assertNull("The sliver is removed.", byId.get(4));
        assertEquals(5, byId.size());
        assertEquals(2500, area(byId.get(1)), AREA_DELTA);
        assertEquals(2500, area(byId.get(2)), AREA_DELTA);
        assertEquals(2500 - 10, area(byId.get(3)), AREA_DELTA);
        // what has no sliver is not touched
        Map<Integer, SimpleFeature> inputById = byId(polygons);
        assertTrue(((Geometry) byId.get(3).getDefaultGeometry()).equalsExact((Geometry) inputById.get(3).getDefaultGeometry()));
        assertTrue(((Geometry) byId.get(5).getDefaultGeometry()).equalsExact((Geometry) inputById.get(5).getDefaultGeometry()));
        // the gap is filled, the two squares become one
        Geometry joined = (Geometry) byId.get(6).getDefaultGeometry();
        assertEquals(1, joined.getNumGeometries());
        assertEquals(101 * 50, joined.getArea(), AREA_DELTA);

        Map<String, Integer> typesCount = new HashMap<>();
        for( SimpleFeature change : FeatureUtilities.featureCollectionToList(cleaner.outChanges) ) {
            typesCount.merge((String) change.getAttribute(FIELD_TYPE), 1, Integer::sum);
        }
        // the spike and the sliver feature
        assertEquals(Integer.valueOf(2), typesCount.get(TYPE_CUT));
        // the notch and the gap
        assertEquals(Integer.valueOf(2), typesCount.get(TYPE_FILL));
        assertEquals(2, typesCount.size());
    }

    /**
     * Two squares that overlap along their border, one apart and a small one all inside the second.
     */
    private static SimpleFeatureCollection overlapping() throws Exception {
        SimpleFeatureCollection polygons = vector(Polygon.class, new String[]{"id"}, //
                "POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))", 1, //
                "POLYGON ((8 0, 28 0, 28 10, 8 10, 8 0))", 2, //
                "POLYGON ((30 0, 40 0, 40 10, 30 10, 30 0))", 3, //
                "POLYGON ((15 2, 17 2, 17 4, 15 4, 15 2))", 4);
        // the features come in the order of their ids: f1 to f4, as in a GeoPackage
        DefaultFeatureCollection ordered = new DefaultFeatureCollection("ordered", polygons.getSchema());
        for( SimpleFeature feature : FeatureUtilities.featureCollectionToList(polygons) ) {
            ordered.add(SimpleFeatureBuilder.build(polygons.getSchema(), feature.getAttributes(), "f" + feature.getAttribute("id")));
        }
        return ordered;
    }

    public void testOverlaps() throws Exception {
        SimpleFeatureCollection polygons = overlapping();

        OmsVectorCleaner cleaner = new OmsVectorCleaner();
        cleaner.inMap = polygons;
        cleaner.pOverlaps = OmsVectorCleaner.OVERLAPS_MARK;
        cleaner.process();
        Map<Integer, SimpleFeature> byId = byId(cleaner.outMap);
        assertEquals(4, byId.size());
        assertEquals(200, area(byId.get(2)), AREA_DELTA);
        List<SimpleFeature> changes = FeatureUtilities.featureCollectionToList(cleaner.outChanges);
        assertEquals(2, changes.size());
        Map<String, Double> overlaps = new HashMap<>();
        for( SimpleFeature change : changes ) {
            assertEquals(OmsVectorCleaner.TYPE_OVERLAP, change.getAttribute(FIELD_TYPE));
            overlaps.put((String) change.getAttribute(FIELD_ORIGID), (Double) change.getAttribute(FIELD_AREA));
        }
        // the ids of both features
        assertEquals(20, overlaps.get("f1;f2"), AREA_DELTA);
        assertEquals(4, overlaps.get("f2;f4"), AREA_DELTA);

        // the smaller gives the overlap away: the first square and the small one, that is removed
        cleaner = new OmsVectorCleaner();
        cleaner.inMap = polygons;
        cleaner.pOverlaps = OmsVectorCleaner.OVERLAPS_LARGEST;
        cleaner.process();
        byId = byId(cleaner.outMap);
        assertEquals(3, byId.size());
        assertNull(byId.get(4));
        assertEquals(80, area(byId.get(1)), AREA_DELTA);
        assertEquals(200, area(byId.get(2)), AREA_DELTA);
        assertEquals(100, area(byId.get(3)), AREA_DELTA);

        // the later gives the overlap away: the second square and again the small one
        cleaner = new OmsVectorCleaner();
        cleaner.inMap = polygons;
        cleaner.pOverlaps = OmsVectorCleaner.OVERLAPS_FIRST;
        cleaner.process();
        byId = byId(cleaner.outMap);
        assertEquals(3, byId.size());
        assertNull(byId.get(4));
        assertEquals(100, area(byId.get(1)), AREA_DELTA);
        assertEquals(180, area(byId.get(2)), AREA_DELTA);
        // no overlaps are left
        Geometry first = (Geometry) byId.get(1).getDefaultGeometry();
        Geometry second = (Geometry) byId.get(2).getDefaultGeometry();
        assertEquals(0.0, first.intersection(second).getArea(), DELTA);

        // the default doesn't look for them
        cleaner = new OmsVectorCleaner();
        cleaner.inMap = polygons;
        cleaner.process();
        assertEquals(4, cleaner.outMap.size());
        assertEquals(0, cleaner.outChanges.size());
    }

    public void testLinesAndPoints() throws Exception {
        OmsVectorCleaner cleaner = new OmsVectorCleaner();
        cleaner.inMap = vector(LineString.class, new String[]{"id"}, //
                "LINESTRING (0 0, 10 0)", 1, //
                "LINESTRING (5 5, 5 5)", 2, // collapsed, invalid
                "LINESTRING EMPTY", 3);
        // the polygon parameters are ignored
        cleaner.pMinWidth = 5;
        cleaner.process();
        Map<Integer, SimpleFeature> lines = byId(cleaner.outMap);
        assertEquals(1, lines.size());
        assertTrue(lines.get(1).getDefaultGeometry() instanceof MultiLineString);
        assertEquals(10.0, ((Geometry) lines.get(1).getDefaultGeometry()).getLength(), DELTA);
        List<SimpleFeature> changes = FeatureUtilities.featureCollectionToList(cleaner.outChanges);
        assertEquals(1, changes.size());
        assertEquals(TYPE_INVALID, changes.get(0).getAttribute(FIELD_TYPE));

        cleaner = new OmsVectorCleaner();
        cleaner.inMap = vector(Point.class, new String[]{"id"}, "POINT (1 1)", 1, "POINT EMPTY", 2);
        cleaner.process();
        Map<Integer, SimpleFeature> points = byId(cleaner.outMap);
        assertEquals(1, points.size());
        assertTrue(points.get(1).getDefaultGeometry() instanceof MultiPoint);
        assertEquals(0, cleaner.outChanges.size());
    }

    public void testWrongInputs() throws Exception {
        OmsVectorCleaner cleaner = new OmsVectorCleaner();
        cleaner.inMap = vector(Point.class, new String[]{"id"}, "POINT (1 1)", 1);
        cleaner.pMinWidth = -1;
        try {
            cleaner.process();
            fail("Negative width.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("negative"));
        }

        cleaner = new OmsVectorCleaner();
        cleaner.inMap = vector(Point.class, new String[]{"id"}, "POINT (1 1)", 1);
        cleaner.pPrecision = "km";
        try {
            cleaner.process();
            fail("Unknown precision.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("km"));
        }

        cleaner = new OmsVectorCleaner();
        cleaner.inMap = vector(Point.class, new String[]{"id"}, "POINT (1 1)", 1);
        cleaner.pOverlaps = "merge";
        try {
            cleaner.process();
            fail("Unknown overlaps handling.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("merge"));
        }
    }

    private static Map<Integer, SimpleFeature> byId( SimpleFeatureCollection fc ) {
        Map<Integer, SimpleFeature> map = new HashMap<>();
        for( SimpleFeature feature : FeatureUtilities.featureCollectionToList(fc) ) {
            map.put((Integer) feature.getAttribute("id"), feature);
        }
        return map;
    }

    private static double area( SimpleFeature feature ) {
        return ((Geometry) feature.getDefaultGeometry()).getArea();
    }
}
