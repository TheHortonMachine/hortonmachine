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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.images.ImageUtilities;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.nww.utils.cache.CacheUtils;

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
public class GridCoverageNwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 256;

    private Coordinate centerCoordinate;

    public GridCoverageNwwLayer( File gridCoverageFile, Integer tileSize, Color colorToMakeTransparent ) throws Exception {
        super(makeLevels(gridCoverageFile, getRenderer(gridCoverageFile), tileSize, colorToMakeTransparent));
        this.layerName = FileUtilities.getNameWithoutExtention(gridCoverageFile);

        // ReferencedEnvelope envelope =
        // OmsVectorReader.readEnvelope(gridCoverageFile.getAbsolutePath());
        // ReferencedEnvelope envelopeLL = envelope.transform(DefaultGeographicCRS.WGS84, true);
        //
        // double w = envelopeLL.getMinX();
        // double s = envelopeLL.getMinY();
        // double e = envelopeLL.getMaxX();
        // double n = envelopeLL.getMaxY();
        //
        // double centerX = w + (e - w) / 2.0;
        // double centerY = s + (n - s) / 2.0;
        //
        // centerCoordinate = new Coordinate(centerX, centerY);

        this.setUseTransparentTextures(true);

    }

    private static GTRenderer getRenderer( File rasterFile ) {

        AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
        AbstractGridCoverage2DReader coverageReader = format.getReader(rasterFile);

        MapContent mapContent = new MapContent();
        try {

            Style rasterStyle = SldUtilities.getStyleFromFile(rasterFile);
            if (rasterStyle == null) {
                RasterSymbolizer sym = SldUtilities.sf.getDefaultRasterSymbolizer();
                rasterStyle = SLD.wrapSymbolizers(sym);
            }

            GridReaderLayer layer = new GridReaderLayer(coverageReader, rasterStyle);
            mapContent.addLayer(layer);
            mapContent.getViewport().setCoordinateReferenceSystem(CrsUtilities.WGS84);
        } catch (Exception e) {
            e.printStackTrace();
        }
        GTRenderer renderer = new StreamingRenderer();
        renderer.setMapContent(mapContent);
        return renderer;
    }

    private static LevelSet makeLevels( File imsf, GTRenderer renderer, Integer tileSize, Color colorToMakeTransparent ) throws MalformedURLException {
        AVList params = new AVListImpl();
        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }

        int finalTileSize = tileSize;

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

                Rectangle imageBounds = new Rectangle(0, 0, finalTileSize, finalTileSize);
                BufferedImage image = new BufferedImage(imageBounds.width, imageBounds.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D gr = image.createGraphics();
                gr.setPaint(Color.WHITE);
                gr.fill(imageBounds);
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
                            if (colorToMakeTransparent != null) {
                                image = ImageUtilities.makeColorTransparent(image, colorToMakeTransparent);
                            }
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
