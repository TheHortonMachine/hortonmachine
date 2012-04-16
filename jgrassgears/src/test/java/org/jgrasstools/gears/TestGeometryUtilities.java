package org.jgrasstools.gears;

import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Test {@link GeometryUtilities}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGeometryUtilities extends HMTestCase {

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
        List<Coordinate> coordinatesAtInterval = GeometryUtilities.getCoordinatesAtInterval(l1, 0.5);

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
            assertEquals(coordinatesAtInterval.get(i), expectedCoordinates.get(i));
        }

    }
}
