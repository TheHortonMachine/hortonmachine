package org.hortonmachine.gears;

import org.hortonmachine.gears.utils.DynamicDoubleArray;
import org.hortonmachine.gears.utils.HMTestCase;

/**
 * Test {@link DynamicDoubleArray}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDoubleArray extends HMTestCase {

    public void testDoubleArray() throws Exception {

        DynamicDoubleArray array = new DynamicDoubleArray(10);

        for( int i = 0; i < 20; i++ ) {
            array.setValue(i, i);
        }
        for( int i = 0; i < 20; i++ ) {
            assertEquals(i, array.getValue(i), 0.0000001);
        }

        assertEquals(60, array.getInternalArray().length);
        assertEquals(20, array.getTrimmedInternalArray().length);

    }
}
