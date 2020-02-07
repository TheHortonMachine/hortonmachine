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
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ConvolutionDiffusionDischarge extends SimpsonIntegral implements IntegrableFunction {

    double[][] q = null;
    private double D = 0f;
    private double t = 0;
    private double dist = 0;
    private double c = 0;

    /**
    */
    public ConvolutionDiffusionDischarge( double lowerintegrationlimit, double upperintegrationlimit, int maximalsteps,
            double integrationaccuracy, double[][] discharge, double diffusionparam, double time, double distance, double celerity ) {

        lowerlimit = lowerintegrationlimit;
        upperlimit = upperintegrationlimit;
        maxsteps = maximalsteps;
        accuracy = integrationaccuracy;
        strapezoid = 0f;
        q = discharge;
        D = diffusionparam;
        t = time;
        c = celerity;
        dist = distance;

    }

    public void updateTime( int newt ) {
        t = newt;
    }

    public double integrate() {
        return simpson();

    }

    protected double equation( double tau ) {

        double result = t <= tau ? 0.0 : tau > q[q.length - 1][0] ? 0.0 : 1
                / (Math.sqrt(4 * Math.PI * D * Math.pow((t - tau), 3))) * ModelsEngine.widthInterpolate(q, tau, 0, 1) * dist
                / (Math.exp(Math.pow(dist - c * (t - tau), 2) / (4 * D * (t - tau))));

        return (double) result;
    }
}
