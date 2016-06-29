/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.nww.layers.defaults.raster;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.jgrasstools.nww.utils.cache.CacheUtils;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.awt.AwtGraphicFactory;
import org.mapsforge.map.layer.renderer.DatabaseRenderer;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;

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
 * Procedural layer for mapsforge files
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MapsforgeNwwLayer extends BasicMercatorTiledImageLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 1024;

    public MapsforgeNwwLayer( File mapsforgeFile, Integer tileSize ) throws Exception {
        super(makeLevels(mapsforgeFile, getTilegenerator(mapsforgeFile, tileSize), tileSize));
        this.layerName = FileUtilities.getNameWithoutExtention(mapsforgeFile);
        this.setUseTransparentTextures(true);

    }

    private static OsmTilegenerator getTilegenerator( File mapsforgeFile, Integer tileSize ) {
        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }
        GraphicFactory graphicFactory = AwtGraphicFactory.INSTANCE;
        MapDatabase mapDatabase = new MapDatabase();
        DatabaseRenderer dbRenderer = null;
        XmlRenderTheme xmlRenderTheme = null;
        DisplayModel displayModel = null;
        if (mapsforgeFile.exists()) {
            mapDatabase.openFile(mapsforgeFile);
            dbRenderer = new DatabaseRenderer(mapDatabase, graphicFactory);

            String mapName = FileUtilities.getNameWithoutExtention(mapsforgeFile);
            File xmlStyleFile = new File(mapsforgeFile.getParentFile(), mapName + ".xml");
            if (xmlStyleFile.exists()) {
                try {
                    xmlRenderTheme = new ExternalRenderTheme(xmlStyleFile);
                } catch (Exception e) {
                    xmlRenderTheme = InternalRenderTheme.OSMARENDER;
                }
            } else {
                xmlRenderTheme = InternalRenderTheme.OSMARENDER;
            }
            displayModel = new DisplayModel();
            displayModel.setUserScaleFactor(tileSize / 256f);
        }

        return new OsmTilegenerator(mapsforgeFile, dbRenderer, xmlRenderTheme, displayModel);
    }

    private static LevelSet makeLevels( File mapsforgeFile, OsmTilegenerator osmTilegenerator, Integer tileSize )
            throws MalformedURLException {
        AVList params = new AVListImpl();
        String cacheRelativePath = "mapsforge/" + mapsforgeFile.getName() + "-tiles";

        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }

        int finalTileSize = tileSize;

        String urlString = mapsforgeFile.toURI().toURL().toExternalForm();
        params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, finalTileSize);
        params.setValue(AVKey.TILE_HEIGHT, finalTileSize);
        params.setValue(AVKey.DATA_CACHE_NAME, cacheRelativePath);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "*");
        params.setValue(AVKey.FORMAT_SUFFIX, ".png");
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

                BufferedImage bImg = osmTilegenerator.getImage(zoom, x, y);
                File tileImageFolderFile = new File(cacheFolder, zoom + File.separator + x);
                if (!tileImageFolderFile.exists()) {
                    tileImageFolderFile.mkdirs();
                }
                File imgFile = new File(tileImageFolderFile, y + ".png");
                try {
                    if (!imgFile.exists()) {
                        ImageIO.write(bImg, "png", imgFile);
                    }
                    return imgFile.toURI().toURL();
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

}
