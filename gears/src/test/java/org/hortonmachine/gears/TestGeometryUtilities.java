package org.hortonmachine.gears;

import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.angleBetween3D;
import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.getAngleBetweenLinePlane;
import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.getTriangleCentroid;
import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.getTriangleWindingRule;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

/**
 * Test {@link GeometryUtilities}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGeometryUtilities extends HMTestCase {

    private static final String TWO_BALLS = "POLYGON ((1.7999999999999998 1.29391606655914, 1.8928932188134537 1.4071067811865485, 2.0444297669803992 1.5314696123025462, 2.2173165676349122 1.6238795325112876, 2.404909677983874 1.6807852804032308, 2.6000000000000028 1.7, 2.795090322016131 1.6807852804032297, 2.9826834323650924 1.6238795325112856, 3.155570233019605 1.5314696123025433, 3.30710678118655 1.407106781186545, 3.431469612302547 1.2555702330195992, 3.5238795325112884 1.0826834323650862, 3.5807852804032314 0.8950903220161244, 3.6 0.7, 3.5807852804032305 0.5049096779838718, 3.5238795325112866 0.3173165676349102, 3.4314696123025454 0.1444297669803978, 3.3071067811865476 -0.0071067811865475, 3.1555702330196023 -0.1314696123025453, 2.98268343236509 -0.2238795325112868, 2.7950903220161285 -0.2807852804032305, 2.6 -0.3, 2.4049096779838717 -0.2807852804032305, 2.2173165676349105 -0.2238795325112868, 2.0444297669803984 -0.1314696123025454, 1.8928932188134526 -0.0071067811865476, 1.8 0.1060839334408596, 1.7071067811865475 -0.0071067811865475, 1.5555702330196022 -0.1314696123025453, 1.3826834323650898 -0.2238795325112868, 1.1950903220161284 -0.2807852804032305, 1 -0.3, 0.8049096779838718 -0.2807852804032305, 0.6173165676349103 -0.2238795325112868, 0.444429766980398 -0.1314696123025454, 0.2928932188134525 -0.0071067811865476, 0.1685303876974546 0.1444297669803978, 0.0761204674887132 0.3173165676349105, 0.0192147195967695 0.5049096779838722, 0 0.7000000000000007, 0.0192147195967698 0.8950903220161291, 0.0761204674887137 1.082683432365091, 0.1685303876974555 1.2555702330196032, 0.2928932188134536 1.4071067811865485, 0.4444297669803993 1.5314696123025462, 0.6173165676349122 1.6238795325112876, 0.8049096779838739 1.6807852804032308, 1.0000000000000024 1.7, 1.1950903220161309 1.6807852804032297, 1.3826834323650925 1.6238795325112856, 1.5555702330196048 1.5314696123025433, 1.70710678118655 1.407106781186545, 1.7999999999999998 1.29391606655914))";
    private static final String IRREGULAR_POLYGON = "POLYGON ((1.5 2.2, -1.3 0.5, -0.9 -0.7, 2.1 -0.9, -0.2 -2.2, 4.9 -3, 5.9 -0.6, 3.3 -1.7, 6.3 1.2, 2.7 0.6, 2.8 2.1, 1.1 0, 0.1 0.3, 0 0.7, 1.5 2.2))";
    private static final String RECTANGLE = "POLYGON ((0 10, 10 10, 10 0, 0 0, 0 10))";
    private static final String HEXAGON = "POLYGON ((1 0.5, 0.75 0.9330127018922193, 0.2500000000000001 0.9330127018922194, 0 0.5000000000000001, 0.2499999999999998 0.0669872981077808, 0.7499999999999997 0.0669872981077805, 1 0.5))";
    private static final String EQUILATERAL_TRIANGLE = "POLYGON ((1 2, 3 4, 0.267949 4.73205, 1 2))";

    private Coordinate ll;
    private Coordinate ul;
    private Coordinate ur;
    private Coordinate lr;
    private Coordinate negll;
    private Coordinate negul;

    protected void setUp() throws Exception {
        ll = new Coordinate(0, 0);
        ul = new Coordinate(0, 1);
        ur = new Coordinate(1, 1);
        lr = new Coordinate(1, 0);
        negll = new Coordinate(-1, 0);
        negul = new Coordinate(-1, 1);
    }

    public void testCoordinateAzimuth() throws Exception {
        double azimuth = GeometryUtilities.azimuth(ll, ur);
        assertEquals(45.0, azimuth, DELTA);
        azimuth = GeometryUtilities.azimuth(ul, lr);
        assertEquals(135.0, azimuth, DELTA);
        azimuth = GeometryUtilities.azimuth(ur, ll);
        assertEquals(225.0, azimuth, DELTA);
        azimuth = GeometryUtilities.azimuth(lr, ul);
        assertEquals(315.0, azimuth, DELTA);
        azimuth = GeometryUtilities.azimuth(ll, ul);
        assertEquals(0.0, azimuth, DELTA);
        azimuth = GeometryUtilities.azimuth(ul, ll);
        assertEquals(180.0, azimuth, DELTA);
        azimuth = GeometryUtilities.azimuth(ll, lr);
        assertEquals(90.0, azimuth, DELTA);
        azimuth = GeometryUtilities.azimuth(lr, ll);
        assertEquals(270.0, azimuth, DELTA);
    }

    public void testAnglesAndAzimuth() throws Exception {
        LineSegment ls1 = new LineSegment(ll, ul);
        LineSegment ls2 = new LineSegment(ll, ur);
        double angleBetween = GeometryUtilities.angleBetween(ls1, ls2);
        assertEquals(45.0, angleBetween, DELTA);

        ls1 = new LineSegment(ul, ur);
        ls2 = new LineSegment(ul, negll);
        angleBetween = GeometryUtilities.angleBetween(ls1, ls2);
        assertEquals(135.0, angleBetween, DELTA);

        ls1 = new LineSegment(ll, ur);
        ls2 = new LineSegment(ll, negul);
        angleBetween = GeometryUtilities.angleBetween(ls1, ls2);
        assertEquals(270.0, angleBetween, DELTA);

        ls1 = new LineSegment(lr, ul);
        ls2 = new LineSegment(lr, ll);
        angleBetween = GeometryUtilities.angleBetween(ls1, ls2);
        assertEquals(315.0, angleBetween, DELTA);
    }

    public void testDistanceAzimuth() throws Exception {
        double sq2 = 2 * Math.sqrt(2);
        double sq5 = Math.sqrt(5);
        double[] distances = {2, sq5, sq2, 2, sq5, sq2, 2, sq2, 2, sq2, 2};
        double d = 26.5650511771;
        double[] angles = {0, d, 45, 90, 90 + d, 135, 180, 225, 270, 315, 360};
        Coordinate[] expectedCoords = {//
                new Coordinate(1, 3), //
                new Coordinate(2, 3), //
                new Coordinate(3, 3), //
                new Coordinate(3, 1), //
                new Coordinate(3, 0), //
                new Coordinate(3, -1), //
                new Coordinate(1, -1), //
                new Coordinate(-1, -1), //
                new Coordinate(-1, 1), //
                new Coordinate(-1, 3), //
                new Coordinate(1, 3), //
        };
        Coordinate start = new Coordinate(1, 1);
        for( int i = 0; i < distances.length; i++ ) {

            double angle = angles[i];
            double distance = distances[i];

            Coordinate end = GeometryUtilities.getCoordinateAtAzimuthDistance(start, angle, distance);

            Coordinate expectedC = expectedCoords[i];
            assertEquals(expectedC.x, end.x, 0.000001);
            assertEquals(expectedC.y, end.y, 0.000001);

        }
    }

    public void testLineMerger() throws Exception {
        String l1 = "LINESTRING (0 300, 200 300, 200 200)";
        String l2 = "LINESTRING (200 0, 200 200)";
        String l3 = "LINESTRING (50 100, 250 100, 300 100)";
        String l4 = "LINESTRING (300 100, 300 0)";
        String l5 = "LINESTRING (50 100, 50 0, 200 0)";

        WKTReader r = new WKTReader();
        LineString g1 = (LineString) r.read(l1);
        LineString g2 = (LineString) r.read(l2);
        LineString g3 = (LineString) r.read(l3);
        LineString g4 = (LineString) r.read(l4);
        LineString g5 = (LineString) r.read(l5);

        List<LineString> mergeLinestrings = GeometryUtilities.mergeLinestrings(Arrays.asList(g1, g2, g3, g4));
        assertEquals(2, mergeLinestrings.size());
        mergeLinestrings = GeometryUtilities.mergeLinestrings(Arrays.asList(g1, g2, g3, g4, g5));
        assertEquals(1, mergeLinestrings.size());

    }

    public void testLines2Polygon() throws Exception {
        GeometryFactory gf = GeometryUtilities.gf();

        LineString l1 = gf.createLineString(new Coordinate[]{negll, negul, ul});
        LineString l2 = gf.createLineString(new Coordinate[]{ur, lr});
        LineString l3 = gf.createLineString(new Coordinate[]{ll, lr});

        Polygon lines2Polygon = GeometryUtilities.lines2Polygon(true, l1, l2, l3);

        Coordinate[] polygonCoord = new Coordinate[]{negll, negul, ul, ur, lr, lr, ll, negll};

        LinearRing linearRing = gf.createLinearRing(polygonCoord);
        Polygon expectedPolygon = gf.createPolygon(linearRing, null);
        assertTrue(lines2Polygon.equalsExact(expectedPolygon));
    }

    public void testGetCoordinatesAtInterval() throws Exception {
        GeometryFactory gf = GeometryUtilities.gf();

        LineString l1 = gf.createLineString(new Coordinate[]{ll, ul, ur, lr});
        List<Coordinate> coordinatesAtInterval = GeometryUtilities.getCoordinatesAtInterval(l1, 0.5, false, -1, -1);

        List<Coordinate> expectedCoordinates = new ArrayList<Coordinate>();
        Coordinate c = new Coordinate(0.0, 0.0);
        expectedCoordinates.add(c);
        c = new Coordinate(0.0, 0.5);
        expectedCoordinates.add(c);
        c = new Coordinate(0.0, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(0.5, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 0.5);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 0.0);
        expectedCoordinates.add(c);

        for( int i = 0; i < coordinatesAtInterval.size(); i++ ) {
            assertTrue(coordinatesAtInterval.get(i).distance(expectedCoordinates.get(i)) < DELTA);
        }

        coordinatesAtInterval = GeometryUtilities.getCoordinatesAtInterval(l1, 0.5, false, 0.5, 2.5);

        expectedCoordinates = new ArrayList<Coordinate>();
        c = new Coordinate(0.0, 0.5);
        expectedCoordinates.add(c);
        c = new Coordinate(0.0, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(0.5, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 0.5);
        expectedCoordinates.add(c);

        for( int i = 0; i < coordinatesAtInterval.size(); i++ ) {
            assertTrue(coordinatesAtInterval.get(i).distance(expectedCoordinates.get(i)) < DELTA);
        }

        coordinatesAtInterval = GeometryUtilities.getCoordinatesAtInterval(l1, 0.7, true, -1, -1);

        expectedCoordinates = new ArrayList<Coordinate>();
        c = new Coordinate(0.0, 0.0);
        expectedCoordinates.add(c);
        c = new Coordinate(0.0, 0.7);
        expectedCoordinates.add(c);
        c = new Coordinate(0.0, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(0.4, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 1.0);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 0.9);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 0.2);
        expectedCoordinates.add(c);
        c = new Coordinate(1.0, 0.0);
        expectedCoordinates.add(c);

        for( int i = 0; i < coordinatesAtInterval.size(); i++ ) {
            assertTrue(coordinatesAtInterval.get(i).distance(expectedCoordinates.get(i)) < DELTA);
        }

        // List<LineString> sectionsFromCoordinates =
        // GeometryUtilities.getSectionsFromCoordinates(coordinatesAtInterval, 0.4);
        // MultiLineString createMultiLineString =
        // gf.createMultiLineString(sectionsFromCoordinates.toArray(new LineString[0]));
        // System.out.println(createMultiLineString);

    }

    @SuppressWarnings("nls")
    public void testGeomChecks() {

        Polygon dummyPolygon = GeometryUtilities.createDummyPolygon();
        assertTrue(EGeometryType.isPolygon(dummyPolygon));

        LineString dummyLine = GeometryUtilities.createDummyLine();
        assertTrue(EGeometryType.isLine(dummyLine));
        assertFalse(EGeometryType.isLine(dummyPolygon));

        Point dummyPoint = GeometryUtilities.createDummyPoint();
        assertTrue(EGeometryType.isPoint(dummyPoint));
        assertFalse(EGeometryType.isLine(dummyPoint));
        assertFalse(EGeometryType.isPolygon(dummyPoint));

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("dummy");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Polygon.class);
        SimpleFeatureType polygonType = b.buildFeatureType();
        assertTrue(EGeometryType.isPolygon(polygonType.getGeometryDescriptor()));

        b = new SimpleFeatureTypeBuilder();
        b.setName("dummy");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", LineString.class);
        SimpleFeatureType lineType = b.buildFeatureType();
        assertTrue(EGeometryType.isLine(lineType.getGeometryDescriptor()));
        assertFalse(EGeometryType.isPolygon(lineType.getGeometryDescriptor()));

        b = new SimpleFeatureTypeBuilder();
        b.setName("dummy");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        SimpleFeatureType pointType = b.buildFeatureType();
        assertTrue(EGeometryType.isPoint(pointType.getGeometryDescriptor()));
        assertFalse(EGeometryType.isLine(pointType.getGeometryDescriptor()));
        assertFalse(EGeometryType.isPolygon(pointType.getGeometryDescriptor()));
    }

    public void testRectangleAdaptor() {
        Rectangle2D r1 = new Rectangle2D.Double(0, 0, 100, 50);
        Rectangle2D r2 = new Rectangle2D.Double(0, 0, 10, 10);

        GeometryUtilities.scaleToRatio(r1, r2, false);
        assertEquals(20, (int) r2.getWidth());
        assertEquals(-5, (int) r2.getX());
        assertEquals(10, (int) r2.getHeight());
        assertEquals(0, (int) r2.getY());

        r2 = new Rectangle2D.Double(0, 0, 10, 10);
        GeometryUtilities.scaleToRatio(r1, r2, true);
        assertEquals(10, (int) r2.getWidth());
        assertEquals(0, (int) r2.getX());
        assertEquals(5, (int) r2.getHeight());
        assertEquals(2.5, r2.getY());

        Rectangle2D r11 = new Rectangle2D.Double(0, 0, 20, 60);
        Rectangle2D r22 = new Rectangle2D.Double(0, 0, 10, 10);
        GeometryUtilities.scaleToRatio(r11, r22, false);
        assertEquals(10, (int) r22.getWidth());
        assertEquals(0, (int) r22.getX());
        assertEquals(30, (int) r22.getHeight());
        assertEquals(-10, (int) r22.getY());
    }

    public void testRectangleFitter() {
        Rectangle2D r1 = new Rectangle2D.Double(0, 0, 100, 50);
        Rectangle2D r2 = new Rectangle2D.Double(0, 0, 10, 10);

        GeometryUtilities.scaleDownToFit(r1, r2);
        assertEquals(10, (int) r2.getWidth());
        assertEquals(0, (int) r2.getX());
        assertEquals(10, (int) r2.getHeight());
        assertEquals(0, (int) r2.getY());

        r2 = new Rectangle2D.Double(0, 0, 1000, 1000);
        GeometryUtilities.scaleDownToFit(r1, r2);
        assertEquals(50, (int) r2.getWidth());
        assertEquals(0, (int) r2.getX());
        assertEquals(50, (int) r2.getHeight());
        assertEquals(0, (int) r2.getY());

    }

    public void testPlaneCoeffs() {
        Coordinate c1 = new Coordinate(1, 2, -2);
        Coordinate c2 = new Coordinate(3, -2, 1);
        Coordinate c3 = new Coordinate(5, 1, -4);

        double[] coeffs = GeometryUtilities.getPlaneCoefficientsFrom3Points(c1, c2, c3);
        assertEquals(11.0, coeffs[0], DELTA);
        assertEquals(16.0, coeffs[1], DELTA);
        assertEquals(14.0, coeffs[2], DELTA);
        assertEquals(-15.0, coeffs[3], DELTA);
    }

    public void testLinePlaneIntersection() {
        Coordinate pC1 = new Coordinate(0, 0, 0);
        Coordinate pC2 = new Coordinate(2, 0, 0);
        Coordinate pC3 = new Coordinate(0, 2, 0);

        Coordinate lC1 = new Coordinate(0, 0, 1);
        Coordinate lC2 = new Coordinate(1, 1, -1);

        Coordinate lineWithPlaneIntersection = GeometryUtilities.getLineWithPlaneIntersection(lC1, lC2, pC1, pC2, pC3);
        assertEquals(0.5, lineWithPlaneIntersection.x, DELTA);
        assertEquals(0.5, lineWithPlaneIntersection.y, DELTA);
        assertEquals(0.0, lineWithPlaneIntersection.z, DELTA);

        pC1 = new Coordinate(0, 0, 0);
        pC2 = new Coordinate(2, 0, 1);
        pC3 = new Coordinate(0, 2, 1);

        lC1 = new Coordinate(0, 0, 1);
        lC2 = new Coordinate(1, 1, -1);

        lineWithPlaneIntersection = GeometryUtilities.getLineWithPlaneIntersection(lC1, lC2, pC1, pC2, pC3);
        double expected = 1.0 / 3.0;
        assertEquals(expected, lineWithPlaneIntersection.x, DELTA);
        assertEquals(expected, lineWithPlaneIntersection.y, DELTA);
        assertEquals(expected, lineWithPlaneIntersection.z, DELTA);

        pC1 = new Coordinate(0, 0, 0);
        pC2 = new Coordinate(2, 0, 0);
        pC3 = new Coordinate(0, 2, 0);

        lC1 = new Coordinate(0, 0, 1);
        lC2 = new Coordinate(1, 1, 1);

        lineWithPlaneIntersection = GeometryUtilities.getLineWithPlaneIntersection(lC1, lC2, pC1, pC2, pC3);
        assertNull(lineWithPlaneIntersection);
    }

    public void testLinePlaneAngle() {
        Coordinate p = new Coordinate(1, 2, 3);
        Coordinate iC1 = new Coordinate(1, -2, 0);
        Coordinate c2 = new Coordinate(2, 2, -1);
        Coordinate c3 = new Coordinate(-3, 1, -2);

        double angleBetweenLinePlane = GeometryUtilities.getAngleBetweenLinePlane(p, iC1, c2, c3);
        assertEquals(52.0, (int) angleBetweenLinePlane, DELTA);

    }

    public void testShortestDistanceFromPlane() {
        Coordinate pC1 = new Coordinate(0, 0, 0);
        Coordinate pC2 = new Coordinate(2, 0, 0);
        Coordinate pC3 = new Coordinate(0, 2, 0);

        Coordinate lC1 = new Coordinate(0, 5, 1);

        double distance = GeometryUtilities.getShortestDistanceFromTriangle(lC1, pC1, pC2, pC3);
        assertEquals(1.0, distance, DELTA);

        pC1 = new Coordinate(0, 0, 1);
        pC2 = new Coordinate(2, 0, 0);
        pC3 = new Coordinate(0, 2, 0);

        lC1 = new Coordinate(1, 1, 1);

        distance = GeometryUtilities.getShortestDistanceFromTriangle(lC1, pC1, pC2, pC3);
        assertEquals(0.8164965809277261, distance, DELTA);
    }

    public void testTriangleAngle() {
        Coordinate pC1 = new Coordinate(2, 0, 0);
        Coordinate pC2 = new Coordinate(0, 0, 0);
        Coordinate pC3 = new Coordinate(0, 2, 0);
        double angleBetween3D = angleBetween3D(pC1, pC2, pC3);
        assertEquals(90.0, angleBetween3D, DELTA);

        pC1 = new Coordinate(0, 0, 0);
        pC2 = new Coordinate(2, 0, 0);
        pC3 = new Coordinate(1, 1, 0);
        angleBetween3D = angleBetween3D(pC1, pC2, pC3);
        assertEquals(45.0, angleBetween3D, DELTA);

        pC1 = new Coordinate(0, 0, 0);
        pC2 = new Coordinate(2, 0, 0);
        pC3 = new Coordinate(4, 2, 0);
        angleBetween3D = angleBetween3D(pC1, pC2, pC3);
        assertEquals(135.0, angleBetween3D, DELTA);
    }

    public void testTriangleWindingrule() {
        Coordinate pC1 = new Coordinate(2, 1, 0);
        Coordinate pC2 = new Coordinate(0, 0, 0);
        Coordinate pC3 = new Coordinate(2, -1, 0);
        int windingRule = getTriangleWindingRule(pC1, pC2, pC3);
        assertEquals(1, windingRule);

        windingRule = getTriangleWindingRule(pC1, pC3, pC2);
        assertEquals(-1, windingRule);
    }

    public void testTriangleCentroid() {
        Coordinate pC1 = new Coordinate(-1, -3, 0);
        Coordinate pC2 = new Coordinate(2, 1, 0);
        Coordinate pC3 = new Coordinate(8, -4, 0);
        Coordinate centroid = getTriangleCentroid(pC1, pC2, pC3);
        assertEquals(3.0, centroid.x, DELTA);
        assertEquals(-2.0, centroid.y, DELTA);
        assertEquals(0.0, centroid.z, DELTA);
    }

    public void testTriangleNormalAngle() {
        Coordinate pC1 = new Coordinate(1, 0, 0);
        Coordinate pC2 = new Coordinate(0, 1, 0);
        Coordinate pC3 = new Coordinate(0, 0, 0);
        Coordinate pC1plus = new Coordinate(pC1.x, pC1.y, pC1.z + 1);
        double angle = getAngleBetweenLinePlane(pC1plus, pC1, pC2, pC3);
        assertEquals(90.0, angle, DELTA);

        pC1 = new Coordinate(-1, 0, 0);
        pC2 = new Coordinate(0, 1, 0);
        pC3 = new Coordinate(0, 0, 0);
        pC1plus = new Coordinate(pC1.x, pC1.y, pC1.z + 1);
        angle = getAngleBetweenLinePlane(pC1plus, pC1, pC2, pC3);
        assertEquals(90.0, angle, DELTA);

        pC1 = new Coordinate(-1, 0, 0);
        pC2 = new Coordinate(0, -1, 0);
        pC3 = new Coordinate(0, 0, 0);
        pC1plus = new Coordinate(pC1.x, pC1.y, pC1.z + 1);
        angle = getAngleBetweenLinePlane(pC1plus, pC1, pC2, pC3);
        assertEquals(90.0, angle, DELTA);

        pC1 = new Coordinate(1, 0, 0);
        pC2 = new Coordinate(0, 1, 0);
        pC3 = new Coordinate(0, 0, 1);
        pC1plus = new Coordinate(pC1.x, pC1.y, pC1.z + 1);
        angle = getAngleBetweenLinePlane(pC1plus, pC1, pC2, pC3);
        assertEquals(35.26438968, angle, DELTA);

        pC1 = new Coordinate(1, 0, 0);
        pC2 = new Coordinate(0, 1, 0);
        pC3 = new Coordinate(0, 0, -1);
        pC1plus = new Coordinate(pC1.x, pC1.y, pC1.z + 1);
        angle = getAngleBetweenLinePlane(pC1plus, pC1, pC2, pC3);
        assertEquals(35.26438968, angle, DELTA);

        pC1 = new Coordinate(1, 0, 0);
        pC2 = new Coordinate(0, 1, 0);
        pC3 = new Coordinate(0, 0, 2);
        pC1plus = new Coordinate(pC1.x, pC1.y, pC1.z + 1);
        angle = getAngleBetweenLinePlane(pC1plus, pC1, pC2, pC3);
        assertEquals(19.47122063, angle, DELTA);

        pC1 = new Coordinate(1, 0, 1);
        pC2 = new Coordinate(0, 1, 1);
        pC3 = new Coordinate(0, 0, 0);
        pC1plus = new Coordinate(pC1.x, pC1.y, pC1.z + 1);
        angle = getAngleBetweenLinePlane(pC1plus, pC1, pC2, pC3);
        assertEquals(35.26438968, angle, DELTA);

    }

    public void testPolygonToUnitScaler() throws Exception {
        WKTReader reader = new WKTReader();
        Geometry geometry = reader.read(IRREGULAR_POLYGON);
        Geometry scaled = GeometryUtilities.scaleToUnitaryArea((Polygon) geometry);
        double area = scaled.getArea();
        assertEquals(1.0, area, DELTA);

        geometry = reader.read(TWO_BALLS);
        scaled = GeometryUtilities.scaleToUnitaryArea((Polygon) geometry);
        area = scaled.getArea();
        assertEquals(1.0, area, DELTA);

        geometry = reader.read(RECTANGLE);
        scaled = GeometryUtilities.scaleToUnitaryArea((Polygon) geometry);
        area = scaled.getArea();
        assertEquals(1.0, area, DELTA);
        double perim = scaled.getLength();
        assertEquals(4.0, perim, DELTA);

        // hexa
        geometry = reader.read(HEXAGON);
        scaled = GeometryUtilities.scaleToUnitaryArea((Polygon) geometry);
        area = scaled.getArea();
        assertEquals(1.0, area, DELTA);
        perim = scaled.getLength();
        assertEquals(3.72241943, perim, DELTA);

        // triangle
        geometry = reader.read(EQUILATERAL_TRIANGLE);
        scaled = GeometryUtilities.scaleToUnitaryArea((Polygon) geometry);
        area = scaled.getArea();
        assertEquals(1.0, area, DELTA);
        perim = scaled.getLength();
        assertEquals(4.55901411, perim, DELTA);

    }

    public void testPointOnLineSide() throws Exception {
        Coordinate lineStart = new Coordinate(0, 0);
        Coordinate lineEnd = new Coordinate(200, 200);

        Coordinate p1 = new Coordinate(50, 150);
        Coordinate p2 = new Coordinate(50, 50);
        Coordinate p3 = new Coordinate(152, 50);

        int position = GeometryUtilities.getPointPositionAgainstLine(p1, lineStart, lineEnd);
        assertEquals(1, position);
        position = GeometryUtilities.getPointPositionAgainstLine(p2, lineStart, lineEnd);
        assertEquals(0, position);
        position = GeometryUtilities.getPointPositionAgainstLine(p3, lineStart, lineEnd);
        assertEquals(-1, position);

        // horizontal
        lineStart = new Coordinate(0, 0);
        lineEnd = new Coordinate(200, 0);

        p1 = new Coordinate(50, 150);
        p2 = new Coordinate(10, 0);
        p3 = new Coordinate(50, -50);

        position = GeometryUtilities.getPointPositionAgainstLine(p1, lineStart, lineEnd);
        assertEquals(1, position);
        position = GeometryUtilities.getPointPositionAgainstLine(p2, lineStart, lineEnd);
        assertEquals(0, position);
        position = GeometryUtilities.getPointPositionAgainstLine(p3, lineStart, lineEnd);
        assertEquals(-1, position);

        // vertical
        lineStart = new Coordinate(0, 200);
        lineEnd = new Coordinate(0, 0);

        p1 = new Coordinate(50, 150);
        p2 = new Coordinate(0, 100);
        p3 = new Coordinate(-50, 50);

        position = GeometryUtilities.getPointPositionAgainstLine(p1, lineStart, lineEnd);
        assertEquals(1, position);
        position = GeometryUtilities.getPointPositionAgainstLine(p2, lineStart, lineEnd);
        assertEquals(0, position);
        position = GeometryUtilities.getPointPositionAgainstLine(p3, lineStart, lineEnd);
        assertEquals(-1, position);
    }

}
