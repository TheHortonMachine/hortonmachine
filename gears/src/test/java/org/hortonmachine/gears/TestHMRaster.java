package org.hortonmachine.gears;

import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test HMRaster.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestHMRaster extends HMTestCase {
    private static final double NaN = HMConstants.doubleNovalue;

    private GridCoverage2D inElev;

    protected void setUp() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        inElev = CoverageUtilities.buildCoverageWithNovalue("elevation", mapData, envelopeParams, crs, true, NaN);

    }

    public void testSurrounding() throws Exception {

        try (HMRaster elev = HMRaster.fromGridCoverage(inElev)) {
            List<Coordinate> surroundingCells = elev.getSurroundingCells(1, 1, 1, false);

            assertEquals(8, surroundingCells.size());
            assertEquals(800.0, surroundingCells.get(0).z);
            assertEquals(600.0, surroundingCells.get(3).z);
            assertEquals(550.0, surroundingCells.get(6).z);

            surroundingCells = elev.getSurroundingCells(1, 1, 2, false);
            assertEquals(15, surroundingCells.size());
            assertEquals(800.0, surroundingCells.get(0).z);
            assertEquals(1000.0, surroundingCells.get(3).z);
            assertEquals(850.0, surroundingCells.get(6).z);
            assertEquals(700.0, surroundingCells.get(14).z);

            surroundingCells = elev.getSurroundingCells(9, 7, 1, false);
            assertEquals(3, surroundingCells.size());
            assertEquals(1250.0, surroundingCells.get(0).z);
            assertEquals(1500.0, surroundingCells.get(1).z);
            assertEquals(1450.0, surroundingCells.get(2).z);

            surroundingCells = elev.getSurroundingCells(1, 1, 2, true);
            assertEquals(10, surroundingCells.size());
            assertEquals(800.0, surroundingCells.get(0).z);
            assertEquals(600.0, surroundingCells.get(3).z);
            assertEquals(500.0, surroundingCells.get(6).z);
            assertEquals(410.0, surroundingCells.get(9).z);

            surroundingCells = elev.getSurroundingCells(2, 2, 2, true);
            assertEquals(12, surroundingCells.size());
            assertEquals(1000.0, surroundingCells.get(0).z);
            assertTrue(elev.isNovalue(surroundingCells.get(1).z));
            assertEquals(850.0, surroundingCells.get(3).z);
            assertEquals(750.0, surroundingCells.get(6).z);
            assertEquals(430.0, surroundingCells.get(11).z);

            // surroundingCells.stream().forEach(System.out::println);
        }
    }

    public void testMapping() throws Exception {
        double[][] initial = new double[][]{//
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};
        double[][] map1 = new double[][]{//
                {1, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {2, 2, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};
        double[][] map2 = new double[][]{//
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, 3, 4, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, 4, 4, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        GridCoverage2D initialGC = CoverageUtilities.buildCoverageWithNovalue("init", initial, envelopeParams, crs, true, NaN);
        GridCoverage2D m1GC = CoverageUtilities.buildCoverageWithNovalue("m1", map1, envelopeParams, crs, true, NaN);
        GridCoverage2D m2GC = CoverageUtilities.buildCoverageWithNovalue("m2", map2, envelopeParams, crs, true, NaN);

        HMRaster initR = new HMRaster.HMRasterWritableBuilder().setTemplate(initialGC).setCopyValues(true).build();
        HMRaster m1R = HMRaster.fromGridCoverage(m1GC);
        HMRaster m2R = HMRaster.fromGridCoverage(m2GC);
        initR.mapRaster(null, m1R, true);
        initR.mapRaster(null, m2R, true);

        HMRaster countRaster = initR.getCountRaster();

        assertEquals(2, countRaster.getIntValue(1, 1));
        assertEquals(5.0, initR.getValue(1, 1));
        assertEquals(1, countRaster.getIntValue(2, 2));
        assertEquals(4.0, initR.getValue(2, 2));
        assertTrue(initR.isNovalue(initR.getValue(5, 5)));
        assertFalse(countRaster.isNovalue(countRaster.getValue(5, 5)));
        
        initR.applyCountAverage(null);
        
        assertEquals(2.5, initR.getValue(1, 1));
        assertEquals(4.0, initR.getValue(2, 2));
        assertTrue(initR.isNovalue(initR.getValue(5, 5)));
    }

}
