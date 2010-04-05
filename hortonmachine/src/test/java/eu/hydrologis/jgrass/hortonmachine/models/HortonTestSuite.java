package eu.hydrologis.jgrass.hortonmachine.models;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestAb;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestCb;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestCurvatures;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestDrain;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestEnergyIndexCalculator;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestExtractNetwork0;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestExtractNetwork1;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestFlow;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestGradient;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestJami;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestKriging;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestPitfiller;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestRescaledDistance;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestShalstab;
import eu.hydrologis.jgrass.hortonmachine.models.hm.TestTopindex;

public class HortonTestSuite extends TestCase {

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();

        suite.addTestSuite(TestAb.class);
        suite.addTestSuite(TestCb.class);
        suite.addTestSuite(TestCurvatures.class);
        suite.addTestSuite(TestDrain.class);
        suite.addTestSuite(TestExtractNetwork0.class);
        suite.addTestSuite(TestExtractNetwork1.class);
        suite.addTestSuite(TestFlow.class);
        suite.addTestSuite(TestGradient.class);
        suite.addTestSuite(TestPitfiller.class);
        suite.addTestSuite(TestRescaledDistance.class);
        suite.addTestSuite(TestTopindex.class);

        suite.addTestSuite(TestKriging.class);
        suite.addTestSuite(TestEnergyIndexCalculator.class);
        suite.addTestSuite(TestJami.class);
        
        suite.addTestSuite(TestShalstab.class);

        // suite.addTestSuite(TestAspect.class);
        // suite.addTestSuite(TestD2O.class);
        // suite.addTestSuite(TestD2O3d.class);
        // suite.addTestSuite(TestDD.class);
        // suite.addTestSuite(TestDiameters.class);
        // suite.addTestSuite(TestDistEucli y4 dea.class);
        // suite.addTestSuite(TestGc.class);
        // suite.addTestSuite(Testh2cd0.class);
        // suite.addTestSuite(Testh2cD1.class);
        // suite.addTestSuite(TestH2cd3D.class);
        // suite.addTestSuite(TestHackLength.class);
        // suite.addTestSuite(TestHackLength3D.class);
        // suite.addTestSuite(TestHackStream.class);
        // suite.addTestSuite(TestMagnitudo.class);
        // suite.addTestSuite(TestMarkOutlets.class);
        // suite.addTestSuite(TestMeanDrop.class);
        // suite.addTestSuite(TestMultiTca.class);
        // suite.addTestSuite(TestNabla.class);
        // suite.addTestSuite(TestNabla1.class);
        // suite.addTestSuite(TestNetnumbering.class);
        // suite.addTestSuite(TestNetnumbering1.class);
        // suite.addTestSuite(TestSeol.class);
        // suite.addTestSuite(TestSlope.class);
        // suite.addTestSuite(TestSplitSubBasin.class);
        // suite.addTestSuite(TestSrahler.class);
        // suite.addTestSuite(TestSumdownstream.class);
        // suite.addTestSuite(TestTau.class);
        // suite.addTestSuite(TestTc.class);
        // suite.addTestSuite(TestTca.class);
        // suite.addTestSuite(TestTca3D.class);
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
