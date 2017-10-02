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
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Test {@link OmsTimeSeriesIteratorWriter}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTimeSeriesIteratorWriter extends HMTestCase {

    public void testTimeSeriesIteratorWriter() throws Exception {

        String startDate = "2000-01-01 00:00";
        String endDate = "2000-12-31 00:00";
        String id = "ID";
        int timeStep = 1440;

        URL dataUrl = this.getClass().getClassLoader().getResource("timeseriesiteratorreader_test.csv");
        String dataPath = new File(dataUrl.toURI()).getAbsolutePath();

        // setup reader
        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = dataPath;
        reader.idfield = id;
        reader.tStart = startDate;
        reader.tEnd = endDate;
        reader.tTimestep = timeStep;
        reader.initProcess();

        // setup writer
        File tempFile = File.createTempFile("test", "jgt");
        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
        writer.file = tempFile.getAbsolutePath();
        writer.inTablename = "testrain";
        writer.fileNovalue = "-9999.0";
        writer.tStart = startDate;
        writer.tTimestep = timeStep;
        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            // feed to writer
            writer.inData = id2ValueMap;
            writer.writeNextLine();
        }
        writer.close();
        reader.close();

        // check written stuff
        reader = new OmsTimeSeriesIteratorReader();
        reader.file = tempFile.getAbsolutePath();
        reader.idfield = id;
        reader.tStart = startDate;
        reader.tEnd = endDate;
        reader.tTimestep = timeStep;

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

        tempFile.delete();

    }

    public void testTimeSeriesIteratorWriterComplex() throws Exception {

        String startDate = "2005-05-01 00:00";
        String endDate = "2005-05-01 03:00";
        String id = "ID";
        int timeStep = 60;

        URL dataUrl = this.getClass().getClassLoader().getResource("timeseriesiteratorcomplexreader_test.csv");
        String dataPath = new File(dataUrl.toURI()).getAbsolutePath();

        // setup reader
        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = dataPath;
        reader.idfield = id;
        reader.tStart = startDate;
        reader.tEnd = endDate;
        reader.tTimestep = timeStep;
        reader.initProcess();

        // setup writer
        File tempFile = File.createTempFile("test", "jgt");
        OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
        writer.file = tempFile.getAbsolutePath();
        writer.inTablename = "testrain";
        writer.fileNovalue = "-9999.0";
        writer.tStart = startDate;
        writer.tTimestep = timeStep;
        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            // feed to writer
            writer.inData = id2ValueMap;
            writer.writeNextLine();
        }
        writer.close();
        reader.close();

        // check written stuff
        reader = new OmsTimeSeriesIteratorReader();
        reader.file = tempFile.getAbsolutePath();
        reader.idfield = id;
        reader.tStart = startDate;
        reader.tEnd = endDate;
        reader.tTimestep = timeStep;

        reader.nextRecord();
        // record 1: ,2000-01-01 00:00,-2.5,-2,-1.3,-1.1
        HashMap<Integer, double[]> id2ValueMap = reader.outData;
        assertEquals(8.64, id2ValueMap.get(1221)[0]);
        assertEquals(7.34, id2ValueMap.get(1221)[1]);
        assertEquals(7.16, id2ValueMap.get(1221)[2]);
        assertEquals(6.01, id2ValueMap.get(1221)[3]);
        assertEquals(3.03, id2ValueMap.get(1221)[4]);

        reader.nextRecord();
        // record 2: ,2000-01-02 00:00,-2,2.6,3.9,3.4
        id2ValueMap = reader.outData;
        assertEquals(5.95, id2ValueMap.get(1097)[0]);
        assertEquals(5.77, id2ValueMap.get(1097)[1]);
        assertEquals(5.59, id2ValueMap.get(1097)[2]);
        assertEquals(4.01, id2ValueMap.get(1097)[3]);
        assertEquals(2.39, id2ValueMap.get(1097)[4]);

        reader.close();

        tempFile.delete();

    }

}
