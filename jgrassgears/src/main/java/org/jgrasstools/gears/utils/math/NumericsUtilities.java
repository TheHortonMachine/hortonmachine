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

import static java.lang.Math.abs;

/**
 * Class to help out with numeric issues.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NumericsUtilities {

    private final static double D_EPSILON = 1E-7;
    private final static float F_EPSILON = 1E-7f;

    /**
     * Returns true if two doubles are considered equal based on an epsilon of {@value #D_EPSILON}.
     * 
     * <p>Note that two {@link Double#NaN} are seen as equal and return true.</p>
     * 
     * @param a double to compare.
     * @param b double to compare.
     * @return true if two doubles are considered equal. 
     */
    public static boolean doubleEquals( double a, double b ) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return a == b ? true : abs(a - b) < D_EPSILON;
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
    public static boolean doubleEquals( double a, double b, double epsilon ) {
        if (Double.isNaN(a) && Double.isNaN(b)) {
            return true;
        }
        return a == b ? true : abs(a - b) < epsilon;
    }

    /**
     * Returns true if two floats are considered equal based on an epsilon of {@value #F_EPSILON}.
     * 
     * <p>Note that two {@link Float#NaN} are seen as equal and return true.</p>
     * 
     * @param a float to compare.
     * @param b float to compare.
     * @return true if two floats are considered equal. 
     */
    public static boolean floatEquals( float a, float b ) {
        if (Float.isNaN(a) && Float.isNaN(b)) {
            return true;
        }
        return a == b ? true : abs(a - b) < F_EPSILON;
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
    public static boolean floatEquals( float a, float b, float epsilon ) {
        if (Float.isNaN(a) && Float.isNaN(b)) {
            return true;
        }
        return a == b ? true : abs(a - b) < epsilon;
    }

}
