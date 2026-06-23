package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical;

import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
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
	@Description("The field of the vector of stations, defining the id.")
	@In
	public String fStationsid = null;

	@Description("The field of the vector of stations, defining the elevation.")
	@In
	public String fStationsZ = null;

	@Description("The progress monitor.")
	@In
	public IHMProgressMonitor pm = new LogProgressMonitor();

	@Description("Include zeros in computations (default is true).")
	@In
	public boolean doIncludeZero = true;

	@Description("The type of theoretical semivariogram: exponential, gaussian, spherical, pentaspherical"
			+ "linear, circular, bessel, periodic, hole, logaritmic, power, spline")
	@In
	public String pSemivariogramType = null;
	@Description("Switch for detrended mode.")
	@In
	public boolean doDetrended;

	@Description("Specified cutoff")
	@In
	public double cutoffInput;

	@Description("Number of bins to consider in the anlysis")
	@In
	public int cutoffDivide;
	@Description("In h")
	@In
	public HashMap<Integer, double[]> h;
	@Description("variogram")
	@Out
	public HashMap<Integer, double[]> out;
	@In
	public boolean doLogarithmic = false;

	// @Out
	private StationsSelection stations;
	@Description("The Experimental Variogram.")
	@In
	public String inTheoreticalVariogramFile;
	@Description("In the case of kriging with neighbor, maxdist is the maximum distance "
			+ "within the algorithm has to consider the stations")

	@In
	public boolean getExperimentalVariogramData;
	@In
	public String fileNoValue = "-9999";

	private VariogramParameters vpGlobal = null;
	private VariogramParameters vpGlobalDetrended = null;

	@Execute
	public void execute() {

		try {
			if (inTheoreticalVariogramFile != null) {

				out = SingleStepVariogramEvaluator.createVariogram(inStations, h, doDetrended, doIncludeZero, doLogarithmic, fStationsid,
						fStationsZ);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static HashMap<Integer, double[]> createVariogram(SimpleFeatureCollection inStations,
			HashMap<Integer, double[]> h, boolean doDetrended, boolean doIncludeZero, boolean doLogarithmic,
			String fStationsid, String fStationsZ) {
		StationsSelection stations = new StationsSelection();
		stations.inStations = inStations;
		stations.doIncludezero = doIncludeZero;
		stations.fStationsid = fStationsid;
		stations.fStationsZ = fStationsZ;
		stations.doLogarithmic = doLogarithmic;

		if (doLogarithmic) {
			h = Utility.getLog(h);
		}
		stations.inData = h;
		VariogramParametersCalculator vpcalCulator = new VariogramParametersCalculator(stations, doDetrended,
				doIncludeZero);

		return vpcalCulator.execute().toHashMap();

	};

}
