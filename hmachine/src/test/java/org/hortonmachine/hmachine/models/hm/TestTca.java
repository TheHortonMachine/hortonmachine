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
import org.hortonmachine.gears.utils.PrintUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.tca.OmsTca;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsOldTca} module.
 * 
 * @author Giuseppe Formetta
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTca extends HMTestCase {

//    public void testTca() throws Exception {
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//
//        double[][] flowData = HMTestMaps.flowData;
//        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
//
//        OmsOldTca tca = new OmsOldTca();
//        tca.inFlow = flowCoverage;
//        tca.pm = pm;
//        tca.process();
//        GridCoverage2D tcaCoverage = tca.outTca;
//
//        checkMatrixEqual(tcaCoverage.getRenderedImage(), HMTestMaps.tcaData);
//    }

    public void testNewTca() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsTca tca = new OmsTca();
        tca.inFlow = flowCoverage;
        tca.pm = pm;
        tca.process();
        GridCoverage2D tcaCoverage = tca.outTca;

        PrintUtilities.printCoverageData(tcaCoverage);
        checkMatrixEqual(tcaCoverage.getRenderedImage(), HMTestMaps.tcaData);
    }

}