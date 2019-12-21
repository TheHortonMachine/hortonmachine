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

import org.hortonmachine.gears.libs.modules.ModelsEngine;

/**
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class ConvolutionExponentialPeakflow extends SimpsonIntegral implements IntegrableFunction {

    private double k = 0f;
    private double t = 0.0;
    private final double[][] ampi_sub;

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
    public ConvolutionExponentialPeakflow( double lowerintegrationlimit, double upperintegrationlimit, int maximalsteps,
            double integrationaccuracy, double[][] ampifunction, double invasoConstant, double time ) {
        lowerlimit = lowerintegrationlimit;
        upperlimit = upperintegrationlimit;
        maxsteps = maximalsteps;
        accuracy = integrationaccuracy;
        ampi_sub = ampifunction;
        strapezoid = 0f;
        k = invasoConstant;
        t = time;
    }

    protected double equation( double time ) {
        double d = 1.0 / k * Math.exp(-t / k) * ModelsEngine.widthInterpolate(ampi_sub, time, 0, 1);
        return d;
    }

    public double integrate() {
        return simpson();
    }

}