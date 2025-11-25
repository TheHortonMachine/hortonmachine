package org.hortonmachine.gears;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.SyntheticCalibrationPso;
import org.hortonmachine.gears.utils.optimizers.particleswarm.IPSFunction;
import org.hortonmachine.gears.utils.optimizers.particleswarm.PSEngine;
import org.hortonmachine.gears.utils.optimizers.sceua.CostFunctions;
import org.junit.Test;

public class TestOptimizations {
	@Test
	public void testKge1() {
		// 1) Perfect match: KGE should be ~1
		double[] obs1 = { 1, 2, 3, 4, 5 };
		double[] sim1 = { 1, 2, 3, 4, 5 };
		double kge1 = CostFunctions.kge(obs1, sim1, 0, HMConstants.doubleNovalue);

		assertTrue(Math.abs(kge1 - 1.0) < 1e-6);
	}

	@Test
	public void testKge2() {
		// 2) Pure bias: sim = obs + 10 → R = 1, alpha = 1, beta != 1 → KGE < 0
		double[] obs2 = { 1, 2, 3, 4, 5 };
		double[] sim2 = { 11, 12, 13, 14, 15 };
		double kge2 = CostFunctions.kge(obs2, sim2, 0, HMConstants.doubleNovalue);

		assertTrue(kge2 < 0.0); // bias gives negative KGE
	}

	@Test
	public void testKge3() {

		// 3) Pure scaling: sim = 2 * obs → R = 1, alpha = 2, beta = 2 → KGE < 0
		double[] obs3 = { 1, 2, 3, 4, 5 };
		double[] sim3 = { 2, 4, 6, 8, 10 };
		double kge3 = CostFunctions.kge(obs3, sim3, 0, HMConstants.doubleNovalue);

		assertTrue(kge3 < 0.0); // pure scaling also gives negative KGE
	}

	@Test
	public void testKge4() {
		// 4) Opposite sign: sim = -obs → R = -1, alpha=1, beta=-1 → KGE should be
		// strongly negative
		double[] obs4 = { 1, 2, 3, 4, 5 };
		double[] sim4 = { -1, -2, -3, -4, -5 };
		double kge4 = CostFunctions.kge(obs4, sim4, 0, HMConstants.doubleNovalue);

		assertTrue(kge4 < -0.5);
	}

	@Test
	public void testKge5() {
		// 5) Slight noise: sim = obs + small noise → KGE should be close to 1
		double[] obs5 = new double[100];
		double[] sim5 = new double[100];
		for (int i = 0; i < 100; i++) {
			obs5[i] = i;
			sim5[i] = i + ((i % 5) - 2) * 0.05; // small, structured noise
		}
		double kge5 = CostFunctions.kge(obs5, sim5, 0, HMConstants.doubleNovalue);

		assertTrue(kge5 > 0.8);
	}

	@Test
	public void testPsoKgeConvergence() throws Exception {
		int n = 200;
		double omega = 2 * Math.PI / 30.0;

		double A_true = 10.0;
		double phi_true = 0.7;
		double B_true = 5.0;

		double[] obs = new double[n];
		Random rnd = new Random(1234);
		for (int t = 0; t < n; t++) {
			double clean = A_true * Math.sin(omega * t + phi_true) + B_true;
			obs[t] = clean + rnd.nextGaussian() * 0.3;
		}

		IPSFunction func = new SyntheticCalibrationPso(obs, omega);

		double[][] ranges = { { 0, 20 }, { -Math.PI, Math.PI }, { -10, 20 } };

		PSEngine engine = new PSEngine(30, 800, 2.0, 2.0, 0.9, 0.4, func, 4, "TEST");

		engine.initializeRanges(ranges);
		engine.run();

		double[] best = engine.getSolution();
		double bestCost = engine.getSolutionFittingValue();
		double bestKGE = -bestCost;

		System.out.println("Recovered params = " + Arrays.toString(best));
		System.out.println("Recovered KGE    = " + bestKGE);

		// Assert PSO works
		assertTrue(bestKGE > 0.85); // close fit
		assertEquals(A_true, best[0], 1.0);
		assertEquals(phi_true, best[1], 0.2);
		assertEquals(B_true, best[2], 0.5);
	}

}
