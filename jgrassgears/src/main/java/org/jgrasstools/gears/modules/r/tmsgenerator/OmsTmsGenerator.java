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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.images.ImageGenerator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

@Description("Module for the generation of map tiles.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Vector, TMS, Tiles")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.DRAFT)
@Name("tmsgenerator")
@License("General Public License Version 3 (GPLv3)")
@SuppressWarnings("nls")
public class OmsTmsGenerator extends JGTModel {

    @Description("A file containing the list of raster map paths to consider (the order is relevant, first layers are placed below others).")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRasterFile = null;

    @Description("Optional regions for reading the rasters.")
    @In
    public List<GridGeometry2D> inRasterBounds = null;

    @Description("A file containing the list of vector map paths to consider (the order is relevant, first layers are placed below others).")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVectorFile = null;

    @Description("An optional WMS url and layer name in the format: http://wmsurl#layername")
    @In
    public String inWMS = null;

    @Description("A name of the tile source.")
    @In
    public String pName = "tmstiles";

    @Description("The min zoom for which to generate tiles.")
    @In
    public Integer pMinzoom = null;

    @Description("The max zoom for which to generate tiles.")
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

    @Description("The coordinate reference system of the bound coordinates and supplied datasets (ex. EPSG:4328).")
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pEpsg;

    @Description("Switch that set to true allows for some error due to different datums. If set to false, it won't reproject without Bursa Wolf parameters.")
    @In
    public boolean doLenient = true;

    @Description("The image type to generate (0 = png = default, 1 = jpg).")
    @In
    public int pImagetype = 0;

    @Description("A color rgb tripled. if it is not null and a tiles is made only of that color, then the tiles is not generated. Usefull to avoid generation of empty tiles.")
    @In
    public int[] pCheckcolor = new int[]{255, 255, 255};

    @Description("Optional flag to force a legacy GRASS driver usage.")
    @In
    public Boolean doLegacyGrass = false;

    @Description("The folder inside which to create the tiles.")
    @In
    public String inPath;

    @Description("Max threads to use (default 1)")
    @In
    public Integer pMaxThreads = 1;

    private static final String EPSG_MERCATOR = "EPSG:3857";
    private static final String EPSG_LATLONG = "EPSG:4326";

    private int TILESIZE = 256;

    @Execute
    public void process() throws Exception {
        checkNull(inPath, pEpsg, pMinzoom, pMaxzoom, pWest, pEast, pSouth, pNorth);

        String ext = "png";
        if (pImagetype == 1) {
            ext = "jpg";
        }

        List<String> inVectors = null;
        if (inVectorFile != null && new File(inVectorFile).exists())
            inVectors = FileUtilities.readFileToLinesList(new File(inVectorFile));

        List<String> inRasters = null;
        if (inRasterFile != null && new File(inRasterFile).exists())
            inRasters = FileUtilities.readFileToLinesList(new File(inRasterFile));

        if (inRasters == null && inVectors == null) {
            throw new ModelsIllegalargumentException("No raster and vector input maps available. check your inputs.", this);
        }

        CoordinateReferenceSystem boundsCrs = CRS.decode(pEpsg);

        final CoordinateReferenceSystem mercatorCrs = CRS.decode(EPSG_MERCATOR);
        CoordinateReferenceSystem latLongCrs = CRS.decode(EPSG_LATLONG);
        ReferencedEnvelope inBounds = new ReferencedEnvelope(pWest, pEast, pSouth, pNorth, boundsCrs);
        MathTransform in2MercatorTransform = CRS.findMathTransform(boundsCrs, mercatorCrs);
        Envelope mercatorEnvelope = JTS.transform(inBounds, in2MercatorTransform);
        ReferencedEnvelope mercatorBounds = new ReferencedEnvelope(mercatorEnvelope, mercatorCrs);

        MathTransform transform = CRS.findMathTransform(mercatorCrs, latLongCrs);
        Envelope latLongBounds = JTS.transform(mercatorBounds, transform);
        Coordinate latLongCentre = latLongBounds.centre();

        File inFolder = new File(inPath);
        final File baseFolder = new File(inFolder, pName);

        final ImageGenerator imgGen = new ImageGenerator(pm);
        if (inWMS != null) {
            imgGen.setWMS(inWMS);
        }
        imgGen.doLegacyGrass = doLegacyGrass;

        String notLoading = "Not loading non-existing file: ";
        if (inRasters != null)
            for( String rasterPath : inRasters ) {
                File file = new File(rasterPath);
                if (file.exists()) {
                    imgGen.addCoveragePath(rasterPath);
                } else {
                    pm.errorMessage(notLoading + rasterPath);
                }
            }
        if (inRasterBounds != null)
            for( GridGeometry2D rasterBounds : inRasterBounds ) {
                imgGen.addCoverageRegion(rasterBounds);
            }
        if (inVectors != null)
            for( String vectorPath : inVectors ) {
                File file = new File(vectorPath);
                if (file.exists()) {
                    imgGen.addFeaturePath(vectorPath, null);
                } else {
                    pm.errorMessage(notLoading + vectorPath);
                }
            }
        imgGen.setLayers();

        double w = mercatorBounds.getMinX();
        double s = mercatorBounds.getMinY();
        double e = mercatorBounds.getMaxX();
        double n = mercatorBounds.getMaxY();

        final GlobalMercator mercator = new GlobalMercator();

        for( int z = pMinzoom; z <= pMaxzoom; z++ ) {

            // get ul and lr tile number
            int[] llTileNumber = mercator.MetersToTile(w, s, z);
            int[] urTileNumber = mercator.MetersToTile(e, n, z);

            int startXTile = llTileNumber[0];
            int startYTile = llTileNumber[1];
            int endXTile = urTileNumber[0];
            int endYTile = urTileNumber[1];

            int tileNum = 0;

            final ReferencedEnvelope levelBounds = new ReferencedEnvelope(mercatorCrs);

            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(pMaxThreads);

            pm.beginTask("Generating tiles at zoom level: " + z, (endXTile - startXTile + 1) * (endYTile - startYTile + 1));
            for( int i = startXTile; i <= endXTile; i++ ) {

                for( int j = startYTile; j <= endYTile; j++ ) {
                    tileNum++;
                    double[] bounds = mercator.TileBounds(i, j, z);
                    double west = bounds[0];
                    double south = bounds[1];
                    double east = bounds[2];
                    double north = bounds[3];

                    final ReferencedEnvelope tmpBounds = new ReferencedEnvelope(west, east, south, north, mercatorCrs);
                    levelBounds.expandToInclude(tmpBounds);

                    File imageFolder = new File(baseFolder, z + "/" + i);
                    if (!imageFolder.exists()) {
                        if (!imageFolder.mkdirs()) {
                            throw new ModelsIOException("Unable to create folder:" + imageFolder, this);
                        }
                    }

                    File ignoreMediaFile = new File(imageFolder, ".nomedia");
                    ignoreMediaFile.createNewFile();

                    final File imageFile = new File(imageFolder, j + "." + ext);
                    if (imageFile.exists()) {
                        pm.worked(1);
                        continue;
                    }
                    final String imagePath = imageFile.getAbsolutePath();
                    final ReferencedEnvelope finalBounds = tmpBounds;
                    Runnable runner = new Runnable(){
                        public void run() {
                            try {
                                if (pImagetype == 1) {
                                    imgGen.dumpJpgImage(imagePath, finalBounds, TILESIZE, TILESIZE, 0.0, pCheckcolor);
                                } else {
                                    imgGen.dumpPngImage(imagePath, finalBounds, TILESIZE, TILESIZE, 0.0, pCheckcolor);
                                }
                                pm.worked(1);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                System.exit(1);
                            }
                        }
                    };
                    fixedThreadPool.execute(runner);

                }
            }
            try {
                fixedThreadPool.shutdown();
                while( !fixedThreadPool.isTerminated() ) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException exx) {
                exx.printStackTrace();
            }
            pm.done();

            pm.message("Zoom level: " + z + " has " + tileNum + " tiles.");
            // pm.message("Boundary covered at Zoom level: " + z + ": " + levelBounds);
            // pm.message("Total boundary wanted: " + mercatorBounds);

        }

        StringBuilder properties = new StringBuilder();

        properties.append("url=").append(pName).append("/ZZZ/XXX/YYY.").append(ext).append("\n");
        properties.append("minzoom=").append(pMinzoom).append("\n");
        properties.append("maxzoom=").append(pMaxzoom).append("\n");
        properties.append("center=").append(latLongCentre.y).append(" ").append(latLongCentre.x).append("\n");
        properties.append("type=tms").append("\n");

        File propFile = new File(inFolder, pName + ".mapurl");
        FileUtilities.writeFile(properties.toString(), propFile);
    }
}
