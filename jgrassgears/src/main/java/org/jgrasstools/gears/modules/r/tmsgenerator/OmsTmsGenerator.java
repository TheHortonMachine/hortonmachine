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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_doLegacyGrass_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_doLenient_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_inPath_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_inRasterBounds_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_inRasterFile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_inVectorFile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_inWMS_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pCheckcolor_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pEpsg_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pImagetype_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pMaxzoom_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pMinzoom_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pName_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTMSGENERATOR_pWest_DESCRIPTION;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.libs.exceptions.ModelsIOException;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.images.ImageGenerator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

@Description(OMSTMSGENERATOR_DESCRIPTION)
@Documentation(OMSTMSGENERATOR_DOCUMENTATION)
@Author(name = OMSTMSGENERATOR_AUTHORNAMES, contact = OMSTMSGENERATOR_AUTHORCONTACTS)
@Keywords(OMSTMSGENERATOR_KEYWORDS)
@Label(OMSTMSGENERATOR_LABEL)
@Name(OMSTMSGENERATOR_NAME)
@Status(OMSTMSGENERATOR_STATUS)
@License(OMSTMSGENERATOR_LICENSE)
public class OmsTmsGenerator extends JGTModel {

    @Description(OMSTMSGENERATOR_inRasterFile_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRasterFile = null;

    @Description(OMSTMSGENERATOR_inRasterBounds_DESCRIPTION)
    @In
    public List<GridGeometry2D> inRasterBounds = null;

    @Description(OMSTMSGENERATOR_inVectorFile_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inVectorFile = null;

    @Description(OMSTMSGENERATOR_inWMS_DESCRIPTION)
    @In
    public String inWMS = null;

    @Description(OMSTMSGENERATOR_pName_DESCRIPTION)
    @In
    public String pName = "tmstiles";

    @Description(OMSTMSGENERATOR_pMinzoom_DESCRIPTION)
    @In
    public Integer pMinzoom = null;

    @Description(OMSTMSGENERATOR_pMaxzoom_DESCRIPTION)
    @In
    public Integer pMaxzoom = null;

    @Description(OMSTMSGENERATOR_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSTMSGENERATOR_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSTMSGENERATOR_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSTMSGENERATOR_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSTMSGENERATOR_pEpsg_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pEpsg;

    @Description("An optional prj file to use instead of teh epsg code.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPrj;

    @Description("A shapefile to use to draw maps on zoom levels higher than pZoomLimit. Everything outside is not drawn.")
    @In
    public String inZoomLimitVector;

    @Description("The zoom limit above which the inZoomLimitVector is considered.")
    @In
    public double pZoomLimit = 17;

    @Description(OMSTMSGENERATOR_doLenient_DESCRIPTION)
    @In
    public boolean doLenient = true;

    @Description(OMSTMSGENERATOR_pImagetype_DESCRIPTION)
    @In
    public int pImagetype = 0;

    @Description(OMSTMSGENERATOR_pCheckcolor_DESCRIPTION)
    @In
    public int[] pCheckcolor = new int[]{255, 255, 255};

    @Description(OMSTMSGENERATOR_doLegacyGrass_DESCRIPTION)
    @In
    public Boolean doLegacyGrass = false;

    @Description("Do mbtiles database.")
    @In
    public boolean doMbtiles = false;

    @Description(OMSTMSGENERATOR_inPath_DESCRIPTION)
    @In
    public String inPath;

    private static final String EPSG_MERCATOR = "EPSG:3857";
    private static final String EPSG_LATLONG = "EPSG:4326";

    private int TILESIZE = 256;

    private PreparedGeometry zoomLimitGeometry;

    private MBTilesHelper mbtilesHelper;

    @Execute
    public void process() throws Exception {
        checkNull(inPath, pMinzoom, pMaxzoom, pWest, pEast, pSouth, pNorth);

        CoordinateReferenceSystem dataCrs;
        if (pEpsg != null) {
            dataCrs = CRS.decode(pEpsg);
        } else {
            String wkt = FileUtilities.readFile(inPrj);
            dataCrs = CRS.parseWKT(wkt);
        }

        String format = null;
        if (doMbtiles) {
            mbtilesHelper = new MBTilesHelper();
            File dbFolder = new File(inPath);
            File dbFile = new File(dbFolder, pName + ".mbtiles");

            ReferencedEnvelope dataBounds = new ReferencedEnvelope(pWest, pEast, pSouth, pNorth, dataCrs);
            MathTransform data2LLTransform = CRS.findMathTransform(dataCrs, DefaultGeographicCRS.WGS84);

            Envelope llEnvelope = JTS.transform(dataBounds, data2LLTransform);
            float n = (float) llEnvelope.getMaxY();
            float s = (float) llEnvelope.getMinY();
            float w = (float) llEnvelope.getMinX();
            float e = (float) llEnvelope.getMaxX();

            format = pImagetype == 0 ? "png" : "jpg";
            mbtilesHelper.open(dbFile);
            mbtilesHelper.createTables();
            mbtilesHelper.fillMetadata(n, s, w, e, pName, format, pMinzoom, pMaxzoom);
        }

        int threads = getDefaultThreadsNum() * 5;

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

        if (pEpsg == null && inPrj == null) {
            throw new ModelsIllegalargumentException("No projection info available. check your inputs.", this);
        }

        final CoordinateReferenceSystem mercatorCrs = CRS.decode(EPSG_MERCATOR);

        ReferencedEnvelope dataBounds = new ReferencedEnvelope(pWest, pEast, pSouth, pNorth, dataCrs);
        MathTransform data2MercatorTransform = CRS.findMathTransform(dataCrs, mercatorCrs);

        Envelope mercatorEnvelope = JTS.transform(dataBounds, data2MercatorTransform);
        ReferencedEnvelope mercatorBounds = new ReferencedEnvelope(mercatorEnvelope, mercatorCrs);

        if (inZoomLimitVector != null) {
            SimpleFeatureCollection zoomLimitVector = OmsVectorReader.readVector(inZoomLimitVector);
            List<Geometry> geoms = FeatureUtilities.featureCollectionToGeometriesList(zoomLimitVector, true, null);
            MultiPolygon multiPolygon = gf.createMultiPolygon(geoms.toArray(GeometryUtilities.TYPE_POLYGON));
            // convert to mercator
            Geometry multiPolygonGeom = JTS.transform(multiPolygon, data2MercatorTransform);
            zoomLimitGeometry = PreparedGeometryFactory.prepare(multiPolygonGeom);
        }

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

            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(threads);

            pm.beginTask("Generating tiles at zoom level: " + z, (endXTile - startXTile + 1) * (endYTile - startYTile + 1));
            for( int i = startXTile; i <= endXTile; i++ ) {

                for( int j = startYTile; j <= endYTile; j++ ) {

                    double[] bounds = mercator.TileBounds(i, j, z);
                    double west = bounds[0];
                    double south = bounds[1];
                    double east = bounds[2];
                    double north = bounds[3];

                    final ReferencedEnvelope tmpBounds = new ReferencedEnvelope(west, east, south, north, mercatorCrs);
                    levelBounds.expandToInclude(tmpBounds);

                    // if there is a zoom level geometry limitation, apply it
                    if (zoomLimitGeometry != null && z > pZoomLimit) {
                        double safeExtend = tmpBounds.getWidth() > tmpBounds.getHeight() ? tmpBounds.getWidth() : tmpBounds
                                .getHeight();
                        final ReferencedEnvelope tmp = new ReferencedEnvelope(tmpBounds);
                        tmp.expandBy(safeExtend);
                        Polygon polygon = FeatureUtilities.envelopeToPolygon(tmp);
                        if (!zoomLimitGeometry.intersects(polygon)) {
                            pm.worked(1);
                            continue;
                        }
                    }

                    if (mbtilesHelper != null) {
                        final int x = i;
                        final int y = j;
                        final int zz = z;
                        final String fformat = format;
                        tileNum++;
                        Runnable runner = new Runnable(){
                            public void run() {
                                try {
                                    BufferedImage image = imgGen.getImageWithCheck(tmpBounds, TILESIZE, TILESIZE, 0.0,
                                            pCheckcolor);
                                    if (image != null) {
                                        mbtilesHelper.addTile(x, y, zz, image, fformat);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    System.exit(1);
                                }
                                pm.worked(1);
                            }
                        };
                        fixedThreadPool.execute(runner);

                    } else {
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
                        tileNum++;
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

        if (mbtilesHelper != null) {
            mbtilesHelper.close();
        } else {
            CoordinateReferenceSystem latLongCrs = CRS.decode(EPSG_LATLONG);
            MathTransform transform = CRS.findMathTransform(mercatorCrs, latLongCrs);
            Envelope latLongBounds = JTS.transform(mercatorBounds, transform);
            Coordinate latLongCentre = latLongBounds.centre();

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
}
