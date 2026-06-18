package org.hortonmachine.hmachine.geoframe;

import org.hortonmachine.gears.utils.optimizers.CostFunctions;
import org.hortonmachine.gears.utils.optimizers.particleswarm.PSConfig;
import org.hortonmachine.hmachine.geoframe.calibration.WaterBudgetCalibration;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

@Description("Calibrates ERM/GeoFrame water budget model parameters using Particle Swarm Optimisation.")
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("ERM, GeoFrame, calibration, PSO, water budget")
@Label("GeoFrame")
@Name("ermCalibration")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmCalibration extends ErmBase {

	@Description("Number of PSO iterations.")
	@In
	public int pPsoIterations = 300;

	@Description("Number of PSO particles.")
	@In
	public int pParticlesNum = 20;

	@Description("PSO cognitive acceleration constant (c1).")
	@In
	public double pC1 = 2.0;

	@Description("PSO social acceleration constant (c2).")
	@In
	public double pC2 = 2.0;

	@Description("PSO initial inertia weight (w0).")
	@In
	public double pW0 = 0.9;

	@Description("PSO inertia weight decay factor.")
	@In
	public double pDecay = 0.4;

	@Description("Number of parallel threads for calibration.")
	@In
	public int pCalibrationThreadCount = 20;

	@Description("Cost function used to evaluate parameter fitness.")
	@In
	public CostFunctions pCostFunction = CostFunctions.KGE;


	@Execute
	public void process() throws Exception {
		setup();
		try {
			precipReader.preCacheData();
			tempReader.preCacheData();
			etpReader.preCacheData();

			PSConfig psConfig = new PSConfig();
			psConfig.particlesNum = pParticlesNum;
			psConfig.maxIterations = pPsoIterations;
			psConfig.c1 = pC1;
			psConfig.c2 = pC2;
			psConfig.w0 = pW0;
			psConfig.decay = pDecay;

			double[] psoCalibration = WaterBudgetCalibration.psoCalibration(psConfig, maxBasinId, basinAreas, rootNode,
					pTimeStepMinutes, observedDischarge, pCostFunction, pCalibrationThreadCount, precipReader,
					tempReader, etpReader, runner, spinUpTimesteps, doWriteState, pm);

			pm.message(
					"PSO calibration completed.\nBest parameters found: " + java.util.Arrays.toString(psoCalibration));
		} finally {
			teardown();
		}
	}

	public static void main(String[] args) throws Exception {
		ErmCalibration cal = new ErmCalibration();
		cal.inGeopackagePath = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/inputs/outputs/geoframe_data.gpkg";
		cal.inEnvDataPath = "/home/hydrologis/storage/lavori_tmp/GEOFRAME/env_data.sqlite";
		cal.process();
	}
}
