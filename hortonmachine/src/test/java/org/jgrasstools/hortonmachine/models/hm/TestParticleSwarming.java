//*
package org.jgrasstools.hortonmachine.models.hm;

import org.jgrasstools.hortonmachine.modules.calibrations.ParticleSwarming;
import org.jgrasstools.hortonmachine.utils.HMTestCase;

/**
 * Test the {@link ParticleSwarming} module.
 * 
 * @author Giuseppe Formetta ()
 */
public class TestParticleSwarming extends HMTestCase {

    public void testParticleSwarming() throws Exception {

        ParticleSwarming d = new ParticleSwarming();
        double[] pmin = {-5, -5};
        double[] pmax = {5, 5};
        d.ParRange_minn = pmin;
        d.ParRange_maxn = pmax;
        d.ModelName = "Banana";
        d.pm = pm;

        d.kmax = 1000;
        d.p = 30;
        d.parameters = 2;

        d.process();

        double minimo = d.outOpt;
        double valori[] = d.outOptSet;

        double min_truth = 0;
        double val_truth1 = 1;
        double val_truth2 = 1;

        assertEquals(val_truth1, valori[0], 0.001);
        assertEquals(val_truth2, valori[1], 0.001);
        assertEquals(min_truth, minimo, 0.001);

        ParticleSwarming d2 = new ParticleSwarming();
        double[] pmin2 = {-2 * Math.PI, -2 * Math.PI};
        double[] pmax2 = {2 * Math.PI, 2 * Math.PI};
        d2.ParRange_minn = pmin2;
        d2.ParRange_maxn = pmax2;
        d2.ModelName = "Eggcrate";
        d2.pm = pm;
        d2.kmax = 1000;
        d2.p = 30;
        d2.parameters = 2;

        d2.process();
        double minimo2 = d2.outOpt;
        double valori2[] = d2.outOptSet;

        double min_truth2 = 0;
        double val_truth1_2 = 0;
        double val_truth2_2 = 0;

        assertEquals(val_truth1_2, valori2[0], 0.001);
        assertEquals(val_truth2_2, valori2[1], 0.001);
        assertEquals(min_truth2, minimo2, 0.001);

    }

}
