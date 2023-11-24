package org.hortonmachine.gears;

import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.h2.command.dml.Merge;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.MergeMode;
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
                {NaN, 1, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, 1, 1, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};
        double[][] map1 = new double[][]{//
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}};
        double[][] map2 = new double[][]{//
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}};
        double[][] map3 = new double[][]{//
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
                {0, 1, 2, 0, 0, 0, 0, 0, 0, 0}, //
                {0, 3, 1, 0, 0, 0, 0, 0, 0, 0}, //
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, //
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};

        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        GridCoverage2D initialGC = CoverageUtilities.buildCoverageWithNovalue("init", initial, envelopeParams, crs, true, NaN);
        GridCoverage2D m1GC = CoverageUtilities.buildCoverageWithNovalue("m1", map1, envelopeParams, crs, true, NaN);
        GridCoverage2D m2GC = CoverageUtilities.buildCoverageWithNovalue("m2", map2, envelopeParams, crs, true, NaN);
        GridCoverage2D m3GC = CoverageUtilities.buildCoverageWithNovalue("m3", map3, envelopeParams, crs, true, NaN);

        MergeMode mode = MergeMode.SUM;
        HMRaster workingRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(initialGC).setCopyValues(true).build();
        HMRaster m1R = HMRaster.fromGridCoverage(m1GC);
        HMRaster m2R = HMRaster.fromGridCoverage(m2GC);
        workingRaster.mapRaster(null, m1R, mode);
        workingRaster.mapRaster(null, m2R, mode);
        double[][] expected = new double[][]{//
            {8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, //
            {8, 9, 9, 8, 8, 8, 8, 8, 8, 8}, //
            {8, 9, 9, 8, 8, 8, 8, 8, 8, 8}, //
            {8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, //
            {8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, //
            {8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, //
            {8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, //
            {8, 8, 8, 8, 8, 8, 8, 8, 8, 8}};
            checkMatrixEqual(workingRaster.buildCoverage().getRenderedImage(), expected, DELTA);
            
        mode = MergeMode.AVG;
        initialGC = CoverageUtilities.buildCoverageWithNovalue("init", initial, envelopeParams, crs, true, NaN);
        workingRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(initialGC).setCopyValues(true).build();
        workingRaster.mapRaster(null, m1R, mode);
        workingRaster.mapRaster(null, m2R, mode);
        expected = new double[][]{//
                {4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, //
                {4, 3, 3, 4, 4, 4, 4, 4, 4, 4}, //
                {4, 3, 3, 4, 4, 4, 4, 4, 4, 4}, //
                {4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, //
                {4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, //
                {4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, //
                {4, 4, 4, 4, 4, 4, 4, 4, 4, 4}, //
                {4, 4, 4, 4, 4, 4, 4, 4, 4, 4}};
        checkMatrixEqual(workingRaster.buildCoverage().getRenderedImage(), expected, DELTA);
       
        mode = MergeMode.SUBSTITUTE;
        initialGC = CoverageUtilities.buildCoverageWithNovalue("init", initial, envelopeParams, crs, true, NaN);
        workingRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(initialGC).setCopyValues(true).build();
        workingRaster.mapRaster(null, m1R, mode);
        workingRaster.mapRaster(null, m2R, mode);
        expected = new double[][]{//
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}};
        checkMatrixEqual(workingRaster.buildCoverage().getRenderedImage(), expected, DELTA);
        
        mode = MergeMode.INSERT_ON_NOVALUE;
        initialGC = CoverageUtilities.buildCoverageWithNovalue("init", initial, envelopeParams, crs, true, NaN);
        workingRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(initialGC).setCopyValues(true).build();
        workingRaster.mapRaster(null, m1R, mode);
        workingRaster.mapRaster(null, m2R, mode);
        expected = new double[][]{//
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 1, 1, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 1, 1, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}};
        checkMatrixEqual(workingRaster.buildCoverage().getRenderedImage(), expected, DELTA);

        mode = MergeMode.MOST_POPULAR_VALUE;
        initialGC = CoverageUtilities.buildCoverageWithNovalue("init", initial, envelopeParams, crs, true, NaN);
        workingRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(initialGC).setCopyValues(true).build();
        workingRaster.mapRaster(null, m1R, mode);
        workingRaster.mapRaster(null, m2R, mode);
        HMRaster m3R = HMRaster.fromGridCoverage(m3GC);
        workingRaster.mapRaster(null, m3R, mode);
        expected = new double[][]{//
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 1, 1, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 1, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}, //
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3}};
        checkMatrixEqual(workingRaster.buildCoverage().getRenderedImage(), expected, DELTA);
    }

}
