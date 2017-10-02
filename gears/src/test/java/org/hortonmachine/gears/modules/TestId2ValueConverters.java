package org.hortonmachine.gears.modules;

import java.util.HashMap;
import java.util.Map;

import org.hortonmachine.gears.io.converters.IdValuesArray2IdValuesConverter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Test Id2ValueConverters.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestId2ValueConverters extends HMTestCase {

    public void testId2ValueConverters() throws Exception {

        int id1 = 1;
        double[] d1 = new double[]{1.0, 2.0, 3.0};
        int id2 = 2;
        double[] d2 = new double[]{HMConstants.doubleNovalue, 1.0, 2.0, HMConstants.doubleNovalue, 3.0};

        HashMap<Integer, double[]> data = new HashMap<Integer, double[]>();
        data.put(id1, d1);
        data.put(id2, d2);

        IdValuesArray2IdValuesConverter converter = new IdValuesArray2IdValuesConverter();
        converter.inData = data;
        converter.convert();
        Map<Integer, Double> outData = converter.outData;

        Double c1 = outData.get(1);
        Double c2 = outData.get(2);
        assertEquals(2.0, c1);
        assertEquals(2.0, c2);
    }
}
