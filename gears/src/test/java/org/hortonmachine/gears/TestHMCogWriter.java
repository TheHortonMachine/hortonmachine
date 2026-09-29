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

import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.cog.HMCogWriter;
import org.hortonmachine.gears.io.cog.HMCogWriter.Compression;
import org.hortonmachine.gears.io.cog.HMCogWriter.Resampling;
import org.hortonmachine.gears.io.cog.OmsRasterToCog;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterStatistics;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

/**
 * Test the {@link HMCogWriter}.
 *
 * <p>The files are read back with the imageio-ext TIFF reader, independent from the writer, and compared 
 * with a reference pyramid computed in the test.</p>
 */
public class TestHMCogWriter extends HMTestCase {
    private static final double NV = -9999.0;
    private static final int COLS = 700;
    private static final int ROWS = 530;
    private static final int TILE = 256;

    public void testAllTypesAndCompressions() throws Exception {
        for( int dataType : new int[]{DataBuffer.TYPE_BYTE, DataBuffer.TYPE_SHORT, DataBuffer.TYPE_INT, DataBuffer.TYPE_FLOAT,
                DataBuffer.TYPE_DOUBLE} ) {
            HMRaster raster = createRaster(dataType);
            for( Compression compression : Compression.values() ) {
                for( boolean predictor : new boolean[]{true, false} ) {
                    HMCogWriter writer = new HMCogWriter().setTileSize(TILE).setCompression(compression)
                            .setUsePredictor(predictor);
                    checkCog(raster, writer, Resampling.AUTO, "type " + dataType + " " + compression + " " + predictor);
                }
            }
            raster.close();
        }
    }

    public void testResamplingsAndBigTiff() throws Exception {
        try (HMRaster raster = createRaster(DataBuffer.TYPE_INT)) {
            for( Resampling resampling : Resampling.values() ) {
                HMCogWriter writer = new HMCogWriter().setTileSize(TILE).setResampling(resampling);
                checkCog(raster, writer, resampling, "resampling " + resampling);
            }
            checkCog(raster, new HMCogWriter().setTileSize(TILE).setForceBigTiff(true), Resampling.AUTO, "bigtiff");
        }
    }

    public void testGeoTiffFileWithModule() throws Exception {
        File inFile = File.createTempFile("cog_in_", ".tif");
        File outFile = File.createTempFile("cog_out_", ".tif");
        try (HMRaster raster = createRaster(DataBuffer.TYPE_FLOAT)) {
            OmsRasterWriter.writeRaster(inFile.getAbsolutePath(), raster.buildCoverage());
            OmsRasterToCog toCog = new OmsRasterToCog();
            toCog.inRaster = inFile.getAbsolutePath();
            toCog.outCog = outFile.getAbsolutePath();
            toCog.pTileSize = TILE;
            toCog.pm = pm;
            toCog.process();
            try (HMRaster input = HMRaster.fromFile(inFile.getAbsolutePath())) {
                verify(input, outFile, Resampling.AVERAGE, "geotiff file");
            }
        } finally {
            inFile.delete();
            outFile.delete();
        }
    }

    /**
     * NaN holes have to be copied as NaN, whatever the novalue of the file.
     */
    public void testNanHolesAreCopiedExactly() throws Exception {
        float[][] data = new float[ROWS][COLS];
        for( int r = 0; r < ROWS; r++ ) {
            for( int c = 0; c < COLS; c++ ) {
                boolean hole = (r < 100 && c < 80) || (r >= 300 && r < 305);
                data[r][c] = hole ? Float.NaN : (r * COLS + c) % 97 + 0.25f;
            }
        }
        RegionMap region = RegionMap.fromBoundsAndGrid(600000, 600000 + COLS * 5, 5000000, 5000000 + ROWS * 5, COLS, ROWS);
        File inFile = File.createTempFile("cog_nan_in_", ".tif");
        File outFile = File.createTempFile("cog_nan_out_", ".tif");
        try {
            OmsRasterWriter.writeRaster(inFile.getAbsolutePath(),
                    CoverageUtilities.buildCoverage("nan", data, region, HMTestMaps.getCrs(), true));
            new HMCogWriter().setTileSize(TILE).convert2Cog(pm, inFile.getAbsolutePath(), outFile.getAbsolutePath());

            try (FileImageInputStream stream = new FileImageInputStream(outFile)) {
                ImageReader reader = new TIFFImageReaderSpi().createReaderInstance();
                reader.setInput(stream);
                Raster full = reader.read(0).getRaster();
                for( int r = 0; r < ROWS; r++ ) {
                    for( int c = 0; c < COLS; c++ ) {
                        float value = full.getSampleFloat(c, r, 0);
                        if (Float.isNaN(data[r][c])) {
                            assertTrue("NaN expected at " + c + "/" + r + ", got " + value, Float.isNaN(value));
                        } else {
                            assertEquals(c + "/" + r, data[r][c], value, 0f);
                        }
                    }
                }
                // in the first overview the upper left hole is still a hole, the thin band is not
                Raster overview = reader.read(1).getRaster();
                double hole = overview.getSampleDouble(10, 10, 0);
                assertTrue("overview hole expected, got " + hole, Double.isNaN(hole) || hole == -9999.0);
                assertFalse(Double.isNaN(overview.getSampleDouble(100, 151, 0)));
                reader.dispose();
            }
        } finally {
            inFile.delete();
            outFile.delete();
        }
    }

    private void checkCog( HMRaster raster, HMCogWriter writer, Resampling resampling, String label ) throws Exception {
        File outFile = File.createTempFile("cog_", ".tif");
        try {
            writer.write(pm, raster, outFile.getAbsolutePath());
            verify(raster, outFile, resampling, label);
        } finally {
            outFile.delete();
        }
    }

    private void verify( HMRaster raster, File cog, Resampling resampling, String label ) throws Exception {
        int dataType = raster.getRenderedImage().getSampleModel().getDataType();
        boolean floatingPoint = dataType == DataBuffer.TYPE_FLOAT || dataType == DataBuffer.TYPE_DOUBLE;
        if (resampling == Resampling.AUTO) {
            resampling = floatingPoint ? Resampling.AVERAGE : Resampling.MODE;
        }
        List<double[][]> pyramid = referencePyramid(raster, resampling, !floatingPoint);

        // decoded by the imageio-ext TIFF reader
        try (FileImageInputStream stream = new FileImageInputStream(cog)) {
            ImageReader reader = new TIFFImageReaderSpi().createReaderInstance();
            reader.setInput(stream);
            assertEquals(label + " levels", pyramid.size(), reader.getNumImages(true));
            for( int level = 0; level < pyramid.size(); level++ ) {
                double[][] expected = pyramid.get(level);
                Raster data = reader.read(level).getRaster();
                assertEquals(label + " level " + level + " width", expected[0].length, data.getWidth());
                assertEquals(label + " level " + level + " height", expected.length, data.getHeight());
                for( int r = 0; r < expected.length; r++ ) {
                    for( int c = 0; c < expected[0].length; c++ ) {
                        double value = data.getSampleDouble(c, r, 0);
                        double expectedValue = Double.isNaN(expected[r][c]) ? raster.getNovalue() : expected[r][c];
                        if (dataType == DataBuffer.TYPE_FLOAT) {
                            expectedValue = (float) expectedValue;
                        }
                        assertEquals(label + " level " + level + " cell " + c + "/" + r, expectedValue, value, 0.0);
                    }
                }
            }

            // the exact statistics of the full resolution valid values are stored in the file
            TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
            TIFFField statisticsField = metadata.getTIFFField(42112);
            assertNotNull(label + " statistics", statisticsField);
            List<Double> validValues = new ArrayList<>();
            for( double[] row : pyramid.get(0) ) {
                for( double value : row ) {
                    if (!Double.isNaN(value)) {
                        validValues.add(dataType == DataBuffer.TYPE_FLOAT ? (float) value : value);
                    }
                }
            }
            TestHMRasterTiles.assertStatistics(validValues, (long) raster.getCols() * raster.getRows(),
                    statisticsField.getAsString(0), "Item name=\"%s\" sample=\"0\">");
            reader.dispose();
        }
        assertFalse(label, HMRasterStatistics.getAuxXmlFile(cog).exists());

        // georeferencing
        GridCoverage2D coverage = OmsRasterReader.readRaster(cog.getAbsolutePath());
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(coverage);
        RegionMap expectedRegion = raster.getRegionMap();
        assertEquals(label, expectedRegion.getWest(), region.getWest(), 1e-6);
        assertEquals(label, expectedRegion.getNorth(), region.getNorth(), 1e-6);
        assertEquals(label, expectedRegion.getXres(), region.getXres(), 1e-9);
        assertEquals(label, expectedRegion.getYres(), region.getYres(), 1e-9);
        // a crs read from geotiff keys never equals the original object in every detail, not even with GeoTools'
        // own writer, so the identity is checked through the authority code
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        assertEquals(label + " crs", HMCrsRegistry.getCodeFromCrs(raster.getCrs()), HMCrsRegistry.getCodeFromCrs(crs));
        assertEquals(label + " novalue", raster.getNovalue(), CoverageUtilities.getNovalue(coverage), 0.0);
    }

    /**
     * The expected levels, NaN for novalues, computed independently from the writer.
     */
    private List<double[][]> referencePyramid( HMRaster raster, Resampling resampling, boolean integerData ) {
        List<double[][]> levels = new ArrayList<>();
        int w = raster.getCols();
        int h = raster.getRows();
        double[][] level = new double[h][w];
        for( int r = 0; r < h; r++ ) {
            for( int c = 0; c < w; c++ ) {
                double value = raster.getValue(c, r);
                level[r][c] = raster.isNovalue(value) ? Double.NaN : value;
            }
        }
        levels.add(level);
        while( w > TILE || h > TILE ) {
            int pw = (w + 1) / 2;
            int ph = (h + 1) / 2;
            double[][] parent = new double[ph][pw];
            for( int r = 0; r < ph; r++ ) {
                for( int c = 0; c < pw; c++ ) {
                    List<Double> cells = new ArrayList<>();
                    for( int[] d : new int[][]{{0, 0}, {1, 0}, {0, 1}, {1, 1}} ) {
                        int cc = 2 * c + d[0];
                        int rr = 2 * r + d[1];
                        cells.add(cc < w && rr < h ? level[rr][cc] : Double.NaN);
                    }
                    parent[r][c] = resample(cells, resampling, integerData);
                }
            }
            levels.add(parent);
            level = parent;
            w = pw;
            h = ph;
        }
        return levels;
    }

    private static double resample( List<Double> cells, Resampling resampling, boolean integerData ) {
        if (resampling == Resampling.NEAREST) {
            return cells.get(0);
        }
        if (resampling == Resampling.MODE) {
            double best = Double.NaN;
            int bestCount = 0;
            for( double candidate : cells ) {
                if (Double.isNaN(candidate)) {
                    continue;
                }
                int count = 0;
                for( double other : cells ) {
                    if (other == candidate) {
                        count++;
                    }
                }
                if (count > bestCount) {
                    bestCount = count;
                    best = candidate;
                }
            }
            return best;
        }
        double sum = 0;
        int count = 0;
        for( double cell : cells ) {
            if (!Double.isNaN(cell)) {
                sum += cell;
                count++;
            }
        }
        if (count == 0) {
            return Double.NaN;
        }
        return integerData ? Math.round(sum / count) : sum / count;
    }

    /**
     * A raster of the data type with smooth values, repeated values (for the mode) and novalue areas.
     */
    private HMRaster createRaster( int dataType ) throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(600000, 600000 + COLS * 5, 5000000, 5000000 + ROWS * 5, COLS, ROWS);
        Random random = new Random(dataType);
        double[][] data = new double[ROWS][COLS];
        for( int r = 0; r < ROWS; r++ ) {
            for( int c = 0; c < COLS; c++ ) {
                boolean nodata = (c < 60 && r < 90) || random.nextDouble() < 0.02;
                double value;
                switch( dataType ) {
                case DataBuffer.TYPE_BYTE:
                    value = random.nextInt(4) * 60 + 3;
                    break;
                case DataBuffer.TYPE_SHORT:
                    value = random.nextInt(7) - 3 + (r / 10) * 3;
                    break;
                case DataBuffer.TYPE_INT:
                    value = random.nextInt(5) * 100000 + r * c;
                    break;
                default:
                    value = 1000 + 50 * Math.sin(c / 20.0) * Math.cos(r / 30.0) + random.nextDouble();
                }
                data[r][c] = nodata ? NV : value;
            }
        }
        if (dataType == DataBuffer.TYPE_FLOAT) {
            float[][] floats = new float[ROWS][COLS];
            for( int r = 0; r < ROWS; r++ ) {
                for( int c = 0; c < COLS; c++ ) {
                    floats[r][c] = (float) data[r][c];
                }
            }
            GridCoverage2D coverage = CoverageUtilities.buildCoverage("float", floats, region, HMTestMaps.getCrs(), true);
            return HMRaster.fromGridCoverage(coverage);
        }
        HMRaster.HMRasterWritableBuilder builder = new HMRaster.HMRasterWritableBuilder().setName("data").setRegion(region)
                .setCrs(HMTestMaps.getCrs()).setNoValue(NV);
        if (dataType == DataBuffer.TYPE_BYTE) {
            // no negative bytes: 0 is the novalue
            builder.setDoByte(true).setNoValue(0);
        } else if (dataType == DataBuffer.TYPE_SHORT) {
            builder.setDoShort(true);
        } else if (dataType == DataBuffer.TYPE_INT) {
            builder.setDoInteger(true);
        }
        HMRaster raster = builder.build();
        for( int r = 0; r < ROWS; r++ ) {
            for( int c = 0; c < COLS; c++ ) {
                double value = data[r][c];
                if (dataType == DataBuffer.TYPE_BYTE && value == NV) {
                    value = 0;
                }
                raster.setValue(c, r, value);
            }
        }
        return raster;
    }
}
