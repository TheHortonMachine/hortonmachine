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

import java.awt.image.RenderedImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link OmsRasterNull}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterNull extends HMTestCase {
    
    private double NaN = HMConstants.doubleNovalue;

    private double[][] data = new double[][]{//
    /*    */{5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
            {5, 5, 6, 6, 6, 6, 6, 6, 6, 5}, //
            {5, 7, 6, 6, 6, 6, 6, 7, 7, 5}, //
            {5, 5, 5, 7, 6, 6, 6, 6, 5, 5}, //
            {5, 3, 4, 5, 5, 5, 5, 5, 5, 5}, //
            {5, 2, 3, 3, 4, 4, 4, 3, 3, 5}, //
            {5, 4, 4, 4, 4, 4, 5, 4, 4, 5}, //
            {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}};

    public void testRasterNull1() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", data, envelopeParams, crs, true);

        OmsRasterNull transformer = new OmsRasterNull();
        transformer.inRaster = inCoverage;
        transformer.pValue = 4.0;
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;

        double[][] expected1 = new double[][]{//
        /*    */{5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0}, //
                {5.0, 5.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 5.0}, //
                {5.0, 7.0, 6.0, 6.0, 6.0, 6.0, 6.0, 7.0, 7.0, 5.0}, //
                {5.0, 5.0, 5.0, 7.0, 6.0, 6.0, 6.0, 6.0, 5.0, 5.0}, //
                {5.0, 3.0, NaN, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0}, //
                {5.0, 2.0, 3.0, 3.0, NaN, NaN, NaN, 3.0, 3.0, 5.0}, //
                {5.0, NaN, NaN, NaN, NaN, NaN, 5.0, NaN, NaN, 5.0}, //
                {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0} //
        };
        checkMatrixEqual(outCoverage.getRenderedImage(), expected1);

    }

    public void testRasterNull2() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", data, envelopeParams, crs, true);

        OmsRasterNull transformer = new OmsRasterNull();
        transformer.inRaster = inCoverage;
        transformer.pValue = 4.0;
        transformer.pNull = 1.0;
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;

        double[][] expected2 = new double[][]{//
        /*    */{5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0}, //
                {5.0, 5.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 5.0}, //
                {5.0, 7.0, 6.0, 6.0, 6.0, 6.0, 6.0, 7.0, 7.0, 5.0}, //
                {5.0, 5.0, 5.0, 7.0, 6.0, 6.0, 6.0, 6.0, 5.0, 5.0}, //
                {5.0, 3.0, 1.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0}, //
                {5.0, 2.0, 3.0, 3.0, 1.0, 1.0, 1.0, 3.0, 3.0, 5.0}, //
                {5.0, 1.0, 1.0, 1.0, 1.0, 1.0, 5.0, 1.0, 1.0, 5.0}, //
                {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0} //
        };
        checkMatrixEqual(outCoverage.getRenderedImage(), expected2);
    }

    public void testRasterNull3() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", data, envelopeParams, crs, true);

        OmsRasterNull transformer = new OmsRasterNull();
        transformer.inRaster = inCoverage;
        transformer.pValue = 4.0;
        transformer.doInverse = true;
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;

        double[][] expected3 = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 4.0, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 4.0, 4.0, 4.0, NaN, NaN, NaN}, //
                {NaN, 4.0, 4.0, 4.0, 4.0, 4.0, NaN, 4.0, 4.0, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(outCoverage.getRenderedImage(), expected3);
    }

    /**
     * The inverse with a null value gives a mask of the value: 4 on its cells, 0 on the others.
     */
    public void testRasterNullInverseWithNullValue() throws Exception {
        OmsRasterNull transformer = new OmsRasterNull();
        transformer.inRaster = CoverageUtilities.buildCoverage("data", data, HMTestMaps.getEnvelopeparams(), HMTestMaps.getCrs(),
                true);
        transformer.pValue = 4.0;
        transformer.doInverse = true;
        transformer.pNull = 0.0;
        transformer.process();

        RenderedImage image = transformer.outRaster.getRenderedImage();
        for( int row = 0; row < data.length; row++ ) {
            for( int col = 0; col < data[0].length; col++ ) {
                double expected = data[row][col] == 4 ? 4 : 0;
                assertEquals(row + "/" + col, expected, image.getData().getSampleDouble(col, row, 0), DELTA);
            }
        }
    }

    /**
     * On a float raster the value is found as the float stores it.
     */
    public void testRasterNullOnFloatRaster() throws Exception {
        float[][] floatData = new float[data.length][data[0].length];
        for( int row = 0; row < data.length; row++ ) {
            for( int col = 0; col < data[0].length; col++ ) {
                floatData[row][col] = (float) (data[row][col] + 0.1);
            }
        }
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", floatData, HMTestMaps.getEnvelopeparams(),
                HMTestMaps.getCrs(), true);

        OmsRasterNull transformer = new OmsRasterNull();
        transformer.inRaster = inCoverage;
        transformer.pValue = 4.1;
        transformer.process();

        RenderedImage image = transformer.outRaster.getRenderedImage();
        for( int row = 0; row < data.length; row++ ) {
            for( int col = 0; col < data[0].length; col++ ) {
                double value = image.getData().getSampleDouble(col, row, 0);
                assertEquals(row + "/" + col, data[row][col] == 4, HMConstants.isNovalue(value));
            }
        }
    }
}
