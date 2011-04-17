package org.jgrasstools.gears.modules;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrasstools.gears.io.timedependent.TimeseriesByStepReaderId2Value;
import org.jgrasstools.gears.io.timeseries.TimeSeriesReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.HMTestCase;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTimeSeriesReader extends HMTestCase {

    public void testId2ValueReader() throws Exception {
        URL dataUrl = this.getClass().getClassLoader().getResource("timeseriesreader_test.csv");

        DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;

        TimeSeriesReader reader = new TimeSeriesReader();
        reader.file = new File(dataUrl.toURI()).getAbsolutePath();
        reader.read();
        LinkedHashMap<DateTime, double[]> outData = reader.outData;

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
