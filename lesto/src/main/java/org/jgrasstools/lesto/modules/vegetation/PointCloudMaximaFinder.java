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
package org.jgrasstools.lesto.modules.vegetation;

import static java.lang.Math.pow;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.jgrasstools.gears.io.las.ALasDataManager;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.utils.LasUtils;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.StringUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

@Description("Module that identifies local maxima in point clouds.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("Local maxima, las, lidar")
@Label(JGTConstants.LESTO + "/vegetation")
@Name("pointcloudmaximafinder")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class PointCloudMaximaFinder extends JGTModel {

    @Description("Input las file.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inLas = null;

    @Description("A dtm raster to use for the area of interest and to calculate the elevation threshold.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDtm;

    @Description("A set of polygons to use as region of interest.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRoi;

    @Description("Radius for which a point can be local maxima.")
    @In
    public double pMaxRadius = -1.0;

    @Description("Use an adaptive radius based on the height.")
    @In
    public boolean doDynamicRadius = true;

    @Description("The elevation threshold to apply to the chm.")
    @In
    public double pThreshold = 0.0;

    @Description("The comma separated list of classes to filter (if empty, all are picked).")
    @In
    public String pClass = null;

    @Description("The output local maxima.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outTops = null;

    private SimpleFeatureBuilder lasBuilder;

    private DefaultFeatureCollection outTopsFC;

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        if (inRoi == null && inDtm == null) {
            throw new ModelsIllegalargumentException("At least one of raster or vector roi is necessary.", this);
        }
        GridCoverage2D inDtmGC = null;
        CoordinateReferenceSystem crs = null;
        List<Geometry> regionGeometries = new ArrayList<Geometry>();
        if (inRoi != null) {
            SimpleFeatureCollection inRoiFC = getVector(inRoi);
            regionGeometries = FeatureUtilities.featureCollectionToGeometriesList(inRoiFC, true, null);
            crs = inRoiFC.getBounds().getCoordinateReferenceSystem();
        } else {
            // use the dtm bounds
            inDtmGC = getRaster(inDtm);
            Polygon polygon = CoverageUtilities.getRegionPolygon(inDtmGC);
            regionGeometries.add(polygon);
            crs = inDtmGC.getCoordinateReferenceSystem();
        }

        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLas), inDtmGC, pThreshold, crs)) {
            lasData.open();
            if (pClass != null) {
                double[] classes = StringUtilities.stringToDoubleArray(pClass, ",");
                lasData.setClassesConstraint(classes);
            }

            outTopsFC = new DefaultFeatureCollection();
            lasBuilder = LasUtils.getLasFeatureBuilder(crs);

            final int roiNum = regionGeometries.size();
            int index = 1;
            for( final Geometry regionGeometry : regionGeometries ) {
                StringBuilder sb = new StringBuilder();
                sb.append("\nProcessing geometry N.");
                sb.append(index);
                sb.append(" of ");
                sb.append(roiNum);
                sb.append("\n");
                pm.message(sb.toString());
                // remove holes
                LineString exteriorRing = ((Polygon) regionGeometry).getExteriorRing();
                final Polygon regionPolygon = gf.createPolygon(gf.createLinearRing(exteriorRing.getCoordinates()));
                List<LasRecord> pointsInTile = lasData.getPointsInGeometry(regionPolygon, false);
                final int size = pointsInTile.size();
                if (size == 0) {
                    pm.errorMessage("No points processed in tile: " + regionPolygon);
                    continue;
                }
                try {
                    doProcess(pointsInTile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            dumpVector(outTopsFC, outTops);
        }
    }

    private void doProcess( final List<LasRecord> pointsInTile ) throws Exception {
        /*
         * we use the intensity value to mark local maxima
         * - 1 = maxima
         * - 0 = non maxima
         */
        pm.beginTask("Mark local maxima...", pointsInTile.size());
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(getDefaultThreadsNum());
        for( final LasRecord r : pointsInTile ) {
            Runnable runner = new Runnable(){
                public void run() {
                    // check if it is a local maxima
                    boolean isLocalMaxima = true;
                    for( LasRecord tmpRecord : pointsInTile ) {
                        double distance = LasUtils.distance(r, tmpRecord);
                        double maxRadius = pMaxRadius;
                        if (doDynamicRadius) {
                            // use Popescu lowered to 70% (Popescu & Kini 2004 for mixed pines and
                            // deciduous trees)
                            maxRadius = (2.51503 + 0.00901 * pow(r.z, 2.0)) / 2.0 * 0.7;
                            if (maxRadius > pMaxRadius) {
                                maxRadius = pMaxRadius;
                            }
                        }
                        if (distance > maxRadius) {
                            continue;
                        }
                        if (tmpRecord.z > r.z) {
                            // not local maxima
                            isLocalMaxima = false;
                            break;
                        }
                    }
                    // mark it
                    if (isLocalMaxima) {
                        synchronized (lasBuilder) {
                            final Point point = gf.createPoint(new Coordinate(r.x, r.y));
                            final Object[] values = new Object[]{point, r.z, r.intensity, r.classification, r.returnNumber,
                                    r.numberOfReturns};
                            lasBuilder.addAll(values);
                            final SimpleFeature feature = lasBuilder.buildFeature(null);
                            outTopsFC.add(feature);
                        }
                    }
                    pm.worked(1);
                }
            };
            fixedThreadPool.execute(runner);
        }
        try {
            fixedThreadPool.shutdown();
            fixedThreadPool.awaitTermination(30, TimeUnit.DAYS);
            fixedThreadPool.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pm.done();

    }

}
