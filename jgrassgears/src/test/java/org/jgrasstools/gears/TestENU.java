package org.jgrasstools.gears;

import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.utils.ENU;
import org.jgrasstools.gears.utils.HMTestCase;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Test ENU.
 */
public class TestENU extends HMTestCase {

    public void testPaperData() throws Exception {
        Coordinate cLA = new Coordinate(-117.3335693, 34.00000048, 251.702);

        ENU enu = new ENU(cLA);
        Coordinate c = enu.wgs84ToEcef(cLA);

        assertTrue(isDeltaOk(-2430601.8, c.x));
        assertTrue(isDeltaOk(-4702442.7, c.y));
        assertTrue(isDeltaOk(3546587.4, c.z));

        Coordinate ecefC = new Coordinate(c.x + 1, c.y, c.z);
        Coordinate cEnu = enu.ecefToEnu(ecefC);

        assertTrue(isDeltaOk(0.88834836, cEnu.x));
        assertTrue(isDeltaOk(0.25676467, cEnu.y));
        assertTrue(isDeltaOk(-0.38066927, cEnu.z));

        ecefC = new Coordinate(c.x, c.y + 1, c.z);
        cEnu = enu.ecefToEnu(ecefC);

        assertTrue(isDeltaOk(-0.45917011, cEnu.x));
        assertTrue(isDeltaOk(0.49675810, cEnu.y));
        assertTrue(isDeltaOk(-0.73647416, cEnu.z));

        ecefC = new Coordinate(c.x, c.y, c.z + 1);
        cEnu = enu.ecefToEnu(ecefC);

        assertTrue(isDeltaOk(0.00000000, cEnu.x));
        assertTrue(isDeltaOk(0.82903757, cEnu.y));
        assertTrue(isDeltaOk(0.55919291, cEnu.z));
    }

    public void testWithGeotools() {
        Coordinate c1 = new Coordinate(11, 46, 0);
        Coordinate c2 = new Coordinate(11.001, 46.001, 0);

        GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
        gc.setStartingGeographicPoint(c1.x, c1.y);
        gc.setDestinationGeographicPoint(c1.x, c1.y);
        double orthodromicDistance = gc.getOrthodromicDistance();

        ENU enu = new ENU(c1);
        Coordinate ce1 = enu.wgs84ToEnu(c1);
        Coordinate ce2 = enu.wgs84ToEnu(c2);

        double distance = ce1.distance(ce2);
        assertTrue(isDeltaOk(orthodromicDistance, distance));
    }

    private boolean isDeltaOk( double value1, double value2 ) {
        double delta = Math.abs(value2 - value1);
        return delta < 1e8;
    }
}
