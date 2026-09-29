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
package org.hortonmachine.gears.io.cog;

import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.cog.CogTiffAssembler.Field;
import org.hortonmachine.gears.io.cog.CogTiffAssembler.Level;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterStatistics;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import it.geosolutions.imageio.plugins.tiff.TIFFField;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageMetadata;
import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

/**
 * Writer of Cloud Optimized GeoTIFFs (COG), in pure java.
 *
 * <p>The COG is written as the GDAL COG driver does: 512x512 tiles (by default), overviews
 * halving the resolution until the image fits a tile, all the image directories at the beginning
 * of the file, followed by the tiles of the smallest overview first and of the full resolution
 * image last. Tiles are compressed losslessly (Deflate or LZW, with predictor) in parallel.</p>
 *
 * <p>Large rasters are supported: the input is read by windows, the compressed tiles go to a
 * temporary file next to the output, and only about a row of tiles per level is kept in memory.
 * BigTIFF is used when needed.</p>
 */
public class HMCogWriter {

    /**
     * How the cells of an overview are computed from the 2x2 cells of the level above.
     */
    public static enum Resampling {
        /**
         * {@link #AVERAGE} for floating point data, {@link #MODE} for integer data (codes, classes).
         */
        AUTO,
        /**
         * The average of the valid cells, rounded for integer data.
         */
        AVERAGE,
        /**
         * The upper left cell.
         */
        NEAREST,
        /**
         * The most frequent value of the valid cells, the first in reading order on ties.
         */
        MODE
    }

    /**
     * The lossless compression of the tiles.
     */
    public static enum Compression {
        DEFLATE, LZW
    }

    private static final int TAG_NEW_SUBFILE_TYPE = 254;
    private static final int TAG_IMAGE_WIDTH = 256;
    private static final int TAG_IMAGE_LENGTH = 257;
    private static final int TAG_BITS_PER_SAMPLE = 258;
    private static final int TAG_COMPRESSION = 259;
    private static final int TAG_PHOTOMETRIC = 262;
    private static final int TAG_SAMPLES_PER_PIXEL = 277;
    private static final int TAG_PLANAR_CONFIGURATION = 284;
    private static final int TAG_PREDICTOR = 317;
    private static final int TAG_TILE_WIDTH = 322;
    private static final int TAG_TILE_LENGTH = 323;
    private static final int TAG_SAMPLE_FORMAT = 339;
    private static final int TAG_GDAL_NODATA = 42113;
    private static final int TAG_GDAL_METADATA = 42112;
    /**
     * The georeferencing tags: ModelPixelScale, ModelTiepoint, ModelTransformation, GeoKeyDirectory,
     * GeoDoubleParams, GeoAsciiParams.
     */
    private static final int[] GEO_TAGS = {33550, 33922, 34264, 34735, 34736, 34737};

    private int tileSize = 512;
    private Resampling resampling = Resampling.AUTO;
    private Compression compression = Compression.DEFLATE;
    private int deflateLevel = 6;
    private boolean usePredictor = true;
    private int threads = HMModel.getDefaultThreadsNum();
    private boolean forceBigTiff = false;

    public HMCogWriter setTileSize( int tileSize ) {
        this.tileSize = tileSize;
        return this;
    }

    public HMCogWriter setResampling( Resampling resampling ) {
        this.resampling = resampling;
        return this;
    }

    public HMCogWriter setCompression( Compression compression ) {
        this.compression = compression;
        return this;
    }

    public HMCogWriter setDeflateLevel( int deflateLevel ) {
        this.deflateLevel = deflateLevel;
        return this;
    }

    public HMCogWriter setUsePredictor( boolean usePredictor ) {
        this.usePredictor = usePredictor;
        return this;
    }

    public HMCogWriter setThreads( int threads ) {
        this.threads = threads;
        return this;
    }

    public HMCogWriter setForceBigTiff( boolean forceBigTiff ) {
        this.forceBigTiff = forceBigTiff;
        return this;
    }

    /**
     * Convert a raster file to a COG.
     *
     * <p>GeoTIFF files are read by windows, so they can be larger than the memory, and their
     * georeferencing tags are copied as they are. Other formats are read in memory.</p>
     *
     * @param pm optional progress monitor.
     * @param inPath the raster to convert.
     * @param outPath the COG file to write.
     */
    public void convert2Cog( IHMProgressMonitor pm, String inPath, String outPath ) throws Exception {
        String lower = inPath.toLowerCase();
        boolean isTiff = lower.endsWith(".tif") || lower.endsWith(".tiff");
        try (HMRaster raster = isTiff ? HMRaster.fromFileWindowed(inPath) : HMRaster.fromFile(inPath)) {
            List<Field> geoFields = isTiff ? readGeoFields(new File(inPath)) : null;
            write(pm, raster, geoFields, outPath);
        }
    }

    /**
     * Write a raster as COG.
     *
     * @param pm optional progress monitor.
     * @param raster the raster to write.
     * @param outPath the COG file to write.
     */
    public void write( IHMProgressMonitor pm, HMRaster raster, String outPath ) throws Exception {
        write(pm, raster, null, outPath);
    }

    private void write( IHMProgressMonitor pm, HMRaster raster, List<Field> geoFields, String outPath ) throws Exception {
        if (pm == null) {
            pm = new DummyProgressMonitor();
        }
        if (tileSize < 16 || tileSize % 16 != 0) {
            throw new ModelsIllegalargumentException("The tile size has to be a multiple of 16.", this);
        }
        File outFile = new File(outPath).getAbsoluteFile();
        if (geoFields == null) {
            geoFields = geoFieldsFromRaster(raster, outFile.getParentFile());
        }
        int dataType = raster.getRenderedImage().getSampleModel().getDataType();
        CogTileEncoder.bytesPerSample(dataType); // checks the type is supported
        boolean floatingPoint = CogTileEncoder.isFloatingPoint(dataType);
        Resampling method = resampling;
        if (method == Resampling.AUTO) {
            method = floatingPoint ? Resampling.AVERAGE : Resampling.MODE;
        }
        int predictor = !usePredictor
                ? CogTileEncoder.PREDICTOR_NONE
                : floatingPoint ? CogTileEncoder.PREDICTOR_FLOATING_POINT : CogTileEncoder.PREDICTOR_HORIZONTAL;
        int compressionCode = compression == Compression.LZW
                ? CogTileEncoder.COMPRESSION_LZW
                : CogTileEncoder.COMPRESSION_DEFLATE;
        /*
         * the values are copied untouched, novalues included (NaN stays NaN, -9999 stays -9999). The output
         * novalue is only used for overview cells without valid values and for the tile padding: the
         * GDAL_NODATA of the input if it has one, else NaN for floating point data and the raster novalue for integers.
         */
        double outputNovalue = floatingPoint ? Double.NaN : raster.getNovalue();
        for( Field geoField : geoFields ) {
            if (geoField.tag == TAG_GDAL_NODATA) {
                outputNovalue = parseNodata(geoField, outputNovalue);
            }
        }
        double fillValue = Double.isNaN(outputNovalue) && !floatingPoint ? 0 : outputNovalue;

        // levels: full resolution, then halving until the image fits a tile
        List<int[]> sizes = new ArrayList<>();
        int w = raster.getCols();
        int h = raster.getRows();
        sizes.add(new int[]{w, h});
        while( w > tileSize || h > tileSize ) {
            w = (w + 1) / 2;
            h = (h + 1) / 2;
            sizes.add(new int[]{w, h});
        }
        int levelsCount = sizes.size();
        int[] tilesX = new int[levelsCount];
        int[] tilesY = new int[levelsCount];
        int[] tilesPerLevel = new int[levelsCount];
        for( int l = 0; l < levelsCount; l++ ) {
            tilesX[l] = (sizes.get(l)[0] + tileSize - 1) / tileSize;
            tilesY[l] = (sizes.get(l)[1] + tileSize - 1) / tileSize;
            tilesPerLevel[l] = tilesX[l] * tilesY[l];
        }

        File tempFile = new File(outFile.getParentFile(), outFile.getName() + ".tiles.tmp");
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, threads));
        ThreadLocal<CogTileEncoder> encoders = ThreadLocal
                .withInitial(() -> new CogTileEncoder(dataType, tileSize, compressionCode, deflateLevel, predictor, fillValue));
        boolean success = false;
        try (CogTileStore store = new CogTileStore(tempFile, tilesPerLevel)) {
            Pyramid pyramid = new Pyramid(sizes, tilesX, tilesY, method, !floatingPoint, store, encoders, executor, raster,
                    fillValue);
            HMRasterStatistics statistics = new HMRasterStatistics();
            pm.beginTask("Compressing tiles and building " + (levelsCount - 1) + " overviews...", tilesY[0]);
            for( int ty = 0; ty < tilesY[0]; ty++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                int _ty = ty;
                // each full resolution tile is read, compressed and shrunk in the same task, then dropped
                pyramid.processRow(0, ty, tx -> {
                    double[] tile = readTile(raster, tx, _ty, tileSize, fillValue);
                    statistics.merge(tileStatistics(tile, raster, tx, _ty, tileSize, fillValue));
                    return tile;
                });
                pm.worked(1);
            }
            pm.done();

            // the COG layout
            List<Level> levels = new ArrayList<>();
            for( int l = 0; l < levelsCount; l++ ) {
                List<Field> fields = new ArrayList<>();
                if (l > 0) {
                    fields.add(Field.longs(TAG_NEW_SUBFILE_TYPE, 1));
                }
                fields.add(Field.longs(TAG_IMAGE_WIDTH, sizes.get(l)[0]));
                fields.add(Field.longs(TAG_IMAGE_LENGTH, sizes.get(l)[1]));
                fields.add(Field.shorts(TAG_BITS_PER_SAMPLE, 8 * CogTileEncoder.bytesPerSample(dataType)));
                fields.add(Field.shorts(TAG_COMPRESSION, compressionCode));
                fields.add(Field.shorts(TAG_PHOTOMETRIC, 1));
                fields.add(Field.shorts(TAG_SAMPLES_PER_PIXEL, 1));
                fields.add(Field.shorts(TAG_PLANAR_CONFIGURATION, 1));
                if (predictor != CogTileEncoder.PREDICTOR_NONE) {
                    fields.add(Field.shorts(TAG_PREDICTOR, predictor));
                }
                fields.add(Field.longs(TAG_TILE_WIDTH, tileSize));
                fields.add(Field.longs(TAG_TILE_LENGTH, tileSize));
                fields.add(Field.shorts(TAG_SAMPLE_FORMAT, sampleFormat(dataType)));
                for( Field geoField : geoFields ) {
                    // georeferencing only in the full resolution image, nodata everywhere
                    if (l == 0 || geoField.tag == TAG_GDAL_NODATA) {
                        fields.add(geoField);
                    }
                }
                if (l == 0) {
                    // exact statistics, else clients estimate them from a subsample or the smallest overview
                    String metadata = statistics.toGdalMetadataTag((long) raster.getCols() * raster.getRows());
                    if (metadata != null) {
                        fields.add(Field.ascii(TAG_GDAL_METADATA, metadata));
                    }
                }
                levels.add(new Level(tilesPerLevel[l], fields));
            }
            CogTiffAssembler assembler = new CogTiffAssembler(levels, store);
            boolean bigTiff = forceBigTiff || assembler.computeSize(false) > 0xFFFFFFFFL;
            pm.beginTask("Writing " + outFile.getName() + (bigTiff ? " (BigTIFF)..." : "..."), IHMProgressMonitor.UNKNOWN);
            assembler.write(outFile, bigTiff);
            // a sidecar of a previous file with the same name would take precedence over the new statistics
            HMRasterStatistics.getAuxXmlFile(outFile).delete();
            pm.done();
            success = true;
        } finally {
            executor.shutdownNow();
            if (!success && outFile.exists()) {
                outFile.delete();
            }
        }
    }

    /**
     * The statistics of the valid values of a full resolution tile, the padding excluded.
     */
    private static HMRasterStatistics tileStatistics( double[] tile, HMRaster raster, int tx, int ty, int tileSize,
            double outputNovalue ) {
        HMRasterStatistics statistics = new HMRasterStatistics();
        int w = Math.min(tileSize, raster.getCols() - tx * tileSize);
        int h = Math.min(tileSize, raster.getRows() - ty * tileSize);
        for( int r = 0; r < h; r++ ) {
            for( int c = 0; c < w; c++ ) {
                double value = tile[r * tileSize + c];
                if (!Double.isNaN(value) && !raster.isNovalue(value) && value != outputNovalue) {
                    statistics.add(value);
                }
            }
        }
        return statistics;
    }

    /**
     * Read a full resolution tile, with the values as they are and the padding outside the raster.
     */
    private static double[] readTile( HMRaster raster, int tx, int ty, int tileSize, double padding ) {
        double[] tile = new double[tileSize * tileSize];
        Arrays.fill(tile, padding);
        int col = tx * tileSize;
        int row = ty * tileSize;
        int w = Math.min(tileSize, raster.getCols() - col);
        int h = Math.min(tileSize, raster.getRows() - row);
        double[] values = raster.getValues(col, row, w, h, null);
        for( int r = 0; r < h; r++ ) {
            System.arraycopy(values, r * w, tile, r * tileSize, w);
        }
        return tile;
    }

    /**
     * Parse the GDAL_NODATA ascii value.
     */
    private static double parseNodata( Field field, double fallback ) {
        String text = new String(field.data, java.nio.charset.StandardCharsets.US_ASCII).replace("\0", "").trim();
        if (text.equalsIgnoreCase("nan")) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @FunctionalInterface
    private interface TileSupplier {
        double[] get( int tx ) throws Exception;
    }

    /**
     * The overview pyramid, built while the full resolution rows of tiles arrive.
     */
    private class Pyramid {
        private final List<int[]> sizes;
        private final int[] tilesX;
        private final int[] tilesY;
        private final Resampling method;
        private final boolean integerData;
        private final CogTileStore store;
        private final ThreadLocal<CogTileEncoder> encoders;
        private final ExecutorService executor;
        /**
         * Per level, the row of tiles being filled by the level below.
         */
        private final double[][][] pendingRows;
        private final HMRaster raster;
        private final double outputNovalue;

        Pyramid( List<int[]> sizes, int[] tilesX, int[] tilesY, Resampling method, boolean integerData, CogTileStore store,
                ThreadLocal<CogTileEncoder> encoders, ExecutorService executor, HMRaster raster, double outputNovalue ) {
            this.sizes = sizes;
            this.tilesX = tilesX;
            this.tilesY = tilesY;
            this.method = method;
            this.integerData = integerData;
            this.store = store;
            this.encoders = encoders;
            this.executor = executor;
            this.pendingRows = new double[sizes.size()][][];
            this.raster = raster;
            this.outputNovalue = outputNovalue;
        }

        /**
         * @return <code>true</code> if the value is a novalue: NaN, the raster novalue or the output novalue.
         */
        private boolean isMissing( double value ) {
            return Double.isNaN(value) || raster.isNovalue(value) || value == outputNovalue;
        }

        /**
         * Compress a complete row of tiles of a level and shrink it into the level above it.
         *
         * @param level the level.
         * @param ty the row of tiles.
         * @param tiles the supplier of the tiles of the row, called concurrently.
         */
        void processRow( int level, int ty, TileSupplier tiles ) throws Exception {
            boolean hasParent = level + 1 < sizes.size();
            if (hasParent && pendingRows[level + 1] == null) {
                pendingRows[level + 1] = newRow(level + 1);
            }
            double[][] parentRow = hasParent ? pendingRows[level + 1] : null;
            List<Future< ? >> tasks = new ArrayList<>();
            for( int tx = 0; tx < tilesX[level]; tx++ ) {
                int _tx = tx;
                tasks.add(executor.submit(() -> {
                    double[] tile = tiles.get(_tx);
                    store.put(level, ty * tilesX[level] + _tx, encoders.get().encode(tile));
                    if (parentRow != null) {
                        shrinkInto(level, _tx, ty, tile, parentRow[_tx / 2]);
                    }
                    return null;
                }));
            }
            waitFor(tasks);
            if (parentRow != null && (ty % 2 == 1 || ty == tilesY[level] - 1)) {
                pendingRows[level + 1] = null;
                processRow(level + 1, ty / 2, tx -> parentRow[tx]);
            }
        }

        private double[][] newRow( int level ) {
            double[][] row = new double[tilesX[level]][];
            for( int tx = 0; tx < row.length; tx++ ) {
                row[tx] = new double[tileSize * tileSize];
                Arrays.fill(row[tx], outputNovalue);
            }
            return row;
        }

        /**
         * Shrink a tile 2x2 to 1 into its quarter of the parent tile.
         */
        private void shrinkInto( int level, int tx, int ty, double[] tile, double[] parent ) {
            int half = tileSize / 2;
            int quarterCol = (tx % 2) * half;
            int quarterRow = (ty % 2) * half;
            int parentWidth = sizes.get(level + 1)[0];
            int parentHeight = sizes.get(level + 1)[1];
            double[] cells = new double[4];
            for( int r = 0; r < half; r++ ) {
                int parentRowIndex = (ty / 2) * tileSize + quarterRow + r;
                if (parentRowIndex >= parentHeight) {
                    break;
                }
                for( int c = 0; c < half; c++ ) {
                    int parentColIndex = (tx / 2) * tileSize + quarterCol + c;
                    if (parentColIndex >= parentWidth) {
                        break;
                    }
                    int i = (2 * r) * tileSize + 2 * c;
                    cells[0] = tile[i];
                    cells[1] = tile[i + 1];
                    cells[2] = tile[i + tileSize];
                    cells[3] = tile[i + tileSize + 1];
                    parent[(quarterRow + r) * tileSize + quarterCol + c] = resample(cells);
                }
            }
        }

        private double resample( double[] cells ) {
            switch( method ) {
            case NEAREST:
                return cells[0];
            case MODE: {
                double best = outputNovalue;
                int bestCount = 0;
                for( int i = 0; i < 4; i++ ) {
                    if (isMissing(cells[i])) {
                        continue;
                    }
                    int count = 0;
                    for( int j = i; j < 4; j++ ) {
                        if (cells[j] == cells[i]) {
                            count++;
                        }
                    }
                    if (count > bestCount) {
                        bestCount = count;
                        best = cells[i];
                    }
                }
                return best;
            }
            default: {
                double sum = 0;
                int count = 0;
                for( double cell : cells ) {
                    if (!isMissing(cell)) {
                        sum += cell;
                        count++;
                    }
                }
                if (count == 0) {
                    return outputNovalue;
                }
                double average = sum / count;
                return integerData ? Math.round(average) : average;
            }
            }
        }
    }

    private static int sampleFormat( int dataType ) {
        switch( dataType ) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
            return 1; // unsigned integer
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            return 2; // signed integer
        default:
            return 3; // floating point
        }
    }

    private static void waitFor( List<Future< ? >> futures ) throws Exception {
        for( Future< ? > future : futures ) {
            try {
                future.get();
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                throw e;
            }
        }
    }

    /**
     * Read the georeferencing and nodata tags of a GeoTIFF, to copy them as they are.
     */
    static List<Field> readGeoFields( File tiff ) throws IOException {
        List<Field> fields = new ArrayList<>();
        try (FileImageInputStream stream = new FileImageInputStream(tiff)) {
            ImageReader reader = new TIFFImageReaderSpi().createReaderInstance();
            try {
                reader.setInput(stream, true, false);
                TIFFImageMetadata metadata = (TIFFImageMetadata) reader.getImageMetadata(0);
                int[] tags = Arrays.copyOf(GEO_TAGS, GEO_TAGS.length + 1);
                tags[GEO_TAGS.length] = TAG_GDAL_NODATA;
                for( int tag : tags ) {
                    TIFFField field = metadata.getTIFFField(tag);
                    if (field != null) {
                        Field converted = convert(field);
                        if (converted != null) {
                            fields.add(converted);
                        }
                    }
                }
            } finally {
                reader.dispose();
            }
        }
        return fields;
    }

    private static Field convert( TIFFField field ) {
        int tag = field.getTagNumber();
        int count = field.getCount();
        switch( field.getType() ) {
        case CogTiffAssembler.TYPE_SHORT: {
            int[] values = new int[count];
            for( int i = 0; i < count; i++ ) {
                values[i] = field.getAsInt(i);
            }
            return Field.shorts(tag, values);
        }
        case CogTiffAssembler.TYPE_LONG: {
            long[] values = new long[count];
            for( int i = 0; i < count; i++ ) {
                values[i] = field.getAsLong(i);
            }
            return Field.longs(tag, values);
        }
        case CogTiffAssembler.TYPE_DOUBLE: {
            double[] values = new double[count];
            for( int i = 0; i < count; i++ ) {
                values[i] = field.getAsDouble(i);
            }
            return Field.doubles(tag, values);
        }
        case CogTiffAssembler.TYPE_ASCII: {
            StringBuilder sb = new StringBuilder();
            for( int i = 0; i < count; i++ ) {
                if (i > 0) {
                    sb.append('\0');
                }
                sb.append(field.getAsString(i));
            }
            return Field.ascii(tag, sb.toString());
        }
        default:
            return null;
        }
    }

    /**
     * Georeferencing tags for a raster that is not a GeoTIFF: a tiny GeoTIFF with the same
     * origin, resolution, crs and novalue is written with GeoTools and its tags are used.
     */
    private static List<Field> geoFieldsFromRaster( HMRaster raster, File tempDir ) throws Exception {
        RegionMap region = raster.getRegionMap();
        RegionMap small = RegionMap.fromBoundsAndGrid(region.getWest(), region.getWest() + 2 * region.getXres(),
                region.getNorth() - 2 * region.getYres(), region.getNorth(), 2, 2);
        GridCoverage2D coverage = CoverageUtilities.buildCoverageWithNovalue("georef", new double[][]{{0, 0}, {0, 0}},
                small, raster.getCrs(), true, raster.getNovalue());
        File tempTiff = File.createTempFile("cog_georef_", ".tif", tempDir);
        try {
            OmsRasterWriter.writeRaster(tempTiff.getAbsolutePath(), coverage);
            return readGeoFields(tempTiff);
        } finally {
            tempTiff.delete();
        }
    }
}
