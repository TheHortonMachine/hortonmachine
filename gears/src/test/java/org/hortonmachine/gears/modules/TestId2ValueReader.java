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
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestId2ValueReader extends HMTestCase {

    public void testId2ValueReader() throws Exception {
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("kriging_rain.csv");

        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = new File(krigingRainUrl.toURI()).getAbsolutePath();
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tEnd = "2000-01-01 01:00";
        reader.tTimestep = 15;

        reader.nextRecord();
        HashMap<Integer, double[]> id2ValueMap = reader.outData;
        assertEquals(1.74, id2ValueMap.get(1)[0]);
        assertEquals(1.34, id2ValueMap.get(2)[0]);
        assertEquals(1.61, id2ValueMap.get(3)[0]);
        assertEquals(2.15, id2ValueMap.get(4)[0]);
        assertEquals(1.57, id2ValueMap.get(5)[0]);
        assertEquals(1.15, id2ValueMap.get(6)[0]);

        reader.nextRecord();
        id2ValueMap = reader.outData;
        assertEquals(1.71, id2ValueMap.get(1)[0]);
        assertEquals(1.37, id2ValueMap.get(2)[0]);
        assertEquals(1.62, id2ValueMap.get(3)[0]);
        assertEquals(2.18, id2ValueMap.get(4)[0]);
        assertEquals(1.63, id2ValueMap.get(5)[0]);
        assertEquals(1.19, id2ValueMap.get(6)[0]);

        reader.close();
    }

    /**
     * Try to read without set the timesteo and start time.
     * 
     * @throws Exception
     */
    public void testId2ValueReaderNoTime() throws Exception {
        URL krigingRainUrl = this.getClass().getClassLoader().getResource("kriging_rain.csv");

        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();       
        reader.file = new File(krigingRainUrl.toURI()).getAbsolutePath();
        reader.idfield = "ID";
        reader.nextRecord();
        HashMap<Integer, double[]> id2ValueMap = reader.outData;
        assertEquals(1.74, id2ValueMap.get(1)[0]);
        assertEquals(1.34, id2ValueMap.get(2)[0]);
        assertEquals(1.61, id2ValueMap.get(3)[0]);
        assertEquals(2.15, id2ValueMap.get(4)[0]);
        assertEquals(1.57, id2ValueMap.get(5)[0]);
        assertEquals(1.15, id2ValueMap.get(6)[0]);

        reader.nextRecord();
        id2ValueMap = reader.outData;
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

        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = new File(krigingRainUrl.toURI()).getAbsolutePath();
        reader.pNum = 2;
        reader.pAggregation = 0;
        reader.idfield = "ID";
        reader.tStart = "2000-01-01 00:00";
        reader.tEnd = "2000-01-01 01:00";
        reader.tTimestep = 30;

        reader.nextRecord();
        HashMap<Integer, double[]> id2ValueMap = reader.outData;
        assertEquals(3.45, id2ValueMap.get(1)[0]);
        assertEquals(2.71, id2ValueMap.get(2)[0]);

        reader.nextRecord();
        id2ValueMap = reader.outData;
        assertEquals(3.33, id2ValueMap.get(1)[0]);
        assertEquals(2.87, id2ValueMap.get(2)[0]);

        reader.nextRecord();
        id2ValueMap = reader.outData;
        assertEquals(1.6, id2ValueMap.get(1)[0]);
        assertEquals(1.51, id2ValueMap.get(2)[0]);

        reader.close();
    }

    public static void main( String[] args ) throws Exception {
        new TestId2ValueReader().testId2ValueReader2();
    }
}
