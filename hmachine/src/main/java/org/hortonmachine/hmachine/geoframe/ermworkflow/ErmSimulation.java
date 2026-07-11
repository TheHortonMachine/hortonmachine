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
        		0.8735884428973242, 0.993814479749327, 0.4531126374672848, 0.09869395033295522, 0.003295537974792156, 0.045030885837436176, 0.2928290885280074, 0.6370162793330312, 96.31897792564652, 1.2634746150757599, 0.8765929022954652, 1.0926261086611742, 21.858921067838, 1.3654979854128697, 0.9456065556033244, 252.8095839054716, 0.8892019967496086, 0.960024453462543
            };
        
//        Best cost = -0.7140795568262194
//        		Best params = [0.8735884428973242, 0.993814479749327, 0.4531126374672848, 0.09869395033295522, 0.003295537974792156, 0.045030885837436176, 0.2928290885280074, 0.6370162793330312, 96.31897792564652, 1.2634746150757599, 0.8765929022954652, 1.0926261086611742, 21.858921067838, 1.3654979854128697, 0.9456065556033244, 252.8095839054716, 0.8892019967496086, 0.960024453462543]
//        		PSO calibration completed.
        
        sim.process();
    }
}
