package org.hortonmachine.geoframe.core.parameters;

import java.util.List;

import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;

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
	
	public static List<ParameterBounds> calibrationParameterBounds() {
		return java.util.Arrays.asList(
				new ParameterBounds("combinedMeltingFactor", combinedMeltingFactorRange()[0], combinedMeltingFactorRange()[1]),
				new ParameterBounds("freezingFactor", freezingFactorRange()[0], freezingFactorRange()[1]),
				new ParameterBounds("alfa_l", alfaLRange()[0], alfaLRange()[1])
		);
	}
	
}
