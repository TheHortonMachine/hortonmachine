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
package org.hortonmachine.gears.utils.math.integration;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class SimpsonIntegral {

    public static final int SIMPSON = 0;
    public static final int TRAPEZOIDAL = 0;

    protected double strapezoid = 0f;

    protected double lowerlimit = 0f;

    protected double upperlimit = 0f;

    protected int maxsteps = 0;

    protected double accuracy = 0.0;

    /**
     * Calculate the integral with the simpson method of the equation implemented in the method
     * equation
     * 
     * @return
     * @throws Exception
     */
    protected double simpson() {
        double s = 0f;
        double st = 0f;
        double ost = 0f;
        double os = 0f;

        for( int i = 1; i < maxsteps; i++ ) {
            st = trapezoid(i);

            s = (4f * st - ost) / 3f;

            if (i > 5) {
                if (Math.abs(s - os) < accuracy * Math.abs(os) || (s == 0f && os == 0f)) {
                    return s;
                }
            }
            os = s;
            ost = st;

        }
        return 0d;
    }

    /**
     * Calculate the integral with the trapezoidal algorithm of the equation implemented in the
     * method equation
     * 
     * @param n - number of steps to perform
     * @return
     */
    protected double trapezoid( int n ) {
        double x = 0;
        double tnm = 0;
        double sum = 0;
        double del = 0;
        int it = 0;
        int j = 0;

        if (n == 1) {
            strapezoid = 0.5f * (upperlimit - lowerlimit)
                    * (equation(lowerlimit) + equation(upperlimit));
        } else {
            /*
             * for (it = 1, j = 1; j < n - 1; j++) { it <<= 1; }
             */
            it = (int) Math.pow(2.0, n - 1);

            tnm = (double) it;
            del = (upperlimit - lowerlimit) / tnm;
            x = lowerlimit + 0.5f * del;
            for( sum = 0f, j = 1; j <= it; j++, x += del ) {
                if (x >= upperlimit) {
                    System.out.println("hoi");
                }
                sum += equation(x);
            }
            strapezoid = (double) (0.5f * (strapezoid + (upperlimit - lowerlimit) * sum / tnm));
        }

        return strapezoid;
    }

    /**
     * Equation to integrate
     * 
     * @param x - point in which to calculate the function
     * @return
     */
    protected abstract double equation( double x );

}