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
        sim.inFromTimestamp = ErmCommonData.START_VALIDATION_TIMESTAMP + ":00";
    	sim.inToTimestamp = ErmCommonData.END_VALIDATION_TIMESTAMP + ":00";
    	sim.pTimeStepMinutes = 60;
    	sim.pSpinUpDays = 365;
        sim.inParams = new double[]{
        		0.8863110344376395, 0.8000000004184915, 1.5068086950722062, 1.1890804824211398, 0.13878912814658773, 0.09072609806179345, 0.41343884370688305, 0.6881197400379906, 111.38191229479514, 0.19432656034641646, 0.8558729234353698, 1.1579134139427953, 31.03625970110033, 1.4993940725932058, 0.9119914754211912, 461.83757178895803, 0.08278433435183367, 0.9545277298345779
            }; // 0.8369
        
        sim.process();
    }
}
