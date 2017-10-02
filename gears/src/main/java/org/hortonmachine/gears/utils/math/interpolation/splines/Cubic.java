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
package org.hortonmachine.gears.utils.math.interpolation.splines;
/** 
 * This class represents a cubic polynomial 
 * 
 * @author Tim Lambert (http://www.cse.unsw.edu.au/~lambert/)
 */
public class Cubic {

    double a, b, c, d; /* a + b*u + c*u^2 +d*u^3 */

    public Cubic( double a, double b, double c, double d ) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    /** 
     * evaluate cubic 
     */
    public double eval( double u ) {
        return (((d * u) + c) * u + b) * u + a;
    }
}
