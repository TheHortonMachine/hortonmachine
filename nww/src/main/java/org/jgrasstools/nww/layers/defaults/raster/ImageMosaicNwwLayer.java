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

import com.vividsolutions.jts.geom.Coordinate;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.modules.r.tmsgenerator.MBTilesHelper;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.images.ImageUtilities;
import org.jgrasstools.nww.layers.defaults.NwwLayer;
import org.jgrasstools.nww.utils.cache.CacheUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

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
 * Procedural layer for geotools imagemosaic files
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImageMosaicNwwLayer extends BasicMercatorTiledImageLayer implements NwwLayer {

    private String layerName = "unknown layer";

    private static final int TILESIZE = 512;

    private Coordinate centerCoordinate;

    private static CoordinateReferenceSystem osmCrs;

    public ImageMosaicNwwLayer(File imageMosaicShpFile) throws Exception {
        super(makeLevels(imageMosaicShpFile));
        this.layerName = FileUtilities.getNameWithoutExtention(imageMosaicShpFile);

        ReferencedEnvelope envelope = OmsVectorReader.readEnvelope(imageMosaicShpFile.getAbsolutePath());
        ReferencedEnvelope envelopeLL = envelope.transform(DefaultGeographicCRS.WGS84, true);

        osmCrs = CRS.decode("EPSG:3857");

        double w = envelopeLL.getMinX();
        double s = envelopeLL.getMinY();
        double e = envelopeLL.getMaxX();
        double n = envelopeLL.getMaxY();

        double centerX = w + (e - w) / 2.0;
        double centerY = s + (n - s) / 2.0;

        centerCoordinate = new Coordinate(centerX, centerY);

        this.setUseTransparentTextures(true);

    }

    private static LevelSet makeLevels(File imsf) throws MalformedURLException {
        AVList params = new AVListImpl();
        AbstractGridFormat format = GridFormatFinder.findFormat(imsf);
        AbstractGridCoverage2DReader coverageTilesReader = format.getReader(imsf);

        String tilesPart = "-tiles";
        String cacheRelativePath = "imagemosaics/" + imsf.getName() + tilesPart;

        String urlString = imsf.toURI().toURL().toExternalForm();
        params.setValue(AVKey.URL, urlString);
        params.setValue(AVKey.TILE_WIDTH, TILESIZE);
        params.setValue(AVKey.TILE_HEIGHT, TILESIZE);
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
        params.setValue(AVKey.TILE_URL_BUILDER, new TileUrlBuilder() {

            public URL getURL(Tile tile, String altImageFormat) throws MalformedURLException {
                int zoom = tile.getLevelNumber() + 3;
                int x = tile.getColumn();
                int y = (1 << (tile.getLevelNumber()) + 3) - 1 - tile.getRow();

                double n = MBTilesHelper.tile2lat(y, zoom);
                double s = MBTilesHelper.tile2lat(y + 1, zoom);
                double w = MBTilesHelper.tile2lon(x, zoom);
                double e = MBTilesHelper.tile2lon(x + 1, zoom);

                Coordinate ll = new Coordinate(w, s);
                Coordinate ur = new Coordinate(e, n);
                try {
                    CoordinateReferenceSystem sourceCRS = DefaultGeographicCRS.WGS84;
                    MathTransform transform = CRS.findMathTransform(sourceCRS, osmCrs);
                    ll = JTS.transform(ll, null, transform);
                    ur = JTS.transform(ur, null, transform);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

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
                    sb.append(imageFormat);
                    File imgFile = new File(tileImageFolderFile, sb.toString());
                    if (!imgFile.exists()) {
                        BufferedImage bImg = ImageUtilities.imageFromReader(coverageTilesReader, TILESIZE, TILESIZE,
                            ll.x, ur.x, ll.y, ur.y, osmCrs);
                        if (bImg != null) {
                            ImageIO.write(bImg, imageFormat, imgFile);
                        } else {
                            return null;
                        }

                    }
                    return imgFile.toURI().toURL();
                } catch (Exception ex) {
                    ex.printStackTrace();
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
