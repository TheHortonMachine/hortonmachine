package org.hortonmachine.hmachine.geoframe.ermworkflow;

import org.hortonmachine.gears.utils.optimizers.CostFunctions;
import org.hortonmachine.gears.utils.optimizers.particleswarm.PSConfig;
import org.hortonmachine.hmachine.geoframe.calibration.WaterBudgetCalibration;
import org.hortonmachine.hmachine.geoframe.calibration.WaterBudgetCalibrationResult;

import oms3.annotations.Bibliography;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

@Description("Sixth step of the ERM/GeoFrame water budget workflow: calibrates the 18 parameters of the water budget model against the observed discharge, with Particle Swarm Optimisation. The best parameters found and their score are reported at the end.")
@Bibliography({"Kennedy, J., Eberhart, R. (1995). Particle swarm optimization. Proceedings of the IEEE International Conference on Neural Networks, 1942-1948.", "Gupta, H. V., Kling, H., Yilmaz, K. K., Martinez, G. F. (2009). Decomposition of the mean squared error and NSE performance criteria: implications for improving hydrological modelling. Journal of Hydrology, 377(1-2), 80-91."})
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

	@Description("Measure of the agreement between simulated and observed discharge: KGE, the Kling-Gupta efficiency (Gupta et al., 2009).")
	@In
	public CostFunctions pCostFunction = CostFunctions.KGE;
	
	@Description("If true, the progress of the calibration is reported in detail.")
	@In
	public boolean printDebugInfo = true;

	@Description("The best parameters found, in the order expected by the simulation.")
	@Out
	public double[] outParams;

	@Description("The score of the best parameters, for the chosen measure of agreement.")
	@Out
	public double outCost;

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

			WaterBudgetCalibrationResult psoCalibrationResult = WaterBudgetCalibration.psoCalibration(psConfig,
					maxBasinId, basinAreas, rootNode, pTimeStepMinutes, observedDischarge, pCostFunction,
					pCalibrationThreadCount, precipReader, tempReader, etpReader, runner, spinUpTimesteps, doWriteState,
					pm, printDebugInfo);

			outParams = psoCalibrationResult.parameters;
			outCost = -psoCalibrationResult.cost;
			pm.message("PSO calibration completed.");
			pm.message("Best parameters found: " + java.util.Arrays.toString(psoCalibrationResult.parameters));
			pm.message("Cost: " + (-psoCalibrationResult.cost));
		} finally {
			teardown();
		}
	}

	public static void main(String[] args) throws Exception {
		ErmCalibration cal = new ErmCalibration();
		cal.inGeopackagePath = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/workspace/outputs/geoframe_data.gpkg";
		cal.inFromTimestamp = ErmCommonData.START_TIMESTAMP + ":00";
		cal.inToTimestamp = ErmCommonData.END_TIMESTAMP + ":00";
		cal.pTimeStepMinutes = 60;
		cal.pSpinUpDays = 365;
		cal.pPsoIterations = 300;
		cal.pParticlesNum = 20;
		cal.doWriteState = false;
		cal.process();
	}
}
