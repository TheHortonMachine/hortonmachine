package org.jgrasstools.hortonmachine;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jgrasstools.hortonmachine.models.hm.TestAb;
import org.jgrasstools.hortonmachine.models.hm.TestAspect;
import org.jgrasstools.hortonmachine.models.hm.TestBasinShape;
import org.jgrasstools.hortonmachine.models.hm.TestCb;
import org.jgrasstools.hortonmachine.models.hm.TestCurvatures;
import org.jgrasstools.hortonmachine.models.hm.TestDrainDir;
import org.jgrasstools.hortonmachine.models.hm.TestExtractNetwork;
import org.jgrasstools.hortonmachine.models.hm.TestFlowDirections;
import org.jgrasstools.hortonmachine.models.hm.TestGradient;
import org.jgrasstools.hortonmachine.models.hm.TestHackStream;
import org.jgrasstools.hortonmachine.models.hm.TestHacklength;
import org.jgrasstools.hortonmachine.models.hm.TestHillshade;
import org.jgrasstools.hortonmachine.models.hm.TestInsolation;
import org.jgrasstools.hortonmachine.models.hm.TestMarkoutlets;
import org.jgrasstools.hortonmachine.models.hm.TestNetnumbering;
import org.jgrasstools.hortonmachine.models.hm.TestPitfiller;
import org.jgrasstools.hortonmachine.models.hm.TestRescaledDistance;
import org.jgrasstools.hortonmachine.models.hm.TestShalstab;
import org.jgrasstools.hortonmachine.models.hm.TestSkyview;
import org.jgrasstools.hortonmachine.models.hm.TestSlope;
import org.jgrasstools.hortonmachine.models.hm.TestSumDownstream;
import org.jgrasstools.hortonmachine.models.hm.TestTca;
import org.jgrasstools.hortonmachine.models.hm.TestTca3d;
import org.jgrasstools.hortonmachine.models.hm.TestTopindex;
import org.jgrasstools.hortonmachine.models.hm.TestWateroutlet;

public class HortonTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(TestAb.class);
        // suite.addTestSuite(TestAdige.class);
        suite.addTestSuite(TestAspect.class);
        suite.addTestSuite(TestBasinShape.class);
        suite.addTestSuite(TestCb.class);
        suite.addTestSuite(TestCurvatures.class);
        suite.addTestSuite(TestDrainDir.class);
        // suite.addTestSuite(TestEnergyBalance.class);
        // suite.addTestSuite(TestEnergyIndexCalculator.class);
        suite.addTestSuite(TestExtractNetwork.class);
        suite.addTestSuite(TestFlowDirections.class);
        suite.addTestSuite(TestGradient.class);
        suite.addTestSuite(TestHacklength.class);
        suite.addTestSuite(TestHackStream.class);
        suite.addTestSuite(TestHillshade.class);
        suite.addTestSuite(TestInsolation.class);
        // suite.addTestSuite(TestJami.class);
        // suite.addTestSuite(TestKriging.class);
        suite.addTestSuite(TestMarkoutlets.class);
        suite.addTestSuite(TestNetnumbering.class);
        // suite.addTestSuite(TestPfafstetter.class);
        suite.addTestSuite(TestPitfiller.class);
        suite.addTestSuite(TestRescaledDistance.class);
        suite.addTestSuite(TestShalstab.class);
        suite.addTestSuite(TestSkyview.class);
        suite.addTestSuite(TestSlope.class);
        suite.addTestSuite(TestSumDownstream.class);
        suite.addTestSuite(TestTca.class);
        suite.addTestSuite(TestTca3d.class);
        suite.addTestSuite(TestTopindex.class);
        suite.addTestSuite(TestWateroutlet.class);

        return suite;
    }
}
