package org.jgrasstools.gears;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jgrasstools.gears.modules.TestAdigeBoundaryConditions;
import org.jgrasstools.gears.modules.TestCountourLinesLabeler;
import org.jgrasstools.gears.modules.TestRasterSummary;
import org.jgrasstools.gears.modules.TestCutOut;
import org.jgrasstools.gears.modules.TestEiCalculatorInputOutput;
import org.jgrasstools.gears.modules.TestFeatureFilter;
import org.jgrasstools.gears.modules.TestFeatureReshaper;
import org.jgrasstools.gears.modules.TestId2ValueConverters;
import org.jgrasstools.gears.modules.TestId2ValueReader;
import org.jgrasstools.gears.modules.TestMapcalc;
import org.jgrasstools.gears.modules.TestMarchingSquaresAndRasterizer;
import org.jgrasstools.gears.modules.TestRasterCatToFeatureAttribute;
import org.jgrasstools.gears.modules.TestRasterReader;
import org.jgrasstools.gears.modules.TestReprojectors;
import org.jgrasstools.gears.modules.TestVectorReader;
import org.jgrasstools.gears.modules.TestSourceDirection;
import org.jgrasstools.gears.modules.TestVegetationLibraryReader;

public class FullTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        // IO
        suite.addTestSuite(TestAdigeBoundaryConditions.class);
        suite.addTestSuite(TestEiCalculatorInputOutput.class);
        suite.addTestSuite(TestId2ValueConverters.class);
        suite.addTestSuite(TestId2ValueReader.class);
        suite.addTestSuite(TestRasterReader.class);
        suite.addTestSuite(TestVectorReader.class);
        suite.addTestSuite(TestVegetationLibraryReader.class);

        // modules
        suite.addTestSuite(TestCountourLinesLabeler.class);
        suite.addTestSuite(TestRasterSummary.class);
        suite.addTestSuite(TestCutOut.class);
        suite.addTestSuite(TestFeatureFilter.class);
        suite.addTestSuite(TestFeatureReshaper.class);
        suite.addTestSuite(TestMapcalc.class);
        suite.addTestSuite(TestMarchingSquaresAndRasterizer.class);
        suite.addTestSuite(TestRasterCatToFeatureAttribute.class);
        suite.addTestSuite(TestReprojectors.class);
        suite.addTestSuite(TestSourceDirection.class);

        // other
        suite.addTestSuite(TestInterpolators.class);
        suite.addTestSuite(TestNumericUtilities.class);

        return suite;
    }
}
