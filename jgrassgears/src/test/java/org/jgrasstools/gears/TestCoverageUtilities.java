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
package org.jgrasstools.gears;

import java.awt.image.WritableRaster;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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
        eP = HMTestMaps.envelopeParams;
        crs = HMTestMaps.crs;
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
        HashMap<String, Double> eP = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
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

}
