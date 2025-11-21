package org.hortonmachine.geoframe.core.parameters;

import java.util.List;

import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;

/**
 * Parameters for the water budget rootzone model that need to be calibrated.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record WaterBudgetRootzoneParameters(
		/**
		 * Maximum value of the rootzone water storage [mm]
		 */
		double s_RootZoneMax,
		/**
		 * Maximum percolation rate [-]
		 */
		double g,
		/**
		 * Exponential of non-linear reservoir model [-]
		 */
		double h,
		/**
		 * Degree of spatial variability of the soil moisture capacity [-]
		 */
		double pB_soil
)
{
	public static final WaterBudgetRootzoneParameters CALIBRATION_DEFAULT = new WaterBudgetRootzoneParameters(150.0, 0.05, 1.5, 2.0);

	public static double[] sRootZoneMaxRange() {
		return new double[] {40.0, 150.0};
	}
	
	public static double[] gRange() {
		return new double[] {0.000001, 0.3};
	}
	
	public static double[] hRange() {
		return new double[] {1.0, 3.0};
	}
	
	public static double[] pBSoilRange() {
		return new double[] {0.5, 3.0};
	}
	
	public static List<ParameterBounds> calibrationParameterBounds() {
		return java.util.Arrays.asList(
				new ParameterBounds("s_RootZoneMax", sRootZoneMaxRange()[0], sRootZoneMaxRange()[1]),
				new ParameterBounds("g", gRange()[0], gRange()[1]),
				new ParameterBounds("h", hRange()[0], hRange()[1]),
				new ParameterBounds("pB_soil", pBSoilRange()[0], pBSoilRange()[1])
		);
	}
}
