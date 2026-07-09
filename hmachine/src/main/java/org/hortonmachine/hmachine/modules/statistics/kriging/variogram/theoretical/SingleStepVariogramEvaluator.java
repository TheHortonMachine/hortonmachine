package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationsSelection;
import org.hortonmachine.hmachine.modules.statistics.kriging.utilities.Utility;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

//TODO temporary class to fix
public class SingleStepVariogramEvaluator {
	@Description("The .shp of the measurement point, containing the position of the stations.")
	@In
	public SimpleFeatureCollection inStations = null;

	@Description("The progress monitor.")
	@In
	public IHMProgressMonitor pm = new LogProgressMonitor();

	@Description("Include zeros in computations (default is true).")
	@In
	public boolean doIncludeZero = true;

	@Description("Include zeros in computations (default is true).")
	@In
	public int cutoffDivide = 0;

	@Description("Switch for detrended mode.")
	@In
	public boolean doDetrended;

	@Description("Specified cutoff")
	@In
	public double cutoffInput = 0.0;

	@Description("In h")
	@In
	public HashMap<Integer, double[]> h;
	@Description("variogram")
	@Out
	public HashMap<Integer, double[]> out;
	@In
	public boolean doLogarithmic = false;

	// @In
	private StationsSelection stations;

	// @In
	private String variogramType;

	@Execute
	public void execute() {

		try {

			out = SingleStepVariogramEvaluator.createVariogram(stations, h, doDetrended, doIncludeZero, doLogarithmic,
					variogramType, cutoffDivide, cutoffInput);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static HashMap<Integer, double[]> createVariogram(StationsSelection stations, HashMap<Integer, double[]> h,
			boolean doDetrended, boolean doIncludeZero, boolean doLogarithmic, String semivariogramType,
			int cutoffDivide, double cutoffInput) {

		if (doLogarithmic) {
			h = Utility.getLog(h);
		}
		stations.inData = h;
		VariogramParametersCalculator vpcalCulator = new VariogramParametersCalculator(stations, doDetrended,
				doIncludeZero);
		vpcalCulator.setCutOffInput(cutoffInput);
		vpcalCulator.setCutOffdivide(cutoffDivide);
		vpcalCulator.setType(semivariogramType);

		return vpcalCulator.execute().toHashMap();

	};

}
