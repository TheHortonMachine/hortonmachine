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
import org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet.ExtractBasin;
import org.jgrasstools.hortonmachine.modules.demmanipulation.wateroutlet.Wateroutlet;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Wateroutlet} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestWateroutlet extends HMTestCase {
    public void testWateroutlet() throws Exception {

        double[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        Wateroutlet wateroutlet = new Wateroutlet();
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
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        ExtractBasin extractBasin = new ExtractBasin();
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
