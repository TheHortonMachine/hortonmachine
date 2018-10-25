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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.debrisvandre;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.distance3d;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_doWholenet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inObstacles_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inSoil_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inTriggers_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_outIndexedTriggers_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_outPaths_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_outSoil_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_pDistance_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_pMode_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.StringUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

@Description(OMSDEBRISVANDRE_DESCRIPTION)
@Author(name = OMSDEBRISVANDRE_AUTHORNAMES, contact = OMSDEBRISVANDRE_AUTHORCONTACTS)
@Keywords(OMSDEBRISVANDRE_KEYWORDS)
@Label(OMSDEBRISVANDRE_LABEL)
@Name(OMSDEBRISVANDRE_NAME)
@Status(OMSDEBRISVANDRE_STATUS)
@License(OMSDEBRISVANDRE_LICENSE)
public class OmsDebrisVandre extends HMModel {

    @Description(OMSDEBRISVANDRE_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSDEBRISVANDRE_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSDEBRISVANDRE_inSlope_DESCRIPTION)
    @Unit("degree")
    @In
    public GridCoverage2D inSlope = null;

    @Description(OMSDEBRISVANDRE_inTriggers_DESCRIPTION)
    @In
    public GridCoverage2D inTriggers = null;

    @Description(OMSDEBRISVANDRE_inSoil_DESCRIPTION)
    @In
    public GridCoverage2D inSoil = null;

    @Description(OMSDEBRISVANDRE_inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(OMSDEBRISVANDRE_doWholenet_DESCRIPTION)
    @In
    public boolean doWholenet = false;

    @Description(OMSDEBRISVANDRE_pDistance_DESCRIPTION)
    @In
    @Unit("m")
    public double pDistance = 100.0;

    @Description(OMSDEBRISVANDRE_inObstacles_DESCRIPTION)
    @In
    public SimpleFeatureCollection inObstacles = null;

    @Description(OMSDEBRISVANDRE_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSDEBRISVANDRE_outPaths_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outPaths = null;

    @Description(OMSDEBRISVANDRE_outIndexedTriggers_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outIndexedTriggers = null;

    @Description(OMSDEBRISVANDRE_outSoil_DESCRIPTION)
    @Out
    public GridCoverage2D outSoil = null;

    /**
     * Alpha value of the Vandre equation: W = alpha*deltaElev.
     */
    private double alphaVandre = 0.4;

    private double minDegreesBurton = 4.0;
    private double toggleDegreesBurton = 10.0;

    private double minDegreesModifiedBurton = Double.NEGATIVE_INFINITY;
    private double toggleDegreesModifiedBurton = 8.0;

    private double minDegrees = -1.0;
    private double toggleDegrees = -1.0;

    private GeometryFactory gf = GeometryUtilities.gf();

    private TreeSet<String> processedtriggersMap = new TreeSet<String>();

    private List<java.awt.Point> obstaclesSet = new ArrayList<java.awt.Point>();
    private boolean useObstacles = false;

    /**
     * Check if the processing passed through the condition to be between slopes.
     * 
     * If after that condition, the slope gets greater than the angle again
     * the delta elevation of the Vandre equation has to be reset to 0.
     */
    private boolean wasBetweenSlopes = false;

    private int cols;

    private int rows;

    private WritableRaster outSoilWR;

    private WritableRandomIter outSoilIter;

    private RandomIter soilIter;
    private RandomIter netIter;

    private double xRes;

    private double yRes;

    private WritableRandomIter flowIter;

    @Execute
    public void process() throws Exception {
        checkNull(inFlow, inTriggers, inSlope);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.getCols();
        rows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();

        if (inSoil != null) {
            if (inNet == null) {
                throw new ModelsIllegalargumentException("If the soil map is supplied also the network map is needed.", this, pm);
            }
            outSoilWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
            outSoilIter = RandomIterFactory.createWritable(outSoilWR, null);

            RenderedImage soilRI = inSoil.getRenderedImage();
            soilIter = RandomIterFactory.create(soilRI, null);

            RenderedImage netRI = inNet.getRenderedImage();
            netIter = RandomIterFactory.create(netRI, null);
        }
        switch( pMode ) {
        case 1:
            minDegrees = minDegreesModifiedBurton;
            toggleDegrees = toggleDegreesModifiedBurton;
            break;
        case 0:
        default:
            minDegrees = minDegreesBurton;
            toggleDegrees = toggleDegreesBurton;
            break;
        }

        GridGeometry2D gridGeometry = inFlow.getGridGeometry();

        if (inObstacles != null) {
            List<Geometry> obstacleGeometries = FeatureUtilities.featureCollectionToGeometriesList(inObstacles, false, null);

            for( Geometry geometry : obstacleGeometries ) {
                java.awt.Point p = new java.awt.Point();
                CoverageUtilities.colRowFromCoordinate(geometry.getCoordinate(), gridGeometry, p);
                obstaclesSet.add(p);
            }
            useObstacles = true;
        }

        RenderedImage elevRI = inElev.getRenderedImage();
        RandomIter elevIter = RandomIterFactory.create(elevRI, null);

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, false);
        flowIter = RandomIterFactory.createWritable(flowWR, null);

        RenderedImage triggerRI = inTriggers.getRenderedImage();
        RandomIter triggerIter = RandomIterFactory.create(triggerRI, null);

        RenderedImage slopeRI = inSlope.getRenderedImage();
        RandomIter slopeIter = RandomIterFactory.create(slopeRI, null);

        outPaths = new DefaultFeatureCollection();
        outIndexedTriggers = new DefaultFeatureCollection();

        SimpleFeatureType triggersType = createTriggersType();
        SimpleFeatureType pathsType = createPathType();

        int featureIndex = 0;

        /*
         * FIXME the common paths are extracted many times, not good
         */
        pm.beginTask("Extracting paths...", cols);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double netflowValue = flowIter.getSampleDouble(c, r, 0);
                if (isNovalue(netflowValue)) {
                    continue;
                }
                if (ModelsEngine.isSourcePixel(flowIter, c, r)) {
                    // pm.message("NEW SOURCE: " + c + "/" + r);
                    // start navigating down until you find a debris trigger
                    int[] flowDirColRow = new int[]{c, r};
                    if (!moveToNextTriggerpoint(triggerIter, flowIter, flowDirColRow)) {
                        // we reached the exit
                        continue;
                    }

                    int triggerCol = flowDirColRow[0];
                    int triggerRow = flowDirColRow[1];

                    /*
                     * analyze for this trigger, after that, continue with the next one down
                     */
                    double flowValue = 0;
                    do {
                        /*
                         * check if this trigger was already processed, wich can happen if the same path is run
                         * after a confluence
                         */
                        String triggerId = StringUtilities.joinStrings(null, String.valueOf(triggerCol), "_",
                                String.valueOf(triggerRow));
                        if (processedtriggersMap.add(triggerId)) {
                            // trigger point has never been touched, process it

                            // pm.message(StringUtilities.joinStrings(null, "TRIGGER: ",
                            // String.valueOf(triggerCol), "/",
                            // String.valueOf(triggerRow)));
                            List<Coordinate> pathCoordinates = new ArrayList<Coordinate>();
                            /*
                             * the pathCondition defines if the current point (pathCoordinates)
                             * contributes mass to the defined path. 
                             */
                            List<Boolean> isBetweenSlopesCondition = new ArrayList<Boolean>();
                            Coordinate triggerCoord = CoverageUtilities.coordinateFromColRow(flowDirColRow[0], flowDirColRow[1],
                                    gridGeometry);
                            double elevationValue = elevIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                            flowValue = flowIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                            triggerCoord.z = elevationValue;
                            pathCoordinates.add(triggerCoord);
                            isBetweenSlopesCondition.add(false);

                            /*
                             * found a trigger, start recording and analizing, once created 
                             */
                            double slopeValue;
                            double triggerValue;

                            double lengthWithDegreeLessThanTogglePoint = 0;
                            double deltaElevWithDegreeLessThanTogglePoint = 0;
                            wasBetweenSlopes = false;
                            boolean isMoving = true; // in the triggerpoint we assume it is moving
                            while( isMoving ) {
                                // go one down
                                if (!ModelsEngine.go_downstream(flowDirColRow, flowValue))
                                    throw new ModelsIllegalargumentException(
                                            "Unable to go downstream. There might be problems in the consistency of your data.",
                                            this, pm);

                                if (useObstacles) {
                                    /*
                                     * if we land on a point in which an obstacle stops the path, 
                                     * add the point and stop the path geometry.
                                     */
                                    java.awt.Point currentPoint = new java.awt.Point(flowDirColRow[0], flowDirColRow[1]);
                                    if (obstaclesSet.contains(currentPoint)) {
                                        pm.message("Found obstacle in " + currentPoint.x + "/" + currentPoint.y);
                                        // we are passing an obstacle
                                        break;
                                    }
                                }

                                elevationValue = elevIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                                slopeValue = slopeIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                                triggerValue = triggerIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                                flowValue = flowIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                                if (flowValue == 10) {
                                    break;
                                }

                                // pm.message("---> " + flowDirColRow[0] + "/" + flowDirColRow[1]);

                                /*
                                 * add the current coordinate to the path
                                 */
                                Coordinate tmpCoord = CoverageUtilities.coordinateFromColRow(flowDirColRow[0], flowDirColRow[1],
                                        gridGeometry);
                                tmpCoord.z = elevationValue;
                                pathCoordinates.add(tmpCoord);
                                int size = pathCoordinates.size();
                                Coordinate c1 = pathCoordinates.get(size - 1);
                                Coordinate c2 = pathCoordinates.get(size - 2);

                                // length is calculated when slowing down starts (between slopes)
                                if (wasBetweenSlopes) {
                                    lengthWithDegreeLessThanTogglePoint = lengthWithDegreeLessThanTogglePoint
                                            + distance3d(c1, c2, null);
                                    isBetweenSlopesCondition.add(true);
                                    // when between slopes, delta elev is constant
                                } else {
                                    deltaElevWithDegreeLessThanTogglePoint = deltaElevWithDegreeLessThanTogglePoint
                                            + Math.abs(c1.z - c2.z);
                                    isBetweenSlopesCondition.add(false);
                                }

                                /*
                                 * and check if we will move on
                                 */
                                if (!isNovalue(triggerValue) || slopeValue >= toggleDegrees) {
                                    /*
                                     * in the cases in which we:
                                     *  - have a trigger value
                                     *  - the slope is major than the toggleDegrees
                                     *  
                                     *  => we move
                                     */
                                    isMoving = true;
                                    if (wasBetweenSlopes) {
                                        /*
                                         * if we came into this condition from being between the slopes
                                         * we have to reset the elevation delta used by Vandre, since
                                         * the debris will get speed again. 
                                         */
                                        deltaElevWithDegreeLessThanTogglePoint = 0.0;
                                        lengthWithDegreeLessThanTogglePoint = 0.0;
                                        wasBetweenSlopes = false;
                                        // pm.message("--> RE-ENTERING IN STEEP PART AFTER BETWEEN
                                        // SLOPES CONDITION");
                                    }
                                } else if (slopeValue <= minDegrees) {
                                    /*
                                     * supposed to be stopped if below minDegrees. We add the coordinate
                                     * which will be the last.
                                     */
                                    isMoving = false;
                                } else if (slopeValue > minDegrees && slopeValue < toggleDegrees) {
                                    /*
                                     * in this case we need to check on the base of the Vandre equation 
                                     * with the chosen criterias
                                     */
                                    double w = alphaVandre * deltaElevWithDegreeLessThanTogglePoint;
                                    if (lengthWithDegreeLessThanTogglePoint > w) {
                                        // debris stops
                                        isMoving = false;
                                    } else {
                                        isMoving = true;
                                        wasBetweenSlopes = true;
                                    }
                                } else if (isNovalue(elevationValue) || isNovalue(slopeValue) || isNovalue(triggerValue)
                                        || isNovalue(flowValue)) {
                                    if (isNovalue(elevationValue)) {
                                        pm.errorMessage("Found an elevation novalue along the way");
                                    }
                                    if (isNovalue(slopeValue)) {
                                        pm.errorMessage("Found a slope novalue along the way");
                                    }
                                    if (isNovalue(triggerValue)) {
                                        pm.errorMessage("Found a trigger novalue along the way");
                                    }
                                    if (isNovalue(flowValue)) {
                                        pm.errorMessage("Found a flow novalue along the way");
                                    }
                                    isMoving = false;
                                } else {
                                    throw new RuntimeException();
                                }
                            } // while isMoving

                            /*
                             * create the trigger and linked path geometry 
                             */
                            if (pathCoordinates.size() > 2) {
                                Point triggerPoint = gf.createPoint(pathCoordinates.get(0));
                                LineString pathLine = gf.createLineString(pathCoordinates.toArray(new Coordinate[0]));

                                if (inSoil != null) {
                                    cumulateSoil(pathCoordinates, isBetweenSlopesCondition);
                                }

                                int id = featureIndex;

                                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(pathsType);
                                Object[] values = new Object[]{pathLine, id};
                                builder.addAll(values);
                                SimpleFeature pathFeature = builder.buildFeature(null);
                                ((DefaultFeatureCollection) outPaths).add(pathFeature);

                                builder = new SimpleFeatureBuilder(triggersType);
                                values = new Object[]{triggerPoint, id};
                                builder.addAll(values);
                                SimpleFeature triggerFeature = builder.buildFeature(null);
                                ((DefaultFeatureCollection) outIndexedTriggers).add(triggerFeature);

                                featureIndex++;
                            }

                        }

                        /*
                         * we have to go back and start again from after the trigger that 
                         * created the previous calculus 
                         */
                        flowDirColRow[0] = triggerCol;
                        flowDirColRow[1] = triggerRow;
                        // search for the next trigger
                        if (!moveToNextTriggerpoint(triggerIter, flowIter, flowDirColRow)) {
                            // we reached the exit
                            break;
                        }
                        triggerCol = flowDirColRow[0];
                        triggerRow = flowDirColRow[1];
                        flowValue = flowIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                    } while( flowValue != 10 );

                } // isSource

            }
            pm.worked(1);
        }
        pm.done();

        if (inSoil != null) {
            /*
             * make volume
             */
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = outSoilIter.getSampleDouble(c, r, 0);
                    if (isNovalue(value)) {
                        continue;
                    }
                    value = value * xRes * yRes;
                    outSoilIter.setSample(c, r, 0, value);
                }
            }
            outSoil = CoverageUtilities.buildCoverage("cumulatedsoil", outSoilWR, regionMap,
                    inSoil.getCoordinateReferenceSystem());
        }
    }

    /**
     * Calculate the soil cumulation map along the paths.
     * 
     * @param pathCoordinates the coordinates of the path to be cumulated.
     * @param isBetweenSlopesCondition conditions defining in which points the path does
     *                  contribute mass.
     */
    private void cumulateSoil( List<Coordinate> pathCoordinates, List<Boolean> isBetweenSlopesCondition ) {
        // pm.beginTask("Cumulate Soil...", pathCoordinates.size());

        java.awt.Point point = new java.awt.Point();
        GridGeometry2D gridGeometry = inSoil.getGridGeometry();

        /*
         * check if the trigger is too far to reach the network.
         */
        double distance = 0.0;
        Coordinate previousCoordinate = null;
        for( int i = 0; i < pathCoordinates.size(); i++ ) {
            Coordinate coordinate = pathCoordinates.get(i);
            CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, point);
            double net = netIter.getSampleDouble(point.x, point.y, 0);

            if (previousCoordinate != null) {
                distance = distance + coordinate.distance(previousCoordinate);
            }
            previousCoordinate = coordinate;

            if (distance > pDistance) {
                // if distance is too large, the path can be discarded
                return;
            }

            if (!isNovalue(net)) {
                // if path gets into the network, it has to be considered
                break;
            }
        }

        /*
         * do the actual cumulation map
         */
        double previousCumulated = 0.0;
        for( int i = 0; i < pathCoordinates.size(); i++ ) {
            Coordinate coordinate = pathCoordinates.get(i);
            CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, point);

            boolean isBetweenSlopes = isBetweenSlopesCondition.get(i);

            double soil = soilIter.getSampleDouble(point.x, point.y, 0);
            if (isNovalue(soil)) {
                throw new ModelsIllegalargumentException("The soil map needs to cover the whole paths.", this, pm);
            }
            double net = netIter.getSampleDouble(point.x, point.y, 0);
            if (!isNovalue(net)) {
                isBetweenSlopes = true;
            }

            double cumulated = outSoilIter.getSampleDouble(point.x, point.y, 0);

            if (isBetweenSlopes) {
                if (isNovalue(cumulated)) {
                    outSoilIter.setSample(point.x, point.y, 0, previousCumulated);
                } else {
                    double newCumulated = cumulated + previousCumulated;
                    outSoilIter.setSample(point.x, point.y, 0, newCumulated);
                }
            } else {
                if (isNovalue(cumulated)) {
                    double newCumulated = soil + previousCumulated;
                    outSoilIter.setSample(point.x, point.y, 0, newCumulated);
                    previousCumulated = newCumulated;
                } else {
                    double newCumulated = cumulated + previousCumulated;
                    outSoilIter.setSample(point.x, point.y, 0, newCumulated);
                }
            }

        }

        if (doWholenet) {
            Coordinate lastCoordinate = pathCoordinates.get(pathCoordinates.size() - 1);
            CoverageUtilities.colRowFromCoordinate(lastCoordinate, gridGeometry, point);

            int[] flowDirColRow = new int[]{point.x, point.y};

            double net = netIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
            double tmpFlowValue = flowIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);

            // handle the last point of the path only if it was inside the net...
            if (!isNovalue(net)) {
                // first move to the next point
                if (!ModelsEngine.go_downstream(flowDirColRow, tmpFlowValue))
                    throw new ModelsIllegalargumentException(
                            "Unable to go downstream [col/row]: " + flowDirColRow[0] + "/" + flowDirColRow[1], this, pm);
                tmpFlowValue = flowIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                while( !isNovalue(tmpFlowValue) && tmpFlowValue != 10 ) {
                    double cumulated = outSoilIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                    if (isNovalue(cumulated)) {
                        cumulated = 0.0;
                    }
                    double newCumulated = cumulated + previousCumulated;
                    outSoilIter.setSample(flowDirColRow[0], flowDirColRow[1], 0, newCumulated);

                    if (!ModelsEngine.go_downstream(flowDirColRow, tmpFlowValue))
                        throw new ModelsIllegalargumentException(
                                "Unable to go downstream [col/row]: " + flowDirColRow[0] + "/" + flowDirColRow[1], this, pm);
                    tmpFlowValue = flowIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
                }

            }

        }

    }
    /**
     * Moves the flowDirColRow variable to the next trigger point.
     * 
     * @param triggerIter
     * @param flowDirColRow
     * @return <code>true</code> if a new trigger was found, <code>false</code> if the exit was reached.
     */
    private boolean moveToNextTriggerpoint( RandomIter triggerIter, RandomIter flowIter, int[] flowDirColRow ) {
        double tmpFlowValue = flowIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
        if (tmpFlowValue == 10) {
            return false;
        }
        if (!ModelsEngine.go_downstream(flowDirColRow, tmpFlowValue))
            throw new ModelsIllegalargumentException("Unable to go downstream: " + flowDirColRow[0] + "/" + flowDirColRow[1],
                    this, pm);
        while( isNovalue(triggerIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0)) ) {
            tmpFlowValue = flowIter.getSampleDouble(flowDirColRow[0], flowDirColRow[1], 0);
            if (tmpFlowValue == 10) {
                return false;
            }
            if (!ModelsEngine.go_downstream(flowDirColRow, tmpFlowValue))
                throw new ModelsIllegalargumentException("Unable to go downstream: " + flowDirColRow[0] + "/" + flowDirColRow[1],
                        this, pm);
        }
        return true;
    }

    private SimpleFeatureType createTriggersType() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("indexedtriggers");
        b.setCRS(inFlow.getCoordinateReferenceSystem());
        b.add("the_geom", Point.class);
        b.add("PATHID", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        return type;
    }

    private SimpleFeatureType createPathType() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("debrispaths");
        b.setCRS(inFlow.getCoordinateReferenceSystem());
        b.add("the_geom", LineString.class);
        b.add("ID", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        return type;
    }

}
