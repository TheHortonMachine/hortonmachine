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
    
    public void testDoubleArrayAdd() throws Exception {

        DynamicDoubleArray array = new DynamicDoubleArray(10);
        double[] expected = new double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0};
        
        array.addValue(1.0);
        array.addValue(2.0);
        array.setValue(2, 3.0);
        array.addValue(4.0);
        array.setValue(7, 8.0);
        array.setValue(4, 5.0);
        array.setValue(5, 6.0);
        array.addValue(9.0);
        array.addValue(10.0);
        array.addValue(11.0);
        array.setValue(11, 12.0);
        array.setValue(6, 7.0);
        
        for( int i = 0; i < expected.length; i++ ) {
			assertEquals(expected[i], array.getValue(i), 0.0000001);
		}

    }
}
