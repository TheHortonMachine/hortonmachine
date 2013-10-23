package org.jgrasstools.gears;

import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.HMTestCase;
import org.joda.time.DateTime;

/**
 * Test las utils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestLasUtilities extends HMTestCase {

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

}
