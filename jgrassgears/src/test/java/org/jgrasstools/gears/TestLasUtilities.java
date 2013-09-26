package org.jgrasstools.gears;

import org.jgrasstools.gears.io.las.utils.LasUtils;
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
        double adjusted = dateTimeToStandardGpsTime / 1000 - 1E9;
        assertEquals(adjustedGpsTime, adjusted, DELTA);
    }

}
