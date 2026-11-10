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
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Style;
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
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

/**
 * Procedural layer for geotools imagemosaic files
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GridCoverageNwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 256;

    private Coordinate centerCoordinate;
    private Envelope boundsLL;

    /** The style the tiles are drawn with. */
    private final Style style;

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
        this(gridCoverageFile, tileSize, colorToMakeTransparent, getRenderer(gridCoverageFile, style));
    }

    private GridCoverageNwwLayer( File gridCoverageFile, Integer tileSize, Color colorToMakeTransparent,
            StyledRenderer styledRenderer ) throws Exception {
        super(makeLevels(gridCoverageFile, styledRenderer, tileSize, colorToMakeTransparent));
        this.style = styledRenderer.style();
        this.layerName = FileUtilities.getNameWithoutExtention(gridCoverageFile);

        ReferencedEnvelope envelope = OmsRasterReader.readEnvelope(gridCoverageFile.getAbsolutePath());
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

    private static StyledRenderer getRenderer( File rasterFile, Style givenStyle ) {

        AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
        AbstractGridCoverage2DReader coverageReader = format.getReader(rasterFile);

        MapContent mapContent = new MapContent();
        String styleKey = "default";
        Style rasterStyle = givenStyle;
        try {
            if (rasterStyle != null) {
                // the same style gets the same tiles
                styleKey = "style" + Integer.toHexString(SldUtilities.styleToString(rasterStyle).hashCode());
            } else {
                rasterStyle = SldUtilities.getStyleFromFile(rasterFile);
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
     * The style a raster without .sld file is drawn with: for single band rasters of non byte
     * data, as for example DEMs, an elevation colortable on the value range of the whole raster,
     * else the default raster style, which shows well RGB and palette images.
     *
     * @param rasterFile the raster file.
     * @return the style.
     */
    public static Style createDefaultStyle( File rasterFile ) {
        AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
        AbstractGridCoverage2DReader coverageReader = format.getReader(rasterFile);
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
     * @return <code>true</code> if the raster has a single band of values that a colortable can
     *          show, <code>false</code> for RGB, byte and palette images, which have their own
     *          colors.
     */
    public static boolean isColortableRaster( File rasterFile ) {
        AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
        AbstractGridCoverage2DReader coverageReader = format.getReader(rasterFile);
        try {
            return isColortableImage(coverageReader);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            coverageReader.dispose();
        }
    }

    private static boolean isColortableImage( AbstractGridCoverage2DReader coverageReader ) throws Exception {
        // the image is read lazily, only its structure is used here
        RenderedImage image = coverageReader.read((GeneralParameterValue[]) null).getRenderedImage();
        SampleModel sampleModel = image.getSampleModel();
        return sampleModel.getNumBands() == 1 && sampleModel.getDataType() != DataBuffer.TYPE_BYTE
                && !(image.getColorModel() instanceof IndexColorModel);
    }

    /**
     * Make an elevation colortable style on the value range of the whole raster, for single band rasters
     * of non byte data, as for example DEMs. With the default style, each tile would be
     * stretched on its own values range.
     *
     * @return the style or <code>null</code> if the raster is shown well by the default style,
     *          as RGB or palette images.
     */
    private static Style makeFullRangeStyle( AbstractGridCoverage2DReader coverageReader, File rasterFile ) {
        try {
            if (!isColortableImage(coverageReader)) {
                return null;
            }
            double[] stats;
            try (HMRaster raster = HMRaster.fromFile(rasterFile)) {
                stats = raster.getStatistics();
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
            return style;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static LevelSet makeLevels( File imsf, StyledRenderer styledRenderer, Integer tileSize, Color colorToMakeTransparent ) throws MalformedURLException {
        GTRenderer renderer = styledRenderer.renderer();
        AVList params = new AVListImpl();
        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }

        int finalTileSize = tileSize;

        String tilesPart = "-" + styledRenderer.styleKey() + "-tiles";
        // a changed raster or style gets new tiles
        long stamp = imsf.lastModified();
        File styleFile = SldUtilities.getStyleFile(imsf);
        if ("sld".equals(styledRenderer.styleKey()) && styleFile != null && styleFile.exists()) {
            stamp = Math.max(stamp, styleFile.lastModified());
        }
        String cacheRelativePath = "imagemosaics/" + imsf.getName() + "-" + stamp + tilesPart;

        String urlString = imsf.toURI().toURL().toExternalForm();
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
