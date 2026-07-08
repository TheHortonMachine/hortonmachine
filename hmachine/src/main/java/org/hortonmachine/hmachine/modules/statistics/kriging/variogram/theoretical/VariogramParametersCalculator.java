package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical;

import java.util.Arrays;
import java.util.HashMap;

import org.hortonmachine.hmachine.modules.statistics.kriging.Kriging;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.ResidualsEvaluator;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationsSelection;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.experimental.ExperimentalVariogram;

/**
 * Computes (local) variogram parameters from a subset of stations, optionally
 * detrending residuals before building the experimental variogram and fitting a
 * theoretical model.
 *
 * Fallback logic: - If the local fit is valid -> return local parameters -
 * Otherwise, try returning global parameters (detrended or not) if
 * available/valid.
 *
 * Assumptions: - stations.execute() populates the arrays used here
 * (hStationInitialSet, zStationInitialSet, etc.). - globalVP/globalDeTrendedVP
 * must be set by the caller before execute(), otherwise they can be null.
 * 
 * @author Daniele Andreis and Giuseppe Formetta.
 * 
 */
public class VariogramParametersCalculator {
	private VariogramParameters globalVP;
	private VariogramParameters globalDeTrendedVP;
	private StationsSelection stations = null;
	private boolean doDetrend = false;
	private String semivariogramType = null;
	private int cutoffDivide;
	private double cutoffInput;
	private HashMap<Integer, double[]> dd;
	private HashMap<Integer, double[]> nn;
	private HashMap<Integer, double[]> hh;

	public VariogramParametersCalculator(StationsSelection stations, boolean doDetrend, Boolean doIncludeZero) {
		this.stations = stations;
		this.doDetrend = doDetrend;
		try {
			int tempN = 0;
			if (stations.inNumCloserStations != 0) {
				tempN = stations.inNumCloserStations;
				stations.inNumCloserStations = 0;
			}
			this.stations.execute();
			stations.inNumCloserStations = tempN;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Main entry point.
	 *
	 * Workflow: 1) Guard checks: require enough stations and non-degenerate data.
	 * 2) Compute residuals (optionally detrended) using a regression model. 3)
	 * Build experimental variogram from residuals. 4) Fit theoretical semivariogram
	 * model (VariogramParamsEvaluator). 5) If fit is good -> return local
	 * parameters, else return global fallback if available.
	 */
	public VariogramParameters execute() {
		if (!stations.areAllEquals && stations.n1 > 2) {
			int n1 = stations.hStationInitialSet.length - 1;
			ResidualsEvaluator rEvaluator = new ResidualsEvaluator();
			rEvaluator.doDetrended = doDetrend;
			rEvaluator.hStations = Arrays.copyOfRange(stations.hStationInitialSet, 0, n1);
			rEvaluator.zStations = Arrays.copyOfRange(stations.zStationInitialSet, 0, n1);
			rEvaluator.regressionOrder = Kriging.REGRESSION_ORDER;
			rEvaluator.process();
			int[] idStations = Arrays.copyOfRange(stations.idStationInitialSet, 0, n1);
			double[] hResiduals = rEvaluator.hResiduals;
			hResiduals = rEvaluator.hResiduals;
			SemivariogramParameterFitter semivariogramFitter = new SemivariogramParameterFitter();
			semivariogramFitter.expVar = getExperimentalVariogram(hResiduals, idStations);
			semivariogramFitter.pSemivariogramType = this.semivariogramType;
			semivariogramFitter.proces();

			dd = semivariogramFitter.expVar.outDistances;
			hh = semivariogramFitter.expVar.outDistances;
			nn = semivariogramFitter.expVar.outNumberPairsPerBin;

			boolean variogramOk = semivariogramFitter.nugget >= 0 && semivariogramFitter.sill > 0
					&& semivariogramFitter.range > 0 && semivariogramFitter.isFitGood;

			/**
			 * Selection logic: - If local fit ok -> return local - Else if no global valid
			 * OR (detrend enabled AND global detrended valid) -> return local anyway (!)
			 *
			 */
			if (variogramOk || (!globalVP.isValid()) || (doDetrend && !globalDeTrendedVP.isValid())) {
				VariogramParameters myVariogramParam = new VariogramParameters.Builder(
						semivariogramFitter.outSemivariogramType, semivariogramFitter.nugget, semivariogramFitter.range,
						semivariogramFitter.sill).setLocal(true).setTrend(rEvaluator.isPValueOk)
						.setTrendIntercept(rEvaluator.trendIntercept).setTrendSlope(rEvaluator.trendCoefficient)
						.build();

				return myVariogramParam;
			} else if (doDetrend && globalDeTrendedVP.isValid()) {
				return globalDeTrendedVP;
			} else if (globalVP.isValid()) {
				return globalVP;
			}
		}
		if (doDetrend && globalDeTrendedVP.isValid()) {
			return globalDeTrendedVP;
		} else if (globalVP.isValid()) {
			return globalVP;
		}
		return null;
	}

	public HashMap<Integer, double[]> getHH() {
		return hh;
	}

	public HashMap<Integer, double[]> getDD() {
		return dd;
	}

	public HashMap<Integer, double[]> getNN() {
		return nn;
	}

	private ExperimentalVariogram getExperimentalVariogram(double[] hresiduals, int[] idArray) {
		ExperimentalVariogram expVariogram = ExperimentalVariogram.create(stations.fStationsid, stations.inStations,
				stations.doIncludezero, cutoffDivide, cutoffInput, 0);
		HashMap<Integer, double[]> tmpInData = new HashMap<>();
		for (int i = 0; i < idArray.length; i++) {
			tmpInData.put(idArray[i], new double[] { hresiduals[i] });
		}
		expVariogram.inData = tmpInData;
		return expVariogram;
	}

	public void setGlobalDeTrendedVp(VariogramParameters vpGlobalDetrended) {
		// TODO Auto-generated method stub
		if (vpGlobalDetrended != null) {
			this.globalDeTrendedVP = vpGlobalDetrended;
		}
	}

	public void setGlobalVp(VariogramParameters vpGlobal) {
		// TODO Auto-generated method stub
		if (vpGlobal != null) {
			this.globalVP = vpGlobal;
		}
	}

	public void setType(String semivariogramType) {
		// TODO Auto-generated method stub
		if (semivariogramType != null) {
			this.semivariogramType = semivariogramType;
		}
	}

	public void setCutOffdivide(int cutoffDivide) {
		// TODO Auto-generated method stub
		if (cutoffDivide > 0) {
			this.cutoffDivide = cutoffDivide;
		}
	}

	public void setCutOffInput(double cutoffInput) {
		// TODO Auto-generated method stub
		if (cutoffInput > 0) {
			this.cutoffInput = cutoffInput;
		}
	}

}
