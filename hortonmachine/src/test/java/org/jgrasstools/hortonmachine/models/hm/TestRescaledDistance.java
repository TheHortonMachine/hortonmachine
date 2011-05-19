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
import org.jgrasstools.hortonmachine.modules.basin.rescaleddistance.RescaledDistance;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test {@link RescaledDistance}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRescaledDistance extends HMTestCase {

    public void testRescaledDistance() throws IOException {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet0Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        RescaledDistance rescaledDistance = new RescaledDistance();
        rescaledDistance.inFlow = flowCoverage;
        rescaledDistance.inNet = netCoverage;
        rescaledDistance.pRatio = 0.3;
        rescaledDistance.pm = pm;

        rescaledDistance.process();

        GridCoverage2D rescaledDistanceCoverage = rescaledDistance.outRescaled;
        checkMatrixEqual(rescaledDistanceCoverage.getRenderedImage(), HMTestMaps.rescaledDistanceData, 0.1);
    }

}
