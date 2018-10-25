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
package org.hortonmachine.gears;

import java.awt.image.WritableRaster;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Envelope2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.coverage.ProfilePoint;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
/**
 * Test {@link CoverageUtilities}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCoverageUtilities extends HMTestCase {

    private RegionMap eP;
    private GridCoverage2D elevationCoverage;
    private double north;
    private double south;
    private double west;
    private double east;
    private double xres;
    private double yres;
    private int cols;
    private int rows;
    private CoordinateReferenceSystem crs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        double[][] elevationData = HMTestMaps.mapData;
        eP = HMTestMaps.getEnvelopeparams();
        crs = HMTestMaps.getCrs();
        elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, eP, crs, true);
        north = eP.getNorth();
        south = eP.getSouth();
        west = eP.getWest();
        east = eP.getEast();
        xres = eP.getXres();
        yres = eP.getYres();
        rows = eP.getRows();
        cols = eP.getCols();
    }

    @SuppressWarnings("nls")
    public void testCoverageUtilities() throws Exception {
        RegionMap paramsMap = CoverageUtilities.makeRegionParamsMap(1.0, 0.0, 1.0, 2.0, 1.0, 1.0, 1, 1);

        assertEquals(1.0, paramsMap.getNorth());
        assertEquals(0.0, paramsMap.getSouth());
        assertEquals(1.0, paramsMap.getWest());
        assertEquals(2.0, paramsMap.getEast());
        assertEquals(1.0, paramsMap.getXres());
        assertEquals(1.0, paramsMap.getYres());
        assertEquals(1, paramsMap.getCols());
        assertEquals(1, paramsMap.getRows());
        assertEquals(1.0, paramsMap.getHeight(), DELTA);
        assertEquals(1.0, paramsMap.getWidth(), DELTA);

        paramsMap = CoverageUtilities.getRegionParamsFromGridCoverage(elevationCoverage);
        assertEquals(north, paramsMap.getNorth());
        assertEquals(south, paramsMap.getSouth());
        assertEquals(west, paramsMap.getWest());
        assertEquals(east, paramsMap.getEast());
        assertEquals(xres, paramsMap.getXres());
        assertEquals(yres, paramsMap.getYres());
        assertEquals(cols, paramsMap.getCols());
        assertEquals(rows, paramsMap.getRows());
        assertEquals(north - south, paramsMap.getHeight(), DELTA);
        assertEquals(east - west, paramsMap.getWidth(), DELTA);

        WritableRaster wr1 = CoverageUtilities.renderedImage2WritableRaster(elevationCoverage.getRenderedImage(), false);
        WritableRaster wr2 = CoverageUtilities.renderedImage2WritableRaster(elevationCoverage.getRenderedImage(), false);
        assertTrue(CoverageUtilities.equals(wr1, wr2));

        wr2.setSample(3, 3, 0, -1.0);
        assertFalse(CoverageUtilities.equals(wr1, wr2));
    }

    public void testCoverageSubregionLoop() throws Exception {
        Envelope2D env = new Envelope2D();
        env.setRect(west, south, east - west, north - south);
        int[] loopColsRowsForSubregion1 = CoverageUtilities.getLoopColsRowsForSubregion(elevationCoverage, env);
        assertEquals(0, loopColsRowsForSubregion1[0]);
        assertEquals(cols, loopColsRowsForSubregion1[1]);
        assertEquals(0, loopColsRowsForSubregion1[2]);
        assertEquals(rows, loopColsRowsForSubregion1[3]);

        env = new Envelope2D();
        env.setRect(west + xres, south + yres, east - west - 2 * xres, north - south - 2 * yres);
        int[] loopColsRowsForSubregion2 = CoverageUtilities.getLoopColsRowsForSubregion(elevationCoverage, env);
        assertEquals(1, loopColsRowsForSubregion2[0]);
        assertEquals(cols - 1, loopColsRowsForSubregion2[1]);
        assertEquals(1, loopColsRowsForSubregion2[2]);
        assertEquals(rows - 1, loopColsRowsForSubregion2[3]);

        env = new Envelope2D();
        env.setRect(west + 2 * xres, south + yres, east - west - 3 * xres, north - south - 2 * yres);
        int[] loopColsRowsForSubregion3 = CoverageUtilities.getLoopColsRowsForSubregion(elevationCoverage, env);
        assertEquals(2, loopColsRowsForSubregion3[0]);
        assertEquals(cols - 1, loopColsRowsForSubregion3[1]);
        assertEquals(1, loopColsRowsForSubregion3[2]);
        assertEquals(rows - 1, loopColsRowsForSubregion3[3]);
    }

    public void testHypsographic() throws Exception {
        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> eP = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, eP, crs, true);

        double[][] calculateHypsographic = CoverageUtilities.calculateHypsographic(elevationCoverage, 10,
                new DummyProgressMonitor());
        double[][] expected = {{455.0, 0.0711},//
                {565.0, 0.0612},//
                {675.0, 0.0558},//
                {785.0, 0.0504},//
                {895.0, 0.0369},//
                {1005.0, 0.0288},//
                {1115.0, 0.0216},//
                {1225.0, 0.018},//
                {1335.0, 0.0117},//
                {1445.0, 0.0090}//
        };

        for( int i = 0; i < expected.length; i++ ) {
            for( int j = 0; j < expected[0].length; j++ ) {
                assertEquals(expected[i][j], calculateHypsographic[i][j]);
            }
        }

    }

    public void testProfile() throws Exception {
        double[][] elevationData = HMTestMaps.mapData;
        RegionMap eP = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, eP, crs, true);

        Coordinate c1 = new Coordinate(west + xres / 2.0, north - yres / 2.0);
        Coordinate c2 = new Coordinate(east - xres / 2.0, north - yres / 2.0);
        List<ProfilePoint> profile = CoverageUtilities.doProfile(elevationCoverage, c1, c2);
        double[][] expected = {//
        /*    */{0.0, 800.0},//
                {30.0, 900.0},//
                {60.0, 1000.0},//
                {90.0, 1000.0},//
                {120.0, 1200.0},//
                {150.0, 1250.0},//
                {180.0, 1300.0},//
                {210.0, 1350.0},//
                {240.0, 1450.0},//
                {270.0, 1500.0}//
        };
        checkProfile(profile, expected);

        c1 = new Coordinate(west - xres * 3.0 / 2.0, north - yres * 3.0 / 2.0);
        c2 = new Coordinate(east + xres * 3.0 / 2.0, north - yres * 3.0 / 2.0);
        profile = CoverageUtilities.doProfile(elevationCoverage, c1, c2);
        expected = new double[][]{//
        /*    */{0.0, HMConstants.doubleNovalue},//
                {30.0, HMConstants.doubleNovalue},//
                {60.0, 600.0},//
                {90.0, HMConstants.doubleNovalue},//
                {120.0, 750.0},//
                {150.0, 850.0},//
                {180.0, 860.0},//
                {210.0, 900.0},//
                {240.0, 1000.0},//
                {270.0, 1200.0},//
                {300.0, 1250.0},//
                {330.0, 1500.0},//
                {360.0, HMConstants.doubleNovalue},//
                {390.0, HMConstants.doubleNovalue}//
        };
        checkProfile(profile, expected);

    }

    public void testLos() throws Exception {
        double[][] elevationData = HMTestMaps.mapData;
        RegionMap eP = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, eP, crs, true);

        Coordinate c1 = new Coordinate(west + xres / 2.0, north - yres / 2.0);
        Coordinate c2 = new Coordinate(east - xres / 2.0, north - yres / 2.0);
        List<ProfilePoint> profile = CoverageUtilities.doProfile(elevationCoverage, c1, c2);
        double[] losData = ProfilePoint.getLastVisiblePointData(profile);
        double[] expected = {1200.0, 1640785.0, 5140005.0, 120.0, 16.69924423399362, 1000.0, 1640755.0, 5140005.0, 90.0,
                24.22774531795417};
        for( int i = 0; i < expected.length; i++ ) {
            assertEquals(expected[i], losData[i], DELTA);
        }
    }

    private void checkProfile( List<ProfilePoint> profile, double[][] expected ) {
        for( int i = 0; i < expected.length; i++ ) {
            ProfilePoint point = profile.get(i);
            double elevation = point.getElevation();
            assertEquals(expected[i][0], point.getProgressive());
            if (Double.isNaN(elevation)) {
                assertTrue(Double.isNaN(expected[i][1]));
            } else {
                assertEquals(expected[i][1], elevation);
            }
        }
    }
}
