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
package org.hortonmachine.gears.libs.modules.hmraster;

import org.hortonmachine.gears.libs.modules.HMRaster;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.PlanarImage;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.util.factory.Hints;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * Writes rasters to GeoTIFF files tile by tile, computing the tiles on demand.
 *
 * <p>This allows to write rasters that do not fit in memory: the tiles are computed in
 * parallel, a few ahead of the writers, and dropped once written. The files are tiled with
 * the same tile size, Deflate compressed and switched to BigTIFF when needed.</p>
 *
 */
public class HMRasterTiledWriter {

    /**
     * Deflate is lossless at any level. The writer compresses in a single thread, and level 3
     * (1 + 8 * quality) was measured 11 times faster than level 9, for 4% larger DEM files.
     */
    private static final float DEFLATE_QUALITY = 0.25f;

    /**
     * The computation of the values of a tile.
     */
    @FunctionalInterface
    public static interface TileComputer {
        /**
         * @param tile the tile to compute.
         * @return the values of the tile, in row-major order, with novalues as NaN or as the
         *          novalue of the output. It can be <code>null</code> for a tile of only novalues.
         * @throws Exception
         */
        double[] computeTile( HMRasterTile tile ) throws Exception;
    }

    /**
     * The computation of the values of a tile for several outputs.
     */
    @FunctionalInterface
    public static interface MultiTileComputer {
        /**
         * @param tile the tile to compute.
         * @return the values of the tile for each output, see {@link TileComputer#computeTile(HMRasterTile)}.
         *          The array itself can be <code>null</code> for a tile of only novalues in every output.
         * @throws Exception
         */
        double[][] computeTile( HMRasterTile tile ) throws Exception;
    }

    /**
     * An output file.
     */
    public static class TiledOutput {
        final String path;
        final int dataType;
        final double novalue;

        /**
         * @param path the path of the GeoTIFF to write.
         * @param dataType the {@link DataBuffer} data type of the file.
         * @param novalue the novalue of the file.
         */
        public TiledOutput( String path, int dataType, double novalue ) {
            this.path = path;
            this.dataType = dataType;
            this.novalue = novalue;
        }
    }

    /**
     * Write a GeoTIFF computing its tiles on demand.
     *
     * @param pm optional progress monitor.
     * @param path the path of the file to write.
     * @param template the raster giving region, crs, novalue and data type of the output.
     * @param tileSize the size of the tiles, a multiple of 16.
     * @param threads the number of threads computing the tiles.
     * @param computer the computation of a tile. It is called concurrently.
     * @throws Exception
     */
    public static void writeGeotiff( IHMProgressMonitor pm, String path, HMRaster template, int tileSize, int threads,
            TileComputer computer ) throws Exception {
        TiledOutput output = new TiledOutput(path, template.getRenderedImage().getSampleModel().getDataType(),
                template.getNovalue());
        writeGeotiffs(pm, template, tileSize, threads, Collections.singletonList(output), tile -> {
            double[] values = computer.computeTile(tile);
            return values == null ? null : new double[][]{values};
        });
    }

    /**
     * Write several GeoTIFFs computing each tile once for all of them.
     *
     * @param pm optional progress monitor.
     * @param template the raster giving region and crs of the outputs.
     * @param tileSize the size of the tiles, a multiple of 16.
     * @param threads the number of threads computing the tiles.
     * @param outputs the output files.
     * @param computer the computation of a tile for all outputs. It is called concurrently.
     * @throws Exception
     */
    public static void writeGeotiffs( IHMProgressMonitor pm, HMRaster template, int tileSize, int threads,
            List<TiledOutput> outputs, MultiTileComputer computer ) throws Exception {
        if (tileSize < 16 || tileSize % 16 != 0) {
            throw new ModelsIllegalargumentException("The tile size of a GeoTIFF has to be a multiple of 16.",
                    HMRasterTiledWriter.class);
        }
        if (pm == null) {
            pm = new DummyProgressMonitor();
        }
        int cols = template.getCols();
        int rows = template.getRows();
        List<HMRasterTile> tiles = HMRasterTile.createGrid(cols, rows, tileSize);
        int outputsCount = outputs.size();

        RenderedImage templateImage = template.getRenderedImage();
        SampleModel[] sampleModels = new SampleModel[outputsCount];
        ColorModel[] colorModels = new ColorModel[outputsCount];
        for( int o = 0; o < outputsCount; o++ ) {
            int dataType = outputs.get(o).dataType;
            if (dataType == templateImage.getSampleModel().getDataType()) {
                sampleModels[o] = templateImage.getSampleModel().createCompatibleSampleModel(tileSize, tileSize);
                colorModels[o] = templateImage.getColorModel();
            } else {
                sampleModels[o] = new PixelInterleavedSampleModel(dataType, tileSize, tileSize, 1, tileSize, new int[]{0});
                colorModels[o] = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
                        Transparency.OPAQUE, dataType);
            }
        }

        IHMProgressMonitor _pm = pm;
        int computeThreads = Math.max(1, threads);
        ExecutorService computeExecutor = Executors.newFixedThreadPool(computeThreads);
        ExecutorService writersExecutor = Executors.newFixedThreadPool(outputsCount);
        boolean success = false;
        StringBuilder names = new StringBuilder();
        for( TiledOutput output : outputs ) {
            names.append(names.length() > 0 ? ", " : "").append(new File(output.path).getName());
        }
        _pm.beginTask("Writing " + names + "...", tiles.size());
        try {
            SharedTiles shared = new SharedTiles(tiles, outputsCount, computeExecutor, 2 * computeThreads, tile -> {
                if (_pm.isCanceled()) {
                    throw new InterruptedException("Process cancelled.");
                }
                double[][] values = computer.computeTile(tile);
                Raster[] rasters = new Raster[outputsCount];
                for( int o = 0; o < outputsCount; o++ ) {
                    rasters[o] = toRaster(tile, tileSize, sampleModels[o], outputs.get(o).novalue,
                            values == null ? null : values[o]);
                }
                _pm.worked(1);
                return rasters;
            });

            List<Future< ? >> writers = new ArrayList<>();
            for( int o = 0; o < outputsCount; o++ ) {
                TiledOutput output = outputs.get(o);
                ImageLayout layout = new ImageLayout(0, 0, cols, rows, 0, 0, tileSize, tileSize, sampleModels[o],
                        colorModels[o]);
                TilesImage image = new TilesImage(layout, shared, o);
                writers.add(writersExecutor.submit(() -> {
                    try {
                        GridCoverage2D coverage = CoverageUtilities.buildCoverageWithNovalue(template.getName(), image,
                                template.getRegionMap(), template.getCrs(), output.novalue);
                        write(output, coverage, cols, rows, tileSize);
                    } catch (Exception | Error e) {
                        shared.fail();
                        throw e;
                    }
                    return null;
                }));
            }
            for( Future< ? > writer : writers ) {
                try {
                    writer.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    throw e;
                }
            }
            success = !_pm.isCanceled();
        } finally {
            computeExecutor.shutdownNow();
            writersExecutor.shutdownNow();
            _pm.done();
            if (!success) {
                for( TiledOutput output : outputs ) {
                    File file = new File(output.path);
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
        }
    }

    private static Raster toRaster( HMRasterTile tile, int tileSize, SampleModel sampleModel, double novalue, double[] values ) {
        double[] tileValues = new double[tileSize * tileSize];
        Arrays.fill(tileValues, novalue);
        if (values != null) {
            for( int r = 0; r < tile.getHeight(); r++ ) {
                for( int c = 0; c < tile.getWidth(); c++ ) {
                    double value = values[r * tile.getWidth() + c];
                    if (!Double.isNaN(value)) {
                        tileValues[r * tileSize + c] = value;
                    }
                }
            }
        }
        WritableRaster raster = Raster.createWritableRaster(sampleModel, new Point(tile.getCol(), tile.getRow()));
        raster.setSamples(tile.getCol(), tile.getRow(), tileSize, tileSize, 0, tileValues);
        return raster;
    }

    private static void write( TiledOutput output, GridCoverage2D coverage, int cols, int rows, int tileSize ) throws Exception {
        GeoTiffFormat format = new GeoTiffFormat();
        GeoTiffWriteParams wp = new GeoTiffWriteParams();
        wp.setTilingMode(GeoTiffWriteParams.MODE_EXPLICIT);
        wp.setTiling(tileSize, tileSize);
        wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        wp.setCompressionType("Deflate");
        wp.setCompressionQuality(DEFLATE_QUALITY);
        long bytes = (long) cols * rows * (DataBuffer.getDataTypeSize(output.dataType) / 8L);
        if (bytes > OmsRasterWriter.BIGTIFF_THRESHOLD_BYTES) {
            wp.setForceToBigTIFF(true);
        }
        ParameterValueGroup paramWrite = format.getWriteParameters();
        paramWrite.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
        GeoTiffWriter writer = new GeoTiffWriter(new File(output.path),
                new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE));
        try {
            writer.write(coverage, (GeneralParameterValue[]) paramWrite.values().toArray(new GeneralParameterValue[1]));
        } finally {
            writer.dispose();
        }
    }

    @FunctionalInterface
    private interface RastersComputer {
        Raster[] compute( HMRasterTile tile ) throws Exception;
    }

    /**
     * The tiles computed once for all the outputs.
     *
     * <p>The writers request the tiles in row-major order, so when a tile is requested, the
     * following ones are submitted to the executor, to keep the threads busy while the writers
     * compress. A computed tile is dropped once every output has taken it. A writer that runs
     * too far ahead of the others waits, so that the kept tiles stay bounded.</p>
     */
    private static class SharedTiles {
        private final List<HMRasterTile> tiles;
        private final int outputsCount;
        private final ExecutorService executor;
        private final int prefetch;
        private final int maxLead;
        private final RastersComputer computer;
        private final Map<Integer, Future<Raster[]>> computed = new HashMap<>();
        private final Map<Integer, Integer> takenCount = new HashMap<>();
        private final int[] positions;
        private int nextToSubmit = 0;
        private boolean failed = false;

        SharedTiles( List<HMRasterTile> tiles, int outputsCount, ExecutorService executor, int prefetch,
                RastersComputer computer ) {
            this.tiles = tiles;
            this.outputsCount = outputsCount;
            this.executor = executor;
            this.prefetch = prefetch;
            this.maxLead = 4 * prefetch;
            this.computer = computer;
            this.positions = new int[outputsCount];
            Arrays.fill(positions, -1);
        }

        synchronized Future<Raster[]> take( int output, int index ) throws InterruptedException {
            while( !failed && index > minPosition() + maxLead ) {
                wait();
            }
            if (failed) {
                throw new IllegalStateException("The writing of another output failed.");
            }
            positions[output] = Math.max(positions[output], index);
            Future<Raster[]> future = computed.get(index);
            if (future == null) {
                future = submit(index);
                computed.put(index, future);
            }
            if (nextToSubmit <= index) {
                nextToSubmit = index + 1;
            }
            while( nextToSubmit < tiles.size() && nextToSubmit <= index + prefetch ) {
                if (!computed.containsKey(nextToSubmit)) {
                    computed.put(nextToSubmit, submit(nextToSubmit));
                }
                nextToSubmit++;
            }
            int taken = takenCount.merge(index, 1, Integer::sum);
            if (taken >= outputsCount) {
                computed.remove(index);
                takenCount.remove(index);
            }
            notifyAll();
            return future;
        }

        synchronized void fail() {
            failed = true;
            notifyAll();
        }

        private int minPosition() {
            int min = Integer.MAX_VALUE;
            for( int position : positions ) {
                min = Math.min(min, position);
            }
            return min;
        }

        private Future<Raster[]> submit( int index ) {
            HMRasterTile tile = tiles.get(index);
            return executor.submit(() -> computer.compute(tile));
        }
    }

    /**
     * The image of one output, whose tiles come from the shared computation.
     *
     * <p>The TIFF writer can read a tile in several bands of rows, so the last delivered
     * tiles are kept, to not take them again.</p>
     */
    private static class TilesImage extends PlanarImage {
        private static final int DELIVERED_CACHE_SIZE = 4;

        private final SharedTiles shared;
        private final int output;
        private final int tileCols;
        private final Map<Integer, Future<Raster[]>> delivered = new LinkedHashMap<Integer, Future<Raster[]>>(){
            @Override
            protected boolean removeEldestEntry( Map.Entry<Integer, Future<Raster[]>> eldest ) {
                return size() > DELIVERED_CACHE_SIZE;
            }
        };

        TilesImage( ImageLayout layout, SharedTiles shared, int output ) {
            super(layout, null, null);
            this.shared = shared;
            this.output = output;
            this.tileCols = getNumXTiles();
        }

        @Override
        public Raster getTile( int tileX, int tileY ) {
            int index = tileY * tileCols + tileX;
            try {
                Future<Raster[]> future;
                synchronized (delivered) {
                    future = delivered.get(index);
                    if (future == null) {
                        future = shared.take(output, index);
                        delivered.put(index, future);
                    }
                }
                return future.get()[output];
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }
}
