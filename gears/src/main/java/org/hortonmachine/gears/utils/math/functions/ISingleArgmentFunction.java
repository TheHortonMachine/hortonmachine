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
package org.hortonmachine.gears.utils.math.functions;

/**
 * Generic mathematical function that takes a single argument.
 * 
 * @author Daniele Andreis
 */
public interface ISingleArgmentFunction {
    /**
     * Return the value of the function in a specific point.
     * 
     * @param x the point where evaluate the function.
     * @return the function value.
     */
    double getValue( double x ) throws ArithmeticException;

    /**
     * Set the parameters for the particular function.
     * 
     * <p>
     * The list and definition of the parameters are described in the class
     * documentation of each function.
     * 
     * @param params the parameters to set.
     */
    void setParameters( double... params );

}
