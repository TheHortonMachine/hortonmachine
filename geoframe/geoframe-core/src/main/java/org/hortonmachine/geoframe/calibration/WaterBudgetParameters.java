package org.hortonmachine.geoframe.calibration;

public class WaterBudgetParameters {
	public RainSnowSeparation rainSnowSeparation = new RainSnowSeparation();
	public SnowMeltingParameters snowMelting = new SnowMeltingParameters();
	public WaterBudgetCanopyParameters waterBudgetCanopy = new WaterBudgetCanopyParameters();
	public WaterBudgetRootzoneParameters waterBudgetRootzone = new WaterBudgetRootzoneParameters();
	public WaterBudgetRunoffParameters waterBudgetRunoff = new WaterBudgetRunoffParameters();
	public WaterBudgetGroundParameters waterBudgetGround = new WaterBudgetGroundParameters();
	
	
	public double[] toParameterArray() {
		return new double[] {
				rainSnowSeparation.alfa_r,
				rainSnowSeparation.alfa_s,
				rainSnowSeparation.meltingTemperature,
				snowMelting.combinedMeltingFactor,
				snowMelting.freezingFactor,
				snowMelting.alfa_l,
				waterBudgetCanopy.kc,
				waterBudgetCanopy.p,
				waterBudgetRootzone.s_RootZoneMax,
				waterBudgetRootzone.g,
				waterBudgetRootzone.h,
				waterBudgetRootzone.pB_soil,
				waterBudgetRunoff.sRunoffMax,
				waterBudgetRunoff.c,
				waterBudgetRunoff.d,
				waterBudgetGround.s_GroundWaterMax,
				waterBudgetGround.e,
				waterBudgetGround.f
		};
	}
	
	public static WaterBudgetParameters fromParameterArray( double[] params ) {
		WaterBudgetParameters wbp = new WaterBudgetParameters();
		int i = 0;
		wbp.rainSnowSeparation.alfa_r = params[i++];
		wbp.rainSnowSeparation.alfa_s = params[i++];
		wbp.rainSnowSeparation.meltingTemperature = params[i++];
		wbp.snowMelting.combinedMeltingFactor = params[i++];
		wbp.snowMelting.freezingFactor = params[i++];
		wbp.snowMelting.alfa_l = params[i++];
		wbp.waterBudgetCanopy.kc = params[i++];
		wbp.waterBudgetCanopy.p = params[i++];
		wbp.waterBudgetRootzone.s_RootZoneMax = params[i++];
		wbp.waterBudgetRootzone.g = params[i++];
		wbp.waterBudgetRootzone.h = params[i++];
		wbp.waterBudgetRootzone.pB_soil = params[i++];
		wbp.waterBudgetRunoff.sRunoffMax = params[i++];
		wbp.waterBudgetRunoff.c = params[i++];
		wbp.waterBudgetRunoff.d = params[i++];
		wbp.waterBudgetGround.s_GroundWaterMax = params[i++];
		wbp.waterBudgetGround.e = params[i++];
		wbp.waterBudgetGround.f = params[i++];
		return wbp;
	}
	
	
	

	public static class RainSnowSeparation {
		/**
		 * Smoothing degree parameter [-]
		 */
		public double m1 = 1.0;
		/**
		 * Adjustment coefficient for rain measurements [-]
		 */
		public double alfa_r = 1.0;
		/**
		 * Adjustment coefficient for snow measurements [-]
		 */
		public double alfa_s = 1.0;
		/**
		 * Melting temperature [°C]
		 */
		public double meltingTemperature = 0.0;

		public static double[] alphaRRange() {
			return new double[] { 0.8, 1.5 };
		}

		public static double[] alphaSRange() {
			return new double[] { 0.8, 1.5 };
		}

		public static double[] meltingTemperatureRange() {
			return new double[] { -1.0, 3.0 };
		}
	}

	public static class SnowMeltingParameters {
		/**
		 * Melting factor [mm/°C/day]
		 */
		public double combinedMeltingFactor = 1.5;
		/**
		 * Freezing factor [mm/°C/day]
		 */
		public double freezingFactor = 0.8;
		/**
		 * Coefficient for the computation of the maximum liquid water [-]
		 */
		public double alfa_l = 0.2;

		public static double[] combinedMeltingFactorRange() {
			return new double[] { 0.0001, 2.0 };
		}

		public static double[] freezingFactorRange() {
			return new double[] { 0.0001, 1.0 };
		}

		public static double[] alfaLRange() {
			return new double[] { 0.001, 0.5 };
		}
	}

	public static class WaterBudgetCanopyParameters {
		/**
		 * Coefficient canopy out [-]
		 */
		public double kc = 0.6;
		/**
		 * Partitioning coefficient free throughfall [-]
		 */
		public double p = 0.4;

		public static double[] kcRange() {
			return new double[] { 0.1, 0.9 };
		}

		public static double[] pRange() {
			return new double[] { 0.5, 0.98 };
		}

	}

	public static class WaterBudgetRootzoneParameters {
		/**
		 * Maximum value of the rootzone water storage [mm]
		 */
		public double s_RootZoneMax = 150.0;
		/**
		 * Maximum percolation rate [-]
		 */
		public double g = 0.05;
		/**
		 * Exponential of non-linear reservoir model [-]
		 */
		public double h = 1.5;
		/**
		 * Degree of spatial variability of the soil moisture capacity [-]
		 */
		public double pB_soil = 2.0;

		public static double[] sRootZoneMaxRange() {
			return new double[] { 40.0, 250.0 };
		}

		public static double[] gRange() {
			return new double[] { 0.000001, 3 };
		}

		public static double[] hRange() {
			return new double[] { 0.8, 1.0 };
		}

		public static double[] pBSoilRange() {
			return new double[] { 0.5, 3.0 };
		}

	}

	public static class WaterBudgetRunoffParameters {
		/**
		 * Maximum runoff storage [mm]
		 */
		public double sRunoffMax = 60.0;
		/**
		 * Coefficient of the non-linear reservoir model [-]
		 */
		public double c = 0.4;
		/**
		 * Exponent of the non-linear reservoir model [-]
		 */
		public double d = 2.0;

		public static double[] sRunoffMaxRange() {
			return new double[] { 5.0, 100.0 };
		}

		public static double[] cRange() {
			return new double[] { 0.000001, 5 };
		}

		public static double[] dRange() {
			return new double[] { 0.9, 1.0 };
		}

	}

	public static class WaterBudgetGroundParameters {
		/**
		 * Maximum groundwater storage [mm]
		 */
		public double s_GroundWaterMax = 1000.0;
		/**
		 * Coefficient of the non-linear reservoir model [-]
		 */
		public double e = 0.002;
		/**
		 * Exponent of the non-linear reservoir model [-]
		 */
		public double f = 1.0;

		public static double[] sGroundWaterMaxRange() {
			return new double[] { 100.0, 1000.0 };
		}

		public static double[] eRange() {
			return new double[] { 0.0000005, 2 };
		}

		public static double[] fRange() {
			return new double[] { 0.95, 1.0 };
		}
	}
	
	public static class Builder {
		private WaterBudgetParameters params = new WaterBudgetParameters();
		
		public Builder setRainSnowSeparation(double alfa_r, double alfa_s, double meltingTemperature) {
			params.rainSnowSeparation.alfa_r = alfa_r;
			params.rainSnowSeparation.alfa_s = alfa_s;
			params.rainSnowSeparation.meltingTemperature = meltingTemperature;
			return this;
		}
		
		public Builder setSnowMelting(double combinedMeltingFactor, double freezingFactor, double alfa_l) {
			params.snowMelting.combinedMeltingFactor = combinedMeltingFactor;
			params.snowMelting.freezingFactor = freezingFactor;
			params.snowMelting.alfa_l = alfa_l;
			return this;
		}
		
		public Builder setWaterBudgetCanopy(double kc, double p) {
			params.waterBudgetCanopy.kc = kc;
			params.waterBudgetCanopy.p = p;
			return this;
		}
		
		public Builder setWaterBudgetRootzone(double s_RootZoneMax, double g, double h, double pB_soil) {
			params.waterBudgetRootzone.s_RootZoneMax = s_RootZoneMax;
			params.waterBudgetRootzone.g = g;
			params.waterBudgetRootzone.h = h;
			params.waterBudgetRootzone.pB_soil = pB_soil;
			return this;
		}
		
		public Builder setWaterBudgetRunoff(double sRunoffMax, double c, double d) {
			params.waterBudgetRunoff.sRunoffMax = sRunoffMax;
			params.waterBudgetRunoff.c = c;
			params.waterBudgetRunoff.d = d;
			return this;
		}
		
		public Builder setWaterBudgetGround(double s_GroundWaterMax, double e, double f) {
			params.waterBudgetGround.s_GroundWaterMax = s_GroundWaterMax;
			params.waterBudgetGround.e = e;
			params.waterBudgetGround.f = f;
			return this;
		}
		
		public WaterBudgetParameters build() {
			return params;
		}
	}

}
