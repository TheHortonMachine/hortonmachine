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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_DO_LENIENT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_IN_PATH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_IN_RASTER_BOUNDS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_IN_RASTER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_IN_VECTOR_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_IN_WMS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_CHECK_COLOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_EAST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_EPSG_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_IMAGE_TYPE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_MAX_ZOOM_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_MIN_ZOOM_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_NAME_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_NORTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_SOUTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_P_WEST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTMSGENERATOR_STATUS;
import static org.hortonmachine.gears.modules.r.tmsgenerator.MBTilesHelper.TILESIZE;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIOException;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsUserCancelException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.images.ImageGenerator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

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

@Description(OMSTMSGENERATOR_DESCRIPTION)
@Documentation(OMSTMSGENERATOR_DOCUMENTATION)
@Author(name = OMSTMSGENERATOR_AUTHORNAMES, contact = OMSTMSGENERATOR_AUTHORCONTACTS)
@Keywords(OMSTMSGENERATOR_KEYWORDS)
@Label(OMSTMSGENERATOR_LABEL)
@Name(OMSTMSGENERATOR_NAME)
@Status(OMSTMSGENERATOR_STATUS)
@License(OMSTMSGENERATOR_LICENSE)
public class OmsTmsGenerator extends HMModel {

    @Description(OMSTMSGENERATOR_IN_RASTER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRasterFile = null;

    @Description(OMSTMSGENERATOR_IN_RASTER_BOUNDS_DESCRIPTION)
    @In
    public List<GridGeometry2D> inRasterBounds = null;

    @Description(OMSTMSGENERATOR_IN_VECTOR_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVectorFile = null;

    @Description(OMSTMSGENERATOR_IN_WMS_DESCRIPTION)
    @In
    public String inWMS = null;

    @Description(OMSTMSGENERATOR_P_NAME_DESCRIPTION)
    @In
    public String pName = "tmstiles";

    @Description(OMSTMSGENERATOR_P_MIN_ZOOM_DESCRIPTION)
    @In
    public Integer pMinzoom = null;

    @Description(OMSTMSGENERATOR_P_MAX_ZOOM_DESCRIPTION)
    @In
    public Integer pMaxzoom = null;

    @Description(OMSTMSGENERATOR_P_NORTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSTMSGENERATOR_P_SOUTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSTMSGENERATOR_P_WEST_DESCRIPTION)
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSTMSGENERATOR_P_EAST_DESCRIPTION)
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSTMSGENERATOR_P_EPSG_DESCRIPTION)
    @UI(HMConstants.CRS_UI_HINT)
    @In
    public String pEpsg;

    @Description("An optional prj file to use instead of the epsg code.")
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inPrj;

    @Description("A shapefile to use to draw maps on zoom levels higher than pZoomLimit. Everything outside is not drawn.")
    @In
    public String inZoomLimitVector;

    @Description("The zoom limit above which the inZoomLimitVector is considered.")
    @In
    public double pZoomLimit = 17;

    @Description(OMSTMSGENERATOR_DO_LENIENT_DESCRIPTION)
    @In
    public boolean doLenient = true;

    @Description(OMSTMSGENERATOR_P_IMAGE_TYPE_DESCRIPTION)
    @In
    public int pImagetype = 0;

    @Description(OMSTMSGENERATOR_P_CHECK_COLOR_DESCRIPTION)
    @In
    public int[] pCheckcolor = new int[]{255, 255, 255};

    @Description("Do mbtiles database.")
    @In
    public boolean doMbtiles = false;

    @Description(OMSTMSGENERATOR_IN_PATH_DESCRIPTION)
    @In
    public String inPath;

    private static final String EPSG_MERCATOR = "EPSG:3857";
    private static final String EPSG_LATLONG = "EPSG:4326";

    private PreparedGeometry zoomLimitGeometry;

    private MBTilesHelper mbtilesHelper;

    public CoordinateReferenceSystem dataCrs;

    private volatile boolean cancelModule = false;

    @Execute
    public void process() throws Exception {
        try {
            checkNull(inPath, pMinzoom, pMaxzoom, pWest, pEast, pSouth, pNorth);

            if (dataCrs == null) {
                if (pEpsg != null) {
                    dataCrs = CrsUtilities.getCrsFromEpsg(pEpsg, null);
                } else {
                    String wkt = FileUtilities.readFile(inPrj);
                    dataCrs = CRS.parseWKT(wkt);
                }
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
                mbtilesHelper.createTables(false);
                mbtilesHelper.fillMetadata(n, s, w, e, pName, format, pMinzoom, pMaxzoom);
            }

            int threads = getDefaultThreadsNum();

            String ext = "png";
            if (pImagetype == 1) {
                ext = "jpg";
            }

            checkCancel();
            List<String> inVectors = null;
            if (inVectorFile != null && new File(inVectorFile).exists())
                inVectors = FileUtilities.readFileToLinesList(new File(inVectorFile));

            checkCancel();
            List<String> inRasters = null;
            if (inRasterFile != null && new File(inRasterFile).exists())
                inRasters = FileUtilities.readFileToLinesList(new File(inRasterFile));

            if (inRasters == null && inVectors == null) {
                throw new ModelsIllegalargumentException("No raster and vector input maps available. check your inputs.", this,
                        pm);
            }

            if (dataCrs == null && pEpsg == null && inPrj == null) {
                throw new ModelsIllegalargumentException("No projection info available. check your inputs.", this, pm);
            }

            final CoordinateReferenceSystem mercatorCrs = CrsUtilities.getCrsFromEpsg(EPSG_MERCATOR, null);

            ReferencedEnvelope dataBounds = new ReferencedEnvelope(pWest, pEast, pSouth, pNorth, dataCrs);
            MathTransform data2MercatorTransform = CRS.findMathTransform(dataCrs, mercatorCrs);

            Envelope mercatorEnvelope = JTS.transform(dataBounds, data2MercatorTransform);
            ReferencedEnvelope mercatorBounds = new ReferencedEnvelope(mercatorEnvelope, mercatorCrs);

            checkCancel();
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

            final ImageGenerator imgGen = new ImageGenerator(pm, mercatorCrs);
            if (inWMS != null) {
                imgGen.setWMS(inWMS);
            }

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
                checkCancel();

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
                    checkCancel();
                    for( int j = startYTile; j <= endYTile; j++ ) {
                        checkCancel();
                        double[] bounds = mercator.TileBounds(i, j, z);
                        double west = bounds[0];
                        double south = bounds[1];
                        double east = bounds[2];
                        double north = bounds[3];

                        final ReferencedEnvelope tmpBounds = new ReferencedEnvelope(west, east, south, north, mercatorCrs);
                        levelBounds.expandToInclude(tmpBounds);

                        // if there is a zoom level geometry limitation, apply it
                        if (zoomLimitGeometry != null && z > pZoomLimit) {
                            double safeExtend = tmpBounds.getWidth() > tmpBounds.getHeight()
                                    ? tmpBounds.getWidth()
                                    : tmpBounds.getHeight();
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
                                    if (!cancelModule) {
                                        try {
                                            checkCancel();
                                            BufferedImage image = imgGen.getImageWithCheck(tmpBounds, TILESIZE, TILESIZE, 0.0,
                                                    pCheckcolor);
                                            if (image != null) {
                                                mbtilesHelper.addTile(x, y, zz, image, fformat);
                                            }
                                        } catch (Exception e) {
                                            pm.errorMessage(e.getMessage());
                                            cancelModule = true;
                                        }
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
                                    if (!cancelModule) {
                                        try {
                                            if (pImagetype == 1) {
                                                imgGen.dumpJpgImage(imagePath, finalBounds, TILESIZE, TILESIZE, 0.0, pCheckcolor);
                                            } else {
                                                imgGen.dumpPngImage(imagePath, finalBounds, TILESIZE, TILESIZE, 0.0, pCheckcolor);
                                            }
                                            pm.worked(1);
                                        } catch (Exception ex) {
                                            pm.errorMessage(ex.getMessage());
                                            cancelModule = true;
                                        }
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
                mbtilesHelper.createIndexes();
                mbtilesHelper.close();
            } else {
                CoordinateReferenceSystem latLongCrs = CrsUtilities.getCrsFromEpsg(EPSG_LATLONG, null);
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
        } catch (ModelsUserCancelException e) {
            pm.errorMessage(ModelsUserCancelException.DEFAULTMESSAGE);
        }
    }
}
