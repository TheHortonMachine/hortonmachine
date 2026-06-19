/*
 * GNU GPL v3 License
 *
 * Copyright 2016 Marialaura Bancheri
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.hortonmachine.hmachine.kriging;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.filter.text.cql2.CQLException;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.hmachine.modules.statistics.kriging.pointcase.KrigingPointCase;
import org.junit.Test;

public class KrigingPointCaseTest {
    public boolean  doParalle =true;
	/**
	 * Run the kriging models.
	 *
	 * Test from Sic97 data (Spatial interpolation Comparison). Value are evaluated
	 * in R with gstat.
	 *
	 * <script src=
	 * "https://gist.github.com/bubbobne/0e029b541c6f30d8ce7ff95c55608c8b.js"></script>
	 *
	 *
	 * @throws Exception
	 * @throws Exceptionimport org.junit.jupiter.api.BeforeEach;
	 */

	@Test
	public void testKrigingSic97() throws URISyntaxException, SchemaException, CQLException, IOException {
		//
		String stationIdField = "id";
		// 100 station to training model
		URL stazioniGridUrl = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/sic97/observed.shp");
		File stazioniGridFile = new File(stazioniGridUrl.toURI());
		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		stationsReader.file = stazioniGridFile.getAbsolutePath();
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;

		URL observedRain4Url = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/sic97/observed_H.csv");
		File observedFile = new File(observedRain4Url.toURI());
		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		reader.file = observedFile.getAbsolutePath();
		reader.idfield = "ID";
		reader.tStart = "2022-12-06 17:00";
		reader.tTimestep = 60;
		// reader.tEnd = "2000-01-01 00:00";
		reader.fileNovalue = "-9999";
		reader.initProcess();

		OmsTimeSeriesIteratorReader predictedFromRReaderValue = new OmsTimeSeriesIteratorReader();
		URL testRainFromR = this.getClass().getClassLoader()
				.getResource("Output/krigings/PointCase/h_fromR_no_trend.csv");
		File testFileFromR = new File(testRainFromR.toURI());
		predictedFromRReaderValue.file = testFileFromR.getAbsolutePath();
		predictedFromRReaderValue.idfield = "ID";

		predictedFromRReaderValue.tStart = "2022-12-06 17:00";
		predictedFromRReaderValue.tTimestep = 60;
		predictedFromRReaderValue.fileNovalue = "-9999";
		predictedFromRReaderValue.initProcess();
		KrigingPointCase kriging = new KrigingPointCase();
		URL testGridUrl = this.getClass().getClassLoader().getResource("Input/krigings/PointCase/sic97/test.shp");
		File testGridFile = new File(testGridUrl.toURI());
		OmsShapefileFeatureReader testReader = new OmsShapefileFeatureReader();
		testReader.file = testGridFile.getAbsolutePath();
		testReader.readFeatureCollection();
		SimpleFeatureCollection testFC = testReader.geodata;
		kriging.inInterpolate = testFC;
		kriging.inStations = stationsFC;
		kriging.fStationsid = stationIdField;
		kriging.fInterpolateid = stationIdField;
		kriging.nugget = 0;
		kriging.sill = 20903.88;
		kriging.range = 64126.08;
		kriging.pSemivariogramType = "exponential";
		kriging.parallelComputation = doParalle;
		while (reader.doProcess) {
			try {
				reader.nextRecord();
				HashMap<Integer, double[]> id2ValueMap = reader.outData;
				kriging.inData = id2ValueMap;
				kriging.execute();
				predictedFromRReaderValue.nextRecord();
				HashMap<Integer, double[]> predictedGstatR = predictedFromRReaderValue.outData;
				HashMap<Integer, double[]> result = kriging.outData;
				Set<Integer> pointsToInterpolateResult = result.keySet();
				Iterator<Integer> iteratorTest = pointsToInterpolateResult.iterator();
				double maxError = Double.MIN_VALUE;
				double minError = Double.MAX_VALUE;
				double meanError = 0;
				int n = 0;
				while (iteratorTest.hasNext()) {
					int id = iteratorTest.next();
					double[] values = result.get(id);
					double[] actual = predictedGstatR.get(id);
					assertEquals(actual[0], values[0], 1);
					System.out.println("actual is:" + actual[0] + " evaluate " + values[0]);
					double error = Math.abs(actual[0] - values[0]);
					meanError += error;
					if (error < minError) {
						minError = error;
					}
					if (error > maxError) {
						maxError = error;
					}

					n++;

				}
				meanError = meanError / n;
				System.out.println("mean error is:" + meanError + " max " + maxError + " min:" + minError);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (CQLException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// writer.close();
	}

	/**
	 * Run the kriging models.
	 *
	 * Test from Sic97 data (Spatial interpolation Comparison). Value are evaluated
	 * in R with gstat. N.B. data are generated in Rwith a slop of 0.05 and the
	 * intercept the mean value of original h pf sic97.
	 *
	 * Crete the data with: <script src=
	 * "https://gist.github.com/bubbobne/320dc6145ec89e73e8feaae40102773c.js"></script>
	 *
	 * Detrended kriging: <script src=
	 * "https://gist.github.com/bubbobne/924bc09f6c580f0d2ee95614d0b5f59c.js"></script>
	 *
	 * @throws Exception
	 * @throws Exception
	 */

	@Test
	public void testKrigingSic97Trend() throws URISyntaxException, SchemaException, CQLException, IOException {
		//
		String stationIdField = "id";
		// 100 station to training model
		URL stazioniGridUrl = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/sic97/observed.shp");
		File stazioniGridFile = new File(stazioniGridUrl.toURI());
		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		stationsReader.file = stazioniGridFile.getAbsolutePath();
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;

		URL observedRain4Url = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/sic97/observed_trend.csv");
		File observedFile = new File(observedRain4Url.toURI());
		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		reader.file = observedFile.getAbsolutePath();
		reader.idfield = "ID";
		reader.tStart = "2022-12-06 17:00";
		reader.tTimestep = 60;
		// reader.tEnd = "2000-01-01 00:00";
		reader.fileNovalue = "-9999";
		reader.initProcess();

		OmsTimeSeriesIteratorReader predictedFromRReaderValue = new OmsTimeSeriesIteratorReader();
		URL testRainFromR = this.getClass().getClassLoader()
				.getResource("Output/krigings/PointCase//h_from_R_trend.csv");
		File testFileFromR = new File(testRainFromR.toURI());
		predictedFromRReaderValue.file = testFileFromR.getAbsolutePath();
		predictedFromRReaderValue.idfield = "ID";
		predictedFromRReaderValue.tStart = "2022-12-06 17:00";
		predictedFromRReaderValue.tTimestep = 60;
		predictedFromRReaderValue.fileNovalue = "-9999";
		predictedFromRReaderValue.initProcess();
		KrigingPointCase kriging = new KrigingPointCase();
		URL testGridUrl = this.getClass().getClassLoader().getResource("Input/krigings/PointCase/sic97/test.shp");
		File testGridFile = new File(testGridUrl.toURI());
		OmsShapefileFeatureReader testReader = new OmsShapefileFeatureReader();
		testReader.file = testGridFile.getAbsolutePath();
		testReader.readFeatureCollection();
		SimpleFeatureCollection testFC = testReader.geodata;

		kriging.inInterpolate = testFC;
		kriging.inStations = stationsFC;
		kriging.fStationsid = stationIdField;
		kriging.fInterpolateid = stationIdField;
		kriging.fStationsZ = "z1";
		kriging.fPointZ = "z1";
		// kriging.inNumCloserStations = 200;
		kriging.nugget = 102.99804;

		kriging.sill = 60.07937;
		kriging.range = 492121.5;
		kriging.inIntercept = 181.79;
		kriging.inSlope = 0.049;
		kriging.pSemivariogramType = "exponential";
		kriging.doDetrended = true;
		kriging.parallelComputation = doParalle;

		while (reader.doProcess) {
			try {
				reader.nextRecord();
				HashMap<Integer, double[]> id2ValueMap = reader.outData;
				kriging.inData = id2ValueMap;
				kriging.execute();
				predictedFromRReaderValue.nextRecord();
				HashMap<Integer, double[]> predictedGstatR = predictedFromRReaderValue.outData;
				HashMap<Integer, double[]> result = kriging.outData;
				Set<Integer> pointsToInterpolateResult = result.keySet();
				Iterator<Integer> iteratorTest = pointsToInterpolateResult.iterator();
				double maxError = Double.MIN_VALUE;
				double minError = Double.MAX_VALUE;
				double meanError = 0;
				int n = 0;
				while (iteratorTest.hasNext()) {
					int id = iteratorTest.next();
					double[] values = result.get(id);
					double[] actual = predictedGstatR.get(id);
					assertEquals(actual[0], values[0], 3);
					System.out.println(" " + id + "  actual is:" + actual[0] + " evaluate " + values[0]);
					double error = Math.abs(actual[0] - values[0]);
					meanError += error;
					if (error < minError) {
						minError = error;
					}
					if (error > maxError) {
						maxError = error;
					}

					n++;

				}
				meanError = meanError / n;
				System.out.println("mean error is:" + meanError + " max " + maxError + " min:" + minError);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (CQLException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// writer.close();
	}

	/**
	 * Run the kriging models. ex test2
	 * <p>
	 * This is the case which all the station have the same value equal to -9999, no
	 * values.
	 * </p>
	 *
	 * @throws Exception
	 * @throws Exception
	 */

	@Test
	public void testKrigingAllNoVAlue() throws Exception {
		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		URL stazioniGridUrl = this.getClass().getClassLoader().getResource("Input/krigings/PointCase/rainstations.shp");
		File stazioniGridFile = new File(stazioniGridUrl.toURI());
		stationsReader.file = stazioniGridFile.getAbsolutePath();
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;
		//
		OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
		stazioniGridUrl = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/basins_passirio_width0.shp");
		stazioniGridFile = new File(stazioniGridUrl.toURI());
		interpolatedPointsReader.file = stazioniGridFile.getAbsolutePath();
		interpolatedPointsReader.readFeatureCollection();
		SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
		//
		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		URL observedRain4Url = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/rain_test_all_NoValue.csv");
		File observedFile = new File(observedRain4Url.toURI());
		reader.file = observedFile.getAbsolutePath();
		reader.idfield = "ID";
		reader.tStart = "2000-01-01 00:00";
		reader.tTimestep = 60;
		// reader.tEnd = "2000-01-01 00:00";
		reader.fileNovalue = "-9999";
		//
		reader.initProcess();
		//
		KrigingPointCase kriging = new KrigingPointCase();

		//
		kriging.inStations = stationsFC;
		kriging.fStationsid = "ID_PUNTI_M";
		kriging.doDetrended = false;
		kriging.inInterpolate = interpolatedPointsFC;
		kriging.fInterpolateid = "netnum";
		kriging.maxdist = 40368.0;

		kriging.range = 123537.0;
		kriging.nugget = 0.0;
		kriging.sill = 1.678383;
		kriging.pSemivariogramType = "linear";
		kriging.parallelComputation = doParalle;

		//
		OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
		URL interpolatedUrl = this.getClass().getClassLoader()
				.getResource("Output/krigings/PointCase/kriging_interpolated_NoValue.csv");
		File interpolatedFile = new File(interpolatedUrl.toURI());
		writer.file = interpolatedFile.getAbsolutePath();
		//
		writer.tStart = reader.tStart;
		writer.tTimestep = reader.tTimestep;
		//
		while (reader.doProcess) {
			reader.nextRecord();
			HashMap<Integer, double[]> id2ValueMap = reader.outData;
			kriging.inData = id2ValueMap;
			kriging.execute();
			/*
			 * Extract the result.
			 */

			HashMap<Integer, double[]> result = kriging.outData;
			Set<Integer> pointsToInterpolateResult = result.keySet();
			Iterator<Integer> iterator = pointsToInterpolateResult.iterator();
			while (iterator.hasNext()) {
				int id = iterator.next();
				double[] actual = result.get(id);
				assertEquals(-9999, actual[0], 0);
			}

			writer.inData = result;
			writer.writeNextLine();
		}

		//
		reader.close();
		writer.close();
	}

	/**
	 * Run the kriging models. ex Test5
	 * <p>
	 * This is the case which there is only one station.
	 *
	 * Pay attention, there are several points to evaluate. For the first point,
	 * kriging enters the first if condition (n1 != 0), which checks if there is at
	 * least one station. Then, areAllEqual is true since only one value is
	 * available. In this section, n1 is set to 0 (I suppose it's a design choice).
	 *
	 * </p>
	 *
	 * @throws Exception
	 * @throws Exception
	 */

	@Test
	public void testKrigingOnlyOneStation() throws Exception {
		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		stationsReader.file = new File(
				this.getClass().getClassLoader().getResource("Input/krigings/PointCase/rainstations.shp").toURI())
				.getAbsolutePath();
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;
		//
		OmsShapefileFeatureReader interpolatedPointsReader = new OmsShapefileFeatureReader();
		interpolatedPointsReader.file = new File(this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/basins_passirio_width0.shp").toURI()).getAbsolutePath();
		interpolatedPointsReader.readFeatureCollection();
		SimpleFeatureCollection interpolatedPointsFC = interpolatedPointsReader.geodata;
		//
		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		reader.file = new File(this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/rain_test_one_value.csv").toURI()).getAbsolutePath();
		reader.idfield = "ID";
		reader.tStart = "2000-01-01 00:00";
		reader.tTimestep = 60;
		// reader.tEnd = "2000-01-01 00:00";
		reader.fileNovalue = "-9999";
		//
		reader.initProcess();

		KrigingPointCase kriging = new KrigingPointCase();
		// kriging.pm = pm;
		//
		kriging.inStations = stationsFC;
		kriging.fStationsid = "ID_PUNTI_M";
		kriging.doDetrended = false;
		kriging.inInterpolate = interpolatedPointsFC;
		kriging.fInterpolateid = "netnum";
		// Set up the model in order to use the variogram with an explicit integral
		// scale and
		// variance.
		kriging.pSemivariogramType = "linear";
		kriging.range = 123537.0;
		kriging.nugget = 0.0;
		kriging.sill = 1.678383;
		kriging.parallelComputation = doParalle;

		// kriging.maxdist=1000;
		OmsTimeSeriesIteratorWriter writer = new OmsTimeSeriesIteratorWriter();
		writer.file = new File(this.getClass().getClassLoader()
				.getResource("Output/krigings/PointCase/kriging_interpolated_3.csv").toURI()).getAbsolutePath();
		//
		writer.tStart = reader.tStart;
		writer.tTimestep = reader.tTimestep;
		int j = 0;
		while (reader.doProcess) {
			System.out.println(j);
			reader.nextRecord();
			HashMap<Integer, double[]> id2ValueMap = reader.outData;
			kriging.inData = id2ValueMap;
			kriging.execute();
			// Extract the result.
			HashMap<Integer, double[]> result = kriging.outData;
			Set<Integer> pointsToInterpolateResult = result.keySet();
			Iterator<Integer> iteratorTest = pointsToInterpolateResult.iterator();
			double expected;
			if (j == 0) {
				expected = 10.0;
			} else if (j == 1) {
				expected = 15;
			} else if (j == 2) {
				expected = 1;
			} else if (j == 3) {
				expected = 2;
			} else if (j == 4) {
				expected = 2;
			} else if (j == 5) {
				expected = 0;
			} else if (j == 6) {
				expected = 0;
			} else if (j == 7) {
				expected = 23;
			} else if (j == 8) {
				expected = 50;
			} else if (j == 9) {
				expected = 70;
			} else if (j == 10) {
				expected = 30;
			} else if (j == 11) {
				expected = 10;
			} else if (j == 12) {
				expected = 2;
			} else {
				expected = 1.0;
			}
			//
			while (iteratorTest.hasNext()) {
				int id = iteratorTest.next();
				double[] actual = result.get(id);
				//
				assertEquals(expected, actual[0], 0);
			}
			writer.inData = result;
			writer.writeNextLine();
			j++;
		}
		//
		reader.close();
		writer.close();
	}

}