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
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.imageio.ImageIO;

import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.TileEntry;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.utils.MercatorUtils;
import org.hortonmachine.gears.utils.images.TileUtilities;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
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
 * Procedural layer for geopackage databases
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopackageTilesNwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 256;

    protected static final boolean DEBUG = true;
    protected static final boolean DEBUG_ALSO_WITHOUT_IMAGE = true;

    private Coordinate centerCoordinate;

    public GeopackageTilesNwwLayer( File gpkgFile, String tableName ) throws Exception {
        super(makeLevels(gpkgFile, getTilegenerator(gpkgFile), tableName));
        this.layerName = tableName;
        this.setUseTransparentTextures(true);

        try (GeopackageCommonDb db = new GeopackageDb()) {
            db.open(gpkgFile.getAbsolutePath());

            TileEntry tile = db.tile(tableName);
            Envelope bounds = tile.getBounds();
            Coordinate centre3857 = bounds.centre();
            Coordinate ll = MercatorUtils.convert3857To4326(centre3857);

            centerCoordinate = ll;
        }
    }

    private static GeopackageCommonDb getTilegenerator( File gpkgFile ) {
        GeopackageCommonDb gpkgDb = new GeopackageDb();
        try {
            gpkgDb.open(gpkgFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return gpkgDb;
    }

    private static LevelSet makeLevels( File gpkgFile, GeopackageCommonDb gpkgDb, String tableName ) throws Exception {
        AVList params = new AVListImpl();

        String cacheRelativePath = "gpkg/" + gpkgFile.getName() + "-" + tableName + "-tiles";

        TileEntry tile = gpkgDb.tile(tableName);
        int tileSize = tile.getTileMatricies().get(0).getTileWidth();

        String urlString = gpkgFile.toURI().toURL().toExternalForm();
        params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, tileSize);
        params.setValue(AVKey.TILE_HEIGHT, tileSize);
        params.setValue(AVKey.DATA_CACHE_NAME, cacheRelativePath);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "*");

        final String _imageFormat = "png";
        params.setValue(AVKey.FORMAT_SUFFIX, "." + _imageFormat);
        params.setValue(AVKey.NUM_LEVELS, 22);
        params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
        params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(Angle.fromDegrees(22.5d), Angle.fromDegrees(45d)));
        params.setValue(AVKey.SECTOR, new MercatorSector(-1.0, 1.0, Angle.NEG180, Angle.POS180));
        File cacheRoot = CacheUtils.getCacheRoot();
        final File cacheFolder = new File(cacheRoot, cacheRelativePath);
        if (!cacheFolder.exists()) {
            cacheFolder.mkdirs();
        }

        List<Integer> tileZoomLevelsWithData = gpkgDb.getTileZoomLevelsWithData(tableName);

        params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder(){

            public URL getURL( Tile tile, String altImageFormat ) throws MalformedURLException {
                int zoom = tile.getLevelNumber() + 3;
//                int x = tile.getColumn();
//                int y = tile.getRow();

                Sector sector = tile.getSector();
                double north = sector.getMaxLatitude().degrees;
                double south = sector.getMinLatitude().degrees;
                double east = sector.getMaxLongitude().degrees;
                double west = sector.getMinLongitude().degrees;
                double centerX = west + (east - west) / 2.0;
                double centerY = south + (north - south) / 2.0;

                int[] zxy = MercatorUtils.getTileNumber(centerY, centerX, zoom);
                int x = zxy[1];
                int y = zxy[2];
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
                        BufferedImage bImg = null;
                        byte[] bytes = gpkgDb.getTile(tableName, x, y, zoom);
                        if (bytes != null) {
                            ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                            bImg = ImageIO.read(bais);
                        } else {
                            // try downscaling from higher resolutions
                            // look in the other levels
                            int from = zoom + 1;
                            int to = Math.min(zoom + 5, 19);
                            for( int higherZoom = from; higherZoom < to; higherZoom++ ) {
                                if (!tileZoomLevelsWithData.contains(higherZoom)) {
                                    continue;
                                }

                                BufferedImage img = TileUtilities.getTileFromDifferentZoomlevel(gpkgDb, tableName, x, y, zoom,
                                        tileSize, higherZoom);
                                if (img != null) {
                                    bImg = img;
                                    break;
                                }
                            }

                            if (bImg == null) {
                                // then try upscaling from lower resolution
                                from = zoom - 1;
                                int zoomLevelTotry = 5;
                                to = Math.max(zoom - zoomLevelTotry, 1);
                                for( int lowerZoom = from; lowerZoom > to; lowerZoom-- ) {
                                    if (!tileZoomLevelsWithData.contains(lowerZoom)) {
                                        continue;
                                    }
                                    BufferedImage img = TileUtilities.getTileFromDifferentZoomlevel(gpkgDb, tableName, x, y, zoom,
                                            tileSize, lowerZoom);
                                    if (img != null) {
                                        bImg = img;
                                        break;
                                    }
                                }
                            }

                        }
                        if (DEBUG) {
                            if (bImg == null && DEBUG_ALSO_WITHOUT_IMAGE) {
                                bImg = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
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
        return centerCoordinate;
    }

}
