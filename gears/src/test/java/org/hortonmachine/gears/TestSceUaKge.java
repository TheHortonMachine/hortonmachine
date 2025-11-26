package org.hortonmachine.gears;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.hortonmachine.gears.utils.optimizers.CostFunctions;
import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaConfig;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaOptimizer;
import org.hortonmachine.gears.utils.optimizers.sceua.SceUaResult;
import org.junit.Test;

public class TestSceUaKge {

    /**
     * Simple synthetic calibration test:
     * Q_sim = a * P + b  with true a=2, b=1.
     *
     * We generate noisy observations from the true model and see if
     * SCE-UA + KGE can recover (a,b) reasonably well.
     */
    @Test
    public void testLinearModelCalibrationWithKGE() {
        // ----- 1) Generate synthetic forcing and "observed" data -----
        int n = 200;
        double[] P = new double[n];      // "rainfall"
        double[] observedQ = new double[n];

        double trueA = 2.0;
        double trueB = 1.0;

        Random rnd = new Random(1234L);
        for (int t = 0; t < n; t++) {
            P[t] = 10.0 * rnd.nextDouble();   // between 0 and 10
            double noise = rnd.nextGaussian() * 0.3; // small noise
            observedQ[t] = trueA * P[t] + trueB + noise;
        }

        // ----- 2) Define parameter bounds -----
        List<ParameterBounds> bounds = Arrays.asList(
                new ParameterBounds("a", -5.0, 5.0),
                new ParameterBounds("b", -5.0, 5.0)
        );

        // ----- 3) Define objective function: KGE cost -----
        SceUaOptimizer.ObjectiveFunction objFn = params -> {
            double a = params[0];
            double b = params[1];

            double[] simQ = simulation(n, P, a, b);

            return -CostFunctions.kge(simQ, observedQ, 0, -9999.0); // minimize -KGE
        };

        // ----- 4) Configure SCE-UA -----
        SceUaConfig config = SceUaConfig.builder()
                .maxIterations(2000)
                .maxEvaluations(5000)
                .complexCount(5)
                .objectiveStdTolerance(1e-4)
                .random(new Random(42L))   // deterministic
                .verbose(false)
                .build();

        // ----- 5) Run optimization -----
        SceUaOptimizer optimizer = new SceUaOptimizer(bounds, objFn, config);
        SceUaResult result = optimizer.optimizeParallel(10);

        double[] best = result.getBestParameters();
        double bestObj = result.getBestObjective();

        System.out.println("Best objective (cost) = " + bestObj);
        System.out.println("Best params a,b = " + Arrays.toString(best));

        // Convert cost back to KGE for interpretation
        double bestKGE = -bestObj;
        System.out.println("Best KGE = " + bestKGE);

        // ----- 6) Assertions -----
        // KGE should be close to 1
        assertTrue("KGE too low", bestKGE > 0.95);

        // Optional sanity checks:
        assertTrue("a out of range", best[0] >= -5 && best[0] <= 5);
        assertTrue("b out of range", best[1] >= -5 && best[1] <= 5);
    }

	private double[] simulation(int n, double[] P, double a, double b) {
		double[] simQ = new double[n];
		for (int t = 0; t < n; t++) {
		    simQ[t] = a * P[t] + b;
		}
		return simQ;
	}
}
