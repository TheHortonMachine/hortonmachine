package org.hortonmachine.geoframe.calibration;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.optimizers.CostFunctions;
import org.hortonmachine.gears.utils.optimizers.particleswarm.IPSFunction;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.geoframe.utils.WaterSimulationRunner;

public class WaterBudgetCalibrationPsoFunction implements IPSFunction {

	private double[] observedDischarge;
	private GeoframeEnvDatabaseIterator precipReader;
	private GeoframeEnvDatabaseIterator tempReader;
	private GeoframeEnvDatabaseIterator etpReader;

	// --- convergence state ---
	private Double previousGlobalBest = null;
	private int stagnantIterations = 0;

	// tune these to taste
	private final double valueAbsTol = 1e-3; // absolute change in cost
	private final double valueRelTol = 5e-3; // relative change in cost
	private final double paramAbsTol = 1e-2; // max change in any parameter
	private final int maxStagnantIterations = 30; // how many stable iters before stopping
	private Integer spinupTimesteps;
	private WaterSimulationRunner runner;
	private CostFunctions costFunction;

	public WaterBudgetCalibrationPsoFunction(int timeStepMinutes, double[] observedDischarge, int maxBasinId,
			double[] basinAreas, TopologyNode rootNode, GeoframeEnvDatabaseIterator precipReader,
			GeoframeEnvDatabaseIterator tempReader, GeoframeEnvDatabaseIterator etpReader, Integer spinupTimesteps,
			CostFunctions costFunction, boolean doParallel,
		    boolean doTopologicallyOrdered, boolean writeState, IHMProgressMonitor pm) {
		this.observedDischarge = observedDischarge;
		this.precipReader = precipReader;
		this.tempReader = tempReader;
		this.etpReader = etpReader;
		this.spinupTimesteps = spinupTimesteps;
		this.costFunction = costFunction;

		runner = new WaterSimulationRunner();
		runner.configure(timeStepMinutes, maxBasinId, rootNode, basinAreas, doParallel, writeState, doTopologicallyOrdered, null, pm);
	}

	@Override
	public double evaluateCost(int iterationStep, int particleNum, double[] params, double[]... ranges)
			throws Exception {
		var lai = 0.6; // TODO handle LAI properly

		WaterBudgetParameters wbParams = WaterBudgetParameters.fromParameterArray(params);

		String iterationInfo = String.format("PSO Iteration %d, Particle %d", iterationStep, particleNum);
		double[] simQ = runner.run(wbParams, lai, precipReader, tempReader, etpReader, iterationInfo);

		double cost = costFunction.evaluateCost(observedDischarge, simQ, spinupTimesteps, HMConstants.doubleNovalue);
		return cost;
	}

	@Override
	public String optimizationDescription() {
		return "Waterbudget PSO";
	}

	@Override
	public boolean hasConverged(double globalBest, double[] globalBestLocations, double[] previousBestLocations) {

		// First iteration: nothing to compare yet
		if (previousBestLocations == null || globalBestLocations == null) {
			previousGlobalBest = globalBest;
			stagnantIterations = 0;
			return false;
		}

		// ---------- 1) Parameter change ----------
		double maxParamDiff = 0.0;
		for (int i = 0; i < globalBestLocations.length; i++) {
			double diff = Math.abs(globalBestLocations[i] - previousBestLocations[i]);
			if (diff > maxParamDiff) {
				maxParamDiff = diff;
			}
		}

		boolean paramsStable = maxParamDiff < paramAbsTol;

		// ---------- 2) Value change (using globalBest) ----------
		double valueImprovement = (previousGlobalBest == null) ? Double.POSITIVE_INFINITY
				: Math.abs(previousGlobalBest - globalBest);

		double denom = Math.max(Math.abs(previousGlobalBest != null ? previousGlobalBest : globalBest), 1e-9);
		double relImprovement = valueImprovement / denom;

		boolean valueStable = valueImprovement < valueAbsTol || relImprovement < valueRelTol;

		// ---------- 3) Stagnation counter ----------
		if (paramsStable && valueStable) {
			stagnantIterations++;
		} else {
			stagnantIterations = 0;
		}

		// update memory for next call
		previousGlobalBest = globalBest;

		// ---------- 4) Final decision ----------
		return stagnantIterations >= maxStagnantIterations;
	}

}
