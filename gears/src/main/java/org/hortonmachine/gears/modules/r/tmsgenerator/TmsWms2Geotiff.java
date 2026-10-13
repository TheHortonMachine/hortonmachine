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
import static org.hortonmachine.gears.libs.modules.Variables.GOOGLE;
import static org.hortonmachine.gears.libs.modules.Variables.OGC_TMS;
import static org.hortonmachine.gears.libs.modules.Variables.TMS;
import static org.hortonmachine.gears.libs.modules.Variables.WMS;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_KEYWORDS;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_LABEL;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_LICENSE;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_NAME;
import static org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff.OmsOnlineTiles2Geotiff_STATUS;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.URI;

import javax.imageio.ImageIO;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

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
    @UI("combo:" + GOOGLE + "," + OGC_TMS)
    @In
    public String pSchemaType = GOOGLE;

    @Description(OmsOnlineTiles2Geotiff_pSourceType_DESCRIPTION)
    @UI("combo:" + TMS + "," + WMS)
    @In
    public String pSourceType = TMS;

    @Description(OmsOnlineTiles2Geotiff_pMinzoom_DESCRIPTION)
    @In
    public Integer pZoomlevel = 18;

    @Description(OmsOnlineTiles2Geotiff_outRaster_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster = null;

    public static final String OmsOnlineTiles2Geotiff_DESCRIPTION = "Downloads the map tiles of an online service over a region and joins them into a GeoTIFF image.";
    public static final String OmsOnlineTiles2Geotiff_DOCUMENTATION = """
            The service is a tile service (TMS, XYZ or WMTS with the indexes in the url) or a \
            WMS. The url of a tile service has the placeholders ZZZ, XXX and YYY, replaced by the \
            zoom level and the column and row of each tile, as \
            https://tile.example.org/ZZZ/XXX/YYY.png. The rows are numbered from the north, as \
            in most of the web maps (the Google schema, also called XYZ), or from the south \
            (the OGC TMS schema). The url of a WMS has the placeholders WWW, SSS, EEE and NNN, \
            replaced by the bounds of each tile in longitude and latitude: it has to ask for \
            images of 256x256 pixels in EPSG:4326.

            The tiles of the zoom level that cover the bounds of the region of interest are joined \
            in an RGB image in Web Mercator (EPSG:3857) with the extent of the whole tiles, so a \
            bit larger than the region. Each zoom level halves the size of the cells: at zoom 18 \
            they are about 0.6 m at the equator, and the tiles of a large region quickly become \
            millions, too many for a single image. A tile that can't be downloaded stays black, \
            with an error message. The terms of use of the services usually limit the bulk \
            downloads of tiles.
            """;
    public static final String OmsOnlineTiles2Geotiff_KEYWORDS = "Raster, Vector, TMS, WMS, Tiles, Download";
    public static final String OmsOnlineTiles2Geotiff_LABEL = RASTERPROCESSING;
    public static final String OmsOnlineTiles2Geotiff_NAME = "tms2geotiff";
    public static final int OmsOnlineTiles2Geotiff_STATUS = 40;
    public static final String OmsOnlineTiles2Geotiff_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OmsOnlineTiles2Geotiff_AUTHORNAMES = "Andrea Antonello";
    public static final String OmsOnlineTiles2Geotiff_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OmsOnlineTiles2Geotiff_REGION_OF_INTEREST_SHAPEFILE_PATH = "The vector whose bounds are the region to download.";
    public static final String OmsOnlineTiles2Geotiff_inServiceUrl_DESCRIPTION = "The url of the service, with ZZZ, XXX and YYY in place of the tile indexes, or WWW, SSS, EEE, NNN in place of the bounds of a WMS.";
    public static final String OmsOnlineTiles2Geotiff_pType_DESCRIPTION = "The numbering of the tile rows: from the north (Google, XYZ) or from the south (OGC TMS).";
    public static final String OmsOnlineTiles2Geotiff_pSourceType_DESCRIPTION = "The type of service: tiles or WMS.";
    public static final String OmsOnlineTiles2Geotiff_outRaster_DESCRIPTION = "The output GeoTIFF path.";
    public static final String OmsOnlineTiles2Geotiff_pMinzoom_DESCRIPTION = "The zoom level of the tiles.";

    private static final String EPSG_MERCATOR = "EPSG:3857";

    private static final int tileSize = 256;

    @Execute
    public void process() throws Exception {
        checkNull(inServiceUrl, outRaster, inRoiPath, pZoomlevel);
        if (!GOOGLE.equals(pSchemaType) && !OGC_TMS.equals(pSchemaType)) {
            throw new ModelsIllegalargumentException("The tile schema can be only: " + GOOGLE + " or " + OGC_TMS, this, pm);
        }
        if (!TMS.equals(pSourceType) && !WMS.equals(pSourceType)) {
            throw new ModelsIllegalargumentException("The source type can be only: " + TMS + " or " + WMS, this, pm);
        }

        CoordinateReferenceSystem mercatorCrs = HMCrsRegistry.INSTANCE.getCrs(EPSG_MERCATOR);
        CoordinateReferenceSystem latLongCrs = DefaultGeographicCRS.WGS84;

        SimpleFeatureCollection inRoi = getVector(inRoiPath);
        ReferencedEnvelope latLongBounds = inRoi.getBounds().transform(latLongCrs, true);

        int z = pZoomlevel;
        int maxIndex = (int) Math.pow(2, z) - 1;
        GlobalMercator gm = new GlobalMercator();

        // the tiles in the Google numbering, with the rows from the north
        int[] southWestTile = gm.GoogleTile(latLongBounds.getMinY(), latLongBounds.getMinX(), z);
        int[] northEastTile = gm.GoogleTile(latLongBounds.getMaxY(), latLongBounds.getMaxX(), z);
        int startXTile = Math.min(southWestTile[0], northEastTile[0]);
        int endXTile = Math.max(southWestTile[0], northEastTile[0]);
        int startYTile = Math.min(southWestTile[1], northEastTile[1]);
        int endYTile = Math.max(southWestTile[1], northEastTile[1]);

        int tilesCountX = endXTile - startXTile + 1;
        int tilesCountY = endYTile - startYTile + 1;
        long pixels = (long) tilesCountX * tilesCountY * tileSize * tileSize;
        if (pixels > Integer.MAX_VALUE / 4) {
            throw new ModelsIllegalargumentException("The region needs " + tilesCountX * (long) tilesCountY
                    + " tiles at zoom level " + z + ", too many for a single image: use a lower zoom level or a smaller region.",
                    this, pm);
        }
        int width = tilesCountX * tileSize;
        int height = tilesCountY * tileSize;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) img.getGraphics();
        ReferencedEnvelope finalMercatorBounds = new ReferencedEnvelope(mercatorCrs);

        pm.beginTask("Getting tiles at zoom level: " + z, tilesCountX);
        for( int x = startXTile; x <= endXTile; x++ ) {
            for( int y = startYTile; y <= endYTile; y++ ) {
                // the TMS row, from the south, that also gives the bounds of the tile
                int tmsY = maxIndex - y;
                double[] bounds = gm.TileBounds(x, tmsY, z);
                ReferencedEnvelope tileBounds = new ReferencedEnvelope(bounds[0], bounds[2], bounds[1], bounds[3], mercatorCrs);
                finalMercatorBounds.expandToInclude(tileBounds);

                String tileUrl;
                if (pSourceType.equals(TMS)) {
                    int urlY = pSchemaType.equals(GOOGLE) ? y : tmsY;
                    tileUrl = inServiceUrl.replaceFirst("ZZZ", String.valueOf(z));
                    tileUrl = tileUrl.replaceFirst("XXX", String.valueOf(x));
                    tileUrl = tileUrl.replaceFirst("YYY", String.valueOf(urlY));
                } else {
                    ReferencedEnvelope llTB = tileBounds.transform(latLongCrs, true);
                    tileUrl = inServiceUrl.replaceFirst("SSS", String.valueOf(llTB.getMinY()));
                    tileUrl = tileUrl.replaceFirst("NNN", String.valueOf(llTB.getMaxY()));
                    tileUrl = tileUrl.replaceFirst("WWW", String.valueOf(llTB.getMinX()));
                    tileUrl = tileUrl.replaceFirst("EEE", String.valueOf(llTB.getMaxX()));
                }

                // the rows from the north go down the image
                int xPix = (x - startXTile) * tileSize;
                int yPix = (y - startYTile) * tileSize;
                try (InputStream imgStream = URI.create(tileUrl).toURL().openStream()) {
                    BufferedImage tileImg = ImageIO.read(imgStream);
                    g2d.drawImage(tileImg, xPix, yPix, tileSize, tileSize, null);
                } catch (Exception ex) {
                    pm.errorMessage("Unable to get image: " + tileUrl);
                }
            }
            pm.worked(1);
        }
        pm.done();
        g2d.dispose();

        pm.message("Zoom level: " + z + " has " + tilesCountX * tilesCountY + " tiles.");

        RegionMap envelopeParams = RegionMap.fromBoundsAndGrid(finalMercatorBounds.getMinX(), finalMercatorBounds.getMaxX(),
                finalMercatorBounds.getMinY(), finalMercatorBounds.getMaxY(), width, height);
        GridCoverage2D coverage = CoverageUtilities.buildCoverage("tmsraster", img, envelopeParams, mercatorCrs);
        OmsRasterWriter.writeRaster(outRaster, coverage);
    }

}
