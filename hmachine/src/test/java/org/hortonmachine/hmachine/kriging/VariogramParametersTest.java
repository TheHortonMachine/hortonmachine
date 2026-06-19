package org.hortonmachine.hmachine.kriging;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical.VariogramParameters;
import org.junit.Test;

public class VariogramParametersTest {

	// Dummy HMConstants for testing purposes.
	public static class HMConstants {
		public static final double doubleNovalue = -9999.0;
	}

	@Test
	public void testConstructorWithString() {
		// Create an instance using the constructor with (String, double, double,
		// double)
		VariogramParameters vp = new VariogramParameters.Builder("exponential", 0.1, 100.0, 150.0).build();
		assertEquals("exponential", vp.getModelName());
		assertEquals(0.1, vp.getNugget(), 1e-6);
		assertEquals(100.0, vp.getRange(), 1e-6);
		assertEquals(150.0, vp.getSill(), 1e-6);

		// By default, isTrend and isLocal should be false.
		assertFalse(vp.getIsTrend());
		assertFalse(vp.getIsLocal());

		// Since parameters are non-negative and modelName is not null, isValid() should
		// return true.
		assertTrue(vp.isValid());
	}

//    @Test
//    public void testConstructorWithDouble() {
//        // Using the constructor with (double, double, double, double)
//        // Passing 1.0 as the first parameter means modelName should be availableTheorethicalVariogra[1] ("linear")
//        VariogramParameters vp = new VariogramParameters.Builder(1.0, 0.2, 120.0, 180.0).build();
//        assertEquals("linear", vp.getModelName());
//        assertEquals(0.2, vp.getNugget(), 1e-6);
//        assertEquals(120.0, vp.getRange(), 1e-6);
//        assertEquals(180.0, vp.getSill(), 1e-6);
//    }

	@Test
	public void testToHashMapWithValidModel() {
		// Create an instance with a valid model name.
		VariogramParameters vp = new VariogramParameters.Builder("spherical", 0.5, 80.0, 130.0).setLocal(true)
				.setTrend(true).build();

		HashMap<Integer, double[]> paramsMap = vp.toHashMap();
		assertNotNull(paramsMap);

		// Expected mapping:
		// 0 -> nugget, 1 -> sill, 2 -> range, 3 -> isLocal flag (0.0 if true, else
		// 1.0),
		// 4 -> isTrend flag (0.0 if true, else 1.0), 5 -> variogram code.
		assertEquals(0.5, paramsMap.get(0)[0], 1e-6);
		assertEquals(130.0, paramsMap.get(1)[0], 1e-6);
		assertEquals(80.0, paramsMap.get(2)[0], 1e-6);
		// For isLocal true, we expect 1.0; for isTrend true, we expect 0.0.
		assertEquals(1.0, paramsMap.get(3)[0], 1e-6);
		assertEquals(1.0, paramsMap.get(4)[0], 1e-6);
		// "spherical" is at index 3 in availableTheorethicalVariogra.
		assertEquals(3, paramsMap.get(5)[0], 1e-6);
	}

//    @Test
//    public void testToHashMapWithInvalidModel() {
//        // Use the default constructor, which leaves modelName null.
//        VariogramParameters vp = new VariogramParameters();
//        HashMap<Integer, double[]> paramsMap = vp.toHashMap();
//
//        // All entries should be set to HMConstants.doubleNovalue.
//        for (int i = 0; i < 6; i++) {
//            assertEquals(HMConstants.doubleNovalue, paramsMap.get(i)[0], 1e-6);
//        }
//    }

	@Test
	public void testGetVariogramCode() {
		// Check known model names.
		assertEquals(0, VariogramParameters.getVariogramCode("exponential"));
		assertEquals(1, VariogramParameters.getVariogramCode("linear"));
		assertEquals(2, VariogramParameters.getVariogramCode("power"));
		assertEquals(3, VariogramParameters.getVariogramCode("spherical"));
		// Unknown model should return -9999.
		assertEquals(-9999, VariogramParameters.getVariogramCode("unknown"));
	}

	@Test
	public void testGetVariogramType() {
		// Test getVariogramType by passing a double value.
		assertEquals("exponential", VariogramParameters.getVariogramType(0.0));
		assertEquals("linear", VariogramParameters.getVariogramType(1.0));
		assertEquals("power", VariogramParameters.getVariogramType(2.0));
		assertEquals("spherical", VariogramParameters.getVariogramType(3.0));
	}

	@Test
	public void testIsValid() {
		// Instance with valid parameters.
		VariogramParameters vpValid = new VariogramParameters.Builder("linear", 0.0, 50.0, 75.0).build();
		assertTrue(vpValid.isValid());

		// Instance with a negative nugget should be invalid.
		VariogramParameters vpNeg = new VariogramParameters.Builder("linear", -1.0, 50.0, 75.0).build();
		assertFalse(vpNeg.isValid());

		// Instance with a null modelName should be invalid.
		VariogramParameters vpNull = new VariogramParameters.Builder(null, 0.0, 50.0, 75.0).build();
		assertFalse(vpNull.isValid());
	}
}