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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.statistics.cb.OmsCb;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for the {@link OmsCb} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCb extends HMTestCase {

    private double[][] expected = new double[][]{ //
    {405.0, 2.0, 405.0}, //
            {430.0, 1.0, 430.0}, //
            {450.0, 3.0, 450.0}, //
            {490.0, 1.0, 490.0}, //
            {500.0, 4.0, 500.0}, //
            {550.0, 2.0, 550.0}, //
            {600.0, 4.0, 600.0}, //
            {650.0, 1.0, 650.0}, //
            {700.0, 5.0, 700.0}, //
            {750.0, 5.0, 750.0}, //
            {760.0, 1.0, 760.0}, //
            {770.0, 1.0, 770.0}, //
            {780.0, 1.0, 780.0}, //
            {790.0, 1.0, 790.0}, //
            {800.0, 6.0, 800.0}, //
            {850.0, 4.0, 850.0}, //
            {860.0, 1.0, 860.0}, //
            {902.5, 4.0, 902.5}, //
            {980.0, 1.0, 980.0}, //
            {1000.1428571428571, 7.0, 1000.1428571428571}, //
            {1100.0, 2.0, 1100.0}, //
            {1150.0, 2.0, 1150.0}, //
            {1200.0, 3.0, 1200.0}, //
            {1250.0, 4.0, 1250.0}, //
            {1300.0, 2.0, 1300.0}, //
            {1350.0, 1.0, 1350.0}, //
            {1450.0, 2.0, 1450.0}, //
            {1500.0, 8.0, 1500.0} //
    };

    public void testCb() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        GridCoverage2D map1 = CoverageUtilities.buildCoverage("map1", HMTestMaps.mapData, envelopeParams, crs, true);
        GridCoverage2D map2 = map1;

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        OmsCb cb = new OmsCb();
        cb.pBins = 100;
        cb.pFirst = 1;
        cb.pLast = 1;
        cb.inRaster1 = map1;
        cb.inRaster2 = map2;
        cb.pm = pm;

        cb.process();

        double[][] moments = cb.outCb;

        for( int i = 0; i < moments.length; i++ ) {
            for( int j = 0; j < moments[0].length; j++ ) {
                double value = moments[i][j];
                assertEquals(value, expected[i][j], 0.01);
            }
        }
    }

}
