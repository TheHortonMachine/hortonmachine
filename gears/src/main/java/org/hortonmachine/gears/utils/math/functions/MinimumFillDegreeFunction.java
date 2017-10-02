/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org (C) HydroloGIS -
 * www.hydrologis.com
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

import static java.lang.Math.pow;
import static java.lang.Math.sin;
/**
 * Mathematical function for the minimum fill degree.
 * 
 * <p>
 * It's a function that is used to evaluate the empty degree angle.
 * </p>
 * 
 * <p>
 * The parameters required by the functions in {@link #setParameters(double...)}
 * are:
 * <ul>
 * <li><b>known</b> the know value.</li>
 * <li><b>exponent</b> the value used as exponent.</li>
 * <li><b>minG</b> the fill degree</li>
 * </ul>
 * 
 * @author Daniele Andreis
 */
public final class MinimumFillDegreeFunction implements ISingleArgmentFunction {

    private double known;
    private double exponent;
    private double minG;

    public double getValue( double x ) {
        if (x > 0) {
            return (known - (x - sin(x)) * pow((1 - sin(x) / (x)), exponent));
        } else {
            /* minimum fill degree allowed */
            return (known - (minG - sin(minG)) * pow((1 - sin(minG) / (minG)), exponent));
        }

    }

    public void setParameters( double... params ) {
        known = params[0];
        exponent = params[1];
        minG = params[2];
    }

}