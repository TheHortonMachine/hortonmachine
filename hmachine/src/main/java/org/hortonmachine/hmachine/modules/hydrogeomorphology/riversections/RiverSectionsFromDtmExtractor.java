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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.riversections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.coverage.ProfilePoint;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;

/**
 * An extractor of river sections from dtm.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
public class RiverSectionsFromDtmExtractor extends ARiverSectionsExtractor {

    /**
     * Extracts sections on the dtm from a riverline at regular intervals.
     * 
     * @param riverLine the river line to consider for the cross sections extraction.
     *          <b>The river line has to be oriented from upstream to downstream.</b>
     * @param elevation the elevation {@link GridCoverage2D}.
     * @param sectionsInterval the distance to use between extracted sections. 
     * @param sectionsWidth the width of the extracted sections.
     * @param bridgePoints the list of bridge {@link Point}s. 
     * @param bridgeWidthAttribute the name of the attribute in the bridges feature that defines the width of the bridge.
     * @param bridgeBuffer a buffer to use for the bridge inside which the bridge is considered to be on the river.
     * @param monitor the progress monitor.
     * @throws Exception
     */
    public RiverSectionsFromDtmExtractor( //
            LineString riverLine, //
            GridCoverage2D elevation, //
            double sectionsInterval, //
            double sectionsWidth, //
            List<FeatureMate> bridgePoints, //
            String bridgeWidthAttribute, //
            double bridgeBuffer, //
            IHMProgressMonitor monitor //
    ) throws Exception {
        crs = elevation.getCoordinateReferenceSystem();

        RandomIter elevIter = CoverageUtilities.getRandomIterator(elevation);
        GridGeometry2D gridGeometry = elevation.getGridGeometry();
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(elevation);
        Envelope envelope = regionMap.toEnvelope();

        riverPointsList = new ArrayList<RiverPoint>();
        LengthIndexedLine indexedLine = new LengthIndexedLine(riverLine);

        double length = riverLine.getLength();
        int totalWork = (int) (length / sectionsInterval);
        monitor.beginTask("Extracting sections...", totalWork);
        double runningLength = 0;
        while( runningLength <= length ) {
            // important to extract from left to right

            // TODO extract with point before and after in order to have more regular sections
            Coordinate leftPoint = indexedLine.extractPoint(runningLength, sectionsWidth);
            Coordinate rightPoint = indexedLine.extractPoint(runningLength, -sectionsWidth);
            if (envelope.intersects(leftPoint) && envelope.intersects(rightPoint)) {
                RiverPoint netPoint = getNetworkPoint(riverLine, elevIter, gridGeometry, runningLength, null, leftPoint,
                        rightPoint);
                if (netPoint != null)
                    riverPointsList.add(netPoint);
            }
            runningLength = runningLength + sectionsInterval;
            monitor.worked(1);
        }
        monitor.done();

        process(riverLine, sectionsWidth, bridgePoints, bridgeWidthAttribute, bridgeBuffer, elevIter, gridGeometry, envelope,
                indexedLine);
    }

    /**
     * Extracts sections on the dtm on predefined points with a given id and KS.
     * 
     * @param riverLine the river line to consider for the cross sections extraction.
     *          <b>The river line has to be oriented from upstream to downstream.</b>
     * @param riverPointCoordinates the points coordinates.
     * @param riverPointIds the points ids.
     * @param riverPointKs the points KS.
     * @param elevation the elevation {@link GridCoverage2D}.
     * @param sectionsInterval the distance to use between extracted sections. 
     * @param sectionsWidth the width of the extracted sections.
     * @param bridgePoints the list of bridge {@link Point}s. 
     * @param bridgeWidthAttribute the name of the attribute in the bridges feature that defines the width of the bridge.
     * @param bridgeBuffer a buffer to use for the bridge inside which the bridge is considered to be on the river.
     * @param monitor the progress monitor.
     * @throws Exception
     */
    public RiverSectionsFromDtmExtractor( //
            LineString riverLine, //
            Coordinate[] riverPointCoordinates, //
            int[] riverPointIds, //
            double[] riverPointKs, GridCoverage2D elevation, //
            double sectionsInterval, //
            double sectionsWidth, //
            List<FeatureMate> bridgePoints, //
            String bridgeWidthAttribute, //
            double bridgeBuffer, //
            IHMProgressMonitor monitor //
    ) throws Exception {
        crs = elevation.getCoordinateReferenceSystem();

        RandomIter elevIter = CoverageUtilities.getRandomIterator(elevation);
        GridGeometry2D gridGeometry = elevation.getGridGeometry();
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(elevation);
        Envelope envelope = regionMap.toEnvelope();

        riverPointsList = new ArrayList<RiverPoint>();
        LengthIndexedLine indexedLine = new LengthIndexedLine(riverLine);

        monitor.beginTask("Extracting sections in supplied net points...", riverPointCoordinates.length);
        for( int i = 0; i < riverPointCoordinates.length; i++ ) {
            double pointIndex = indexedLine.indexOf(riverPointCoordinates[i]);
            // important to extract from left to right
            Coordinate leftPoint = indexedLine.extractPoint(pointIndex, sectionsWidth);
            Coordinate rightPoint = indexedLine.extractPoint(pointIndex, -sectionsWidth);
            if (envelope.intersects(leftPoint) && envelope.intersects(rightPoint)) {
                // TODO add ks of net point for section

                RiverPoint netPoint = getNetworkPoint(riverLine, elevIter, gridGeometry, pointIndex, riverPointKs[i], leftPoint,
                        rightPoint);
                netPoint.setSectionId(riverPointIds[i]);
//                netPoint.setSectionGaukler(riverPointKs[i]);
                
                if (netPoint != null)
                    riverPointsList.add(netPoint);
            }
            monitor.worked(1);
        }
        monitor.done();

        process(riverLine, sectionsWidth, bridgePoints, bridgeWidthAttribute, bridgeBuffer, elevIter, gridGeometry, envelope,
                indexedLine);
    }

    private void process( LineString riverLine, double sectionsWidth, List<FeatureMate> bridgePoints, String bridgeWidthAttribute,
            double bridgeBuffer, RandomIter elevIter, GridGeometry2D gridGeometry, Envelope envelope,
            LengthIndexedLine indexedLine ) throws TransformException {
        /*
         * handle bridges
         */
        if (bridgePoints != null) {
            for( int j = 0; j < bridgePoints.size(); ++j ) {
                FeatureMate bridgeFeature = bridgePoints.get(j);
                Geometry bridgeGeometry = bridgeFeature.getGeometry();
                Geometry bufferedBridgeGeometry = bridgeGeometry.buffer(bridgeBuffer);

                if (riverLine.intersects(bufferedBridgeGeometry)) {

                    double bridgeIndex = indexedLine.project(new Coordinate(bridgeGeometry.getCoordinate()));
                    double bridgeWidth = bridgeFeature.getAttribute(bridgeWidthAttribute, Double.class).doubleValue();
                    double pre = bridgeIndex - bridgeWidth / 2.0 - 1d;
                    double post = bridgeIndex + bridgeWidth / 2.0 + 1d;

                    // important to extract from left to right
                    Coordinate leftBridgePoint = indexedLine.extractPoint(bridgeIndex, sectionsWidth);
                    Coordinate rightBridgePoint = indexedLine.extractPoint(bridgeIndex, -sectionsWidth);
                    Coordinate leftPreBridgePoint = indexedLine.extractPoint(pre, sectionsWidth);
                    Coordinate rightPreBridgePoint = indexedLine.extractPoint(pre, -sectionsWidth);
                    Coordinate leftPostBridgePoint = indexedLine.extractPoint(post, sectionsWidth);
                    Coordinate rightPostBridgePoint = indexedLine.extractPoint(post, -sectionsWidth);

                    // bridge extraction
                    RiverPoint netPoint = getNetworkPoint(riverLine, elevIter, gridGeometry, bridgeIndex, null, leftBridgePoint,
                            rightBridgePoint);
                    if (netPoint != null)
                        riverPointsList.add(netPoint);

                    // pre bridge extraction
                    netPoint = getNetworkPoint(riverLine, elevIter, gridGeometry, pre, null, leftPreBridgePoint,
                            rightPreBridgePoint);
                    if (netPoint != null)
                        riverPointsList.add(netPoint);

                    // post bridge extraction
                    netPoint = getNetworkPoint(riverLine, elevIter, gridGeometry, post, null, leftPostBridgePoint,
                            rightPostBridgePoint);
                    if (netPoint != null)
                        riverPointsList.add(netPoint);
                }
            }
        }

        // add also the river coordinates that do not have sections
        Coordinate[] coordinates = riverLine.getCoordinates();
        List<ProfilePoint> riverPprofile = CoverageUtilities.doProfile(elevIter, gridGeometry, coordinates);
        for( ProfilePoint profilePoint : riverPprofile ) {
            Coordinate position = profilePoint.getPosition();
            if (envelope.intersects(position)) {
                position.z = profilePoint.getElevation();
                RiverPoint netPoint = new RiverPoint(position, profilePoint.getProgressive(), null, null);
                riverPointsList.add(netPoint);
            }
        }

        pointsWithSectionsNum = 0;
        for( RiverPoint netPoint : riverPointsList ) {
            if (netPoint.hasSection) {
                pointsWithSectionsNum++;
            }
        }
        Collections.sort(riverPointsList);
    }
    /**
     * Extract a {@link RiverPoint}.
     * 
     * @param riverLine the geometry of the main river.
     * @param elevIter the elevation raster.
     * @param gridGeometry the raster geometry.
     * @param progressiveDistance the progressive distance along the main river.
     * @param ks the KS for the section.
     * @param leftPoint the left point of the section.
     * @param rightPoint the right point of the section.
     * @return the created {@link RiverPoint}.
     * @throws TransformException
     */
    private RiverPoint getNetworkPoint( LineString riverLine, RandomIter elevIter, GridGeometry2D gridGeometry,
            double progressiveDistance, Double ks, Coordinate leftPoint, Coordinate rightPoint ) throws TransformException {
        List<ProfilePoint> sectionPoints = CoverageUtilities.doProfile(elevIter, gridGeometry, rightPoint, leftPoint);
        List<Coordinate> coordinate3dList = new ArrayList<Coordinate>();
        for( ProfilePoint sectionPoint : sectionPoints ) {
            Coordinate position = sectionPoint.getPosition();
            position.z = sectionPoint.getElevation();
            coordinate3dList.add(position);
        }
        LineString sectionLine3d = gf.createLineString(coordinate3dList.toArray(new Coordinate[0]));
        Geometry crossPoint = sectionLine3d.intersection(riverLine);
        Coordinate coordinate = crossPoint.getCoordinate();
        if (coordinate == null) {
            return null;
        }
        int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, null);
        double elev = elevIter.getSampleDouble(colRow[0], colRow[1], 0);
        coordinate.z = elev;
        RiverPoint netPoint = new RiverPoint(coordinate, progressiveDistance, sectionLine3d, ks);
        return netPoint;
    }

}
