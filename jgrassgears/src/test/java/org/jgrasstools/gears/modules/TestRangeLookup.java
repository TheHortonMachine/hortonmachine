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
import org.jgrasstools.gears.modules.r.rangelookup.RangeLookup;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link RangeLookup}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRangeLookup extends HMTestCase {

    public void testRangeLookup() throws Exception {
        double[][] inData = HMTestMaps.rangeLookupInData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        RangeLookup range = new RangeLookup();
        range.pm = pm;
        range.inRaster = inCoverage;
        range.pRanges = "[0 90),[90 180),[180 270),[270 360)";
        range.pClasses = "1,2,3,4";
        range.process();
        GridCoverage2D out = range.outRaster;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.rangeLookupOutData, 0);
    }

}
