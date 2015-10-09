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
package org.jgrasstools.gears.modules.r.tmsgenerator;

import static java.lang.Math.round;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class OpenstreetmapImageCreator {

    private static final String EPSG_MERCATOR = "EPSG:3857";
    private static final String EPSG_LATLONG = "EPSG:4326";
    private String inServiceUrl;
    private int zoomLevel;
    private ReferencedEnvelope mercatorBounds;
    private IJGTProgressMonitor pm;
    private File outFile;

    /**
     * @param inServiceUrl The OSM tile service to use (XXX, YYY, ZZZ will be substituted by tile indexes and zoom level).
     * @param zoomLevel The zoom level to use.
     * @param inBounds The area to consider in 3875.
     * @param pm The progress monitor.
     * @return the image.
     * @throws Exception
     */
    public OpenstreetmapImageCreator( String inServiceUrl, int zoomLevel, ReferencedEnvelope mercatorBounds, File outFile,
            IJGTProgressMonitor pm ) {
        this.inServiceUrl = inServiceUrl;
        this.zoomLevel = zoomLevel;
        this.mercatorBounds = mercatorBounds;
        this.outFile = outFile;
        this.pm = pm;
    }

    public void generate() throws Exception {

        CoordinateReferenceSystem mercatorCrs = CRS.decode(EPSG_MERCATOR);
        // CoordinateReferenceSystem latLongCrs = CRS.decode(EPSG_LATLONG);

        double w = mercatorBounds.getMinX();
        double e = mercatorBounds.getMaxX();
        double s = mercatorBounds.getMinY();
        double n = mercatorBounds.getMaxY();

        GlobalMercator mercator = new GlobalMercator();

        int[] llTileNumber = mercator.MetersToTile(w, s, zoomLevel);
        int[] urTileNumber = mercator.MetersToTile(e, n, zoomLevel);

        int startXTile = llTileNumber[0];
        int startYTile = llTileNumber[1];
        int endXTile = urTileNumber[0] + 1;
        int endYTile = urTileNumber[1] + 1;

        double[] extractedLL = mercator.TileBounds(startXTile, startYTile, zoomLevel);
        double[] extractedUR = mercator.TileBounds(endXTile, endYTile, zoomLevel);
        double imageW = extractedLL[0];
        double imageN = extractedUR[3];
        double imageE = extractedUR[2];
        double imageS = extractedLL[1];

        int tileCols = endXTile - startXTile;
        int tileRows = endYTile - startYTile;

        int tileSize = 256;

        int height = tileRows * tileSize;
        int width = tileCols * tileSize;
        BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) outImage.getGraphics();

        pm.beginTask("Generating tiles at zoom level: " + zoomLevel + " with tiles: " + tileCols + "x" + tileRows,
                (endXTile - startXTile + 1));
        int runningX = 0;
        for( int x = startXTile; x <= endXTile; x++ ) {
            int runningY = height - tileSize;
            for( int y = startYTile; y <= endYTile; y++ ) {

                int[] tmsNUms = mercator.TMSTileFromGoogleTile(x, y, zoomLevel);

                String tmp = inServiceUrl.replaceFirst("ZZZ", String.valueOf(zoomLevel));
                tmp = tmp.replaceFirst("XXX", String.valueOf(tmsNUms[0]));
                tmp = tmp.replaceFirst("YYY", String.valueOf(tmsNUms[1]));

                URL url = new URL(tmp);
                try {
                    BufferedImage tileImage = ImageIO.read(url);

                    g2d.drawImage(tileImage, null, runningX, runningY);

                } catch (Exception ex) {
                    pm.errorMessage("Unable to get image: " + tmp);
                }
                runningY -= tileSize;
            }
            runningX += tileSize;
            pm.worked(1);
        }
        pm.done();

        g2d.dispose();

        ImageIO.write(outImage, "tiff", outFile);

        // create tfw
        double dx = (imageE - imageW) / width;
        double dy = (imageN - imageS) / height;
        StringBuilder sb = new StringBuilder();
        sb.append(dx).append("\n");
        sb.append("0.00000000").append("\n");
        sb.append("0.00000000").append("\n");
        sb.append(-dy).append("\n");
        sb.append(imageW).append("\n");
        sb.append(imageN).append("\n");

        String fileName = FileUtilities.getNameWithoutExtention(outFile);

        File folder = outFile.getParentFile();

        File tfwFile = new File(folder, fileName + ".tfw");

        FileUtilities.writeFile(sb.toString(), tfwFile);

        // create prj
        File prjFile = new File(folder, fileName + ".prj");
        FileUtilities.writeFile(mercatorCrs.toWKT(), prjFile);

    }

    public static String getTileNumber( final double lat, final double lon, final int zoom ) {
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor(
                (1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));
        if (xtile < 0)
            xtile = 0;
        if (xtile >= (1 << zoom))
            xtile = ((1 << zoom) - 1);
        if (ytile < 0)
            ytile = 0;
        if (ytile >= (1 << zoom))
            ytile = ((1 << zoom) - 1);
        return ("" + zoom + "/" + xtile + "/" + ytile);
    }

    class BoundingBox {
        double north;
        double south;
        double east;
        double west;
    }
    BoundingBox tile2boundingBox( final int x, final int y, final int zoom ) {
        BoundingBox bb = new BoundingBox();
        bb.north = tile2lat(y, zoom);
        bb.south = tile2lat(y + 1, zoom);
        bb.west = tile2lon(x, zoom);
        bb.east = tile2lon(x + 1, zoom);
        return bb;
    }

    static double tile2lon( int x, int z ) {
        return x / Math.pow(2.0, z) * 360.0 - 180;
    }

    static double tile2lat( int y, int z ) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
    
    public static void main( String[] args ) throws Exception {
        String outFile = "/home/hydrologis/Dropbox/hydrologis/lavori/2015_05_bim_sarcamincio/shape_3857/image_output/osm12.tiff";

        String bounds = "/home/hydrologis/Dropbox/hydrologis/lavori/2015_05_bim_sarcamincio/shape_3857/area.shp";
        String inServiceUrl = "http://a.tile.opencyclemap.org/cycle/ZZZ/XXX/YYY.png";
        // String inServiceUrl = "http://a.tile.openstreetmap.org/ZZZ/XXX/YYY.png";

        ReferencedEnvelope env = OmsVectorReader.readVector(bounds).getBounds();

        OpenstreetmapImageCreator c = new OpenstreetmapImageCreator(inServiceUrl, 12, env, new File(outFile),
                new LogProgressMonitor());
        c.generate();

    }

}
