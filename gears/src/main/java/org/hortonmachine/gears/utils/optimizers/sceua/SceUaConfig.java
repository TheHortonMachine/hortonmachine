package org.hortonmachine.gears.utils.optimizers.sceua;

import java.util.Random;

/**
 * Configuration parameters for the SCE-UA optimizer.
 *
 * These have reasonable hydrology-oriented defaults but can be tuned.
 */
public final class SceUaConfig {

    /** Maximum number of SCE iterations (outer loops). */
    private final int maxIterations;

    /** Maximum number of objective function evaluations. */
    private final int maxEvaluations;

    /** Number of complexes (m) in the population. */
    private final int complexCount;

    /** Convergence tolerance on the standard deviation of objective values. */
    private final double objectiveStdTolerance;

    /** Random number generator. */
    private final Random random;

    /** If true, print some progress info to stdout. */
    private final boolean verbose;

    private SceUaConfig(Builder b) {
        this.maxIterations = b.maxIterations;
        this.maxEvaluations = b.maxEvaluations;
        this.complexCount = b.complexCount;
        this.objectiveStdTolerance = b.objectiveStdTolerance;
        this.random = b.random;
        this.verbose = b.verbose;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public int getMaxEvaluations() {
        return maxEvaluations;
    }

    public int getComplexCount() {
        return complexCount;
    }

    public double getObjectiveStdTolerance() {
        return objectiveStdTolerance;
    }

    public Random getRandom() {
        return random;
    }

    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Creates a builder with defaults that work well for many hydro problems.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maxIterations = 500;
        private int maxEvaluations = 20000;
        private int complexCount = 5;
        private double objectiveStdTolerance = 1e-4;
        private Random random = new Random(12345L);
        private boolean verbose = false;

        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }

        public Builder maxEvaluations(int maxEvaluations) {
            this.maxEvaluations = maxEvaluations;
            return this;
        }

        public Builder complexCount(int complexCount) {
            this.complexCount = complexCount;
            return this;
        }

        public Builder objectiveStdTolerance(double tol) {
            this.objectiveStdTolerance = tol;
            return this;
        }

        public Builder random(Random random) {
            this.random = random;
            return this;
        }

        public Builder verbose(boolean verbose) {
            this.verbose = verbose;
            return this;
        }

        public SceUaConfig build() {
            if (maxIterations <= 0) {
                throw new IllegalArgumentException("maxIterations must be > 0");
            }
            if (maxEvaluations <= 0) {
                throw new IllegalArgumentException("maxEvaluations must be > 0");
            }
            if (complexCount < 2) {
                throw new IllegalArgumentException("complexCount must be >= 2");
            }
            if (objectiveStdTolerance <= 0) {
                throw new IllegalArgumentException("objectiveStdTolerance must be > 0");
            }
            if (random == null) {
                random = new Random();
            }
            return new SceUaConfig(this);
        }
    }
}
