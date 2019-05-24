package org.hortonmachine.gears.utils.math.regressions;

/**
 * Taken from https://stackoverflow.com/questions/17592139/trend-lines-regression-curve-fitting-java-library
 */
public class LogTrendLine extends OLSTrendLine {

    private double a;
    private double b;

    public LogTrendLine( double a, double b ) {
        this.a = a;
        this.b = b;
    }

    public LogTrendLine() {
        this.a = 1;
        this.b = 0;
    }

    @Override
    protected double[] xVector( double x ) {
        return new double[]{1, a * Math.log(x) + b};
    }

    @Override
    protected boolean logY() {
        return false;
    }
}