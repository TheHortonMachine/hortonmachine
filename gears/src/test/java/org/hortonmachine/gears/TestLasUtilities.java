package org.hortonmachine.gears;

import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.EGpsWeekDays;
import org.hortonmachine.gears.io.las.utils.GpsTimeConverter;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.io.las.utils.LasUtils.VALUETYPE;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.HMTestCase;
import org.joda.time.DateTime;

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
        DateTime gpsTimeToDateTime = LasUtils.adjustedStandardGpsTime2DateTime(adjustedGpsTime);
        assertEquals("2011-09-16T11:16:30.870Z", gpsTimeToDateTime.toString());

        double dateTimeToStandardGpsTime = LasUtils.dateTimeToStandardGpsTime(gpsTimeToDateTime);
        double adjusted = dateTimeToStandardGpsTime - 1E9;
        assertEquals(adjustedGpsTime, adjusted, DELTA);
    }

    public void testWeekSecondsTimeConversion() throws Exception {
        double weekSecondsTime = 1622.379604;
        DateTime gpsTimeToDateTime = GpsTimeConverter.gpsWeekTime2DateTime(weekSecondsTime);
        String expected = "1980-01-06 00:27:02";
        assertEquals(expected, gpsTimeToDateTime.toString(HMConstants.utcDateFormatterYYYYMMDDHHMMSS));

        weekSecondsTime = 1622.386961;
        gpsTimeToDateTime = GpsTimeConverter.gpsWeekTime2DateTime(weekSecondsTime);
        expected = "1980-01-06 00:27:02";
        assertEquals(expected, gpsTimeToDateTime.toString(HMConstants.utcDateFormatterYYYYMMDDHHMMSS));
    }

    public void testWeekDaysFromTime() throws Exception {
        double seconds = 43574.480238020005;
        assertEquals(EGpsWeekDays.SUNDAY, EGpsWeekDays.getDay4Seconds(seconds));
        seconds = 108370.480238020005;
        assertEquals(EGpsWeekDays.MONDAY, EGpsWeekDays.getDay4Seconds(seconds));
        seconds = 238270.480238020005;
        assertEquals(EGpsWeekDays.TUESDAY, EGpsWeekDays.getDay4Seconds(seconds));
        seconds = 382270.1111110005;
        assertEquals(EGpsWeekDays.THURSDAY, EGpsWeekDays.getDay4Seconds(seconds));
        seconds = 490570.9463;
        assertEquals(EGpsWeekDays.FRIDAY, EGpsWeekDays.getDay4Seconds(seconds));
        seconds = 600821.123456;
        assertEquals(EGpsWeekDays.SATURDAY, EGpsWeekDays.getDay4Seconds(seconds));
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

    public void testLastVisiblePointData() throws Exception {
        LasRecord r1 = new LasRecord();
        r1.x = 1.5;
        r1.y = 1.5;
        r1.z = 1.0;

        LasRecord r2 = new LasRecord();
        r2.x = 2.0;
        r2.y = 2.0;
        r2.z = 4.0;

        LasRecord r3 = new LasRecord();
        r3.x = 4.0;
        r3.y = 2.5;
        r3.z = 3.0;

        LasRecord base = new LasRecord();
        base.x = 1.0;
        base.y = 0.5;
        base.z = 2.0;

        double[] lastVisiblePointData = LasUtils.getLastVisiblePointData(base, Arrays.asList(r1, r2, r3), false);
        assertEquals(4.0, lastVisiblePointData[0], DELTA);
        assertEquals(2.0, lastVisiblePointData[1], DELTA);
        assertEquals(2.0, lastVisiblePointData[2], DELTA);
        assertEquals(1.8027756377319946, lastVisiblePointData[3], DELTA);
        assertEquals(42.03111377419729, lastVisiblePointData[4], DELTA);
        assertEquals(1.0, lastVisiblePointData[5], DELTA);
        assertEquals(1.5, lastVisiblePointData[6], DELTA);
        assertEquals(1.5, lastVisiblePointData[7], DELTA);
        assertEquals(1.118033988749895, lastVisiblePointData[8], DELTA);
        assertEquals(131.8103148957786, lastVisiblePointData[9], DELTA);
    }

}
