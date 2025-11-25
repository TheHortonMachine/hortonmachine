package org.hortonmachine.gears.utils.optimizers.sceua;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.DynamicDoubleArray;

public final class CostFunctions {

    /**
	 * Kling-Gupta Efficiency (KGE) metric and associated cost function.
	 *
	 * Gupta et al. (2009):
	 * KGE = 1 - sqrt( (r - 1)^2 + (alpha - 1)^2 + (beta - 1)^2 )
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
        int obsLength = obs.length;
        DynamicDoubleArray simFilteredTemp = new DynamicDoubleArray(obsLength);
        DynamicDoubleArray obsFilteredTemp = new DynamicDoubleArray(obsLength);
		for (int i = startIndex; i < obsLength; i++) {
        	if(HMConstants.isNovalue(obs[i], noDataValue) || obs[i] < 0) {
        		continue;
        	}
        	simFilteredTemp.addValue(sim[i]);
        	obsFilteredTemp.addValue(obs[i]);
        }
        sim = simFilteredTemp.getTrimmedInternalArray();
        obs = obsFilteredTemp.getTrimmedInternalArray();
        
        int newObsLength = obs.length;
        double sommamediaoss = 0;
        double sommamediasim = 0;
        for (int i = 0; i < newObsLength; i++) {
            sommamediaoss += obs[i];
            sommamediasim += sim[i];
        }
        double mediaoss = sommamediaoss / newObsLength;
        double mediasim = sommamediasim / newObsLength;
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
}
