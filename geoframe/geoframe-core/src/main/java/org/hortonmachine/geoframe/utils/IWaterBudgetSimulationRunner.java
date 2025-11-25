package org.hortonmachine.geoframe.utils;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.geoframe.calibration.WaterBudgetParameters;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;

/**
 * An interface representing a complete execution of the
 * Geoframe Water Budget simulation.
 */
public interface IWaterBudgetSimulationRunner {

	/**
	 * Configure the runner with the necessary parameters.
	 * 
	 * @param timeStepMinutes the minutes corresponding to the time step used.
	 * @param maxBasinId the maximum basin id (to size output and state arrays).
	 * @param rootNode the root of the topological basin network.
	 * @param basinAreas array of basin areas indexed by basin id.
	 * @param doParallel whether to run the simulation in parallel mode over the basins.
	 * @param doTopologicallyOrdered whether to process nodes in topological order.
	 * @param outputDb output database for writing simulation results (may be null).
	 * @param pm progress monitor for messages and progress tracking.
	 */
	void configure(
	    int timeStepMinutes,
	    int maxBasinId,
	    TopologyNode rootNode,
	    double[] basinAreas,
	    boolean doParallel,
	    boolean doTopologicallyOrdered,
	    ADb outputDb,
	    IHMProgressMonitor pm
		);
	
    /**
     * Executes a water budget simulation for the given time interval and model parameters.
     *
     * @param wbParams all paramters required by the water budget model.
     * @param lai Leaf Area Index used by the canopy model.
     * @param precipReader Iterator providing precipitation input data.
     * @param tempReader Iterator providing temperature input data.
     * @param etpReader Iterator providing evapotranspiration input data.
     * @param iterationInfo String containing information about the current iteration (for logging purposes).
     * @return Array containing the simulated discharge values at the root node over time.
     * @throws Exception
     */
    double[] run(
        WaterBudgetParameters wbParams,
        double lai,
        GeoframeEnvDatabaseIterator precipReader,
        GeoframeEnvDatabaseIterator tempReader,
        GeoframeEnvDatabaseIterator etpReader,
        String iterationInfo
    ) throws Exception;
}

