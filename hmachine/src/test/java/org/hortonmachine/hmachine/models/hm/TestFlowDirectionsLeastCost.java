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
import org.hortonmachine.hmachine.modules.geomorphology.flow.OmsLeastCostFlowDirections;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsLeastCostFlowDirections} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestFlowDirectionsLeastCost extends HMTestCase {

    public void testFlowDirectionsLeastCost() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D mapCoverage = CoverageUtilities.buildCoverage("elev", mapData, envelopeParams, crs, true);

        // PrintUtilities.printCoverageData(mapCoverage);

        OmsLeastCostFlowDirections flowDirections = new OmsLeastCostFlowDirections();
        flowDirections.inElev = mapCoverage;
        flowDirections.pm = pm;
        flowDirections.doAspect = true;
        flowDirections.doSlope = true;

        flowDirections.process();

        // GridCoverage2D flowCoverage = flowDirections.outFlow;
        // PrintUtilities.printCoverageData(flowCoverage);
        // System.out.println();
        // GridCoverage2D tcaCoverage = flowDirections.outTca;
        // PrintUtilities.printCoverageData(tcaCoverage);
        // System.out.println();
        // GridCoverage2D slopeCoverage = flowDirections.outSlope;
        // PrintUtilities.printCoverageData(slopeCoverage);
        // System.out.println();
        // GridCoverage2D aspectCoverage = flowDirections.outAspect;
        // PrintUtilities.printCoverageData(aspectCoverage);

        // checkMatrixEqual(flowCoverage.getRenderedImage(), HMTestMaps.newFlowData, 0);
    }

}
