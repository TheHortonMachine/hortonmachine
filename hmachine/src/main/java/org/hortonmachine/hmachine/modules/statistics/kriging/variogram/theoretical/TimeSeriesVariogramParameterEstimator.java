package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.ResidualsEvaluator;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationsSelection;
import org.hortonmachine.hmachine.modules.statistics.kriging.utilities.Utility;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.experimental.ExperimentalVariogram;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;

public class TimeSeriesVariogramParameterEstimator {
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
	public double cutoffInput = 0;

	@Description("Number of bins to consider in the anlysis")
	@In
	public int cutoffDivide = 0;
	@Description("Distances input path")
	@In
	public String inHValuesPath;
	@In
	public String tStart = null;
	@In
	public String tEnd = null;
	@In
	public int tTimeStep = 60;
	/** transform to log. */
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
		verifyInput();

		stations = new StationsSelection();
		stations.inStations = inStations;
		stations.doIncludezero = doIncludeZero;
		stations.fStationsid = fStationsid;
		stations.fStationsZ = fStationsZ;
		stations.doLogarithmic = doLogarithmic;

		try {
			this.createDefaulParams(stations);
			if (inTheoreticalVariogramFile != null) {

				this.createFile(stations);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void createFile(StationsSelection stations2) {
		// TODO Auto-generated method stub
		OmsTimeSeriesIteratorWriter parameterWriter = getWriter();
		OmsTimeSeriesIteratorWriter distanceWriter = null;
		OmsTimeSeriesIteratorWriter hwriter = null;
		OmsTimeSeriesIteratorWriter nwriter = null;

		OmsTimeSeriesIteratorReader readH = getReader();
		if (getExperimentalVariogramData) {
			distanceWriter = getDistanceWriter();
			hwriter = getHWriter();
			nwriter = getNWriter();
		}

		try {
			while (readH.doProcess) {

				readH.nextRecord();
				HashMap<Integer, double[]> h = readH.outData;
				if (doLogarithmic) {
					h = Utility.getLog(h);
				}
				stations.inData = h;
				VariogramParametersCalculator vpcalCulator = new VariogramParametersCalculator(stations, doDetrended,
						doIncludeZero);
				vpcalCulator.setGlobalVp(vpGlobal);
				vpcalCulator.setGlobalDeTrendedVp(vpGlobalDetrended);
				vpcalCulator.setCutOffdivide(cutoffDivide);
				vpcalCulator.setCutOffInput(cutoffInput);
				vpcalCulator.setType(pSemivariogramType);

				HashMap<Integer, double[]> out = vpcalCulator.execute().toHashMap();

				parameterWriter.inData = out;
				parameterWriter.writeNextLine();

				if (getExperimentalVariogramData) {
					distanceWriter.inData = vpcalCulator.getDD();

					distanceWriter.writeNextLine();
					hwriter.inData = vpcalCulator.getHH();
					hwriter.writeNextLine();
					nwriter.inData = vpcalCulator.getNN();
					nwriter.writeNextLine();
				}

			}
			parameterWriter.close();
			if (getExperimentalVariogramData) {
				distanceWriter.close();

				hwriter.close();

				nwriter.close();

			}
			readH.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private OmsTimeSeriesIteratorWriter getWriter() {
		OmsTimeSeriesIteratorWriter parameterWriter = new OmsTimeSeriesIteratorWriter();
		parameterWriter.file = this.inTheoreticalVariogramFile;
		parameterWriter.fileNovalue = fileNoValue;
		parameterWriter.tStart = tStart;
		parameterWriter.tTimestep = tTimeStep;
		return parameterWriter;
	}

	private String getParentPath() {
		String fullFileName = this.inTheoreticalVariogramFile;
		File file = new File(fullFileName);
		return file.getParent();
	}

	private OmsTimeSeriesIteratorWriter getDistanceWriter() {
		OmsTimeSeriesIteratorWriter parameterWriter = new OmsTimeSeriesIteratorWriter();
		parameterWriter.file = getParentPath() + "/distance.csv";
		parameterWriter.fileNovalue = fileNoValue;
		parameterWriter.tStart = tStart;
		parameterWriter.tTimestep = tTimeStep;
		return parameterWriter;
	}

	private OmsTimeSeriesIteratorWriter getHWriter() {
		OmsTimeSeriesIteratorWriter parameterWriter = new OmsTimeSeriesIteratorWriter();
		parameterWriter.file = getParentPath() + "/h.csv";
		parameterWriter.fileNovalue = fileNoValue;
		parameterWriter.tStart = tStart;
		parameterWriter.tTimestep = tTimeStep;
		return parameterWriter;
	}

	private OmsTimeSeriesIteratorWriter getNWriter() {
		OmsTimeSeriesIteratorWriter parameterWriter = new OmsTimeSeriesIteratorWriter();
		parameterWriter.file = getParentPath() + "/n.csv";
		parameterWriter.fileNovalue = fileNoValue;
		parameterWriter.tStart = tStart;
		parameterWriter.tTimestep = tTimeStep;
		return parameterWriter;
	}

	private void verifyInput() {
		if (doDetrended) {
			int ff = inStations.getSchema().indexOf(fStationsZ);

			if (ff < 0) {
				throw new NullPointerException("check if the z field name is correct");
			}
		}
	}

	private void createDefaulParams(StationsSelection stations) throws Exception {

		System.out.println("evaluate global params");
		double[] variance = new double[cutoffDivide];
		double[] distance = new double[cutoffDivide];
		double[] n = new double[cutoffDivide];

		double[] varianceDeTrended = new double[cutoffDivide];
		double[] distanceDeTrended = new double[cutoffDivide];
		double[] nDeTrended = new double[cutoffDivide];
		double intecept = 0;
		double slope = 0;

		OmsTimeSeriesIteratorReader readH = getReader();
		ExperimentalVariogram exp = ExperimentalVariogram.create(fStationsid, inStations, doIncludeZero, cutoffDivide,
				cutoffInput, 0);
		int nRows = 0;
		int nRowsDeTrended = 0;

		try {
			while (readH.doProcess) {

				readH.nextRecord();
				HashMap<Integer, double[]> h = readH.outData;
				if (doLogarithmic) {
					h = Utility.getLog(h);
				}
				exp.inData = h;
				exp.process();
				if (!exp.areAllEquals && exp.differents > 2) {
					HashMap<Integer, double[]> d = exp.outDistances;
					HashMap<Integer, double[]> v = exp.outExperimentalVariogram;
					int j = 0;
					HashMap<Integer, double[]> nTmp = exp.outNumberPairsPerBin;

					for (Map.Entry<Integer, double[]> tt : d.entrySet()) {
						distance[j] = distance[j] + tt.getValue()[0];
						variance[j] = variance[j] + v.get(tt.getKey())[0];
						n[j] = n[j] + nTmp.get(tt.getKey())[0];

						j = j + 1;
					}
					if (doDetrended) {

						stations.inData = h;
						stations.execute();
						int n1 = stations.hStationInitialSet.length - 1;

						ResidualsEvaluator rEvaluator = new ResidualsEvaluator();
						rEvaluator.doDetrended = true;
						rEvaluator.hStations = Arrays.copyOfRange(stations.hStationInitialSet, 0, n1);
						rEvaluator.zStations = Arrays.copyOfRange(stations.zStationInitialSet, 0, n1);

						rEvaluator.process();
						if (rEvaluator.isPValueOk) {
							double[] hVal = rEvaluator.hResiduals;
							nRowsDeTrended = nRowsDeTrended + 1;
							HashMap<Integer, double[]> hRes = new HashMap<>();
							for (int i = 0; i < n1; i++) {
								hRes.put(stations.idStationInitialSet[i], new double[] { hVal[i] });
							}
							exp.inData = hRes;
							exp.process();
							if (exp.differents > 2) {
								d = exp.outDistances;
								v = exp.outExperimentalVariogram;
								HashMap<Integer, double[]> nDeTrendedTmp = exp.outNumberPairsPerBin;
								j = 0;
								for (Map.Entry<Integer, double[]> tt : d.entrySet()) {
									distanceDeTrended[j] = distanceDeTrended[j] + tt.getValue()[0];
									varianceDeTrended[j] = varianceDeTrended[j] + v.get(tt.getKey())[0];
									nDeTrended[j] = nDeTrended[j] + nDeTrendedTmp.get(tt.getKey())[0];
									slope = slope + rEvaluator.trendCoefficient;
									intecept = intecept + rEvaluator.trendIntercept;
									j = j + 1;
								}
							}
						}
					}
					nRows = nRows + 1;
				}

			}
			readH.close();
			if (distance.length != variance.length) {
				throw new IllegalArgumentException();
			}
			for (int i = 0; i < distance.length; i++) {
				distance[i] = distance[i] / nRows;
				variance[i] = variance[i] / nRows;
				n[i] = n[i] / nRows;
			}
			SemivariogramParameterFitter vEvaluator = new SemivariogramParameterFitter();
			vEvaluator.pSemivariogramType = pSemivariogramType;
			vEvaluator.x = distance;
			vEvaluator.y = variance;
			vEvaluator.n = n;

			vEvaluator.proces();
			vpGlobal = new VariogramParameters.Builder(vEvaluator.outSemivariogramType, vEvaluator.nugget,
					vEvaluator.range, vEvaluator.sill).setLocal(false).setTrend(false).setTrendIntercept(0)
					.setTrendSlope(0).build();
			pm.message("Global value for nugget: " + vpGlobal.getNugget() + " sill:" + vpGlobal.getSill() + " range: "
					+ vpGlobal.getRange() + "  semivariogram type:" + vpGlobal.getModelName());

			if (!vEvaluator.isFitGood) {
				pm.message("warning the fit is not good!");
			}

			if (doDetrended) {
				if (distanceDeTrended.length != varianceDeTrended.length) {
					throw new IllegalArgumentException();
				}

				if (nRowsDeTrended > 0) {
					for (int i = 0; i < distanceDeTrended.length; i++) {
						distanceDeTrended[i] = distanceDeTrended[i] / nRowsDeTrended;
						varianceDeTrended[i] = varianceDeTrended[i] / nRowsDeTrended;
						nDeTrended[i] = nDeTrended[i] / nRowsDeTrended;
					}
					vEvaluator = new SemivariogramParameterFitter();
					vEvaluator.pSemivariogramType = pSemivariogramType;
					vEvaluator.x = distanceDeTrended;
					vEvaluator.y = varianceDeTrended;
					vEvaluator.n = nDeTrended;
					vEvaluator.proces();
					vpGlobalDetrended = new VariogramParameters.Builder(vEvaluator.outSemivariogramType,
							vEvaluator.nugget, vEvaluator.range, vEvaluator.sill).setLocal(false).setTrend(true)
							.setTrendIntercept(intecept / nRows).setTrendSlope(slope / nRows).build();

					pm.message("Global value with TREND for nugget: " + vpGlobalDetrended.getNugget() + " sill:"
							+ vpGlobalDetrended.getSill() + " range: " + vpGlobalDetrended.getRange()
							+ "  semivariogram type:" + vpGlobalDetrended.getModelName());
					if (!vEvaluator.isFitGood) {
						pm.message("warning the fit is not good!");
					}
				} else {
					pm.message("no trend has been found, so no parameters has been evauate");
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private OmsTimeSeriesIteratorReader getReader() {
		OmsTimeSeriesIteratorReader readH = new OmsTimeSeriesIteratorReader();
		readH.file = this.inHValuesPath;
		readH.idfield = "ID";
		readH.fileNovalue = fileNoValue;
		readH.tStart = tStart;
		readH.tTimestep = tTimeStep;
		if (this.tEnd != null) {
			readH.tEnd = this.tEnd;
		}
		readH.initProcess();
		return readH;
	}

	public VariogramParameters getGlobalVariogramParameters() {
		// TODO Auto-generated method stub
		return vpGlobal;
	}

	public VariogramParameters getGlobalVariogramParametersDeTrended() {
		// TODO Auto-generated method stub
		return vpGlobalDetrended;
	}
}
