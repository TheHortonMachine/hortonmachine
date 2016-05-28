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
package org.jgrasstools.nww.layers.defaults;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import com.vividsolutions.jts.geom.Coordinate;

import org.jgrasstools.gears.modules.r.tmsgenerator.MBTilesHelper;
import org.jgrasstools.gears.utils.files.FileUtilities;

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

    private static final int TILESIZE = 512;

    private File mbtilesFile;

    private Coordinate centerCoordinate;

    public MBTilesNwwLayer(File mbtilesFile) throws Exception {
        super(makeLevels(mbtilesFile, getTilegenerator(mbtilesFile)));
        this.mbtilesFile = mbtilesFile;
        this.layerName = FileUtilities.getNameWithoutExtention(mbtilesFile);
        this.setUseTransparentTextures(true);

    }

    private static MBTilesHelper getTilegenerator(File mbtilesFile) {
        MBTilesHelper mbTilesHelper = new MBTilesHelper();
        try {
            mbTilesHelper.open(mbtilesFile);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return mbTilesHelper;
    }

    private static LevelSet makeLevels(File mbtilesFile, MBTilesHelper mbtilesHelper) throws MalformedURLException {
        AVList params = new AVListImpl();

        String urlString = mbtilesFile.toURI().toURL().toExternalForm();
        params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, TILESIZE);
        params.setValue(AVKey.TILE_HEIGHT, TILESIZE);
        params.setValue(AVKey.DATA_CACHE_NAME, "huberg/" + mbtilesFile.getName() + "-tiles");
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
        final File cacheFolder = new File(mbtilesFile.getAbsolutePath() + "-tiles");
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }
        params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder() {

            public URL getURL(Tile tile, String altImageFormat) throws MalformedURLException {
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

    public static int[] getTileNumber(final double lat, final double lon, final int zoom) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile =
            (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI)
                / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        return new int[] { xtile, ytile };
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
