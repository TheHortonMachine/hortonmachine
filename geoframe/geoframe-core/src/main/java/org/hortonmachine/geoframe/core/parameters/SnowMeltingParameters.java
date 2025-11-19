package org.hortonmachine.geoframe.core.parameters;

/**
 * Parameters for the snow melting model that need to be calibrated.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record SnowMeltingParameters(
	/**
	 * Melting factor [mm/°C/day]
	 */
	double combinedMeltingFactor,
	/**
	 * Freezing factor [mm/°C/day]
	 */
	double freezingFactor,
	/**
	 * Coefficient for the computation of the maximum liquid water [-]
	 */
	double alfa_l
)
{
	public static final SnowMeltingParameters CALIBRATION_DEFAULT = new SnowMeltingParameters(1.5, 0.8, 0.2);
	
	public static double[] combinedMeltingFactorRange() {
		return new double[] {0.0001, 2.0};
	}
	
	public static double[] freezingFactorRange() {
		return new double[] {0.0001, 1.0};
	}
	
	public static double[] alfaLRange() {
		return new double[] {0.001, 0.5};
	}
	
}
