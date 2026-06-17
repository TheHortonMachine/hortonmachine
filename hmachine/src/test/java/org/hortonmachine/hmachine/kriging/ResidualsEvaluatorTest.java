package org.hortonmachine.hmachine.kriging;



import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.ResidualsEvaluator;
import org.junit.Test;

public class ResidualsEvaluatorTest {

	@Test
	public void testNoDetrending() {
		// Synthetic data arrays
		double[] zStations = { 1, 2, 3 };
		double[] hStations = { 10, 20, 30 };
		boolean doDetrended = false;
		int regressionOrder = 1;

		ResidualsEvaluator evaluator = ResidualsEvaluator.create(zStations, hStations, doDetrended, regressionOrder);

		// With no detrending, hResiduals should equal hStations and trend parameters
		// remain 0.
		assertArrayEquals(hStations, evaluator.hResiduals, 1e-6);
		assertEquals(0, evaluator.trendIntercept, 1e-6);
		assertEquals(0, evaluator.trendCoefficient, 1e-6);
		assertFalse(evaluator.isPValueOk);
	}
	
	// TODO disabled due to residual evaluator not yet properly ported.
//	@Test
//	public void testSignificantTrend() {
//		// Generate synthetic data with a clear linear trend: h = 2 + 3*z + small noise.
//		int n = 10;
//		double[] zStations = new double[n];
//		double[] hStations = new double[n];
//		double trueIntercept = 2.0;
//		double trueCoefficient = 3.0;
//
//		for (int i = 0; i < n; i++) {
//			zStations[i] = i;
//			// Adding small noise
//			hStations[i] = trueIntercept + trueCoefficient * i + 0.1 * (Math.random() - 0.5);
//		}
//
//		boolean doDetrended = true;
//		int regressionOrder = 1;
//		ResidualsEvaluator evaluator = ResidualsEvaluator.create(zStations, hStations, doDetrended, regressionOrder);
//
//		// Expect a statistically significant trend.
//		assertTrue("Expected significant trend (p-value < 0.05)",evaluator.isPValueOk);
//
//		// Check that the estimated intercept and coefficient are close to the true
//		// values.
//		assertEquals(trueIntercept, evaluator.trendIntercept, 0.5);
//		assertEquals(trueCoefficient, evaluator.trendCoefficient, 0.5);
//
//		// Verify residuals are approximately the difference between observed values and
//		// the predicted trend.
//		for (int i = 0; i < n; i++) {
//			double predicted = evaluator.trendIntercept + evaluator.trendCoefficient * zStations[i];
//			double expectedResidual = hStations[i] - predicted;
//			assertEquals(expectedResidual, evaluator.hResiduals[i], 0.5);
//		}
//	}
//
//	@Test
//	public void testNonSignificantTrend() {
//		// Generate synthetic data with no clear trend: random hStations independent of
//		// zStations.
//		int n = 10;
//		double[] zStations = new double[n];
//		double[] hStations = new double[n];
//
//		for (int i = 0; i < n; i++) {
//			zStations[i] = i;
//			hStations[i] = Math.random() * 10; // No correlation with zStations.
//		}
//
//		boolean doDetrended = true;
//		int regressionOrder = 1;
//		ResidualsEvaluator evaluator = ResidualsEvaluator.create(zStations, hStations, doDetrended, regressionOrder);
//
//		// In case the regression is not significant, the evaluator should return the
//		// original hStations.
//		if (!evaluator.isPValueOk) {
//			assertArrayEquals(hStations, evaluator.hResiduals, 1e-6);
//			assertEquals(0, evaluator.trendIntercept, 1e-6);
//			assertEquals(0, evaluator.trendCoefficient, 1e-6);
//		} else {
//			// If by chance the random data shows a significant trend, output a message.
//			System.out.println("Random data unexpectedly produced a significant trend.");
//		}
//	}
}
