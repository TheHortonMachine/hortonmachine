package org.jgrasstools.gears.modules;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.utils.HMTestCase;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestId2ValueReader extends HMTestCase {

    public void testId2ValueReader() throws Exception {
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("kriging_rain.csv");

        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
        reader.file = new File(krigingRainUrl.toURI()).getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tEnd = "2000-01-01 01:00";
        reader.tTimestep = 15;

        reader.nextRecord();
        HashMap<Integer, double[]> id2ValueMap = reader.data;
        assertEquals(1.74, id2ValueMap.get(1)[0]);
        assertEquals(1.34, id2ValueMap.get(2)[0]);
        assertEquals(1.61, id2ValueMap.get(3)[0]);
        assertEquals(2.15, id2ValueMap.get(4)[0]);
        assertEquals(1.57, id2ValueMap.get(5)[0]);
        assertEquals(1.15, id2ValueMap.get(6)[0]);

        reader.nextRecord();
        id2ValueMap = reader.data;
        assertEquals(1.71, id2ValueMap.get(1)[0]);
        assertEquals(1.37, id2ValueMap.get(2)[0]);
        assertEquals(1.62, id2ValueMap.get(3)[0]);
        assertEquals(2.18, id2ValueMap.get(4)[0]);
        assertEquals(1.63, id2ValueMap.get(5)[0]);
        assertEquals(1.19, id2ValueMap.get(6)[0]);

        reader.close();
    }

    public void testId2ValueReader2() throws Exception {
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("kriging_rain.csv");

        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
        reader.file = new File(krigingRainUrl.toURI()).getAbsolutePath();
        reader.pNum = 2;
        reader.pAggregation = 0;
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tEnd = "2000-01-01 01:00";
        reader.tTimestep = 30;

        reader.nextRecord();
        HashMap<Integer, double[]> id2ValueMap = reader.data;
        assertEquals(3.45, id2ValueMap.get(1)[0]);
        assertEquals(2.71, id2ValueMap.get(2)[0]);

        reader.nextRecord();
        id2ValueMap = reader.data;
        assertEquals(3.33, id2ValueMap.get(1)[0]);
        assertEquals(2.87, id2ValueMap.get(2)[0]);

        reader.nextRecord();
        id2ValueMap = reader.data;
        assertEquals(1.6, id2ValueMap.get(1)[0]);
        assertEquals(1.51, id2ValueMap.get(2)[0]);

        reader.close();
    }

    public static void main( String[] args ) throws Exception {
        new TestId2ValueReader().testId2ValueReader2();
    }
}
