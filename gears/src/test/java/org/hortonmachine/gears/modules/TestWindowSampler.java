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
package org.hortonmachine.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.modules.r.windowsampler.OmsWindowSampler;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsWindowSampler}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestWindowSampler extends HMTestCase {
    public void testWindowSampler() throws Exception {

        double[][] inData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true); //$NON-NLS-1$

        OmsWindowSampler windowSampler = new OmsWindowSampler();
        windowSampler.inGeodata = inCoverage;
        windowSampler.pMode = 0;
        windowSampler.pCols = 2;
        windowSampler.pRows = 2;
        windowSampler.process();
        GridCoverage2D outGeodata = windowSampler.outGeodata;

        double[][] res2x2 = {//
        {766.6666666666666, 900.0, 1052.5, 1212.5, 1425.0}, //
                {465.0, 700.0, 800.0, 810.0, 1137.5}, //
                {525.0, 595.0, 707.5, 787.5, 1150.0}, //
                {752.5, 882.75, 980.0, 1162.5, 1425.0}, //
        };
        checkMatrixEqual(outGeodata.getRenderedImage(), res2x2, 0.000001);

        windowSampler = new OmsWindowSampler();
        windowSampler.inGeodata = inCoverage;
        windowSampler.pMode = 0;
        windowSampler.pCols = 3;
        windowSampler.pRows = 5;
        windowSampler.process();
        outGeodata = windowSampler.outGeodata;

        double[][] res3x5 = {//
        {620.7142857142857, 834.0, 939.3333333333334, 1500.0}, //
                {726.6666666666666, 889.0, 1150.0, 1500.0} //
        };
        checkMatrixEqual(outGeodata.getRenderedImage(), res3x5, 0.000001);
    }

}
