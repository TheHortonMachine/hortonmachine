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
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.ab.OmsAb;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test ab.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAb extends HMTestCase {

    public void testAb() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData,
                envelopeParams, crs, true);
        double[][] planData = HMTestMaps.planData;
        GridCoverage2D planCoverage = CoverageUtilities.buildCoverage("plan", planData,
                envelopeParams, crs, true);

        OmsAb ab = new OmsAb();
        ab.inTca = tcaCoverage;
        ab.inPlan = planCoverage;
        ab.pm = pm;

        ab.process();

        GridCoverage2D alungCoverage = ab.outAb;
        GridCoverage2D bCoverage = ab.outB;

        checkMatrixEqual(alungCoverage.getRenderedImage(), HMTestMaps.abData, 0.01);
        checkMatrixEqual(bCoverage.getRenderedImage(), HMTestMaps.bData, 0.01);
    }

}
