package org.jgrasstools.hortonmachine.releasetestsuites;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jgrasstools.gears.modules.TestCoverageSummary;
import org.jgrasstools.gears.modules.TestCutout;
import org.jgrasstools.gears.modules.TestFeatureFilter;
import org.jgrasstools.gears.modules.TestFeatureReshaper;
import org.jgrasstools.gears.modules.TestMapcalc;
import org.jgrasstools.gears.modules.TestMarchingSquaresAndRasterizer;
import org.jgrasstools.gears.modules.TestRasterCatToFeatureAttribute;
import org.jgrasstools.gears.modules.TestReprojectors;
import org.jgrasstools.gears.modules.io.TestRasterReader;
import org.jgrasstools.gears.modules.io.TestShapefileIO;
import org.jgrasstools.hortonmachine.models.hm.TestAb;
import org.jgrasstools.hortonmachine.models.hm.TestAdige;
import org.jgrasstools.hortonmachine.models.hm.TestAspect;
import org.jgrasstools.hortonmachine.models.hm.TestBasinShape;
import org.jgrasstools.hortonmachine.models.hm.TestCb;
import org.jgrasstools.hortonmachine.models.hm.TestCurvatures;
import org.jgrasstools.hortonmachine.models.hm.TestDrain;
import org.jgrasstools.hortonmachine.models.hm.TestEnergyBalance;
import org.jgrasstools.hortonmachine.models.hm.TestEnergyIndexCalculator;
import org.jgrasstools.hortonmachine.models.hm.TestExtractNetwork0;
import org.jgrasstools.hortonmachine.models.hm.TestExtractNetwork1;
import org.jgrasstools.hortonmachine.models.hm.TestFlow;
import org.jgrasstools.hortonmachine.models.hm.TestGradient;
import org.jgrasstools.hortonmachine.models.hm.TestHacklength;
import org.jgrasstools.hortonmachine.models.hm.TestHackstream;
import org.jgrasstools.hortonmachine.models.hm.TestHillshade;
import org.jgrasstools.hortonmachine.models.hm.TestInsolation;
import org.jgrasstools.hortonmachine.models.hm.TestJami;
import org.jgrasstools.hortonmachine.models.hm.TestKriging;
import org.jgrasstools.hortonmachine.models.hm.TestNetnumbering;
import org.jgrasstools.hortonmachine.models.hm.TestPfafstetter;
import org.jgrasstools.hortonmachine.models.hm.TestPitfiller;
import org.jgrasstools.hortonmachine.models.hm.TestRescaledDistance;
import org.jgrasstools.hortonmachine.models.hm.TestShalstab;
import org.jgrasstools.hortonmachine.models.hm.TestSkyview;
import org.jgrasstools.hortonmachine.models.hm.TestSlope;
import org.jgrasstools.hortonmachine.models.hm.TestTca;
import org.jgrasstools.hortonmachine.models.hm.TestTca3d;
import org.jgrasstools.hortonmachine.models.hm.TestTopindex;
import org.jgrasstools.hortonmachine.models.hm.TestWateroutlet;

/**
 * Testsuite that tests release 01, as of:
 * http://code.google.com/p/jgrasstools/wiki/RoadMap
 */
public class Release01TestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(TestPitfiller.class);
        suite.addTestSuite(TestWateroutlet.class);
        suite.addTestSuite(TestAb.class);
        suite.addTestSuite(TestAspect.class);
        suite.addTestSuite(TestCurvatures.class);
        suite.addTestSuite(TestDrain.class);
        suite.addTestSuite(TestFlow.class);
        suite.addTestSuite(TestGradient.class);
        suite.addTestSuite(TestExtractNetwork0.class);
        suite.addTestSuite(TestExtractNetwork1.class);
        suite.addTestSuite(TestNetnumbering.class);
        suite.addTestSuite(TestCb.class);
        suite.addTestSuite(TestJami.class);
        suite.addTestSuite(TestKriging.class);
        suite.addTestSuite(TestAdige.class);
        suite.addTestSuite(TestEnergyBalance.class);
        suite.addTestSuite(TestEnergyIndexCalculator.class);
        suite.addTestSuite(TestShalstab.class);
        suite.addTestSuite(TestHacklength.class);
        suite.addTestSuite(TestHackstream.class);
        suite.addTestSuite(TestBasinShape.class);
        suite.addTestSuite(TestPfafstetter.class);
        suite.addTestSuite(TestSkyview.class);
        suite.addTestSuite(TestHillshade.class);
        suite.addTestSuite(TestInsolation.class);
        suite.addTestSuite(TestRescaledDistance.class);
        suite.addTestSuite(TestSlope.class);
        suite.addTestSuite(TestTca.class);
        suite.addTestSuite(TestTca3d.class);
        suite.addTestSuite(TestTopindex.class);
        suite.addTestSuite(TestReprojectors.class);
        suite.addTestSuite(TestCoverageSummary.class);
        suite.addTestSuite(TestMapcalc.class);
        suite.addTestSuite(TestMarchingSquaresAndRasterizer.class);
        suite.addTestSuite(TestRasterCatToFeatureAttribute.class);
        // suite.addTestSuite(TestGeometrySimplifier.class);
        // suite.addTestSuite(TestAttributesJoiner.class);
        // suite.addTestSuite(TestAttributesRounder.class);
        suite.addTestSuite(TestFeatureFilter.class);
        suite.addTestSuite(TestFeatureReshaper.class);
        suite.addTestSuite(TestCutout.class);
        suite.addTestSuite(TestRasterReader.class);
        suite.addTestSuite(TestShapefileIO.class);

        return suite;
    }
}
