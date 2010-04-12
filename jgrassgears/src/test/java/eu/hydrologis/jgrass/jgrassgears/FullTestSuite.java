package eu.hydrologis.jgrass.jgrassgears;

import eu.hydrologis.jgrass.jgrassgears.modules.TestMapcalc;
import eu.hydrologis.jgrass.jgrassgears.modules.TestReprojectors;
import eu.hydrologis.jgrass.jgrassgears.modules.TestSourceDirection;
import eu.hydrologis.jgrass.jgrassgears.modules.io.TestAdigeBoundaryConditions;
import eu.hydrologis.jgrass.jgrassgears.modules.io.TestId2ValueConverters;
import eu.hydrologis.jgrass.jgrassgears.modules.io.TestId2ValueReader;
import eu.hydrologis.jgrass.jgrassgears.modules.io.TestShapefileIO;
import eu.hydrologis.jgrass.jgrassgears.modules.io.TestVegetationLibraryReader;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FullTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        // IO
        suite.addTestSuite(TestId2ValueReader.class);
        suite.addTestSuite(TestShapefileIO.class);
        suite.addTestSuite(TestVegetationLibraryReader.class);
        suite.addTestSuite(TestId2ValueConverters.class);
        suite.addTestSuite(TestAdigeBoundaryConditions.class);

        // modules
        suite.addTestSuite(TestMapcalc.class);
        suite.addTestSuite(TestReprojectors.class);
        suite.addTestSuite(TestSourceDirection.class);

        return suite;
    }
}
