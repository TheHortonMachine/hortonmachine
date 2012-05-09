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

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.io.File;
import java.io.IOException;
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
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.vectorreader.VectorReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.images.ImageGenerator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.GeometryFactory;

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

    @Description("The north bound of the region to consider (needs to be EPSG:900913)")
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description("The south bound of the region to consider (needs to be EPSG:900913)")
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description("The west bound of the region to consider (needs to be EPSG:900913)")
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description("The east bound of the region to consider (needs to be EPSG:900913)")
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The folder inside which to create the tiles.")
    @In
    public String inPath;

    private static final String EPSG_900913 = "EPSG:3857";

    private GeometryFactory gf = GeometryUtilities.gf();

    private int TILESIZE = 256;

    @Execute
    public void process() throws Exception {
        checkNull(inPath, inRasters, inVectors, pMinzoom, pMaxzoom, pWest, pEast, pSouth, pNorth);
        CoordinateReferenceSystem crs = CRS.decode(EPSG_900913);
        ReferencedEnvelope totalBounds = new ReferencedEnvelope(pWest, pEast, pSouth, pNorth, crs);

        File inFolder = new File(inPath);
        File baseFolder = new File(inFolder, pName);

        ImageGenerator imgGen = new ImageGenerator(null, totalBounds);
        for( String rasterPath : inRasters ) {
            imgGen.addCoveragePath(rasterPath);
        }
        for( String vectorPath : inVectors ) {
            imgGen.addFeaturePath(vectorPath, null);
        }
        imgGen.setLayers();

        GlobalMercator mercator = new GlobalMercator();

        for( int z = pMinzoom; z <= pMaxzoom; z++ ) {

            // get ul and lr tile number
            int[] ulTileNumber = mercator.MetersToTile(pWest, pNorth, z);
            int[] lrTileNumber = mercator.MetersToTile(pEast, pSouth, z);

            int startXTile = min(ulTileNumber[0], lrTileNumber[0]);
            int startYTile = min(ulTileNumber[1], lrTileNumber[1]);
            int endXTile = max(ulTileNumber[0], lrTileNumber[0]);
            int endYTile = max(ulTileNumber[1], lrTileNumber[1]);

            pm.beginTask("Generating tiles at zoom level: " + z, (endXTile - startXTile + 1));
            for( int i = startXTile; i <= endXTile; i++ ) {

                for( int j = startYTile; j <= endYTile; j++ ) {
                    double[] bounds = mercator.TileBounds(i, j, z);
                    double west = bounds[0];
                    double south = bounds[1];
                    double east = bounds[2];
                    double north = bounds[3];

                    ReferencedEnvelope tmpBounds = new ReferencedEnvelope(west, east, south, north, crs);

                    File imageFolder = new File(baseFolder, z + "/" + i);
                    if (!imageFolder.exists()) {
                        if (!imageFolder.mkdirs()) {
                            throw new ModelsIOException("Unable to create folder:" + imageFolder, this);
                        }
                    }
                    File imageFile = new File(imageFolder, j + ".png");
                    try {
                        imgGen.dumpPngImage(imageFile.getAbsolutePath(), tmpBounds, TILESIZE, TILESIZE, 0.0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                }
                pm.worked(1);
            }
            pm.done();

        }

    }

    public static void main( String[] args ) throws Exception {

        String base = "D:/My Dropbox/hydrologis/lavori/2011_01_carta_pericolo_valsole/parteC/pericolo_ftf_2012_05_01/";
        String ctpFile = "D:/data-mega/ctp/ctp.shp";
        // String ctpFile = "/home/moovida/data/ctp/ctp.shp";

        String[] shpNames = {//
        // "pericolo_ftf_almazzago.shp", //
                "pericolo_ftf_corda_filled.shp", //
                // "pericolo_ftf_fazzon_filled.shp", //
                // "pericolo_ftf_meledrio_filled.shp", //
                // "pericolo_ftf_piano_filled.shp", //
                // "pericolo_ftf_rotiano_filled.shp", //
                // "pericolo_ftf_spona_filled.shp", //
                // "pericolo_ftf_valdelduc_filled.shp", //
                // "pericolo_ftf_vallone_filled.shp", //
                // "pericolo_ftf_valpanciana_filled.shp", //
                "sintesi_geo_conoide_reticolo.shp", //
                "pericolo_ftf_reticolo.shp", // main layer
        };

        List<String> inVectors = new ArrayList<String>();
        for( String name : shpNames ) {
            inVectors.add(base + name);
        }
        List<String> inRasters = new ArrayList<String>();
        inRasters.add(ctpFile);

        SimpleFeatureCollection boundsVector = VectorReader.readVector(inVectors.get(0));
        ReferencedEnvelope bounds = boundsVector.getBounds();

        TmsGenerator gen = new TmsGenerator();
        gen.inVectors = inVectors;
        gen.inRasters = inRasters;
        gen.pMinzoom = 16;
        gen.pMaxzoom = 19;
        gen.pName = "corda";
        gen.inPath = "D:/TMP/AAAACORDA/tiles";
        gen.pWest = bounds.getMinX();
        gen.pEast = bounds.getMaxX();
        gen.pNorth = bounds.getMaxY();
        gen.pSouth = bounds.getMaxX();
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
        gen.pm = pm;
        gen.process();
    }
}
