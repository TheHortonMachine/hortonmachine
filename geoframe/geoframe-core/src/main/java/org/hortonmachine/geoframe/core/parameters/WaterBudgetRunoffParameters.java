package org.hortonmachine.geoframe.core.parameters;

/**
 * Parameters for the water budget runoff model that need to be calibrated.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record WaterBudgetRunoffParameters(
	/**
	 * Maximum runoff storage [mm]
	 */
	double sRunoffMax,
	/**
	 * Coefficient of the non-linear reservoir model [-]
	 */
	double c,
	/**
	 * Exponent of the non-linear reservoir model [-]
	 */
	double d
)
{
	public static final WaterBudgetRunoffParameters CALIBRATION_DEFAULT = new WaterBudgetRunoffParameters(60.0, 0.4, 2.0);

	public static double[] sRunoffMaxRange() {
		return new double[] {5.0, 100.0};
	}
	
	public static double[] cRange() {
		return new double[] {0.000001, 0.6};
	}
	
	public static double[] dRange() {
		return new double[] {1.0, 3.0};
	}
}
