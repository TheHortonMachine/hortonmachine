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

import org.hortonmachine.gears.utils.math.NumericsUtilities;

/**
 * A class for doing linear interpolations on arrays of X and Y.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LinearArrayInterpolator implements Interpolator {

    private final double[] xList;
    private final double[] yList;
    private boolean isInverse = false;

    public LinearArrayInterpolator( double[] xList, double[] yList ) {
        if (xList.length != yList.length) {
            throw new IllegalArgumentException("The arrays have to be of the same length.");
        }
        this.xList = xList;
        this.yList = yList;
    }

    /**
     * A simple interpolation between existing numbers.
     * 
     * @param xValue the value for which we want the y
     * @return the y value
     */
    public double linearInterpolateY( double xValue ) {

        double first = xList[0];
        double last = xList[xList.length - 1];

        // check out of range
        if (first <= last) {
            if (xValue < xList[0] || xValue > xList[xList.length - 1]) {
                return Double.NaN;
            }
            isInverse = false;
        } else {
            // inverse proportional
            if (xValue > xList[0] || xValue < xList[xList.length - 1]) {
                return Double.NaN;
            }
            isInverse = true;
        }

        for( int i = 0; i < xList.length; i++ ) {
            double x2 = xList[i];
            // if equal to a number in the array
            if (NumericsUtilities.dEq(x2, xValue)) {
                return yList[i];
            }// else interpolate
            else if ((!isInverse && x2 > xValue) || (isInverse && x2 < xValue)) {
                double x1 = xList[i - 1];
                double y1 = yList[i - 1];
                double y2 = yList[i];

                double y = (y2 - y1) * (xValue - x1) / (x2 - x1) + y1;
                return y;
            }
        }
        return Double.NaN;
    }

    /**
     * A simple interpolation between existing numbers.
     * 
     * @param yValue the value for which we want the x
     * @return the x value
     */
    public double linearInterpolateX( double yValue ) {

        double first = yList[0];
        double last = yList[yList.length - 1];

        // check out of range
        if (first <= last) {
            if (yValue < yList[0] || yValue > yList[yList.length - 1]) {
                return Double.NaN;
            }
            isInverse = false;
        } else {
            // inverse proportional
            if (yValue > yList[0] || yValue < yList[yList.length - 1]) {
                return Double.NaN;
            }
            isInverse = true;
        }

        for( int i = 0; i < yList.length; i++ ) {
            double y2 = yList[i];
            // if equal to a number in the array
            if (NumericsUtilities.dEq(y2, yValue)) {
                return xList[i];
            }// else interpolate
            else if ((!isInverse && y2 > yValue) || (isInverse && y2 < yValue)) {
                double y1 = yList[i - 1];
                double x1 = xList[i - 1];
                double x2 = xList[i];

                double x = (x2 - x1) * (yValue - y1) / (y2 - y1) + x1;
                return x;
            }
        }
        return Double.NaN;
    }

    public double getInterpolated( double x ) {
        return linearInterpolateY(x);
    }

}
