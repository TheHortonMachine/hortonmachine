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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import org.hortonmachine.gears.modules.r.tmsgenerator.MBTilesHelper;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.utils.cache.CacheUtils;

import org.locationtech.jts.geom.Coordinate;

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
public class MBTilesNwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 256;

    protected static final boolean DEBUG = true;
    protected static final boolean DEBUG_ALSO_WITHOUT_IMAGE = false;

    private File mbtilesFile;

    private Coordinate centerCoordinate;

    public MBTilesNwwLayer( File mbtilesFile ) throws Exception {
        super(makeLevels(mbtilesFile, getTilegenerator(mbtilesFile)));
        this.mbtilesFile = mbtilesFile;
        this.layerName = FileUtilities.getNameWithoutExtention(mbtilesFile);
        this.setUseTransparentTextures(true);

    }

    private static MBTilesHelper getTilegenerator( File mbtilesFile ) {
        MBTilesHelper mbTilesHelper = new MBTilesHelper();
        try {
            mbTilesHelper.open(mbtilesFile);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return mbTilesHelper;
    }

    private static LevelSet makeLevels( File mbtilesFile, MBTilesHelper mbtilesHelper ) throws MalformedURLException {
        AVList params = new AVListImpl();
        String cacheRelativePath = "mbtiles/" + mbtilesFile.getName() + "-tiles";

        String urlString = mbtilesFile.toURI().toURL().toExternalForm();
        params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, TILESIZE);
        params.setValue(AVKey.TILE_HEIGHT, TILESIZE);
        params.setValue(AVKey.DATA_CACHE_NAME, cacheRelativePath);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "*");

        String imageFormat = null;
        try {
            imageFormat = mbtilesHelper.getImageFormat();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        if (imageFormat == null) {
            imageFormat = "png";
        }
        final String _imageFormat = imageFormat;
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
                int x = tile.getColumn();
                int y = tile.getRow();

                try {
                    StringBuilder sb = new StringBuilder();
                    sb.append(zoom);
                    sb.append(File.separator);
                    sb.append(x);
                    File tileImageFolderFile = new File(cacheFolder, sb.toString());
                    if (!tileImageFolderFile.exists()) {
                        tileImageFolderFile.mkdirs();
                    }

                    sb = new StringBuilder();
                    sb.append(y);
                    sb.append(".");
                    sb.append(_imageFormat);
                    File imgFile = new File(tileImageFolderFile, sb.toString());
                    if (!imgFile.exists()) {
                        BufferedImage bImg = mbtilesHelper.getTile(x, y, zoom);
                        if (DEBUG) {
                            if (bImg == null && DEBUG_ALSO_WITHOUT_IMAGE) {
                                bImg = new BufferedImage(TILESIZE, TILESIZE, BufferedImage.TYPE_INT_ARGB);
                            }
                            if (bImg != null) {
                                Graphics2D g2d = (Graphics2D) bImg.getGraphics();
                                g2d.setColor(Color.black);
                                g2d.drawRect(0, 0, TILESIZE, TILESIZE);
                                g2d.drawString(zoom + "/" + x + "/" + y, 3, TILESIZE / 2);
                                g2d.dispose();
                            }
                        }
                        if (bImg != null) {
                            ImageIO.write(bImg, _imageFormat, imgFile);
                        } else {
                            return null;
                        }

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

    @Override
    public Coordinate getCenter() {
        if (centerCoordinate == null) {
            try (MBTilesHelper mbTilesHelper = new MBTilesHelper()) {
                mbTilesHelper.open(mbtilesFile);
                double[] wsen = mbTilesHelper.getBounds();

                double centerX = wsen[0] + (wsen[2] - wsen[0]) / 2.0;
                double centerY = wsen[1] + (wsen[3] - wsen[1]) / 2.0;
                centerCoordinate = new Coordinate(centerX, centerY);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return centerCoordinate;
    }

}
