package org.jgrasstools.gears;

import org.jgrasstools.gears.utils.HMTestCase;
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
    }
}
