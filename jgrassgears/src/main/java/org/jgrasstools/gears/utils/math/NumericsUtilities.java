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

import static java.lang.Math.*;
import static java.lang.Float.*;
import static java.lang.Double.*;

/**
 * Class to help out with numeric issues.
 * 
 * <p>
 * Since the floating point representation keeps a constant relative precision,
 * comparison is done using relative error.  
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
    private final static double D_TOLERANCE = MACHINE_D_EPSILON * 10d;

    /**
     * The float tolerance used for comparisons.
     */
    private final static float F_TOLERANCE = MACHINE_F_EPSILON * 10f;

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
        return a == b ? true : 
            diffAbs < D_TOLERANCE ? true :
            diffAbs / max(abs(a), abs(b)) < D_TOLERANCE;
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
        return a == b ? true : 
            diffAbs < epsilon ? true :
            diffAbs / max(abs(a), abs(b)) < epsilon;
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
        return a == b ? true : 
            diffAbs < F_TOLERANCE ? true :
            diffAbs / max(abs(a), abs(b)) < F_TOLERANCE;
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
        return a == b ? true : 
            diffAbs < epsilon ? true :
            diffAbs / max(abs(a), abs(b)) < epsilon;
    }

}
