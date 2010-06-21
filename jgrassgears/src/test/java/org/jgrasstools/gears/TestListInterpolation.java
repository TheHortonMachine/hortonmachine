package org.jgrasstools.gears;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.math.ListInterpolator;
/**
 * Test interpolation.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestListInterpolation extends HMTestCase {

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
        
        ListInterpolator dischargeScaleInterpolator = new ListInterpolator(xList, yList);
        
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
}
