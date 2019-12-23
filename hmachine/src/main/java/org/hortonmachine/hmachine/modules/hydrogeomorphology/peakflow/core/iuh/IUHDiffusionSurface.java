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

import static java.lang.Math.PI;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.chart.PlotFrame;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.math.integration.ConvolutionDiffusionWidth;
import org.hortonmachine.gears.utils.math.integration.IntegralConstants;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;

/**
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class IUHDiffusionSurface {

    private double[][] ampiDiffusion = null;
    private double[][] ampi = null;
    private double diffusionParameter = 0f;
    private double vc = 0f;
    private double delta = 0f;
    private double xres = 0f;
    private double yres = 0f;
    private double npixel = 0f;
    private final IHMProgressMonitor pm;

    public IUHDiffusionSurface( double[][] _ampi, ParameterBox fixedParameters, IHMProgressMonitor pm ) {
        ampi = _ampi;
        this.pm = pm;
        delta = fixedParameters.getDelta();

        double threshold = 10;
//        int additionalSteps = (int) (threshold / delta);
//        ampiDiffusion = new double[ampi.length + additionalSteps][ampi[0].length];
//
//        for( int i = 0; i < ampi.length; i++ ) {
//            ampiDiffusion[i][0] = ampi[i][0];
//        }
//        for( int i = 1; i < threshold / delta; i++ ) {
//            ampiDiffusion[ampi.length - 1 + i][0] = ampi[ampi.length - 1][0] + i * delta;
//        }
        int additionalSteps = (int) Math.round(threshold / delta);
        ampiDiffusion = new double[ampi.length + additionalSteps][ampi[0].length + 1];

        for( int i = 0; i < ampi.length; i++ ) {
            ampiDiffusion[i][0] = ampi[i][0];
        }
        for( int i = 1; i <= additionalSteps; i++ ) {
            ampiDiffusion[ampi.length - 1 + i][0] = ampi[ampi.length - 1][0] + i * delta;
        }

        diffusionParameter = fixedParameters.getDiffusionparameter();
        vc = fixedParameters.getVc();
        xres = fixedParameters.getXres();
        yres = fixedParameters.getYres();
        npixel = fixedParameters.getNpixel();
    }

    public double[][] calculateIUH() {
        double cum = 0f;
        double t = 0;

//        final SimpsonIntegrator si = new SimpsonIntegrator(IntegralConstants.diffusionaccurancy,
//                IntegralConstants.diffusionaccurancy, 5, IntegralConstants.diffusionmaxsteps);
//        
//        pm.beginTask("Calculating diffusion...", ampiDiffusion.length - 1);
//        for( int i = 0; i < ampiDiffusion.length - 1; i++ ) {
//
//            t = ampiDiffusion[i + 1][0];
//
//            double finalT = t;
//            UnivariateFunction f = x -> {
//                double wfValue = ModelsEngine.widthInterpolate(ampi, x, 0, 1);
//                double result = 1 / sqrt(4 * PI * diffusionParameter * pow(finalT, 3.0f)) * wfValue * x
//                        / (exp(pow((x - finalT), 2) / (4 * diffusionParameter * finalT)));
//                return result;
//            };
//            double integral = si.integrate(IntegralConstants.diffusionmaxsteps, f, 0.0,
//                    ampiDiffusion[ampiDiffusion.length - 1][0]);
//
//            ampiDiffusion[i + 1][3] = integral;
//
//            pm.worked(1);
//        }
//        pm.done();

        /*
         * next part calculates the convolution between the aplitude function and the diffusion
         * equation
         */
        ConvolutionDiffusionWidth diffIntegral = new ConvolutionDiffusionWidth(0.0, ampiDiffusion[ampiDiffusion.length - 1][0],
                IntegralConstants.diffusionmaxsteps, IntegralConstants.diffusionaccurancy, ampi, diffusionParameter, t);
        cum = 0f;
        t = 0;
        pm.beginTask("Calculating diffusion...", ampiDiffusion.length - 1);
        for( int i = 0; i < ampiDiffusion.length - 1; i++ ) {

            t = ampiDiffusion[i + 1][0];

            diffIntegral.updateTime((int) t);
            double integral = diffIntegral.integrate();

            ampiDiffusion[i + 1][1] = integral;
            cum += integral * delta / (xres * yres * npixel * vc);
            ampiDiffusion[i + 1][2] = cum;

            pm.worked(1);
        }
        pm.done();

        double[] xSup = new double[ampiDiffusion.length];
        double[] ySup = new double[ampiDiffusion.length];
        double[] ySub = new double[ampiDiffusion.length];
        for( int i = 0; i < ampiDiffusion.length; i++ ) {
            xSup[i] = ampiDiffusion[i][0];
            ySup[i] = ampiDiffusion[i][1];
        }
        for( int i = 0; i < ampiDiffusion.length; i++ ) {
            ySub[i] = ampiDiffusion[i][3];
        }

        Scatter scatterChart = new Scatter("QReal total");
//      scatterChart.setShowLines(doLines);
//      scatterChart.setShowShapes(doShapes);
        scatterChart.setXLabel("time");
        scatterChart.setYLabel("q");
        scatterChart.addSeries("Superficial", xSup, ySup);
        scatterChart.addSeries("Sub", xSup, ySub);
        PlotFrame pf = new PlotFrame(scatterChart);
        pf.setDimension(2000, 900);
        pf.plot();

        return ampiDiffusion;
    }

}
