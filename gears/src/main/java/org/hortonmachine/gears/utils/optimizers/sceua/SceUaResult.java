package org.hortonmachine.gears.utils.optimizers.sceua;

/**
 * Result of an SCE-UA calibration run.
 */
public final class SceUaResult {

    private final double[] bestParameters;
    private final double bestObjective;
    private final int iterations;
    private final int evaluations;

    public SceUaResult(double[] bestParameters, double bestObjective,
                       int iterations, int evaluations) {
        this.bestParameters = bestParameters;
        this.bestObjective = bestObjective;
        this.iterations = iterations;
        this.evaluations = evaluations;
    }

    /**
     * Best parameter vector found.
     */
    public double[] getBestParameters() {
        return bestParameters.clone();
    }

    /**
     * Best objective value (smaller is better).
     */
    public double getBestObjective() {
        return bestObjective;
    }

    /**
     * Number of SCE iterations performed.
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * Number of objective function evaluations performed.
     */
    public int getEvaluations() {
        return evaluations;
    }
}
