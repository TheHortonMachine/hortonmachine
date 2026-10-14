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

import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.modules.v.vectorclipper.OmsVectorClipper;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

/**
 * Test {@link OmsVectorClipper}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestVectorClipper extends HMTestCase {

    private static final WKTReader WKT = new WKTReader();

    /**
     * Two adjacent squares and a third one far from the features, that touches none of them.
     */
    static SimpleFeatureCollection squares() throws Exception {
        return vector(Polygon.class, new String[]{"id", "name"}, //
                "POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))", 1, "a", //
                "POLYGON ((10 0, 20 0, 20 10, 10 10, 10 0))", 2, "b", //
                "POLYGON ((200 200, 210 200, 210 210, 200 210, 200 200))", 3, "c");
    }

    /**
     * A river crossing both squares, a line far from them and one that touches a square in a corner.
     */
    static SimpleFeatureCollection lines() throws Exception {
        return vector(LineString.class, new String[]{"id", "name"}, //
                "LINESTRING (-5 5, 25 5)", 7, "river", //
                "LINESTRING (100 100, 110 100)", 8, "far", //
                "LINESTRING (-5 15, 0 10)", 9, "touching");
    }

    public void testClipLines() throws Exception {
        SimpleFeatureCollection lines = lines();
        OmsVectorClipper clipper = new OmsVectorClipper();
        clipper.inMap = lines;
        clipper.inClipper = squares();
        clipper.process();

        assertEquals(MultiLineString.class, clipper.outMap.getSchema().getGeometryDescriptor().getType().getBinding());
        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(clipper.outMap);
        // the river once, with its part inside the two squares
        assertEquals(1, features.size());
        SimpleFeature river = features.get(0);
        assertEquals(7, river.getAttribute("id"));
        assertEquals("river", river.getAttribute("name"));
        Geometry geometry = (Geometry) river.getDefaultGeometry();
        assertEquals(20.0, geometry.getLength(), DELTA);
        assertTrue(geometry.getEnvelopeInternal().getMinX() == 0 && geometry.getEnvelopeInternal().getMaxX() == 20);

        // the input is not changed
        for( SimpleFeature feature : FeatureUtilities.featureCollectionToList(lines) ) {
            if (feature.getAttribute("id").equals(7)) {
                assertEquals(30.0, ((Geometry) feature.getDefaultGeometry()).getLength(), DELTA);
            }
        }
    }

    public void testClipPointsAndPolygons() throws Exception {
        OmsVectorClipper clipper = new OmsVectorClipper();
        clipper.inMap = vector(Point.class, new String[]{"id"}, "POINT (5 5)", 1, "POINT (50 5)", 2);
        clipper.inClipper = squares();
        clipper.process();
        List<SimpleFeature> points = FeatureUtilities.featureCollectionToList(clipper.outMap);
        assertEquals(1, points.size());
        assertTrue(points.get(0).getDefaultGeometry() instanceof MultiPoint);
        assertEquals(1, points.get(0).getAttribute("id"));

        clipper = new OmsVectorClipper();
        clipper.inMap = vector(Polygon.class, new String[]{"id"}, "POLYGON ((5 -5, 15 -5, 15 5, 5 5, 5 -5))", 1);
        clipper.inClipper = squares();
        clipper.process();
        List<SimpleFeature> polygons = FeatureUtilities.featureCollectionToList(clipper.outMap);
        assertEquals(1, polygons.size());
        // the half inside the squares, in one piece across the two
        Geometry polygon = (Geometry) polygons.get(0).getDefaultGeometry();
        assertEquals(50.0, polygon.getArea(), DELTA);
        assertEquals(1, polygon.getNumGeometries());
    }

    public void testNoFeaturesInClipper() throws Exception {
        // a single polygon that touches no feature: the old version waited forever
        OmsVectorClipper clipper = new OmsVectorClipper();
        clipper.inMap = lines();
        clipper.inClipper = vector(Polygon.class, new String[]{"id"}, "POLYGON ((500 500, 510 500, 510 510, 500 510, 500 500))", 1);
        clipper.process();
        assertEquals(0, clipper.outMap.size());
    }

    public void testWrongInputs() throws Exception {
        OmsVectorClipper clipper = new OmsVectorClipper();
        clipper.inMap = squares();
        clipper.inClipper = lines();
        try {
            clipper.process();
            fail("Lines can't be used to clip.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("polygons"));
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("utm");
        b.setCRS(HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"));
        b.add("the_geom", Polygon.class);
        DefaultFeatureCollection utm = new DefaultFeatureCollection("utm", b.buildFeatureType());
        clipper = new OmsVectorClipper();
        clipper.inMap = lines();
        clipper.inClipper = utm;
        try {
            clipper.process();
            fail("The crs differ.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("coordinate reference systems"));
        }
    }

    /**
     * A vector in EPSG:32633 from wkt geometries, each followed by its attributes.
     */
    static SimpleFeatureCollection vector( Class< ? > geometryClass, String[] fields, Object... wktAndValues ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(HMCrsRegistry.INSTANCE.getCrs("EPSG:32633"));
        b.add("the_geom", geometryClass);
        int fieldsCount = fields.length;
        for( int i = 0; i < fieldsCount; i++ ) {
            b.add(fields[i], wktAndValues[i + 1].getClass());
        }
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new DefaultFeatureCollection("test", type);
        for( int i = 0; i < wktAndValues.length; i += fieldsCount + 1 ) {
            builder.add(WKT.read((String) wktAndValues[i]));
            for( int j = 1; j <= fieldsCount; j++ ) {
                builder.add(wktAndValues[i + j]);
            }
            fc.add(builder.buildFeature(null));
        }
        return fc;
    }
}
