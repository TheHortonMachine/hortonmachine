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
package org.hortonmachine.gears.modules;

import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.modules.r.summary.OmsZonalStats;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test for the {@link OmsZonalStats}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestZonalStats extends HMTestCase {
    private CoordinateReferenceSystem crs = HMTestMaps.getCrs();
    private RegionMap ep = HMTestMaps.getEnvelopeparams();

    private GridCoverage2D flowCoverage;
    private int[][] flowData;
    private SimpleFeatureCollection testLeftFC;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testLeftFC = HMTestMaps.getTestLeftFC();

        flowData = HMTestMaps.flowData;
        flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, ep, crs, true);

    }

    public void testZonalStats() throws Exception {
        OmsZonalStats zs = new OmsZonalStats();
        zs.pm = new DummyProgressMonitor();
        zs.inRaster = flowCoverage;
        zs.inVector = testLeftFC;
        zs.pPercentageThres = 0;
        zs.process();
        SimpleFeatureCollection outVector = zs.outVector;
        List<SimpleFeature> testList = FeatureUtilities.featureCollectionToList(outVector);
        assertEquals(testList.size(), 1);
        SimpleFeature feature = testList.get(0);

        Double max = (Double) feature.getAttribute(Variables.MAX);
        assertEquals(7.0, max);
        Double min = (Double) feature.getAttribute(Variables.MIN);
        assertEquals(2.0, min);
        Double avg = (Double) feature.getAttribute(Variables.AVG);
        assertEquals(4.82608695, avg, DELTA);
        Double var = (Double) feature.getAttribute(Variables.VAR);
        assertEquals(1.7958412098, var, DELTA);
        Double sum = (Double) feature.getAttribute(Variables.SUM);
        assertEquals(111, sum, DELTA);
        int activeCells = (Integer) feature.getAttribute(Variables.ACTCELLS);
        assertEquals(23, activeCells);
        int invalidCells = (Integer) feature.getAttribute(Variables.INVCELLS);
        assertEquals(17, invalidCells);
    }

}
