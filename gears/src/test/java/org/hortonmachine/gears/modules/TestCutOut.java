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
import org.hortonmachine.gears.modules.r.cutout.OmsCutOut;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsCutOut}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCutOut extends HMTestCase {

    private GridCoverage2D inCoverage;
    private GridCoverage2D inMask;

    protected void setUp() throws Exception {
        double[][] inData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        double[][] maskData = HMTestMaps.extractNet0Data;
        inMask = CoverageUtilities.buildCoverage("data", maskData, envelopeParams, crs, true);
    }

    public void testCutout() throws Exception {
        OmsCutOut cutout = new OmsCutOut();
        cutout.pm = pm;
        cutout.inRaster = inCoverage;
        cutout.inMask = inMask;
        cutout.process();
        GridCoverage2D out = cutout.outRaster;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutData, 0);
    }

    public void testCutoutInverse() throws Exception {
        OmsCutOut cutout = new OmsCutOut();
        cutout.pm = pm;
        cutout.inRaster = inCoverage;
        cutout.inMask = inMask;
        cutout.doInverse = true;
        cutout.process();
        GridCoverage2D out = cutout.outRaster;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutDataInverse, 0);
    }

    public void testCutoutInverseWithThresholds() throws Exception {
        OmsCutOut cutout = new OmsCutOut();
        cutout.pm = pm;
        cutout.inRaster = inCoverage;
        cutout.inMask = inMask;
        cutout.doInverse = true;
        cutout.pMax = 1400.0;
        cutout.pMin = 800.0;
        cutout.process();
        GridCoverage2D out = cutout.outRaster;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutDataMaxMinInverse800_1400, 0);
    }

}
