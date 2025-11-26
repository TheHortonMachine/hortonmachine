package org.hortonmachine.geoframe.utils;

import java.util.Arrays;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.geoframe.calibration.WaterBudgetParameters;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.core.WaterBudgetSimulation;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.geoframe.io.GeoframeWaterBudgetSimulationWriter;

public class WaterSimulationRunner implements IWaterBudgetSimulationRunner {

	private int timeStepMinutes;
	private int maxBasinId;
	private TopologyNode rootNode;
	private double[] basinAreas;
	private ADb outputDb;
	private IHMProgressMonitor pm;
	private boolean doTopologicallyOrdered;
	private boolean doParallel;
	private boolean writeState = false;

	@Override
	public void configure(int timeStepMinutes, int maxBasinId, TopologyNode rootNode, double[] basinAreas,
			boolean doParallel, boolean doTopologicallyOrdered, boolean writeState, ADb outputDb, IHMProgressMonitor pm) {
		this.timeStepMinutes = timeStepMinutes;
		this.maxBasinId = maxBasinId;
		this.rootNode = rootNode;
		this.basinAreas = basinAreas;
		this.doParallel = doParallel;
		this.doTopologicallyOrdered = doTopologicallyOrdered;
		this.writeState = writeState;
		this.outputDb = outputDb;
		this.pm = pm;
	}

	@Override
	public double[] run(WaterBudgetParameters wbParams, double lai, GeoframeEnvDatabaseIterator precipReader,
			GeoframeEnvDatabaseIterator tempReader, GeoframeEnvDatabaseIterator etpReader, String iterationInfo)
			throws Exception {
		TopologyNode localRootNode = rootNode.clone();
		
		if (pm == null) {
			pm = new LogProgressMonitor();
		}
		long t1 = 0;
		if (iterationInfo != null) {
			t1 = System.currentTimeMillis();
			pm.message("Begin: " + iterationInfo);
		}

		GeoframeWaterBudgetSimulationWriter resultsWriter = null;
		if (outputDb != null) {
			resultsWriter = new GeoframeWaterBudgetSimulationWriter();
			resultsWriter.db = outputDb;
			resultsWriter.rootNode = localRootNode;
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
		wbSim.rootNode = localRootNode;
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
		wbSim.wbSimParams = wbParams;
		wbSim.lai = lai;
		wbSim.resultsWriter = resultsWriter;
		wbSim.doParallel = doParallel;
		wbSim.doTopologically = doTopologicallyOrdered;
		wbSim.doDebugMessages = outputDb != null;
		wbSim.stateDb = writeState ? outputDb : null;

		wbSim.init();
		wbSim.process();

		var lastNodeDischargeArray = wbSim.outRootNodeDischargeInTime;
		var sim = lastNodeDischargeArray.getTrimmedInternalArray();

		if (iterationInfo != null) {
			long t2 = System.currentTimeMillis();
			System.out.println("End " + iterationInfo + " (" + (t2 - t1) / 1000.0 + " seconds)");
		}
		return sim;
	}

}
