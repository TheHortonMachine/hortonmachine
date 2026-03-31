package org.hortonmachine.gears;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.fixes.HMCylindricalEqualArea;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class TestCrs {

    private static final double TOL_DEGREES = 1e-7;
    private static final double TOL_METERS = 1e-2;

    @Test
    public void test6933To4326AtOrigin() throws Exception {
    	HMCrsRegistry.INSTANCE.init();
		CoordinateReferenceSystem sourceCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:6933", true);
        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);

        assertNotNull(sourceCrs);
        assertNotNull(targetCrs);

        MathTransform transform = CRS.findMathTransform(sourceCrs, targetCrs, true);
        assertNotNull(transform);

        GeometryFactory gf = new GeometryFactory();

        // In EPSG:6933, (0,0) should map to lon=0, lat=0
        Point sourcePoint = gf.createPoint(new Coordinate(0.0, 0.0));
        Point targetPoint = (Point) JTS.transform(sourcePoint, transform);

        assertEquals(0.0, targetPoint.getX(), TOL_DEGREES); // longitude
        assertEquals(0.0, targetPoint.getY(), TOL_DEGREES); // latitude
    }

    @Test
    public void test6933To4326RoundTrip() throws Exception {
    	HMCrsRegistry.INSTANCE.init();
		CoordinateReferenceSystem sourceCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:6933", true);
        CoordinateReferenceSystem targetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);

        MathTransform to4326 = CRS.findMathTransform(sourceCrs, targetCrs, true);
        MathTransform to4326fixed = HMCylindricalEqualArea.createEpsg6933Inverse();
        MathTransform to6933 = CRS.findMathTransform(targetCrs, sourceCrs, true);
        MathTransform to6933fixed = HMCylindricalEqualArea.createEpsg6933Forward();

        GeometryFactory gf = new GeometryFactory();

        // THIS SHOWS THE CURRENT GEOTOOLS BUG BEHAVIOR
        Point p6933 = gf.createPoint(new Coordinate(964862.802509, 1269436.7435378));
        Point p4326 = (Point) JTS.transform(p6933, to4326);
        Point p6933Back = (Point) JTS.transform(p4326, to6933);
        assertNotEquals(p4326.getX(), 10.0, TOL_DEGREES);
        assertNotEquals(p4326.getY(), 10.0, TOL_DEGREES);
        assertEquals(p6933.getX(), p6933Back.getX(), TOL_METERS);
        assertEquals(p6933.getY(), p6933Back.getY(), TOL_METERS);

        // THIS SHOWS THE FIXED BEHAVIOR
        Point p4326fixed = (Point) JTS.transform(p6933, to4326fixed);
        Point p6933BackFixed = (Point) JTS.transform(p4326fixed, to6933fixed);
        assertEquals(p4326fixed.getX(), 10.0, TOL_DEGREES);
        assertEquals(p4326fixed.getY(), 10.0, TOL_DEGREES);
        assertEquals(p6933.getX(), p6933BackFixed.getX(), TOL_METERS);
        assertEquals(p6933.getY(), p6933BackFixed.getY(), TOL_METERS);
    }
}