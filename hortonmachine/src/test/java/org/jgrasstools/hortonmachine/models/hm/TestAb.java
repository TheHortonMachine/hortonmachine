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
package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.ab.Ab;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test ab.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAb extends HMTestCase {

    public void testAb() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData,
                envelopeParams, crs, true);
        double[][] planData = HMTestMaps.planData;
        GridCoverage2D planCoverage = CoverageUtilities.buildCoverage("plan", planData,
                envelopeParams, crs, true);

        Ab ab = new Ab();
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
