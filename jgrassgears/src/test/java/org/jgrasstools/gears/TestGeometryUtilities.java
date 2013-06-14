package org.jgrasstools.gears;

import static org.jgrasstools.gears.utils.geometry.GeometryUtilities.*;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Test {@link GeometryUtilities}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGeometryUtilities extends HMTestCase {

    private static final double DELTA = 0.0000001;
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
        assertTrue(GeometryUtilities.isPolygon(dummyPolygon));

        LineString dummyLine = GeometryUtilities.createDummyLine();
        assertTrue(GeometryUtilities.isLine(dummyLine));
        assertFalse(GeometryUtilities.isLine(dummyPolygon));

        Point dummyPoint = GeometryUtilities.createDummyPoint();
        assertTrue(GeometryUtilities.isPoint(dummyPoint));
        assertFalse(GeometryUtilities.isLine(dummyPoint));
        assertFalse(GeometryUtilities.isPolygon(dummyPoint));

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("dummy");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Polygon.class);
        SimpleFeatureType polygonType = b.buildFeatureType();
        assertTrue(GeometryUtilities.isPolygon(polygonType.getGeometryDescriptor()));

        b = new SimpleFeatureTypeBuilder();
        b.setName("dummy");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", LineString.class);
        SimpleFeatureType lineType = b.buildFeatureType();
        assertTrue(GeometryUtilities.isLine(lineType.getGeometryDescriptor()));
        assertFalse(GeometryUtilities.isPolygon(lineType.getGeometryDescriptor()));

        b = new SimpleFeatureTypeBuilder();
        b.setName("dummy");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        SimpleFeatureType pointType = b.buildFeatureType();
        assertTrue(GeometryUtilities.isPoint(pointType.getGeometryDescriptor()));
        assertFalse(GeometryUtilities.isLine(pointType.getGeometryDescriptor()));
        assertFalse(GeometryUtilities.isPolygon(pointType.getGeometryDescriptor()));
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
}
