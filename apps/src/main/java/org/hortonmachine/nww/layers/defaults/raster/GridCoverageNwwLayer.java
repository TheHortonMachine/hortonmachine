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
package org.hortonmachine.nww.layers.defaults.raster;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.geotools.api.coverage.ColorInterpretation;
import org.geotools.api.coverage.grid.GridEnvelope;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Style;
import org.geotools.api.util.InternationalString;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.SLD;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.crs.CrsUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.nww.utils.cache.CacheUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

/**
 * A raster drawn as image tiles, made when the map shows them and kept in the tiles cache.
 *
 * <p>The raster is read by a GeoTools reader: of a file, or of a remote raster, as a cloud
 * optimized GeoTIFF read with range requests, of which each tile reads only the blocks of the
 * overview of its zoom.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GridCoverageNwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 256;

    /** The cells across the read of a remote or large raster that gives its value range. */
    private static final int STATISTICS_READ_SIZE = 512;

    /**
     * The most cells of a raster file whose statistics are read exactly, from all its cells: the
     * larger ones are read at a low resolution, as from an overview.
     */
    public static final long EXACT_STATISTICS_MAX_CELLS = 25_000_000L;

    private Coordinate centerCoordinate;
    private Envelope boundsLL;

    /** The style the tiles are drawn with. */
    private final Style style;

    /** The reader of a remote raster, disposed with the layer, <code>null</code> for the files. */
    private final AbstractGridCoverage2DReader remoteReader;

    /**
     * The start of the paths in the cache of the tiles folders of the raster, of all its styles,
     * the same for the layers of the same raster.
     */
    private final String cachePrefix;

    /**
     * The renderer of the tiles, with its style and a key of the style for the tiles cache.
     */
    private record StyledRenderer( GTRenderer renderer, Style style, String styleKey ) {
    }

    public GridCoverageNwwLayer( File gridCoverageFile, Integer tileSize, Color colorToMakeTransparent ) throws Exception {
        this(gridCoverageFile, tileSize, colorToMakeTransparent, (Style) null);
    }

    /**
     * @param gridCoverageFile the raster file.
     * @param tileSize the size of the tiles, <code>null</code> for the default.
     * @param colorToMakeTransparent if not <code>null</code>, the empty parts of the tiles are
     *          transparent.
     * @param style the style to draw the raster with, <code>null</code> for the one of the .sld
     *          file next to the raster, if there is one, else a default one.
     * @throws Exception
     */
    public GridCoverageNwwLayer( File gridCoverageFile, Integer tileSize, Color colorToMakeTransparent, Style style )
            throws Exception {
        this(gridCoverageFile, tileSize, colorToMakeTransparent, getRenderer(fileReader(gridCoverageFile), gridCoverageFile, style));
    }

    private GridCoverageNwwLayer( File gridCoverageFile, Integer tileSize, Color colorToMakeTransparent,
            StyledRenderer styledRenderer ) throws Exception {
        this(FileUtilities.getNameWithoutExtention(gridCoverageFile), OmsRasterReader.readEnvelope(gridCoverageFile.getAbsolutePath()),
                styledRenderer, fileCachePrefix(gridCoverageFile), fileCachePath(gridCoverageFile, styledRenderer),
                gridCoverageFile.toURI().toURL().toExternalForm(), tileSize, colorToMakeTransparent, null);
    }

    /**
     * A layer of a raster read by a reader, as a remote cloud optimized GeoTIFF.
     *
     * @param reader the reader, disposed with the layer.
     * @param name the name of the layer.
     * @param cacheKey what tells the raster apart in the tiles cache, as its address.
     * @param tileSize the size of the tiles, <code>null</code> for the default.
     * @param colorToMakeTransparent if not <code>null</code>, the empty parts of the tiles are
     *          transparent.
     * @param style the style to draw the raster with, <code>null</code> for a default one: for
     *          single band rasters of values, a colortable on their range, read from an overview.
     * @throws Exception
     */
    public GridCoverageNwwLayer( AbstractGridCoverage2DReader reader, String name, String cacheKey, Integer tileSize,
            Color colorToMakeTransparent, Style style ) throws Exception {
        this(reader, name, cacheKey, tileSize, colorToMakeTransparent, getRenderer(reader, null, style));
    }

    private GridCoverageNwwLayer( AbstractGridCoverage2DReader reader, String name, String cacheKey, Integer tileSize,
            Color colorToMakeTransparent, StyledRenderer styledRenderer ) throws Exception {
        this(name, ReferencedEnvelope.reference(reader.getOriginalEnvelope()), styledRenderer, remoteCachePrefix(name, cacheKey),
                remoteCachePath(name, cacheKey, styledRenderer), cacheKey, tileSize, colorToMakeTransparent, reader);
    }

    private GridCoverageNwwLayer( String name, ReferencedEnvelope envelope, StyledRenderer styledRenderer, String cachePrefix,
            String cacheRelativePath, String url, Integer tileSize, Color colorToMakeTransparent,
            AbstractGridCoverage2DReader remoteReader ) throws Exception {
        super(makeLevels(cacheRelativePath, url, styledRenderer, tileSize, colorToMakeTransparent));
        this.style = styledRenderer.style();
        this.layerName = name;
        this.remoteReader = remoteReader;
        this.cachePrefix = cachePrefix;

        ReferencedEnvelope envelopeLL = envelope.transform(DefaultGeographicCRS.WGS84, true);
        
        double w = envelopeLL.getMinX();
        double s = envelopeLL.getMinY();
        double e = envelopeLL.getMaxX();
        double n = envelopeLL.getMaxY();
        
        double centerX = w + (e - w) / 2.0;
        double centerY = s + (n - s) / 2.0;
        
        centerCoordinate = new Coordinate(centerX, centerY);
        boundsLL = new Envelope(w, e, s, n);

        this.setUseTransparentTextures(true);

    }

    /**
     * @return the reader of a raster file, with the longitude first axis order, as
     *          {@link OmsRasterReader} does: without the hint, the geographic CRSs, as EPSG:4326,
     *          are read with the latitude first if the forceXY setting was not made before, and
     *          the raster is not found where the map draws it.
     */
    private static AbstractGridCoverage2DReader fileReader( File rasterFile ) {
        AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
        return format.getReader(rasterFile, HMCrsRegistry.longitudeFirstHints());
    }

    /**
     * @param rasterFile the raster file, for its .sld file and statistics, <code>null</code> for
     *          the remote rasters.
     */
    private static StyledRenderer getRenderer( AbstractGridCoverage2DReader coverageReader, File rasterFile,
            Style givenStyle ) {
        MapContent mapContent = new MapContent();
        String styleKey = "default";
        Style rasterStyle = givenStyle;
        try {
            if (rasterStyle != null) {
                // the same style gets the same tiles
                styleKey = "style" + Integer.toHexString(SldUtilities.styleToString(rasterStyle).hashCode());
            } else {
                rasterStyle = rasterFile != null ? SldUtilities.getStyleFromFile(rasterFile) : null;
                if (rasterStyle != null) {
                    styleKey = "sld";
                } else {
                    rasterStyle = makeFullRangeStyle(coverageReader, rasterFile);
                    if (rasterStyle != null) {
                        styleKey = "auto";
                    } else {
                        RasterSymbolizer sym = SldUtilities.sf.getDefaultRasterSymbolizer();
                        rasterStyle = SLD.wrapSymbolizers(sym);
                    }
                }
            }

            GridReaderLayer layer = new GridReaderLayer(coverageReader, rasterStyle);
            mapContent.addLayer(layer);
            mapContent.getViewport().setCoordinateReferenceSystem(CrsUtilities.WGS84);
        } catch (Exception e) {
            e.printStackTrace();
        }
        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);
        return new StyledRenderer(renderer, rasterStyle, styleKey);
    }

    /**
     * @return the style the raster is drawn with.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * @return <code>true</code> for a remote raster, read by a reader given to the layer, as a
     *          cloud optimized GeoTIFF.
     */
    public boolean isRemote() {
        return remoteReader != null;
    }

    /**
     * @return the reader of a remote raster, closed when the layer is disposed, or
     *          <code>null</code> for the files.
     */
    public AbstractGridCoverage2DReader getRemoteReader() {
        return remoteReader;
    }

    /**
     * What a raster is, for its style.
     */
    public enum RasterKind {
        /** An image with a palette, drawn with its own colors. */
        PALETTE,
        /** An image with red, green and blue bands, drawn with its own colors. */
        RGB,
        /** One or more bands of values, that a colortable shows one at a time. */
        VALUES
    }

    /**
     * The structure of a raster, for its style.
     *
     * @param kind what the raster is.
     * @param bandNames the names of the bands, to choose them.
     * @param dataType the type of the data, as {@link DataBuffer#TYPE_BYTE}.
     */
    public record RasterInfo( RasterKind kind, String[] bandNames, int dataType ) {
        /**
         * @return the number of bands.
         */
        public int bandsCount() {
            return bandNames.length;
        }
    }

    /**
     * @param coverageReader the reader of a raster.
     * @return <code>true</code> if the raster has bands of values that a colortable can show, as
     *          {@link #isColortableRaster(File)}.
     */
    public static boolean isColortableRaster( AbstractGridCoverage2DReader coverageReader ) {
        try {
            return readRasterInfo(coverageReader).kind() == RasterKind.VALUES;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param rasterFile a raster file.
     * @return the structure of the raster.
     * @throws Exception if the raster can't be read.
     */
    public static RasterInfo readRasterInfo( File rasterFile ) throws Exception {
        AbstractGridCoverage2DReader coverageReader = fileReader(rasterFile);
        try {
            return readRasterInfo(coverageReader);
        } finally {
            coverageReader.dispose();
        }
    }

    /**
     * The structure of a raster: images with a palette, or whose bands are marked as red, green
     * and blue, have their own colors, the others have bands of values, also of byte data, as
     * the classes or percentages of a land cover.
     *
     * @param coverageReader the reader of a raster.
     * @return the structure of the raster.
     * @throws Exception if the raster can't be read.
     */
    public static RasterInfo readRasterInfo( AbstractGridCoverage2DReader coverageReader ) throws Exception {
        // the image is read lazily, only its structure is used here
        GridCoverage2D coverage = coverageReader.read((GeneralParameterValue[]) null);
        RenderedImage image = coverage.getRenderedImage();
        int dataType = image.getSampleModel().getDataType();
        int bandsCount = coverage.getNumSampleDimensions();
        String[] bandNames = new String[bandsCount];
        boolean rgb = false;
        for( int i = 0; i < bandsCount; i++ ) {
            GridSampleDimension sampleDimension = coverage.getSampleDimension(i);
            ColorInterpretation interpretation = sampleDimension.getColorInterpretation();
            if (interpretation == ColorInterpretation.RED_BAND || interpretation == ColorInterpretation.GREEN_BAND
                    || interpretation == ColorInterpretation.BLUE_BAND) {
                rgb = true;
            }
            bandNames[i] = bandName(i, sampleDimension);
        }
        /*
         * the readers can mark the bands of an RGB GeoTIFF as gray and undefined, its color
         * model is RGB, while the rasters of values have no color space
         */
        ColorModel colorModel = image.getColorModel();
        if (colorModel != null && colorModel.getColorSpace().getType() == ColorSpace.TYPE_RGB) {
            rgb = true;
        }
        RasterKind kind;
        if (colorModel instanceof IndexColorModel) {
            kind = RasterKind.PALETTE;
        } else if (rgb) {
            kind = RasterKind.RGB;
        } else {
            kind = RasterKind.VALUES;
        }
        return new RasterInfo(kind, bandNames, dataType);
    }

    /**
     * @return the name of a band, as "Band 2", with its description, if it is not the generic one
     *          of the readers, as "Band2".
     */
    private static String bandName( int index, GridSampleDimension sampleDimension ) {
        String name = "Band " + (index + 1);
        InternationalString description = sampleDimension.getDescription();
        String text = description == null ? null : description.toString().trim();
        if (text != null && !text.isEmpty() && !text.replace(" ", "").equalsIgnoreCase("band" + (index + 1))) {
            name += " - " + text;
        }
        return name;
    }

    /**
     * The style a raster without .sld file is drawn with: for rasters of values, as for example
     * DEMs or the bands of a land cover, an elevation colortable on the value range of the whole
     * raster, of its first band, else the default raster style, which shows well RGB and palette
     * images, and the single band byte images, as gray.
     *
     * @param rasterFile the raster file.
     * @return the style.
     */
    public static Style createDefaultStyle( File rasterFile ) {
        AbstractGridCoverage2DReader coverageReader = fileReader(rasterFile);
        try {
            Style style = makeFullRangeStyle(coverageReader, rasterFile);
            if (style != null) {
                return style;
            }
            return SLD.wrapSymbolizers(SldUtilities.sf.getDefaultRasterSymbolizer());
        } finally {
            coverageReader.dispose();
        }
    }

    /**
     * @param rasterFile a raster file.
     * @return <code>true</code> if the raster has bands of values that a colortable can show,
     *          <code>false</code> for RGB and palette images, which have their own colors.
     */
    public static boolean isColortableRaster( File rasterFile ) {
        try {
            return readRasterInfo(rasterFile).kind() == RasterKind.VALUES;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Make an elevation colortable style on the value range of the whole raster, for rasters of
     * values, as for example DEMs, on their first band. With the default style, each tile would
     * be stretched on its own values range, and the multi band rasters of values would be drawn
     * as RGB images.
     *
     * @param rasterFile the raster file, whose statistics are read whole if it is not larger
     *          than {@link #EXACT_STATISTICS_MAX_CELLS}, <code>null</code> for the remote rasters.
     *          The remote and large rasters have their range read at a low resolution, as from an
     *          overview.
     * @return the style or <code>null</code> if the raster is shown well by the default style,
     *          as RGB or palette images, and the single band byte images, drawn as gray.
     */
    private static Style makeFullRangeStyle( AbstractGridCoverage2DReader coverageReader, File rasterFile ) {
        try {
            RasterInfo info = readRasterInfo(coverageReader);
            if (info.kind() != RasterKind.VALUES || (info.bandsCount() == 1 && info.dataType() == DataBuffer.TYPE_BYTE)) {
                return null;
            }
            double[] stats;
            if (rasterFile != null && !isLargeRaster(coverageReader)) {
                try (HMRaster raster = HMRaster.fromFile(rasterFile)) {
                    stats = raster.getStatistics();
                }
            } else {
                // remote and large rasters: read at a low resolution, as from an overview
                stats = readLowResolutionStatistics(coverageReader, 0);
            }
            if (stats == null) {
                return null;
            }
            double min = stats[0];
            double max = stats[1];
            if (max <= min) {
                max = min + 1;
            }
            Style style = RasterStyleUtilities.createStyleForColortable(EColorTables.elev.name(), min, max, 1.0);
            // the name of the colortable, as the style editor does
            style.featureTypeStyles().get(0).rules().get(0).setName(EColorTables.elev.name());
            if (info.bandsCount() > 1) {
                RasterStyleUtilities.setGrayChannel(style, 0);
            }
            return style;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param coverageReader the reader of a raster.
     * @return <code>true</code> if the raster has more cells than
     *          {@link #EXACT_STATISTICS_MAX_CELLS}, so that its statistics are read at a low
     *          resolution.
     */
    private static boolean isLargeRaster( AbstractGridCoverage2DReader coverageReader ) {
        GridEnvelope gridRange = coverageReader.getOriginalGridRange();
        return (long) gridRange.getSpan(0) * gridRange.getSpan(1) > EXACT_STATISTICS_MAX_CELLS;
    }

    /**
     * @param rasterFile a raster file.
     * @return <code>true</code> if the raster has more cells than
     *          {@link #EXACT_STATISTICS_MAX_CELLS}, so that {@link #readStatistics(File)} reads
     *          it at a low resolution.
     */
    public static boolean isLargeRaster( File rasterFile ) {
        AbstractGridCoverage2DReader coverageReader = fileReader(rasterFile);
        try {
            return isLargeRaster(coverageReader);
        } finally {
            coverageReader.dispose();
        }
    }

    /**
     * The statistics of a band of a raster file, leaving out the novalues: exact, reading all the
     * cells, for the rasters up to {@link #EXACT_STATISTICS_MAX_CELLS}, else read at a low
     * resolution, as from an overview of a cloud optimized GeoTIFF, since reading all the cells
     * of a large raster can take hours.
     *
     * @param rasterFile the raster file.
     * @param band the 0 based index of the band.
     * @return the statistics, as {@link HMRaster#getStatistics()}.
     * @throws Exception if the raster can't be read.
     */
    public static double[] readStatistics( File rasterFile, int band ) throws Exception {
        AbstractGridCoverage2DReader coverageReader = fileReader(rasterFile);
        try {
            if (isLargeRaster(coverageReader)) {
                return readLowResolutionStatistics(coverageReader, band);
            }
        } finally {
            coverageReader.dispose();
        }
        // the first band is the one read without extracting it
        try (HMRaster raster = HMRaster.fromFile(rasterFile, band == 0 ? null : band)) {
            return raster.getStatistics();
        }
    }

    /**
     * The value range of a band of a raster read at a low resolution, which the readers of cloud
     * optimized GeoTIFFs read from an overview, without reading the whole raster.
     *
     * @param band the 0 based index of the band.
     * @return the statistics, as {@link HMRaster#getStatistics()}.
     */
    public static double[] readLowResolutionStatistics( AbstractGridCoverage2DReader coverageReader, int band )
            throws Exception {
        GridEnvelope gridRange = coverageReader.getOriginalGridRange();
        int cols = gridRange.getSpan(0);
        int rows = gridRange.getSpan(1);
        double scale = Math.min(1.0, (double) STATISTICS_READ_SIZE / Math.max(cols, rows));
        int readCols = Math.max(1, (int) Math.round(cols * scale));
        int readRows = Math.max(1, (int) Math.round(rows * scale));
        ParameterValue<GridGeometry2D> gridGeometry = AbstractGridFormat.READ_GRIDGEOMETRY2D.createValue();
        gridGeometry.setValue(new GridGeometry2D(new GridEnvelope2D(0, 0, readCols, readRows), coverageReader.getOriginalEnvelope()));
        GridCoverage2D coverage = coverageReader.read(new GeneralParameterValue[]{gridGeometry});
        try (HMRaster raster = HMRaster.fromGridCoverage(coverage, band == 0 ? null : band)) {
            return raster.getStatistics();
        }
    }

    /**
     * @return the start of the folders of the tiles of a raster file in the cache: its name and a
     *          hash of its path, which tells apart the files of the same name in different folders.
     */
    private static String fileCachePrefix( File imsf ) {
        return "imagemosaics/" + imsf.getName() + "-" + Integer.toHexString(imsf.getAbsolutePath().hashCode()) + "-";
    }

    /**
     * @return the folder of the tiles of a raster file in the cache, named after the file, its
     *          path and the style, with the times of the file and of its .sld, so that a changed
     *          raster or style gets new tiles.
     */
    private static String fileCachePath( File imsf, StyledRenderer styledRenderer ) {
        long stamp = imsf.lastModified();
        File styleFile = SldUtilities.getStyleFile(imsf);
        if ("sld".equals(styledRenderer.styleKey()) && styleFile != null && styleFile.exists()) {
            stamp = Math.max(stamp, styleFile.lastModified());
        }
        return fileCachePrefix(imsf) + stamp + "-" + styledRenderer.styleKey() + "-tiles";
    }

    /**
     * @return the start of the folders of the tiles of a remote raster in the cache: the name of
     *          the layer and a hash of the key of the raster, as its address.
     */
    private static String remoteCachePrefix( String name, String cacheKey ) {
        String safeName = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return "remoterasters/" + safeName + "-" + Integer.toHexString(cacheKey.hashCode()) + "-";
    }

    /**
     * @return the folder of the tiles of a remote raster in the cache, named after the layer, a
     *          hash of the key of the raster, as its address, and the style.
     */
    private static String remoteCachePath( String name, String cacheKey, StyledRenderer styledRenderer ) {
        return remoteCachePrefix(name, cacheKey) + styledRenderer.styleKey() + "-tiles";
    }

    /**
     * @return the start of the paths in the cache of the tiles folders of the raster, of all its
     *          styles, the same for all the layers of the raster.
     */
    public String getCachePrefix() {
        return cachePrefix;
    }

    /**
     * Delete the tiles of the raster from the cache, of all its styles.
     *
     * @return the number of deleted folders.
     */
    public int clearCache() {
        File cacheRoot = CacheUtils.getCacheRoot();
        int slash = cachePrefix.lastIndexOf('/');
        File folder = new File(cacheRoot, cachePrefix.substring(0, slash));
        String namePrefix = cachePrefix.substring(slash + 1);
        File[] tilesFolders = folder.listFiles(f -> f.isDirectory() && f.getName().startsWith(namePrefix));
        int deleted = 0;
        if (tilesFolders != null) {
            for( File tilesFolder : tilesFolders ) {
                try {
                    FileUtilities.deleteFileOrDir(tilesFolder);
                    deleted++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return deleted;
    }

    /**
     * After a raster layer has been removed from the viewer, delete its tiles from the cache, in
     * the background, if the setting {@link CacheUtils#isClearRasterCacheOnRemove()} is on and
     * no other layer shows the same raster.
     *
     * @param removed the removed layer, any kind of layer: only the raster layers are handled.
     * @param layers the layers still in the viewer.
     */
    public static void clearCacheOfRemoved( Layer removed, Iterable<Layer> layers ) {
        if (!(removed instanceof GridCoverageNwwLayer rasterLayer) || !CacheUtils.isClearRasterCacheOnRemove()) {
            return;
        }
        for( Layer layer : layers ) {
            if (layer != removed && layer instanceof GridCoverageNwwLayer other
                    && other.getCachePrefix().equals(rasterLayer.getCachePrefix())) {
                return;
            }
        }
        new Thread(rasterLayer::clearCache, "hm-raster-cache-clear").start();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (remoteReader != null) {
            remoteReader.dispose();
        }
    }

    private static LevelSet makeLevels( String cacheRelativePath, String urlString, StyledRenderer styledRenderer,
            Integer tileSize, Color colorToMakeTransparent ) throws MalformedURLException {
        GTRenderer renderer = styledRenderer.renderer();
        AVList params = new AVListImpl();
        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }

        int finalTileSize = tileSize;

        params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, finalTileSize);
        params.setValue(AVKey.TILE_HEIGHT, finalTileSize);
        params.setValue(AVKey.DATA_CACHE_NAME, cacheRelativePath);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "*");

        final String imageFormat = "png";
        params.setValue(AVKey.FORMAT_SUFFIX, "." + imageFormat);
        params.setValue(AVKey.NUM_LEVELS, 22);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
        params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
        File cacheRoot = CacheUtils.getCacheRoot();
        final File cacheFolder = new File(cacheRoot, cacheRelativePath);
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }

        params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder(){

            public URL getURL( Tile tile, String altImageFormat ) throws MalformedURLException {
                int zoom = tile.getLevelNumber() + 3;
                Sector sector = tile.getSector();
                double north = sector.getMaxLatitude().degrees;
                double south = sector.getMinLatitude().degrees;
                double east = sector.getMaxLongitude().degrees;
                double west = sector.getMinLongitude().degrees;
                double centerX = west + (east - west) / 2.0;
                double centerY = south + (north - south) / 2.0;
                int[] tileNumber = NwwUtilities.getTileNumber(centerY, centerX, zoom);
                int x = tileNumber[0];
                int y = tileNumber[1];

                Rectangle imageBounds = new Rectangle(0, 0, finalTileSize, finalTileSize);
                // with transparency, the empty parts stay transparent and the opacity of the style is kept
                boolean transparent = colorToMakeTransparent != null;
                BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height,
                        transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
                Graphics2D gr = image.createGraphics();
                if (!transparent) {
                    gr.setPaint(Color.WHITE);
                    gr.fill(imageBounds);
                }
                gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                try {
                    synchronized (renderer) {
                        renderer.paint(gr, imageBounds,
                                new ReferencedEnvelope(west, east, south, north, DefaultGeographicCRS.WGS84));
                        File tileImageFolderFile = new File(cacheFolder, zoom + File.separator + x);
                        if (!tileImageFolderFile.exists()) {
                            tileImageFolderFile.mkdirs();
                        }
                        File imgFile = new File(tileImageFolderFile, y + ".png");
                        if (!imgFile.exists()) {
                            ImageIO.write(image, "png", imgFile);
                        }
                        return imgFile.toURI().toURL();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });

        return new LevelSet(params);
    }

    public String toString() {
        return layerName;
    }

    @Override
    public Coordinate getCenter() {
        return centerCoordinate;
    }

    @Override
    public Envelope getBounds() {
        return boundsLL;
    }

}
