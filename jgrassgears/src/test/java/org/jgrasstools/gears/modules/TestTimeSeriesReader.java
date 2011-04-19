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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrasstools.gears.io.timeseries.TimeSeriesReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.HMTestCase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
/**
 * Test {@link TimeSeriesReader}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTimeSeriesReader extends HMTestCase {

    public void testTimeSeriesReader() throws Exception {
        DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;
        URL dataUrl = this.getClass().getClassLoader().getResource("timeseriesreader_test.csv");
        String dataPath = new File(dataUrl.toURI()).getAbsolutePath();

        TimeSeriesReader reader = new TimeSeriesReader();
        reader.file = dataPath;
        reader.read();
        HashMap<DateTime, double[]> outData = reader.outData;

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

        reader.close();
    }

}
