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
package org.hortonmachine.gears;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterStatistics;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterTile;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterTiledWriter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * Test the tiles support of {@link HMRaster}.
 */
public class TestHMRasterTiles extends HMTestCase {
    private static final double N = HMConstants.doubleNovalue;

    public void testGridCoversEachCellOnce() throws Exception {
        int cols = 103;
        int rows = 57;
        for( int tileSize : new int[]{1, 7, 16, 57, 200} ) {
            List<HMRasterTile> tiles = HMRasterTile.createGrid(cols, rows, tileSize);
            int[][] hits = new int[rows][cols];
            for( int i = 0; i < tiles.size(); i++ ) {
                HMRasterTile tile = tiles.get(i);
                assertEquals(i, tile.getIndex());
                assertEquals(i, tile.getTileRow() * HMRasterTile.getTilesCount(cols, tileSize) + tile.getTileCol());
                for( int r = tile.getRow(); r < tile.getRow() + tile.getHeight(); r++ ) {
                    for( int c = tile.getCol(); c < tile.getCol() + tile.getWidth(); c++ ) {
                        hits[r][c]++;
                        assertTrue(tile.contains(c, r));
                    }
                }
            }
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    assertEquals("tile size " + tileSize + " cell " + c + "/" + r, 1, hits[r][c]);
                }
            }
        }
    }

    public void testProcessTilesRunsAllAndThrows() throws Exception {
        List<HMRasterTile> tiles = HMRasterTile.createGrid(100, 100, 10);
        AtomicInteger count = new AtomicInteger();
        HMRaster.processTiles(pm, null, tiles, 4, tile -> count.incrementAndGet());
        assertEquals(tiles.size(), count.get());

        try {
            HMRaster.processTiles(pm, null, tiles, 4, tile -> {
                if (tile.getIndex() == 42) {
                    throw new IllegalStateException("tile 42");
                }
            });
            fail("The exception of the processor should be thrown back.");
        } catch (IllegalStateException e) {
            assertEquals("tile 42", e.getMessage());
        }
    }

    /**
     * Write a multi tile GeoTIFF (512 tiles) with values only inside a window and some scattered novalues.
     */
    private File writeTiledFile( int cols, int rows, int[] dataWindow ) throws Exception {
        double[][] data = new double[rows][cols];
        Random random = new Random(11);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                boolean inWindow = c >= dataWindow[0] && c <= dataWindow[1] && r >= dataWindow[2] && r <= dataWindow[3];
                data[r][c] = inWindow && random.nextDouble() > 0.05 ? Math.round(random.nextDouble() * 1000) / 4.0 : N;
            }
        }
        RegionMap region = RegionMap.fromBoundsAndGrid(500000, 500000 + cols, 5000000, 5000000 + rows, cols, rows);
        File file = File.createTempFile("hmraster_strips_", ".tif");
        OmsRasterWriter.writeRaster(file.getAbsolutePath(),
                CoverageUtilities.buildCoverageWithNovalue("data", data, region, HMTestMaps.getCrs(), true, N));
        return file;
    }

    public void testStripReadsMatchCellReads() throws Exception {
        int cols = 1300;
        int rows = 1100;
        int[] window = {37, 1211, 101, 1060};
        File file = writeTiledFile(cols, rows, window);
        try (HMRaster raster = HMRaster.fromFile(file.getAbsolutePath())) {
            assertTrue("the file must be tiled", raster.getRenderedImage().getTileWidth() < cols);

            // process: same cells, same values, row-major order
            List<double[]> visited = new java.util.ArrayList<>();
            raster.process(pm, null, ( col, row, value, c, r ) -> visited.add(new double[]{col, row, value}));
            assertEquals(cols * rows, visited.size());
            int index = 0;
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double[] v = visited.get(index++);
                    assertEquals(c, v[0], 0.0);
                    assertEquals(r, v[1], 0.0);
                    assertSameValue(raster, raster.getValue(c, r), v[2]);
                }
            }

            // data region: bounds of the valid values
            RegionMap dataRegion = raster.getDataRegionMap();
            int[] expected = {Integer.MAX_VALUE, -1, Integer.MAX_VALUE, -1};
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    if (!raster.isNovalue(raster.getValue(c, r))) {
                        expected[0] = Math.min(expected[0], c);
                        expected[1] = Math.max(expected[1], c);
                        expected[2] = Math.min(expected[2], r);
                        expected[3] = Math.max(expected[3], r);
                    }
                }
            }
            RegionMap full0 = raster.getRegionMap();
            assertEquals(expected[1] - expected[0] + 1, dataRegion.getCols());
            assertEquals(expected[3] - expected[2] + 1, dataRegion.getRows());
            assertEquals(full0.getWest() + expected[0] * full0.getXres(), dataRegion.getWest(), 1e-6);
            assertEquals(full0.getWest() + (expected[1] + 1) * full0.getXres(), dataRegion.getEast(), 1e-6);
            assertEquals(full0.getNorth() - expected[2] * full0.getYres(), dataRegion.getNorth(), 1e-6);
            assertEquals(full0.getNorth() - (expected[3] + 1) * full0.getYres(), dataRegion.getSouth(), 1e-6);
            assertEquals(full0.getXres(), dataRegion.getXres(), 1e-9);
            assertEquals(full0.getYres(), dataRegion.getYres(), 1e-9);
            try (HMRaster empty = new HMRaster.HMRasterWritableBuilder().setRegion(full0).setCrs(raster.getCrs())
                    .setNoValue(N).build()) {
                assertNull(empty.getDataRegionMap());
            }

            // writable copies of all types, with and without null border
            for( String type : new String[]{"double", "int", "short", "byte"} ) {
                for( boolean nullBorder : new boolean[]{false, true} ) {
                    HMRaster.HMRasterWritableBuilder builder = new HMRaster.HMRasterWritableBuilder().setTemplate(raster)
                            .setCopyValues(true);
                    if (type.equals("int")) {
                        builder.setDoInteger(true);
                    } else if (type.equals("short")) {
                        builder.setDoShort(true);
                    } else if (type.equals("byte")) {
                        builder.setDoByte(true);
                    }
                    if (nullBorder) {
                        builder.setDoNullBorder();
                    }
                    try (HMRaster copy = builder.build()) {
                        for( int r = 0; r < rows; r += 7 ) {
                            for( int c = 0; c < cols; c += 3 ) {
                                boolean border = nullBorder && (c == 0 || r == 0 || c == cols - 1 || r == rows - 1);
                                double value = raster.getValue(c, r);
                                double expectedValue;
                                if (type.equals("int")) {
                                    expectedValue = border ? raster.getNovalue() : (int) value;
                                } else if (type.equals("short")) {
                                    expectedValue = border ? (short) raster.getNovalue() : (short) value;
                                } else if (type.equals("byte")) {
                                    expectedValue = border ? (byte) raster.getNovalue() : (byte) value;
                                } else {
                                    expectedValue = border ? raster.getNovalue() : value;
                                }
                                double actual = type.equals("double") ? copy.getValue(c, r) : copy.getIntValue(c, r);
                                if (type.equals("byte")) {
                                    actual = (byte) copy.getIntValue(c, r);
                                } else if (type.equals("short")) {
                                    actual = (short) copy.getIntValue(c, r);
                                }
                                assertEquals(type + " " + nullBorder + " " + c + "/" + r, expectedValue, actual, 0.0);
                            }
                        }
                    }
                }
            }

            // sub rasters: aligned (partly outside the raster) and resampled
            RegionMap full = raster.getRegionMap();
            RegionMap aligned = RegionMap.fromBoundsAndGrid(full.getWest() + 200, full.getWest() + 1500,
                    full.getNorth() - 1250, full.getNorth() - 150, 1300, 1100);
            RegionMap resampled = RegionMap.fromBoundsAndGrid(full.getWest() + 55.5, full.getWest() + 1255.5,
                    full.getNorth() - 1000.3, full.getNorth() - 20.3, 700, 450);
            for( RegionMap subRegion : new RegionMap[]{aligned, resampled} ) {
                try (HMRaster sub = raster.toSubRaster(pm, subRegion)) {
                    for( int r = 0; r < subRegion.getRows(); r++ ) {
                        for( int c = 0; c < subRegion.getCols(); c++ ) {
                            double expectedValue = raster.isContained(c, r) ? raster.getValue(sub.getWorld(c, r)) : N;
                            assertSameValue(raster, expectedValue, sub.getValue(c, r));
                        }
                    }
                }
            }

            // extraction on a polygon (aligned grid path)
            org.locationtech.jts.geom.Geometry polygon = new org.locationtech.jts.io.WKTReader().read("POLYGON ((" //
                    + (full.getWest() + 100) + " " + (full.getNorth() - 100) + ", " //
                    + (full.getWest() + 1200) + " " + (full.getNorth() - 300) + ", " //
                    + (full.getWest() + 700) + " " + (full.getNorth() - 1050) + ", " //
                    + (full.getWest() + 100) + " " + (full.getNorth() - 100) + "))");
            try (HMRaster extracted = raster.extractOnPolygon(pm, polygon)) {
                org.locationtech.jts.geom.prep.PreparedGeometry prepared = org.locationtech.jts.geom.prep.PreparedGeometryFactory
                        .prepare(polygon);
                org.locationtech.jts.geom.GeometryFactory gf = new org.locationtech.jts.geom.GeometryFactory();
                for( int r = 0; r < extracted.getRows(); r++ ) {
                    for( int c = 0; c < extracted.getCols(); c++ ) {
                        org.locationtech.jts.geom.Coordinate world = extracted.getWorld(c, r);
                        double expectedValue = raster.isContained(c, r) && prepared.contains(gf.createPoint(world))
                                ? raster.getValue(world)
                                : N;
                        assertSameValue(raster, expectedValue, extracted.getValue(c, r));
                    }
                }
            }

            // mapping onto another raster, summing
            RegionMap target = RegionMap.fromBoundsAndGrid(full.getWest() - 100, full.getEast() - 300, full.getSouth() + 50,
                    full.getNorth() + 80, cols - 200, rows + 30);
            try (HMRaster mapped = new HMRaster.HMRasterWritableBuilder().setRegion(target).setCrs(raster.getCrs())
                    .setNoValue(N).setInitialValue(1.0).build()) {
                mapped.mapRaster(pm, raster, HMRaster.MergeMode.SUM);
                for( int r = 0; r < target.getRows(); r += 3 ) {
                    for( int c = 0; c < target.getCols(); c += 3 ) {
                        double other = raster.getValue(mapped.getWorld(c, r));
                        double expectedValue = raster.isNovalue(other) ? 1.0 : 1.0 + other;
                        assertEquals(c + "/" + r, expectedValue, mapped.getValue(c, r), 1e-9);
                    }
                }
            }
        } finally {
            file.delete();
        }
    }

    private void assertSameValue( HMRaster raster, double expected, double actual ) {
        if (raster.isNovalue(expected) || HMConstants.isNovalue(expected)) {
            assertTrue("expected novalue, got " + actual, raster.isNovalue(actual) || HMConstants.isNovalue(actual));
        } else {
            assertEquals(expected, actual, 0.0);
        }
    }

    public void testStatisticsReadByBlocks() throws Exception {
        int cols = 2500;
        int rows = 1300;
        RegionMap region = RegionMap.fromBoundsAndGrid(500000, 500000 + cols, 5000000, 5000000 + rows, cols, rows);
        Random random = new Random(3);
        try (HMRaster raster = new HMRaster.HMRasterWritableBuilder().setRegion(region).setCrs(HMTestMaps.getCrs())
                .setNoValue(N).build()) {
            // the writable image has 1024x1024 tiles, so blocks cross tile borders
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    if (random.nextDouble() > 0.1) {
                        raster.setValue(c, r, random.nextDouble() * 1000 - 200);
                    }
                }
            }
            org.locationtech.jts.geom.Envelope[] envelopes = {null,
                    new org.locationtech.jts.geom.Envelope(500000 + 333.3, 500000 + 2111.7, 5000000 + 77.2, 5000000 + 1250.9)};
            for( org.locationtech.jts.geom.Envelope envelope : envelopes ) {
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                double sum = 0;
                long count = 0;
                for( int r = 0; r < rows; r++ ) {
                    for( int c = 0; c < cols; c++ ) {
                        double value = raster.getValue(c, r);
                        if (raster.isNovalue(value)) {
                            continue;
                        }
                        if (envelope != null) {
                            org.locationtech.jts.geom.Coordinate world = raster.getWorld(c, r);
                            if (!envelope.contains(world.x, world.y)) {
                                continue;
                            }
                        }
                        min = Math.min(min, value);
                        max = Math.max(max, value);
                        sum += value;
                        count++;
                    }
                }
                double[] stats = raster.getStatistics(envelope);
                assertEquals(min, stats[0], 0.0);
                assertEquals(max, stats[1], 0.0);
                assertEquals(sum / count, stats[2], 1e-9);
                assertEquals(sum, stats[3], 1e-6);
                assertEquals(count, stats[4], 0.0);
            }
        }
    }

    public void testWritableRastersOfAllTypesCanBeWritten() throws Exception {
        int rows = 20;
        int cols = 30;
        RegionMap region = RegionMap.fromBoundsAndGrid(500000, 500000 + cols, 5000000, 5000000 + rows, cols, rows);
        for( String type : new String[]{"double", "int", "short", "byte"} ) {
            HMRaster.HMRasterWritableBuilder builder = new HMRaster.HMRasterWritableBuilder().setName(type).setRegion(region)
                    .setCrs(HMTestMaps.getCrs()).setNoValue(-99);
            if (type.equals("int")) {
                builder.setDoInteger(true);
            } else if (type.equals("short")) {
                builder.setDoShort(true);
            } else if (type.equals("byte")) {
                builder.setDoByte(true).setNoValue(0);
            }
            File file = File.createTempFile("hmraster_" + type + "_", ".tif");
            try {
                try (HMRaster raster = builder.build()) {
                    for( int r = 0; r < rows; r++ ) {
                        for( int c = 0; c < cols; c++ ) {
                            raster.setValue(c, r, (double) ((r * cols + c) % 100 + 1));
                        }
                    }
                    OmsRasterWriter.writeRaster(file.getAbsolutePath(), raster);
                }
                try (HMRaster read = HMRaster.fromGridCoverage(OmsRasterReader.readRaster(file.getAbsolutePath()))) {
                    for( int r = 0; r < rows; r++ ) {
                        for( int c = 0; c < cols; c++ ) {
                            assertEquals(type + " cell " + c + "/" + r, (r * cols + c) % 100 + 1, read.getValue(c, r), 0.0);
                        }
                    }
                }
            } finally {
                file.delete();
            }
        }
    }

    public void testFileWindowedAndTiledWriterRoundtrip() throws Exception {
        int rows = 90;
        int cols = 130;
        double[][] data = new double[rows][cols];
        Random random = new Random(7);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                data[r][c] = random.nextDouble() < 0.05 ? N : 100 + random.nextDouble() * 50;
            }
        }
        RegionMap region = RegionMap.fromBoundsAndGrid(500000, 500000 + cols, 5000000, 5000000 + rows, cols, rows);
        GridCoverage2D coverage = CoverageUtilities.buildCoverageWithNovalue("data", data, region, HMTestMaps.getCrs(), true,
                N);

        File inFile = File.createTempFile("hmraster_tiles_in_", ".tif");
        File outFile = new File(inFile.getParentFile(), inFile.getName().replace("_in_", "_out_"));
        try {
            OmsRasterWriter.writeRaster(inFile.getAbsolutePath(), coverage);

            try (HMRaster windowed = HMRaster.fromFileWindowed(inFile.getAbsolutePath())) {
                assertEquals(cols, windowed.getCols());
                assertEquals(rows, windowed.getRows());

                // parallel block reads must match the cell values
                HMRaster.processTiles(pm, null, windowed.getTiles(16), 4, tile -> {
                    double[] values = windowed.getValues(tile.getCol(), tile.getRow(), tile.getWidth(), tile.getHeight(), null);
                    for( int r = 0; r < tile.getHeight(); r++ ) {
                        for( int c = 0; c < tile.getWidth(); c++ ) {
                            double expected = data[tile.getRow() + r][tile.getCol() + c];
                            double value = values[r * tile.getWidth() + c];
                            if (windowed.isNovalue(expected)) {
                                assertTrue(windowed.isNovalue(value));
                            } else {
                                assertEquals(expected, value, 0.0);
                            }
                        }
                    }
                });

                // write back through the tiled writer: identity on even tiles, novalues (null) on odd ones
                HMRasterTiledWriter.writeGeotiff(pm, outFile.getAbsolutePath(), windowed, 32, 4, tile -> {
                    if (tile.getIndex() % 2 == 1) {
                        return null;
                    }
                    return windowed.getValues(tile.getCol(), tile.getRow(), tile.getWidth(), tile.getHeight(), null);
                });
            }

            try (HMRaster written = HMRaster.fromGridCoverage(OmsRasterReader.readRaster(outFile.getAbsolutePath()))) {
                assertEquals(cols, written.getCols());
                assertEquals(rows, written.getRows());
                List<HMRasterTile> tiles = written.getTiles(32);
                List<Double> validValues = new java.util.ArrayList<>();
                for( HMRasterTile tile : tiles ) {
                    for( int r = tile.getRow(); r < tile.getRow() + tile.getHeight(); r++ ) {
                        for( int c = tile.getCol(); c < tile.getCol() + tile.getWidth(); c++ ) {
                            double value = written.getValue(c, r);
                            if (tile.getIndex() % 2 == 1 || HMConstants.isNovalue(data[r][c])) {
                                assertTrue("cell " + c + "/" + r, written.isNovalue(value));
                            } else {
                                assertEquals("cell " + c + "/" + r, data[r][c], value, 0.0);
                                validValues.add(value);
                            }
                        }
                    }
                }

                // the exact statistics of the written values are in the GDAL sidecar
                File auxFile = HMRasterStatistics.getAuxXmlFile(outFile);
                assertTrue(auxFile.exists());
                String aux = new String(java.nio.file.Files.readAllBytes(auxFile.toPath()));
                assertFalse(aux.contains("APPROXIMATE"));
                assertStatistics(validValues, (long) cols * rows, aux, "MDI key=\"%s\">");
            }
        } finally {
            inFile.delete();
            outFile.delete();
            HMRasterStatistics.getAuxXmlFile(outFile).delete();
        }
    }

    public void testStatisticsAccumulator() throws Exception {
        Random random = new Random(5);
        List<Double> values = new java.util.ArrayList<>();
        HMRasterStatistics merged = new HMRasterStatistics();
        HMRasterStatistics part = new HMRasterStatistics();
        for( int i = 0; i < 100000; i++ ) {
            double value = 1e6 + random.nextGaussian() * 30;
            values.add(value);
            part.add(value);
            // uneven chunks, merged as the tiles are
            if (random.nextDouble() < 0.001) {
                merged.merge(part);
                part = new HMRasterStatistics();
            }
        }
        merged.merge(part);
        merged.merge(new HMRasterStatistics());
        assertStatistics(values, values.size(), merged.toGdalMetadataTag(values.size()), "Item name=\"%s\" sample=\"0\">");
    }

    /**
     * Compare the statistics in GDAL xml with a two pass computation of the values.
     */
    static void assertStatistics( List<Double> values, long totalCells, String xml, String keyPattern ) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        for( double value : values ) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }
        double mean = sum / values.size();
        double squares = 0;
        for( double value : values ) {
            squares += (value - mean) * (value - mean);
        }
        double stddev = Math.sqrt(squares / values.size());
        assertEquals(min, xmlValue(xml, keyPattern, "STATISTICS_MINIMUM"), 0.0);
        assertEquals(max, xmlValue(xml, keyPattern, "STATISTICS_MAXIMUM"), 0.0);
        assertEquals(mean, xmlValue(xml, keyPattern, "STATISTICS_MEAN"), Math.abs(mean) * 1e-12);
        assertEquals(stddev, xmlValue(xml, keyPattern, "STATISTICS_STDDEV"), stddev * 1e-9 + 1e-12);
        assertEquals(100.0 * values.size() / totalCells, xmlValue(xml, keyPattern, "STATISTICS_VALID_PERCENT"), 1e-9);
    }

    private static double xmlValue( String xml, String keyPattern, String key ) {
        String start = String.format(keyPattern, key);
        int from = xml.indexOf(start);
        assertTrue("missing " + key + " in " + xml, from >= 0);
        from += start.length();
        return Double.parseDouble(xml.substring(from, xml.indexOf('<', from)));
    }
}
