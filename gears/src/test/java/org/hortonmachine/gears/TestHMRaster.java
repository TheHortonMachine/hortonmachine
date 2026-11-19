package org.hortonmachine.gears;

import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.MergeMode;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterZones;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
/**
 * Test HMRaster.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestHMRaster extends HMTestCase {
    private static final double NaN = HMConstants.doubleNovalue;

    private GridCoverage2D inElev;

    protected void setUp() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
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
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
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

    /**
     * Maps a UTM raster on an Albers raster, rotated by about 10 degrees: each cell has to take the value of the UTM cell
     * containing its centre.
     */
    public void testMappingOtherCrs() throws Exception {
        CoordinateReferenceSystem utm = HMCrsRegistry.INSTANCE.getCrs("EPSG:32612");
        CoordinateReferenceSystem albers = HMCrsRegistry.INSTANCE.getCrs("EPSG:5070");
        int utmCols = 200;
        int utmRows = 200;
        RegionMap utmRegion = RegionMap.fromBoundsAndGrid(500000, 506000, 5050000, 5056000, utmCols, utmRows);
        HMRaster source = new HMRaster.HMRasterWritableBuilder().setRegion(utmRegion).setCrs(utm).build();
        for( int r = 0; r < utmRows; r++ ) {
            for( int c = 0; c < utmCols; c++ ) {
                source.setValue(c, r, c + r * utmCols);
            }
        }

        // an albers grid larger than the utm one, so that it also has cells outside of it
        Envelope env = new HMCrsTransformer(utm, albers).transformDensified(utmRegion.toEnvelope(), 20);
        env.expandBy(600);
        int cols = (int) Math.ceil(env.getWidth() / 60);
        int rows = (int) Math.ceil(env.getHeight() / 60);
        RegionMap region = RegionMap.fromBoundsAndGrid(env.getMinX(), env.getMinX() + cols * 60, env.getMinY(),
                env.getMinY() + rows * 60, cols, rows);
        HMRaster target = new HMRaster.HMRasterWritableBuilder().setRegion(region).setCrs(albers).build();
        target.mapRaster(null, source, MergeMode.SUBSTITUTE);

        HMCrsTransformer toUtm = new HMCrsTransformer(albers, utm);
        int inside = 0;
        int outside = 0;
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                Coordinate centre = toUtm.transform(target.getWorld(c, r));
                int utmCol = (int) Math.floor((centre.x - 500000) / 30);
                int utmRow = (int) Math.floor((5056000 - centre.y) / 30);
                double value = target.getValue(c, r);
                if (utmCol >= 0 && utmCol < utmCols && utmRow >= 0 && utmRow < utmRows) {
                    assertEquals("cell " + c + "/" + r, utmCol + utmRow * utmCols, value, DELTA);
                    inside++;
                } else {
                    assertTrue("cell " + c + "/" + r + " outside of the utm raster", target.isNovalue(value));
                    outside++;
                }
            }
        }
        assertTrue(inside > 9000);
        assertTrue(outside > 0);
    }

	public void testSubraster() throws Exception {
		double[][] expected = new double[][] { //
				{ 800, 900, 1000, 1000, 1200 }, //
				{ 600, NaN, 750, 850, 860 }, //
				{ 500, 550, 700, 750, 800 }, //
				{ 400, 410, 650, 700, 750 }, //
				{ 450, 550, 430, 500, 600 }, //
				{ 500, 600, 700, 750, 760 }, //
				{ 600, 700, 750, 800, 780 }, //
				{ 800, 910, 980, 1001, 1150 } };
		RegionMap regionMap = HMTestMaps.getEnvelopeparamsLeftHalf();
		try (HMRaster elev = HMRaster.fromGridCoverage(inElev); HMRaster subRaster = elev.toSubRaster(null, regionMap)) {
			checkMatrixEqual(subRaster.buildCoverage().getRenderedImage(), expected, DELTA);
		}

	}

	public void testPolygonExtract() throws Exception {
		double[][] expected = new double[][] { //
				{ 800, 900, 1000, 1000, 1200 }, //
				{ 600, NaN, 750, 850, 860 }, //
				{ 500, 550, 700, 750, 800 }, //
				{ 400, 410, 650, 700, 750 }, //
				{ 450, 550, 430, 500, 600 }, //
				{ 500, 600, 700, 750, 760 }, //
				{ 600, 700, 750, 800, 780 }, //
				{ 800, 910, 980, 1001, 1150 } };
		SimpleFeatureCollection fc = HMTestMaps.getTestLeftFC();
		Geometry polygon = (Geometry) DataUtilities.first(fc).getDefaultGeometry();
		try (HMRaster elev = HMRaster.fromGridCoverage(inElev);
				HMRaster subRaster = elev.extractOnPolygon(null, polygon)) {
			checkMatrixEqual(subRaster.buildCoverage().getRenderedImage(), expected, DELTA);
		}
	}
	
	public void testZonalStats() throws Exception {
		SimpleFeatureCollection fc = HMTestMaps.getTestLeftFC();
		try (HMRaster elev = HMRaster.fromGridCoverage(inElev)){
				HashMap<Integer, double[]> zonalStats = elev.getZonalStats(null, fc, "cat");
				double[] stats = zonalStats.get(1);
				assertNotNull(stats);
				assertEquals(400, stats[0], DELTA);
				assertEquals(1200, stats[1], DELTA);
				assertEquals(730.2820512820513, stats[2], DELTA);
				assertEquals(28481.0, stats[3], DELTA);
				assertEquals(39, stats[4], DELTA);
		}
	}

	public void testZonalStatsWithReusedZones() throws Exception {
		SimpleFeatureCollection fc = HMTestMaps.getTestLeftFC();
		try (HMRaster elev = HMRaster.fromGridCoverage(inElev);
				HMRaster doubled = new HMRaster.HMRasterWritableBuilder().setTemplate(elev).setCopyValues(true).build()) {
			HMRasterZones zones = elev.rasterizeZones(null, fc, "cat");
			assertTrue(zones.getZoneIds().contains(1));

			// the same stats of the zonal stats of the polygons
			double[] stats = elev.getZonalStats(null, zones).get(1);
			assertEquals(400, stats[0], DELTA);
			assertEquals(1200, stats[1], DELTA);
			assertEquals(730.2820512820513, stats[2], DELTA);
			assertEquals(28481.0, stats[3], DELTA);
			assertEquals(39, stats[4], DELTA);

			// the zones reused on another raster with the same grid
			for (int row = 0; row < doubled.getRows(); row++) {
				for (int col = 0; col < doubled.getCols(); col++) {
					double value = doubled.getValue(col, row);
					if (!doubled.isNovalue(value)) {
						doubled.setValue(col, row, value * 2);
					}
				}
			}
			stats = doubled.getZonalStats(null, zones).get(1);
			assertEquals(800, stats[0], DELTA);
			assertEquals(2400, stats[1], DELTA);
			assertEquals(2 * 730.2820512820513, stats[2], DELTA);
			assertEquals(2 * 28481.0, stats[3], DELTA);
			assertEquals(39, stats[4], DELTA);
		}

		// the zones can't be used on a different grid
		RegionMap region = HMTestMaps.getEnvelopeparams();
		double halfCell = region.getXres() / 2;
		RegionMap shifted = RegionMap.fromBoundsAndGrid(region.getWest() + halfCell, region.getEast() + halfCell,
				region.getSouth(), region.getNorth(), region.getCols(), region.getRows());
		try (HMRaster elev = HMRaster.fromGridCoverage(inElev);
				HMRaster other = new HMRaster.HMRasterWritableBuilder().setRegion(shifted).setCrs(elev.getCrs()).build()) {
			HMRasterZones zones = elev.rasterizeZones(null, fc, "cat");
			try {
				other.getZonalStats(null, zones);
				fail("Zones on another grid must be refused.");
			} catch (ModelsIllegalargumentException e) {
				// expected
			}
		}
	}
	
	
    

}
