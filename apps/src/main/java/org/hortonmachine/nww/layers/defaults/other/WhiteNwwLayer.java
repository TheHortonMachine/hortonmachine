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
package org.hortonmachine.nww.layers.defaults.other;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.hortonmachine.nww.layers.defaults.raster.BasicMercatorTiledImageLayer;
import org.hortonmachine.nww.utils.cache.CacheUtils;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.mercator.MercatorSector;
import gov.nasa.worldwind.util.LevelSet;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;

/**
 * Procedural layer for mbtiles databases
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WhiteNwwLayer extends BasicMercatorTiledImageLayer {

    private static final String WHITE_BACKGROUND = "white_background";

    private String layerName = "unknown layer";

    private static final int TILESIZE = 512;
    private static BufferedImage img = null;

    public WhiteNwwLayer() {
        super(makeLevels());
        this.layerName = "hide_white_backgroundlayer";
    }

    private static LevelSet makeLevels() {
        AVList params = new AVListImpl();

        // String urlString = "";
        // params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, TILESIZE);
        params.setValue(AVKey.TILE_HEIGHT, TILESIZE);
        params.setValue(AVKey.DATA_CACHE_NAME, WHITE_BACKGROUND);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "*");

        final String imageFormat = "png";
        params.setValue(AVKey.FORMAT_SUFFIX, "." + imageFormat);
        params.setValue(AVKey.NUM_LEVELS, 22);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
        params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
        params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder(){

            public URL getURL( Tile tile, String altImageFormat ) throws MalformedURLException {
                int zoom = tile.getLevelNumber() + 3;
                int x = tile.getColumn();
                int y = tile.getRow();

                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(zoom);
                    sb.append(File.separator);
                    sb.append(x);
                    File cacheRoot = CacheUtils.getCacheRoot();
                    File cacheFolderFile = new File(cacheRoot, WHITE_BACKGROUND);
                    File tileImageFolderFile = new File(cacheFolderFile, sb.toString());
                    if (!tileImageFolderFile.exists()) {
                        tileImageFolderFile.mkdirs();
                    }

                    sb = new StringBuilder();
                    sb.append(y);
                    sb.append(".");
                    sb.append(imageFormat);
                    File imgFile = new File(tileImageFolderFile, sb.toString());
                    if (!imgFile.exists()) {
                        if (img == null) {
                            img = new BufferedImage(TILESIZE, TILESIZE, BufferedImage.TYPE_INT_RGB);
                            Graphics2D g2d = img.createGraphics();
                            g2d.setColor(Color.WHITE);
                            g2d.fillRect(0, 0, TILESIZE, TILESIZE);
                            g2d.dispose();
                        }
                        ImageIO.write(img, imageFormat, imgFile);
                    }
                    return imgFile.toURI().toURL();
                } catch (Exception e) {
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
