package org.jgrasstools.gears;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jgrasstools.gears.modules.TestMapcalc;
import org.jgrasstools.gears.modules.TestMarchingSquaresAndRasterizer;
import org.jgrasstools.gears.modules.TestReprojectors;
import org.jgrasstools.gears.modules.TestSourceDirection;
import org.jgrasstools.gears.modules.io.TestAdigeBoundaryConditions;
import org.jgrasstools.gears.modules.io.TestId2ValueConverters;
import org.jgrasstools.gears.modules.io.TestId2ValueReader;
import org.jgrasstools.gears.modules.io.TestShapefileIO;
import org.jgrasstools.gears.modules.io.TestVegetationLibraryReader;

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
        suite.addTestSuite(TestMarchingSquaresAndRasterizer.class);

        return suite;
    }
}
