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
import org.jgrasstools.gears.utils.PrintUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.LeastCostFlowDirections;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link LeastCostFlowDirections} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestFlowDirectionsLeastCost extends HMTestCase {

    public void testFlowDirectionsLeastCost() throws Exception {
        double[][] mapData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D mapCoverage = CoverageUtilities.buildCoverage("elev", mapData, envelopeParams, crs, true);

        // RasterWriter.writeRaster("/home/moovida/Dropbox/hydrologis/lavori/2012_03_27_finland_forestry/data/testdem.asc",
        // mapCoverage);
        PrintUtilities.printCoverageData(mapCoverage);

        LeastCostFlowDirections flowDirections = new LeastCostFlowDirections();
        flowDirections.inElev = mapCoverage;
        flowDirections.pm = pm;

        flowDirections.process();

        GridCoverage2D flowCoverage = flowDirections.outFlow;

        PrintUtilities.printCoverageData(flowCoverage);

        // checkMatrixEqual(flowCoverage.getRenderedImage(), HMTestMaps.newFlowData, 0);
    }

}
