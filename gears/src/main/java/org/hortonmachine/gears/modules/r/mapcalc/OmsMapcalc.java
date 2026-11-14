/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gears.modules.r.mapcalc;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_IN_RASTERS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_P_FUNCTION_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMAPCALC_STATUS;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.PointOpImage;
import org.eclipse.imagen.SourcelessOpImage;
import org.eclipse.imagen.TileCache;
import org.eclipse.imagen.TiledImage;
import org.eclipse.imagen.media.jiffle.Jiffle;
import org.eclipse.imagen.media.jiffle.runtime.AbstractJiffleRuntime;
import org.eclipse.imagen.media.jiffle.runtime.AffineCoordinateTransform;
import org.eclipse.imagen.media.jiffle.runtime.CoordinateTransform;
import org.eclipse.imagen.media.jiffle.runtime.JiffleDirectRuntime;
import org.eclipse.imagen.media.jiffle.runtime.JiffleIndirectRuntime;
import org.eclipse.imagen.media.jiffle.runtime.NullProgressListener;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.utils.jaitools.ImageUtils;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSMAPCALC_DESCRIPTION)
@Documentation(OMSMAPCALC_DOCUMENTATION)
@Author(name = OMSMAPCALC_AUTHORNAMES, contact = OMSMAPCALC_AUTHORCONTACTS)
@Keywords(OMSMAPCALC_KEYWORDS)
@Label(OMSMAPCALC_LABEL)
@Name(OMSMAPCALC_NAME)
@Status(OMSMAPCALC_STATUS)
@License(OMSMAPCALC_LICENSE)
public class OmsMapcalc extends HMModel {

    @Description(OMSMAPCALC_IN_RASTERS_DESCRIPTION)
    @In
    public List<GridCoverage2D> inRasters;

    @Description(OMSMAPCALC_P_FUNCTION_DESCRIPTION)
    @UI(HMConstants.MULTILINE_UI_HINT + "10," + HMConstants.MAPCALC_UI_HINT)
    @In
    public String pFunction;

    @Description("Store the output as float instead of double, halving its memory. The calculation is still done in double.")
    @In
    public boolean doFloat = false;

    @Description("Compute the output only when it is read, a tile at a time, instead of all in memory: "
            + "for huge maps written to file. Each read of the output computes it again. "
            + "Scripts that carry values from a cell to the next, like counters, give different results.")
    @In
    public boolean doStream = false;

    @Description("The number of threads computing the output when streaming. More threads are faster, "
            + "but need more memory: start with 1 and raise it if the memory allows.")
    @In
    public int pThreads = 1;

    @Description(OMSMAPCALC_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    private RegionMap regionParameters = null;

    private CoordinateReferenceSystem crs;

    private Rectangle2D worldBounds;

    private long updateInterval;
    private long totalCount = 100;

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }

        String script = pFunction;
        script = script.trim();

        Jiffle jiffle = new Jiffle();
        jiffle.setScript(script);
        jiffle.compile();
        // the direct runtime walks all the map writing into an image, the indirect one gives the
        // value of a single cell, when asked for it
        AbstractJiffleRuntime jiffleRuntime = (AbstractJiffleRuntime) (doStream
                ? jiffle.getRuntimeInstance(Jiffle.RuntimeModel.INDIRECT)
                : jiffle.getRuntimeInstance());

        CoordinateTransform jiffleCRS = null;
        Rectangle gridBounds = null;
        List<TileCache> sourceCaches = new ArrayList<>();
        Map<String, RenderedImage> sourceImages = new LinkedHashMap<>();

        // gather maps
        for( GridCoverage2D mapGC : inRasters ) {
            if (regionParameters == null) {
                regionParameters = CoverageUtilities.getRegionParamsFromGridCoverage(mapGC);
                crs = mapGC.getCoordinateReferenceSystem();

                ReferencedEnvelope envelope2d = mapGC.getEnvelope2D();
                
//                TODO check this!
				worldBounds = new Rectangle2D.Double(envelope2d.getMinX(), envelope2d.getMinY(),
						envelope2d.getWidth(), envelope2d.getHeight());
                gridBounds = mapGC.getGridGeometry().getGridRange2D().getBounds();

                jiffleCRS = getTransform(worldBounds, gridBounds);

                double xRes = regionParameters.xres;
                double yRes = regionParameters.yres;
                jiffleRuntime.setWorldByResolution(worldBounds, xRes, yRes);
            }
            // the novalues of the map become NaN, the null of Jiffle
            NovalueToNaNOpImage renderedImage = new NovalueToNaNOpImage(mapGC, doStream, doStream && pThreads > 1);
            sourceCaches.add(renderedImage.getTileCache());
            // add map
            String name = mapGC.getName().toString();
            jiffleRuntime.setSourceImage(name, renderedImage, jiffleCRS);
            sourceImages.put(name, renderedImage);
        }
        if (regionParameters == null) {
            throw new ModelsIllegalargumentException("No map has been supplied.", this.getClass().getSimpleName(), pm);
        }
        // a map read by the script and not supplied would fail only when evaluated
        Set<String> suppliedNames = new TreeSet<>(jiffleRuntime.get_images().keySet());
        for( String sourceName : jiffleRuntime.getSourceVarNames() ) {
            if (!suppliedNames.contains(sourceName)) {
                throw new ModelsIllegalargumentException("The script reads the map " + sourceName
                        + ", which is not among the supplied maps: " + String.join(", ", suppliedNames)
                        + ". Maps are known by their file name without extension.", this.getClass().getSimpleName(), pm);
            }
        }
        int nCols = regionParameters.cols;
        int nRows = regionParameters.rows;
        long pixelsNum = (long) nCols * nRows;

        if (pixelsNum < totalCount) {
            totalCount = pixelsNum;
        }
        updateInterval = pixelsNum / totalCount;

        String destName = jiffleRuntime.getDestinationVarNames()[0];
        if (!assignsVariable(script, destName)) {
            throw new ModelsIllegalargumentException("The script never assigns a value to the output map " + destName
                    + ", which would be all 0. Assign it in the script, as in: " + destName + " = ...;",
                    this.getClass().getSimpleName(), pm);
        }
        if (doStream) {
            // a runtime keeps the state of the script while evaluating a cell, so each thread
            // computes its tiles with a runtime of its own, on the same sources
            int threads = Math.max(1, pThreads);
            List<JiffleIndirectRuntime> runtimes = new ArrayList<>();
            ((JiffleIndirectRuntime) jiffleRuntime).setDestinationImage(destName, jiffleCRS);
            runtimes.add((JiffleIndirectRuntime) jiffleRuntime);
            for( int i = 1; i < threads; i++ ) {
                AbstractJiffleRuntime runtime = (AbstractJiffleRuntime) jiffle.getRuntimeInstance(Jiffle.RuntimeModel.INDIRECT);
                runtime.setWorldByResolution(worldBounds, regionParameters.xres, regionParameters.yres);
                // the transform keeps the position being transformed: one for each runtime
                CoordinateTransform runtimeCRS = getTransform(worldBounds, gridBounds);
                for( Map.Entry<String, RenderedImage> source : sourceImages.entrySet() ) {
                    runtime.setSourceImage(source.getKey(), source.getValue(), runtimeCRS);
                }
                ((JiffleIndirectRuntime) runtime).setDestinationImage(destName, runtimeCRS);
                runtimes.add((JiffleIndirectRuntime) runtime);
            }
            StreamOpImage streamImg = new StreamOpImage(runtimes, nCols, nRows, worldBounds, regionParameters.xres,
                    regionParameters.yres, doFloat);
            outRaster = CoverageUtilities.buildCoverage(destName, streamImg, regionParameters, crs);
            return;
        }

        // Jiffle writes its double results casting them to the type of the image
        TiledImage destImg = ImageUtils.createConstantImage(nCols, nRows, doFloat ? (Number) 0f : (Number) 0d);
        JiffleDirectRuntime directRuntime = (JiffleDirectRuntime) jiffleRuntime;
        directRuntime.setDestinationImage(destName, destImg, jiffleCRS);

        try {
            directRuntime.evaluateAll(new NullProgressListener());
        } finally {
            // the converted strips of the sources are not needed any more
            for( TileCache cache : sourceCaches ) {
                cache.flush();
            }
        }

        // the null results of Jiffle become the novalue of the HortonMachine, in place, tile by
        // tile, so that the output is never copied
        for( int ty = destImg.getMinTileY(); ty <= destImg.getMaxTileY(); ty++ ) {
            for( int tx = destImg.getMinTileX(); tx <= destImg.getMaxTileX(); tx++ ) {
                WritableRaster tile = destImg.getWritableTile(tx, ty);
                try {
                    Rectangle tb = tile.getBounds().intersection(destImg.getBounds());
                    for( int r = tb.y; r < tb.y + tb.height; r++ ) {
                        for( int c = tb.x; c < tb.x + tb.width; c++ ) {
                            if (Double.isNaN(tile.getSampleDouble(c, r, 0))) {
                                tile.setSample(c, r, 0, HMConstants.doubleNovalue);
                            }
                        }
                    }
                } finally {
                    destImg.releaseWritableTile(tx, ty);
                }
            }
        }
        outRaster = CoverageUtilities.buildCoverage(destName, destImg, regionParameters, crs);

//        // create the executor
//        JiffleExecutor executor = new JiffleExecutor();
//        JiffleEventListener listener = new JiffleEventListener(){
//            
//            @Override
//            public void onFailureEvent( JiffleEvent event ) {
//             
//            }
//            
//            @Override
//            public void onCompletionEvent( JiffleEvent event ) {
//                JiffleExecutorResult result = event.getResult();
//                Map<String, RenderedImage> imgMap = result.getImages();
//                RenderedImage destImage = imgMap.get(destName);
//                outRaster = CoverageUtilities.buildCoverage(destName, destImage, regionParameters, crs);
//                executor.shutdown();
//            }
//        };
//        executor.addEventListener(listener);
//
//        executor.submit(jiffleRuntime, new JiffleProgressListener(){
//            private long count = 0;
//            public void update( long done ) {
//                if (count == done) {
//                    pm.worked(1);
//                    count = count + updateInterval;
//                }
//            }
//
//            public void start() {
//                pm.beginTask("Processing maps...", (int) totalCount);
//            }
//
//            public void setUpdateInterval( double propPixels ) {
//            }
//
//            public void setUpdateInterval( long numPixels ) {
//            }
//
//            public void setTaskSize( long numPixels ) {
//                count = updateInterval;
//            }
//
//            public long getUpdateInterval() {
//                if (updateInterval == 0) {
//                    return 1;
//                }
//                return updateInterval;
//            }
//
//            public void finish() {
//                pm.done();
//            }
//        });


    }

    /**
     * Checks if the statements of a script assign a variable, outside of its comments and of the
     * images block, where the output map is declared. Jiffle accepts an output map that is never
     * assigned, and leaves it at the constant value of the destination image.
     *
     * @param script the Jiffle script.
     * @param name the name of the variable.
     * @return <code>true</code> if the variable is assigned somewhere.
     */
    public static boolean assignsVariable( String script, String name ) {
        String statements = script.replaceAll("(?s)/\\*.*?\\*/", " ") //
                .replaceAll("//[^\\n]*", " ") //
                .replaceAll("(?s)\\bimages\\s*\\{.*?\\}", " ");
        Pattern assignment = Pattern.compile("\\b" + Pattern.quote(name) + "\\s*([-+*/%]?=(?!=)|\\+\\+|--)");
        return assignment.matcher(statements).find();
    }

    /**
     * The transform from the world positions of the script to the image cells: the rows grow
     * towards the south, so north is up, and each position goes to the cell that contains it.
     * 
     * <p>
     * Jiffle rounds the transformed positions to the nearest cell, and walks the world from the
     * south-west corner of each cell: the half cell shift makes the rounding pick the cell that
     * contains a position, and the epsilon keeps the corners of the walk, on the edges between
     * two cells, in their cell.
     */
    private static CoordinateTransform getTransform( Rectangle2D worldBounds, Rectangle imageBounds ) {
        if (worldBounds == null || worldBounds.isEmpty()) {
            throw new IllegalArgumentException("worldBounds must not be null or empty");
        }
        if (imageBounds == null || imageBounds.isEmpty()) {
            throw new IllegalArgumentException("imageBounds must not be null or empty");
        }
        double epsilon = 1E-6;

        double xscale = (imageBounds.getMaxX() - imageBounds.getMinX()) / (worldBounds.getMaxX() - worldBounds.getMinX());
        double xoff = imageBounds.getMinX() - xscale * worldBounds.getMinX() - 0.5 + epsilon;

        double yscale = (imageBounds.getMaxY() - imageBounds.getMinY()) / (worldBounds.getMaxY() - worldBounds.getMinY());
        double yoff = imageBounds.getMinY() + yscale * worldBounds.getMaxY() - 0.5 - epsilon;

        return new AffineCoordinateTransform(new AffineTransform(xscale, 0, 0, -yscale, xoff, yoff));
    }

    /**
     * The image of a map as doubles, with its novalues set to NaN, the null of Jiffle.
     *
     * <p>
     * The conversion is lazy: the image is made of strips as wide as the map, computed when
     * Jiffle reads them and kept in a small cache of their own, so that the map is never copied
     * as a whole. Jiffle walks the map by rows, so the strip in use, and those around it for the
     * neighbourhood references, stay in the cache.
     *
     * <p>
     * When streaming, the output is computed in tiles of {@link StreamOpImage#TILE_SIZE} rows,
     * from west to east: the cache then holds a whole band of tiles, plus the strips around it,
     * so that the strips are not converted again for each tile of the band.
     *
     * <p>
     * The strips follow the rows of tiles of the source, as the blocks of a tiled GeoTIFF read
     * lazily: each block is then decoded once, instead of once for each strip that crosses it,
     * since the default tile cache can't hold a whole row of blocks of a huge map.
     *
     * <p>
     * The strips are floats when the type of the source fits a float without losing values, as
     * bytes, shorts and floats, halving their memory, doubles otherwise.
     */
    private static class NovalueToNaNOpImage extends PointOpImage {
        /** The size of a strip, in cells. */
        private static final long STRIP_CELLS = 512 * 1024;
        /** The tallest tiles of a source that the strips follow. */
        private static final int MAX_ALIGNED_TILE_ROWS = 1024;
        /** The minimum memory of the strips cache of each map, in bytes. */
        private static final long CACHE_BYTES = 64L * 1024 * 1024;
        /** The rows converted at once, to keep the temporary arrays small. */
        private static final int CHUNK_CELLS = 1024 * 1024;

        private final double novalue;

        /**
         * @param map the map.
         * @param stream if the output is streamed, read by bands of tiles.
         * @param prefetch if the next band of tiles is computed while the current one is read.
         */
        NovalueToNaNOpImage( GridCoverage2D map, boolean stream, boolean prefetch ) {
            super(map.getRenderedImage(), layout(map.getRenderedImage()), null, true);
            novalue = HMConstants.getNovalue(map);
            long cellBytes = DataBuffer.getDataTypeSize(getSampleModel().getDataType()) / 8;
            long stripBytes = (long) getTileHeight() * getWidth() * cellBytes;
            // the strip in use and those around it, for the neighbourhood references
            long cacheBytes = Math.max(CACHE_BYTES, 3 * stripBytes);
            if (stream) {
                // the bands of output tiles being computed, plus the strips around them
                long bandRows = (prefetch ? 2L : 1L) * StreamOpImage.TILE_SIZE + 2L * getTileHeight();
                cacheBytes = Math.max(CACHE_BYTES, bandRows * getWidth() * cellBytes + stripBytes);
            }
            setTileCache(ImageN.createTileCache(cacheBytes));
        }

        private static int dataType( RenderedImage source ) {
            switch( source.getSampleModel().getDataType() ) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_FLOAT:
                return DataBuffer.TYPE_FLOAT;
            default:
                return DataBuffer.TYPE_DOUBLE;
            }
        }

        private static ImageLayout layout( RenderedImage source ) {
            int width = source.getWidth();
            int stripRows = (int) Math.max(1, STRIP_CELLS / width);
            int sourceTileRows = source.getTileHeight();
            int gridYOffset = source.getMinY();
            if (sourceTileRows <= MAX_ALIGNED_TILE_ROWS) {
                // whole rows of tiles of the source
                stripRows = (stripRows + sourceTileRows - 1) / sourceTileRows * sourceTileRows;
                gridYOffset = source.getTileGridYOffset();
            }
            stripRows = Math.min(source.getHeight(), stripRows);
            SampleModel sm = new ComponentSampleModel(dataType(source), width, stripRows, 1, width, new int[]{0});
            ImageLayout layout = new ImageLayout(source.getMinX(), source.getMinY(), width, source.getHeight());
            layout.setTileGridXOffset(source.getMinX());
            layout.setTileGridYOffset(gridYOffset);
            layout.setTileWidth(width);
            layout.setTileHeight(stripRows);
            layout.setSampleModel(sm);
            layout.setColorModel(PlanarImage.createColorModel(sm));
            return layout;
        }

        /**
         * The threads computing the output in parallel ask for the same strips at once: one
         * computes a strip, the others wait for it and then find it in the cache, instead of each
         * converting it again.
         */
        @Override
        public synchronized Raster getTile( int tileX, int tileY ) {
            return super.getTile(tileX, tileY);
        }

        @Override
        protected void computeRect( Raster[] sources, WritableRaster dest, Rectangle destRect ) {
            Raster source = sources[0];
            int chunkRows = Math.max(1, Math.min(destRect.height, CHUNK_CELLS / destRect.width));
            double[] values = null;
            for( int y = destRect.y; y < destRect.y + destRect.height; y += chunkRows ) {
                int rows = Math.min(chunkRows, destRect.y + destRect.height - y);
                values = source.getSamples(destRect.x, y, destRect.width, rows, 0, values);
                int count = destRect.width * rows;
                for( int i = 0; i < count; i++ ) {
                    if (HMConstants.isNovalue(values[i], novalue)) {
                        values[i] = Double.NaN;
                    }
                }
                dest.setSamples(destRect.x, y, destRect.width, rows, 0, values);
            }
        }
    }

    /**
     * The output map computed only when it is read, a tile at a time, through the indirect
     * runtime of Jiffle, with the null results already set to the novalue.
     *
     * <p>
     * Each cell is evaluated at the world position the direct runtime gives it, walking from the
     * south-west corner of the map, so that the transform of {@link #getTransform} reads the same
     * cells of the sources.
     *
     * <p>
     * The tiles are computed in parallel, a band of tiles at a time: when a tile of a band is
     * asked for, all the tiles of the band are computed by a pool of threads, a tile by a thread,
     * each with a runtime of its own, since a runtime keeps the state of the script. With more
     * threads, the next band is computed meanwhile, while the tiles of the current one are read,
     * as by a writer that compresses them.
     */
    private static class StreamOpImage extends SourcelessOpImage {
        /** The tile size, the same as the tiles of the written GeoTIFFs. */
        static final int TILE_SIZE = 512;

        private final BlockingQueue<JiffleIndirectRuntime> runtimes;
        private final ThreadPoolExecutor pool;
        /** If the next band is computed while the current one is read. */
        private final boolean prefetch;
        /** The tiles of the bands being computed or ready, by band index. */
        private final Map<Integer, List<Future<Raster>>> bands = new HashMap<>();
        private final int rows;
        private final double minX;
        private final double minY;
        private final double xRes;
        private final double yRes;

        StreamOpImage( List<JiffleIndirectRuntime> runtimes, int cols, int rows, Rectangle2D worldBounds, double xRes,
                double yRes, boolean isFloat ) {
            super(layout(cols, rows, isFloat), null, sampleModel(cols, rows, isFloat), 0, 0, cols, rows);
            this.runtimes = new ArrayBlockingQueue<>(runtimes.size(), false, runtimes);
            this.prefetch = runtimes.size() > 1;
            this.rows = rows;
            this.minX = worldBounds.getMinX();
            this.minY = worldBounds.getMinY();
            this.xRes = xRes;
            this.yRes = yRes;
            // built outside of the operations registry, the image has no tile cache: one band of
            // tiles keeps the reads by rows from computing the tiles again for each row
            long bandBytes = (long) getTileHeight() * cols * DataBuffer.getDataTypeSize(getSampleModel().getDataType()) / 8;
            setTileCache(ImageN.createTileCache(bandBytes + bandBytes / 4));

            // daemon threads that end when idle, so that a forgotten image doesn't keep them
            AtomicInteger threadCount = new AtomicInteger();
            pool = new ThreadPoolExecutor(runtimes.size(), runtimes.size(), 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
                    r -> {
                        Thread thread = new Thread(r, "hm-mapcalc-" + threadCount.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    });
            pool.allowCoreThreadTimeOut(true);
        }

        @Override
        public Raster computeTile( int tileX, int tileY ) {
            Future<Raster> tile = takeTile(tileX, tileY);
            try {
                return tile.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new RuntimeException(cause);
            }
        }

        /**
         * Takes a tile out of its band, submitting the band if not yet there, together with the
         * next band when there are more threads, and forgetting the other bands: once handed out,
         * a tile is kept only by the tile cache.
         *
         * @return the tile being computed or ready.
         */
        private synchronized Future<Raster> takeTile( int tileX, int tileY ) {
            List<Future<Raster>> band = submitBand(tileY);
            if (prefetch && tileY < getMaxTileY()) {
                submitBand(tileY + 1);
            }
            bands.keySet().removeIf(y -> y < tileY || y > tileY + 1);
            Future<Raster> tile = band.set(tileX - getMinTileX(), null);
            if (tile == null) {
                // handed out already, and then dropped by the tile cache: computed again
                tile = pool.submit(() -> computeOneTile(tileX, tileY));
            }
            return tile;
        }

        private List<Future<Raster>> submitBand( int tileY ) {
            return bands.computeIfAbsent(tileY, y -> {
                List<Future<Raster>> band = new ArrayList<>();
                for( int tileX = getMinTileX(); tileX <= getMaxTileX(); tileX++ ) {
                    int tx = tileX;
                    band.add(pool.submit(() -> computeOneTile(tx, y)));
                }
                return band;
            });
        }

        private Raster computeOneTile( int tileX, int tileY ) throws InterruptedException {
            WritableRaster dest = createWritableRaster(getSampleModel(), new Point(tileXToX(tileX), tileYToY(tileY)));
            Rectangle destRect = getTileRect(tileX, tileY);
            JiffleIndirectRuntime runtime = runtimes.take();
            try {
                compute(runtime, dest, destRect);
            } finally {
                runtimes.put(runtime);
            }
            return dest;
        }

        private static SampleModel sampleModel( int cols, int rows, boolean isFloat ) {
            int tileWidth = Math.min(TILE_SIZE, cols);
            int tileHeight = Math.min(TILE_SIZE, rows);
            int dataType = isFloat ? DataBuffer.TYPE_FLOAT : DataBuffer.TYPE_DOUBLE;
            return new ComponentSampleModel(dataType, tileWidth, tileHeight, 1, tileWidth, new int[]{0});
        }

        private static ImageLayout layout( int cols, int rows, boolean isFloat ) {
            SampleModel sm = sampleModel(cols, rows, isFloat);
            ImageLayout layout = new ImageLayout(0, 0, cols, rows);
            layout.setTileGridXOffset(0);
            layout.setTileGridYOffset(0);
            layout.setTileWidth(sm.getWidth());
            layout.setTileHeight(sm.getHeight());
            layout.setSampleModel(sm);
            layout.setColorModel(PlanarImage.createColorModel(sm));
            return layout;
        }

        private void compute( JiffleIndirectRuntime runtime, WritableRaster dest, Rectangle destRect ) {
            double[] values = new double[destRect.width * destRect.height];
            double[] pixel = new double[1];
            int i = 0;
            for( int r = destRect.y; r < destRect.y + destRect.height; r++ ) {
                double y = minY + (rows - 1 - r) * yRes;
                for( int c = destRect.x; c < destRect.x + destRect.width; c++ ) {
                    // a cell the script leaves unassigned is 0, as in the direct runtime
                    pixel[0] = 0;
                    runtime.evaluate(minX + c * xRes, y, pixel);
                    values[i++] = Double.isNaN(pixel[0]) ? HMConstants.doubleNovalue : pixel[0];
                }
            }
            dest.setSamples(destRect.x, destRect.y, destRect.width, destRect.height, 0, values);
        }
    }

}
