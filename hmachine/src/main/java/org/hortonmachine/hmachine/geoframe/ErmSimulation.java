package org.hortonmachine.hmachine.geoframe;

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
        sim.inGeopackagePath = "/home/hydrologis/development/hm_models_testdata/geoframe/newage/noce/inputs/outputs/geoframe_data.gpkg";
        sim.inEnvDataPath    = "/home/hydrologis/storage/lavori_tmp/GEOFRAME/env_data.sqlite";
    	sim.inFromTimestamp = "2015-10-01 01:00:00";
    	sim.inToTimestamp = "2023-10-01 01:00:00";
    	sim.pTimeStepMinutes = 60;
    	sim.pSpinUpDays = 365;
        sim.inParams = new double[]{
        		1.3723579249362787, 0.8030688513294623, 0.9521310621737642, 0.4472304203349618, 0.005941986165493968, 0.37020481034749925, 0.25309350711307343, 0.6797503705004188, 42.46662129728443, 1.1101833084335944, 0.8510346882618802, 0.9182092091642994, 5.141170995342713, 0.5892644215798772, 0.912501889777516, 100.58266069359193, 0.5874411503382478, 0.9799374057256469
            };
        sim.process();
    }
}
