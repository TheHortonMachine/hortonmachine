package eu.hydrologis.jgrass.hortonmachine.models;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import eu.hydrologis.jgrass.hortonmachine.models.io.TestId2ValueReader;
import eu.hydrologis.jgrass.hortonmachine.models.io.TestShapefileIO;

public class IoTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(TestId2ValueReader.class);
        suite.addTestSuite(TestShapefileIO.class);

        return suite;
    }
}
