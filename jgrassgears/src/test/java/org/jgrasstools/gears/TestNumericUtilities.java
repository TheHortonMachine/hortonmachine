package org.jgrasstools.gears;

import static java.lang.Math.abs;
import static org.jgrasstools.gears.utils.math.NumericsUtilities.dEq;
import static org.jgrasstools.gears.utils.math.NumericsUtilities.fEq;
import static org.jgrasstools.gears.utils.math.NumericsUtilities.isBetween;

import org.jgrasstools.gears.utils.HMTestCase;

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
}
