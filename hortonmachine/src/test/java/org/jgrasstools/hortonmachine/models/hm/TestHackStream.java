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

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.hackstream.HackStream;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test {@link HackStream}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestHackStream extends HMTestCase {

    @SuppressWarnings("nls")
    public void testHackstream() throws IOException {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.mflowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);
        double[][] hacklengthData = HMTestMaps.hacklengthData;
        GridCoverage2D hacklengthCoverage = CoverageUtilities.buildCoverage("hacklength", hacklengthData, envelopeParams, crs,
                true);
        double[][] netData = HMTestMaps.extractNet1Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        HackStream hackStream = new HackStream();
        hackStream.pm = pm;
        hackStream.inFlow = flowCoverage;
        hackStream.inTca = tcaCoverage;
        hackStream.inNet = netCoverage;
        hackStream.inHacklength = hacklengthCoverage;

        hackStream.process();

        GridCoverage2D hackStreamCoverage = hackStream.outHackstream;
        checkMatrixEqual(hackStreamCoverage.getRenderedImage(), HMTestMaps.hackstream, 0.01);
    }

}
