package org.hortonmachine.gears;

import static java.lang.Math.abs;
import static java.lang.Math.random;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.fEq;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.isBetween;

import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

/**
 * Test numerics.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestNumericUtilities extends HMTestCase {

    public void testNumericUtilities() throws Exception {
        double a = 1.000000001;
        double b = 1.0;
        assertTrue(!dEq(a, b));
        assertTrue(dEq(a, b, 1E-8));

        float af = 1.000000001f;
        float bf = 1.0f;
        assertTrue(dEq(af, bf));
        assertTrue(dEq(af, bf, 1E-8f));

        a = Double.NaN;
        b = 1.0;
        assertFalse(dEq(a, b));
        b = Double.NaN;
        assertTrue(dEq(a, b));

        af = Float.NaN;
        bf = 1.0f;
        assertFalse(fEq(af, bf));
        bf = Float.NaN;
        assertTrue(fEq(af, bf));

        a = Double.POSITIVE_INFINITY;
        b = Double.POSITIVE_INFINITY;
        assertTrue(dEq(a, b));

        a = Double.POSITIVE_INFINITY;
        b = Double.NEGATIVE_INFINITY;
        assertTrue(dEq(a, abs(b)));

        af = Float.POSITIVE_INFINITY;
        bf = Float.POSITIVE_INFINITY;
        assertTrue(fEq(af, bf));

        af = Float.POSITIVE_INFINITY;
        bf = Float.NEGATIVE_INFINITY;
        assertTrue(fEq(af, abs(bf)));

        a = 0.3 - 0.2 - 0.1;
        b = 0.0;
        assertTrue(dEq(a, b));

        a = 1.0;
        double[] ranges = {0.0, 1.0, -3.5, 6.7};
        assertTrue(isBetween(a, ranges));
        ranges = new double[]{0.0, 1.0, -3.5, 6.7, 1.2, 2.0};
        assertFalse(isBetween(a, ranges));

    }

    public void testRanges() throws Exception {
        double[] x = {1, 2, -1, -4, 6, 2, 2, 3, -1, -2, -4};
        List<int[]> negativeRanges = NumericsUtilities.getNegativeRanges(x);
        int[] i1 = negativeRanges.get(0);
        assertEquals(i1[0], 2);
        assertEquals(i1[1], 3);
        int[] i2 = negativeRanges.get(1);
        assertEquals(i2[0], 8);
        assertEquals(i2[1], 10);
    }

    public void testRangeToBin() throws Exception {
        double[] range2Bins = NumericsUtilities.range2Bins(-6, 12, 6);
        double[][] expected = {{-6.0, -3.0, 0.0, 3.0, 6.0, 9.0, 12.0}};
        double[][] got = {range2Bins};
        checkMatrixEqual(got, expected, DELTA);

        // use steps
        range2Bins = NumericsUtilities.range2Bins(-6, 12, 3.0, false);
        expected = new double[][]{{-6.0, -3.0, 0.0, 3.0, 6.0, 9.0, 12.0}};
        got = new double[][]{range2Bins};
        checkMatrixEqual(got, expected, DELTA);

        range2Bins = NumericsUtilities.range2Bins(-6, 12, 3.0, true);
        expected = new double[][]{{-6.0, -3.0, 0.0, 3.0, 6.0, 9.0, 12.0}};
        got = new double[][]{range2Bins};
        checkMatrixEqual(got, expected, DELTA);

        range2Bins = NumericsUtilities.range2Bins(-6, 12, 1.5, false);
        expected = new double[][]{{-6.0, -4.5, -3.0, -1.5, 0.0, 1.5, 3.0, 4.5, 6.0, 7.5, 9.0, 10.5, 12.0}};
        got = new double[][]{range2Bins};
        checkMatrixEqual(got, expected, DELTA);

        range2Bins = NumericsUtilities.range2Bins(-6, 12, 5.0, false);
        expected = new double[][]{{-6.0, -1.0, 4.0, 9.0, 12.0}};
        got = new double[][]{range2Bins};
        checkMatrixEqual(got, expected, DELTA);

        range2Bins = NumericsUtilities.range2Bins(-6, 12, 5.0, true);
        expected = new double[][]{{-6.0, -1.0, 4.0, 9.0, 14.0}};
        got = new double[][]{range2Bins};
        checkMatrixEqual(got, expected, DELTA);

        range2Bins = NumericsUtilities.range2Bins(-6, 12, 7.5, false);
        expected = new double[][]{{-6.0, 1.5, 9.0, 12.0}};
        got = new double[][]{range2Bins};
        checkMatrixEqual(got, expected, DELTA);

        range2Bins = NumericsUtilities.range2Bins(-6, 12, 7.5, true);
        expected = new double[][]{{-6.0, 1.5, 9.0, 16.5}};
        got = new double[][]{range2Bins};
        checkMatrixEqual(got, expected, DELTA);
    }
}
