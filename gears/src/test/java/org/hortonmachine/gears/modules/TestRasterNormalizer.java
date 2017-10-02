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
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.normalizer.OmsRasterNormalizer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsRasterNormalizer}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterNormalizer extends HMTestCase {
    private double NaN = HMConstants.doubleNovalue;
    private GridCoverage2D inCoverage;

    protected void setUp() throws Exception {
        double[][] inData = new double[][]{//
        {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 6, 6, 6, 6, 6, 6, 6, NaN}, //
                {NaN, 7, 6, 6, 6, 6, 6, 7, 7, NaN}, //
                {NaN, 10, 5, 7, 6, 6, 6, 6, 5, NaN}, //
                {0, 3, 4, 5, 5, 5, 5, 5, 5, NaN}, //
                {NaN, 2, 3, 3, 4, 4, 4, 3, 3, NaN}, //
                {NaN, 4, 4, 4, 4, 4, 5, 4, 4, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);
    }

    public void testCutout() throws Exception {
        OmsRasterNormalizer cutout = new OmsRasterNormalizer();
        cutout.pm = pm;
        cutout.inRaster = inCoverage;
        cutout.doSetnovalues = true;
        cutout.pNValue = 1.0;
        cutout.process();
        GridCoverage2D out = cutout.outRaster;

        double[][] expected = new double[][]{//
        /*    */{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //
                {0.0, 0.0, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.6, 0.0}, //
                {0.0, 0.7, 0.6, 0.6, 0.6, 0.6, 0.6, 0.7, 0.7, 0.0},//
                {0.0, 1.0, 0.5, 0.7, 0.6, 0.6, 0.6, 0.6, 0.5, 0.0}, //
                {0.0, 0.3, 0.4, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.0}, //
                {0.0, 0.2, 0.3, 0.3, 0.4, 0.4, 0.4, 0.3, 0.3, 0.0}, //
                {0.0, 0.4, 0.4, 0.4, 0.4, 0.4, 0.5, 0.4, 0.4, 0.0}, //
                {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}};

        checkMatrixEqual(out.getRenderedImage(), expected, DELTA);
    }

}
