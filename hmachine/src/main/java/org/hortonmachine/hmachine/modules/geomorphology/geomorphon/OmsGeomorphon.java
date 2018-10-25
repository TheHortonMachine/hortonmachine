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
package org.hortonmachine.hmachine.modules.geomorphology.geomorphon;
import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.libs.modules.HMConstants.GEOMORPHOLOGY;

import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.coverage.ProfilePoint;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;

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

@Description(OmsGeomorphon.DESCRIPTION)
@Author(name = OmsGeomorphon.AUTHORS, contact = OmsGeomorphon.CONTACT)
@Keywords(OmsGeomorphon.KEYWORDS)
@Label(GEOMORPHOLOGY)
@Name("_" + OmsGeomorphon.NAME)
@Status(OmsGeomorphon.STATUS)
@License(OmsGeomorphon.LICENSE)
public class OmsGeomorphon extends HMModel {

    @Description(inELEV_DESCR)
    @In
    public GridCoverage2D inElev;

    @Description(pRadius_DESCR)
    @Unit(pRadius_UNIT)
    @In
    public double pRadius;

    @Description(pThreshold_DESCR)
    @Unit(pThreshold_UNIT)
    @In
    public double pThreshold = 1;

    @Description(outRaster_DESCR)
    @Out
    public GridCoverage2D outRaster;

    public static final String LICENSE = HMConstants.GPL3_LICENSE;
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String NAME = "geomorphonraster";
    public static final String KEYWORDS = "raster, geomorphon";
    public static final String CONTACT = "www.hydrologis.com";
    public static final String AUTHORS = "Andrea Antonello, Silvia Franceschi";
    public static final String DESCRIPTION = "The Geomorphon method for rasters";
    public static final String outRaster_DESCR = "Output categories raster.";
    public static final String pThreshold_DESCR = "Vertical angle threshold.";
    public static final String pRadius_DESCR = "Maximum search radius";
    public static final String inELEV_DESCR = "An elevation raster.";
    public static final String pThreshold_UNIT = "degree";
    public static final String pRadius_UNIT = "m";

    @Execute
    public void process() throws Exception {
        checkNull(inElev);

        if (pRadius <= 0) {
            throw new ModelsIllegalargumentException("The search radius has to be > 0.", this, pm);
        }

        final double diagonalDelta = pRadius / sqrt(2.0);

        final RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.getCols();
        final int rows = regionMap.getRows();

        final RandomIter elevIter = CoverageUtilities.getRandomIterator(inElev);
        final GridGeometry2D gridGeometry = inElev.getGridGeometry();

        WritableRaster[] outWRHolder = new WritableRaster[1];
        outRaster = CoverageUtilities.createCoverageFromTemplate(inElev, HMConstants.doubleNovalue, outWRHolder);
        final WritableRandomIter outIter = CoverageUtilities.getWritableRandomIterator(outWRHolder[0]);

        pm.beginTask("Calculate classes...", cols);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                try {
                    double classification = calculateGeomorphon(elevIter, gridGeometry, pRadius, pThreshold, diagonalDelta, c, r);
                    outIter.setSample(c, r, 0, classification);
                } catch (TransformException e) {
                    e.printStackTrace();
                }
            }
            pm.worked(1);
        }
        pm.done();

    }

    /**
     * Calculate the geomorphon for a given cell of an elevation map.
     * 
     * @param elevIter the elevation map {@link RandomIter}.
     * @param gridGeometry the {@link GridGeometry2D} of the read map.
     * @param searchRadius the search radius to use.
     * @param angleThreshold the angle threshold to apply.
     * @param diagonalDelta the search radius for diagonal cells (usually radius/sqrt(2.0) )
     * @param c the column of the cell to analyse.
     * @param r the row of the cell to analyse.
     * @return the geomorphon classification for the cell.
     * @throws TransformException
     */
    public static double calculateGeomorphon( RandomIter elevIter, GridGeometry2D gridGeometry, double searchRadius,
            double angleThreshold, double diagonalDelta, int c, int r ) throws TransformException {
        int[] plusCount = new int[1];
        int[] minusCount = new int[1];

        double elevation = elevIter.getSampleDouble(c, r, 0);
        if (HMConstants.isNovalue(elevation)) {
            return HMConstants.doubleNovalue;
        }
        DirectPosition worldPosition = gridGeometry.gridToWorld(new GridCoordinates2D(c, r));
        double[] coordinateArray = worldPosition.getCoordinate();
        Coordinate center = new Coordinate(coordinateArray[0], coordinateArray[1]);
        center.z = elevation;

        // calc 8 directions at max distance
        Coordinate c1 = new Coordinate(center.x + searchRadius, center.y, center.z);
        calculateCount(elevIter, gridGeometry, plusCount, minusCount, elevation, center, c1, angleThreshold);

        Coordinate c2 = new Coordinate(center.x + diagonalDelta, center.y + diagonalDelta, center.z);
        calculateCount(elevIter, gridGeometry, plusCount, minusCount, elevation, center, c2, angleThreshold);

        Coordinate c3 = new Coordinate(center.x, center.y + searchRadius, center.z);
        calculateCount(elevIter, gridGeometry, plusCount, minusCount, elevation, center, c3, angleThreshold);

        Coordinate c4 = new Coordinate(center.x - diagonalDelta, center.y + diagonalDelta, center.z);
        calculateCount(elevIter, gridGeometry, plusCount, minusCount, elevation, center, c4, angleThreshold);

        Coordinate c5 = new Coordinate(center.x - searchRadius, center.y, center.z);
        calculateCount(elevIter, gridGeometry, plusCount, minusCount, elevation, center, c5, angleThreshold);

        Coordinate c6 = new Coordinate(center.x - diagonalDelta, center.y - diagonalDelta, center.z);
        calculateCount(elevIter, gridGeometry, plusCount, minusCount, elevation, center, c6, angleThreshold);

        Coordinate c7 = new Coordinate(center.x, center.y - searchRadius, center.z);
        calculateCount(elevIter, gridGeometry, plusCount, minusCount, elevation, center, c7, angleThreshold);

        Coordinate c8 = new Coordinate(center.x + diagonalDelta, center.y - diagonalDelta, center.z);
        calculateCount(elevIter, gridGeometry, plusCount, minusCount, elevation, center, c8, angleThreshold);

        int classification = GeomorphonClassification.getClassification(plusCount[0], minusCount[0]);
        return classification;
    }

    private static void calculateCount( RandomIter elevIter, GridGeometry2D gridGeometry, int[] plusCount, int[] minusCount,
            double elevation, Coordinate center, Coordinate otherCoordinate, double angleThreshold ) throws TransformException {
        List<ProfilePoint> profile = CoverageUtilities.doProfile(elevIter, gridGeometry, center, otherCoordinate);
        double[] lastVisiblePointData = ProfilePoint.getLastVisiblePointData(profile);

        if (lastVisiblePointData != null) {
            double zenithAngle = lastVisiblePointData[4];
            double nadirAngle = 180 - lastVisiblePointData[9];

            double diff = nadirAngle - zenithAngle;
            int zeroCount = 0;
            if (diff > angleThreshold) {
                plusCount[0] = plusCount[0] + 1;
            } else if (diff < -angleThreshold) {
                minusCount[0] = minusCount[0] + 1;
            } else if (abs(diff) < angleThreshold) {
                zeroCount++;
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * Calculate a simple line of sight, given two coordinates on a raster.
     * 
     * @param regionMap
     * @param elevIter
     * @param gridGeometry
     * @param startCoordinate
     * @param endCoordinate
     * @return the last visible point, starting from the startCoordinate.
     * @throws TransformException
     */
    public static ProfilePoint getLastVisiblePoint( RegionMap regionMap, RandomIter elevIter, GridGeometry2D gridGeometry,
            Coordinate startCoordinate, Coordinate endCoordinate ) throws TransformException {
        Envelope2D envelope2d = gridGeometry.getEnvelope2D();
        ProfilePoint lastVisible = null;
        double minX = envelope2d.getMinX();
        double maxX = envelope2d.getMaxX();
        double minY = envelope2d.getMinY();
        double maxY = envelope2d.getMaxY();

        if (endCoordinate.x >= minX && //
                endCoordinate.x <= maxX && //
                endCoordinate.y >= minY && //
                endCoordinate.y <= maxY//
        ) {
            List<ProfilePoint> profile = CoverageUtilities.doProfile(elevIter, gridGeometry, startCoordinate, endCoordinate);

            ProfilePoint first = profile.get(0);
            double viewerelev = first.getElevation();

            ProfilePoint secondPoint = profile.get(1);
            double lastMax = secondPoint.getElevation() - viewerelev;
            double lastMaxFactor = lastMax / secondPoint.getProgressive();
            lastVisible = secondPoint;

            for( int i = 2; i < profile.size(); i++ ) {
                ProfilePoint currentPoint = profile.get(i);
                double currentElev = currentPoint.getElevation() - viewerelev;
                double currentProg = currentPoint.getProgressive();
                // the maximum value that it can reach. If it is bigger, it is the new max
                double possibleMax = currentProg * lastMaxFactor;
                if (currentElev >= possibleMax) {
                    // new max found, recalculate line of sight and set this as last seen point
                    lastMax = currentElev;
                    lastMaxFactor = lastMax / currentProg;
                    lastVisible = currentPoint;
                }
            }
        }

        return lastVisible;
    }

}
