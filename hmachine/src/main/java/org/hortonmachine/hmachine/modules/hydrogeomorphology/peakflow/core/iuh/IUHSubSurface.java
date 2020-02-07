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
import org.hortonmachine.gears.utils.math.integration.ConvolutionExponentialPeakflow;
import org.hortonmachine.gears.utils.math.integration.IntegralConstants;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;

/**
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvano Pisoni
 */
public class IUHSubSurface {

	private double[][] ampiSubSup = null;
	private double[][] ampiHelp = null;
	private double vc = 0f;
	private double delta_sub = 0f;
	private double xres = 0f;
	private double yres = 0f;
	private double npixel_sub = 0f;
	private double resid_time = 0f;
	private final IHMProgressMonitor pm;
	private double diffusionParameterSubSup;

	public IUHSubSurface(double[][] ampi, ParameterBox fixedParameters, IHMProgressMonitor pm) {
		ampiHelp = ampi;
		this.pm = pm;
		ampiSubSup = new double[ampiHelp.length][ampiHelp[0].length];

		for (int i = 0; i < ampiHelp.length; i++) {
			ampiSubSup[i][0] = ampiHelp[i][0];
		}

		diffusionParameterSubSup = fixedParameters.getDiffusionParameterSubSup();
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
		 * next part calculates the convolution between the aplitude function and the
		 * exponential equation
		 */
		double upperIntegrationLimit = ampiSubSup[ampiSubSup.length - 1][0];
		ConvolutionDiffusionWidth diffIntegral = new ConvolutionDiffusionWidth(0.0, upperIntegrationLimit,
				IntegralConstants.diffusionSubSupMaxsteps, IntegralConstants.diffusionSubSupAccurancy, ampiHelp,
				diffusionParameterSubSup, t);

		pm.beginTask("Calculating subsurface IUH...", ampiHelp.length);
		for (int i = 0; i < ampiHelp.length; i++) {
			t = ampiSubSup[i][0];

			// ConvolutionExponentialPeakflow expIntegral = new
			// ConvolutionExponentialPeakflow(0.0,
			// upperIntegrationLimit, 20, 0.00001, ampiHelp, resid_time, t);

			diffIntegral.updateTime((int) t);
			integral = diffIntegral.integrate();
			ampiSubSup[i][1] = integral;
			/*
			 * if (isScs) { cum += integral delta_sub / (xres yres npixel_sub vc / vcvv); }
			 * else {
			 */
			cum += integral * delta_sub / (xres * yres * npixel_sub * vc);

			ampiSubSup[i][2] = cum;

			pm.worked(1);
		}

		double maxCum = ampiSubSup[ampiSubSup.length - 1][2];
		double factor = 1.0 / maxCum;

		for (int i = 0; i < ampiSubSup.length; i++) {

			ampiSubSup[i][1] = ampiSubSup[i][1] * factor;
			ampiSubSup[i][2] = ampiSubSup[i][2] * factor;

			pm.worked(1);
		}

		pm.done();

		return ampiSubSup;
	}
}
