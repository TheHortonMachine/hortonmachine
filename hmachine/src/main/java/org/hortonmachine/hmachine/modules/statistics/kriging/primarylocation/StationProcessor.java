package org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation;

import java.util.HashMap;

import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.VariogramParameters;
import org.locationtech.jts.geom.Coordinate;

public class StationProcessor {
	private StationsSelection stations;
	private double[] xStations;
	private double[] yStations;
	private double[] zStations;
	private double[] hStations;
	private double[] hResiduals;
	private int count;
	private boolean areAllEquals;
	private VariogramParameters vp;

	public StationProcessor(StationsSelection stations, VariogramParameters vp) {
		this.vp = vp;
		this.stations = stations;
	}

	/**
	 * Execute station selection for the given coordinate.
	 */
	public void updateForCoordinate(Coordinate coordinate, HashMap<Integer, double[]> inData, int inNumCloserStations,
			double maxdist) throws Exception {

		if (coordinate != null && (maxdist > 0 || inNumCloserStations > 0)) {
			stations.idx = coordinate.x;
			stations.idy = coordinate.y;
			if (inNumCloserStations > 0) {
				stations.inNumCloserStations = inNumCloserStations;
			}
			if (maxdist > 0) {
				stations.maxdist = maxdist;
			}
		}
		stations.inData = inData;
		stations.execute();

		this.xStations = stations.xStationInitialSet;
		this.yStations = stations.yStationInitialSet;
		this.zStations = stations.zStationInitialSet;
		this.hStations = stations.hStationInitialSet;
		this.count = xStations.length - 1;
		this.areAllEquals = stations.areAllEquals;
		this.hResiduals = this.evaluateResidual();
//		int n1 = getCount();
//		this.xStations[n1] = coordinate.x;
//		this.yStations[n1] = coordinate.y;
//		this.zStations[n1] = coordinate.z;
	}

	private double[] evaluateResidual() {
		if (vp.getIsTrend() || (vp.getIntercept() != 0 && vp.getSlope() != 0)) {
			double[] residuals = new double[this.hStations.length];
			for (int i = 0; i < this.hStations.length; i++) {
				residuals[i] = this.hStations[i] - vp.getIntercept() - vp.getSlope() * this.zStations[i];
			}
			return residuals;
		}

		return this.hStations;
	}

	// Getters for the various station data fields
	public double[] getXStations() {
		return xStations;
	}

	public double[] getYStations() {
		return yStations;
	}

	public double[] getZStations() {
		return zStations;
	}

	public double[] getHStations() {
		return hStations;
	}

	public double[] getHResiduals() {
		return hResiduals;
	}

	public int getCount() {
		return count;
	}

	public double getTrendCoeff() {
		return vp.getSlope();
	}

	public double getTrendIntercept() {
		return vp.getIntercept();
	}

	public boolean getIsTrend() {
		return vp.getIsTrend();
	}

	public boolean areAllEquals() {
		return areAllEquals;
	}

}
