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

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.PrintUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.tca.OmsTca;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsOldTca} module.
 * 
 * @author Giuseppe Formetta
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTca extends HMTestCase {

//    public void testTca() throws Exception {
//        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
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
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
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

    /**
     * The flow maps of OmsFlowDirections have -1 as novalue, not the HortonMachine one.
     */
    public void testTcaWithFlowNovalue() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double flowNovalue = -1;
        double[][] flowData = HMTestMaps.flowData;
        double[][] flowDataNv = new double[flowData.length][];
        for( int r = 0; r < flowData.length; r++ ) {
            flowDataNv[r] = flowData[r].clone();
            for( int c = 0; c < flowDataNv[r].length; c++ ) {
                if (isNovalue(flowDataNv[r][c])) {
                    flowDataNv[r][c] = flowNovalue;
                }
            }
        }
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverageWithNovalue("flow", flowDataNv, envelopeParams, crs,
                true, flowNovalue);

        OmsTca tca = new OmsTca();
        tca.inFlow = flowCoverage;
        tca.pm = pm;
        tca.process();

        checkMatrixEqual(tca.outTca.getRenderedImage(), HMTestMaps.tcaData);
    }

}