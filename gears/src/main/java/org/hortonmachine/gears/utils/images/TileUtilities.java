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
package org.hortonmachine.gears.utils.images;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.utils.MercatorUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * An utility class for tile handling. 
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.9.7
 */
public class TileUtilities {
    public static final Color transparent = new Color(255, 255, 255, 0);

    /**
     * Produce a geopackage tile for a different zoomlevel applying patching/clipping and scaling.
     * 
     * <p>This can be used to get a tile for a zoomlevel, in which there are no data.
     * 
     * @param gpkgDb the database.
     * @param tableName the name of the table to extract from.
     * @param x the original tile x.
     * @param y the original tile y.
     * @param zoom the original tile zoom level.
     * @param tileSize the tile size.
     * @param otherZoomLevel the zoomlevel to extract the new data for.
     * @return the tile image or null if none is available..
     * @throws Exception
     */
    public static BufferedImage getTileFromDifferentZoomlevel( GeopackageCommonDb gpkgDb, String tableName, int x, int y, int zoom,
            int tileSize, int otherZoomLevel ) throws Exception {

        if (otherZoomLevel > zoom) {
            // for higher zoomlevel, we need to retrieve the tiles and patch them together
            List<int[]> tilesAtHigherZoom = MercatorUtils.getTilesAtHigherZoom(x, y, zoom, otherZoomLevel, tileSize);
            int delta = otherZoomLevel - zoom;
            int splits = (int) Math.pow(2, delta);
            int size = splits * tileSize;
            BufferedImage finalImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = (Graphics2D) finalImage.getGraphics();
//            g2d.setColor(transparent);
//            g2d.fillRect(0, 0, size, size);
            int runningX = 0;
            int runningY = 0;
            boolean hasOne = false;
            for( int[] zxy2 : tilesAtHigherZoom ) {
                byte[] tile2 = gpkgDb.getTile(tableName, zxy2[1], zxy2[2], zxy2[0]);
                if (tile2 != null) {
                    hasOne = true;
                    ByteArrayInputStream bais = new ByteArrayInputStream(tile2);
                    BufferedImage img = ImageIO.read(bais);
                    int imageX = runningX * tileSize;
                    int imageY = runningY * tileSize;
                    g2d.drawImage(img, imageX, imageY, null);
                }
                runningX++;
                if (runningX == splits) {
                    runningX = 0;
                    runningY++;
                }
            }

            if (hasOne) {
                // if we arrive here, the image was fully covered
//            int scaledSize = (int) Math.round(tileSize / (double) splits);
//            Image scaledInstance = img.getScaledInstance(scaledSize, scaledSize, Image.SCALE_FAST);
                return ImageUtilities.scaleImage(finalImage, tileSize);
            }
        } else {
            // for lower zoomlevels the piece needs to be extracted and resized to fit the tile
            Envelope swEnv = MercatorUtils.tileBounds4326(x, y, zoom);
            Coordinate centre = swEnv.centre();
            double swx2 = swEnv.getMaxX();
            double swx1 = swEnv.getMinX();
            double swy2 = swEnv.getMaxY();
            double swy1 = swEnv.getMinY();

            int[] zxy3 = MercatorUtils.getTileNumber(centre.y, centre.x, otherZoomLevel);
            byte[] tileBytes = gpkgDb.getTile(tableName, zxy3[1], zxy3[2], zxy3[0]);
            if (tileBytes != null) {
                ByteArrayInputStream bais = new ByteArrayInputStream(tileBytes);
                BufferedImage img = ImageIO.read(bais);
                Envelope wEnv = MercatorUtils.tileBounds4326(zxy3[1], zxy3[2], zxy3[0]);

                double wx2 = wEnv.getMaxX();
                double wx1 = wEnv.getMinX();
                double wy2 = wEnv.getMaxY();
                double wy1 = wEnv.getMinY();

                double dx = (wx2 - wx1) / tileSize;
                int px1 = (int) Math.round((swx1 - wx1) / dx);
                int px2 = (int) Math.round((swx2 - wx1) / dx);

                double dy = (wy2 - wy1) / (-tileSize);
                int py1 = (int) Math.round((swy2 - wy1) / dy + tileSize);
                int py2 = (int) Math.round((swy1 - wy1) / dy + tileSize);

                BufferedImage finalImage = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = (Graphics2D) finalImage.getGraphics();
                g2d.drawImage(img, 0, 0, tileSize, tileSize, px1, py1, px2, py2, null);
                g2d.dispose();

                return finalImage;
            }

        }
        return null;
    }

}
