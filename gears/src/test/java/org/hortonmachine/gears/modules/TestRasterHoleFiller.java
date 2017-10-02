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
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.holefiller.OmsHoleFiller;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link OmsHoleFiller}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterHoleFiller extends HMTestCase {

    private double NaN = HMConstants.doubleNovalue;

    private double[][] data = new double[][]{//
            /*    */{5, 5, 5, 5, 5, 5, 5, 5, 5, 5}, //
            {5, 5, 6, 6, 6, 6, 6, 6, 6, 5}, //
            {5, 7, 6, 6, 6, NaN, 6, 7, 7, 5}, //
            {5, 5, 5, 7, 6, 6, 6, 6, 5, 5}, //
            {5, NaN, 4, 5, 5, 5, 5, 5, 5, 5}, //
            {5, NaN, 3, 3, 4, 4, 4, 3, 3, 5}, //
            {5, NaN, 4, 4, 4, 4, 5, 4, 4, 5}, //
            {5, 5, 5, 5, 5, 5, 5, 5, 5, 5}};

    public void testHoleFiller() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", data, envelopeParams, crs, true);

        OmsHoleFiller transformer = new OmsHoleFiller();
        transformer.inRaster = inCoverage;
        transformer.pMode = Variables.IDW;
        transformer.pBuffer = 10000;
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;

        double[][] expected = new double[][]{//
                /*    */{5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0}, //
                {5.0, 5.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 6.0, 5.0}, //
                {5.0, 7.0, 6.0, 6.0, 6.0, 6.0, 6.0, 7.0, 7.0, 5.0}, //
                {5.0, 5.0, 5.0, 7.0, 6.0, 6.0, 6.0, 6.0, 5.0, 5.0}, //
                {5.0, 4.6000000000000005, 4.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0}, //
                {5.0, 4.32, 3.0, 3.0, 4.0, 4.0, 4.0, 3.0, 3.0, 5.0}, //
                {5.0, 4.553333333333334, 4.0, 4.0, 4.0, 4.0, 5.0, 4.0, 4.0, 5.0}, //
                {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0} //
        };
        RenderedImage renderedImage = outCoverage.getRenderedImage();
        checkMatrixEqual(renderedImage, expected);

    }

}
