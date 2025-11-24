package org.hortonmachine.geoframe.core.parameters;

import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;

/**
 * Parameters for the rain/snow separation model that need to be calibrated.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record RainSnowSeparationParameters(
	/**
	 * Smoothing degree parameter [-]
	 */
	double m1, // TODO this might not go into calibration
	/**
	 * Adjustment coefficient for rain measurements [-]
	 */
	double alfa_r, 
	/**
	 * Adjustment coefficient for snow measurements [-]
	 */
	double alfa_s,
	/**
	 * Melting temperature [°C]
	 */
	double meltingTemperature
)
{
	public static final RainSnowSeparationParameters CALIBRATION_DEFAULT = new RainSnowSeparationParameters(1.0, 1.0, 1.0, 0.0);
	
	public static double[] alphaRRange() {
		return new double[] {0.8, 1.5};
	}
	
	public static double[] alphaSRange() {
		return new double[] {0.8, 1.5};
	}
	
	public static double[] meltingTemperatureRange() {
		return new double[] {-1.0, 3.0};
	}
	
	public static List<ParameterBounds> calibrationParameterBounds() {
		return Arrays.asList(
				new ParameterBounds("alfa_r", alphaRRange()[0], alphaRRange()[1]),
				new ParameterBounds("alfa_s", alphaSRange()[0], alphaSRange()[1]),
				new ParameterBounds("meltingTemperature", meltingTemperatureRange()[0], meltingTemperatureRange()[1])
		);
	}
	
}
