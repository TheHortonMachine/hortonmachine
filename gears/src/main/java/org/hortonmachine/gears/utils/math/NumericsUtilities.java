/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.utils.math;

import static java.lang.Math.*;
import static java.lang.Float.*;
import static java.lang.Double.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * Class to help out with numeric issues, mostly due to floating point usage.
 * 
 * <p>
 * Since the floating point representation keeps a constant relative precision,
 * comparison is done using relative error.  
 * </p>
 * <p>
 * Be aware of the fact that the methods 
 * <ul>
 * <li>{@link #dEq(double, double)}</li>
 * <li>{@link #fEq(float, float)}</li>
 * </ul>
 * can be used in the case of "simple" numerical
 * comparison, while in the case of particular values that are generated through 
 * iterations the user/developer should consider to supply an epsilon value
 * derived from the knowledge of the domain of the current problem 
 * and use the methods
 * <ul>
 * <li>{@link #dEq(double, double, double)}</li>
 * <li>{@link #fEq(float, float, float)}</li>
 * </ul>
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NumericsUtilities {

    /**
     * The machine epsilon for double values.
     */
    private static double MACHINE_D_EPSILON;

    /**
     * The machine epsilon for float values.
     */
    private static float MACHINE_F_EPSILON;

    // calculate the machine epsilon
    static {
        float fTmp = 0.5f;
        double dTmp = 0.5d;
        while( 1 + fTmp > 1 )
            fTmp = fTmp / 2;
        while( 1 + dTmp > 1 )
            dTmp = dTmp / 2;
        MACHINE_D_EPSILON = dTmp;
        MACHINE_F_EPSILON = fTmp;
    }

    /**
     * The double tolerance used for comparisons.
     */
    public final static double D_TOLERANCE = MACHINE_D_EPSILON * 10d;

    /**
     * The float tolerance used for comparisons.
     */
    public final static float F_TOLERANCE = MACHINE_F_EPSILON * 10f;

    /**
     * Radiants to degrees conversion factor.
     */
    public static final double RADTODEG = 360.0 / (2.0 * PI);

    /**
     * Getter for the calculated machine double epsilon.
     * 
     * @return the machine epsilon for double values.
     */
    public static double getMachineDEpsilon() {
        return MACHINE_D_EPSILON;
    }

    /**
     * Getter for the calculated machine float epsilon.
     * 
     * @return the machine epsilon for float values.
     */
    public static float machineFEpsilon() {
        return MACHINE_F_EPSILON;
    }

    /**
     * Returns true if two doubles are considered equal based on a tolerance of {@value #D_TOLERANCE}.
     * 
     * <p>Note that two {@link Double#NaN} are seen as equal and return true.</p>
     * 
     * @param a double to compare.
     * @param b double to compare.
     * @return true if two doubles are considered equal. 
     */
    public static boolean dEq( double a, double b ) {
        if (isNaN(a) && isNaN(b)) {
            return true;
        }
        double diffAbs = abs(a - b);
        return a == b ? true : diffAbs < D_TOLERANCE ? true : diffAbs / Math.max(abs(a), abs(b)) < D_TOLERANCE;
    }

    /**
     * Returns true if two doubles are considered equal based on an supplied epsilon.
     * 
     * <p>Note that two {@link Double#NaN} are seen as equal and return true.</p>
     * 
     * @param a double to compare.
     * @param b double to compare.
     * @return true if two doubles are considered equal. 
     */
    public static boolean dEq( double a, double b, double epsilon ) {
        if (isNaN(a) && isNaN(b)) {
            return true;
        }
        double diffAbs = abs(a - b);
        return a == b ? true : diffAbs < epsilon ? true : diffAbs / Math.max(abs(a), abs(b)) < epsilon;
    }

    /**
     * Returns true if two floats are considered equal based on a tolerance of {@value #F_TOLERANCE}.
     * 
     * <p>Note that two {@link Float#NaN} are seen as equal and return true.</p>
     * 
     * @param a float to compare.
     * @param b float to compare.
     * @return true if two floats are considered equal. 
     */
    public static boolean fEq( float a, float b ) {
        if (isNaN(a) && isNaN(b)) {
            return true;
        }
        float diffAbs = abs(a - b);
        return a == b ? true : diffAbs < F_TOLERANCE ? true : diffAbs / Math.max(abs(a), abs(b)) < F_TOLERANCE;
    }

    /**
     * Returns true if two floats are considered equal based on an supplied epsilon.
     * 
     * <p>Note that two {@link Float#NaN} are seen as equal and return true.</p>
     * 
     * @param a float to compare.
     * @param b float to compare.
     * @return true if two float are considered equal. 
     */
    public static boolean fEq( float a, float b, float epsilon ) {
        if (isNaN(a) && isNaN(b)) {
            return true;
        }
        float diffAbs = abs(a - b);
        return a == b ? true : diffAbs < epsilon ? true : diffAbs / Math.max(abs(a), abs(b)) < epsilon;
    }

    /**
     * Checks if a string is a number (currently Double, Float, Integer).
     * @param <T>
     * 
     * @param value the string to check. 
     * @param adaptee the class to check against. If null, the more permissive {@link Double} will be used.
     * @return the number or <code>null</code>, if the parsing fails.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T isNumber( String value, Class<T> adaptee ) {
        if (value == null) {
            return null;
        }
        if (adaptee == null) {
            adaptee = (Class<T>) Double.class;
        }
        if (adaptee.isAssignableFrom(Double.class)) {
            try {
                Double parsed = Double.parseDouble(value);
                return adaptee.cast(parsed);
            } catch (Exception e) {
                return null;
            }
        } else if (adaptee.isAssignableFrom(Float.class)) {
            try {
                Float parsed = Float.parseFloat(value);
                return adaptee.cast(parsed);
            } catch (Exception e) {
                return null;
            }
        } else if (adaptee.isAssignableFrom(Integer.class)) {
            try {
                Integer parsed = Integer.parseInt(value);
                return adaptee.cast(parsed);
            } catch (Exception e) {
                try {
                    // try also double and convert by truncating
                    Integer parsed = (int) Double.parseDouble(value);
                    return adaptee.cast(parsed);
                } catch (Exception ex) {
                    return null;
                }
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Calculates the hypothenuse as of the Pythagorean theorem.
     * 
     * @param d1 the length of the first leg.
     * @param d2 the length of the second leg.
     * @return the length of the hypothenuse.
     */
    public static double pythagoras( double d1, double d2 ) {
        return sqrt(pow(d1, 2.0) + pow(d2, 2.0));
    }

    /**
     * Check if value is inside a ND interval (bounds included).
     * 
     * @param value the value to check.
     * @param ranges the bounds (low1, high1, low2, high2, ...)
     * @return <code>true</code> if value lies inside the interval.
     */
    public static boolean isBetween( double value, double... ranges ) {
        boolean even = true;
        for( int i = 0; i < ranges.length; i++ ) {
            if (even) {
                // lower bound
                if (value < ranges[i]) {
                    return false;
                }
            } else {
                // higher bound
                if (value > ranges[i]) {
                    return false;
                }
            }
            even = !even;
        }
        return true;
    }

    /** Lanczos coefficients */
    private static final double[] LANCZOS = {0.99999999999999709182, 57.156235665862923517, -59.597960355475491248,
            14.136097974741747174, -0.49191381609762019978, .33994649984811888699e-4, .46523628927048575665e-4,
            -.98374475304879564677e-4, .15808870322491248884e-3, -.21026444172410488319e-3, .21743961811521264320e-3,
            -.16431810653676389022e-3, .84418223983852743293e-4, -.26190838401581408670e-4, .36899182659531622704e-5,};

    /** Avoid repeated computation of log of 2 PI in logGamma */
    private static final double HALF_LOG_2_PI = 0.5 * log(2.0 * PI);

    /**
     * Gamma function ported from the apache math package.
     * 
     * <b>This should be removed if the apache math lib gets in use by HortonMachine.</b>
     * 
     * <p>Returns the natural logarithm of the gamma function &#915;(x).
     *
     * The implementation of this method is based on:
     * <ul>
     * <li><a href="http://mathworld.wolfram.com/GammaFunction.html">
     * Gamma Function</a>, equation (28).</li>
     * <li><a href="http://mathworld.wolfram.com/LanczosApproximation.html">
     * Lanczos Approximation</a>, equations (1) through (5).</li>
     * <li><a href="http://my.fit.edu/~gabdo/gamma.txt">Paul Godfrey, A note on
     * the computation of the convergent Lanczos complex Gamma approximation
     * </a></li>
     * </ul>
     *
     * @param x Value.
     * @return log(&#915;(x))
     */
    public static double logGamma( double x ) {
        double ret;

        if (Double.isNaN(x) || (x <= 0.0)) {
            ret = Double.NaN;
        } else {
            double g = 607.0 / 128.0;

            double sum = 0.0;
            for( int i = LANCZOS.length - 1; i > 0; --i ) {
                sum = sum + (LANCZOS[i] / (x + i));
            }
            sum = sum + LANCZOS[0];

            double tmp = x + g + .5;
            ret = ((x + .5) * log(tmp)) - tmp + HALF_LOG_2_PI + log(sum / x);
        }

        return ret;
    }

    public static double gamma( double x ) {
        return exp(logGamma(x));
    }

    /**
     * Normalizes a value for a given normalization max (assuming the lower is 0).
     * 
     * @param max the max of the sampling values.
     * @param min the min of the sampling values.
     * @param value the current value from the sampling values to be normalized.
     * @param normMax the normalization maximum to use.
     * @return the normalized value.
     */
    public static double normalize( double max, double min, double value, double normMax ) {
        return normMax / (1 + ((max - value) / (value - min)));
    }

    /**
     * Get the range index for which x is negative. 
     * 
     * @param x
     * @return
     */
    public static List<int[]> getNegativeRanges( double[] x ) {
        int firstNegative = -1;
        int lastNegative = -1;
        List<int[]> rangeList = new ArrayList<int[]>();

        for( int i = 0; i < x.length; i++ ) {
            double xValue = x[i];
            if (firstNegative == -1 && xValue < 0) {
                firstNegative = i;
            } else if (firstNegative != -1 && lastNegative == -1 && xValue > 0) {
                lastNegative = i - 1;
            }

            if (i == x.length - 1 && firstNegative != -1 && lastNegative == -1) {
                // need to close the range with the last value available, even if negative
                lastNegative = i;
            }

            if (firstNegative != -1 && lastNegative != -1) {
                rangeList.add(new int[]{firstNegative, lastNegative});
                firstNegative = -1;
                lastNegative = -1;
            }
        }

        return rangeList;
    }

    /**
     * Creates an array of equal bin from a range and the number of bins.
     * 
     * @param min the min value.
     * @param max the max value.
     * @param binsNum the number of wanted bins.
     * @return the array of bin bounds of size [binNum+1], since it contains the min and max.
     */
    public static double[] range2Bins( double min, double max, int binsNum ) {
        double delta = (max - min) / binsNum;
        double[] bins = new double[binsNum + 1];
        int count = 0;
        double running = min;
        for( int i = 0; i < binsNum; i++ ) {
            bins[count] = running;
            running = running + delta;
            count++;
        }
        bins[binsNum] = max;
        return bins;
    }

    /**
     * Creates an array of bins from a range and a step to use.
     * 
     * <p>Note that if the step doesn't split the range exactly, the last bin 
     * will be smaller if doLastEqual is set to <code>false</code>.</p>
     * 
     * @param min the min value.
     * @param max the max value.
     * @param step the wanted size of the bins.
     * @param doLastEqual make also the last bin of equal step size.
     * @return the array of bin bounds.
     */
    public static double[] range2Bins( double min, double max, double step, boolean doLastEqual ) {
        double intervalsDouble = (max - min) / step;

        int intervals = (int) intervalsDouble;
        double rest = intervalsDouble - intervals;
        if (rest > D_TOLERANCE) {
            intervals++;
        }

        double[] bins = new double[intervals + 1];
        int count = 0;
        double running = min;
        for( int i = 0; i < intervals; i++ ) {
            bins[count] = running;
            running = running + step;
            count++;
        }
        if (doLastEqual) {
            bins[intervals] = running;
        } else {
            bins[intervals] = max;
        }
        return bins;
    }

}
