package org.hortonmachine.gears.utils;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.optimizers.CostFunctions;
import org.hortonmachine.gears.utils.optimizers.particleswarm.IPSFunction;

/**
 * Synthetic IPSFunction: params = [A, phi, B] obs[t] = A_true * sin(ω t +
 * φ_true) + B_true sim[t] = A * sin(ω t + φ) + B
 *
 * cost = -KGE(obs, sim)
 */
public class SyntheticCalibrationPso implements IPSFunction {

	private final double[] obs;
	private final double omega;
	private final int n;
	private Double previousGlobalBest = null;
	private int stagnantIterations = 0;

	private final double paramAbsTol = 1e-4;
	private final double valueAbsTol = 1e-4;
	private final double valueRelTol = 1e-3;
	private final int maxStagnantIterations = 50;

	public SyntheticCalibrationPso(double[] obs, double omega) {
		this.obs = obs;
		this.omega = omega;
		this.n = obs.length;
	}

	@Override
	public double evaluateCost(int iterationStep, int particleNum, double[] params, double[]... ranges)
			throws Exception {
		double A = params[0];
		double phi = params[1];
		double B = params[2];

		double[] sim = new double[n];
		for (int t = 0; t < n; t++) {
			sim[t] = A * Math.sin(omega * t + phi) + B;
		}

		int spinup = 0;
		double noData = HMConstants.doubleNovalue;
		double kge = CostFunctions.kge(obs, sim, spinup, noData);

		if (Double.isNaN(kge) || Double.isInfinite(kge)) {
			return 1e9; // penalize invalid
		}

		double cost = -kge; // PSO minimizes
		return cost;
	}

	@Override
	public String optimizationDescription() {
		return "Synthetic sine calibration with KGE";
	}

	@Override
	public boolean isBetter(double evaluatedValue, double consideredBest) {
		return evaluatedValue < consideredBest; // minimization
	}

	@Override
	public boolean hasConverged(double globalBest, double[] globalBestLocations, double[] previousBestLocations) {
		if (globalBestLocations == null || previousBestLocations == null) {
			previousGlobalBest = globalBest;
			stagnantIterations = 0;
			return false;
		}

		// param movement
		double maxParamDiff = 0.0;
		for (int i = 0; i < globalBestLocations.length; i++) {
			maxParamDiff = Math.max(maxParamDiff, Math.abs(globalBestLocations[i] - previousBestLocations[i]));
		}
		boolean paramsStable = maxParamDiff < paramAbsTol;

		// cost improvement
		double valueImprovement = (previousGlobalBest == null) ? Double.POSITIVE_INFINITY
				: Math.abs(previousGlobalBest - globalBest);

		double denom = Math.max(Math.abs(previousGlobalBest != null ? previousGlobalBest : globalBest), 1e-9);
		double relImprovement = valueImprovement / denom;

		boolean valueStable = valueImprovement < valueAbsTol || relImprovement < valueRelTol;

		if (paramsStable && valueStable) {
			stagnantIterations++;
		} else {
			stagnantIterations = 0;
		}

		previousGlobalBest = globalBest;
		return stagnantIterations >= maxStagnantIterations;
	}

	@Override
	public double getInitialGlobalBest() {
		return Double.POSITIVE_INFINITY;
	}

}