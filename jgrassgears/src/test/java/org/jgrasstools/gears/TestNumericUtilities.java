package org.jgrasstools.gears;

import org.jgrasstools.gears.utils.HMTestCase;

import static java.lang.Math.abs;
import static org.jgrasstools.gears.utils.math.NumericsUtilities.*;

/**
 * Test numerics.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestNumericUtilities extends HMTestCase {

    public void testNumericUtilities() throws Exception {
        double a = 1.000000001;
        double b = 1.0;
        assertTrue(doubleEquals(a, b));
        assertTrue(!doubleEquals(a, b, 1E-10));

        float af = 1.000000001f;
        float bf = 1.0f;
        assertTrue(doubleEquals(af, bf));
        assertTrue(doubleEquals(af, bf, 1E-8f));

        a = Double.NaN;
        b = 1.0;
        assertFalse(doubleEquals(a, b));
        b = Double.NaN;
        assertTrue(doubleEquals(a, b));

        af = Float.NaN;
        bf = 1.0f;
        assertFalse(floatEquals(af, bf));
        bf = Float.NaN;
        assertTrue(floatEquals(af, bf));

        a = Double.POSITIVE_INFINITY;
        b = Double.POSITIVE_INFINITY;
        assertTrue(doubleEquals(a, b));

        a = Double.POSITIVE_INFINITY;
        b = Double.NEGATIVE_INFINITY;
        assertTrue(doubleEquals(a, abs(b)));

        af = Float.POSITIVE_INFINITY;
        bf = Float.POSITIVE_INFINITY;
        assertTrue(floatEquals(af, bf));

        af = Float.POSITIVE_INFINITY;
        bf = Float.NEGATIVE_INFINITY;
        assertTrue(floatEquals(af, abs(bf)));

    }
}
