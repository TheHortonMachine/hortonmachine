package org.hortonmachine.hmachine.kriging;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.utils.math.matrixes.ColumnVector;
import org.hortonmachine.hmachine.modules.statistics.kriging.Kriging;
import org.hortonmachine.hmachine.modules.statistics.kriging.linearsystemsolver.SimpleLinearSystemSolverFactory;
import org.hortonmachine.hmachine.modules.statistics.kriging.pointcase.KrigingPointCase;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.VariogramParameters;
import org.junit.Test;

public class KrigingFunctionTest {

	@Test
	public void testCovarianceMatrixCalculationUsingReflection() throws Exception {
		// result from R
		// [,1] [,2] [,3]
		// [1,] 0.0000000 0.6321206 1
		// [2,] 0.6321206 0.0000000 1
		// [3,] 1.0000000 1.0000000 0

		// Synthetic data for two stations
		// Create variogram parameters with an exponential model:
		// Variogram model: gamma(h) = nugget + sill * (1 - exp(-h/range))
		// For h = 1, semivariance = 1 - exp(-1) ≈ 0.6321, with nugget = 0, sill = 1,
		// range = 1.

		double[] x = { 0.0, 1.0, 0.0 };
		// Create variogram parameters with an exponential model:
		// Variogram model: gamma(h) = nugget + sill * (1 - exp(-h/range))
		// For h = 1, semivariance = 1 - exp(-1) ≈ 0.6321, with nugget = 0, sill = 1,
		// range = 1.

		double[] y = { 0.0, 0.0, 1.0 };
		double[] z = { 0.0, 0.0, 0.0 };
		int n = 2; // number of stations

		// Instantiate your Kriging class and set the variogram parameters.
		Kriging kriging = new KrigingPointCase();
		VariogramParameters vp = new VariogramParameters.Builder("exponential", 0.0, 1.0, 1.0).setLocal(false)
				.setTrend(false).build();

		// Get the private field 'variogramParameters' from the Kriging class
		Field vpField = Kriging.class.getDeclaredField("variogramParameters");
		// Allow access to the private field
		vpField.setAccessible(true);
		// Set the field's value to our vp object
		vpField.set(kriging, vp);
		// Use reflection to access the non-public covMatrixCalculating method.
		Method method = Kriging.class.getDeclaredMethod("covMatrixCalculating", double[].class, double[].class,
				double[].class, int.class);
		method.setAccessible(true); // Allow access to the non-public method

		// Invoke the method
		double[][] covMatrix = (double[][]) method.invoke(kriging, x, y, z, n);

		// Expected semivariance for distance 1.
		double expectedSemivariance = 1.0 - Math.exp(-1.0);

		// The covariance matrix should be of size (n+1) x (n+1) = 3x3.
		// For i, j in {0, 1}:
		// - covMatrix[0][0] = 0 (distance 0)
		// - covMatrix[0][1] = expectedSemivariance (distance 1)
		// - covMatrix[1][0] = expectedSemivariance (distance 1)
		// - covMatrix[1][1] = 0 (distance 0)
		assertEquals(0.0, covMatrix[0][0], 1e-6);
		assertEquals(expectedSemivariance, covMatrix[0][1], 1e-6);
		assertEquals(expectedSemivariance, covMatrix[1][0], 1e-6);
		assertEquals(0.0, covMatrix[1][1], 1e-6);

		// The extra row/column (index 2) for the interpolation point:
		// - covMatrix[i][2] = 1.0 and covMatrix[2][i] = 1.0 for i = 0,1
		// - covMatrix[2][2] = 0
		assertEquals(1.0, covMatrix[0][2], 1e-6);
		assertEquals(1.0, covMatrix[1][2], 1e-6);
		assertEquals(1.0, covMatrix[2][0], 1e-6);
		assertEquals(1.0, covMatrix[2][1], 1e-6);
		assertEquals(0.0, covMatrix[2][2], 1e-6);
		Method method2 = Kriging.class.getDeclaredMethod("knownTermsCalculation", double[].class, double[].class,
				double[].class, int.class);
		method2.setAccessible(true);
		double[] knownTerm = (double[]) method2.invoke(kriging, x, y, z, n);
		assertEquals(0.6321, knownTerm[0], 1e-3);
		assertEquals(0.7568, knownTerm[1], 1e-3);
	}

	@Test
	public void testSolveSystem() throws Exception {
		double[] x = { 0.0, 0.0, 10.0, 10.0, 15.0 };
		double[] y = { 0.0, 10.0, 0.0, 10.0, 15.0 };
		double[] z = { 0.0, 0.0, 0.0, 0.0, 0.0 };
		int n = 4; // number of stations

		// Instantiate your Kriging class and set the variogram parameters.
		Kriging kriging = new KrigingPointCase();
		VariogramParameters vp = new VariogramParameters.Builder("exponential", 0.0, 1.0, 1.0).setLocal(false)
				.setTrend(false).build();

		// Get the private field 'variogramParameters' from the Kriging class
		Field vpField = Kriging.class.getDeclaredField("variogramParameters");
		// Allow access to the private field
		vpField.setAccessible(true);
		// Set the field's value to our vp object
		vpField.set(kriging, vp);
		// Use reflection to access the non-public covMatrixCalculating method.
		Method method = Kriging.class.getDeclaredMethod("covMatrixCalculating", double[].class, double[].class,
				double[].class, int.class);
		method.setAccessible(true); // Allow access to the non-public method

		// Invoke the method
		double[][] covMatrix = (double[][]) method.invoke(kriging, x, y, z, n);
		Method method2 = Kriging.class.getDeclaredMethod("knownTermsCalculation", double[].class, double[].class,
				double[].class, int.class);
		method2.setAccessible(true);
		double[] knownTerm = (double[]) method2.invoke(kriging, x, y, z, n);
		ColumnVector solution = SimpleLinearSystemSolverFactory.solve(knownTerm, covMatrix, "default");
		solution.print();
		assertEquals(0.25, solution.at(0), 1e-3);
		assertEquals(0.25, solution.at(1), 1e-3);
		assertEquals(0.25, solution.at(2), 1e-3);
		assertEquals(0.25, solution.at(3), 1e-3);
		assertEquals(0.24917, solution.at(4), 1e-3);
	}

	@Test
	public void testFixedVariogram() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, IOException, URISyntaxException {
		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		URL stazioniGridUrl = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/sic97/observed.shp");
		File stazioniGridFile = new File(stazioniGridUrl.toURI());
		stationsReader.file = stazioniGridFile.getAbsolutePath();
		stationsReader.readFeatureCollection();
		SimpleFeatureCollection stationsFC = stationsReader.geodata;
		KrigingPointCase kriging = new KrigingPointCase();
		// Get the private field 'variogramParameters' from the Kriging class
		Field vpField = Kriging.class.getDeclaredField("variogramParameters");
		// Allow access to the private field
		vpField.setAccessible(true);
		double intercept = 1;
		double slope = 0.5;
		double nugget = 0.0;
		double sill = 100;
		double range = 10000;
		String model = "linear";
		kriging.nugget = nugget;
		kriging.pSemivariogramType = model;
		kriging.doDetrended = true;
		kriging.sill = sill;
		kriging.range = range;
		kriging.inSlope = slope;
		kriging.inIntercept = intercept;
		kriging.inData = new HashMap<Integer, double[]>();
		kriging.inStations = stationsFC;
		kriging.fStationsZ = "z1";
		kriging.fPointZ = "z1";
		kriging.fStationsid = "ID";
		kriging.fInterpolateid = "ID";
		URL testGridUrl = this.getClass().getClassLoader().getResource("Input/krigings/PointCase/sic97/test.shp");
		File testGridFile = new File(testGridUrl.toURI());
		OmsShapefileFeatureReader testReader = new OmsShapefileFeatureReader();
		testReader.file = testGridFile.getAbsolutePath();
		testReader.readFeatureCollection();
		SimpleFeatureCollection testFC = testReader.geodata;
		kriging.inInterpolate = testFC;
		
		
		Method method = Kriging.class.getDeclaredMethod("initializeKrigingParameters");
		method.setAccessible(true);
		VariogramParameters vp = (VariogramParameters) method.invoke(kriging);
		assertEquals(vp.getIntercept(), intercept, 1e-3);
		assertEquals(vp.getSlope(), slope, 1e-3);
		assertEquals(vp.getSill(), sill, 1e-3);
		assertEquals(vp.getRange(), range, 1e-3);
		assertEquals(vp.getNugget(), nugget, 1e-3);
		assertTrue(vp.getIsTrend());
		assertEquals(vp.getModelName(), model);
		Method method2 = Kriging.class.getDeclaredMethod("determineVariogram", VariogramParameters.class);
		method2.setAccessible(true);
		method2.invoke(kriging, vp);
		vp = (VariogramParameters) vpField.get(kriging);
		assertEquals(vp.getIntercept(), intercept, 1e-3);
		assertEquals(vp.getSlope(), slope, 1e-3);
		assertEquals(vp.getSill(), sill, 1e-3);
		assertEquals(vp.getRange(), range, 1e-3);
		assertEquals(vp.getNugget(), nugget, 1e-3);
		assertTrue(vp.getIsTrend());
		assertEquals(vp.getModelName(), model);
	}

	@Test
	public void testVariableVariogram() throws NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchFieldException, URISyntaxException, IOException {

		OmsShapefileFeatureReader stationsReader = new OmsShapefileFeatureReader();
		URL stazioniGridUrl = this.getClass().getClassLoader()
				.getResource("Input/krigings/PointCase/sic97/observed.shp");
		File stazioniGridFile = new File(stazioniGridUrl.toURI());
		stationsReader.file = stazioniGridFile.getAbsolutePath();
		stationsReader.readFeatureCollection();
	
		SimpleFeatureCollection stationsFC = stationsReader.geodata;
		KrigingPointCase kriging = new KrigingPointCase();
        kriging.inStations = stationsFC;
        kriging.fStationsid = "ID";
    	URL testGridUrl = this.getClass().getClassLoader().getResource("Input/krigings/PointCase/sic97/test.shp");
		File testGridFile = new File(testGridUrl.toURI());
		OmsShapefileFeatureReader testReader = new OmsShapefileFeatureReader();
		testReader.file = testGridFile.getAbsolutePath();
		testReader.readFeatureCollection();
		SimpleFeatureCollection testFC = testReader.geodata;
		kriging.inInterpolate = testFC; 
		kriging.fInterpolateid = "ID";

		// Get the private field 'variogramParameters' from the Kriging class
		
		Field vpField = Kriging.class.getDeclaredField("variogramParameters");
		// Allow access to the private field
		vpField.setAccessible(true);

		double intercept = 1;
		double slope = 0.5;
		double nugget = 0.0;
		double sill = 100;
		double range = 10000;
		String model = "exponential";
		HashMap<Integer, double[]> variogram = new HashMap();

		variogram.put(0, new double[] { nugget });
		variogram.put(1, new double[] { sill });
		variogram.put(2, new double[] { range });
		variogram.put(3, new double[] { 1 });
		variogram.put(4, new double[] { 1 });
		variogram.put(5, new double[] { 0 });
		variogram.put(6, new double[] { intercept });
		variogram.put(7, new double[] { slope });
		kriging.inTheoreticalVariogram = variogram;
		kriging.inData = new HashMap<Integer, double[]>();
		Method method = Kriging.class.getDeclaredMethod("initializeKrigingParameters");
		method.setAccessible(true);
		VariogramParameters vp = (VariogramParameters) method.invoke(kriging);
		Method method2 = Kriging.class.getDeclaredMethod("determineVariogram", VariogramParameters.class);
		method2.setAccessible(true);
		method2.invoke(kriging, vp);
		vp = (VariogramParameters) vpField.get(kriging);
		assertEquals(vp.getIntercept(), intercept, 1e-3);
		assertEquals(vp.getSlope(), slope, 1e-3);
		assertEquals(vp.getSill(), sill, 1e-3);
		assertEquals(vp.getRange(), range, 1e-3);
		assertEquals(vp.getNugget(), nugget, 1e-3);
		assertTrue(vp.getIsTrend());
		assertEquals(vp.getModelName(), model);

		variogram.put(0, new double[] { nugget });
		variogram.put(1, new double[] { sill });
		variogram.put(2, new double[] { range });
		variogram.put(3, new double[] { 1 });
		variogram.put(4, new double[] { 0 });
		variogram.put(5, new double[] { 0 });
		variogram.put(6, new double[] { intercept });
		variogram.put(7, new double[] { slope });
		kriging.inTheoreticalVariogram = variogram;
		vp = (VariogramParameters) method.invoke(kriging);
		method2.invoke(kriging, vp);
		vp = (VariogramParameters) vpField.get(kriging);
		assertEquals(vp.getIntercept(), intercept, 1e-3);
		assertEquals(vp.getSlope(), slope, 1e-3);
		assertEquals(vp.getSill(), sill, 1e-3);
		assertEquals(vp.getRange(), range, 1e-3);
		assertEquals(vp.getNugget(), nugget, 1e-3);
		assertFalse(vp.getIsTrend());
		assertEquals(vp.getModelName(), model);

		HashMap<Integer, double[]> vpHM = vp.toHashMap();
		for (Integer key : vpHM.keySet()) {
			assertTrue("Actual map does not contain key: " + key, variogram.containsKey(key));
			assertArrayEquals("Arrays differ at key " + key, vpHM.get(key), variogram.get(key), 1e-3);
		}

	}
}
