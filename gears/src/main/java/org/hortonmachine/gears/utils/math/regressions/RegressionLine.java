package org.hortonmachine.gears.utils.math.regressions;

/**
 * Taken from https://stackoverflow.com/questions/17592139/trend-lines-regression-curve-fitting-java-library
 */
public interface RegressionLine {
    public void setValues( double[] y, double[] x ); // y ~ f(x)

    public double predict( double x ); // get a predicted y for a given x

    double[] getRegressionParameters();

    double[] getRegressionParametersErrors();
    
    double getRSquared();
    
    double[] getResiduals();
}