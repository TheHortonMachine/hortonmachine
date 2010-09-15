package org.jgrasstools.hortonmachine.modules.statistics.kriging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsEngine;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description("Verify if there is a double station into the data. If the double station have different value then it is possible calculating the mean of two value ")
@Author(name = "Daniele Andreis and Riccardo Rigon")
@Keywords("Hydrology, statistic")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/")
public class ValidateDoubleStation extends JGTModel {

	@Description("The collection of the measurement point, containing the position of the station.")
	@In
	public SimpleFeatureCollection inStations = null;

	@Description("The field of the stations collections, defining the id.")
	@In
	public String fStationsid = null;

	@Description("The measured data, to be verified.")
	@In
	public HashMap<Integer, double[]> inData = null;

	@Description("The measured data, without a double value.")
	@In
	public HashMap<Integer, double[]> outData = null;

	@Description("The collection of the measurement point, containing the position of the station without the double point.")
	@In
	public SimpleFeatureCollection outStations = null;

	@Role(Role.PARAMETER)
	@Description("The progress monitor.")
	@In
	public IJGTProgressMonitor pm = new DummyProgressMonitor();

	@Role(Role.PARAMETER)
	@Description("Select if do the mean between double value or delete one of these if they are equals.")
	@In
	public boolean doMean = false;

	private HortonMessageHandler msg = HortonMessageHandler.getInstance();

	@Execute
	public void verifyDoubleStation() throws Exception {
		if (!concatOr((outStations == null || outData == null), doReset)) {
			return;
		}

		if (inData == null || inStations == null) {
			throw new NullPointerException(msg
					.message("kriging.stationproblem"));
		}

		List<Double> xStationList = new ArrayList<Double>();
		List<Double> yStationList = new ArrayList<Double>();
		List<Double> zStationList = new ArrayList<Double>();
		List<Double> hStationList = new ArrayList<Double>();
		List<Integer> idStationList = new ArrayList<Integer>();

		/*
		 * Store the station coordinates and measured data in the array.
		 */
		FeatureIterator<SimpleFeature> stationsIter = inStations.features();
		try {
			while (stationsIter.hasNext()) {
				SimpleFeature feature = stationsIter.next();
				int id = ((Number) feature.getAttribute(fStationsid))
						.intValue();
				Coordinate coordinate = ((Geometry) feature
						.getDefaultGeometry()).getCentroid().getCoordinate();
				double[] h = inData.get(id);
				if (h == null) {
					/*
					 * skip data for non existing stations, they are allowed.
					 * Also skip novalues.
					 */
					// throw new NullPointerException("thereisn't data");
					continue;
				}
				idStationList.add(id);
				xStationList.add(coordinate.x);
				yStationList.add(coordinate.y);
				zStationList.add(coordinate.z);
				hStationList.add(h[0]);

			}
		} finally {
			inStations.close(stationsIter);
		}

		int nStaz = xStationList.size();
		/*
		 * The coordinates of the station points plus in last position a place
		 * for the coordinate of the point to interpolate.
		 */
		int[] idStation = new int[nStaz];
		double[] xStation = new double[nStaz];
		double[] yStation = new double[nStaz];
		double[] zStation = new double[nStaz];
		double[] hStation = new double[nStaz];
		idStation[0] = idStationList.get(0);
		xStation[0] = xStationList.get(0);
		yStation[0] = yStationList.get(0);
		zStation[0] = zStationList.get(0);
		hStation[0] = hStationList.get(0);
		int k = 0;
		int j = 0;
		hStation[k] = hStationList.get(0);
		idStation[k] = idStationList.get(0);
		ModelsEngine modelsEngine = new ModelsEngine();
		List<Integer> idStationtoDelete = new ArrayList<Integer>();
		outData = new HashMap<Integer, double[]>();
		for (int i = 1; i < xStation.length; i++) {
			int id = idStationList.get(i);
			double xTmp = xStationList.get(i);
			double yTmp = yStationList.get(i);
			double zTmp = zStationList.get(i);
			double hTmp = hStationList.get(i);
			boolean doubleStation = modelsEngine.verifyDoubleStation(xStation,
					yStation, zStation, hStation, xTmp, yTmp, zTmp, hTmp, i,
					doMean, null);

			if (!doubleStation) {
				xStation[k] = xStationList.get(i);
				yStation[k] = yStationList.get(i);
				zStation[k] = zStationList.get(i);
				hStation[k] = hStationList.get(i);
				idStation[k] = id;
				k++;

			} else {
				idStationtoDelete.add(id);
				j++;
			}
		}

		for (int i = 0; i < k; i++) {
			outData.put(idStation[i], new double[] { hStation[i] });
		}

		stationsIter = inStations.features();
		outStations = FeatureCollections.newCollection();

		try {
			while (stationsIter.hasNext()) {
				SimpleFeature feature = stationsIter.next();
				int id = ((Number) feature.getAttribute(fStationsid))
						.intValue();

				for (int q = 0; q < j; q++) {
					if (idStationtoDelete.get(q) == id) {

						continue;
					}
				}

				outStations.add(feature);

			}

		} finally {
			inStations.close(stationsIter);
		}

	}

}
