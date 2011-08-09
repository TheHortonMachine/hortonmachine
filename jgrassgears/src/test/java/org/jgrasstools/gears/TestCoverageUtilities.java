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

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
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

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> eP = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, eP, crs, true);

        paramsMap = CoverageUtilities.getRegionParamsFromGridCoverage(elevationCoverage);
        assertEquals(eP.get(CoverageUtilities.NORTH), paramsMap.getNorth());
        assertEquals(eP.get(CoverageUtilities.SOUTH), paramsMap.getSouth());
        assertEquals(eP.get(CoverageUtilities.WEST), paramsMap.getWest());
        assertEquals(eP.get(CoverageUtilities.EAST), paramsMap.getEast());
        assertEquals(eP.get(CoverageUtilities.XRES), paramsMap.getXres());
        assertEquals(eP.get(CoverageUtilities.YRES), paramsMap.getYres());
        assertEquals(eP.get(CoverageUtilities.COLS).intValue(), paramsMap.getCols());
        assertEquals(eP.get(CoverageUtilities.ROWS).intValue(), paramsMap.getRows());
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
