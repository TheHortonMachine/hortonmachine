package org.hortonmachine.geoframe.core;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.util.HashSet;
import java.util.Set;

import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.geoframe.core.parameters.RainSnowSeparationParameters;
import org.hortonmachine.geoframe.core.parameters.SnowMeltingParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetCanopyParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetGroundParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRootzoneParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRunoffParameters;
import org.hortonmachine.geoframe.core.utils.TopologyNode;

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
	
	@Description("Rain snow separation parameters")
	@In
	public RainSnowSeparationParameters rssepParam;
	
	@Description("Snow melting parameters")
	@In
	public SnowMeltingParameters snowMParams;
	
	@Description("Water budget canopy parameters")
	@In
	public WaterBudgetCanopyParameters wbCanopyParams;
	
	@Description("Water budget rootzone parameters")
	@In
	public WaterBudgetRootzoneParameters wbRootzoneParams;
	
	@Description("Water budget runoff parameters")
	@In
	public WaterBudgetRunoffParameters wbRunoffParams;
	
	@Description("Water budget groundwater parameters")
	@In
	public WaterBudgetGroundParameters wbGroundParams;
	
	@Description("Leaf area index")
	@In
	public double lai;
	
	@Description("Results writer")
	@In
	public GeoframeWaterBudgetSimulationWriter resultsWriter;
	

	/**
	 * Whether to do the parallel processing
	 */
	public boolean doParallel = true;
	/**
	 * If parallel processing is chosen, whether to do it topologically (at nodes wait for upstream nodes to finish)
	 */
	public boolean doParallelTopologically = true;
	
	private TopologyNode[] basinid2nodeMap = null;
	
	public boolean doDebugMessages = true;

	private String previousDay = null;

	@Initialize
	public void init() {
		if (basinid2nodeMap == null) {
			// create a map of basinId -> TopologyNode
			basinid2nodeMap = new TopologyNode[basinAreas.length];
			rootNode.visitUpstream(node -> {
				basinid2nodeMap[node.basinId] = node;
			});
		}
	}
	
	@Execute	
	public void process() throws Exception {
		while (precipReader.next() && tempReader.next() && etpReader.next()) {
			double[] precipMap = precipReader.outData;
			double[] tempMap = tempReader.outData;
			double[] etpMap = etpReader.outData;

			String pTs = precipReader.tCurrent;
			String tTs = tempReader.tCurrent;
			String eTs = etpReader.tCurrent;
			// check that time steps are the same
			if (!pTs.equals(tTs) || !pTs.equals(eTs)) {
				throw new Exception("Time steps do not match: " + pTs + " <> " + tTs + " <> " + eTs);
			}

//			pm.message("Processing time step: " + pTs);
			
			// reset nodes values
			rootNode.visitUpstream(node -> {
				node.value = Double.NaN;
				node.accumulatedValue = Double.NaN;
			});
			
			if (doParallel) {
				if (doParallelTopologically) {
					rootNode.visitDownstreamFromLeavesParallel(null, node -> {
						processTimestep(precipMap, tempMap, etpMap, node);
					});
				} else {
					Set<TopologyNode> nodes = new HashSet<>();
					TopologyNode.collectAllUpstreamRecursive(rootNode, nodes);
					nodes.parallelStream().forEach(node -> {
						processTimestep(precipMap, tempMap, etpMap, node);
					});
				}
			} else {
				// process each node independently in parallel
				rootNode.visitDownstreamFromLeaves(node -> {
					processTimestep(precipMap, tempMap, etpMap, node);
				});
			}
			
			// accumulate the discharges downstream
			TopologyNode.accumulateDownstream(rootNode);
			
			// TODO do something with the resulting discharges
			if (doDebugMessages) {
				String yearDay = pTs.substring(0, 10);
				if (previousDay == null || !yearDay.equals(previousDay)) {
					pm.message(pTs + "; " + rootNode.accumulatedValue + " m3/s at outlet");
					previousDay = yearDay;
				}
			}
			resultsWriter.currentT = precipReader.currentT;
			resultsWriter.insert();
		}
		
	}

	private void processTimestep(double[] precipMap, double[] tempMap, double[] etpMap, TopologyNode node) {
		int basinId = node.basinId;

		// FOR EACH BASIN
		double precipitation = precipMap[basinId];
		double temperature = tempMap[basinId];
		double etp = etpMap[basinId];
		double basinAreaKm2 = basinAreas[basinId];

		// RAIN SNOW SEPARATION
		double[] rainSnow = RainSnowSeparationPointCase.calculateRSSeparation(//
				precipitation, //
				temperature, //
				rssepParam.meltingTemperature(), //
				rssepParam.alfa_r(), //
				rssepParam.alfa_s(), //
				rssepParam.m1() //
		);

		// SNOW MELTING
		double freezingFactor = snowMParams.freezingFactor();
		freezingFactor = freezingFactor / 1440 * timeStepMinutes;
		double combinedMeltingFactor = snowMParams.combinedMeltingFactor();
		combinedMeltingFactor = combinedMeltingFactor / 1440 * timeStepMinutes;

		double initialSolidWater = initialConditionSolidWater[basinId];
		double initialLiquidWater = initialConditionLiquidWater[basinId];

		SnowStepResult resultSM = SnowMeltingPointCaseDegreeDay.computeSnowStep( //
				temperature, //
				rainSnow[0], //
				rainSnow[1], //
				rssepParam.meltingTemperature(), //
				combinedMeltingFactor, //
				freezingFactor, //
				snowMParams.alfa_l(), //
				initialSolidWater, //
				initialLiquidWater //
		);

		initialConditionSolidWater[basinId] = resultSM.solidWater();
		initialConditionLiquidWater[basinId] = resultSM.liquidWater();

		// CANOPY
		var kc = wbCanopyParams.kc();
		double ci = initalConditionsCanopyMap[basinId];
		if (isNovalue(ci)) {
			ci = kc * lai / 2.0;
		}
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
				wbCanopyParams.p() //
		);
		initalConditionsCanopyMap[basinId] = resultWBC.waterStorage();
		
		// ROOTZONE
		var s_RootZoneMax = wbRootzoneParams.s_RootZoneMax(); 
		var sat_degree = 0.5;
		
		ci = initalConditionsRootzoneMap[basinId];
		if (isNovalue(ci)) {
			ci = s_RootZoneMax * sat_degree;
		}
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
				wbRootzoneParams.pB_soil(), //
				s_RootZoneMax, //
				wbRootzoneParams.g(), //
				wbRootzoneParams.h(), //
				basinAreaKm2, //
				timeStepMinutes//
		);
		initalConditionsRootzoneMap[basinId] = resultRZ.waterStorage();
		
		// RUNOFF
		var s_RunoffMax = wbRunoffParams.sRunoffMax();
		
		ci = initalConditionsRunoffMap[basinId];
		if (isNovalue(ci)) {
			ci = 0.5 * s_RunoffMax;
		}
		
		double quick_mm = resultRZ.quick_mm();
		if (isNovalue(quick_mm)) {
			quick_mm = 0.0;
		}
		WaterBudgetStepResult resultWB = WaterBudget.calculateWaterBudget(//
				quick_mm, //
				ci, //
				wbRunoffParams.c(), //
				wbRunoffParams.d(), //
				s_RunoffMax, //
				basinAreaKm2, //
				timeStepMinutes//
				);
		initalConditionsRunoffMap[basinId] = resultWB.waterStorage();
		
		// GROUNDWATER
		var s_GroundWaterMax = wbGroundParams.s_GroundWaterMax();
		
		ci = initalConditionsGroundMap[basinId];
		if (isNovalue(ci)) {
			ci = 0.01 * s_GroundWaterMax;
		}
		double recharge = resultRZ.recharge();
		if (isNovalue(recharge)) {
			recharge = 0.0;
		}
		WaterBudgetGroundStepResult resultWBG = WaterBudgetGround.calculateWaterBudgetGround(//
				recharge, //
				ci, //
				wbGroundParams.e(), //
				wbGroundParams.f(), //
				s_GroundWaterMax, //
				basinAreaKm2, //
				timeStepMinutes //
				);
		initalConditionsGroundMap[basinId] = resultWBG.waterStorage();
		
		// final discharge
		double outDischargeRunoff = resultWB.runoff();
		double outDischargeGround = resultWBG.discharge();
		double totalDischarge = outDischargeRunoff + outDischargeGround;
		
		basinid2nodeMap[basinId].value = totalDischarge;
	}

}
