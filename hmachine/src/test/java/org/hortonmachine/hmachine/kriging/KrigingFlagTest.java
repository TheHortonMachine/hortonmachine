package org.hortonmachine.hmachine.kriging;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.hmachine.modules.statistics.kriging.Kriging;
import org.hortonmachine.hmachine.modules.statistics.kriging.interpolationdata.InterpolationDataProvider;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationProcessor;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationsSelection;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.VariogramParameters;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import oms3.annotations.Description;
import oms3.annotations.Out;

public class KrigingFlagTest {

	private KrigingTest kriging;
	DefaultFeatureCollection collection;

	@Before
	public void setUp() throws Exception {
		kriging = new KrigingTest();
		kriging.parallelComputation = true;
		// Build a SimpleFeatureType with a geometry, station id, and z attribute.
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setName("Station");
		typeBuilder.add("the_geom", Point.class, DefaultGeographicCRS.WGS84);
		typeBuilder.add("id", Integer.class);
		typeBuilder.add("z", Double.class);
		final SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

		// Create a feature collection and a geometry factory.
		collection = new DefaultFeatureCollection("internal", TYPE);
		GeometryFactory geomFactory = new GeometryFactory();

		// Create three synthetic features.
		SimpleFeature feature1 = SimpleFeatureBuilder.build(TYPE,
				new Object[] { geomFactory.createPoint(new Coordinate(0, 0)), 1, 10.0 }, "fid.1");
		SimpleFeature feature2 = SimpleFeatureBuilder.build(TYPE,
				new Object[] { geomFactory.createPoint(new Coordinate(8, 8)), 2, 20.0 }, "fid.2");
		SimpleFeature feature3 = SimpleFeatureBuilder.build(TYPE,
				new Object[] { geomFactory.createPoint(new Coordinate(8, 0)), 3, 30.0 }, "fid.3");
		SimpleFeature feature4 = SimpleFeatureBuilder.build(TYPE,
				new Object[] { geomFactory.createPoint(new Coordinate(0, 8)), 4, 30.0 }, "fid.4");
		SimpleFeature feature5 = SimpleFeatureBuilder.build(TYPE,
				new Object[] { geomFactory.createPoint(new Coordinate(225, 225)), 5, 30.0 }, "fid.5");
		SimpleFeature feature6 = SimpleFeatureBuilder.build(TYPE,
				new Object[] { geomFactory.createPoint(new Coordinate(235, 235)), 6, 30.0 }, "fid.6");
		SimpleFeature feature7 = SimpleFeatureBuilder.build(TYPE,
				new Object[] { geomFactory.createPoint(new Coordinate(225, 235)), 7, 30.0 }, "fid.7");
		SimpleFeature feature8 = SimpleFeatureBuilder.build(TYPE,
				new Object[] { geomFactory.createPoint(new Coordinate(235, 225)), 8, 30.0 }, "fid.8");
		collection.add(feature1);
		collection.add(feature2);
		collection.add(feature3);
		collection.add(feature4);
		collection.add(feature5);
		collection.add(feature6);
		collection.add(feature7);
		collection.add(feature8);
	}

	@Test
	public void testBoundToZero() throws Exception {
		// Set up StationsSelection with neighbor selection turned off.

		// Create inData mapping station id to measured values.
		HashMap<Integer, double[]> inData = new HashMap<>();
		inData.put(1, new double[] { -100.0 });
		inData.put(2, new double[] { -200.0 });
		inData.put(3, new double[] { -300.0 });
		inData.put(4, new double[] { -300.0 });
		inData.put(5, new double[] { -300.0 });
		inData.put(6, new double[] { -300.0 });
		inData.put(7, new double[] { -300.0 });
		inData.put(8, new double[] { -300.0 });

		kriging.inStations = collection;
		kriging.inData = inData;
		kriging.doIncludeZero = true;
		kriging.doLogarithmic = false;
		kriging.boundedToZero = true;
		kriging.maxdist = 0; // No neighbor selection.
		kriging.inNumCloserStations = 0; // No neighbor selection.
		kriging.fStationsid = "id";
		kriging.fStationsZ = "z";
		kriging.nugget = 0;
		kriging.doDetrended = false;
		kriging.sill = 10;
		kriging.range = 4;
		kriging.pSemivariogramType = "linear";
		kriging.execute();
		HashMap<Integer, double[]> result = kriging.outData;

		for (Map.Entry<Integer, double[]> entry : result.entrySet()) {
			double value = entry.getValue()[0];
			assertTrue(" not bounded", value == 0);
		}

	}

	// TODO disabled due to residual evaluator not yet properly ported.
//	@Test
//	public void testNearest() throws Exception {
//		// Set up StationsSelection with neighbor selection turned off.
//
//		// Create inData mapping station id to measured values.
//		HashMap<Integer, double[]> inData = new HashMap<>();
//		inData.put(1, new double[] { 100.0 });
//		inData.put(2, new double[] { 100.0 });
//		inData.put(3, new double[] { 100.0 });
//		inData.put(4, new double[] { 100.0 });
//		inData.put(5, new double[] { 400.0 });
//		inData.put(6, new double[] { 400.0 });
//		inData.put(7, new double[] { 400.0 });
//		inData.put(8, new double[] { 400.0 });
//		kriging.inStations = collection;
//		kriging.inData = inData;
//		kriging.doIncludeZero = true;
//		kriging.doLogarithmic = false;
//		kriging.fStationsid = "id";
//		kriging.fStationsZ = "z";
//		kriging.nugget = 0;
//		kriging.doDetrended = false;
//		kriging.sill = 10;
//		kriging.range = 15;
//		kriging.inNumCloserStations = 4;
//		kriging.pSemivariogramType = "exponential";
//		kriging.execute();
//		HashMap<Integer, double[]> results = kriging.outData;
//		double firstValue = results.get(1)[0];
//		assertTrue(" not bounded", firstValue == 100);
//		assertTrue(" not bounded", firstValue > 90);
//		double secondValue = results.get(2)[0];
//		assertTrue(" not bounded", secondValue == 400);
//		assertTrue(" not bounded", secondValue > 390);
//
//		kriging.inNumCloserStations = 0;
//		kriging.maxdist = 8.0;
//		kriging.execute();
//		results = kriging.outData;
//		firstValue = results.get(1)[0];
//		assertTrue(" not bounded", firstValue == 100);
//		assertTrue(" not bounded", firstValue > 90);
//		secondValue = results.get(2)[0];
//		assertTrue(" not bounded", secondValue == 400);
//		assertTrue(" not bounded", secondValue > 390);
//
//	}

	private class KrigingTest extends Kriging {

		@Description("The hashmap with the interpolated results")
		@Out
		public HashMap<Integer, double[]> outData = null;

		@Override
		protected void storeResult(double[] result, HashMap<Integer, Coordinate> interpolatedCoordinatesMap) {
			outData = new HashMap();
			for (int i = 1; i <= result.length; i++) {
				outData.put(i, new double[] { result[i - 1] });
			}
		}

		@Override
		protected InterpolationDataProvider initializeInterpolatorData() {
			return new InterpolationDataProvider() {

				@Override
				public double getValueAt(Coordinate coordinate) {
					// TODO Auto-generated method stub
					return 0;
				}

				@Override
				public LinkedHashMap<Integer, Coordinate> getCoordinates() {
					LinkedHashMap<Integer, Coordinate> coords = new LinkedHashMap();
					coords.put(1, new Coordinate(4, 4));
					coords.put(2, new Coordinate(230, 230));

					return coords;
				}
			};
		}

		@Override
		protected StationProcessor createStationProcessor(StationsSelection stations, VariogramParameters vp) {
			return new StationProcessor(stations, vp);
		}

	}

}
