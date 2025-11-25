package org.hortonmachine.geoframe.core;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.DynamicDoubleArray;
import org.hortonmachine.geoframe.calibration.WaterBudgetParameters;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;
import org.hortonmachine.geoframe.io.GeoframeWaterBudgetSimulationWriter;
import org.hortonmachine.geoframe.utils.WaterBudgetState;

import canopyOut.WaterBudgetCanopyOUT;
import canopyOut.WaterBudgetCanopyOUT.WaterBudgetCanopyStepResult;
import groundWater.WaterBudgetGround;
import groundWater.WaterBudgetGround.WaterBudgetGroundStepResult;
import it.geoframe.blogspot.snowmelting.pointcase.SnowMeltingPointCaseDegreeDay;
import it.geoframe.blogspot.snowmelting.pointcase.SnowMeltingPointCaseDegreeDay.SnowStepResult;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Out;
import rainSnowSperataion.RainSnowSeparationPointCase;
import rootZone.WaterBudgetRootZone;
import rootZone.WaterBudgetRootZone.WaterBudgetRootZoneStepResult;
import simpleBucket.WaterBudget;
import simpleBucket.WaterBudget.WaterBudgetStepResult;

public class WaterBudgetSimulation extends HMModel {
	@Description("Root node of the topology. after each simulation it contains the discharges of all nodes.")
	@In
	public TopologyNode rootNode;
	
	@Description("Map of basin areas in km2")
	@In
	public double[] basinAreas;
	
	@Description("Time step in minutes")
	@In
	public int timeStepMinutes;
	
	@Description("Precipitation reader")
	@In
	public GeoframeEnvDatabaseIterator precipReader;
	
	@Description("Temperature reader")
	@In
	public GeoframeEnvDatabaseIterator tempReader;
	
	@Description("ETP reader")
	@In
	public GeoframeEnvDatabaseIterator etpReader;
	
	@Description("Initial condition solid water map")
	@In
	public double[] initialConditionSolidWater;
	
	@Description("Initial condition liquid water map")
	@In
	public double[] initialConditionLiquidWater;
	
	@Description("Initial conditions canopy map")
	@In
	public double[] initalConditionsCanopyMap;
	
	@Description("Initial conditions rootzone map")
	@In
	public double[] initalConditionsRootzoneMap;
	
	@Description("Initial conditions runoff map")
	@In
	public double[] initalConditionsRunoffMap;
	
	@Description("Initial conditions groundwater map")
	@In
	public double[] initalConditionsGroundMap;
	
	@Description("Water budget simulation parameters")
	@In
	public WaterBudgetParameters wbSimParams;
	
	@Description("Leaf area index")
	@In
	public double lai;
	
	@Description("Results writer")
	@In
	public GeoframeWaterBudgetSimulationWriter resultsWriter;
	
	@Description("Dynamic array to store the most downstream node discharge over time")
	@Out
	public DynamicDoubleArray outRootNodeDischargeInTime = new DynamicDoubleArray(10000, 10000);
	
	/**
	 * Optional state database to store results.
	 * 
	 * State database schema is created automatically if not existing.
	 * State is dumped after each time step only if this variable is not null.
	 */
	public ADb stateDb = null;

	/**
	 * Whether to do the parallel processing
	 */
	public boolean doParallel = true;
	/**
	 * If parallel processing is chosen, whether to do it topologically (at nodes wait for upstream nodes to finish)
	 */
	public boolean doTopologically = true;
	
	public int threadPoolSize = Runtime.getRuntime().availableProcessors();
	
	private TopologyNode[] basinid2nodeMap = null;
	
	private WaterBudgetState[] waterBudgetStates = null; 
	
	public boolean doDebugMessages = true;

	private String previousDay = null;
	
	private int timestepIndex = 0;

	private String stateTableName;

	@Initialize
	public void init() throws Exception {
		if (basinid2nodeMap == null) {
			// create a map of basinId -> TopologyNode
			basinid2nodeMap = new TopologyNode[basinAreas.length];
			rootNode.visitUpstream(node -> {
				basinid2nodeMap[node.basinId] = node;
			});
			
			waterBudgetStates = new WaterBudgetState[basinAreas.length];
			rootNode.visitUpstream(node -> {
				waterBudgetStates[node.basinId] = new WaterBudgetState();
			});
			if (stateDb != null) {
				stateTableName = WaterBudgetState.initTable(stateDb);
			}
		}
	}
	
	@Execute	
	public void process() throws Exception {
		if (precipReader.isPreCachingMode()) {
			double[] precipData = precipReader.getCached(timestepIndex);
			double[] tempData = tempReader.getCached(timestepIndex);
			double[] etpData = etpReader.getCached(timestepIndex);
			while (precipData != null && tempData != null && etpData != null) {
				processTimestep(precipData, tempData, etpData);
				timestepIndex++;
				precipData = precipReader.getCached(timestepIndex);
				tempData = tempReader.getCached(timestepIndex);
				etpData = etpReader.getCached(timestepIndex);
			}
		} else {
			while (precipReader.next() && tempReader.next() && etpReader.next()) {

				double[] precipMap = precipReader.outData;
				double[] tempMap = tempReader.outData;
				double[] etpMap = etpReader.outData;

				processTimestep(precipMap, tempMap, etpMap);
			}
		}
		
	}

	private void processTimestep(double[] precipMap, double[] tempMap, double[] etpMap) throws Exception {
		String pTs = precipReader.tCurrent;
		String tTs = tempReader.tCurrent;
		String eTs = etpReader.tCurrent;
		// check that time steps are the same
		if (!pTs.equals(tTs) || !pTs.equals(eTs)) {
			throw new Exception("Time steps do not match: " + pTs + " <> " + tTs + " <> " + eTs);
		}

		// reset nodes values
		rootNode.visitUpstream(node -> {
			node.value = Double.NaN;
			node.accumulatedValue = Double.NaN;
		});
		
		if (doParallel) {
			if (doTopologically) {
				rootNode.visitDownstreamFromLeavesParallel(threadPoolSize, node -> {
					processTimestepForBasin(precipMap, tempMap, etpMap, node);
				});
			} else {
				Set<TopologyNode> nodes = new HashSet<>();
				TopologyNode.collectAllUpstreamRecursive(rootNode, nodes);
				
				// do parallel processing with threadPoolSize threads
				ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
				CountDownLatch latch = new CountDownLatch(nodes.size());
				for (TopologyNode node : nodes) {
					executor.submit(() -> {
						try {
							processTimestepForBasin(precipMap, tempMap, etpMap, node);
						} finally {
							latch.countDown();
						}
					});
				}
				try {
		            latch.await();
		        } catch (InterruptedException e) {
		            Thread.currentThread().interrupt();
		            throw new RuntimeException("Interrupted while waiting for basin parallel operation to finish", e);
		        } finally {
		            executor.shutdown();
		        }
			}
		} else {
			if (doTopologically) {
				rootNode.visitDownstreamFromLeaves(node -> {
					processTimestepForBasin(precipMap, tempMap, etpMap, node);
				});
			} else {
				Set<TopologyNode> nodes = new HashSet<>();
				TopologyNode.collectAllUpstreamRecursive(rootNode, nodes);
				nodes.stream().forEach(node -> {
					processTimestepForBasin(precipMap, tempMap, etpMap, node);
				});
			}
		}
		
		// accumulate the discharges downstream
		TopologyNode.accumulateDownstream(rootNode);
		
		// TODO do something with the resulting discharges
		if (doDebugMessages) {
			TopologyNode dbNode = rootNode;// TopologyNode.findNodeByBasinId(rootNode, 126);
			String yearDay = pTs.substring(0, 10);
			if (previousDay == null || !yearDay.equals(previousDay)) {
				pm.message("Node " + dbNode.basinId + "   " + pTs + "; " + dbNode.accumulatedValue + "; " + dbNode.value);
				previousDay = yearDay;
			}
		}
		
		if (resultsWriter != null) {
			resultsWriter.currentT = precipReader.currentT;
			resultsWriter.insert();
			
			if (stateDb != null) {
				// store state in database
				List<Object[]> insertObjectsList = new ArrayList<>();
				rootNode.visitUpstream(node -> {
					try {
						int basinId = node.basinId;
						WaterBudgetState waterBudgetState = waterBudgetStates[basinId];
						Object[] insertIntoDbObjects = waterBudgetState.getInsertIntoDbObjects(basinId, precipReader.currentT);
						insertObjectsList.add(insertIntoDbObjects);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				});
				String preparedInsertSql = WaterBudgetState.getPreparedInsertSql(stateTableName);
				stateDb.executeBatchPreparedSql(preparedInsertSql, insertObjectsList);
			}
		}
		
		// store outlet discharge in dynamic array
		outRootNodeDischargeInTime.addValue(rootNode.accumulatedValue);
	}

	private void processTimestepForBasin(double[] precipMap, double[] tempMap, double[] etpMap, TopologyNode node) {
		int basinId = node.basinId;
		WaterBudgetState waterBudgetState = waterBudgetStates[basinId];

		// FOR EACH BASIN
		double precipitation = precipMap[basinId];
		double temperature = tempMap[basinId];
		double etp = etpMap[basinId];
		double basinAreaKm2 = basinAreas[basinId];

		// RAIN SNOW SEPARATION
		double[] rainSnow = RainSnowSeparationPointCase.calculateRSSeparation(//
				precipitation, //
				temperature, //
				wbSimParams.rainSnowSeparation.meltingTemperature, //
				wbSimParams.rainSnowSeparation.alfa_r, //
				wbSimParams.rainSnowSeparation.alfa_s, //
				wbSimParams.rainSnowSeparation.m1 //
		);
		
		waterBudgetState.separatedPrecipitationRain = rainSnow[0];
		waterBudgetState.separatedPrecipitationSnow = rainSnow[1];

		// SNOW MELTING
		double freezingFactor = wbSimParams.snowMelting.freezingFactor;
		freezingFactor = freezingFactor / 1440 * timeStepMinutes;
		double combinedMeltingFactor = wbSimParams.snowMelting.combinedMeltingFactor;
		combinedMeltingFactor = combinedMeltingFactor / 1440 * timeStepMinutes;

		double initialSolidWater = initialConditionSolidWater[basinId];
		double initialLiquidWater = initialConditionLiquidWater[basinId];
		
		waterBudgetState.solidWaterInitial = initialSolidWater;
		waterBudgetState.liquidWaterInitial = initialLiquidWater;

		SnowStepResult resultSM = SnowMeltingPointCaseDegreeDay.computeSnowStep( //
				temperature, //
				rainSnow[0], //
				rainSnow[1], //
				wbSimParams.rainSnowSeparation.meltingTemperature, //
				combinedMeltingFactor, //
				freezingFactor, //
				wbSimParams.snowMelting.alfa_l, //
				initialSolidWater, //
				initialLiquidWater //
		);

		initialConditionSolidWater[basinId] = resultSM.solidWater();
		initialConditionLiquidWater[basinId] = resultSM.liquidWater();
		
		waterBudgetState.solidWaterFinal = resultSM.solidWater();
		waterBudgetState.liquidWaterFinal = resultSM.liquidWater();
		waterBudgetState.swe = resultSM.swe();
		waterBudgetState.freezing = resultSM.freezing();
		waterBudgetState.melting = resultSM.melting();
		waterBudgetState.meltingDischarge = resultSM.meltingDischarge();
		waterBudgetState.errorODESolidWater = resultSM.errorODESolidWater();
		waterBudgetState.errorODELiquidWater = resultSM.errorODELiquidWater();
		waterBudgetState.errorSWE = resultSM.errorSWE();

		// CANOPY
		var kc = wbSimParams.waterBudgetCanopy.kc;
		double ci = initalConditionsCanopyMap[basinId];
		if (isNovalue(ci)) {
			ci = kc * lai / 2.0;
		}
		
		waterBudgetState.canopyInitial = ci;
		
		double meltingDischarge = resultSM.meltingDischarge();
		if (isNovalue(meltingDischarge))
			meltingDischarge = 0.0;
		if (isNovalue(etp) || etp < 0)
			etp = 0.0;
		WaterBudgetCanopyStepResult resultWBC = WaterBudgetCanopyOUT.calculateWaterBudgetCanopy(//
				meltingDischarge, //
				lai, //
				etp, //
				ci, //
				kc, //
				wbSimParams.waterBudgetCanopy.p //
		);
		initalConditionsCanopyMap[basinId] = resultWBC.waterStorage();
		
		waterBudgetState.canopyFinal = resultWBC.waterStorage();
		waterBudgetState.canopyThroughfall = resultWBC.throughfall();
		waterBudgetState.canopyAET = resultWBC.AET();
		waterBudgetState.canopyActualInput = resultWBC.actualInput();
		waterBudgetState.canopyActualOutput = resultWBC.actualOutput();
		waterBudgetState.canopyError = resultWBC.error();
		
		// ROOTZONE
		var s_RootZoneMax = wbSimParams.waterBudgetRootzone.s_RootZoneMax; 
		var sat_degree = 0.5;
		
		ci = initalConditionsRootzoneMap[basinId];
		if (isNovalue(ci)) {
			ci = s_RootZoneMax * sat_degree;
		}
		
		waterBudgetState.rootzoneInitial = ci;
		
		var throughFall = resultWBC.throughfall();
		if(isNovalue(throughFall)) {
			throughFall = 0.0;
		}
		var aet = resultWBC.AET();
		if(isNovalue(aet)) {
			aet = 0.0;
		}
		WaterBudgetRootZoneStepResult resultRZ = WaterBudgetRootZone.calculateWaterBudgetRootZone(//
				throughFall, //
				etp, //
				aet, //
				ci, //
				wbSimParams.waterBudgetRootzone.pB_soil, //
				s_RootZoneMax, //
				wbSimParams.waterBudgetRootzone.g, //
				wbSimParams.waterBudgetRootzone.h, //
				basinAreaKm2, //
				timeStepMinutes//
		);
		initalConditionsRootzoneMap[basinId] = resultRZ.waterStorage();
		
		waterBudgetState.rootzoneFinal = resultRZ.waterStorage();
		waterBudgetState.rootzoneActualInput = resultRZ.actualInput();
		waterBudgetState.rootzoneAET = resultRZ.AET();
		waterBudgetState.rootzoneRecharge = resultRZ.recharge();
		waterBudgetState.rootzoneQuick = resultRZ.quick();
		waterBudgetState.rootzoneAlpha = resultRZ.alpha();
		waterBudgetState.rootzoneError = resultRZ.error();
		
		// RUNOFF
		var s_RunoffMax = wbSimParams.waterBudgetRunoff.sRunoffMax;
		
		ci = initalConditionsRunoffMap[basinId];
		if (isNovalue(ci)) {
			ci = 0.5 * s_RunoffMax;
		}
		
		waterBudgetState.runoffInitial = ci;
		
		double quick_mm = resultRZ.quick_mm();
		if (isNovalue(quick_mm)) {
			quick_mm = 0.0;
		}
		WaterBudgetStepResult resultWB = WaterBudget.calculateWaterBudget(//
				quick_mm, //
				ci, //
				wbSimParams.waterBudgetRunoff.c, //
				wbSimParams.waterBudgetRunoff.d, //
				s_RunoffMax, //
				basinAreaKm2, //
				timeStepMinutes//
				);
		initalConditionsRunoffMap[basinId] = resultWB.waterStorage();
		
		waterBudgetState.runoffFinal = resultWB.waterStorage();
		waterBudgetState.runoffDischarge = resultWB.runoff();
		waterBudgetState.runoffError = resultWB.error();
		
		// GROUNDWATER
		var s_GroundWaterMax = wbSimParams.waterBudgetGround.s_GroundWaterMax;
		
		ci = initalConditionsGroundMap[basinId];
		if (isNovalue(ci)) {
			ci = 0.01 * s_GroundWaterMax;
		}
		
		waterBudgetState.groundInitial = ci;
		
		double recharge = resultRZ.recharge();
		if (isNovalue(recharge)) {
			recharge = 0.0;
		}
		WaterBudgetGroundStepResult resultWBG = WaterBudgetGround.calculateWaterBudgetGround(//
				recharge, //
				ci, //
				wbSimParams.waterBudgetGround.e, //
				wbSimParams.waterBudgetGround.f, //
				s_GroundWaterMax, //
				basinAreaKm2, //
				timeStepMinutes //
				);
		initalConditionsGroundMap[basinId] = resultWBG.waterStorage();
		
		waterBudgetState.groundFinal = resultWBG.waterStorage();
		waterBudgetState.groundDischarge = resultWBG.discharge();
		waterBudgetState.groundError = resultWBG.error();
		
		// final discharge
		double outDischargeRunoff = resultWB.runoff();
		double outDischargeGround = resultWBG.discharge();
		double totalDischarge = outDischargeRunoff + outDischargeGround;
		
		basinid2nodeMap[basinId].value = totalDischarge;
	}

}
