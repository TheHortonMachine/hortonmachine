package org.jgrasstools.hortonmachine;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jgrasstools.hortonmachine.models.hm.TestAb;
import org.jgrasstools.hortonmachine.models.hm.TestAspect;
import org.jgrasstools.hortonmachine.models.hm.TestDrainDir;
import org.jgrasstools.hortonmachine.models.hm.TestExtractNetwork;
import org.jgrasstools.hortonmachine.models.hm.TestFlowDirections;
import org.jgrasstools.hortonmachine.models.hm.TestPitfiller;

public class HortonTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(TestAb.class);
        // suite.addTestSuite(TestAdige.class);
        suite.addTestSuite(TestAspect.class);
        // suite.addTestSuite(TestBasinShape.class);
        // suite.addTestSuite(TestCb.class);
        // suite.addTestSuite(TestCurvatures.class);
        suite.addTestSuite(TestDrainDir.class);
        // suite.addTestSuite(TestEnergyBalance.class);
        // suite.addTestSuite(TestEnergyIndexCalculator.class);
        suite.addTestSuite(TestExtractNetwork.class);
        suite.addTestSuite(TestFlowDirections.class);
        // suite.addTestSuite(TestGradient.class);
        // suite.addTestSuite(TestHacklength.class);
        // suite.addTestSuite(TestHackstream.class);
        // suite.addTestSuite(TestHillshade.class);
        // suite.addTestSuite(TestInsolation.class);
        // suite.addTestSuite(TestJami.class);
        // suite.addTestSuite(TestKriging.class);
        // suite.addTestSuite(TestNetnumbering.class);
        // // suite.addTestSuite(TestPeakflow.class);
        // suite.addTestSuite(TestPfafstetter.class);
        suite.addTestSuite(TestPitfiller.class);
        // suite.addTestSuite(TestRescaledDistance.class);
        // suite.addTestSuite(TestShalstab.class);
        // suite.addTestSuite(TestSkyview.class);
        // suite.addTestSuite(TestSlope.class);
        // suite.addTestSuite(TestTca.class);
        // suite.addTestSuite(TestTca3d.class);
        // suite.addTestSuite(TestTopindex.class);
        // suite.addTestSuite(TestValidationDoubleStation.class);
        // suite.addTestSuite(TestWateroutlet.class);

        return suite;
    }
}
