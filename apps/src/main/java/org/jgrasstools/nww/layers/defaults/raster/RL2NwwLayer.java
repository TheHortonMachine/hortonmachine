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
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.spatialite.RL2CoverageHandler;
import org.jgrasstools.gears.spatialite.RasterCoverage;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.nww.layers.defaults.NwwLayer;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.jgrasstools.nww.utils.cache.CacheUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

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
 * Procedural layer for rasterlite2 files
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RL2NwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 512;

    private Coordinate centerCoordinateLL;

    public RL2NwwLayer( RL2CoverageHandler rl2Handler, Integer tileSize ) throws Exception {
        super(makeLevels(rl2Handler, tileSize));
        RasterCoverage rasterCoverage = rl2Handler.getRasterCoverage();
        this.layerName = rasterCoverage.coverage_name;

        double w = rasterCoverage.extent_minx;
        double s = rasterCoverage.extent_miny;
        double e = rasterCoverage.extent_maxx;
        double n = rasterCoverage.extent_maxy;

        double centerX = w + (e - w) / 2.0;
        double centerY = s + (n - s) / 2.0;
        Coordinate centerCoordinate = new Coordinate(centerX, centerY);

        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
        CoordinateReferenceSystem sourceCRS = CrsUtilities.getCrsFromSrid(rasterCoverage.srid);

        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        centerCoordinateLL = JTS.transform(centerCoordinate, null, transform);

        this.setUseTransparentTextures(true);

    }

    private static LevelSet makeLevels( RL2CoverageHandler rl2Handler, Integer tileSize ) throws MalformedURLException {
        AVList params = new AVListImpl();
        if (tileSize == null || tileSize < 256) {
            tileSize = TILESIZE;
        }

        int finalTileSize = tileSize;

        String databasePath = rl2Handler.getDatabasePath();
        File databaseFile = new File(databasePath);
        String tilesPart = "-tiles" + File.separator + rl2Handler.getRasterCoverage().coverage_name;
        String cacheRelativePath = "rl2/" + databaseFile.getName() + tilesPart;

        String urlString = databaseFile.toURI().toURL().toExternalForm();
        params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, finalTileSize);
        params.setValue(AVKey.TILE_HEIGHT, finalTileSize);
        params.setValue(AVKey.DATA_CACHE_NAME, cacheRelativePath);
        params.setValue(AVKey.SERVICE, "*");
        params.setValue(AVKey.DATASET_NAME, "*");

        String imageFormat = null;
        try {
            String compression = rl2Handler.getRasterCoverage().compression;
            if (compression.equals("JPEG")) {
                imageFormat = "jpg";
            }
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
                Sector sector = tile.getSector();
                double n = sector.getMaxLatitude().degrees;
                double s = sector.getMinLatitude().degrees;
                double e = sector.getMaxLongitude().degrees;
                double w = sector.getMinLongitude().degrees;
                double centerX = w + (e - w) / 2.0;
                double centerY = s + (n - s) / 2.0;

                int[] tileNumber = NwwUtilities.getTileNumber(centerY, centerX, zoom);
                int x = tileNumber[0];
                int y = tileNumber[1];

                // int zoom = tile.getLevelNumber() + 3;
                // int x = tile.getColumn();
                // int y = (1 << (tile.getLevelNumber()) + 3) - 1 - tile.getRow();
                //
                // double n = MBTilesHelper.tile2lat(y, zoom);
                // double s = MBTilesHelper.tile2lat(y + 1, zoom);
                // double w = MBTilesHelper.tile2lon(x, zoom);
                // double e = MBTilesHelper.tile2lon(x + 1, zoom);

                Coordinate ll = new Coordinate(w, s);
                Coordinate ur = new Coordinate(e, n);

                Polygon polygon = GeometryUtilities.createPolygonFromEnvelope(new com.vividsolutions.jts.geom.Envelope(ll, ur));

                try {
                    File imgFile;
                    synchronized (rl2Handler) {
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
                        imgFile = new File(tileImageFolderFile, sb.toString());
                        if (!imgFile.exists()) {

                            BufferedImage bImg = rl2Handler.getRL2Image(polygon, "" + CrsUtilities.WGS84_SRID, finalTileSize,
                                    finalTileSize);
                            if (bImg != null) {
                                ImageIO.write(bImg, _imageFormat, imgFile);
                            } else {
                                return null;
                            }

                        }
                    }
                    return imgFile.toURI().toURL();
                } catch (Exception ez) {
                    ez.printStackTrace();
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
        return centerCoordinateLL;
    }

}
