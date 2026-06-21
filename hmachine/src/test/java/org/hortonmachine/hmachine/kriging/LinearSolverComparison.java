package org.hortonmachine.hmachine.kriging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

import org.hortonmachine.gears.utils.math.matrixes.ColumnVector;
import org.hortonmachine.hmachine.modules.statistics.kriging.Kriging;
import org.hortonmachine.hmachine.modules.statistics.kriging.linearsystemsolver.SimpleLinearSystemSolverFactory;
import org.hortonmachine.hmachine.modules.statistics.kriging.pointcase.KrigingPointCase;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.VariogramParameters;
import org.junit.Test;

public class LinearSolverComparison {
	@Test
	public void testtMethods() throws Exception {

		double[] x = generateRandomArray(1001, 0, 100);
		double[] y = generateRandomArray(1001, 0, 100);
		double[] z = generateRandomArray(1001, 0, 100);
		int n = 1000;
		Kriging kriging = new KrigingPointCase();
		VariogramParameters vp = new VariogramParameters.Builder("exponential", 0.0, 1.0, 1.0).setLocal(false).setTrend(false).build();

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
		long startTime = System.currentTimeMillis();
		ColumnVector solution = SimpleLinearSystemSolverFactory.solve(knownTerm, covMatrix, "default");
		long endTime = System.currentTimeMillis();
		System.out.println("Default method:" + (endTime - startTime));
		startTime = System.currentTimeMillis();
		solution = SimpleLinearSystemSolverFactory.solve(knownTerm, covMatrix, "math3");
		endTime = System.currentTimeMillis();
		System.out.println("Math3 method:" + (endTime - startTime));
		startTime = System.currentTimeMillis();
		 solution = SimpleLinearSystemSolverFactory.solve(knownTerm, covMatrix, "prec");
		endTime = System.currentTimeMillis();
		System.out.println("Prec method:" + (endTime - startTime));
//		startTime = System.currentTimeMillis();
//		 solution = SimpleLinearSystemSolverFactory.solve(knownTerm, covMatrix, "Cholesky");
//		endTime = System.currentTimeMillis();
//		System.out.println("Cholesky method:" + (endTime - startTime));
	}

	public static double[] generateRandomArray(int length, double min, double max) {
		double[] array = new double[length];
		Random rand = new Random();
		for (int i = 0; i < length; i++) {
			array[i] = min + (max - min) * rand.nextDouble();
		}
		return array;
	}
}
