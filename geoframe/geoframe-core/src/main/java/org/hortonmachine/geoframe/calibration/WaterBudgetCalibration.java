package org.hortonmachine.geoframe.calibration;

import java.util.Arrays;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.optimizers.particleswarm.IPSFunction;
import org.hortonmachine.gears.utils.optimizers.particleswarm.PSConfig;
import org.hortonmachine.gears.utils.optimizers.particleswarm.PSEngine;
import org.hortonmachine.gears.utils.optimizers.sceua.CostFunctions;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.geoframe.utils.IWaterBudgetSimulationRunner;

public class WaterBudgetCalibration {

//	public static void sceCalibration(int maxBasinId, double[] basinAreas, TopologyNode rootNode, String fromTS, String toTS,
//			int timeStepMinutes, double[] observedDischarge, int calibrationThreadCount,
//			GeoframeEnvDatabaseIterator precipReader, GeoframeEnvDatabaseIterator tempReader,
//			GeoframeEnvDatabaseIterator etpReader, IWaterBudgetSimulationRunner runner,
//			int spinUpTimesteps, AtomicInteger calibrationCounter, IHMProgressMonitor pm) throws Exception {
//		AtomicDouble bestSoFar = new AtomicDouble(Double.POSITIVE_INFINITY);
//
//		List<ParameterBounds> allParameterBounds = new ArrayList<>();
//		allParameterBounds.addAll(RainSnowSeparationParameters.calibrationParameterBounds());
//		allParameterBounds.addAll(SnowMeltingParameters.calibrationParameterBounds());
//		allParameterBounds.addAll(WaterBudgetCanopyParameters.calibrationParameterBounds());
//		allParameterBounds.addAll(WaterBudgetRootzoneParameters.calibrationParameterBounds());
//		allParameterBounds.addAll(WaterBudgetRunoffParameters.calibrationParameterBounds());
//		allParameterBounds.addAll(WaterBudgetGroundParameters.calibrationParameterBounds());
//
//		SceUaOptimizer.ObjectiveFunction objFn = params -> {
//			var lai = 0.6; // TODO handle LAI properly
//
//			int i = 0;
//			double alpha_r = params[i++];
//			double alpha_s = params[i++];
//			double meltingTemperature = params[i++];
//			var m1 = 1.0; // TODO handle m1 properly
//			RainSnowSeparationParameters rssepParamCalib = new RainSnowSeparationParameters(m1, alpha_r,
//					alpha_s, meltingTemperature);
//
//			double combinedMeltingFactor = params[i++];
//			double freezingFactor = params[i++];
//			double alfa_l = params[i++];
//			SnowMeltingParameters snowMParamsCalib = new SnowMeltingParameters(combinedMeltingFactor,
//					freezingFactor, alfa_l);
//
//			double kc = params[i++];
//			double p = params[i++];
//			WaterBudgetCanopyParameters wbCanopyParamsCalib = new WaterBudgetCanopyParameters(kc, p);
//
//			double s_RootZoneMax = params[i++];
//			double g = params[i++];
//			double h = params[i++];
//			double pB_soil = params[i++];
//			WaterBudgetRootzoneParameters wbRootzoneParamsCalib = new WaterBudgetRootzoneParameters(
//					s_RootZoneMax, g, h, pB_soil);
//
//			double sRunoffMax = params[i++];
//			double c = params[i++];
//			double d = params[i++];
//			WaterBudgetRunoffParameters wbRunoffParamsCalib = new WaterBudgetRunoffParameters(sRunoffMax, c, d);
//
//			double s_GroundWaterMax = params[i++];
//			double e = params[i++];
//			double f = params[i++];
//			WaterBudgetGroundParameters wbGroundParamsCalib = new WaterBudgetGroundParameters(s_GroundWaterMax,
//					e, f);
//			int calibRun = calibrationCounter.incrementAndGet();
//			double[] simQ = runner.run(fromTS, toTS, timeStepMinutes, maxBasinId, rootNode.clone(),
//					basinAreas, rssepParamCalib, snowMParamsCalib, wbCanopyParamsCalib, wbRootzoneParamsCalib,
//					wbRunoffParamsCalib, wbGroundParamsCalib, lai, null,
//					precipReader, tempReader, etpReader, calibRun, pm);
//			double kgeCost = - CostFunctions.kge(simQ, observedDischarge, spinUpTimesteps, HMConstants.doubleNovalue);
//			double old = bestSoFar.get();
//		    if (kgeCost < old && bestSoFar.compareAndSet(old, kgeCost)) {
//		        synchronized (pm) {
//		            pm.message("New best cost = " + kgeCost +
//		                               " | KGE = " + (-kgeCost) +
//		                               " | Params = " + Arrays.toString(params));
//		        }
//		    }
//			return kgeCost; // minimize -KGE
//		};
//		SceUaConfig config = SceUaConfig.builder().maxIterations(2000).maxEvaluations(2000).complexCount(5)
//				.objectiveStdTolerance(1e-4).random(new Random(42L)) // deterministic
//				.verbose(false).build();
//
//		SceUaOptimizer optimizer = new SceUaOptimizer(allParameterBounds, objFn, config);
//		SceUaResult result = optimizer.optimizeParallel(calibrationThreadCount);
//
//		double[] best = result.getBestParameters();
//		double bestObj = result.getBestObjective();
//
//		System.out.println("Best objective (cost) = " + bestObj);
//		System.out.println("Best params = " + Arrays.toString(best));
//
//		// Convert cost back to KGE for interpretation
//		double bestKGE = -bestObj;
//		System.out.println("Best KGE = " + bestKGE);
//	}

	public static double[] psoCalibration(PSConfig psConfig, int maxBasinId, double[] basinAreas, TopologyNode rootNode, int timeStepMinutes,
			double[] observedDischarge, CostFunctions costFunction, int calibrationThreadCount,
			GeoframeEnvDatabaseIterator precipReader, GeoframeEnvDatabaseIterator tempReader,
			GeoframeEnvDatabaseIterator etpReader, IWaterBudgetSimulationRunner runner, int spinUpTimesteps,
			IHMProgressMonitor pm)
			throws Exception {

		double[][] ranges = new double[][] { WaterBudgetParameters.RainSnowSeparation.alphaRRange(),
				WaterBudgetParameters.RainSnowSeparation.alphaSRange(),
				WaterBudgetParameters.RainSnowSeparation.meltingTemperatureRange(),
				WaterBudgetParameters.SnowMeltingParameters.combinedMeltingFactorRange(),
				WaterBudgetParameters.SnowMeltingParameters.freezingFactorRange(),
				WaterBudgetParameters.SnowMeltingParameters.alfaLRange(),
				WaterBudgetParameters.WaterBudgetCanopyParameters.kcRange(),
				WaterBudgetParameters.WaterBudgetCanopyParameters.pRange(),
				WaterBudgetParameters.WaterBudgetRootzoneParameters.sRootZoneMaxRange(),
				WaterBudgetParameters.WaterBudgetRootzoneParameters.gRange(),
				WaterBudgetParameters.WaterBudgetRootzoneParameters.hRange(),
				WaterBudgetParameters.WaterBudgetRootzoneParameters.pBSoilRange(),
				WaterBudgetParameters.WaterBudgetRunoffParameters.sRunoffMaxRange(),
				WaterBudgetParameters.WaterBudgetRunoffParameters.cRange(),
				WaterBudgetParameters.WaterBudgetRunoffParameters.dRange(),
				WaterBudgetParameters.WaterBudgetGroundParameters.sGroundWaterMaxRange(),
				WaterBudgetParameters.WaterBudgetGroundParameters.eRange(),
				WaterBudgetParameters.WaterBudgetGroundParameters.fRange() };

		IPSFunction wbFunction = new WaterBudgetCalibrationPsoFunction(timeStepMinutes, observedDischarge, maxBasinId,
				basinAreas, rootNode, precipReader, tempReader, etpReader, spinUpTimesteps, costFunction, false,
				true, pm);

		PSEngine engine = new PSEngine(psConfig.particlesNum, psConfig.maxIterations, psConfig.c1, psConfig.c2, psConfig.w0, psConfig.decay, wbFunction,
				calibrationThreadCount, "PSO-Waterbudget");
		engine.initializeRanges(ranges);

		// 4. Run the swarm
		engine.run();

		// 5. Extract results
		double[] best = engine.getSolution();
		double cost = engine.getSolutionFittingValue();

		pm.message("PSO calibration completed.");
		pm.message("Best cost = " + cost);
		pm.message("Best params = " + Arrays.toString(best));
		
		return best;
	}

}
