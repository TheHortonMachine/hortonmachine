/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 0 of the License, or
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
import org.hortonmachine.gears.modules.r.rasterdiff.OmsRasterDiff;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsRasterDiff}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterDiff extends HMTestCase {
    private double NaN = HMConstants.doubleNovalue;
    private GridCoverage2D inRaster1;
    private GridCoverage2D inRaster2;

    protected void setUp() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] inData = HMTestMaps.flowData;
        double[][] inDataPlus = new double[inData.length][inData[0].length];
        for( int i = 0; i < inData.length; i++ ) {
            for( int j = 0; j < inData[0].length; j++ ) {
                inDataPlus[i][j] = inData[i][j] + 0.0;
            }
        }
        inRaster1 = CoverageUtilities.buildCoverage("data", inDataPlus, envelopeParams, crs, true);

        inData = HMTestMaps.flowData;
        inRaster2 = CoverageUtilities.buildCoverage("flow", inData, envelopeParams, crs, true);
    }

    public void testDiff() throws Exception {
        OmsRasterDiff cutout = new OmsRasterDiff();
        cutout.pm = pm;
        cutout.inRaster1 = inRaster1;
        cutout.inRaster2 = inRaster2;
        cutout.process();
        GridCoverage2D out = cutout.outRaster;

        double[][] expected = new double[][]{//
        {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};
        checkMatrixEqual(out.getRenderedImage(), expected, 0);
    }

    public void testDiffWithThres() throws Exception {
        OmsRasterDiff cutout = new OmsRasterDiff();
        cutout.pm = pm;
        cutout.inRaster1 = inRaster1;
        cutout.inRaster2 = inRaster2;
        cutout.pThreshold = 0.0;
        cutout.process();
        GridCoverage2D out = cutout.outRaster;

        double[][] expected = new double[][]{//
        {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, 0, 0, 0, 0, 0, 0, 0, 0, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};
        checkMatrixEqual(out.getRenderedImage(), expected, 0);
    }

}
