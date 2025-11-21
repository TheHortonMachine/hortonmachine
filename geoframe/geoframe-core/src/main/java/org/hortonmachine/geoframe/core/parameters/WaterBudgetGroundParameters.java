package org.hortonmachine.geoframe.core.parameters;

import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;

/**
 * Parameters for the water budget ground model that need to be calibrated.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record WaterBudgetGroundParameters(
	/**
	 * Maximum groundwater storage [mm]
	 */
	double s_GroundWaterMax,
	/**
	 * Coefficient of the non-linear reservoir model [-]
	 */
	double e,
	/**
	 * Exponent of the non-linear reservoir model [-]
	 */
	double f
	
)
{
	public static final WaterBudgetGroundParameters CALIBRATION_DEFAULT = new WaterBudgetGroundParameters(1000.0, 0.002, 1.0);

	public static double[] sGroundWaterMaxRange() {
		return new double[] {100.0, 1000.0};
	}
	
	public static double[] eRange() {
		return new double[] {0.0000005, 0.02};
	}
	
	public static double[] fRange() {
		return new double[] {1.0, 3.0};
	}
	
	public static List<ParameterBounds> calibrationParameterBounds() {
		return Arrays.asList(
				new ParameterBounds("s_GroundWaterMax", sGroundWaterMaxRange()[0], sGroundWaterMaxRange()[1]),
				new ParameterBounds("e", eRange()[0], eRange()[1]),
				new ParameterBounds("f", fRange()[0], fRange()[1])
		);
	}
}
