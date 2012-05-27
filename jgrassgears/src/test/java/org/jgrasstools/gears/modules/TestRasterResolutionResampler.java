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
package org.jgrasstools.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.modules.r.transformer.RasterResolutionResampler;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.PrintUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link RasterResolutionResampler}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterResolutionResampler extends HMTestCase {

    public void testRasterResolutionResampler() throws Exception {

        double[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        RasterResolutionResampler transformer = new RasterResolutionResampler();
        transformer.inGeodata = flowCoverage;
        transformer.pInterpolation = 0;
        transformer.pXres = 60.0;
        transformer.pYres = 60.0;
        transformer.process();
        GridCoverage2D outCoverage = transformer.outGeodata;
        double value = CoverageUtilities.getValue(outCoverage, 1, 1);

        assertEquals(7.0, value, 0.000000001);
    }
}
