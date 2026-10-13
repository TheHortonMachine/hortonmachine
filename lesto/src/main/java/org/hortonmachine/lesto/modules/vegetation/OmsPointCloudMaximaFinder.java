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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.imagen.iterator.RandomIter;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.Position2D;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.lesto.modules.vegetation.rastermaxima.OmsRasterMaximaFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@Description(OmsPointCloudMaximaFinder.DESCR)
@Documentation(OmsPointCloudMaximaFinder.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsPointCloudMaximaFinder.KEYWORDS)
@Label(OmsPointCloudMaximaFinder.LABEL)
@Name("_" + OmsPointCloudMaximaFinder.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@Bibliography(OmsRasterMaximaFinder.BIBLIOGRAPHY_POPESCU_WYNNE_2004)
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
    @Out
    public SimpleFeatureCollection outTops = null;

    // VARS DOCS START
    public static final String outTops_DESCR = "The output local maxima.";
    public static final String pClass_DESCR = "The comma separated list of classes to filter (if empty, all are picked).";
    public static final String pThreshold_DESCR = "The height over the ground up to which the points are left out.";
    public static final String pElevDiffThres_DESCR = "With the dsm-dtm difference, the largest difference of a top from the cells around it.";
    public static final String doDynamicRadius_DESCR = "Use an adaptive radius based on the height.";
    public static final String pMaxRadius_DESCR = "The radius within which a point has to be the highest to be a maximum. With the dynamic radius it is its upper limit, ignored if not larger than 0.";
    public static final String inDsmDtmDiff_DESCR = "An optional dsm-dtm difference raster to use to check on the extracted tops.";
    public static final String inRoi_DESCR = "A set of polygons to use as region of interest.";
    public static final String inDtm_DESCR = "The dtm raster that gives the height of the points over the ground, and the area of interest if no roi is given.";
    public static final String inLas_DESCR = "The lidar data: a las file, the index file of a folder of las files created by LasIndexer or a las database.";
    public static final String NAME = "pointcloudmaximafinder";
    public static final String KEYWORDS = "Local maxima, las, lidar";
    public static final String DESCR = "Finds the tops of the trees as the points of a lidar survey higher than all the others around them.";
    public static final String LABEL = HMConstants.LESTO + "/vegetation";
    public static final String DOCUMENTATION = """
            The points are read with their height over the elevation model: those outside of it, or not higher than the threshold over the ground, are left out, as are those of the classes not listed, if any. A point is a top when no other point within the radius on the plane is higher; points of the same height within the radius are all tops.

            With the dynamic radius, the radius grows with the height H of the point as half the crown width of mixed pines and deciduous trees of Popescu and Wynne (2004), CW = 2.51503 + 0.00901 H², lowered to 70%, as the crowns overlap: about 1 m at 5 m of height and 2.1 m at 20 m, never larger than the given radius if this is larger than 0. Without it the radius is the given one. The points must be denser than the radius: where they are farther apart than it, every point is a top.

            With the regions of interest, each polygon, without its holes, is processed by itself, comparing only its points: a top near its border may have a higher point just outside. Without them the area is the one of the elevation model. Each point is compared with all the others of its area, so the time grows with the square of their number: large surveys are best processed in regions of a few hectares.

            With the difference of the surface and the elevation model, a top is discarded if its cell has no value, is on the border of the raster, or if any of its neighbours differs from it by more than the elevation difference threshold, as the isolated points over the canopy.

            The tops keep the attributes of their point: the height over the ground (elev, rounded to 10 cm), intensity, classification and return.
            """;
    // VARS DOCS END

    private AtomicInteger index = new AtomicInteger();

    @Execute
    public void process() throws Exception {
        checkNull(inLas);

        if (inRoi == null && inDtm == null) {
            throw new ModelsIllegalargumentException("At least one of raster or vector roi is necessary.", this);
        }
        if (!doDynamicRadius && pMaxRadius <= 0) {
            throw new ModelsIllegalargumentException("Without the dynamic radius, the radius has to be larger than 0.", this);
        }

        CoordinateReferenceSystem crs = null;
        if (inRoi != null) {
            crs = inRoi.getBounds().getCoordinateReferenceSystem();
        } else {
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
                        double maxRadius = pMaxRadius;
                        if (doDynamicRadius) {
                            // half the crown width of Popescu & Wynne 2004 for mixed pines and
                            // deciduous trees, lowered to 70%
                            maxRadius = (2.51503 + 0.00901 * pow(currentDot.groundElevation, 2.0)) / 2.0 * 0.7;
                            if (pMaxRadius > 0 && maxRadius > pMaxRadius) {
                                maxRadius = pMaxRadius;
                            }
                        }
                        for( LasRecord tmpDot : pointsInTile ) {
                            double distance = LasUtils.distance(currentDot, tmpDot);
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
                                        .worldToGrid(new Position2D(currentDot.x, currentDot.y));
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
