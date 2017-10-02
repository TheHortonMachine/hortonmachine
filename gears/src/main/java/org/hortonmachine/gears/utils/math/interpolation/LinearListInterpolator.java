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

/**
 * A class for doing linear interpolations on lists of X and Y.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LinearListInterpolator implements Interpolator {

    private final List<Double> xList;
    private final List<Double> yList;
    private boolean isInverse = false;

    public LinearListInterpolator( List<Double> xList, List<Double> yList ) {
        if (xList.size() != yList.size()) {
            throw new IllegalArgumentException("The lists have to be of the same length.");
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
    public Double linearInterpolateY( Double xValue ) {

        Double first = xList.get(0);
        Double last = xList.get(xList.size() - 1);

        // check out of range
        if (first <= last) {
            if (xValue < xList.get(0) || xValue > xList.get(xList.size() - 1)) {
                return new Double(Double.NaN);
            }
            isInverse = false;
        } else {
            // inverse proportional
            if (xValue > xList.get(0) || xValue < xList.get(xList.size() - 1)) {
                return new Double(Double.NaN);
            }
            isInverse = true;
        }

        for( int i = 0; i < xList.size(); i++ ) {
            Double x2 = xList.get(i);
            // if equal to a number in the list
            if (x2.equals(xValue)) {
                return yList.get(i);
            }// else interpolate
            else if ((!isInverse && x2 > xValue) || (isInverse && x2 < xValue)) {
                double x1 = xList.get(i - 1);
                double y1 = yList.get(i - 1);
                double y2 = yList.get(i);

                double y = (y2 - y1) * (xValue - x1) / (x2 - x1) + y1;
                return y;
            }
        }
        return new Double(Double.NaN);
    }

    /**
     * A simple interpolation between existing numbers.
     * 
     * @param yValue the value for which we want the x
     * @return the x value
     */
    public Double linearInterpolateX( Double yValue ) {

        Double first = yList.get(0);
        Double last = yList.get(yList.size() - 1);

        // check out of range
        if (first <= last) {
            if (yValue < yList.get(0) || yValue > yList.get(yList.size() - 1)) {
                return new Double(Double.NaN);
            }
            isInverse = false;
        } else {
            // inverse proportional
            if (yValue > yList.get(0) || yValue < yList.get(yList.size() - 1)) {
                return new Double(Double.NaN);
            }
            isInverse = true;
        }

        for( int i = 0; i < yList.size(); i++ ) {
            Double y2 = yList.get(i);
            // if equal to a number in the list
            if (y2.equals(yValue)) {
                return xList.get(i);
            }// else interpolate
            else if ((!isInverse && y2 > yValue) || (isInverse && y2 < yValue)) {
                double y1 = yList.get(i - 1);
                double x1 = xList.get(i - 1);
                double x2 = xList.get(i);

                double x = (x2 - x1) * (yValue - y1) / (y2 - y1) + x1;
                return x;
            }
        }
        return new Double(Double.NaN);
    }

    public double getInterpolated( double x ) {
        return linearInterpolateY(x);
    }

}
