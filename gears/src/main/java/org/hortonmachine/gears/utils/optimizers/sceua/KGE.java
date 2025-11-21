package org.hortonmachine.gears.utils.optimizers.sceua;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.DynamicDoubleArray;

/**
 * Kling-Gupta Efficiency (KGE) metric and associated cost function.
 *
 * Gupta et al. (2009):
 * KGE = 1 - sqrt( (r - 1)^2 + (alpha - 1)^2 + (beta - 1)^2 )
 */
public final class KGE {

    private KGE() {
    }

    /**
     * Computes KGE(sim, obs).
     *
     * @param sim simulated time series
     * @param obs observed time series
     * @return KGE value (1 is perfect, can be negative)
     */
    public static double kge(double[] sim, double[] obs, Integer spinupTimesteps, double noDataValue) {
        if (sim.length != obs.length) {
            throw new IllegalArgumentException("sim and obs must have same length.");
        }
        int n = sim.length;
        if (n == 0) {
            throw new IllegalArgumentException("Empty time series.");
        }
        
        int startIndex = 0;
        if (spinupTimesteps != null && spinupTimesteps > 0) {
			startIndex = spinupTimesteps;
		}
        DynamicDoubleArray simFilteredTemp = new DynamicDoubleArray(n);
        DynamicDoubleArray obsFilteredTemp = new DynamicDoubleArray(n);
        for (int i = startIndex; i < n; i++) {
        	if(!HMConstants.isNovalue(obs[i], noDataValue)) {
        		simFilteredTemp.addValue(sim[i]);
				obsFilteredTemp.addValue(obs[i]);
        	}
        }
        sim = simFilteredTemp.getTrimmedInternalArray();
        obs = obsFilteredTemp.getTrimmedInternalArray();
        n = sim.length;
        if (n == 0) {
			throw new IllegalArgumentException("No overlapping valid data in time series.");
		}
        
        double meanSim = 0.0;
        double meanObs = 0.0;
        for (int i = 0; i < n; i++) {
            meanSim += sim[i];
            meanObs += obs[i];
        }
        meanSim /= n;
        meanObs /= n;

        double varSim = 0.0;
        double varObs = 0.0;
        double cov = 0.0;
        for (int i = 0; i < n; i++) {
            double ds = sim[i] - meanSim;
            double dobs = obs[i] - meanObs;
            varSim += ds * ds;
            varObs += dobs * dobs;
            cov += ds * dobs;
        }
        varSim /= n;
        varObs /= n;
        cov    /= n;

        if (varSim == 0 || varObs == 0) {
            return Double.NEGATIVE_INFINITY;
        }

        double sdSim = Math.sqrt(varSim);
        double sdObs = Math.sqrt(varObs);

        double r = cov / (sdSim * sdObs);
        double alpha = sdSim / sdObs;
        double beta  = meanSim / meanObs;

        double dr = r - 1.0;
        double da = alpha - 1.0;
        double db = beta - 1.0;

        return 1.0 - Math.sqrt(dr * dr + da * da + db * db);
    }

    /**
     * Cost to MINIMIZE for calibration (higher KGE is better, so cost = -KGE).
     */
    public static double kgeCost(double[] sim, double[] obs, Integer spinupTimesteps, double noDataValue) {
        return -kge(sim, obs, spinupTimesteps, noDataValue);
    }
}
