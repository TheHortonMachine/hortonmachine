package org.hortonmachine.geoframe.core.parameters;

/**
 * Parameters for the water budget canopy model that need to be calibrated.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record WaterBudgetCanopyParameters(
	/**
	 * Coefficient canopy out [-]
	 */
	double kc,
	/**
	 * Partitioning coefficient free throughfall [-]
	 */
	double p
)
{
	public static final WaterBudgetCanopyParameters CALIBRATION_DEFAULT = new WaterBudgetCanopyParameters(0.6, 0.4);
	
	public static double[] kcRange() {
		return new double[] {0.1, 0.9};
	}
	
	public static double[] pRange() {
		return new double[] {0.5, 0.98};
	}
}
