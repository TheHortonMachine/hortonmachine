package org.hortonmachine.gears;

import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.utils.ENU;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.math.matrixes.MatrixException;

import org.locationtech.jts.geom.Coordinate;

/**
 * Test ENU.
 */
public class TestENU extends HMTestCase {

    public void testPaperData() throws Exception {
        Coordinate cLA = new Coordinate(-117.3335693, 34.00000048, 251.702);

        ENU enu = new ENU(cLA);
        Coordinate c = enu.wgs84ToEcef(cLA);

        assertTrue(Math.abs(-2430601.8 - c.x) < 0.05);
        assertTrue(Math.abs(-4702442.7- c.y) < 0.05);
        assertTrue(Math.abs(3546587.4- c.z) < 0.05);

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

    public void testWithGeotools() throws MatrixException {
        Coordinate c1 = new Coordinate(11, 46, 0);
        Coordinate c2 = new Coordinate(11.001, 46.001, 0);

        GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
        gc.setStartingGeographicPoint(c1.x, c1.y);
        gc.setDestinationGeographicPoint(c2.x, c2.y);
        double orthodromicDistance = gc.getOrthodromicDistance();

        ENU enu = new ENU(c1);
        Coordinate ce1 = enu.wgs84ToEnu(c1);
        Coordinate ce2 = enu.wgs84ToEnu(c2);

        double distance = ce1.distance(ce2);
        assertTrue(isDeltaOk(orthodromicDistance, distance));
        
        Coordinate c1Back = enu.enuToWgs84(ce1);
        Coordinate c2Back = enu.enuToWgs84(ce2);
        
        assertEquals(0, c1.distance(c1Back), 0.000001);
        assertEquals(0, c2.distance(c2Back), 0.000001);
        
    }

//    public void testEnu2WithGeotools() throws MatrixException {
//        Coordinate c1 = new Coordinate(11, 46, 0);
//        Coordinate c2 = new Coordinate(11.001, 46.001, 0);
//
//        GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
//        gc.setStartingGeographicPoint(c1.x, c1.y);
//        gc.setDestinationGeographicPoint(c2.x, c2.y);
//        double orthodromicDistance = gc.getOrthodromicDistance();
//
//        ENU2 enu1 = ENU2.globalGeodInstance(c1.y, c1.x, c1.z);
//        enu1.computeLocal(enu1);
//        double e1 = enu1.getE();
//        double n1 = enu1.getN();
//        double u1 = enu1.getU();
//        Coordinate ce1 = new Coordinate(e1, n1, u1);
//        ENU2 enu2 = ENU2.globalGeodInstance(c2.y, c2.x, c2.z);
//        enu2.computeLocal(enu1);
//        double e2 = enu2.getE();
//        double n2 = enu2.getN();
//        double u2 = enu2.getU();
//        Coordinate ce2 = new Coordinate(e2, n2, u2);
//
//        double distance = ce1.distance(ce2);
//        assertTrue(isDeltaOk(orthodromicDistance, distance));
//
//        // now go back
//        
//        double x = enu1.getX();
//        double y = enu1.getY();
//        double z = enu1.getZ();
//        
//        ENU2 enu11 = ENU2.globalXYZInstance(x,y,z);
//        enu11.computeGeodetic();
//        double lon1 = enu11.getGeodeticLongitude();
//        double lat1 = enu11.getGeodeticLatitude();
//        double elev1 = enu11.getGeodeticHeight();
//        Coordinate c1back = new Coordinate(lon1, lat1, elev1);
//        // ENU2 enu22 = ENU2.globalENUInstance(e2,n2,u2 );
//        System.out.println(c1back);
//
//    }

    private boolean isDeltaOk( double value1, double value2 ) {
        double delta = Math.abs(value2 - value1);
        return delta < 1e-6;
    }
}
