package org.hortonmachine.geoframe.core.parameters;

import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;

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
		return new double[] {0.000001, 5};
	}
	
	public static double[] dRange() {
		return new double[] {0.9, 1.0};
	}
	
	public static List<ParameterBounds> calibrationParameterBounds() {
		return Arrays.asList(
				new ParameterBounds("sRunoffMax", sRunoffMaxRange()[0], sRunoffMaxRange()[1]),
				new ParameterBounds("c", cRange()[0], cRange()[1]),
				new ParameterBounds("d", dRange()[0], dRange()[1])
		);
	}
}
