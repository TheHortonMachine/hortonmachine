package org.jgrasstools.gears;

import java.util.ArrayList;
import java.util.List;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
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

}
