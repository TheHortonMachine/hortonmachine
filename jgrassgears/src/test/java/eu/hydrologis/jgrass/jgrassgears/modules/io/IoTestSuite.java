package eu.hydrologis.jgrass.jgrassgears.modules.io;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class IoTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(TestId2ValueReader.class);
        suite.addTestSuite(TestShapefileIO.class);

        return suite;
    }
}
