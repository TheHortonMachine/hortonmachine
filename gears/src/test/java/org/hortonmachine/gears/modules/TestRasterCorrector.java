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
import org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsRasterCorrector}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterCorrector extends HMTestCase {
    private double NaN = HMConstants.doubleNovalue;
    public void testRasterCorrector() throws Exception {

        double[][] inData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        OmsRasterCorrector reprojector = new OmsRasterCorrector();
        reprojector.inRaster = inCoverage;
        reprojector.pCorrections = "0,0,1.0, 5,5, 666";
        reprojector.process();
        GridCoverage2D outGeodata = reprojector.outRaster;

        double[][] correctedMapData = new double[][]{//
        {1.0, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
                {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
                {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
                {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
                {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
                {500, 600, 700, 750, 760, 666.0, 850, 1000, 1150, 1500}, //
                {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
                {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};

        checkMatrixEqual(outGeodata.getRenderedImage(), correctedMapData);
    }

}
