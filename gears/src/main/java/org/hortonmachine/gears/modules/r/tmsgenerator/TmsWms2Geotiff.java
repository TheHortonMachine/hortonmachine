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

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.*;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_KEYWORDS;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_LABEL;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_LICENSE;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_NAME;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_STATUS;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URL;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OmsOnlineTiles2Geotiff_DESCRIPTION)
@Documentation(OmsOnlineTiles2Geotiff_DOCUMENTATION)
@Author(name = OmsOnlineTiles2Geotiff_AUTHORNAMES, contact = OmsOnlineTiles2Geotiff_AUTHORCONTACTS)
@Keywords(OmsOnlineTiles2Geotiff_KEYWORDS)
@Label(OmsOnlineTiles2Geotiff_LABEL)
@Name(OmsOnlineTiles2Geotiff_NAME)
@Status(OmsOnlineTiles2Geotiff_STATUS)
@License(OmsOnlineTiles2Geotiff_LICENSE)
public class TmsWms2Geotiff extends HMModel {

    @Description(OmsOnlineTiles2Geotiff_inServiceUrl_DESCRIPTION)
    @In
    public String inServiceUrl = null;

    @Description(OmsOnlineTiles2Geotiff_REGION_OF_INTEREST_SHAPEFILE_PATH)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inRoiPath = null;

    @Description(OmsOnlineTiles2Geotiff_pType_DESCRIPTION)
    @UI("combo:" + OGC_TMS + "," + GOOGLE)
    @In
    public String pSchemaType = TMS;

    @Description(OmsOnlineTiles2Geotiff_pSourceType_DESCRIPTION)
    @UI("combo:" + TMS + "," + WMS)
    @In
    public String pSourceType = TMS;

    @Description(OmsOnlineTiles2Geotiff_pMinzoom_DESCRIPTION)
    @In
    public Integer pZoomlevel = null;

    @Description(OmsOnlineTiles2Geotiff_outRaster_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    public static final String OmsOnlineTiles2Geotiff_REGION_OF_INTEREST_SHAPEFILE_PATH = "Region of interest shapefile path";
    public static final String OmsOnlineTiles2Geotiff_DESCRIPTION = "Module for the downloading map tiles of a TMS service and convert them to geotiff.";
    public static final String OmsOnlineTiles2Geotiff_DOCUMENTATION = "";
    public static final String OmsOnlineTiles2Geotiff_KEYWORDS = "Raster, Vector, TMS, Tiles";
    public static final String OmsOnlineTiles2Geotiff_LABEL = RASTERPROCESSING;
    public static final String OmsOnlineTiles2Geotiff_NAME = "tms2geotiff";
    public static final int OmsOnlineTiles2Geotiff_STATUS = 10;
    public static final String OmsOnlineTiles2Geotiff_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OmsOnlineTiles2Geotiff_AUTHORNAMES = "Andrea Antonello";
    public static final String OmsOnlineTiles2Geotiff_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OmsOnlineTiles2Geotiff_inServiceUrl_DESCRIPTION = "An online tile service with XXX,YYY,ZZZ or SSS,WWW,NNN,EEE to be substituted by indexes or bounds.";
    public static final String OmsOnlineTiles2Geotiff_pType_DESCRIPTION = "The tile schema type.";
    public static final String OmsOnlineTiles2Geotiff_pSourceType_DESCRIPTION = "The source schema type.";
    public static final String OmsOnlineTiles2Geotiff_outRaster_DESCRIPTION = "The output geotiff path.";
    public static final String OmsOnlineTiles2Geotiff_pMinzoom_DESCRIPTION = "The zoom level for which to generate the geotiff.";

    private static final String EPSG_MERCATOR = "EPSG:3857";

    private static final int tileSize = 256;

    private static final boolean doDebug = false;

    @Execute
    public void process() throws Exception {
        checkNull(inServiceUrl, outRaster, inRoiPath);

        CoordinateReferenceSystem mercatorCrs = CrsUtilities.getCrsFromEpsg(EPSG_MERCATOR, null);
        CoordinateReferenceSystem latLongCrs = DefaultGeographicCRS.WGS84;

        SimpleFeatureCollection inRoi = getVector(inRoiPath);

        ReferencedEnvelope inBounds = inRoi.getBounds();
        ReferencedEnvelope latLongBounds = inBounds.transform(latLongCrs, true);

        double w = latLongBounds.getMinX();
        double s = latLongBounds.getMinY();
        double e = latLongBounds.getMaxX();
        double n = latLongBounds.getMaxY();

        int z = 18;
        if (pZoomlevel != null) {
            z = pZoomlevel;
        }

        GlobalMercator gm = new GlobalMercator();

        int[] llTileXY = gm.GoogleTile(s, w, z);
        int[] urTileXY = gm.GoogleTile(n, e, z);
        int startXTile = Math.min(llTileXY[0], urTileXY[0]);
        int endXTile = Math.max(llTileXY[0], urTileXY[0]);
        int startYTile = Math.min(llTileXY[1], urTileXY[1]);
        int endYTile = Math.max(llTileXY[1], urTileXY[1]);

        if (pSchemaType.equals(OGC_TMS)) {
            llTileXY = gm.TMSTileFromGoogleTile(llTileXY[0], llTileXY[1], z);
            urTileXY = gm.TMSTileFromGoogleTile(urTileXY[0], urTileXY[1], z);
        }

        startXTile = Math.min(llTileXY[0], urTileXY[0]);
        endXTile = Math.max(llTileXY[0], urTileXY[0]);
        startYTile = Math.min(llTileXY[1], urTileXY[1]);
        endYTile = Math.max(llTileXY[1], urTileXY[1]);

        int tilesCountX = endXTile - startXTile + 1;
        int tilesCountY = endYTile - startYTile + 1;
        int width = tilesCountX * tileSize;
        int height = tilesCountY * tileSize;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) img.getGraphics();

        int tileNum = 0;
        final ReferencedEnvelope finalMercatorBounds = new ReferencedEnvelope(mercatorCrs);

        pm.beginTask("Getting tiles at zoom level: " + z, (endXTile - startXTile + 1));
        int runningXPix = 0;
        int runningYPix = 0;
        for( int x = startXTile; x <= endXTile; x++ ) {
            for( int y = startYTile; y <= endYTile; y++ ) {
                tileNum++;

                int yy = y;

                if (pSchemaType.equalsIgnoreCase(GOOGLE))
                    yy = (int) ((Math.pow(2, z) - 1) - y);
                double[] bounds = gm.TileBounds(x, yy, z);
                double west = bounds[0];
                double south = bounds[1];
                double east = bounds[2];
                double north = bounds[3];

                final ReferencedEnvelope tmpBounds = new ReferencedEnvelope(west, east, south, north, mercatorCrs);
                finalMercatorBounds.expandToInclude(tmpBounds);

                String tmp = "";
                if (pSourceType.equals(TMS)) {
                    tmp = inServiceUrl.replaceFirst("ZZZ", String.valueOf(z));
                    tmp = tmp.replaceFirst("XXX", String.valueOf(x));
                    tmp = tmp.replaceFirst("YYY", String.valueOf(y));
                } else if (pSourceType.equals(WMS)) {
                    ReferencedEnvelope llTB = tmpBounds.transform(latLongCrs, true);
                    tmp = inServiceUrl.replaceFirst("SSS", String.valueOf(llTB.getMinY()));
                    tmp = tmp.replaceFirst("NNN", String.valueOf(llTB.getMaxY()));
                    tmp = tmp.replaceFirst("WWW", String.valueOf(llTB.getMinX()));
                    tmp = tmp.replaceFirst("EEE", String.valueOf(llTB.getMaxX()));
                } else {
                    throw new ModelsIllegalargumentException("Source Type can be only 0 or 1.", this);
                }

//                System.out.println(x + "/" + y + ": " + tmp);
                URL url = new URL(tmp);
                try (InputStream imgStream = url.openStream()) {
                    BufferedImage tileImg = ImageIO.read(imgStream);
                    g2d.drawImage(tileImg, runningXPix, runningYPix, null);

                    if (doDebug) {
                        g2d.setColor(Color.RED);
                        g2d.drawRect(runningXPix, runningYPix, tileSize, tileSize);
                        String tileDescr = x + "/" + y + "/" + z;
                        g2d.drawString(tileDescr, runningXPix + 10, runningYPix + 20);
                    }
                } catch (Exception ex) {
                    pm.errorMessage("Unable to get image: " + tmp);
                }
                runningYPix += tileSize;
            }

            runningXPix += tileSize;
            runningYPix = 0;

            pm.worked(1);
        }
        pm.done();

        pm.message("Zoom level: " + z + " has " + tileNum + " tiles.");

        double ww = finalMercatorBounds.getMinX();
        double ss = finalMercatorBounds.getMinY();
        double ee = finalMercatorBounds.getMaxX();
        double nn = finalMercatorBounds.getMaxY();

        double xres = (ee - ww) / width;
        double yres = (nn - ss) / height;

        RegionMap envelopeParams = new RegionMap();
        envelopeParams.put(CoverageUtilities.NORTH, nn);
        envelopeParams.put(CoverageUtilities.SOUTH, ss);
        envelopeParams.put(CoverageUtilities.WEST, ww);
        envelopeParams.put(CoverageUtilities.EAST, ee);
        envelopeParams.put(CoverageUtilities.XRES, xres);
        envelopeParams.put(CoverageUtilities.YRES, yres);
        envelopeParams.put(CoverageUtilities.ROWS, (double) height);
        envelopeParams.put(CoverageUtilities.COLS, (double) width);

        GridCoverage2D coverage = CoverageUtilities.buildCoverage("tmsraster", img, envelopeParams, mercatorCrs);
        OmsRasterWriter.writeRaster(outRaster, coverage);

    }

    public static void main( String[] args ) throws Exception {
        TmsWms2Geotiff g = new TmsWms2Geotiff();
        g.inRoiPath = "/home/hydrologis/TMP/VIENNA/roi.shp";

        // VIENNA CONTOURS
//        g.inServiceUrl = "https://data.wien.gv.at/daten/geo?version=1.3.0&service=WMS&request=GetMap&crs=EPSG:4326&bbox=SSS,WWW,NNN,EEE&width=256&height=256&layers=ogdwien:HOEHENLINIEOGD&styles=&format=image/jpeg";
//        g.outRaster = "/home/hydrologis/TMP/VIENNA/vienna_contours_zoom16.tiff";

        // VIENNA AERIAL
        g.inServiceUrl = "http://maps.wien.gv.at/wmts/lb/farbe/google3857/ZZZ/YYY/XXX.jpeg";
        g.outRaster = "/home/hydrologis/TMP/VIENNA/vienna_zoom19.tiff";
        g.pSourceType = TMS;
        g.pSchemaType = GOOGLE;

        // VENETO WMTS
//        g.inRoiPath = "/home/hydrologis/Dropbox/hydrologis/lavori/2018_12_idro_bertani/data/roi.shp";
//        g.inServiceUrl = "https://idt2.regione.veneto.it/gwc/service/wmts?layer=rv:OrthoPhoto_2015_pyramid&tilematrixset=EPSG:900913&Service=WMTS&Request=GetTile&Version=1.0.0&Format=image/jpeg&TileMatrix=EPSG:900913:ZZZ&TileCol=XXX&TileRow=YYY";
//        g.outRaster = "/home/hydrologis/Dropbox/hydrologis/lavori/2018_12_idro_bertani/data/orto.tiff";
//        g.pSourceType = TMS;   
//        g.pSchemaType = GOOGLE;

        // VENETO WMS
//        g.inRoiPath = "/home/hydrologis/Dropbox/hydrologis/lavori/2018_12_idro_bertani/data/roi.shp";
//        g.inServiceUrl = "https://idt2.regione.veneto.it/gwc/service/wms?SERVICE=WMS&LAYERS=rv:OrthoPhoto_2015_pyramid&FORMAT=image/jpeg&HEIGHT=256&TRANSPARENT=TRUE&REQUEST=GetMap&BBOX=WWW,SSS,EEE,NNN&WIDTH=256&STYLES=&SRS=EPSG:4326&VERSION=1.1.1";
//        g.outRaster = "/home/hydrologis/Dropbox/hydrologis/lavori/2018_12_idro_bertani/data/orto3.tiff";
//        g.pSourceType = WMS;   
//        g.pSchemaType = GOOGLE;

        g.pZoomlevel = 19;
        g.process();

    }

}
