package org.hortonmachine.geoframe.utils;

import java.util.concurrent.atomic.AtomicInteger;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.geoframe.core.TopologyNode;
import org.hortonmachine.geoframe.core.parameters.RainSnowSeparationParameters;
import org.hortonmachine.geoframe.core.parameters.SnowMeltingParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetCanopyParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetGroundParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRootzoneParameters;
import org.hortonmachine.geoframe.core.parameters.WaterBudgetRunoffParameters;
import org.hortonmachine.geoframe.io.GeoframeEnvDatabaseIterator;

/**
 * A functional interface representing a complete execution of the
 * Geoframe Water Budget simulation.
 * <p>
 * This interface allows you to pass the simulation
 * logic as a lambda expression or method reference. Typical usage is:
 * </p>
 *
 * <pre>{@code
 * WaterBudgetSimulationRunner runner = YourClass::runSimulation;
 * double[] result = runner.run(...);
 * }</pre>
 */
@FunctionalInterface
public interface IWaterBudgetSimulationRunner {

    /**
     * Executes a water budget simulation for the given time interval and model parameters.
     *
     * @param fromTS              Start timestamp of the simulation (inclusive), as a string.
     * @param toTS                End timestamp of the simulation (inclusive), as a string.
     * @param timeStepMinutes     Time step in minutes for the simulation.
     * @param maxBasinId          Maximum basin ID (used to size output and state arrays).
     * @param rootNode            The root of the topological basin network.
     * @param basinAreas          Array of basin areas indexed by basin ID.
     * @param rssepParam          Parameters for rain/snow separation.
     * @param snowMParams         Parameters for snow melting.
     * @param wbCanopyParams      Water budget parameters for the canopy.
     * @param wbRootzoneParams    Water budget parameters for the root zone.
     * @param wbRunoffParams      Water budget parameters for runoff generation.
     * @param wbGroundParams      Water budget parameters for groundwater.
     * @param lai                 Leaf Area Index used by the canopy model.
     * @param outputDb            Output database for writing simulation results (may be null).
     * @param precipReader        Iterator providing precipitation input data.
     * @param tempReader          Iterator providing temperature input data.
     * @param etpReader           Iterator providing evapotranspiration input data.
     * @param calibrationRun      Counter for calibration runs (may be null).
     * @param pm                  Progress monitor for messages and progress tracking.
     * @return                    Array containing the simulated discharge values at the root node over time.
     * @throws Exception          If the simulation or input/output operations fail.
     */
    double[] run(
        String fromTS,
        String toTS,
        int timeStepMinutes,
        int maxBasinId,
        TopologyNode rootNode,
        double[] basinAreas,
        RainSnowSeparationParameters rssepParam,
        SnowMeltingParameters snowMParams,
        WaterBudgetCanopyParameters wbCanopyParams,
        WaterBudgetRootzoneParameters wbRootzoneParams,
        WaterBudgetRunoffParameters wbRunoffParams,
        WaterBudgetGroundParameters wbGroundParams,
        double lai,
        ADb outputDb,
        GeoframeEnvDatabaseIterator precipReader,
        GeoframeEnvDatabaseIterator tempReader,
        GeoframeEnvDatabaseIterator etpReader,
        Integer calibrationRun,
        IHMProgressMonitor pm
    ) throws Exception;
}

