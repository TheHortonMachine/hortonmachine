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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.vectorreader.VectorReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.images.ImageGenerator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

@Description("Module for the generation of map tiles.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Vector, TMS, tiles")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.DRAFT)
@Name("tmsgenerator")
@License("General Public License Version 3 (GPLv3)")
@SuppressWarnings("nls")
public class TmsGenerator extends JGTModel {

    @Description("Raster layers to consider (the order is relevant, first layers are placed below others).")
    @In
    public List<String> inRasters = null;

    @Description("Vector layers to consider (the order is relevant, first layers are placed below others).")
    @In
    public List<String> inVectors = null;

    @Description("A name of the tile source.")
    @In
    public String pName = "tmstiles";

    @Description("Min zoom.")
    @In
    public Integer pMinzoom = null;

    @Description("Max zoom.")
    @In
    public Integer pMaxzoom = null;

    @Description("The north bound of the region to consider.")
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description("The south bound of the region to consider.")
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description("The west bound of the region to consider.")
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description("The east bound of the region to consider.")
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description("The coordinate reference system of the bound coordinates (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pEpsg;

    @Description("Switch that set to true allows for some error due to different datums. If set to false, it won't reproject without Bursa Wolf parameters.")
    @In
    public boolean doLenient = true;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The folder inside which to create the tiles.")
    @In
    public String inPath;

    private static final String EPSG_MERCATOR = "EPSG:3857";
    private static final String EPSG_LATLONG = "EPSG:4326";

    private int TILESIZE = 256;

    @Execute
    public void process() throws Exception {
        checkNull(inPath, pEpsg, pMinzoom, pMaxzoom, pWest, pEast, pSouth, pNorth);

        CoordinateReferenceSystem boundsCrs = CRS.decode(pEpsg);

        CoordinateReferenceSystem mercatorCrs = CRS.decode(EPSG_MERCATOR);
        CoordinateReferenceSystem latLongCrs = CRS.decode(EPSG_LATLONG);
        ReferencedEnvelope inBounds = new ReferencedEnvelope(pWest, pEast, pSouth, pNorth, boundsCrs);
        MathTransform in2MercatorTransform = CRS.findMathTransform(boundsCrs, mercatorCrs);
        Envelope mercatorEnvelope = JTS.transform(inBounds, in2MercatorTransform);
        ReferencedEnvelope mercatorBounds = new ReferencedEnvelope(mercatorEnvelope, mercatorCrs);

        MathTransform transform = CRS.findMathTransform(mercatorCrs, latLongCrs);
        Envelope latLongBounds = JTS.transform(mercatorBounds, transform);
        Coordinate latLongCentre = latLongBounds.centre();

        File inFolder = new File(inPath);
        File baseFolder = new File(inFolder, pName);

        ImageGenerator imgGen = new ImageGenerator(null);
        if (inRasters != null)
            for( String rasterPath : inRasters ) {
                imgGen.addCoveragePath(rasterPath);
            }
        if (inVectors != null)
            for( String vectorPath : inVectors ) {
                imgGen.addFeaturePath(vectorPath, null);
            }
        imgGen.setLayers();

        double w = mercatorBounds.getMinX();
        double s = mercatorBounds.getMinY();
        double e = mercatorBounds.getMaxX();
        double n = mercatorBounds.getMaxY();

        GlobalMercator mercator = new GlobalMercator();

        for( int z = pMinzoom; z <= pMaxzoom; z++ ) {

            // get ul and lr tile number
            int[] llTileNumber = mercator.MetersToTile(w, s, z);
            int[] urTileNumber = mercator.MetersToTile(e, n, z);

            int startXTile = llTileNumber[0];
            int startYTile = llTileNumber[1];
            int endXTile = urTileNumber[0];
            int endYTile = urTileNumber[1];

            // minx, miny, maxx, maxy
            // while( mercator.TileBounds(startXTile, startYTile, z)[0] > w ) {
            // startXTile--;
            // }
            // while( mercator.TileBounds(startXTile, startYTile, z)[1] > s ) {
            // startYTile--;
            // }
            // while( mercator.TileBounds(endXTile, endYTile, z)[2] < e ) {
            // startXTile++;
            // }
            // while( mercator.TileBounds(endXTile, endYTile, z)[3] < n ) {
            // startYTile++;
            // }
            // double[] firstTileBounds = mercator.TileBounds(startXTile, startYTile, z);
            // double[] lastTileBounds = mercator.TileBounds(endXTile, endYTile, z);

            int tileNum = 0;

            ReferencedEnvelope levelBounds = new ReferencedEnvelope();

            pm.beginTask("Generating tiles at zoom level: " + z, (endXTile - startXTile + 1));
            for( int i = startXTile; i <= endXTile; i++ ) {

                for( int j = startYTile; j <= endYTile; j++ ) {
                    tileNum++;
                    double[] bounds = mercator.TileBounds(i, j, z);
                    double west = bounds[0];
                    double south = bounds[1];
                    double east = bounds[2];
                    double north = bounds[3];

                    ReferencedEnvelope tmpBounds = new ReferencedEnvelope(west, east, south, north, mercatorCrs);
                    levelBounds.expandToInclude(tmpBounds);

                    File imageFolder = new File(baseFolder, z + "/" + i);
                    if (!imageFolder.exists()) {
                        if (!imageFolder.mkdirs()) {
                            throw new ModelsIOException("Unable to create folder:" + imageFolder, this);
                        }
                    }
                    File imageFile = new File(imageFolder, j + ".png");
                    if (imageFile.exists()) {
                        continue;
                    }
                    try {
                        imgGen.dumpPngImage(imageFile.getAbsolutePath(), tmpBounds, TILESIZE, TILESIZE, 0.0);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.exit(1);
                    }
                }
                pm.worked(1);
            }
            pm.done();

            pm.message("Zoom level: " + z + " has " + tileNum + " tiles.");
            pm.message("Boundary covered at Zoom level: " + z + ": " + levelBounds);
            pm.message("Total boundary wanted: " + mercatorBounds);

        }

        StringBuilder properties = new StringBuilder();
        properties.append("url=").append(pName).append("/ZZZ/XXX/YYY.png\n");
        properties.append("minzoom=").append(pMinzoom).append("\n");
        properties.append("maxzoom=").append(pMaxzoom).append("\n");
        properties.append("center=").append(latLongCentre.y).append(" ").append(latLongCentre.x).append("\n");
        properties.append("type=tms").append("\n");

        File propFile = new File(inFolder, pName + ".mapurl");
        FileUtilities.writeFile(properties.toString(), propFile);
    }
}
