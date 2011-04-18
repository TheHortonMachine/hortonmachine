/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.jgrasstools.gears.io.timedependent.TimeSeriesIteratorReader;
import org.jgrasstools.gears.io.timedependent.TimeSeriesIteratorWriter;
import org.jgrasstools.gears.utils.HMTestCase;
/**
 * Test {@link TimeSeriesIteratorWriter}.
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
        TimeSeriesIteratorReader reader = new TimeSeriesIteratorReader();
        reader.file = dataPath;
        reader.idfield = id;
        reader.tStart = startDate;
        reader.tEnd = endDate;
        reader.tTimestep = timeStep;
        reader.initProcess();

        // setup writer
        File tempFile = File.createTempFile("test", "jgt");
        TimeSeriesIteratorWriter writer = new TimeSeriesIteratorWriter();
        writer.file = tempFile.getAbsolutePath();
        writer.tablename = "testrain";
        writer.fileNovalue = "-9999.0";
        writer.tStart = startDate;
        writer.tTimestep = timeStep;
        while( reader.doProcess ) {
            reader.nextRecord();
            HashMap<Integer, double[]> id2ValueMap = reader.data;
            // feed to writer
            writer.data = id2ValueMap;
            writer.writeNextLine();
        }
        writer.close();
        reader.close();

        // check written stuff
        reader = new TimeSeriesIteratorReader();
        reader.file = tempFile.getAbsolutePath();
        reader.idfield = id;
        reader.tStart = startDate;
        reader.tEnd = endDate;
        reader.tTimestep = timeStep;

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

        assertTrue(tempFile.delete());

    }

}
