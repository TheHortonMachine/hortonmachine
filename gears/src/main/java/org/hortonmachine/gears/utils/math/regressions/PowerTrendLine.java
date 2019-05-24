package org.hortonmachine.gears.utils.math.regressions;


/**
 * Taken from https://stackoverflow.com/questions/17592139/trend-lines-regression-curve-fitting-java-library
 */
public class PowerTrendLine extends OLSTrendLine {
    @Override
    protected double[] xVector( double x ) {
        return new double[]{1, Math.log(x)};
    }

    @Override
    protected boolean logY() {
        return true;
    }

}