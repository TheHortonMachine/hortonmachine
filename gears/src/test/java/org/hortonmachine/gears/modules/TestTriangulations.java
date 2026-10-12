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
import java.util.Random;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.modules.v.triangulation.OmsDelaunayTriangulation;
import org.hortonmachine.gears.modules.v.triangulation.OmsVoronoiDiagram;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Triangle;

/**
 * Test {@link OmsDelaunayTriangulation} and {@link OmsVoronoiDiagram}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestTriangulations extends HMTestCase {

    /**
     * The corners of a square of 10 m and its center, with an integer elevation and a name.
     */
    private static SimpleFeatureCollection squareAndCenter() {
        return points(new double[][]{{0, 0, 100}, {10, 0, 200}, {10, 10, 300}, {0, 10, 400}, {5, 5, 250}},
                new String[]{"sw", "se", "ne", "nw", "c"});
    }

    private static SimpleFeatureCollection points( double[][] xyElev, String[] names ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("points");
        b.setCRS(HMTestMaps.getCrs());
        b.add("the_geom", Point.class);
        b.add("elev", Integer.class);
        b.add("name", String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        for( int i = 0; i < xyElev.length; i++ ) {
            Point point = GeometryUtilities.gf().createPoint(new Coordinate(xyElev[i][0], xyElev[i][1]));
            builder.addAll(new Object[]{point, (int) xyElev[i][2], names[i]});
            // the collection is sorted by id: keep the order of the points
            fc.add(builder.buildFeature(String.format("points.%03d", i)));
        }
        return fc;
    }

    public void testDelaunayWithElevationField() throws Exception {
        SimpleFeatureCollection inFC = squareAndCenter();

        OmsDelaunayTriangulation delaunay = new OmsDelaunayTriangulation();
        delaunay.pm = pm;
        delaunay.inMap = inFC;
        delaunay.fElev = "ELEV";
        delaunay.process();

        List<SimpleFeature> triangles = FeatureUtilities.featureCollectionToList(delaunay.outMap);
        // the center joined to the four sides
        assertEquals(4, triangles.size());
        double area = 0;
        for( SimpleFeature triangle : triangles ) {
            Geometry geometry = (Geometry) triangle.getDefaultGeometry();
            area += geometry.getArea();
            assertEquals(25.0, geometry.getArea(), DELTA);
            // all triangles have the center
            Coordinate[] coordinates = geometry.getCoordinates();
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            boolean hasCenter = false;
            for( Coordinate c : coordinates ) {
                // the vertices have the elevation of their point as z
                assertEquals(elevationOf(c), c.z, DELTA);
                min = Math.min(min, c.z);
                max = Math.max(max, c.z);
                hasCenter = hasCenter || c.equals2D(new Coordinate(5, 5));
            }
            assertTrue(hasCenter);
            assertEquals(min, (Double) triangle.getAttribute(OmsDelaunayTriangulation.MINELEV), DELTA);
            assertEquals(max, (Double) triangle.getAttribute(OmsDelaunayTriangulation.MAXELEV), DELTA);
        }
        assertEquals(100.0, area, DELTA);

        // the input points are not changed
        for( Geometry point : FeatureUtilities.featureCollectionToGeometriesList(inFC, false, null) ) {
            assertTrue(Double.isNaN(point.getCoordinate().z));
        }
    }

    private static double elevationOf( Coordinate c ) {
        if (c.equals2D(new Coordinate(0, 0)))
            return 100;
        if (c.equals2D(new Coordinate(10, 0)))
            return 200;
        if (c.equals2D(new Coordinate(10, 10)))
            return 300;
        if (c.equals2D(new Coordinate(0, 10)))
            return 400;
        return 250;
    }

    public void testDelaunayWithoutElevation() throws Exception {
        OmsDelaunayTriangulation delaunay = new OmsDelaunayTriangulation();
        delaunay.pm = pm;
        delaunay.inMap = squareAndCenter();
        delaunay.process();

        // 2D points without elevation field: only the triangles
        assertEquals(4, delaunay.outMap.size());
        assertEquals(1, delaunay.outMap.getSchema().getAttributeCount());
    }

    public void testDelaunayMissingField() throws Exception {
        OmsDelaunayTriangulation delaunay = new OmsDelaunayTriangulation();
        delaunay.pm = pm;
        delaunay.inMap = squareAndCenter();
        delaunay.fElev = "height";
        try {
            delaunay.process();
            fail("A missing elevation field must be reported.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("height"));
        }
    }

    /**
     * The circle through the vertices of each triangle contains no other point.
     */
    public void testDelaunayEmptyCircles() throws Exception {
        Random random = new Random(7);
        int n = 60;
        double[][] xyElev = new double[n][];
        String[] names = new String[n];
        for( int i = 0; i < n; i++ ) {
            xyElev[i] = new double[]{random.nextDouble() * 1000, random.nextDouble() * 1000, random.nextInt(500)};
            names[i] = "p" + i;
        }
        OmsDelaunayTriangulation delaunay = new OmsDelaunayTriangulation();
        delaunay.pm = pm;
        delaunay.inMap = points(xyElev, names);
        delaunay.process();

        List<Geometry> triangles = FeatureUtilities.featureCollectionToGeometriesList(delaunay.outMap, false, null);
        // the triangles cover the convex hull of the points
        Geometry hull = FeatureUtilities.featureCollectionToGeometriesList(delaunay.inMap, false, null).stream()
                .reduce(Geometry::union).get().convexHull();
        double area = triangles.stream().mapToDouble(Geometry::getArea).sum();
        assertEquals(hull.getArea(), area, 1E-6);

        for( Geometry geometry : triangles ) {
            Coordinate[] c = geometry.getCoordinates();
            Coordinate center = Triangle.circumcentre(c[0], c[1], c[2]);
            double radius = center.distance(c[0]);
            for( double[] p : xyElev ) {
                assertTrue(center.distance(new Coordinate(p[0], p[1])) > radius - 1E-6);
            }
        }
    }

    public void testVoronoi() throws Exception {
        // a duplicate of the center, with another name
        SimpleFeatureCollection inFC = points(
                new double[][]{{0, 0, 100}, {10, 0, 200}, {10, 10, 300}, {0, 10, 400}, {5, 5, 250}, {5, 5, 999}},
                new String[]{"sw", "se", "ne", "nw", "c", "duplicate"});

        OmsVoronoiDiagram voronoi = new OmsVoronoiDiagram();
        voronoi.pm = pm;
        voronoi.inMap = inFC;
        voronoi.process();

        List<SimpleFeature> cells = FeatureUtilities.featureCollectionToList(voronoi.outMap);
        assertEquals(5, cells.size());
        for( SimpleFeature cell : cells ) {
            Geometry geometry = (Geometry) cell.getDefaultGeometry();
            String name = (String) cell.getAttribute("name");
            int elev = (Integer) cell.getAttribute("elev");
            // each cell has the attributes of the point inside it
            Coordinate site = switch( name ) {
            case "sw" -> new Coordinate(0, 0);
            case "se" -> new Coordinate(10, 0);
            case "ne" -> new Coordinate(10, 10);
            case "nw" -> new Coordinate(0, 10);
            case "c" -> new Coordinate(5, 5);
            default -> throw new AssertionError("Unexpected cell: " + name);
            };
            assertTrue(geometry.contains(GeometryUtilities.gf().createPoint(site)));
            assertEquals(elevationOf(site), elev, DELTA);
            if (name.equals("c")) {
                // the diamond between the axes of the sides
                assertEquals(50.0, geometry.getArea(), DELTA);
            }
        }

        // cut on the bounds of the points, enlarged by their diagonal
        ReferencedEnvelope bounds = voronoi.outMap.getBounds();
        double diagonal = Math.sqrt(200);
        assertEquals(-diagonal, bounds.getMinX(), DELTA);
        assertEquals(10 + diagonal, bounds.getMaxX(), DELTA);
        assertEquals(-diagonal, bounds.getMinY(), DELTA);
        assertEquals(10 + diagonal, bounds.getMaxY(), DELTA);
    }

    private static final double DELTA = 1E-9;
}
