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

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.network.hacklength.OmsHackLength;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test {@link OmsHackLength}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestHackLength extends HMTestCase {

    public void testHacklength() throws IOException {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.mflowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);

        OmsHackLength hackLength = new OmsHackLength();
        hackLength.inFlow = flowCoverage;
        hackLength.inTca = tcaCoverage;
        hackLength.pm = pm;

        hackLength.process();

        GridCoverage2D hackLengthCoverage = hackLength.outHacklength;
        checkMatrixEqual(hackLengthCoverage.getRenderedImage(), HMTestMaps.hacklengthData, 0.01);
    }

    public void testHacklength3d() throws IOException {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] pitData = HMTestMaps.pitData;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs, true);
        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);

        OmsHackLength hackLength = new OmsHackLength();
        hackLength.inFlow = flowCoverage;
        hackLength.inTca = tcaCoverage;
        hackLength.inElevation = pitCoverage;
        hackLength.pm = pm;

        hackLength.process();

        GridCoverage2D hackLengthCoverage = hackLength.outHacklength;
        RenderedImage renderedImage = hackLengthCoverage.getRenderedImage();
        checkMatrixEqual(renderedImage, HMTestMaps.hacklength3DData, 2);
    }

}
