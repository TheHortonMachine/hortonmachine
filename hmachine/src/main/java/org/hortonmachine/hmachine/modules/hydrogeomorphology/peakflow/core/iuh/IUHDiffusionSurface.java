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
public class IUHDiffusionSurface {

    private double[][] ampi_diffusion = null;
    private double[][] ampi = null;
    private double diffusionparameter = 0f;
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
    public IUHDiffusionSurface( double[][] _ampi, ParameterBox fixedParameters,
            IHMProgressMonitor pm ) {
        ampi = _ampi;
        this.pm = pm;
        delta = fixedParameters.getDelta();

        double threshold = 5000f;
        ampi_diffusion = new double[(int) (ampi.length + threshold / delta)][ampi[0].length];

        for( int i = 0; i < ampi.length; i++ ) {
            ampi_diffusion[i][0] = ampi[i][0];
        }
        for( int i = 1; i < threshold / delta; i++ ) {
            ampi_diffusion[ampi.length - 1 + i][0] = ampi[ampi.length - 1][0] + i * delta;
        }

        diffusionparameter = fixedParameters.getDiffusionparameter();
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
                ampi_diffusion[ampi_diffusion.length - 1][0], IntegralConstants.diffusionmaxsteps,
                IntegralConstants.diffusionaccurancy, ampi, diffusionparameter, t);

        pm.beginTask("Calculating diffusion...", ampi_diffusion.length - 1);
        for( int i = 0; i < ampi_diffusion.length - 1; i++ ) {

            t = ampi_diffusion[i + 1][0];

            diffIntegral.updateTime((int) t);
            integral = diffIntegral.integrate();

            ampi_diffusion[i + 1][1] = integral;
            cum += integral * delta / (xres * yres * npixel * vc);
            ampi_diffusion[i + 1][2] = cum;

            pm.worked(1);
        }
        pm.done();
        
        return ampi_diffusion;
    }

}
