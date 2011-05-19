package org.jgrasstools.gears;

import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.time.UtcTimeUtilities;
import org.joda.time.DateTime;

/**
 * Test time.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestUtcTimeUtilities extends HMTestCase {

    public void testUtcTimeUtilities() throws Exception {

        String dtString = "2010-09-01 00:00";
        DateTime dt = UtcTimeUtilities.fromStringWithMinutes(dtString);
        DateTime plusSeconds = dt.plusSeconds(30);
        String dtWithSeconds = UtcTimeUtilities.toStringWithSeconds(plusSeconds);
        assertEquals(dtString + ":30", dtWithSeconds);
    }
}
