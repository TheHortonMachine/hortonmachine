package org.hortonmachine.geoframe.core.parameters;

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
}
