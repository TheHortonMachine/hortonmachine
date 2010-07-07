package org.jgrasstools.hortonmachine;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jgrasstools.hortonmachine.models.hm.TestAb;
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
import org.jgrasstools.hortonmachine.models.hm.TestHillshade;
import org.jgrasstools.hortonmachine.models.hm.TestInsolation;
import org.jgrasstools.hortonmachine.models.hm.TestKriging;
import org.jgrasstools.hortonmachine.models.hm.TestNetnumbering;
import org.jgrasstools.hortonmachine.models.hm.TestPitfiller;
import org.jgrasstools.hortonmachine.models.hm.TestRescaledDistance;
import org.jgrasstools.hortonmachine.models.hm.TestShalstab;
import org.jgrasstools.hortonmachine.models.hm.TestSkyview;
import org.jgrasstools.hortonmachine.models.hm.TestSlope;
import org.jgrasstools.hortonmachine.models.hm.TestTca;
import org.jgrasstools.hortonmachine.models.hm.TestTca3d;
import org.jgrasstools.hortonmachine.models.hm.TestTopindex;
import org.jgrasstools.hortonmachine.models.hm.TestWateroutlet;

public class HortonTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(TestAb.class);
        suite.addTestSuite(TestAspect.class);
        suite.addTestSuite(TestCb.class);
        suite.addTestSuite(TestCurvatures.class);
        suite.addTestSuite(TestDrain.class);
        suite.addTestSuite(TestEnergyBalance.class);
        suite.addTestSuite(TestEnergyIndexCalculator.class);
        suite.addTestSuite(TestExtractNetwork0.class);
        suite.addTestSuite(TestExtractNetwork1.class);
        suite.addTestSuite(TestFlow.class);
        suite.addTestSuite(TestGradient.class);
        // suite.addTestSuite(TestJami.class);
        suite.addTestSuite(TestKriging.class);
        suite.addTestSuite(TestNetnumbering.class);
        suite.addTestSuite(TestTca.class);
        suite.addTestSuite(TestTca3d.class);

        suite.addTestSuite(TestPitfiller.class);
        suite.addTestSuite(TestRescaledDistance.class);
        suite.addTestSuite(TestShalstab.class);
        suite.addTestSuite(TestTopindex.class);
        suite.addTestSuite(TestWateroutlet.class);

        suite.addTestSuite(TestBasinShape.class);
        suite.addTestSuite(TestSkyview.class);
        suite.addTestSuite(TestHillshade.class);
        suite.addTestSuite(TestInsolation.class);

        // suite.addTestSuite(TestD2O.class);
        // suite.addTestSuite(TestD2O3d.class);
        // suite.addTestSuite(TestDD.class);
        // suite.addTestSuite(TestDiameters.class);
        // suite.addTestSuite(TestDistEucli y4 dea.class);
        // suite.addTestSuite(TestGc.class);
        // suite.addTestSuite(Testh2cd0.class);
        // suite.addTestSuite(Testh2cD1.class);
        // suite.addTestSuite(TestH2cd3D.class);
        suite.addTestSuite(TestHacklength.class);
        // suite.addTestSuite(TestHackLength3D.class);
        // suite.addTestSuite(TestHackStream.class);
        // suite.addTestSuite(TestMagnitudo.class);
        // suite.addTestSuite(TestMarkOutlets.class);
        // suite.addTestSuite(TestMeanDrop.class);
        // suite.addTestSuite(TestMultiTca.class);
        // suite.addTestSuite(TestNabla.class);
        // suite.addTestSuite(TestNabla1.class);
        // suite.addTestSuite(TestNetnumbering1.class);
        // suite.addTestSuite(TestSeol.class);
        suite.addTestSuite(TestSlope.class);
        // suite.addTestSuite(TestSplitSubBasin.class);
        // suite.addTestSuite(TestSrahler.class);
        // suite.addTestSuite(TestSumdownstream.class);
        // suite.addTestSuite(TestTau.class);
        // suite.addTestSuite(TestTc.class);

        // 
        //        
        // 
        // 
        // suite.addTestSuite(TestRescaledDistance3d.class);
        // suite.addTestSuite(TestWateroutlet.class);
        // suite.addTestSuite(TestH2ca.class);
        // 
        //
        // suite.addTestSuite(TestTrasmissivity.class);
        // suite.addTestSuite(TestNetdif.class);

        return suite;
    }
}
