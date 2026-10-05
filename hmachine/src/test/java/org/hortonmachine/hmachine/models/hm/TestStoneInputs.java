/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.hmachine.models.hm;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs;
import org.hortonmachine.hmachine.utils.HMTestCase;

/**
 * Test the {@link OmsStoneInputs} module.
 */
public class TestStoneInputs extends HMTestCase {

    /**
     * The sources of the crop were made in GRASS from the slope over 45 degrees of the whole DEM,
     * here from the crop: the inner cells have the same neighbours and have to agree.
     */
    public void testSourcesFromSlope() throws Exception {
        OmsStoneInputs inputs = new OmsStoneInputs();
        inputs.inElev = read("crop_dem.tif");
        inputs.pSourceSlope = 45;
        inputs.pm = pm;
        inputs.process();

        double[][] sources = toMatrix(inputs.outSources);
        double[][] expected = toMatrix(read("crop_sources.tif"));
        int both = 0, onlyOurs = 0, onlyExpected = 0;
        for( int r = 1; r < sources.length - 1; r++ ) {
            for( int c = 1; c < sources[0].length - 1; c++ ) {
                boolean ours = !Double.isNaN(sources[r][c]);
                boolean theirs = !Double.isNaN(expected[r][c]);
                if (ours) {
                    assertEquals(1.0, sources[r][c], 0.0);
                }
                if (ours && theirs) {
                    both++;
                } else if (ours) {
                    onlyOurs++;
                } else if (theirs) {
                    onlyExpected++;
                }
            }
        }
        System.out.println("Sources from slope: both " + both + ", only ours " + onlyOurs + ", only GRASS " + onlyExpected);
        double agreement = both / (double) (both + onlyOurs + onlyExpected);
        assertTrue("agreement " + agreement, agreement > 0.99);
    }

    public void testUniformCoefficientsFromDefaultClass() throws Exception {
        OmsStoneInputs inputs = new OmsStoneInputs();
        inputs.inElev = read("crop_dem.tif");
        inputs.pDefaultLithology = "Carbonate rocks";
        inputs.pm = pm;
        inputs.process();

        assertAllValues(inputs.outFriction, 0.30);
        assertAllValues(inputs.outNormalRestitution, 65);
        assertAllValues(inputs.outTangentialRestitution, 75);
    }

    public void testLithologyWithBuiltInTable() throws Exception {
        GridCoverage2D dem = read("crop_dem.tif");
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(dem);
        int rows = region.getRows();
        int cols = region.getCols();
        // left half alluvial deposits (2), right half carbonate rocks (12), a novalue row, an unknown code
        double[][] lithology = new double[rows][cols];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                lithology[r][c] = c < cols / 2 ? 2 : 12;
            }
        }
        java.util.Arrays.fill(lithology[10], Double.NaN);
        lithology[20][5] = 99;

        OmsStoneInputs inputs = new OmsStoneInputs();
        inputs.inElev = dem;
        inputs.inLithology = CoverageUtilities.buildCoverageWithNovalue("lithology", lithology, region,
                dem.getCoordinateReferenceSystem(), true, Double.NaN);
        inputs.pDefaultLithology = "Flysch";
        inputs.pm = pm;
        inputs.process();

        double[][] friction = toMatrix(inputs.outFriction);
        double[][] normal = toMatrix(inputs.outNormalRestitution);
        double[][] tangential = toMatrix(inputs.outTangentialRestitution);
        // alluvial deposits, with the tangential restitution of the docs table
        assertEquals(0.80, friction[0][0], 1E-9);
        assertEquals(15, normal[0][0], 0.0);
        assertEquals(40, tangential[0][0], 0.0);
        // carbonate rocks
        assertEquals(0.30, friction[0][cols - 1], 1E-9);
        assertEquals(65, normal[0][cols - 1], 0.0);
        assertEquals(75, tangential[0][cols - 1], 0.0);
        // missing lithology and unknown code: the default class
        assertEquals(0.40, friction[10][0], 1E-9);
        assertEquals(55, normal[10][0], 0.0);
        assertEquals(65, tangential[20][5], 0.0);
    }

    public void testCustomTable() throws Exception {
        GridCoverage2D dem = read("crop_dem.tif");
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(dem);
        double[][] lithology = new double[region.getRows()][region.getCols()];
        for( double[] row : lithology ) {
            java.util.Arrays.fill(row, 7);
        }
        File table = File.createTempFile("stone_lithology_", ".csv");
        try {
            Files.write(table.toPath(), List.of("# my classes", "code,friction,nrest,trest", "7, 0.55, 42, 61"));
            OmsStoneInputs inputs = new OmsStoneInputs();
            inputs.inElev = dem;
            inputs.inLithology = CoverageUtilities.buildCoverageWithNovalue("lithology", lithology, region,
                    dem.getCoordinateReferenceSystem(), true, Double.NaN);
            inputs.inLithologyTable = table.getAbsolutePath();
            inputs.pm = pm;
            inputs.process();
            assertAllValues(inputs.outFriction, 0.55);
            assertAllValues(inputs.outNormalRestitution, 42);
            assertAllValues(inputs.outTangentialRestitution, 61);
        } finally {
            table.delete();
        }
    }

    public void testExistingSourcesAndStopAreas() throws Exception {
        GridCoverage2D dem = read("crop_dem.tif");
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(dem);
        double[][] stop = new double[region.getRows()][region.getCols()];
        for( double[] row : stop ) {
            java.util.Arrays.fill(row, Double.NaN);
        }
        // a lake in the first rows
        for( int r = 0; r < 5; r++ ) {
            java.util.Arrays.fill(stop[r], 1);
        }

        OmsStoneInputs inputs = new OmsStoneInputs();
        inputs.inElev = dem;
        inputs.inSources = read("crop_sources.tif");
        inputs.inStopAreas = CoverageUtilities.buildCoverageWithNovalue("stop", stop, region,
                dem.getCoordinateReferenceSystem(), true, Double.NaN);
        inputs.pm = pm;
        inputs.process();

        double[][] sources = toMatrix(inputs.outSources);
        double[][] given = toMatrix(read("crop_sources.tif"));
        for( int r = 0; r < sources.length; r++ ) {
            for( int c = 0; c < sources[0].length; c++ ) {
                if (r < 5) {
                    assertEquals(-1.0, sources[r][c], 0.0);
                } else if (Double.isNaN(given[r][c])) {
                    assertTrue(Double.isNaN(sources[r][c]));
                } else {
                    assertEquals(given[r][c], sources[r][c], 0.0);
                }
            }
        }
    }

    /**
     * From the DEM only to a simulation.
     */
    public void testFromDemToStone() throws Exception {
        OmsStoneInputs inputs = new OmsStoneInputs();
        inputs.inElev = read("crop_dem.tif");
        inputs.pDefaultLithology = "Consolidated clastic deposits";
        inputs.pm = pm;
        inputs.process();

        OmsStone stone = new OmsStone();
        stone.inElev = inputs.inElev;
        stone.inSources = inputs.outSources;
        stone.inNormalRestitution = inputs.outNormalRestitution;
        stone.inTangentialRestitution = inputs.outTangentialRestitution;
        stone.inFriction = inputs.outFriction;
        stone.pStopVelocity = 3;
        stone.pm = pm;
        stone.process();

        double sum = 0;
        for( double[] row : toMatrix(stone.outCounter) ) {
            for( double value : row ) {
                if (!Double.isNaN(value)) {
                    sum += value;
                }
            }
        }
        assertTrue("counts " + sum, sum > 10000);
    }

    private static void assertAllValues( GridCoverage2D coverage, double expected ) throws Exception {
        int count = 0;
        for( double[] row : toMatrix(coverage) ) {
            for( double value : row ) {
                if (!Double.isNaN(value)) {
                    assertEquals(expected, value, 1E-9);
                    count++;
                }
            }
        }
        assertTrue(count > 0);
    }

    private static double[][] toMatrix( GridCoverage2D coverage ) throws Exception {
        try (HMRaster raster = HMRaster.fromGridCoverage(coverage)) {
            double[][] matrix = new double[raster.getRows()][raster.getCols()];
            for( int r = 0; r < raster.getRows(); r++ ) {
                for( int c = 0; c < raster.getCols(); c++ ) {
                    double value = raster.getValue(c, r);
                    matrix[r][c] = raster.isNovalue(value) ? Double.NaN : value;
                }
            }
            return matrix;
        }
    }

    private GridCoverage2D read( String name ) throws Exception {
        URL url = this.getClass().getClassLoader().getResource("Input/stone/" + name);
        return OmsRasterReader.readRaster(new File(url.toURI()).getAbsolutePath());
    }
}
