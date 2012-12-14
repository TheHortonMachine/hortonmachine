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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.iuh;

import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.math.integration.ConvolutionExponentialPeakflow;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.ParameterBox;

/**
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvano Pisoni
 */
public class IUHSubSurface {

    private double[][] ampi_sub = null;
    private double[][] ampi_help = null;
    private double vc = 0f;
    private double delta_sub = 0f;
    private double xres = 0f;
    private double yres = 0f;
    private double npixel_sub = 0f;
    private double resid_time = 0f;
    private final IJGTProgressMonitor pm;

    public IUHSubSurface( double[][] _ampi, ParameterBox fixedParameters, IJGTProgressMonitor pm ) {
        ampi_help = _ampi;
        this.pm = pm;
        ampi_sub = new double[ampi_help.length][ampi_help[0].length];

        for( int i = 0; i < ampi_help.length; i++ ) {
            ampi_sub[i][0] = ampi_help[i][0];
        }

        vc = fixedParameters.getVc();
        delta_sub = fixedParameters.getDelta_sub();
        xres = fixedParameters.getXres();
        yres = fixedParameters.getYres();
        npixel_sub = fixedParameters.getNpixel_sub();
        resid_time = fixedParameters.getResid_time();
    }

    public double[][] calculateIUH() {
        double cum = 0f;
        double t = 0;
        double integral = 0;

        /*
         * next part calculates the convolution between the aplitude function and the exponential
         * equation
         */
        pm.beginTask("Calculating subsurface IUH...", ampi_help.length - 1);
        for( int i = 0; i < ampi_help.length - 1; i++ ) {
            t = ampi_sub[i + 1][0];

            double upperintegrationlimit = ampi_sub[ampi_sub.length - 1][0];
            ConvolutionExponentialPeakflow expIntegral = new ConvolutionExponentialPeakflow(0.0,
                    upperintegrationlimit, 20, 0.00001, ampi_help, resid_time, t);

            integral = expIntegral.integrate();
            ampi_sub[i + 1][1] = integral;
            /*
             * if (isScs) { cum += integral delta_sub / (xres yres npixel_sub vc / vcvv); } else {
             */
            cum += integral * delta_sub / (xres * yres * npixel_sub * vc);

            ampi_sub[i + 1][2] = cum;

            pm.worked(1);
        }
        pm.done();

        return ampi_sub;
    }
}
