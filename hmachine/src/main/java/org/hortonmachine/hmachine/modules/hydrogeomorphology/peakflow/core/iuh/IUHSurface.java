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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.iuh;

import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.math.integration.ConvolutionDiffusionWidth;
import org.hortonmachine.gears.utils.math.integration.IntegralConstants;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;

/**
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class IUHSurface {

    private double[][] ampiDiffusion = null;
    private double[][] ampi = null;
    private double diffusionParameterSup = 0f;
    private double vc = 0f;
    private double delta = 0f;
    private double xres = 0f;
    private double yres = 0f;
    private double npixel = 0f;
    private final IHMProgressMonitor pm;

    /**
     * @param out 
    * 
    */
    public IUHSurface( double[][] _ampi, ParameterBox fixedParameters,
            IHMProgressMonitor pm ) {
        ampi = _ampi;
        this.pm = pm;
        delta = fixedParameters.getDelta();

        double threshold = 1000f;
        ampiDiffusion = new double[(int) (ampi.length + threshold / delta)][ampi[0].length];

        for( int i = 0; i < ampi.length; i++ ) {
            ampiDiffusion[i][0] = ampi[i][0];
        }
        for( int i = 1; i < threshold / delta; i++ ) {
            ampiDiffusion[ampi.length - 1 + i][0] = ampi[ampi.length - 1][0] + i * delta;
        }

        diffusionParameterSup = fixedParameters.getDiffusionParameterSup();
        vc = fixedParameters.getVc();
        xres = fixedParameters.getXres();
        yres = fixedParameters.getYres();
        npixel = fixedParameters.getNpixel();
    }

    public double[][] calculateIUH() {
        double cum = 0f;
        double t = 0;
        double integral = 0;

        /*
         * next part calculates the convolution between the aplitude function and the diffusion
         * equation
         */
        ConvolutionDiffusionWidth diffIntegral = new ConvolutionDiffusionWidth(0.0,
                ampiDiffusion[ampiDiffusion.length - 1][0], IntegralConstants.diffusionSupMaxsteps,
                IntegralConstants.diffusionSupAccurancy, ampi, diffusionParameterSup, t);

        pm.beginTask("Calculating diffusion...", ampiDiffusion.length);
        for( int i = 0; i < ampiDiffusion.length; i++ ) {

            t = ampiDiffusion[i][0];

            diffIntegral.updateTime((int) t);
            integral = diffIntegral.integrate();

            ampiDiffusion[i][1] = integral;
            cum += integral * delta / (xres * yres * npixel * vc);
            ampiDiffusion[i][2] = cum;

            pm.worked(1);
        }
        pm.done();
        
        
		double maxCum = ampiDiffusion[ampiDiffusion.length - 1][2];
		double factor = 1.0 / maxCum;

		for (int i = 0; i < ampiDiffusion.length; i++) {

			ampiDiffusion[i][1] = ampiDiffusion[i][1] * factor;
			ampiDiffusion[i][2] = ampiDiffusion[i][2] * factor;

			pm.worked(1);
		}

        
        return ampiDiffusion;
    }

}
