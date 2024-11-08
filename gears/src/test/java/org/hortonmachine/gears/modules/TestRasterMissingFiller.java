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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.rasternull.OmsRasterMissingValuesFiller;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link OmsRasterMissingValuesFiller }.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterMissingFiller extends HMTestCase {

    private double NaN = HMConstants.doubleNovalue;

    private double[][] data = new double[][]{//
            {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
            {5, 5, 6, 6, 6, 6, 6, 6, 6, 5}, //
            {5, 7, 6, 6, 6, 6, 6, 7, 7, 5}, //
            {5, 5, 5, 7, NaN, 6, 6, 6, 5, 5}, //
            {5, 3, 4, 5, 5, 5, 5, 5, 5, 5}, //
            {5, 2, 3, 3, 4, 4, 4, 3, 3, 5}, //
            {5, 4, 4, 4, 4, 4, 5, 4, 4, 5}, //
            {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}};
    private double[][] mask = new double[][]{//
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};
    private double[][] mask2 = new double[][]{//
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, NaN, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}, //
            {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};

    public void testRasterFiller() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", data, envelopeParams, crs, true);
        GridCoverage2D inMask = CoverageUtilities.buildCoverage("mask", mask, envelopeParams, crs, true);

        OmsRasterMissingValuesFiller transformer = new OmsRasterMissingValuesFiller();
        transformer.inRaster = inCoverage;
        transformer.inRasterMask = inMask;
        transformer.doUseOnlyBorderValues = false;
        transformer.pMaxDistance = 1;
        transformer.pMinDistance = 0;
        transformer.pMode = "AVERAGING";
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;

        double[][] expected = new double[][]{//
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 6, 6, 6, 6, 6, 6, 6, 5}, //
                {5, 7, 6, 6, 6, 6, 6, 7, 7, 5}, //
                {5, 5, 5, 7, 6, 6, 6, 6, 5, 5}, //
                {5, 3, 4, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 2, 3, 3, 4, 4, 4, 3, 3, 5}, //
                {5, 4, 4, 4, 4, 4, 5, 4, 4, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}};

        checkMatrixEqual(outCoverage.getRenderedImage(), expected);
    }

    public void testRasterFillerMasked() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", data, envelopeParams, crs, true);
        GridCoverage2D inMask2 = CoverageUtilities.buildCoverage("mask2", mask2, envelopeParams, crs, true);

        OmsRasterMissingValuesFiller transformer = new OmsRasterMissingValuesFiller();
        transformer.inRaster = inCoverage;
        transformer.inRasterMask = inMask2;
        transformer.doUseOnlyBorderValues = false;
        transformer.pMaxDistance = 1;
        transformer.pMinDistance = 0;
        transformer.pMode = "AVERAGING";
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;

        double[][] expected = new double[][]{//
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 6, 6, 6, 6, 6, 6, 6, 5}, //
                {5, 7, 6, 6, 6, 6, 6, 7, 7, 5}, //
                {5, 5, 5, 7, NaN, 6, 6, 6, 5, 5}, //
                {5, 3, 4, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 2, 3, 3, 4, 4, 4, 3, 3, 5}, //
                {5, 4, 4, 4, 4, 4, 5, 4, 4, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}};

        checkMatrixEqual(outCoverage.getRenderedImage(), expected);
    }

    public void testRasterFillerBordersOnly() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", data, envelopeParams, crs, true);
        GridCoverage2D inMask = CoverageUtilities.buildCoverage("mask", mask, envelopeParams, crs, true);

        OmsRasterMissingValuesFiller transformer = new OmsRasterMissingValuesFiller();
        transformer.inRaster = inCoverage;
        transformer.inRasterMask = inMask;
        transformer.doUseOnlyBorderValues = false;
        transformer.pMaxDistance = 3;
        transformer.pMinDistance = 0;
        transformer.doUseOnlyBorderValues = true;
        transformer.pMode = "AVERAGING";
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;

        double[][] expected = new double[][]{//
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 5, 6, 6, 6, 6, 6, 6, 6, 5}, //
                {5, 7, 6, 6, 6, 6, 6, 7, 7, 5}, //
                {5, 5, 5, 7, 5.75, 6, 6, 6, 5, 5}, //
                {5, 3, 4, 5, 5, 5, 5, 5, 5, 5}, //
                {5, 2, 3, 3, 4, 4, 4, 3, 3, 5}, //
                {5, 4, 4, 4, 4, 4, 5, 4, 4, 5}, //
                {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}};

        checkMatrixEqual(outCoverage.getRenderedImage(), expected);

    }

}
