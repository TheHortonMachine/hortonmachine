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
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsDrainDir} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDrainDir extends HMTestCase {

    public void testDrain() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pitfiller", pitfillerData, envelopeParams, crs, true);
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsDrainDir drainDir = new OmsDrainDir();
        // drainDir.doLad = false;
        drainDir.pLambda = 1;
        drainDir.inPit = pitfillerCoverage;
        drainDir.inFlow = flowCoverage;
        drainDir.pm = pm;

        drainDir.process();

        GridCoverage2D draindirCoverage = drainDir.outFlow;
        GridCoverage2D tcaCoverage = drainDir.outTca;

        checkMatrixEqual(draindirCoverage.getRenderedImage(), HMTestMaps.drainData1);
        checkMatrixEqual(tcaCoverage.getRenderedImage(), HMTestMaps.mtcaData);
    }

    public void testDrainLtd() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pitfiller", pitfillerData, envelopeParams, crs, true);
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsDrainDir drainDir = new OmsDrainDir();
        drainDir.doLad = false;
        drainDir.pLambda = 1;
        drainDir.inPit = pitfillerCoverage;
        drainDir.inFlow = flowCoverage;
        drainDir.pm = pm;

        drainDir.process();

        GridCoverage2D draindirCoverage = drainDir.outFlow;
        GridCoverage2D tcaCoverage = drainDir.outTca;

        checkMatrixEqual(draindirCoverage.getRenderedImage(), HMTestMaps.drainData0);
        checkMatrixEqual(tcaCoverage.getRenderedImage(), HMTestMaps.mtcaData0);
    }

}
