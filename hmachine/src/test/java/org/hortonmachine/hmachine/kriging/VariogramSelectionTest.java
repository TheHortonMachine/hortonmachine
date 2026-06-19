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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.filter.text.cql2.CQLException;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.TimeSeriesVariogramParameterEstimator;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.VariogramParameters;
import org.junit.Test;

public class VariogramSelectionTest {

	/**
	 * Run the kriging models.
	 *
	 * Test from Sic97 data (Spatial interpolation Comparison). Value are evaluated
	 * in R with gstat.
	 *
	 * <script src=
	 * "https://gist.github.com/bubbobne/0e029b541c6f30d8ce7ff95c55608c8b.js"></script>
	 *
	 * <pre>
	 *   model   nugget     sill    range      SSE
	 *   Gau  613.8793  14814.40  33795.47  1.979926
	 *   Lin    0.0000  14922.00  58249.02  1.989518
	 *   Cir    0.0000  15147.50  71352.25  2.335471
	 *   Sph    0.0000  15292.38  82946.36  2.521664
	 *   Ste    0.0000  20901.02  90668.00  4.281376
	 *   Exp    0.0000  20903.88  64126.08  4.281377
	 *   Mat    0.0000  20903.88  64126.09  4.281377
	 * </pre>
	 *
	 *
	 *
	 *
	 * @throws Exception
	 * @throws Exception
	 */

	@Test
	public void testVariogramSic97() throws URISyntaxException, SchemaException, CQLException, IOException {
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

		TimeSeriesVariogramParameterEstimator parameterEvaluator = new TimeSeriesVariogramParameterEstimator();
		URL thVariogramUrl = this.getClass().getClassLoader().getResource("Output/krigings/PointCase");
		File thVariogramFile = new File(thVariogramUrl.toURI());

		parameterEvaluator.inHValuesPath = observedFile.getAbsolutePath();
		parameterEvaluator.inStations = stationsFC;
		parameterEvaluator.fStationsid = stationIdField;
		parameterEvaluator.cutoffDivide = 15;
		parameterEvaluator.doDetrended = false;
		parameterEvaluator.doIncludeZero = true;
		parameterEvaluator.tStart = "2022-12-06 17:00";
		parameterEvaluator.getExperimentalVariogramData = true;
		parameterEvaluator.inTheoreticalVariogramFile = thVariogramFile.getAbsolutePath() + "/variogram.csv";
		parameterEvaluator.execute();
		VariogramParameters vp = parameterEvaluator.getGlobalVariogramParameters();
		assertTrue("Il nugget deve essere >= 0", vp.getNugget() >= 0);
		assertTrue("Il sill deve essere >= nugget", vp.getSill() >= vp.getNugget());
		double expectedSill = 14814.40; // valore da R
		double tolerance = 0.1 * expectedSill; // 10% di tolleranza
		assertEquals(expectedSill, vp.getSill(), tolerance);

	}

}