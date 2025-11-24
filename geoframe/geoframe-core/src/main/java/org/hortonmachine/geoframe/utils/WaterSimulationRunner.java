package org.hortonmachine.geoframe.utils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.core.WaterBudgetSimulation;
import org.hortonmachine.geoframe.core.parameters.RainSnowSeparationParameters;
import org.hortonmachine.geoframe.core.parameters.SnowMeltingParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetCanopyParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetGroundParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRootzoneParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRunoffParameters;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.geoframe.io.GeoframeWaterBudgetSimulationWriter;

public class WaterSimulationRunner implements IWaterBudgetSimulationRunner {

	@Override
	public double[] run(String fromTS, String toTS, 
			int timeStepMinutes, int maxBasinId, TopologyNode rootNode, double[] basinAreas, 
			RainSnowSeparationParameters rssepParam, SnowMeltingParameters snowMParams,
			WaterBudgetCanopyParameters wbCanopyParams, WaterBudgetRootzoneParameters wbRootzoneParams,
			WaterBudgetRunoffParameters wbRunoffParams, WaterBudgetGroundParameters wbGroundParams, 
			double lai,
			ADb outputDb, 
			GeoframeEnvDatabaseIterator precipReader, GeoframeEnvDatabaseIterator tempReader,
			GeoframeEnvDatabaseIterator etpReader, 
			Integer calibRun, IHMProgressMonitor pm)
			throws Exception {

		long t1 = 0;
		if (calibRun != null) {
			t1 = System.currentTimeMillis();
			pm.message("Running simulation " + calibRun);
		}

		GeoframeWaterBudgetSimulationWriter resultsWriter = null;
		if (outputDb != null) {
			resultsWriter = new GeoframeWaterBudgetSimulationWriter();
			resultsWriter.db = outputDb;
			resultsWriter.rootNode = rootNode;
		}

		double[] initialConditionSolidWater = new double[maxBasinId + 1]; // ok init with 0s
		double[] initialConditionLiquidWater = new double[maxBasinId + 1]; // ok init with 0s
		double[] initalConditionsCanopyMap = new double[maxBasinId + 1];
		Arrays.fill(initalConditionsCanopyMap, Double.NaN);
		double[] initalConditionsRootzoneMap = new double[maxBasinId + 1];
		Arrays.fill(initalConditionsRootzoneMap, Double.NaN);
		double[] initalConditionsRunoffMap = new double[maxBasinId + 1];
		Arrays.fill(initalConditionsRunoffMap, Double.NaN);
		double[] initalConditionsGroundMap = new double[maxBasinId + 1];
		Arrays.fill(initalConditionsGroundMap, Double.NaN);

		WaterBudgetSimulation wbSim = new WaterBudgetSimulation();
		wbSim.pm = pm;
		wbSim.rootNode = rootNode;
		wbSim.basinAreas = basinAreas;
		wbSim.timeStepMinutes = timeStepMinutes;
		wbSim.precipReader = precipReader;
		wbSim.tempReader = tempReader;
		wbSim.etpReader = etpReader;
		wbSim.initialConditionSolidWater = initialConditionSolidWater;
		wbSim.initialConditionLiquidWater = initialConditionLiquidWater;
		wbSim.initalConditionsCanopyMap = initalConditionsCanopyMap;
		wbSim.initalConditionsRootzoneMap = initalConditionsRootzoneMap;
		wbSim.initalConditionsRunoffMap = initalConditionsRunoffMap;
		wbSim.initalConditionsGroundMap = initalConditionsGroundMap;
		wbSim.rssepParam = rssepParam;
		wbSim.snowMParams = snowMParams;
		wbSim.wbCanopyParams = wbCanopyParams;
		wbSim.wbRootzoneParams = wbRootzoneParams;
		wbSim.wbRunoffParams = wbRunoffParams;
		wbSim.wbGroundParams = wbGroundParams;
		wbSim.lai = lai;
		wbSim.resultsWriter = resultsWriter;
		wbSim.doParallel = false;
		wbSim.doTopologically = false;
		wbSim.doDebugMessages = outputDb != null;

		wbSim.init();
		wbSim.process();

		var lastNodeDischargeArray = wbSim.outRootNodeDischargeInTime;
		var sim = lastNodeDischargeArray.getTrimmedInternalArray();

		if (calibRun != null) {
			long t2 = System.currentTimeMillis();
			System.out.println("Simulation run " + calibRun + " completed in " + (t2 - t1) / 1000.0 + " seconds.");
		}
		return sim;
	}

}
