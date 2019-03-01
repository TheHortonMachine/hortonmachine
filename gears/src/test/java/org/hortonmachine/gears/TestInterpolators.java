package org.hortonmachine.gears;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.interpolation.Interpolator;
import org.hortonmachine.gears.utils.math.interpolation.LeastSquaresInterpolator;
import org.hortonmachine.gears.utils.math.interpolation.LinearArrayInterpolator;
import org.hortonmachine.gears.utils.math.interpolation.LinearListInterpolator;
import org.hortonmachine.gears.utils.math.interpolation.PolynomialInterpolator;
import org.hortonmachine.gears.utils.sorting.OddEvenSortAlgorithm;
import org.hortonmachine.gears.utils.sorting.QuickSortAlgorithm;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPoint;
/**
 * Test interpolation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestInterpolators extends HMTestCase {

    private static final double DELTA = 0.00000001;

    public void testListInterpolation() throws Exception {

        List<Double> xList = new ArrayList<Double>();
        List<Double> yList = new ArrayList<Double>();

        xList.add(1.0);
        xList.add(2.0);
        xList.add(3.0);

        yList.add(1.0);
        yList.add(2.0);
        yList.add(3.0);

        LinearListInterpolator dischargeScaleInterpolator = new LinearListInterpolator(xList, yList);

        Double interpolated = dischargeScaleInterpolator.linearInterpolateX(2.0);
        assertEquals(2.0, interpolated, DELTA);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(1.5);
        assertEquals(1.5, interpolated, DELTA);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(0.0);
        assertTrue(Double.isNaN(interpolated));
        interpolated = dischargeScaleInterpolator.linearInterpolateX(4.0);
        assertTrue(Double.isNaN(interpolated));

        Collections.reverse(yList);

        interpolated = dischargeScaleInterpolator.linearInterpolateX(2.0);
        assertEquals(2.0, interpolated, DELTA);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(1.5);
        assertEquals(2.5, interpolated, DELTA);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(0.0);
        assertTrue(Double.isNaN(interpolated));
        interpolated = dischargeScaleInterpolator.linearInterpolateX(4.0);
        assertTrue(Double.isNaN(interpolated));
    }

    public void testArrayInterpolation() throws Exception {

        double[] xarray = {1.0, 2.0, 3.0};
        double[] yarray = {1.0, 2.0, 3.0};

        LinearArrayInterpolator dischargeScaleInterpolator = new LinearArrayInterpolator(xarray, yarray);

        Double interpolated = dischargeScaleInterpolator.linearInterpolateX(2.0);
        assertEquals(2.0, interpolated, DELTA);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(1.5);
        assertEquals(1.5, interpolated, DELTA);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(0.0);
        assertTrue(Double.isNaN(interpolated));
        interpolated = dischargeScaleInterpolator.linearInterpolateX(4.0);
        assertTrue(Double.isNaN(interpolated));

        yarray = new double[]{3.0, 2.0, 1.0};

        dischargeScaleInterpolator = new LinearArrayInterpolator(xarray, yarray);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(2.0);
        assertEquals(2.0, interpolated, DELTA);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(1.5);
        assertEquals(2.5, interpolated, DELTA);
        interpolated = dischargeScaleInterpolator.linearInterpolateX(0.0);
        assertTrue(Double.isNaN(interpolated));
        interpolated = dischargeScaleInterpolator.linearInterpolateX(4.0);
        assertTrue(Double.isNaN(interpolated));
    }

    public void testPolynomialInterpolator() {
        // approximate e^1.4

        // samples
        List<Double> xList = new ArrayList<Double>();
        List<Double> yList = new ArrayList<Double>();
        xList.add(1.12);
        yList.add(3.0648541);
        xList.add(1.55);
        yList.add(4.71147);
        xList.add(1.25);
        yList.add(3.4903429);
        xList.add(1.92);
        yList.add(6.820958);
        xList.add(1.33);
        yList.add(3.7810435);
        xList.add(1.75);
        yList.add(5.754603);

        Interpolator interp = new PolynomialInterpolator(xList, yList);
        
//        OddEvenSortAlgorithm.oddEvenSort(xList, yList);
//        
//        List<Coordinate> list = new ArrayList<>();
//        for( int i = 0; i < xList.size(); i++ ) {
//            double x = xList.get(i);
//            double y = yList.get(i);
//            Coordinate c = new Coordinate(x, y);
//            list.add(c);
//        }
//
//        MultiPoint mp = GeometryUtilities.gf().createMultiPointFromCoords(list.toArray(new Coordinate[0]));
//        System.out.println(mp);
//        
//        List<Coordinate> list2 = new ArrayList<>();
//        for( int i = 0; i < xList.size(); i++ ) {
//            double x = xList.get(i);
//            double y = interp.getInterpolated(x);
//            Coordinate c = new Coordinate(x, y);
//            list2.add(c);
//            System.out.println(x + " " + y);
//        }
//
//        LineString ls = GeometryUtilities.gf().createLineString(list2.toArray(new Coordinate[0]));
//        System.out.println(ls);
        
        assertEquals(Math.exp(1.4), interp.getInterpolated(1.4), 0.0001);
    }

    public void testLeastSqareInterpolator() {
        // approximate e^1.4

        // samples
        List<Double> xList = new ArrayList<Double>();
        List<Double> yList = new ArrayList<Double>();
        xList.add(6.2);
        yList.add(6.0);
        xList.add(1.3);
        yList.add(0.75);
        xList.add(5.5);
        yList.add(3.05);
        xList.add(2.8);
        yList.add(2.96);
        xList.add(4.7);
        yList.add(4.72);
        xList.add(7.9);
        yList.add(5.81);
        xList.add(3.0);
        yList.add(2.49);

        LeastSquaresInterpolator interp = new LeastSquaresInterpolator(xList, yList);

        assertEquals(31.399999618530273, interp.getSumX(), 0.0001);
        assertEquals(25.77999973297119, interp.getSumY(), 0.0001);
        assertEquals(171.71999621391296, interp.getSumXX(), 0.0001);
        assertEquals(138.7909932732582, interp.getSumXY(), 0.0001);
        assertEquals(0.74993044, interp.getA1(), 0.0001);
        assertEquals(0.31888318, interp.getA0(), 0.0001);
    }

}
