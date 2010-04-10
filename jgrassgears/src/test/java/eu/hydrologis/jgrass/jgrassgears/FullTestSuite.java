package eu.hydrologis.jgrass.jgrassgears;

import eu.hydrologis.jgrass.jgrassgears.modules.TestMapcalc;
import eu.hydrologis.jgrass.jgrassgears.modules.TestReprojectors;
import eu.hydrologis.jgrass.jgrassgears.modules.io.TestId2ValueReader;
import eu.hydrologis.jgrass.jgrassgears.modules.io.TestShapefileIO;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FullTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        // IO
        suite.addTestSuite(TestId2ValueReader.class);
        suite.addTestSuite(TestShapefileIO.class);

        // modules
        suite.addTestSuite(TestMapcalc.class);
        suite.addTestSuite(TestReprojectors.class);

        return suite;
    }
}
