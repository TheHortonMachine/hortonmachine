/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.utils.math.interpolation;

import java.util.List;

import org.hortonmachine.gears.libs.modules.HMConstants;

/**
 * A least square regression interpolator.
 * 
 * <p>
 * This was done basing on the Java Number Cruncher Book by Ronald Mak
 * (see http://www.apropos-logic.com/books.html).
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LeastSquaresInterpolator implements Interpolator {
    private int n;
    private double[][] data;
    /** line coefficient a0 */
    private double a0;
    /** line coefficient a1 */
    private double a1;
    private double sumX;
    private double sumY;
    private double sumXX;
    private double sumXY;
    private boolean coefsValid;

    /**
     * Constructor.
     * 
     * @param xList the list of X samples.
     * @param yList the list of Y = f(X) samples.
     */
    public LeastSquaresInterpolator( List<Double> xList, List<Double> yList ) {

        data = new double[xList.size()][xList.size()];
        for( int i = 0; i < xList.size(); i++ ) {
            data[i][0] = xList.get(i);
            data[i][1] = yList.get(i);
        }

        for( int i = 0; i < data.length; ++i ) {
            addPoint(data[i]);
        }
    }

    private void addPoint( double[] dataPoint ) {
        if (n >= data.length)
            return;

        sumX += dataPoint[0];
        sumY += dataPoint[1];
        sumXX += dataPoint[0] * dataPoint[0];
        sumXY += dataPoint[0] * dataPoint[1];

        ++n;
        coefsValid = false;
    }

    public double getInterpolated( double x ) {
        if (n < 2)
            return HMConstants.doubleNovalue;

        validateCoefficients();
        return a0 + a1 * x;
    }

    /**
     * Validate the coefficients.
     */
    private void validateCoefficients() {
        if (coefsValid)
            return;

        if (n >= 2) {
            double xBar = (double) sumX / n;
            double yBar = (double) sumY / n;

            a1 = (double) ((n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX));
            a0 = (double) (yBar - a1 * xBar);
        } else {
            a0 = a1 = Double.NaN;
        }

        coefsValid = true;
    }

    /**
     * Return the coefficient a0.
     * @return the value of a0
     */
    public double getA0() {
        validateCoefficients();
        return a0;
    }

    /**
     * Return the coefficient a1.
     * @return the value of a1
     */
    public double getA1() {
        validateCoefficients();
        return a1;
    }

    /**
     * Return the sum of the x values.
     * @return the sum
     */
    public double getSumX() {
        return sumX;
    }

    /**
     * Return the sum of the y values.
     * @return the sum
     */
    public double getSumY() {
        return sumY;
    }

    /**
     * Return the sum of the x*x values.
     * @return the sum
     */
    public double getSumXX() {
        return sumXX;
    }

    /**
     * Return the sum of the x*y values.
     * @return the sum
     */
    public double getSumXY() {
        return sumXY;
    }

}