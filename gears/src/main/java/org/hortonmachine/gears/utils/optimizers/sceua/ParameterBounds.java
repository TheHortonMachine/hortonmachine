package org.hortonmachine.gears.utils.optimizers.sceua;

/**
 * Defines the allowed range for a calibration parameter.
 */
public final class ParameterBounds {

	private final String name;
	private final double lower;
	private final double upper;

	/**
	 * @param name  logical name of the parameter (for reporting only)
	 * @param lower lower bound (inclusive)
	 * @param upper upper bound (inclusive)
	 */
	public ParameterBounds(String name, double lower, double upper) {
		if (name == null || name.isEmpty()) {
			throw new IllegalArgumentException("Parameter name must not be empty.");
		}
		if (Double.isNaN(lower) || Double.isNaN(upper)) {
			throw new IllegalArgumentException("Bounds must be finite numbers.");
		}
		if (upper <= lower) {
			throw new IllegalArgumentException("Upper bound must be > lower bound for " + name);
		}
		this.name = name;
		this.lower = lower;
		this.upper = upper;
	}

	public String getName() {
		return name;
	}

	public double getLower() {
		return lower;
	}

	public double getUpper() {
		return upper;
	}

	/**
	 * Maps a normalized value in [0,1] to this parameter range.
	 */
	public double denormalize(double u) {
		return lower + u * (upper - lower);
	}

	/**
	 * Maps a value in [lower,upper] to [0,1].
	 */
	public double normalize(double value) {
		return (value - lower) / (upper - lower);
	}
}
