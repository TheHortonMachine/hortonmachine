package org.hortonmachine.geoframe.calibration;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.DynamicDoubleArray;
import org.hortonmachine.gears.utils.optimizers.particleswarm.IPSFunction;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.core.parameters.RainSnowSeparationParameters;
import org.hortonmachine.geoframe.core.parameters.SnowMeltingParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetCanopyParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetGroundParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRootzoneParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRunoffParameters;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.geoframe.utils.WaterSimulationRunner;

import oms3.util.Statistics;

public class WaterBudgetCalibrationPso implements IPSFunction {

	private String fromTS;
	private String toTS;
	private int timeStepMinutes;
	private double[] observedDischarge;
	private int maxBasinId;
	private double[] basinAreas;
	private TopologyNode rootNode;
	private GeoframeEnvDatabaseIterator precipReader;
	private GeoframeEnvDatabaseIterator tempReader;
	private GeoframeEnvDatabaseIterator etpReader;
	private IHMProgressMonitor pm;
	
    // --- convergence state ---
    private Double previousGlobalBest = null;
    private int stagnantIterations = 0;

    // tune these to taste
    private final double valueAbsTol = 1e-4;    // absolute change in cost
    private final double valueRelTol = 1e-3;    // relative change in cost
    private final double paramAbsTol = 1e-3;    // max change in any parameter
    private final int maxStagnantIterations = 100; // how many stable iters before stopping
	private Integer spinupTimesteps;


	public WaterBudgetCalibrationPso(String fromTS, String toTS, int timeStepMinutes, double[] observedDischarge,
			int maxBasinId, double[] basinAreas, TopologyNode rootNode, GeoframeEnvDatabaseIterator precipReader,
			GeoframeEnvDatabaseIterator tempReader, GeoframeEnvDatabaseIterator etpReader, Integer spinupTimesteps, IHMProgressMonitor pm) {
		this.fromTS = fromTS;
		this.toTS = toTS;
		this.timeStepMinutes = timeStepMinutes;
		this.observedDischarge = observedDischarge;
		this.maxBasinId = maxBasinId;
		this.basinAreas = basinAreas;
		this.rootNode = rootNode;
		this.precipReader = precipReader;
		this.tempReader = tempReader;
		this.etpReader = etpReader;
		this.spinupTimesteps = spinupTimesteps;
		this.pm = pm;
	}

	@Override
	public double evaluate(int iterationStep, int particleNum, double[] params, double[]... ranges) throws Exception {
		var lai = 0.6; // TODO handle LAI properly

		int i = 0;
		double alpha_r = params[i++];
		double alpha_s = params[i++];
		double meltingTemperature = params[i++];
		var m1 = 1.0; // TODO handle m1 properly
		RainSnowSeparationParameters rssepParamCalib = new RainSnowSeparationParameters(m1, alpha_r, alpha_s,
				meltingTemperature);

		double combinedMeltingFactor = params[i++];
		double freezingFactor = params[i++];
		double alfa_l = params[i++];
		SnowMeltingParameters snowMParamsCalib = new SnowMeltingParameters(combinedMeltingFactor, freezingFactor,
				alfa_l);

		double kc = params[i++];
		double p = params[i++];
		WaterBudgetCanopyParameters wbCanopyParamsCalib = new WaterBudgetCanopyParameters(kc, p);

		double s_RootZoneMax = params[i++];
		double g = params[i++];
		double h = params[i++];
		double pB_soil = params[i++];
		WaterBudgetRootzoneParameters wbRootzoneParamsCalib = new WaterBudgetRootzoneParameters(s_RootZoneMax, g, h,
				pB_soil);

		double sRunoffMax = params[i++];
		double c = params[i++];
		double d = params[i++];
		WaterBudgetRunoffParameters wbRunoffParamsCalib = new WaterBudgetRunoffParameters(sRunoffMax, c, d);

		double s_GroundWaterMax = params[i++];
		double e = params[i++];
		double f = params[i++];
		WaterBudgetGroundParameters wbGroundParamsCalib = new WaterBudgetGroundParameters(s_GroundWaterMax, e, f);

		WaterSimulationRunner runner = new WaterSimulationRunner();
		double[] simQ = runner.run(fromTS, toTS, timeStepMinutes, maxBasinId, rootNode.clone(), basinAreas,
				rssepParamCalib, snowMParamsCalib, wbCanopyParamsCalib, wbRootzoneParamsCalib, wbRunoffParamsCalib,
				wbGroundParamsCalib, lai, null, precipReader, tempReader, etpReader, iterationStep, pm);

        // --- Compute KGE ---
        double kge = kge(observedDischarge, simQ, spinupTimesteps, HMConstants.doubleNovalue);
        double cost = -kge;  // <-- PSO minimizes!
        return cost;
	}
	
    /**
     * Kling and Gupta Efficiency.
     *
     * @param obs the observed data set
     * @param sim the simulated data set
     * @param missingValue the missing value
     * @return the kge
     */
    public static double kge(double[] obs, double[] sim, Integer spinupTimesteps, double noDataValue) {
        sameArrayLen(obs, sim);
        
        int startIndex = 0;
        if (spinupTimesteps != null && spinupTimesteps > 0) {
			startIndex = spinupTimesteps;
		}
        DynamicDoubleArray simFilteredTemp = new DynamicDoubleArray(obs.length);
        DynamicDoubleArray obsFilteredTemp = new DynamicDoubleArray(obs.length);
        for (int i = startIndex; i < obs.length; i++) {
        	if(!HMConstants.isNovalue(obs[i], noDataValue)) {
        		simFilteredTemp.addValue(sim[i]);
				obsFilteredTemp.addValue(obs[i]);
        	}
        }
        sim = simFilteredTemp.getTrimmedInternalArray();
        obs = obsFilteredTemp.getTrimmedInternalArray();
        
        
        int contamedia = 0;
        double sommamediaoss = 0;
        double sommamediasim = 0;
        for (int i = 0; i < obs.length; i++) {
            contamedia++;
            sommamediaoss += obs[i];
            sommamediasim += sim[i];
        }
        double mediaoss = sommamediaoss / contamedia;
        double mediasim = sommamediasim / contamedia;
        int count = 0;
        double numvaprev = 0;
        double coef1_den = 0;
        double numR = 0;
        double den1R = 0;
        double den2R = 0;
		for (int i = 0; i < obs.length; i++) {
			count++;
			coef1_den += (obs[i] - mediaoss) * (obs[i] - mediaoss);
			numR += (obs[i] - mediaoss) * (sim[i] - mediasim);
			den1R += (obs[i] - mediaoss) * (obs[i] - mediaoss);
			den2R += (sim[i] - mediasim) * (sim[i] - mediasim);
			numvaprev += (sim[i] - mediasim) * (sim[i] - mediasim);
		}
        double sdosservati = Math.sqrt(coef1_den / (count - 1));
        double sdsimulati = Math.sqrt(numvaprev / (count - 1));
        double R = numR / (Math.sqrt(den1R) * Math.sqrt(den2R));
        double alpha = sdsimulati / sdosservati;
        double beta = mediasim / mediaoss;
        return 1 - Math.sqrt((R - 1) * (R - 1) + (alpha - 1) * (alpha - 1) + (beta - 1) * (beta - 1));
    }
    
    /**
     * Check if the arrays have the same length
     *
     * @param arr a list of arrays
     */
    private static void sameArrayLen(double[]... arr) {
        int len = arr[0].length;
        for (double[] a : arr) {
            if (a.length != len) {
                throw new IllegalArgumentException("obs and sim data have not same size (" + a.length + "/" + len + ")");
            }
        }
    }

	@Override
	public double optimization(double... parameters) {
        try {
            return evaluate(0, 0, parameters, (double[][]) null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	}

	@Override
	public String optimizationDescription() {
		return "Waterbudget PSO";
	}

	@Override
	public boolean isBetter(double evaluatedValue, double consideredBest) {
		return evaluatedValue < consideredBest; // minimization
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

	@Override
	public double getInitialGlobalBest() {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public String getPostInfoString() {
		// TODO Auto-generated method stub
		return null;
	}

}
