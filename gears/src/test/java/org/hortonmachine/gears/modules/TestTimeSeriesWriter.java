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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.HMTestCase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
/**
 * Test {@link OmsTimeSeriesWriter}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTimeSeriesWriter extends HMTestCase {

    public void testTimeSeriesWriter() throws Exception {

        DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

        URL dataUrl = this.getClass().getClassLoader().getResource("timeseriesreader_test.csv");
        String dataPath = new File(dataUrl.toURI()).getAbsolutePath();

        OmsTimeSeriesReader reader = new OmsTimeSeriesReader();
        reader.file = dataPath;
        reader.read();
        HashMap<DateTime, double[]> outData = reader.outData;
        reader.close();

        File tempFile = File.createTempFile("test", "jgt");
        OmsTimeSeriesWriter writer = new OmsTimeSeriesWriter();
        writer.columns = "datetime, rain";
        writer.file = tempFile.getAbsolutePath();
        writer.doDates = true;
        writer.inData = outData;
        writer.tablename = "testrain";
        writer.write();
        writer.close();

        reader = new OmsTimeSeriesReader();
        reader.file = dataPath;
        reader.read();
        outData = reader.outData;
        reader.close();

        if(!tempFile.delete()){
            tempFile.deleteOnExit();
        }

        Set<Entry<DateTime, double[]>> entrySet = outData.entrySet();

        Iterator<Entry<DateTime, double[]>> iterator = entrySet.iterator();

        Entry<DateTime, double[]> next = iterator.next();
        assertEquals(10.0, next.getValue()[0]);
        assertEquals("2000-01-01 00:00", next.getKey().toString(formatter));

        next = iterator.next();
        next = iterator.next();
        assertEquals(1.0, next.getValue()[0]);
        assertEquals("2000-01-01 02:00", next.getKey().toString(formatter));

        next = iterator.next();
        next = iterator.next();
        assertEquals(2.0, next.getValue()[0]);
        assertEquals("2000-01-01 04:00", next.getKey().toString(formatter));

    }

}
