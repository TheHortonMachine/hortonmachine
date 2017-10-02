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
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsExtractBasin;
import org.hortonmachine.hmachine.modules.demmanipulation.wateroutlet.OmsWateroutlet;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsWateroutlet} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestWateroutlet extends HMTestCase {
    public void testWateroutlet() throws Exception {

        double[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsWateroutlet wateroutlet = new OmsWateroutlet();
        wateroutlet.pm = pm;
        wateroutlet.inFlow = flowCoverage;
        wateroutlet.pNorth = 5139885.0;
        wateroutlet.pEast = 1640724.0;

        wateroutlet.process();

        GridCoverage2D basinCoverage = wateroutlet.outBasin;

        // System.out.println(wateroutlet.outArea);
        // PrintUtilities.printCoverageData(basinCoverage);

        checkMatrixEqual(basinCoverage.getRenderedImage(), HMTestMaps.basinWateroutletData, 0);
    }

    public void testExtractBasin() throws Exception {

        double[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsExtractBasin extractBasin = new OmsExtractBasin();
        extractBasin.pm = pm;
        extractBasin.inFlow = flowCoverage;
        extractBasin.pNorth = 5139885.0;
        extractBasin.pEast = 1640724.0;

        extractBasin.process();

        GridCoverage2D basinCoverage = extractBasin.outBasin;

        // System.out.println(extractBasin.outArea);
        // PrintUtilities.printCoverageData(basinCoverage);

        checkMatrixEqual(basinCoverage.getRenderedImage(), HMTestMaps.basinWateroutletData, 0);
    }

}
