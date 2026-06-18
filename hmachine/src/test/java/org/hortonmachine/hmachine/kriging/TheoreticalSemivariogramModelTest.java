package org.hortonmachine.hmachine.kriging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.Exponential;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.Gaussian;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.Linear;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.Logarithmic;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.Model;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.Power;
import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.model.Spherical;
import org.junit.Test;

public class TheoreticalSemivariogramModelTest {
	double dist1 = 5.0;
	double dist2 = 25.0;

	double sill = 10.0;
	double range = 15.0;
	double nug = 1.0;
	private static final double EPSILON = 1e-2;

	// Helper method to check that the gradient array is valid.
	private void assertValidGradient(Model model) {
		double[] gradient = model.computeGradient();
		assertNotNull("Il gradient non deve essere null", gradient);
		assertEquals("Il gradient deve contenere 3 elementi", 3, gradient.length);
	}

	@Test
	public void testLinearSemivariance() {

		Linear linear = new Linear(dist1, sill, range, nug);
		double computed = linear.computeSemivariance();
		assertEquals("Linear semivariance did not match expected value", 4.33, computed, EPSILON);
		linear = new Linear(dist2, sill, range, nug);
		computed = linear.computeSemivariance();
		assertEquals("Linear semivariance did not match expected value", 11, computed, EPSILON);
		assertValidGradient(linear);
	}

//    @Test
//    public void testBesselSemivariance() {
//        double dist = 5.0;
//        double sill = 10.0;
//        double range = 15.0;
//        double nug = 1.0;
//        Bessel bessel = new Bessel(dist, sill, range, nug);
//        double computed = bessel.computeSemivariance();
//        // Instead of assertNotEquals, use assertFalse with Double.compare
//        assertFalse("Bessel semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
//        assertValidGradient(bessel);
//    }

//    @Test
//    public void testCircularSemivariance() {
//        double dist = 5.0;
//        double sill = 10.0;
//        double range = 15.0;
//        double nug = 1.0;
//        Circular circular = new Circular(dist, sill, range, nug);
//        double computed = circular.computeSemivariance();
//        assertFalse("Circular semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
//        assertValidGradient(circular);
//    }

	@Test
	public void testExponentialSemivariance() {
		Exponential exponential = new Exponential(dist1, sill, range, nug);
		double computed = exponential.computeSemivariance();
		assertEquals("Exponential semivariance did not match expected value", 3.834, computed, EPSILON);
		exponential = new Exponential(dist2, sill, range, nug);
		computed = exponential.computeSemivariance();
		assertEquals("Exponential semivariance did not match expected value", 9.111, computed, EPSILON);
		assertFalse("Exponential semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
		assertValidGradient(exponential);
	}

	@Test
	public void testGaussianSemivariance() {
		Gaussian gaussian = new Gaussian(dist1, sill, range, nug);
		double computed = gaussian.computeSemivariance();
		assertFalse("Gaussian semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
		assertEquals("Gausian semivariance did not match expected value", 2.051, computed, EPSILON);

		gaussian = new Gaussian(dist2, sill, range, nug);
		computed = gaussian.computeSemivariance();
		assertFalse("Gaussian semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
		assertEquals("Gaussian semivariance did not match expected value", 10.378, computed, EPSILON);

		assertValidGradient(gaussian);
	}

//    @Test
//    public void testHoleSemivariance() {
//        double dist = 5.0;
//        double sill = 8.0;
//        double range = 12.0;
//        double nug = 0.0;
//        Hole hole = new Hole(dist, sill, range, nug);
//        double computed = hole.computeSemivariance();
//        assertFalse("Hole semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
//        assertValidGradient(hole);
//    }

	@Test
	public void testLogarithmicSemivariance() {
		double dist = 5.0;
		double sill = 7.0;
		double range = 10.0;
		double nug = 0.5;
		Logarithmic logarithmic = new Logarithmic(dist, sill, range, nug);
		double computed = logarithmic.computeSemivariance();
		assertFalse("Logarithmic semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
		assertValidGradient(logarithmic);
	}

//    @Test
//    public void testPhentaspheriacalSemivariance() {
//        double dist = 5.0;
//        double sill = 9.0;
//        double range = 14.0;
//        double nug = 1.0;
//        Pentaspherical pentaspheriacal = new Pentaspherical(dist, sill, range, nug);
//        double computed = pentaspheriacal.computeSemivariance();
//        assertFalse("Phentaspheriacal semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
//        assertValidGradient(pentaspheriacal);
//    }

//    @Test
//    public void testPeriodicSemivariance() {
//        double dist = 5.0;
//        double sill = 11.0;
//        double range = 18.0;
//        double nug = 0.5;
//        Periodic periodic = new Periodic(dist, sill, range, nug);
//        double computed = periodic.computeSemivariance();
//        assertFalse("Periodic semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
//        assertValidGradient(periodic);
//    }

	@Test
	public void testPowerSemivariance() {
		double dist = 5.0;
		double sill = 10.0;
		double range = 15.0;
		double nug = 0.0;
		Power power = new Power(dist, sill, range, nug);
		double computed = power.computeSemivariance();
		assertFalse("Power semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
		assertValidGradient(power);
	}

	@Test
	public void testSphericalSemivariance() {
		Spherical spherical = new Spherical(dist1, sill, range, nug);
		double computed = spherical.computeSemivariance();
		assertFalse(Double.compare(Double.MAX_VALUE, computed) == 0);
		assertEquals("Spherical semivariance should be computed", 5.8148, computed, EPSILON);
		spherical = new Spherical(dist2, sill, range, nug);
		computed = spherical.computeSemivariance();
		assertFalse("Spherical semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
		assertEquals("Spherical semivariance did not match expected value", 11.0, computed, EPSILON);

		assertValidGradient(spherical);
	}

//    @Test
//    public void testSplineSemivariance() {
//        double dist = 5.0;
//        double sill = 10.0;
//        double range = 15.0;
//        double nug = 0.5;
//        Spline spline = new Spline(dist, sill, range, nug);
//        double computed = spline.computeSemivariance();
//        assertFalse("Spline semivariance should be computed", Double.compare(Double.MAX_VALUE, computed) == 0);
//        assertValidGradient(spline);
//    }

}
