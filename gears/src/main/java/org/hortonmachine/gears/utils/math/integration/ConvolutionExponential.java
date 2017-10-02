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

import org.hortonmachine.gears.utils.math.interpolation.LinearListInterpolator;

/**
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class ConvolutionExponential extends SimpsonIntegral implements IntegrableFunction {

    private double k = 0f;
    private final LinearListInterpolator timeDischargeInterpolator;

    /**
     * Calculates the integral of the exponential equation
     * 
     * @param lowerintegrationlimit
     * @param upperintegrationlimit
     * @param maximalsteps
     * @param integrationaccuracy
     * @param invasoConstant
     * @param timeDischargeInterpolator
     */
    public ConvolutionExponential( double lowerintegrationlimit, double upperintegrationlimit,
            int maximalsteps, double integrationaccuracy, double invasoConstant,
            LinearListInterpolator timeDischargeInterpolator ) {
        lowerlimit = lowerintegrationlimit;
        upperlimit = upperintegrationlimit;
        maxsteps = maximalsteps;
        accuracy = integrationaccuracy;
        this.timeDischargeInterpolator = timeDischargeInterpolator;
        strapezoid = 0f;
        k = invasoConstant;
    }

    protected double equation( double time ) {
        double d = (double) (1 / k * Math.exp(-(upperlimit - time) / k) * timeDischargeInterpolator
                .linearInterpolateY(time).doubleValue());
        return d;
    }

    public double integrate() {
        return simpson();
    }

}