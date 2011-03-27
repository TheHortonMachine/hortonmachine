package org.jgrasstools.gears.modules.io;

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
public class TestTimeSeriesByStepReader extends HMTestCase {

    public void testId2ValueReader() throws Exception {
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("csvtest1.csv");

        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
        reader.file = new File(krigingRainUrl.toURI()).getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tEnd = "2000-12-31 00:00";
        reader.tTimestep = 1440;

        reader.nextRecord();
        // record 1: ,2000-01-01 00:00,-2.5,-2,-1.3,-1.1
        HashMap<Integer, double[]> id2ValueMap = reader.data;
        assertEquals(-2.5, id2ValueMap.get(1)[0]);
        assertEquals(-2.0, id2ValueMap.get(2)[0]);
        assertEquals(-1.3, id2ValueMap.get(3)[0]);
        assertEquals(-1.1, id2ValueMap.get(4)[0]);

        reader.nextRecord();
        // record 2: ,2000-01-02 00:00,-2,2.6,3.9,3.4
        id2ValueMap = reader.data;
        assertEquals(-2.0, id2ValueMap.get(1)[0]);
        assertEquals(2.6, id2ValueMap.get(2)[0]);
        assertEquals(3.9, id2ValueMap.get(3)[0]);
        assertEquals(3.4, id2ValueMap.get(4)[0]);

        reader.close();
    }

    public void testId2ValueReader2() throws Exception {
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("csvtest2.csv");

        TimeseriesByStepReaderId2Value reader = new TimeseriesByStepReaderId2Value();
        reader.file = new File(krigingRainUrl.toURI()).getAbsolutePath();
        reader.pAggregation = 0;
        reader.idfield = "ID";
        reader.tStart = "1997-01-01 00:00";
        reader.tEnd = "2006-12-31 00:00";
        reader.tTimestep = 1440;

        reader.nextRecord();
        HashMap<Integer, double[]> id2ValueMap = reader.data;
        assertEquals(14.9, id2ValueMap.get(1)[0]);
        assertEquals(15.2, id2ValueMap.get(2)[0]);

        reader.nextRecord();
        id2ValueMap = reader.data;
        assertEquals(17.2, id2ValueMap.get(1)[0]);
        assertEquals(17.4, id2ValueMap.get(2)[0]);

        reader.nextRecord();
        id2ValueMap = reader.data;
        assertEquals(19.8, id2ValueMap.get(1)[0]);
        assertEquals(20.0, id2ValueMap.get(2)[0]);

        reader.close();
    }

    public static void main( String[] args ) throws Exception {
        new TestTimeSeriesByStepReader().testId2ValueReader2();
    }
}
