package org.hortonmachine.gears.utils.optimizers.sceua;

/**
 * Runs the model for a given parameter vector.
 */
@FunctionalInterface
public interface SimulationFunction {

    /**
     * @param params current parameter vector
     * @return simulated time series or other output used to compute the objective
     * @throws Exception if the model fails
     */
    double[] simulate(double[] params) throws Exception;
}

