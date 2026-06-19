package org.hortonmachine.hmachine.kriging;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.hmachine.modules.statistics.kriging.primarylocation.StationsSelection;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;



public class StationsSelectionTest {

    /**
     * Test the execute() method without triggering neighbor selection.
     */
    @Test
    public void testExecuteWithoutNeighborSelection() throws Exception {
        // Build a SimpleFeatureType with a geometry, station id, and z attribute.
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("Station");
        typeBuilder.add("the_geom", Point.class, DefaultGeographicCRS.WGS84);
        typeBuilder.add("id", Integer.class);
        typeBuilder.add("z", Double.class);
        final SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

        // Create a feature collection and a geometry factory.
        DefaultFeatureCollection collection = new DefaultFeatureCollection("internal", TYPE);
        GeometryFactory geomFactory = new GeometryFactory();

        // Create three synthetic features.
        SimpleFeature feature1 = SimpleFeatureBuilder.build(TYPE,
                new Object[] { geomFactory.createPoint(new Coordinate(1, 1)), 1, 10.0 }, "fid.1");
        SimpleFeature feature2 = SimpleFeatureBuilder.build(TYPE,
                new Object[] { geomFactory.createPoint(new Coordinate(2, 2)), 2, 20.0 }, "fid.2");
        SimpleFeature feature3 = SimpleFeatureBuilder.build(TYPE,
                new Object[] { geomFactory.createPoint(new Coordinate(3, 3)), 3, 30.0 }, "fid.3");

        collection.add(feature1);
        collection.add(feature2);
        collection.add(feature3);

        // Create inData mapping station id to measured values.
        HashMap<Integer, double[]> inData = new HashMap<>();
        inData.put(1, new double[] { 100.0 });
        inData.put(2, new double[] { 200.0 });
        inData.put(3, new double[] { 300.0 });

        // Set up StationsSelection with neighbor selection turned off.
        StationsSelection stationsSelection = new StationsSelection();
        stationsSelection.inStations = collection;
        stationsSelection.inData = inData;
        stationsSelection.doIncludezero = true;
        stationsSelection.doLogarithmic = false;
        stationsSelection.maxdist = 0;          // No neighbor selection.
        stationsSelection.inNumCloserStations = 0; // No neighbor selection.
        stationsSelection.fStationsid = "id";
        stationsSelection.fStationsZ = "z";
        // Set reference point for distance (not used here as neighbor selection is off).
        stationsSelection.idx = 0;
        stationsSelection.idy = 0;

        // Execute the station selection.
        stationsSelection.execute();

        // Check that the arrays have been created.
        // The arrays should have size = nStaz + 1 (nStaz is the number of valid stations)
        // In this test, we expect 3 valid stations -> arrays of length 4.
        assertNotNull(stationsSelection.xStationInitialSet);
        assertEquals(4, stationsSelection.xStationInitialSet.length);
        assertEquals(4, stationsSelection.yStationInitialSet.length);
        assertEquals(4, stationsSelection.zStationInitialSet.length);
        assertEquals(4, stationsSelection.hStationInitialSet.length);
        assertEquals(4, stationsSelection.idStationInitialSet.length);

        // Validate that the first element corresponds to the first feature.
        // Expected: feature1 (x=1, y=1, z=10, h=100, id=1)
        assertEquals(1.0, stationsSelection.xStationInitialSet[0], 1e-6);
        assertEquals(1.0, stationsSelection.yStationInitialSet[0], 1e-6);
        assertEquals(10.0, stationsSelection.zStationInitialSet[0], 1e-6);
        assertEquals(100.0, stationsSelection.hStationInitialSet[0], 1e-6);
        assertEquals(1, stationsSelection.idStationInitialSet[0]);

        // Verify that the counter for valid stations (n1) is set correctly.
        assertEquals(3, stationsSelection.n1);
    }

    /**
     * Test the execute() method with neighbor selection enabled.
     * This test sets inNumCloserStations and maxdist so that neighbor selection is triggered.
     */
    @Test
    public void testExecuteWithNeighborSelection() throws Exception {
        // Build a SimpleFeatureType with a geometry, station id, and z attribute.
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("Station");
        typeBuilder.add("the_geom", Point.class, DefaultGeographicCRS.WGS84);
        typeBuilder.add("id", Integer.class);
        typeBuilder.add("z", Double.class);
        final SimpleFeatureType TYPE = typeBuilder.buildFeatureType();

        // Create a feature collection and a geometry factory.
        DefaultFeatureCollection collection = new DefaultFeatureCollection("internal", TYPE);
        GeometryFactory geomFactory =new GeometryFactory();

        // Create three synthetic features with distinct coordinates.
        SimpleFeature feature1 = SimpleFeatureBuilder.build(TYPE,
                new Object[] { geomFactory.createPoint(new Coordinate(1, 1)), 1, 10.0 }, "fid.1");
        SimpleFeature feature2 = SimpleFeatureBuilder.build(TYPE,
                new Object[] { geomFactory.createPoint(new Coordinate(2, 2)), 2, 20.0 }, "fid.2");
        SimpleFeature feature3 = SimpleFeatureBuilder.build(TYPE,
                new Object[] { geomFactory.createPoint(new Coordinate(3, 3)), 3, 30.0 }, "fid.3");

        collection.add(feature1);
        collection.add(feature2);
        collection.add(feature3);

        // Create inData mapping station id to measured values.
        HashMap<Integer, double[]> inData = new HashMap<>();
        inData.put(1, new double[] { 100.0 });
        inData.put(2, new double[] { 200.0 });
        inData.put(3, new double[] { 300.0 });

        // Set up StationsSelection with neighbor selection enabled.
        StationsSelection stationsSelection = new StationsSelection();
        stationsSelection.inStations = collection;
        stationsSelection.inData = inData;
        stationsSelection.doIncludezero = true;
        stationsSelection.doLogarithmic = false;
        // Set parameters to trigger neighbor selection:
        //stationsSelection.maxdist = 1000;           // a large distance to include all stations
        stationsSelection.inNumCloserStations = 2;    // select only the two closest stations
        stationsSelection.fStationsid = "id";
        stationsSelection.fStationsZ = "z";
        // Set a reference point (e.g., origin) to compute distances.
        stationsSelection.idx = 0;
        stationsSelection.idy = 0;

        // Execute the station selection.
        stationsSelection.execute();

        // In this case, neighbor selection is triggered.
        // We expect the arrays to be re-created with a reduced number of stations.
        // The new arrays are built with dimension = (numberOfStations from modelSelection) + 1.
        // For our synthetic data, the two closest stations (to (0,0)) are feature1 and feature2.
        // Therefore, the arrays should have a length of 2 + 1 = 3.
        assertNotNull(stationsSelection.xStationInitialSet);
        assertEquals(3, stationsSelection.xStationInitialSet.length);
        assertEquals(3, stationsSelection.yStationInitialSet.length);
        assertEquals(3, stationsSelection.zStationInitialSet.length);
        assertEquals(3, stationsSelection.hStationInitialSet.length);
        assertEquals(3, stationsSelection.idStationInitialSet.length);

        // Optionally, check that the selected stations are the ones with the smallest distances.
        // Since distances from (0,0): feature1 = sqrt(2) ≈ 1.41, feature2 ≈ 2.83, feature3 ≈ 4.24,
        // we expect feature1 and feature2 to be selected.
        // We check that the first two values correspond to feature1 and feature2.
        // (The ordering depends on the sorting algorithm implementation.)
        double x0 = stationsSelection.xStationInitialSet[0];
        double x1 = stationsSelection.xStationInitialSet[1];
        // Expect x0 and x1 to be 1 and 2 (or vice versa)
        boolean condition = ((Math.abs(x0 - 1.0) < 1e-6 && Math.abs(x1 - 2.0) < 1e-6) ||
                             (Math.abs(x0 - 2.0) < 1e-6 && Math.abs(x1 - 1.0) < 1e-6));
        assertTrue( "Expected the two closest stations to be selected",condition);
    }
}

