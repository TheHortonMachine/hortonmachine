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
package org.hortonmachine.hmachine.models.hm;

import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.utils.PrintUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.coverage.ProfilePoint;
import org.hortonmachine.hmachine.modules.geomorphology.geomorphon.OmsGeomorphon;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;

/**
 * Test for {@link OmsGeomorphon}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestGeomorphon extends HMTestCase {

    public void testLastVisible() throws Exception {
        double[][] mapData = new double[][]{//
        /*    */{500, 500, 500, 500, 500, 500, 500, 500, 500, 500}, //
                {500, 600, 600, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 600, 700, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 600, 800, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 600, 800, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 600, 700, 600, 600, 700, 800, 1100, 1500, 2500}, //
                {500, 600, 600, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 500, 500, 500, 500, 500, 500, 500, 500, 500}};
        HashMap<String, Double> eP = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inElev = CoverageUtilities.buildCoverage("elevation", mapData, eP, crs, true);

        OmsGeomorphon g = new OmsGeomorphon();
        g.inElev = inElev;
        g.pRadius = 90;
        g.pThreshold = 1;
        g.process();
        GridCoverage2D outRaster = g.outRaster;
        // PrintUtilities.printCoverageData(outRaster);

    }

    public void testLastVisible2() throws Exception {
        double[][] mapData = new double[][]{//
        /*    */{500, 500, 500, 500, 500, 500, 500, 500, 500, 500}, //
                {500, 600, 600, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 600, 700, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 600, 800, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 600, 800, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 600, 700, 600, 600, 700, 800, 1100, 1500, 2500}, //
                {500, 600, 600, 600, 600, 700, 800, 1100, 1500, 1500}, //
                {500, 500, 500, 500, 500, 500, 500, 500, 500, 500}};
        HashMap<String, Double> eP = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inElev = CoverageUtilities.buildCoverage("elevation", mapData, eP, crs, true);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        RandomIter elevIter = CoverageUtilities.getRandomIterator(inElev);
        GridGeometry2D gridGeometry = inElev.getGridGeometry();

        // up
        DirectPosition startPosition = gridGeometry.gridToWorld(new GridCoordinates2D(0, 3));
        double[] coordinateArray = startPosition.getCoordinate();
        Coordinate startCoord = new Coordinate(coordinateArray[0], coordinateArray[1]);
        DirectPosition endPosition = gridGeometry.gridToWorld(new GridCoordinates2D(9, 5));
        coordinateArray = endPosition.getCoordinate();
        Coordinate endCoord = new Coordinate(coordinateArray[0], coordinateArray[1]);

        ProfilePoint lastVisiblePoint = OmsGeomorphon
                .getLastVisiblePoint(regionMap, elevIter, gridGeometry, startCoord, endCoord);
        assertEquals(2500.0, lastVisiblePoint.getElevation(), DELTA);

        // down
        startPosition = gridGeometry.gridToWorld(new GridCoordinates2D(9, 5));
        coordinateArray = startPosition.getCoordinate();
        startCoord = new Coordinate(coordinateArray[0], coordinateArray[1]);
        endPosition = gridGeometry.gridToWorld(new GridCoordinates2D(0, 3));
        coordinateArray = endPosition.getCoordinate();
        endCoord = new Coordinate(coordinateArray[0], coordinateArray[1]);
        lastVisiblePoint = OmsGeomorphon.getLastVisiblePoint(regionMap, elevIter, gridGeometry, startCoord, endCoord);
        assertEquals(500.0, lastVisiblePoint.getElevation(), DELTA);

        // middle
        startPosition = gridGeometry.gridToWorld(new GridCoordinates2D(5, 3));
        coordinateArray = startPosition.getCoordinate();
        startCoord = new Coordinate(coordinateArray[0], coordinateArray[1]);
        endPosition = gridGeometry.gridToWorld(new GridCoordinates2D(0, 3));
        coordinateArray = endPosition.getCoordinate();
        endCoord = new Coordinate(coordinateArray[0], coordinateArray[1]);
        lastVisiblePoint = OmsGeomorphon.getLastVisiblePoint(regionMap, elevIter, gridGeometry, startCoord, endCoord);
        assertEquals(800.0, lastVisiblePoint.getElevation(), DELTA);
    }

}
