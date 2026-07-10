package org.hortonmachine.hmachine.geoframe.ermworkflow;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.optimizers.CostFunctions;
import org.hortonmachine.hmachine.geoframe.calibration.WaterBudgetParameters;
import org.hortonmachine.hmachine.geoframe.utils.IWaterBudgetSimulationRunner;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

@Description("Runs a single ERM/GeoFrame water budget simulation with a fixed parameter set.")
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords("ERM, GeoFrame, simulation, water budget")
@Label("GeoFrame")
@Name("ermSimulation")
@Status(40)
@License("General Public License Version 3 (GPLv3)")
public class ErmSimulation extends ErmBase {

    @Description("Model parameter array (18 values).")
    @In
    public double[] inParams;

    @Execute
    public void process() throws Exception {
        setup();
        try {
            runner.configure(pTimeStepMinutes, maxBasinId, rootNode, basinAreas,
                    true, true, doWriteState, db, pm);

            WaterBudgetParameters wbParams = WaterBudgetParameters.fromParameterArray(inParams);
            double[] simQ = runner.run(wbParams, 0.6, precipReader, tempReader, etpReader, null);

            double cost = Math.round(
                    -CostFunctions.KGE.evaluateCost(observedDischarge, simQ, spinUpTimesteps,
                            HMConstants.doubleNovalue) * 100.0) / 100.0;
            pm.message("KGE = " + cost);

            IWaterBudgetSimulationRunner.quickChartResult(
                    "Simulated vs Observed Discharge ( KGE: " + cost + " )",
                    simQ, observedDischarge, pTimeStepMinutes, inFromTimestamp, spinUpTimesteps);
        } finally {
            teardown();
        }
    }


    public static void main( String[] args ) throws Exception {
        ErmSimulation sim = new ErmSimulation();
        sim.inGeopackagePath = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/workspace/outputs/geoframe_data.gpkg";
        sim.inFromTimestamp = ErmCommonData.START_TIMESTAMP + ":00";
    	sim.inToTimestamp = ErmCommonData.END_TIMESTAMP + ":00";
    	sim.pTimeStepMinutes = 60;
    	sim.pSpinUpDays = 365;
        sim.inParams = new double[]{
        		0.8001552325208339, 0.8000203098902741, -0.37755493535913126, 1.0370361098360203, 0.8391222084840597, 0.0010104465752101264, 0.1097830872727477, 0.5200281370200475, 134.70877964120544, 0.16210769418821144, 0.9976025775217975, 0.5000029390004014, 96.7303127539663, 3.82908126772242, 0.9951928673980068, 901.4679378204077, 1.7230120259344979, 0.9980412458562401
            };
        
        sim.process();
    }
}
