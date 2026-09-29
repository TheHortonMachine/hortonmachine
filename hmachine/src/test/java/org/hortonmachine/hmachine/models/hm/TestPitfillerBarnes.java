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

import java.awt.image.DataBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfillerBarnes;
import org.hortonmachine.hmachine.modules.geomorphology.tca.OmsTca;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;

/**
 * Test the {@link OmsPitfillerBarnes} module.
 */
public class TestPitfillerBarnes extends HMTestCase {
    private static final double N = HMConstants.doubleNovalue;

    public void testTwoCellsDepression() throws Exception {
        double[][] elev = {//
                {9, 9, 9, 9, 9}, //
                {9, 5, 5, 5, 9}, //
                {9, 5, 1, 2, 4}, //
                {9, 5, 5, 5, 9}, //
                {9, 9, 9, 9, 9}};
        double[][] expected = {//
                {9, 9, 9, 9, 9}, //
                {9, 5, 5, 5, 9}, //
                {9, 5, 4, 4, 4}, //
                {9, 5, 5, 5, 9}, //
                {9, 9, 9, 9, 9}};
        for( int tileSize : new int[]{2, 3, 1024} ) {
            assertMatrix(expected, run(elev, tileSize, 2));
        }
    }

    public void testDepressionDrainingIntoNovalueHole() throws Exception {
        double[][] elev = {//
                {9, 9, 9, 9, 9, 9, 9}, //
                {9, 8, 8, 8, 8, 8, 9}, //
                {9, 8, 1, 3, 2, 8, 9}, //
                {9, 8, 8, 8, N, 8, 9}, //
                {9, 8, 8, 8, 8, 8, 9}, //
                {9, 9, 9, 9, 9, 9, 9}};
        // the cells around the novalue are outlets, so only the cell with 1 is filled up to the saddle
        double[][] expected = {//
                {9, 9, 9, 9, 9, 9, 9}, //
                {9, 8, 8, 8, 8, 8, 9}, //
                {9, 8, 3, 3, 2, 8, 9}, //
                {9, 8, 8, 8, N, 8, 9}, //
                {9, 8, 8, 8, 8, 8, 9}, //
                {9, 9, 9, 9, 9, 9, 9}};
        for( int tileSize : new int[]{2, 3, 4, 1024} ) {
            assertMatrix(expected, run(elev, tileSize, 2));
        }
    }

    public void testTestMapAgainstSequentialPriorityFlood() throws Exception {
        double[][] elev = HMTestMaps.mapData;
        double[][] expected = sequentialPriorityFlood(elev);
        for( int tileSize : new int[]{2, 3, 4, 7, 1024} ) {
            assertMatrix(expected, run(elev, tileSize, 4));
        }
    }

    public void testRandomDemsAgainstSequentialPriorityFlood() throws Exception {
        Random random = new Random(123);
        for( int test = 0; test < 12; test++ ) {
            int rows = 20 + random.nextInt(120);
            int cols = 20 + random.nextInt(120);
            double[][] elev = new double[rows][cols];
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = 100 + 2 * Math.sin(c / 5.0) * Math.cos(r / 7.0) + random.nextDouble();
                    if (test % 2 == 0) {
                        // plenty of flats
                        value = Math.round(value * 2) / 2.0;
                    }
                    if (random.nextDouble() < 0.03) {
                        value = N;
                    }
                    elev[r][c] = value;
                }
            }
            double[][] expected = sequentialPriorityFlood(elev);
            for( int tileSize : new int[]{2, 5, 16, 33, 1024} ) {
                for( int threads : new int[]{1, 4} ) {
                    assertMatrix(expected, run(elev, tileSize, threads));
                }
            }
        }
    }

    public void testLargeFilesAgainstSequentialPriorityFlood() throws Exception {
        Random random = new Random(321);
        for( int test = 0; test < 6; test++ ) {
            int rows = 40 + random.nextInt(80);
            int cols = 40 + random.nextInt(80);
            double[][] elev = new double[rows][cols];
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = 100 + 2 * Math.sin(c / 5.0) * Math.cos(r / 7.0) + random.nextDouble();
                    if (test % 2 == 0) {
                        value = Math.round(value * 2) / 2.0;
                    }
                    if (random.nextDouble() < 0.03 || (test == 1 && c < cols / 2 && r < rows / 2)) {
                        // scattered novalues, plus a novalues only quarter to test empty tiles
                        value = N;
                    }
                    elev[r][c] = value;
                }
            }
            double[][] expected = sequentialPriorityFlood(elev);

            File inFile = File.createTempFile("pitbarnes_in_", ".tif");
            File outFile = new File(inFile.getParentFile(), inFile.getName().replace("_in_", "_out_"));
            try {
                OmsRasterWriter.writeRaster(inFile.getAbsolutePath(), buildCoverage(elev));
                for( int tileSize : new int[]{16, 32, 1024} ) {
                    OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
                    pitfiller.doLargeFile = true;
                    pitfiller.inElevFile = inFile.getAbsolutePath();
                    pitfiller.outPitFile = outFile.getAbsolutePath();
                    pitfiller.pTileSize = tileSize;
                    pitfiller.pThreads = 4;
                    pitfiller.pm = pm;
                    pitfiller.process();
                    assertMatrix(expected, toMatrix(OmsRasterReader.readRaster(outFile.getAbsolutePath()), rows, cols));
                }
            } finally {
                inFile.delete();
                outFile.delete();
            }
        }
    }

    public void testFlowOnFlatLakeAcrossManyTiles() throws Exception {
        // a flat basin draining through a single cell on the west border, filled up to 10
        int rows = 30;
        int cols = 60;
        double[][] elev = new double[rows][cols];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                boolean border = r == 0 || c == 0 || r == rows - 1 || c == cols - 1;
                elev[r][c] = border ? 100 : 10;
            }
        }
        elev[rows / 2][0] = 5;
        elev[5][40] = 3; // a pit, filled to 10
        double[][] expected = sequentialPriorityFlood(elev);
        for( int tileSize : new int[]{2, 4, 7, 1024} ) {
            double[][][] result = runWithFlow(elev, tileSize, 4);
            assertMatrix(expected, result[0]);
            checkFlow(expected, result[1]);
        }
    }

    public void testFlowInMemory() throws Exception {
        checkFlow(sequentialPriorityFlood(HMTestMaps.mapData), runWithFlow(HMTestMaps.mapData, 3, 4)[1]);

        Random random = new Random(99);
        for( int test = 0; test < 8; test++ ) {
            double[][] elev = randomDem(random, 20 + random.nextInt(100), 20 + random.nextInt(100), test % 2 == 0, false);
            double[][] expected = sequentialPriorityFlood(elev);
            for( int tileSize : new int[]{2, 5, 16, 1024} ) {
                for( int threads : new int[]{1, 4} ) {
                    double[][][] result = runWithFlow(elev, tileSize, threads);
                    assertMatrix(expected, result[0]);
                    checkFlow(expected, result[1]);
                }
            }
        }
    }

    public void testFlowLargeFiles() throws Exception {
        Random random = new Random(77);
        for( int test = 0; test < 4; test++ ) {
            int rows = 40 + random.nextInt(80);
            int cols = 40 + random.nextInt(80);
            double[][] elev = randomDem(random, rows, cols, test % 2 == 0, test == 1);
            double[][] expected = sequentialPriorityFlood(elev);

            File inFile = File.createTempFile("pitbarnes_in_", ".tif");
            File outFile = new File(inFile.getParentFile(), inFile.getName().replace("_in_", "_out_"));
            File flowFile = new File(inFile.getParentFile(), inFile.getName().replace("_in_", "_flow_"));
            try {
                OmsRasterWriter.writeRaster(inFile.getAbsolutePath(), buildCoverage(elev));
                for( int tileSize : new int[]{16, 32, 1024} ) {
                    OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
                    pitfiller.doLargeFile = true;
                    pitfiller.doFlow = true;
                    pitfiller.inElevFile = inFile.getAbsolutePath();
                    pitfiller.outPitFile = outFile.getAbsolutePath();
                    pitfiller.outFlowFile = flowFile.getAbsolutePath();
                    pitfiller.pTileSize = tileSize;
                    pitfiller.pThreads = 4;
                    pitfiller.pm = pm;
                    pitfiller.process();
                    assertMatrix(expected, toMatrix(OmsRasterReader.readRaster(outFile.getAbsolutePath()), rows, cols));
                    checkFlow(expected, toMatrix(OmsRasterReader.readRaster(flowFile.getAbsolutePath()), rows, cols));
                }
            } finally {
                inFile.delete();
                outFile.delete();
                flowFile.delete();
            }
        }
    }

    public void testTcaAgainstOmsTca() throws Exception {
        Random random = new Random(55);
        for( int test = 0; test < 6; test++ ) {
            double[][] elev = randomDem(random, 20 + random.nextInt(100), 20 + random.nextInt(100), test % 2 == 0, test == 3);
            int rows = elev.length;
            int cols = elev[0].length;
            for( int tileSize : new int[]{2, 5, 16, 1024} ) {
                OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
                pitfiller.inElev = buildCoverage(elev);
                pitfiller.doFlow = true;
                pitfiller.doTca = true;
                pitfiller.pTileSize = tileSize;
                pitfiller.pThreads = 4;
                pitfiller.pm = pm;
                pitfiller.process();
                assertEquals(DataBuffer.TYPE_INT, pitfiller.outTca.getRenderedImage().getSampleModel().getDataType());

                OmsTca omsTca = new OmsTca();
                omsTca.inFlow = pitfiller.outFlow;
                omsTca.pm = pm;
                omsTca.process();
                assertMatrix(toMatrix(omsTca.outTca, rows, cols), toMatrix(pitfiller.outTca, rows, cols));
            }
        }
    }

    public void testTcaOnFlatLake() throws Exception {
        int rows = 30;
        int cols = 60;
        double[][] elev = new double[rows][cols];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                boolean border = r == 0 || c == 0 || r == rows - 1 || c == cols - 1;
                elev[r][c] = border ? 100 : 10;
            }
        }
        elev[rows / 2][0] = 5;
        for( int tileSize : new int[]{2, 4, 7, 1024} ) {
            OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
            pitfiller.inElev = buildCoverage(elev);
            pitfiller.doTca = true;
            pitfiller.pTileSize = tileSize;
            pitfiller.pm = pm;
            pitfiller.process();
            assertNull(pitfiller.outFlow);
            // everything drains to the single outlet on the west border
            double[][] tca = toMatrix(pitfiller.outTca, rows, cols);
            assertEquals(rows * cols, tca[rows / 2][0], 0.0);
        }
    }

    public void testTcaLargeFiles() throws Exception {
        Random random = new Random(66);
        for( int test = 0; test < 3; test++ ) {
            int rows = 40 + random.nextInt(80);
            int cols = 40 + random.nextInt(80);
            double[][] elev = randomDem(random, rows, cols, test % 2 == 0, test == 1);

            File inFile = File.createTempFile("pitbarnes_in_", ".tif");
            File outFile = new File(inFile.getParentFile(), inFile.getName().replace("_in_", "_out_"));
            File tcaFile = new File(inFile.getParentFile(), inFile.getName().replace("_in_", "_tca_"));
            try {
                OmsRasterWriter.writeRaster(inFile.getAbsolutePath(), buildCoverage(elev));
                for( int tileSize : new int[]{16, 32, 1024} ) {
                    // reference: the in memory flow with the same tiles, accumulated by OmsTca
                    OmsPitfillerBarnes memory = new OmsPitfillerBarnes();
                    memory.inElev = buildCoverage(elev);
                    memory.doFlow = true;
                    memory.pTileSize = tileSize;
                    memory.pm = pm;
                    memory.process();
                    OmsTca omsTca = new OmsTca();
                    omsTca.inFlow = memory.outFlow;
                    omsTca.pm = pm;
                    omsTca.process();

                    // tca without flow output
                    OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
                    pitfiller.doLargeFile = true;
                    pitfiller.doTca = true;
                    pitfiller.inElevFile = inFile.getAbsolutePath();
                    pitfiller.outPitFile = outFile.getAbsolutePath();
                    pitfiller.outTcaFile = tcaFile.getAbsolutePath();
                    pitfiller.pTileSize = tileSize;
                    pitfiller.pThreads = 4;
                    pitfiller.pm = pm;
                    pitfiller.process();
                    GridCoverage2D tcaCoverage = OmsRasterReader.readRaster(tcaFile.getAbsolutePath());
                    assertEquals(DataBuffer.TYPE_INT, tcaCoverage.getRenderedImage().getSampleModel().getDataType());
                    assertMatrix(toMatrix(omsTca.outTca, rows, cols), toMatrix(tcaCoverage, rows, cols));
                }
            } finally {
                inFile.delete();
                outFile.delete();
                tcaFile.delete();
            }
        }
    }

    public void testNetAgainstOmsExtractNetwork() throws Exception {
        Random random = new Random(44);
        for( int test = 0; test < 4; test++ ) {
            double[][] elev = randomDem(random, 20 + random.nextInt(100), 20 + random.nextInt(100), test % 2 == 0, test == 1);
            int rows = elev.length;
            int cols = elev[0].length;
            for( int tileSize : new int[]{2, 7, 1024} ) {
                for( long threshold : new long[]{1, 5, 50} ) {
                    OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
                    pitfiller.inElev = buildCoverage(elev);
                    pitfiller.doTca = true;
                    pitfiller.doNet = true;
                    pitfiller.pThres = threshold;
                    pitfiller.pTileSize = tileSize;
                    pitfiller.pThreads = 4;
                    pitfiller.pm = pm;
                    pitfiller.process();

                    OmsExtractNetwork extract = new OmsExtractNetwork();
                    extract.inTca = pitfiller.outTca;
                    extract.pThres = (int) threshold;
                    extract.pm = pm;
                    extract.process();
                    assertMatrix(toMatrix(extract.outNet, rows, cols), toMatrix(pitfiller.outNet, rows, cols));
                }
            }
        }
    }

    public void testNetAloneInBothModes() throws Exception {
        Random random = new Random(33);
        double[][] elev = randomDem(random, 90, 110, true, false);
        int rows = elev.length;
        int cols = elev[0].length;
        long threshold = 20;
        File inFile = File.createTempFile("pitbarnes_in_", ".tif");
        File outFile = new File(inFile.getParentFile(), inFile.getName().replace("_in_", "_out_"));
        File netFile = new File(inFile.getParentFile(), inFile.getName().replace("_in_", "_net_"));
        try {
            OmsRasterWriter.writeRaster(inFile.getAbsolutePath(), buildCoverage(elev));
            for( int tileSize : new int[]{16, 1024} ) {
                OmsPitfillerBarnes memory = new OmsPitfillerBarnes();
                memory.inElev = buildCoverage(elev);
                memory.doNet = true;
                memory.pThres = threshold;
                memory.pTileSize = tileSize;
                memory.pm = pm;
                memory.process();
                assertNull(memory.outTca);
                assertNull(memory.outFlow);
                double[][] memoryNet = toMatrix(memory.outNet, rows, cols);
                int netCells = 0;
                for( double[] row : memoryNet ) {
                    for( double value : row ) {
                        if (!isNovalue(value)) {
                            assertEquals(2.0, value, 0.0);
                            netCells++;
                        }
                    }
                }
                assertTrue("some network expected", netCells > 0);

                OmsPitfillerBarnes large = new OmsPitfillerBarnes();
                large.doLargeFile = true;
                large.doNet = true;
                large.pThres = threshold;
                large.inElevFile = inFile.getAbsolutePath();
                large.outPitFile = outFile.getAbsolutePath();
                large.outNetFile = netFile.getAbsolutePath();
                large.pTileSize = tileSize;
                large.pm = pm;
                large.process();
                assertMatrix(memoryNet, toMatrix(OmsRasterReader.readRaster(netFile.getAbsolutePath()), rows, cols));
            }

            OmsPitfillerBarnes invalid = new OmsPitfillerBarnes();
            invalid.inElev = buildCoverage(elev);
            invalid.doNet = true;
            invalid.pThres = 0;
            invalid.pm = pm;
            try {
                invalid.process();
                fail("A threshold below 1 has to be refused.");
            } catch (ModelsIllegalargumentException e) {
                // expected
            }
        } finally {
            inFile.delete();
            outFile.delete();
            netFile.delete();
        }
    }

    private double[][] randomDem( Random random, int rows, int cols, boolean withFlats, boolean withEmptyQuarter ) {
        double[][] elev = new double[rows][cols];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = 100 + 2 * Math.sin(c / 5.0) * Math.cos(r / 7.0) + random.nextDouble();
                if (withFlats) {
                    value = Math.round(value * 2) / 2.0;
                }
                if (random.nextDouble() < 0.03 || (withEmptyQuarter && c < cols / 2 && r < rows / 2)) {
                    value = N;
                }
                elev[r][c] = value;
            }
        }
        return elev;
    }

    private double[][][] runWithFlow( double[][] elev, int tileSize, int threads ) throws Exception {
        int rows = elev.length;
        int cols = elev[0].length;
        OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
        pitfiller.inElev = buildCoverage(elev);
        pitfiller.doFlow = true;
        pitfiller.pTileSize = tileSize;
        pitfiller.pThreads = threads;
        pitfiller.pm = pm;
        pitfiller.process();
        return new double[][][]{toMatrix(pitfiller.outPit, rows, cols), toMatrix(pitfiller.outFlow, rows, cols)};
    }

    private static final int[] DCOLS = {1, 1, 0, -1, -1, -1, 0, 1};
    private static final int[] DROWS = {0, -1, -1, -1, 0, 1, 1, 1};

    /**
     * Check the flow directions on the (reference) filled surface, with unit cell sizes.
     */
    private void checkFlow( double[][] filled, double[][] flow ) {
        int rows = filled.length;
        int cols = filled[0].length;
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                String cell = "cell " + c + "/" + r;
                if (isNovalue(filled[r][c])) {
                    assertTrue(cell, isNovalue(flow[r][c]));
                    continue;
                }
                assertFalse(cell + " has no flow", isNovalue(flow[r][c]));
                int code = (int) flow[r][c];

                // the expected steepest descent, first maximum in the order of the codes
                int best = -1;
                double maxSlope = 0;
                boolean touchesInvalid = false;
                for( int k = 0; k < 8; k++ ) {
                    int nr = r + DROWS[k];
                    int nc = c + DCOLS[k];
                    if (nr < 0 || nc < 0 || nr >= rows || nc >= cols || isNovalue(filled[nr][nc])) {
                        touchesInvalid = true;
                        continue;
                    }
                    double slope = (filled[r][c] - filled[nr][nc]) / (DCOLS[k] != 0 && DROWS[k] != 0 ? Math.sqrt(2) : 1);
                    if (slope > maxSlope) {
                        maxSlope = slope;
                        best = k;
                    }
                }
                if (best >= 0) {
                    assertEquals(cell + " steepest descent", best + 1, code);
                } else if (touchesInvalid) {
                    assertEquals(cell + " outlet", 10, code);
                } else {
                    assertTrue(cell + " flat code " + code, code >= 1 && code <= 8);
                    int nr = r + DROWS[code - 1];
                    int nc = c + DCOLS[code - 1];
                    assertEquals(cell + " flows along the flat", filled[r][c], filled[nr][nc], 0.0);
                }
            }
        }

        // every flow path ends in an outlet, never going up
        int[][] state = new int[rows][cols]; // 0 unknown, 1 on the current path, 2 reaches an outlet
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                if (isNovalue(filled[r][c]) || state[r][c] == 2) {
                    continue;
                }
                List<int[]> path = new ArrayList<>();
                int pr = r;
                int pc = c;
                while( state[pr][pc] == 0 && (int) flow[pr][pc] != 10 ) {
                    state[pr][pc] = 1;
                    path.add(new int[]{pr, pc});
                    int code = (int) flow[pr][pc];
                    int nr = pr + DROWS[code - 1];
                    int nc = pc + DCOLS[code - 1];
                    assertFalse("flow into a novalue at " + pc + "/" + pr, isNovalue(filled[nr][nc]));
                    assertTrue("flow goes up at " + pc + "/" + pr, filled[nr][nc] <= filled[pr][pc]);
                    pr = nr;
                    pc = nc;
                }
                assertTrue("flow loop through " + pc + "/" + pr, state[pr][pc] != 1);
                state[pr][pc] = 2;
                for( int[] p : path ) {
                    state[p[0]][p[1]] = 2;
                }
            }
        }
    }

    private GridCoverage2D buildCoverage( double[][] elev ) {
        int rows = elev.length;
        int cols = elev[0].length;
        RegionMap region = RegionMap.fromBoundsAndGrid(500000, 500000 + cols, 5000000, 5000000 + rows, cols, rows);
        return CoverageUtilities.buildCoverageWithNovalue("elevation", elev, region, HMTestMaps.getCrs(), true, N);
    }

    private double[][] run( double[][] elev, int tileSize, int threads ) throws Exception {
        int rows = elev.length;
        int cols = elev[0].length;
        OmsPitfillerBarnes pitfiller = new OmsPitfillerBarnes();
        pitfiller.inElev = buildCoverage(elev);
        pitfiller.pTileSize = tileSize;
        pitfiller.pThreads = threads;
        pitfiller.pm = pm;
        pitfiller.process();
        return toMatrix(pitfiller.outPit, rows, cols);
    }

    private double[][] toMatrix( GridCoverage2D coverage, int rows, int cols ) throws Exception {
        double[][] result = new double[rows][cols];
        try (HMRaster outRaster = HMRaster.fromGridCoverage(coverage)) {
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = outRaster.getValue(c, r);
                    result[r][c] = outRaster.isNovalue(value) ? N : value;
                }
            }
        }
        return result;
    }

    /**
     * Plain sequential Priority-Flood, seeded from the cells touching novalues or the border.
     */
    private double[][] sequentialPriorityFlood( double[][] elev ) {
        int rows = elev.length;
        int cols = elev[0].length;
        double[][] filled = new double[rows][cols];
        boolean[][] closed = new boolean[rows][cols];
        PriorityQueue<double[]> queue = new PriorityQueue<>(( a, b ) -> Double.compare(a[0], b[0]));
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                filled[r][c] = elev[r][c];
                if (isNovalue(elev[r][c])) {
                    closed[r][c] = true;
                    continue;
                }
                boolean touchesInvalid = false;
                for( int dr = -1; dr <= 1; dr++ ) {
                    for( int dc = -1; dc <= 1; dc++ ) {
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr < 0 || nc < 0 || nr >= rows || nc >= cols || isNovalue(elev[nr][nc])) {
                            touchesInvalid = true;
                        }
                    }
                }
                if (touchesInvalid) {
                    closed[r][c] = true;
                    queue.add(new double[]{elev[r][c], r, c});
                }
            }
        }
        while( !queue.isEmpty() ) {
            double[] cell = queue.poll();
            int r = (int) cell[1];
            int c = (int) cell[2];
            for( int dr = -1; dr <= 1; dr++ ) {
                for( int dc = -1; dc <= 1; dc++ ) {
                    int nr = r + dr;
                    int nc = c + dc;
                    if (nr < 0 || nc < 0 || nr >= rows || nc >= cols || closed[nr][nc]) {
                        continue;
                    }
                    closed[nr][nc] = true;
                    filled[nr][nc] = Math.max(filled[nr][nc], filled[r][c]);
                    queue.add(new double[]{filled[nr][nc], nr, nc});
                }
            }
        }
        return filled;
    }

    private static boolean isNovalue( double value ) {
        return HMConstants.isNovalue(value, N);
    }

    private void assertMatrix( double[][] expected, double[][] result ) {
        for( int r = 0; r < expected.length; r++ ) {
            for( int c = 0; c < expected[0].length; c++ ) {
                if (isNovalue(expected[r][c])) {
                    assertTrue("row " + r + " col " + c, isNovalue(result[r][c]));
                } else {
                    assertEquals("row " + r + " col " + c, expected[r][c], result[r][c], 0.0);
                }
            }
        }
    }
}
