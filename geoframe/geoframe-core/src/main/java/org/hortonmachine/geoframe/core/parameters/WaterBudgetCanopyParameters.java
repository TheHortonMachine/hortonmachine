package org.hortonmachine.geoframe.core.parameters;

import java.util.Arrays;
import java.util.List;

import org.hortonmachine.gears.utils.optimizers.sceua.ParameterBounds;

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
	
	public static List<ParameterBounds> calibrationParameterBounds() {
		return Arrays.asList(
				new ParameterBounds("kc", kcRange()[0], kcRange()[1]),
				new ParameterBounds("p", pRange()[0], pRange()[1])
		);
	}
}
