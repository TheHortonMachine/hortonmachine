package org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation;

import org.hortonmachine.gears.utils.math.regressions.PolyTrendLine;
import org.hortonmachine.gears.utils.math.regressions.RegressionLine;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

public class ResidualsEvaluator {
	@Description("Switch for detrended mode.")
	@In
	public boolean doDetrended;

	@Description("The order of regression polynomial object.")
	@In
	public int regressionOrder = 1;

	@In
	public double[] zStations;
	@In
	public double[] hStations;

	@Out
	public double trendIntercept;

	@Out
	public double trendCoefficient;

	@In
	@Out
	public double[] hResiduals;

	@Out
	public boolean isPValueOk = false;

	@Execute
	public void process() {
		hResiduals = hStations;
		trendIntercept = 0;
		trendCoefficient = 0;
		isPValueOk = false;

		try {
			if (doDetrended && zStations.length > 2) {

                RegressionLine t = new PolyTrendLine(regressionOrder);
                t.setValues(zStations, hStations);

                double[] regressionParameters = t.getRegressionParameters();
                trendIntercept = regressionParameters[0];
                trendCoefficient = regressionParameters[1];
                hResiduals = t.getResiduals();
                
                // TODO implement the check using p-values.  
//                if (t.getPValue() > 0.05) {
//					isPValueOk = true;
//				} else {
//					trendIntercept = 0;
//					trendCoefficient = 0;
//					hResiduals = hStations;
//				}
				
                
                // ORIGINAL CODE BASED ON FLANAGAN
//				Regression r = new Regression(zStations, hStations);
//				r.polynomial(regressionOrder);
//				/*
//				 * If there is a trend for meteorological variables and elevation and it is
//				 * statistically significant then the residuals from this linear trend are
//				 * computed for each meteorological stations.
//				 */
//				if (r.getPvalues()[1] < 0.05) {
//					isPValueOk = true;
//					trendIntercept = r.getBestEstimates()[0];
//					trendCoefficient = r.getBestEstimates()[1];
//					hResiduals = r.getResiduals();
//				} else {
//					// it's set to true at each time step
//					// set to 0 so the trend (line 330) is 0
//					trendIntercept = 0;
//					trendCoefficient = 0;
//					hResiduals = hStations;
//				}
			}
		} catch (Exception e) {
			//TODO: maybe meessange handler
		    System.err.println("Error in ResidualsEvaluator: " + e.getMessage());
			isPValueOk = false;
			hResiduals = hStations;
			trendIntercept = 0;
			trendCoefficient = 0;
		}

	}
/**
 * @todo
 *
 * @param zStations
 * @param hStations
 * @param doDetrended
 * @param regressionOrder
 * @return
 */
	public final static ResidualsEvaluator create(double[] zStations, double[] hStations,
			boolean doDetrended, int regressionOrder) {
		ResidualsEvaluator residualsEvaluator = new ResidualsEvaluator();
		residualsEvaluator.doDetrended = doDetrended;
		residualsEvaluator.hStations = hStations;
		residualsEvaluator.zStations = zStations;
		residualsEvaluator.regressionOrder = regressionOrder;
		residualsEvaluator.process();
		return residualsEvaluator;
	}

}
