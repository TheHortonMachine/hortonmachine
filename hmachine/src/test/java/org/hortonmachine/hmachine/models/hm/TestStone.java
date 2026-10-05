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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.StoneTrajectory;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.locationtech.jts.geom.LineString;

/**
 * Test the {@link OmsStone} module.
 *
 * <p>The random numbers differ from the ones of the GRASS r.stone module, so the results are
 * compared statistically with the ones of r.stone.</p>
 */
public class TestStone extends HMTestCase {

    public void testAgainstGrassRStone() throws Exception {
        OmsStone stone = runGrassTestCase();
        double[][] counter = toMatrix(stone.outCounter);
        double[][] expected = toMatrix(read("expectedcount.tif"));
        Comparison comparison = new Comparison(counter, expected);
        System.out.println("Counter against r.stone: " + comparison);

        // all the boulders cross the source cell
        assertEquals(100.0, counter[12][13], 0.0);
        assertEquals(78, comparison.expectedCells);
        assertTrue("footprint overlap " + comparison.overlap, comparison.overlap > 0.7);
        // all the cells crossed between the recorded points are counted, r.stone only the ones of the points
        assertTrue("total count " + comparison.sum, comparison.sum >= 0.95 * comparison.expectedSum);
        assertTrue("total count " + comparison.sum, comparison.sum <= 1.25 * comparison.expectedSum);
        assertTrue("correlation " + comparison.correlation, comparison.correlation > 0.9);

        // the fastest point is in the first fall, so it is stable even with a single source
        Comparison velocities = new Comparison(toMatrix(stone.outMaxVelocity), toMatrix(read("expectedmaxvel.tif")));
        System.out.println("Max velocity against r.stone: " + velocities);
        assertEquals("max velocity", velocities.expectedMax, velocities.max, 0.2 * velocities.expectedMax);
        // the heights of the bounces of 100 boulders from a single cell vary too much between realizations
        // to be compared, they are tested on many sources in testManySourcesAgainstGrassRStone
    }

    /**
     * A 200x200 cells crop of a larger example by M. Alvioli, with one boulder from each of the
     * 13022 cells with slope above 45 degrees, against r.stone run on the same crop.
     */
    public void testManySourcesAgainstGrassRStone() throws Exception {
        OmsStone stone = new OmsStone();
        stone.inElev = read("crop_dem.tif");
        stone.inSources = read("crop_sources.tif");
        stone.inFriction = read("crop_friction.tif");
        stone.inNormalRestitution = read("crop_nrest.tif");
        stone.inTangentialRestitution = read("crop_trest.tif");
        stone.pStartVelocity = 1;
        stone.pStopVelocity = 3;
        stone.pm = pm;
        stone.process();

        Comparison comparison = new Comparison(toMatrix(stone.outCounter), toMatrix(read("crop_expectedcount.tif")));
        System.out.println("Crop counter against r.stone: " + comparison);
        assertEquals(18788, comparison.expectedCells);
        assertTrue("footprint overlap " + comparison.overlap, comparison.overlap > 0.8);
        // all the cells crossed between the recorded points are counted, r.stone only the ones of the
        // points: with a 5 m step on 10 m cells it misses about 10% of them
        assertTrue("total count " + comparison.sum, comparison.sum > comparison.expectedSum);
        assertTrue("total count " + comparison.sum, comparison.sum < 1.25 * comparison.expectedSum);
        assertEquals("trajectory cells", comparison.expectedCells, comparison.cells, 0.05 * comparison.expectedCells);
        assertTrue("correlation " + comparison.correlation, comparison.correlation > 0.9);

        // r.stone writes maxima truncated to integers, compare the distributions of the truncated values
        Comparison velocities = new Comparison(truncate(toMatrix(stone.outMaxVelocity)),
                toMatrix(read("crop_expectedmaxvel.tif")));
        System.out.println("Crop max velocity against r.stone: " + velocities);
        // the cells crossed between the recorded points are sampled too, so the maxima are a bit higher
        assertEquals("mean max velocity", velocities.expectedMean, velocities.mean, 0.1 * velocities.expectedMean);
        assertEquals("99th percentile max velocity", velocities.expectedP99, velocities.p99, 0.1 * velocities.expectedP99);
        Comparison heights = new Comparison(truncate(toMatrix(stone.outMaxDz)), toMatrix(read("crop_expectedmaxdz.tif")));
        System.out.println("Crop max height against r.stone: " + heights);
        // heights vary between realizations of about 7% (this r.stone run is a low one) and the cells crossed
        // between the recorded points are sampled too
        assertEquals("mean max height", heights.expectedMean, heights.mean, 0.25 * heights.expectedMean);
        assertEquals("99th percentile max height", heights.expectedP99, heights.p99, 0.15 * heights.expectedP99);
    }

    private static double[][] truncate( double[][] values ) {
        for( double[] row : values ) {
            for( int c = 0; c < row.length; c++ ) {
                if (!Double.isNaN(row[c])) {
                    row[c] = (long) row[c];
                }
            }
        }
        return values;
    }

    /**
     * The sources are independent: running two groups of sources separately gives counts that sum
     * up and maxima that combine to the ones of a single run with all sources. This is what makes
     * a result independent of the processing order (and of a parallel processing).
     */
    public void testSourcesAreIndependent() throws Exception {
        double[][] sources = toMatrix(read("crop_sources.tif"));
        int rows = sources.length;
        int cols = sources[0].length;
        double[][] evenRows = new double[rows][cols];
        double[][] oddRows = new double[rows][cols];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                evenRows[r][c] = r % 2 == 0 ? sources[r][c] : Double.NaN;
                oddRows[r][c] = r % 2 == 1 ? sources[r][c] : Double.NaN;
            }
        }
        GridCoverage2D template = read("crop_sources.tif");
        OmsStone all = runCrop(template);
        OmsStone even = runCrop(buildLike(template, evenRows));
        OmsStone odd = runCrop(buildLike(template, oddRows));

        double[][] allCounter = toMatrix(all.outCounter);
        double[][] evenCounter = toMatrix(even.outCounter);
        double[][] oddCounter = toMatrix(odd.outCounter);
        double[][] allVelocity = toMatrix(all.outMaxVelocity);
        double[][] evenVelocity = toMatrix(even.outMaxVelocity);
        double[][] oddVelocity = toMatrix(odd.outMaxVelocity);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                String cell = "row " + r + " col " + c;
                double sum = nanToZero(evenCounter[r][c]) + nanToZero(oddCounter[r][c]);
                assertEquals(cell, sum, nanToZero(allCounter[r][c]), 0.0);
                double max = Math.max(nanToMinusOne(evenVelocity[r][c]), nanToMinusOne(oddVelocity[r][c]));
                assertEquals(cell, max, nanToMinusOne(allVelocity[r][c]), 0.0);
            }
        }
    }

    private static double nanToZero( double value ) {
        return Double.isNaN(value) ? 0 : value;
    }

    private static double nanToMinusOne( double value ) {
        return Double.isNaN(value) ? -1 : value;
    }

    private GridCoverage2D buildLike( GridCoverage2D template, double[][] values ) {
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(template);
        return CoverageUtilities.buildCoverageWithNovalue("sources", values, region,
                template.getCoordinateReferenceSystem(), true, Double.NaN);
    }

    private OmsStone runCrop( GridCoverage2D sources ) throws Exception {
        OmsStone stone = new OmsStone();
        stone.inElev = read("crop_dem.tif");
        stone.inSources = sources;
        stone.inFriction = read("crop_friction.tif");
        stone.inNormalRestitution = read("crop_nrest.tif");
        stone.inTangentialRestitution = read("crop_trest.tif");
        stone.pStartVelocity = 1;
        stone.pStopVelocity = 3;
        stone.pm = pm;
        stone.process();
        return stone;
    }

    public void testTrajectories() throws Exception {
        GridCoverage2D sources = read("crop_sources.tif");
        OmsStone plain = runCrop(sources);
        OmsStone withTrajectories = new OmsStone();
        withTrajectories.inElev = read("crop_dem.tif");
        withTrajectories.inSources = sources;
        withTrajectories.inFriction = read("crop_friction.tif");
        withTrajectories.inNormalRestitution = read("crop_nrest.tif");
        withTrajectories.inTangentialRestitution = read("crop_trest.tif");
        withTrajectories.pStartVelocity = 1;
        withTrajectories.pStopVelocity = 3;
        withTrajectories.pMaxTrajectories = 500;
        withTrajectories.pm = pm;
        withTrajectories.process();

        // collecting the trajectories doesn't change the simulation
        assertSameMatrix(toMatrix(plain.outCounter), toMatrix(withTrajectories.outCounter));
        assertSameMatrix(toMatrix(plain.outMaxVelocity), toMatrix(withTrajectories.outMaxVelocity));
        assertSameMatrix(toMatrix(plain.outMaxDz), toMatrix(withTrajectories.outMaxDz));

        // a sample of about the requested size, of 13022 boulders
        List<StoneTrajectory> trajectories = withTrajectories.getTrajectories();
        assertTrue("trajectories " + trajectories.size(), trajectories.size() > 400 && trajectories.size() < 600);
        assertEquals(trajectories.size(), withTrajectories.outTrajectories.size());

        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(sources);
        double[][] sourcesMatrix = toMatrix(sources);
        for( StoneTrajectory trajectory : trajectories ) {
            assertEquals(1.0, sourcesMatrix[trajectory.getSourceRow()][trajectory.getSourceCol()], 0.0);
            // starts in the center of the source cell
            double startX = region.getWest() + (trajectory.getSourceCol() + 0.5) * region.getXres();
            double startY = region.getNorth() - (trajectory.getSourceRow() + 0.5) * region.getYres();
            assertEquals(startX, trajectory.getX(0), 1E-3);
            assertEquals(startY, trajectory.getY(0), 1E-3);
            for( int i = 0; i < trajectory.size(); i++ ) {
                assertTrue(trajectory.getX(i) >= region.getWest() - region.getXres());
                assertTrue(trajectory.getX(i) <= region.getEast() + region.getXres());
                assertTrue(trajectory.getY(i) >= region.getSouth() - region.getYres());
                assertTrue(trajectory.getY(i) <= region.getNorth() + region.getYres());
                assertTrue(trajectory.getSpeed(i) >= 0);
            }
        }
        SimpleFeature feature = withTrajectories.outTrajectories.features().next();
        LineString line = (LineString) feature.getDefaultGeometry();
        assertFalse("3D coordinates", Double.isNaN(line.getCoordinateN(0).getZ()));
    }

    public void testStep() throws Exception {
        // a step larger than the cell is limited to the cell
        assertSameMatrix(toMatrix(runCropWithStep(10, 0).outCounter), toMatrix(runCropWithStep(50, 0).outCounter));

        // the results of the model depend on the step (longer runouts with finer steps), so a finer
        // step is only checked to run and to cover about the same area
        OmsStone fine = runCropWithStep(2, 0);
        Comparison comparison = new Comparison(toMatrix(fine.outCounter), toMatrix(runCropWithStep(5, 0).outCounter));
        System.out.println("Crop counter with step 2 against step 5: " + comparison);
        assertTrue("footprint overlap " + comparison.overlap, comparison.overlap > 0.8);
    }

    /**
     * The flights of the collected trajectories are detailed along their parabola, also with the
     * default step.
     */
    public void testTrajectoryFlightsDetail() throws Exception {
        OmsStone stone = runCropWithStep(5, 300);
        double flightLength = 0;
        int flightSegments = 0;
        for( StoneTrajectory trajectory : stone.getTrajectories() ) {
            for( int i = 1; i < trajectory.size(); i++ ) {
                if (trajectory.getHeight(i) > 0.5 && trajectory.getHeight(i - 1) > 0.5) {
                    flightLength += Math.hypot(Math.hypot(trajectory.getX(i) - trajectory.getX(i - 1),
                            trajectory.getY(i) - trajectory.getY(i - 1)), trajectory.getZ(i) - trajectory.getZ(i - 1));
                    flightSegments++;
                }
                // the model can place a boulder a bit under the triangle after switching triangles
                assertTrue("height " + trajectory.getHeight(i), trajectory.getHeight(i) > -0.1);
            }
        }
        double meanSpacing = flightLength / flightSegments;
        System.out.println("Mean spacing of the flight points of the trajectories: " + meanSpacing);
        assertTrue("mean spacing " + meanSpacing, meanSpacing <= 1.0 + 1E-6);
    }

    private OmsStone runCropWithStep( double step, int maxTrajectories ) throws Exception {
        OmsStone stone = new OmsStone();
        stone.inElev = read("crop_dem.tif");
        stone.inSources = read("crop_sources.tif");
        stone.inFriction = read("crop_friction.tif");
        stone.inNormalRestitution = read("crop_nrest.tif");
        stone.inTangentialRestitution = read("crop_trest.tif");
        stone.pStartVelocity = 1;
        stone.pStopVelocity = 3;
        stone.pStep = step;
        stone.pMaxTrajectories = maxTrajectories;
        stone.pm = pm;
        stone.process();
        return stone;
    }

    public void testAllTrajectoriesOfASource() throws Exception {
        OmsStone stone = new OmsStone();
        stone.inElev = read("dem.tif");
        stone.inSources = read("sources.tif");
        stone.inFriction = read("friction.tif");
        stone.inNormalRestitution = read("nrest.tif");
        stone.inTangentialRestitution = read("trest.tif");
        stone.pStopVelocity = 1;
        stone.pStartVelocity = 0.5;
        stone.pMaxTrajectories = 1000;
        stone.pm = pm;
        stone.process();
        assertEquals(100, stone.getTrajectories().size());
    }

    private static void assertSameMatrix( double[][] expected, double[][] values ) {
        for( int r = 0; r < expected.length; r++ ) {
            for( int c = 0; c < expected[0].length; c++ ) {
                assertEquals("row " + r + " col " + c, expected[r][c], values[r][c], 0.0);
            }
        }
    }

    public void testReproducible() throws Exception {
        double[][] first = toMatrix(runGrassTestCase().outCounter);
        double[][] second = toMatrix(runGrassTestCase().outCounter);
        for( int r = 0; r < first.length; r++ ) {
            for( int c = 0; c < first[0].length; c++ ) {
                assertEquals("row " + r + " col " + c, first[r][c], second[r][c], 0.0);
            }
        }
    }

    public void testDifferentGridsRefused() throws Exception {
        OmsStone stone = new OmsStone();
        stone.inElev = read("dem.tif");
        stone.inSources = read("sources.tif");
        stone.inFriction = read("friction.tif");
        stone.inNormalRestitution = read("nrest.tif");
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 10, 0, 10, 10, 10);
        stone.inTangentialRestitution = CoverageUtilities.buildCoverage("trest", new double[10][10], region,
                HMTestMaps.getCrs(), true);
        stone.pm = pm;
        try {
            stone.process();
            fail("Maps with a different grid have to be refused.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
    }

    /**
     * The test case of the GRASS r.stone addon: 100 boulders from a single source.
     */
    private OmsStone runGrassTestCase() throws Exception {
        OmsStone stone = new OmsStone();
        stone.inElev = read("dem.tif");
        stone.inSources = read("sources.tif");
        stone.inFriction = read("friction.tif");
        stone.inNormalRestitution = read("nrest.tif");
        stone.inTangentialRestitution = read("trest.tif");
        stone.pAngleStochRange = 10;
        stone.pNormalRestitutionStochRange = 10;
        stone.pTangentialRestitutionStochRange = 10;
        stone.pFrictionStochRange = 10;
        stone.pStopVelocity = 1;
        stone.pStartVelocity = 0.5;
        stone.pAngleStochFunction = OmsStone.GAUSSIAN;
        stone.pStochFunction = OmsStone.GAUSSIAN;
        stone.pm = pm;
        stone.process();
        return stone;
    }

    /**
     * Statistics of a result map against a reference map, novalues are NaN.
     */
    private static class Comparison {
        int cells, expectedCells;
        double overlap, sum, expectedSum, max, expectedMax, mean, expectedMean, p99, expectedP99, correlation;

        Comparison( double[][] values, double[][] expected ) {
            int both = 0, any = 0;
            double sx = 0, sy = 0, sxx = 0, syy = 0, sxy = 0;
            List<Double> valuesList = new ArrayList<>();
            List<Double> expectedList = new ArrayList<>();
            for( int r = 0; r < values.length; r++ ) {
                for( int c = 0; c < values[0].length; c++ ) {
                    double v = values[r][c];
                    double e = expected[r][c];
                    boolean hasV = !Double.isNaN(v);
                    boolean hasE = !Double.isNaN(e);
                    if (hasV) {
                        cells++;
                        sum += v;
                        max = Math.max(max, v);
                        valuesList.add(v);
                    }
                    if (hasE) {
                        expectedCells++;
                        expectedSum += e;
                        expectedMax = Math.max(expectedMax, e);
                        expectedList.add(e);
                    }
                    if (hasV || hasE) {
                        any++;
                    }
                    if (hasV && hasE) {
                        both++;
                        sx += v;
                        sy += e;
                        sxx += v * v;
                        syy += e * e;
                        sxy += v * e;
                    }
                }
            }
            overlap = both / (double) any;
            double cov = sxy - sx * sy / both;
            correlation = cov / Math.sqrt((sxx - sx * sx / both) * (syy - sy * sy / both));
            mean = sum / cells;
            expectedMean = expectedSum / expectedCells;
            p99 = percentile99(valuesList);
            expectedP99 = percentile99(expectedList);
        }

        private static double percentile99( List<Double> list ) {
            Collections.sort(list);
            return list.get((int) (0.99 * (list.size() - 1)));
        }

        @Override
        public String toString() {
            return String.format(
                    "cells %d/%d, overlap %.3f, sum %.1f/%.1f, mean %.3f/%.3f, p99 %.1f/%.1f, max %.2f/%.2f, correlation %.3f",
                    cells, expectedCells, overlap, sum, expectedSum, mean, expectedMean, p99, expectedP99, max, expectedMax,
                    correlation);
        }
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
