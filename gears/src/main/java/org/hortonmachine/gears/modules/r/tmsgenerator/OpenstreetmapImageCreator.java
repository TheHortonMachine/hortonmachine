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
package org.hortonmachine.gears.modules.r.tmsgenerator;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Coordinate;

public class OpenstreetmapImageCreator {

    private static final String EPSG_MERCATOR = "EPSG:3857";
    private String inServiceUrl;
    private int zoomLevel;
    private ReferencedEnvelope mercatorBounds;
    private IHMProgressMonitor pm;
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
            IHMProgressMonitor pm ) {
        this.inServiceUrl = inServiceUrl;
        this.zoomLevel = zoomLevel;
        this.mercatorBounds = mercatorBounds;
        this.outFile = outFile;
        this.pm = pm;
    }

    public void generate() throws Exception {

        CoordinateReferenceSystem mercatorCrs = CrsUtilities.getCrsFromEpsg(EPSG_MERCATOR, null);
        CoordinateReferenceSystem latLongCrs = DefaultGeographicCRS.WGS84;

        ReferencedEnvelope llBounds = mercatorBounds.transform(latLongCrs, true);

        double w = llBounds.getMinX();
        double e = llBounds.getMaxX();
        double s = llBounds.getMinY();
        double n = llBounds.getMaxY();

        int[] ulTileNumber = getTileXY(n, w, zoomLevel);
        int[] lrTileNumber = getTileXY(s, e, zoomLevel);

        int startXTile = ulTileNumber[0];
        int startYTile = ulTileNumber[1];

        int endXTile = lrTileNumber[0];
        int endYTile = lrTileNumber[1];

        BoundingBox bbUL = tile2boundingBox(startXTile, startYTile, zoomLevel);
        double imageW = bbUL.west;
        double imageN = bbUL.north;
        BoundingBox bbLR = tile2boundingBox(endXTile, endYTile, zoomLevel);
        double imageS = bbLR.south;
        double imageE = bbLR.east;

        int tileCols = endXTile - startXTile + 1;
        int tileRows = endYTile - startYTile + 1;

        int tileSize = 256;

        int height = tileRows * tileSize;
        int width = tileCols * tileSize;
        BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) outImage.getGraphics();

        pm.beginTask("Generating tiles at zoom level: " + zoomLevel + " with tiles: " + tileCols + "x" + tileRows,
                (endXTile - startXTile + 1));
        int runningX = 0;
        for( int x = startXTile; x <= endXTile; x++ ) {
            int runningY = 0;
            for( int y = startYTile; y <= endYTile; y++ ) {

                // int[] tmsNUms = getTileXY(y, x, zoomLevel);

                String tmp = inServiceUrl.replaceFirst("ZZZ", String.valueOf(zoomLevel));
                tmp = tmp.replaceFirst("XXX", String.valueOf(x));
                tmp = tmp.replaceFirst("YYY", String.valueOf(y));

                URL url = new URL(tmp);
                try {
                    BufferedImage tileImage = ImageIO.read(url);

                    g2d.drawImage(tileImage, null, runningX, runningY);

                } catch (Exception ex) {
                    pm.errorMessage("Unable to get image: " + tmp);
                }
                runningY += tileSize;
            }
            runningX += tileSize;
            pm.worked(1);
        }
        pm.done();

        g2d.dispose();

        String nameL = outFile.getName().toLowerCase();
        if (nameL.endsWith("png")) {
            ImageIO.write(outImage, "png", outFile);
        } else if (nameL.endsWith("jpg")) {
            ImageIO.write(outImage, "jpg", outFile);
        } else if (nameL.endsWith("tiff")) {
            ImageIO.write(outImage, "tiff", outFile);
        }

        MathTransform transform = CRS.findMathTransform(latLongCrs, mercatorCrs);

        Coordinate llES = new Coordinate(imageE, imageS);
        Coordinate llWN = new Coordinate(imageW, imageN);
        Coordinate osmES = JTS.transform(llES, null, transform);
        Coordinate osmWN = JTS.transform(llWN, null, transform);

        // create tfw
        double dx = (osmES.x - osmWN.x) / width;
        double dy = (osmWN.y - osmES.y) / height;
        StringBuilder sb = new StringBuilder();
        sb.append(dx).append("\n");
        sb.append("0.00000000").append("\n");
        sb.append("0.00000000").append("\n");
        sb.append(-dy).append("\n");
        sb.append(osmWN.x).append("\n");
        sb.append(osmWN.y).append("\n");

        String fileName = FileUtilities.getNameWithoutExtention(outFile);

        File folder = outFile.getParentFile();

        File tfwFile = new File(folder, fileName + ".tfw");

        FileUtilities.writeFile(sb.toString(), tfwFile);

        // create prj
        File prjFile = new File(folder, fileName + ".prj");
        FileUtilities.writeFile(mercatorCrs.toWKT(), prjFile);

    }

    private static int[] getTileXY( final double lat, final double lon, final int zoom ) {
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
        return new int[]{xtile, ytile};
    }

    private class BoundingBox {
        double north;
        double south;
        double east;
        double west;
    }

    private BoundingBox tile2boundingBox( final int x, final int y, final int zoom ) {
        BoundingBox bb = new BoundingBox();
        bb.north = tile2lat(y, zoom);
        bb.south = tile2lat(y + 1, zoom);
        bb.west = tile2lon(x, zoom);
        bb.east = tile2lon(x + 1, zoom);
        return bb;
    }

    private static double tile2lon( int x, int z ) {
        return x / Math.pow(2.0, z) * 360.0 - 180.0;
    }

    private static double tile2lat( int y, int z ) {
        double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    public static void main( String[] args ) throws Exception {
        String outFile = "/home/hydrologis/Dropbox/hydrologis/lavori/2015_05_bim_sarcamincio/shape_3857/image_output/osm15.png";

        String bounds = "/home/hydrologis/Dropbox/hydrologis/lavori/2015_05_bim_sarcamincio/shape_3857/area.shp";
        String inServiceUrl = "http://a.tile.opencyclemap.org/cycle/ZZZ/XXX/YYY.png";
        // String inServiceUrl = "http://a.tile.openstreetmap.org/ZZZ/XXX/YYY.png";

        ReferencedEnvelope env = OmsVectorReader.readVector(bounds).getBounds();

        OpenstreetmapImageCreator c = new OpenstreetmapImageCreator(inServiceUrl, 15, env, new File(outFile),
                new LogProgressMonitor());
        c.generate();

    }

}
