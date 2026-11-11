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
package org.hortonmachine.nww.layers.defaults.raster;

import java.awt.Color;
import java.awt.image.DataBuffer;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.SwingUtilities;

import org.geotools.api.style.Style;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.time.DatedFilesScanner.DatedFile;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer.RasterInfo;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer.RasterKind;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.AbstractLayer;
import gov.nasa.worldwind.render.DrawContext;

/**
 * The rasters of a time series, as monthly precipitations, one file per date, shown one date
 * at a time with the same style, so that the dates can be compared.
 *
 * <p>Each date is drawn by a {@link GridCoverageNwwLayer} of its file, made when the date is
 * first shown, in the background: the former date stays on the map until the new one is ready.
 * The layers of the last shown dates are kept, the tiles of all dates stay in the tiles cache,
 * so that going back to a date is fast.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TemporalRasterNwwLayer extends AbstractLayer implements NwwLayer {

    /** The layers of the dates kept, besides the shown one. */
    private static final int KEPT_DATES = 8;

    /** The most files read for the value range of the default style. */
    public static final int RANGE_SAMPLES = 12;

    /**
     * The value range of the files of a series.
     *
     * @param min the minimum.
     * @param max the maximum.
     * @param readFiles the number of files read.
     * @param totalFiles the number of files of the series.
     * @param lowResolution if the files were read at a low resolution, being large.
     */
    public record Range( double min, double max, int readFiles, int totalFiles, boolean lowResolution ) {
        /**
         * @return a note for the user about how the range was read.
         */
        public String note() {
            String files = readFiles == totalFiles ? "all the " + totalFiles + " dates" : readFiles + " of " + totalFiles + " dates";
            return "Range of " + files + (lowResolution ? ", read at a low resolution." : ".");
        }
    }

    private final File folder;
    private final String pattern;
    private final List<DatedFile> files;
    private final DateTimeFormatter dateFormatter;
    private final Coordinate center;
    private final Envelope bounds;
    private volatile Style style;

    /** The layers of the dates, by index, the last used last: touched only by the loader. */
    private final Map<Integer, GridCoverageNwwLayer> dateLayers = new LinkedHashMap<>(16, 0.75f, true);
    /** Makes the layers of the dates, one at a time. */
    private final ExecutorService loader = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "hm-temporal-raster");
        thread.setDaemon(true);
        return thread;
    });

    /** The layer drawn, of the shown date. */
    private volatile GridCoverageNwwLayer shown;
    private volatile int shownIndex;
    /** The last date asked: the dates asked before it are skipped, as while dragging a slider. */
    private volatile int requestedIndex;

    private final List<IntConsumer> dateListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Position>> pointListeners = new CopyOnWriteArrayList<>();

    /**
     * Make the layer, showing the first date. It reads the first file, so it is made out of the
     * event thread.
     *
     * @param name the name of the layer.
     * @param folder the folder of the files.
     * @param pattern the pattern of the dates in the names of the files.
     * @param files the files, sorted by date, at least one.
     * @param style the style of all the dates, <code>null</code> for the one of each file.
     * @throws Exception if the first file can't be read.
     */
    public TemporalRasterNwwLayer( String name, File folder, String pattern, List<DatedFile> files, Style style )
            throws Exception {
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No dated files.");
        }
        this.folder = folder;
        this.pattern = pattern;
        this.files = List.copyOf(files);
        this.style = style;
        this.dateFormatter = DateTimeFormatter.ofPattern(labelPattern(pattern));
        setName(name);
        setPickEnabled(false);

        GridCoverageNwwLayer first = makeDateLayer(0);
        dateLayers.put(0, first);
        shown = first;
        shownIndex = 0;
        requestedIndex = 0;
        center = first.getCenter();
        bounds = first.getBounds();
    }

    /**
     * @return the pattern of the labels of the dates: as precise as the pattern of the names.
     */
    private static String labelPattern( String pattern ) {
        if (pattern == null) {
            return "yyyy-MM-dd";
        }
        if (pattern.contains("H")) {
            return pattern.contains("m") ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd HH:00";
        }
        if (pattern.contains("d") || pattern.contains("D")) {
            return "yyyy-MM-dd";
        }
        return pattern.contains("M") ? "yyyy-MM" : "yyyy";
    }

    /**
     * The default style of a series: as {@link GridCoverageNwwLayer#createDefaultStyle(File)},
     * the elevation colortable for the rasters of values, on the range of the series, read from
     * at most {@link #RANGE_SAMPLES} files spread over the dates, as
     * {@link #readRange(List, int, int)}.
     *
     * @param files the files of the series.
     * @return the style, <code>null</code> for the rasters drawn well with their own colors, as RGB
     *          images, or if no file has valid values.
     * @throws Exception if the files can't be read.
     */
    public static Style createDefaultStyle( List<DatedFile> files ) throws Exception {
        RasterInfo info = GridCoverageNwwLayer.readRasterInfo(files.get(0).file());
        if (info.kind() != RasterKind.VALUES || (info.bandsCount() == 1 && info.dataType() == DataBuffer.TYPE_BYTE)) {
            return null;
        }
        Range range = readRange(files, 0, RANGE_SAMPLES);
        if (range == null) {
            return null;
        }
        double max = range.max() > range.min() ? range.max() : range.min() + 1;
        Style style = RasterStyleUtilities.createStyleForColortable(EColorTables.elev.name(), range.min(), max, 1.0);
        // the name of the colortable, as the style editor does
        style.featureTypeStyles().get(0).rules().get(0).setName(EColorTables.elev.name());
        if (info.bandsCount() > 1) {
            RasterStyleUtilities.setGrayChannel(style, 0);
        }
        return style;
    }

    /**
     * The value range of a band of the files of a series, as
     * {@link GridCoverageNwwLayer#readStatistics(File, int)}: reading all the cells of each file,
     * or at a low resolution for the large ones. The overviews read at a low resolution can be
     * resampled smoothly, with values out of the range of the data.
     *
     * @param files the files of the series.
     * @param band the 0 based index of the band.
     * @param maxFiles the most files read, spread over the dates with the first and the last.
     * @return the range, <code>null</code> if no file has valid values.
     * @throws Exception if a file can't be read.
     */
    public static Range readRange( List<DatedFile> files, int band, int maxFiles ) throws Exception {
        List<File> read = sample(files, maxFiles);
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        boolean lowResolution = false;
        for( File file : read ) {
            lowResolution |= GridCoverageNwwLayer.isLargeRaster(file);
            double[] stats = GridCoverageNwwLayer.readStatistics(file, band);
            if (stats != null) {
                min = Math.min(min, stats[0]);
                max = Math.max(max, stats[1]);
            }
        }
        if (min > max) {
            return null;
        }
        return new Range(min, max, read.size(), files.size(), lowResolution);
    }

    private static List<File> sample( List<DatedFile> files, int maxFiles ) {
        List<File> sample = new ArrayList<>();
        int count = files.size();
        if (count <= maxFiles) {
            for( DatedFile datedFile : files ) {
                sample.add(datedFile.file());
            }
            return sample;
        }
        int last = -1;
        for( int i = 0; i < maxFiles; i++ ) {
            int index = (int) Math.round(i * (count - 1) / (double) (maxFiles - 1));
            if (index != last) {
                sample.add(files.get(index).file());
                last = index;
            }
        }
        return sample;
    }

    /**
     * @return the folder of the files.
     */
    public File getFolder() {
        return folder;
    }

    /**
     * @return the pattern of the dates in the names of the files.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @return the files with their dates, sorted by date.
     */
    public List<DatedFile> getFiles() {
        return files;
    }

    /**
     * @param index the index of a date.
     * @return the date as text, as precise as the names of the files, as 2025-01 for monthly
     *          files.
     */
    public String getDateLabel( int index ) {
        return files.get(index).dateTime().format(dateFormatter);
    }

    /**
     * @return the index of the date shown on the map.
     */
    public int getIndex() {
        return shownIndex;
    }

    /**
     * @return the index of the last date asked with {@link #setIndex(int)}, shown or still
     *          being made.
     */
    public int getRequestedIndex() {
        return requestedIndex;
    }

    /**
     * @return the file of the date shown on the map.
     */
    public File getCurrentFile() {
        return files.get(shownIndex).file();
    }

    /**
     * @return the style of all the dates, <code>null</code> if each file has its own.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * Show a date. The layer of the date is made in the background if needed, the shown date
     * changes when it is ready and the date listeners are told.
     *
     * @param index the index of the date.
     */
    public void setIndex( int index ) {
        if (index < 0 || index >= files.size()) {
            throw new IndexOutOfBoundsException(index);
        }
        if (loader.isShutdown()) {
            return;
        }
        requestedIndex = index;
        loader.execute(() -> {
            if (requestedIndex != index) {
                // a later date was asked meanwhile
                return;
            }
            try {
                show(index, dateLayer(index));
                // the next date, as when playing
                if (index + 1 < files.size() && requestedIndex == index) {
                    dateLayer(index + 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Draw all the dates with a new style. The layer of the shown date is made at once, so it is
     * called out of the event thread.
     *
     * @param newStyle the style, <code>null</code> for the one of each file.
     * @throws Exception if the file of the shown date can't be read.
     */
    public void setStyle( Style newStyle ) throws Exception {
        try {
            loader.submit(() -> {
                style = newStyle;
                List<GridCoverageNwwLayer> old = new ArrayList<>(dateLayers.values());
                dateLayers.clear();
                int index = requestedIndex;
                show(index, dateLayer(index));
                for( GridCoverageNwwLayer layer : old ) {
                    layer.dispose();
                }
                return null;
            }).get();
        } catch (java.util.concurrent.ExecutionException e) {
            throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e;
        }
    }

    /**
     * @return the layer of a date, made if needed and kept: of the layers kept, the least
     *          recently used are dropped. Called by the loader.
     */
    private GridCoverageNwwLayer dateLayer( int index ) throws Exception {
        GridCoverageNwwLayer layer = dateLayers.get(index);
        if (layer != null) {
            return layer;
        }
        layer = makeDateLayer(index);
        dateLayers.put(index, layer);
        Iterator<Map.Entry<Integer, GridCoverageNwwLayer>> iterator = dateLayers.entrySet().iterator();
        while( dateLayers.size() > KEPT_DATES + 1 && iterator.hasNext() ) {
            GridCoverageNwwLayer eldest = iterator.next().getValue();
            if (eldest != shown && eldest != layer) {
                iterator.remove();
                eldest.dispose();
            }
        }
        return layer;
    }

    private GridCoverageNwwLayer makeDateLayer( int index ) throws Exception {
        GridCoverageNwwLayer layer = new GridCoverageNwwLayer(files.get(index).file(), null, Color.WHITE, style);
        layer.setName(getName() + " " + getDateLabel(index));
        // the tiles loaded tell the map to draw again through this layer
        layer.addPropertyChangeListener(this);
        return layer;
    }

    private void show( int index, GridCoverageNwwLayer layer ) {
        shown = layer;
        shownIndex = index;
        firePropertyChange(AVKey.LAYER, null, this);
        SwingUtilities.invokeLater(() -> {
            for( IntConsumer listener : dateListeners ) {
                listener.accept(index);
            }
        });
    }

    /**
     * @param listener told in the event thread of the index of each date shown on the map.
     */
    public void addDateListener( IntConsumer listener ) {
        dateListeners.add(listener);
    }

    /**
     * @param listener the listener to remove.
     */
    public void removeDateListener( IntConsumer listener ) {
        dateListeners.remove(listener);
    }

    /**
     * @param listener told of the points of the map whose values through time are asked, as by a
     *          click of the identify mode.
     */
    public void addPointListener( Consumer<Position> listener ) {
        pointListeners.add(listener);
    }

    /**
     * @param listener the listener to remove.
     */
    public void removePointListener( Consumer<Position> listener ) {
        pointListeners.remove(listener);
    }

    /**
     * Ask the values of a point of the map through time, telling the point listeners.
     *
     * @param position the point.
     * @return <code>true</code> if a listener was told.
     */
    public boolean askPoint( Position position ) {
        for( Consumer<Position> listener : pointListeners ) {
            listener.accept(position);
        }
        return !pointListeners.isEmpty();
    }

    @Override
    protected void doPreRender( DrawContext dc ) {
        GridCoverageNwwLayer layer = shown;
        layer.setOpacity(getOpacity());
        layer.preRender(dc);
    }

    @Override
    protected void doRender( DrawContext dc ) {
        GridCoverageNwwLayer layer = shown;
        layer.setOpacity(getOpacity());
        layer.render(dc);
    }

    /**
     * Delete the tiles of all the files from the cache, of all their styles.
     */
    public void clearCache() {
        for( DatedFile datedFile : files ) {
            GridCoverageNwwLayer.clearFileCache(datedFile.file());
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        loader.execute(() -> {
            for( GridCoverageNwwLayer layer : dateLayers.values() ) {
                layer.dispose();
            }
            dateLayers.clear();
        });
        loader.shutdown();
        dateListeners.clear();
        pointListeners.clear();
    }

    @Override
    public Coordinate getCenter() {
        return center;
    }

    @Override
    public Envelope getBounds() {
        return bounds;
    }

    @Override
    public String toString() {
        return getName();
    }
}
