package org.hortonmachine.gears.utils.optimizers.sceua;
/**
 * Computes a scalar error / cost from simulated vs observed data.
 * The optimizer always MINIMIZES this value.
 */
@FunctionalInterface
public interface ErrorFunction {

    /**
     * @param simulated simulated values
     * @param observed  observed values
     * @return scalar cost (smaller is better)
     */
    double evaluate(double[] simulated, double[] observed);
}
