package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical;

import java.util.HashMap;

import org.hortonmachine.gears.libs.modules.HMConstants;

/**
 * This class represents the parameters of a theoretical variogram. It includes
 * nugget, range, sill, and model type, along with options for trend and
 * locality.
 */
public class VariogramParameters {
	/** Available theoretical variogram models */
	public static final String[] AVAILABLE_THEORETICAL_VARIOGRAMS = new String[] { "exponential", "linear", "power",
			"spherical", "gaussian" };
	private double nugget;
	private double range;
	private double sill;
	private String modelName;
	private boolean isTrend;
	private boolean isLocal;
	private double trendIntercept = 0;
	private double trendSlope = 0;

	public static class Builder {
		private String modelName;
		private double nugget;
		private double range;
		private double sill;
		private boolean isTrend = false;
		private boolean isLocal = false;
		private double trendIntercept = 0;
		private double trendSlope = 0;

		public Builder(String modelName, double nugget, double range, double sill) {
			this.modelName = modelName;
			this.nugget = nugget;
			this.range = range;
			this.sill = sill;
		}

		public Builder setTrend(boolean isTrend) {
			this.isTrend = isTrend;
			return this;
		}

		public Builder setLocal(boolean isLocal) {
			this.isLocal = isLocal;
			return this;
		}

		public Builder setTrendIntercept(double trendIntercept) {
			this.trendIntercept = trendIntercept;
			return this;
		}

		public Builder setTrendSlope(double trendSlope) {
			this.trendSlope = trendSlope;
			return this;
		}

		public VariogramParameters build() {
			return new VariogramParameters(this);
		}

		public Builder setLocal(double d) {
			// TODO Auto-generated method stub
			this.isLocal = d == 1.0;
			return this;
		}

		public Builder setTrend(double d) {
			this.isTrend = d == 1.0;
			return this;
		}
	}

	private VariogramParameters(Builder builder) {
		this.modelName = builder.modelName;
		this.nugget = builder.nugget;
		this.range = builder.range;
		this.sill = builder.sill;
		this.isTrend = builder.isTrend;
		this.isLocal = builder.isLocal;
		this.trendIntercept = builder.trendIntercept;
		this.trendSlope = builder.trendSlope;
	}

	/**
	 * Gets whether a trend is considered.
	 *
	 * @return true if a trend is used, false otherwise.
	 */
	public boolean getIsTrend() {
		return isTrend;
	}

	/**
	 * Gets whether the variogram is local.
	 *
	 * @return true if local, false otherwise.
	 */
	public boolean getIsLocal() {
		return isLocal;
	}

	public double getNugget() {
		return nugget;
	}

	public double getRange() {
		return range;
	}

	public double getSill() {
		return sill;
	}

	public String getModelName() {
		return modelName;
	}

	public double getIntercept() {
		return this.trendIntercept;
	}

	public double getSlope() {
		return this.trendSlope;
	}

	/**
	 * Converts the variogram parameters into a HashMap.
	 *
	 * @return HashMap containing variogram parameters.
	 */
	public HashMap<Integer, double[]> toHashMap() {
		HashMap<Integer, double[]> outVariogramParams = new HashMap<>();
		if (modelName != null) {
			outVariogramParams.put(0, new double[] { nugget });
			outVariogramParams.put(1, new double[] { sill });
			outVariogramParams.put(2, new double[] { range });
			outVariogramParams.put(3, new double[] { isLocal ? 1.0 : 0.0 });
			outVariogramParams.put(4, new double[] { isTrend ? 1.0 : 0.0 });
			outVariogramParams.put(5, new double[] { getVariogramCode(modelName) });
			outVariogramParams.put(6, new double[] { trendIntercept });
			outVariogramParams.put(7, new double[] { trendSlope });
		} else {
			for (int i = 0; i <= 7; i++) {
				outVariogramParams.put(i, new double[] { HMConstants.doubleNovalue });
			}
		}
		return outVariogramParams;
	}

	/**
	 * Retrieves the variogram code corresponding to a given model name.
	 *
	 * @param name The model name.
	 * @return The variogram code, or -9999 if not found.
	 */
	public static int getVariogramCode(String name) {
		for (int i = 0; i < AVAILABLE_THEORETICAL_VARIOGRAMS.length; i++) {
			if (AVAILABLE_THEORETICAL_VARIOGRAMS[i].equals(name)) {
				return i;
			}
		}
		return -9999;
	}

	/**
	 * Retrieves the variogram type based on an index.
	 *
	 * @param d The index.
	 * @return The corresponding variogram type, or "unknown" if index is out of
	 *         range.
	 */
	public static String getVariogramType(double d) {
		int index = (int) d;
		return VariogramParameters.getVariogramType(index);
	}

	/**
	 * Retrieves the variogram type based on an index.
	 *
	 * @param d The index.
	 * @return The corresponding variogram type, or "unknown" if index is out of
	 *         range.
	 */
	public static String getVariogramType(int index) {
		if (index >= 0 && index < AVAILABLE_THEORETICAL_VARIOGRAMS.length) {
			return AVAILABLE_THEORETICAL_VARIOGRAMS[index];
		}
		return "unknown";
	}

	/**
	 * Checks if the variogram parameters are valid.
	 *
	 * @return true if valid, false otherwise.
	 */

	public boolean isValid() {
		return nugget >= 0 && range >= 0 && sill >= 0 && modelName != null;
	}

}
