/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.modules;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Test {@link OmsTimeSeriesIteratorReader}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTimeSeriesIteratorReader extends HMTestCase {

    public void testId2ValueReader() throws Exception {
        URL dataUrl = this.getClass().getClassLoader().getResource("timeseriesiteratorreader_test.csv");

        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = new File(dataUrl.toURI()).getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tEnd = "2000-12-31 00:00";
        reader.tTimestep = 1440;

        reader.nextRecord();
        // record 1: ,2000-01-01 00:00,-2.5,-2,-1.3,-1.1
        HashMap<Integer, double[]> id2ValueMap = reader.outData;
        assertEquals(-2.5, id2ValueMap.get(1)[0]);
        assertEquals(-2.0, id2ValueMap.get(2)[0]);
        assertEquals(-1.3, id2ValueMap.get(3)[0]);
        assertEquals(-1.1, id2ValueMap.get(4)[0]);

        reader.nextRecord();
        // record 2: ,2000-01-02 00:00,-2,2.6,3.9,3.4
        id2ValueMap = reader.outData;
        assertEquals(-2.0, id2ValueMap.get(1)[0]);
        assertEquals(2.6, id2ValueMap.get(2)[0]);
        assertEquals(3.9, id2ValueMap.get(3)[0]);
        assertEquals(3.4, id2ValueMap.get(4)[0]);

        reader.close();
    }

    public void testId2ValueReader2() throws Exception {
        URL dataUrl = this.getClass().getClassLoader().getResource("csvtest2.csv");

        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = new File(dataUrl.toURI()).getAbsolutePath();
        reader.pAggregation = 0;
        reader.idfield = "ID";
        reader.tStart = "1997-01-01 00:00";
        reader.tEnd = "2006-12-31 00:00";
        reader.tTimestep = 1440;
        try {
            reader.initProcess();
            int count = 0;
            while( reader.doProcess ) {
                reader.nextRecord();
                HashMap<Integer, double[]> id2ValueMap = reader.outData;
                if (count == 0) {
                    assertEquals(14.9, id2ValueMap.get(1)[0]);
                    assertEquals(15.2, id2ValueMap.get(2)[0]);
                } else if (count == 1) {
                    assertEquals(17.2, id2ValueMap.get(1)[0]);
                    assertEquals(17.4, id2ValueMap.get(2)[0]);
                } else if (count == 2) {
                    assertEquals(19.8, id2ValueMap.get(1)[0]);
                    assertEquals(20.0, id2ValueMap.get(2)[0]);
                    break;
                }
                count++;
            }
        } finally {
            reader.close();
        }

    }
    public static void main( String[] args ) throws Exception {
        new TestTimeSeriesIteratorReader().testId2ValueReader2();
    }
}
