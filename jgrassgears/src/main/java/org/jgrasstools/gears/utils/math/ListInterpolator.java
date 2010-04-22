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
package org.jgrasstools.gears.utils.math;

import java.util.ArrayList;
import java.util.List;

/**
 * A class for doing interpolations on lists of X and Y.
 * 
 * @author Andrea Antonello - www.hydrologis.com
 */
public class ListInterpolator {

    private final List<Double> xList;
    private final List<Double> yList;

    public ListInterpolator( List<Double> xList, List<Double> yList ) {
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

        // if out of range
        if (xValue < xList.get(0) || xValue > xList.get(xList.size() - 1)) {
            return new Double(Double.NaN);
        }

        for( int i = 0; i < xList.size(); i++ ) {
            Double x2 = xList.get(i);
            // if equal to a number in the list
            if (x2.equals(xValue)) {
                return yList.get(i);
            }// else interpolate
            else if (x2 > xValue) {
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

        // if out of range
        if (yValue < yList.get(0) || yValue > yList.get(yList.size() - 1)) {
            return new Double(Double.NaN);
        }

        for( int i = 0; i < yList.size(); i++ ) {
            Double y2 = yList.get(i);
            // if equal to a number in the list
            if (y2.equals(yValue)) {
                return xList.get(i);
            }// else interpolate
            else if (y2 > yValue) {
                double y1 = yList.get(i - 1);
                double x1 = xList.get(i - 1);
                double x2 = xList.get(i);

                double x = (x2 - x1) * (yValue - y1) / (y2 - y1) + x1;
                return x;
            }
        }
        return new Double(Double.NaN);
    }

    public static void main( String[] args ) {
        List<Double> x = new ArrayList<Double>();
        x.add(1.0);
        x.add(3.0);
        List<Double> y = new ArrayList<Double>();
        y.add(1.0);
        y.add(3.0);

        ListInterpolator ip = new ListInterpolator(x, y);
        System.out.println(ip.linearInterpolateY(2.0));

    }
}
