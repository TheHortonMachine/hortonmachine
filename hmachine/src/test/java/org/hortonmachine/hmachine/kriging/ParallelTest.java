package org.hortonmachine.hmachine.kriging;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.hmachine.modules.statistics.kriging.pointcase.KrigingPointCase;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class ParallelTest {
	static File testGridFile;
	static File observedFile;
	static File stazioniGridFile;
	private static String stationIdField = "id";

	@Before
	public void init() throws URISyntaxException {
		// 100 station to training model
		URL stazioniGridUrl = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/sic97/observed.shp");
		stazioniGridFile = new File(stazioniGridUrl.toURI());
		URL observedRain4Url = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/sic97/observed_H.csv");
		observedFile = new File(observedRain4Url.toURI());
		URL testGridUrl = this.getClass().getClassLoader().getResource("Input/krigings/PointCase/sic97/test.shp");
		testGridFile = new File(testGridUrl.toURI());
	}

	@Test
	public void testParallel() throws CQLException, URISyntaxException, SchemaException, IOException {

		long startTime = System.currentTimeMillis();
		HashMap<Integer, double[]> seqResult = ParallelTest.testKrigingSic97(false);
		long endTime = System.currentTimeMillis();
		System.out.println("Parallel method:" + (endTime - startTime));
		startTime = System.currentTimeMillis();
		HashMap<Integer, double[]> parallelResult = ParallelTest.testKrigingSic97(true);
		endTime = System.currentTimeMillis();
		System.out.println("Parallel method:" + (endTime - startTime));

		Set<Entry<Integer, double[]>> set = parallelResult.entrySet();

		for (Entry<Integer, double[]> entry : set) {
			assertEquals(entry.getValue()[0], seqResult.get(entry.getKey())[0], 10e-04);
		}
	}

	public static DefaultFeatureCollection multiplyFeature(SimpleFeatureCollection originalFeatures) {
		DefaultFeatureCollection replicatedCollection = new DefaultFeatureCollection();

		// Random instance to generate offsets.
		Random random = new Random();
		// Define the maximum offset for x and y (e.g., ±0.001)
		double randomOffsetRange = 0.001;
		int j = 0;
		try (SimpleFeatureIterator iter = originalFeatures.features()) {
			while (iter.hasNext()) {
				SimpleFeature feature = iter.next();
				// Replicate each feature 10 times.
				for (int i = 0; i < 100; i++) {
					// Create a deep copy of the feature.
					SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(feature.getFeatureType());
					featureBuilder.addAll(feature.getAttributes());
					// Use null for featureID to generate a new unique ID.
					SimpleFeature featureCopy = featureBuilder.buildFeature(null);

					// Modify the coordinate randomly if the default geometry is a Point.
					Object geom = featureCopy.getDefaultGeometry();

					Point point = (Point) geom;
					Coordinate origCoord = point.getCoordinate();
					// Generate random offsets between -randomOffsetRange and +randomOffsetRange.
					double offsetX = (random.nextDouble() - 0.5) * 2 * randomOffsetRange;
					double offsetY = (random.nextDouble() - 0.5) * 2 * randomOffsetRange;
					Coordinate newCoord = new Coordinate(origCoord.x + offsetX, origCoord.y + offsetY);
					GeometryFactory gf = new GeometryFactory();
					Point newPoint = gf.createPoint(newCoord);

					featureCopy.setDefaultGeometry(newPoint);
					featureCopy.setAttribute("id", j);
					j = j + 1;

					replicatedCollection.add(featureCopy);
				}
			}
		}
		return replicatedCollection;
	}

	public static HashMap<Integer, double[]> testKrigingSic97(boolean parallel)
			throws URISyntaxException, SchemaException, CQLException, IOException {
		//

		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		stationsReader.file = stazioniGridFile.getAbsolutePath();
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;

		OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
		reader.file = observedFile.getAbsolutePath();
		reader.idfield = "ID";
		reader.tStart = "2022-12-06 17:00";
		reader.tTimestep = 60;
		// reader.tEnd = "2000-01-01 00:00";
		reader.fileNovalue = "-9999";
		reader.initProcess();

		OmsShapefileFeatureReader testReader = new OmsShapefileFeatureReader();
		testReader.file = testGridFile.getAbsolutePath();
		testReader.readFeatureCollection();
		SimpleFeatureCollection testFC = testReader.geodata;
		SimpleFeatureCollection sc = ParallelTest.multiplyFeature(testFC);
		KrigingPointCase kriging = new KrigingPointCase();
		kriging.inInterpolate = sc;

		kriging.inStations = stationsFC;
		kriging.fStationsid = stationIdField;
		kriging.fInterpolateid = stationIdField;
		kriging.nugget = 0;
		kriging.sill = 20903.88;
		kriging.range = 64126.08;
		kriging.pSemivariogramType = "exponential";
		kriging.parallelComputation = parallel;
		while (reader.doProcess) {
			try {
				reader.nextRecord();
				HashMap<Integer, double[]> id2ValueMap = reader.outData;
				kriging.inData = id2ValueMap;
				kriging.execute();
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
return kriging.outData;	}

}
