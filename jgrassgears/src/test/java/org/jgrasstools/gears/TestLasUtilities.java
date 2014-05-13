package org.jgrasstools.gears;

import java.util.Arrays;
import java.util.List;

import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.io.las.utils.LasUtils.VALUETYPE;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.HMTestCase;
import org.joda.time.DateTime;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Test las utils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestLasUtilities extends HMTestCase {

    private List<LasRecord> records;

    protected void setUp() throws Exception {
        LasRecord r1 = new LasRecord();
        r1.x = 1.0;
        r1.y = 1.0;
        r1.z = 3.0;
        r1.classification = 1;
        r1.intensity = 256;
        r1.returnNumber = 1;
        r1.numberOfReturns = 3;

        LasRecord r2 = new LasRecord();
        r2.x = 2.0;
        r2.y = 2.0;
        r2.z = 6.0;
        r2.classification = 2;
        r2.intensity = 128;
        r2.returnNumber = 2;
        r2.numberOfReturns = 3;

        LasRecord r3 = new LasRecord();
        r3.x = 3.0;
        r3.y = 3.0;
        r3.z = 9.0;
        r3.classification = 3;
        r3.intensity = 0;
        r3.returnNumber = 3;
        r3.numberOfReturns = 3;

        records = Arrays.asList(r1, r2, r3);

    }

    public void testGpsTime() throws Exception {
        double adjustedGpsTime = 206990.87;
        DateTime gpsTimeToDateTime = LasUtils.gpsTimeToDateTime(adjustedGpsTime, 1);
        assertEquals("2011-09-16T11:16:30.870Z", gpsTimeToDateTime.toString());

        double dateTimeToStandardGpsTime = LasUtils.dateTimeToStandardGpsTime(gpsTimeToDateTime);
        double adjusted = dateTimeToStandardGpsTime - 1E9;
        assertEquals(adjustedGpsTime, adjusted, DELTA);
    }

    public void testWeekSecondsTimeConversion() throws Exception {
        double weekSecondsTime = 1622.379604;
        DateTime gpsTimeToDateTime = LasUtils.gpsTimeToDateTime(weekSecondsTime, 0);
        String expected = "2011-02-10 09:26:44";
        assertEquals(expected, gpsTimeToDateTime.toString(JGTConstants.dateTimeFormatterYYYYMMDDHHMMSS));

        weekSecondsTime = 1622.386961;
        gpsTimeToDateTime = LasUtils.gpsTimeToDateTime(weekSecondsTime, 0);
        expected = "2011-02-10 11:29:21";
        assertEquals(expected, gpsTimeToDateTime.toString(JGTConstants.dateTimeFormatterYYYYMMDDHHMMSS));
    }

    public void testAvg() throws Exception {
        double avg = LasUtils.avg(records, VALUETYPE.ELEVATION);
        assertEquals(6.0, avg, DELTA);
        avg = LasUtils.avg(records, VALUETYPE.CLASSIFICATION);
        assertEquals(2.0, avg, DELTA);
        avg = LasUtils.avg(records, VALUETYPE.IMPULSE);
        assertEquals(2.0, avg, DELTA);
        avg = LasUtils.avg(records, VALUETYPE.INTENSITY);
        assertEquals(128.0, avg, DELTA);
    }

    public void testHistogram() throws Exception {
        double[][] avg = LasUtils.histogram(records, VALUETYPE.ELEVATION, 3);
        assertEquals(4.0, avg[0][0], DELTA);
        assertEquals(1.0, avg[0][1], DELTA);
        assertEquals(6.0, avg[1][0], DELTA);
        assertEquals(1.0, avg[1][1], DELTA);
        assertEquals(8.0, avg[2][0], DELTA);
        assertEquals(1.0, avg[2][1], DELTA);
        assertEquals(1.0, avg[2][2], DELTA);
    }

}
