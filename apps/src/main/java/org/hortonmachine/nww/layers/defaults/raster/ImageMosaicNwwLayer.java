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
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.images.ImageUtilities;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.nww.utils.cache.CacheUtils;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import org.locationtech.jts.geom.Coordinate;

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
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImageMosaicNwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 256;

    private Coordinate centerCoordinate;

    public ImageMosaicNwwLayer( File imageMosaicShpFile, Integer tileSize, GeneralParameterValue[] gp,
            boolean removeSameColorImages ) throws Exception {
        super(makeLevels(imageMosaicShpFile, getRenderer(imageMosaicShpFile, gp), tileSize, removeSameColorImages));
        this.layerName = FileUtilities.getNameWithoutExtention(imageMosaicShpFile);

        ReferencedEnvelope envelope = OmsVectorReader.readEnvelope(imageMosaicShpFile.getAbsolutePath());
        ReferencedEnvelope envelopeLL = envelope.transform(DefaultGeographicCRS.WGS84, true);

        double w = envelopeLL.getMinX();
        double s = envelopeLL.getMinY();
        double e = envelopeLL.getMaxX();
        double n = envelopeLL.getMaxY();

        double centerX = w + (e - w) / 2.0;
        double centerY = s + (n - s) / 2.0;

        centerCoordinate = new Coordinate(centerX, centerY);

        this.setUseTransparentTextures(true);

    }

    private static GTRenderer getRenderer( File imsf, GeneralParameterValue[] gp ) {

        GTRenderer renderer = null;
        try {
            ImageMosaicReader coverageTilesReader = new ImageMosaicReader(imsf);

            MapContent mapContent = new MapContent();
            RasterSymbolizer sym = SldUtilities.sf.getDefaultRasterSymbolizer();
            Style style = SLD.wrapSymbolizers(sym);

            GridReaderLayer layer;
            if (gp == null) {
                layer = new GridReaderLayer(coverageTilesReader, style);
            } else {
                layer = new GridReaderLayer(coverageTilesReader, style, gp);
            }
            mapContent.addLayer(layer);
            mapContent.getViewport().setCoordinateReferenceSystem(CrsUtilities.WGS84);
            renderer = new StreamingRenderer();
            renderer.setMapContent(mapContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return renderer;
    }

    private static LevelSet makeLevels( File imsf, GTRenderer renderer, Integer tileSize, boolean removeSameColorImages )
            throws MalformedURLException {
        AVList params = new AVListImpl();
        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }

        int finalTileSize = tileSize;
        Rectangle imageBounds = new Rectangle(0, 0, finalTileSize, finalTileSize);
        BufferedImage transparentImage = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = transparentImage.createGraphics();
        gr.setPaint(ColorUtilities.makeTransparent(Color.WHITE, 0));
        gr.fill(imageBounds);
        gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gr.dispose();

        String tilesPart = "-tiles";
        String cacheRelativePath = "imagemosaics/" + imsf.getName() + tilesPart;

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

                File tileImageFolderFile = new File(cacheFolder, zoom + File.separator + x);
                if (!tileImageFolderFile.exists()) {
                    tileImageFolderFile.mkdirs();
                }
                File imgFile = new File(tileImageFolderFile, y + ".png");
                if (imgFile.exists()) {
                    return imgFile.toURI().toURL();
                }

                Rectangle imageBounds = new Rectangle(0, 0, finalTileSize, finalTileSize);
                BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gr = image.createGraphics();
                gr.setPaint(ColorUtilities.makeTransparent(Color.WHITE, 0));
                gr.fill(imageBounds);
                gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                try {
                    synchronized (renderer) {
                        ReferencedEnvelope referencedEnvelope = new ReferencedEnvelope(west, east, south, north,
                                DefaultGeographicCRS.WGS84);
                        renderer.paint(gr, imageBounds, referencedEnvelope);
                        gr.dispose();
                        if (removeSameColorImages && ImageUtilities.isAllOneColor(image)) {
                            image = transparentImage;
                        }
                        // image = ImageUtilities.makeColorTransparent(image, Color.white);
                        // File tileImageFolderFile = new File(cacheFolder, zoom + File.separator +
                        // x);
                        // if (!tileImageFolderFile.exists()) {
                        // tileImageFolderFile.mkdirs();
                        // }
                        // File imgFile = new File(tileImageFolderFile, y + ".png");
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

}
