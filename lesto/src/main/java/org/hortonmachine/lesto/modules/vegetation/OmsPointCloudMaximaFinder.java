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
package org.hortonmachine.lesto.modules.vegetation;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.media.jai.iterator.RandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Description(OmsPointCloudMaximaFinder.DESCR)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsPointCloudMaximaFinder.KEYWORDS)
@Label(OmsPointCloudMaximaFinder.LABEL)
@Name("_" + OmsPointCloudMaximaFinder.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class OmsPointCloudMaximaFinder extends HMModel {

    @Description(inLas_DESCR)
    @In
    public List<LasRecord> inLas = null;

    @Description(inDtm_DESCR)
    @In
    public GridCoverage2D inDtm;

    @Description(inRoi_DESCR)
    @In
    public SimpleFeatureCollection inRoi;

    @Description(inDsmDtmDiff_DESCR)
    @In
    public GridCoverage2D inDsmDtmDiff;

    @Description(pMaxRadius_DESCR)
    @In
    public double pMaxRadius = -1.0;

    @Description(doDynamicRadius_DESCR)
    @In
    public boolean doDynamicRadius = true;

    @Description(pElevDiffThres_DESCR)
    @In
    public double pElevDiffThres = 3.5;

    @Description(outTops_DESCR)
    @In
    public SimpleFeatureCollection outTops = null;

    // VARS DOCS START
    public static final String outTops_DESCR = "The output local maxima.";
    public static final String pClass_DESCR = "The comma separated list of classes to filter (if empty, all are picked).";
    public static final String pThreshold_DESCR = "The elevation threshold to apply to the chm.";
    public static final String pElevDiffThres_DESCR = "Max permitted elevation difference around the maxima.";
    public static final String doDynamicRadius_DESCR = "Use an adaptive radius based on the height.";
    public static final String pMaxRadius_DESCR = "Radius for which a point can be local maxima.";
    public static final String inDsmDtmDiff_DESCR = "An optional dsm-dtm difference raster to use to check on the extracted tops.";
    public static final String inRoi_DESCR = "A set of polygons to use as region of interest.";
    public static final String inDtm_DESCR = "A dtm raster to use for the area of interest and to calculate the elevation threshold.";
    public static final String inLas_DESCR = "The input las.";
    public static final String NAME = "pointcloudmaximafinder";
    public static final String KEYWORDS = "Local maxima, las, lidar";
    public static final String DESCR = "Module that identifies local maxima in point clouds.";
    public static final String LABEL = HMConstants.LESTO + "/vegetation";
    // VARS DOCS END

    private AtomicInteger index = new AtomicInteger();

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        if (inRoi == null && inDtm == null) {
            throw new ModelsIllegalargumentException("At least one of raster or vector roi is necessary.", this);
        }

        CoordinateReferenceSystem crs = null;
        List<Geometry> regionGeometries = new ArrayList<Geometry>();
        if (inRoi != null) {
            regionGeometries = FeatureUtilities.featureCollectionToGeometriesList(inRoi, true, null);
            crs = inRoi.getBounds().getCoordinateReferenceSystem();
        } else {
            // use the dtm bounds
            Polygon polygon = CoverageUtilities.getRegionPolygon(inDtm);
            regionGeometries.add(polygon);
            crs = inDtm.getCoordinateReferenceSystem();
        }

        outTops = new DefaultFeatureCollection();
        SimpleFeatureBuilder lasBuilder = LasUtils.getLasFeatureBuilder(crs);

        DsmDtmDiffHelper helper = null;
        if (inDsmDtmDiff != null) {
            helper = new DsmDtmDiffHelper();
            helper.pElevDiffThres = pElevDiffThres;
            helper.gridGeometry = inDsmDtmDiff.getGridGeometry();
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDsmDtmDiff);
            helper.cols = regionMap.getCols();
            helper.rows = regionMap.getRows();
            helper.xres = regionMap.getXres();
            helper.yres = regionMap.getYres();
            helper.dsmDtmDiffIter = CoverageUtilities.getRandomIterator(inDsmDtmDiff);
            helper.novalue = HMConstants.getNovalue(inDsmDtmDiff);
        }

        try {
            doProcess(inLas, pMaxRadius, doDynamicRadius, helper, (DefaultFeatureCollection) outTops, lasBuilder, index, pm);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (helper != null)
            helper.dsmDtmDiffIter.done();

    }

    public static void doProcess( final List<LasRecord> pointsInTile, final double pMaxRadius, final boolean doDynamicRadius,
            final DsmDtmDiffHelper helper, final DefaultFeatureCollection outTopsFC, final SimpleFeatureBuilder lasBuilder,
            final AtomicInteger index, final IHMProgressMonitor pm ) throws Exception {
        /*
         * we use the intensity value to mark local maxima
         * - 1 = maxima
         * - 0 = non maxima
         */
        final GeometryFactory gf = new GeometryFactory();
        pm.beginTask("Mark local maxima...", pointsInTile.size());
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(getDefaultThreadsNum());
        for( final LasRecord currentDot : pointsInTile ) {
            Runnable runner = new Runnable(){
                public void run() {
                    try {
                        // check if it is a local maxima
                        boolean isLocalMaxima = true;
                        for( LasRecord tmpDot : pointsInTile ) {
                            double distance = LasUtils.distance(currentDot, tmpDot);
                            double maxRadius = pMaxRadius;
                            if (doDynamicRadius) {
                                // use Popescu lowered to 70% (Popescu & Kini 2004 for mixed pines
                                // and
                                // deciduous trees)
                                maxRadius = (2.51503 + 0.00901 * pow(currentDot.groundElevation, 2.0)) / 2.0 * 0.7;
                                if (maxRadius > pMaxRadius) {
                                    maxRadius = pMaxRadius;
                                }
                            }
                            if (distance > maxRadius) {
                                continue;
                            }
                            if (tmpDot.groundElevation > currentDot.groundElevation) {
                                // not local maxima
                                isLocalMaxima = false;
                                break;
                            }
                        }
                        // mark it
                        if (isLocalMaxima) {
                            if (helper != null) {
                                // check if it is some border or noise
                                GridCoordinates2D gridCoord = helper.gridGeometry
                                        .worldToGrid(new DirectPosition2D(currentDot.x, currentDot.y));
                                GridNode node = new GridNode(helper.dsmDtmDiffIter, helper.cols, helper.rows, helper.xres,
                                        helper.yres, gridCoord.x, gridCoord.y, helper.novalue);
                                double topElevation = node.elevation;
                                if (!node.isValid() || node.touchesBound()) {
                                    isLocalMaxima = false;
                                } else {
                                    List<GridNode> validSurroundingNodes = node.getValidSurroundingNodes();
                                    for( GridNode tmpNode : validSurroundingNodes ) {
                                        double tmpElevation = tmpNode.elevation;
                                        if (abs(topElevation - tmpElevation) > helper.pElevDiffThres) {
                                            isLocalMaxima = false;
                                        }
                                    }
                                }
                            }
                            if (isLocalMaxima) {
                                synchronized (lasBuilder) {
                                    final Point point = gf.createPoint(new Coordinate(currentDot.x, currentDot.y));
                                    double groundElevation = currentDot.groundElevation;
                                    // round to meter with 1 decimal
                                    groundElevation = ((int) round(groundElevation * 10)) / 10.0;
                                    final Object[] values = new Object[]{point, index.getAndIncrement(), groundElevation,
                                            currentDot.intensity, currentDot.classification, currentDot.returnNumber,
                                            currentDot.numberOfReturns};
                                    lasBuilder.addAll(values);
                                    final SimpleFeature feature = lasBuilder.buildFeature(null);
                                    outTopsFC.add(feature);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        pm.worked(1);
                    }
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

    static class DsmDtmDiffHelper {
        double pElevDiffThres;
        GridGeometry2D gridGeometry;
        RegionMap regionMap;
        int cols;
        int rows;
        double xres;
        double yres;
        RandomIter dsmDtmDiffIter;
        double novalue;
    }

}
