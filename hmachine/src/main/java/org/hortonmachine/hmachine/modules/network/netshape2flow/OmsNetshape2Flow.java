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
package org.hortonmachine.hmachine.modules.network.netshape2flow;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_fActive_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_fId_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_inGrid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_outFlownet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_outNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETSHAPE2FLOW_outProblems_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.MultiPoint;

@Description(OMSNETSHAPE2FLOW_DESCRIPTION)
@Author(name = OMSNETSHAPE2FLOW_AUTHORNAMES, contact = OMSNETSHAPE2FLOW_AUTHORCONTACTS)
@Keywords(OMSNETSHAPE2FLOW_KEYWORDS)
@Label(OMSNETSHAPE2FLOW_LABEL)
@Name(OMSNETSHAPE2FLOW_NAME)
@Status(OMSNETSHAPE2FLOW_STATUS)
@License(OMSNETSHAPE2FLOW_LICENSE)
public class OmsNetshape2Flow extends HMModel {

    @Description(OMSNETSHAPE2FLOW_inNet_DESCRIPTION)
    @In
    public SimpleFeatureCollection inNet = null;

    @Description(OMSNETSHAPE2FLOW_inGrid_DESCRIPTION)
    @In
    public GridGeometry2D inGrid = null;

    @Description(OMSNETSHAPE2FLOW_fActive_DESCRIPTION)
    @In
    public String fActive;

    @Description(OMSNETSHAPE2FLOW_fId_DESCRIPTION)
    @In
    public String fId;

    @Description(OMSNETSHAPE2FLOW_outFlownet_DESCRIPTION)
    @Out
    public GridCoverage2D outFlownet = null;

    @Description(OMSNETSHAPE2FLOW_outNet_DESCRIPTION)
    @Out
    public GridCoverage2D outNet = null;

    @Description(OMSNETSHAPE2FLOW_outProblems_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outProblems = null;

    private List<Coordinate> problemPointsList = new ArrayList<Coordinate>();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outNet == null, doReset)) {
            return;
        }
        checkNull(inNet, inGrid);
        HashMap<String, Double> regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(inGrid);
        double res = regionMap.get(CoverageUtilities.XRES);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        WritableRaster flowWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        WritableRaster netWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);

        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        WritableRandomIter netIter = RandomIterFactory.createWritable(netWR, null);

        int activeFieldPosition = -1;
        // if a field for active reach parts was passed
        SimpleFeatureType simpleFeatureType = inNet.getSchema();
        if (fActive != null) {
            activeFieldPosition = simpleFeatureType.indexOf(fActive);
        }
        int idFieldPosition = -1;
        if (fId != null) {
            idFieldPosition = simpleFeatureType.indexOf(fId);
        }
        int featureNum = inNet.size();
        FeatureIterator<SimpleFeature> featureIterator = inNet.features();

        pm.beginTask("Processing features...", featureNum);
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            // if the reach is not active, do not use it
            if (activeFieldPosition != -1) {
                String attr = (String) feature.getAttribute(activeFieldPosition);
                if (attr == null) {
                    // do what? Means to be active or not? For now it is dealt
                    // as active.
                } else if (attr.trim().substring(0, 1).equalsIgnoreCase("n")) { //$NON-NLS-1$
                    // reach is not active
                    continue;
                }
            }
            // find the id of the reach
            int id = -1;
            if (idFieldPosition == -1) {
                String[] idSplit = feature.getID().split("\\."); //$NON-NLS-1$
                id = Integer.parseInt(idSplit[idSplit.length - 1]);
            } else {
                Object attribute = feature.getAttribute(idFieldPosition);
                id = Integer.parseInt(String.valueOf(attribute));
            }
            // if the feature is active, start working on it
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coordinate[] coordinates = geometry.getCoordinates();
            if (coordinates.length < 2) {
                continue;
            }

            // boolean isLastCoordinateOfSegment = false;
            // boolean isSecondLastCoordinateOfSegment = false;

            Coordinate lastCoord = coordinates[coordinates.length - 1];

            GridCoordinates2D lastPointGC = inGrid.worldToGrid(new DirectPosition2D(lastCoord.x, lastCoord.y));
            int[] lastPoint = new int[]{lastPointGC.y, lastPointGC.x};
            for( int i = 0; i < coordinates.length - 1; i++ ) {
                // if (i == coordinates.length - 2) {
                // isLastCoordinateOfSegment = true;
                // }
                // if (i == coordinates.length - 3) {
                // isSecondLastCoordinateOfSegment = true;
                // }
                Coordinate first = coordinates[i];
                Coordinate second = coordinates[i + 1];

                LineSegment lineSegment = new LineSegment(first, second);
                double segmentLength = lineSegment.getLength();
                double runningLength = 0.0;
                while( runningLength <= segmentLength ) {
                    Coordinate firstPoint = lineSegment.pointAlong(runningLength / segmentLength);
                    // if the resolution is bigger than the length, use the
                    // length, i.e. 1
                    double perc = (runningLength + res) / segmentLength;
                    Coordinate secondPoint = lineSegment.pointAlong(perc > 1.0 ? 1.0 : perc);
                    GridCoordinates2D firstPointGC = inGrid.worldToGrid(new DirectPosition2D(firstPoint.x, firstPoint.y));
                    int[] firstOnRaster = new int[]{firstPointGC.y, firstPointGC.x};
                    GridCoordinates2D secondPointGC = inGrid.worldToGrid(new DirectPosition2D(secondPoint.x, secondPoint.y));
                    int[] secondOnRaster = new int[]{secondPointGC.y, secondPointGC.x};

                    /*
                     * if there is already a value in that point, and if the point in the output
                     * matrix is an outlet (10), then it is an end of another reach and it is ok to
                     * write over it. 
                     * If it is another value and my actual reach is not at the end,
                     * then jump over it, the outlet is threated after the loop. Let's create
                     * a temporary resource that contains all the problem points. The user will for
                     * now have to change the shapefile to proceed.
                     */
                    if (!isNovalue(flowIter.getSampleDouble(firstOnRaster[1], firstOnRaster[0], 0))
                            && flowIter.getSampleDouble(firstOnRaster[1], firstOnRaster[0], 0) != 10.0) {

                        if (i > coordinates.length - 3) {
                            runningLength = runningLength + res;
                            continue;
                        } else
                            problemPointsList.add(firstPoint);
                    }
                    /*
                     * if the two analized points are equal, the point will be added at the next
                     * round
                     */
                    if (Arrays.equals(firstOnRaster, secondOnRaster)) {
                        runningLength = runningLength + res;
                        continue;
                    }
                    // find the flowdirection between the point and the one
                    // after it
                    int rowDiff = secondOnRaster[0] - firstOnRaster[0];
                    int colDiff = secondOnRaster[1] - firstOnRaster[1];
                    int flowDirection = ModelsEngine.getFlowDirection(rowDiff, colDiff);

                    if (isNovalue(flowIter.getSampleDouble(firstOnRaster[1], firstOnRaster[0], 0))
                            || (lastPoint[0] != secondOnRaster[0] && lastPoint[1] != secondOnRaster[1])) {
                        flowIter.setSample(firstOnRaster[1], firstOnRaster[0], 0, flowDirection);
                    }

                    /* I have add this if statment in order to preserve the continuity of the main
                    * river.(first problem in the report)
                    */
                    if (isNovalue(netIter.getSampleDouble(firstOnRaster[1], firstOnRaster[0], 0))
                            || (lastPoint[0] != secondOnRaster[0] && lastPoint[1] != secondOnRaster[1])) {
                        netIter.setSample(firstOnRaster[1], firstOnRaster[0], 0, id);
                    }
                    // increment the distance
                    runningLength = runningLength + res;
                }
                /*
                 * note that the last coordinate is always threated when it is the first of the next
                 * segment. That is good, so we are able to threat the last coordinate of the reach
                 * differently.
                 */

            }
            Coordinate lastCoordinate = coordinates[coordinates.length - 1];
            GridCoordinates2D lastCoordinateGC = inGrid.worldToGrid(new DirectPosition2D(lastCoordinate.x, lastCoordinate.y));
            int[] lastOnRaster = new int[]{lastCoordinateGC.y, lastCoordinateGC.x};

            /*
             * the last is 10, but if there is already another value in the grid, then a major reach
             * has already put its value in it. If that is true, then we do not add this reach's
             * outlet, since it is just a confluence
             */
            if (isNovalue(flowIter.getSampleDouble(lastOnRaster[1], lastOnRaster[0], 0))) {
                flowIter.setSample(lastOnRaster[1], lastOnRaster[0], 0, 10.0);
                netIter.setSample(lastOnRaster[1], lastOnRaster[0], 0, id);
            }
            pm.worked(1);
        }
        pm.done();
        flowIter.done();
        netIter.done();

        CoordinateReferenceSystem crs = inNet.getSchema().getCoordinateReferenceSystem();
        outFlownet = CoverageUtilities.buildCoverage("flow", flowWR, regionMap, crs);
        outNet = CoverageUtilities.buildCoverage("networkd", netWR, regionMap, crs);

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        String typeName = "problemslayer";
        b.setName(typeName);
        b.setCRS(inNet.getSchema().getCoordinateReferenceSystem());
        b.add("the_geom", MultiPoint.class);
        b.add("cat", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        GeometryFactory gf = GeometryUtilities.gf();
        outProblems = new DefaultFeatureCollection();
        for( int i = 0; i < problemPointsList.size(); i++ ) {
            MultiPoint mPoint = gf.createMultiPoint(new Coordinate[]{problemPointsList.get(i)});
            Object[] values = new Object[]{mPoint, i};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            ((DefaultFeatureCollection) outProblems).add(feature);
        }

    }

}
