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
import org.jgrasstools.gears.modules.r.cutout.CutOut;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link CutOut}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCutOut extends HMTestCase {

    private GridCoverage2D inCoverage;
    private GridCoverage2D inMask;

    protected void setUp() throws Exception {
        double[][] inData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        double[][] maskData = HMTestMaps.extractNet0Data;
        inMask = CoverageUtilities.buildCoverage("data", maskData, envelopeParams, crs, true);
    }

    public void testCutout() throws Exception {
        CutOut cutout = new CutOut();
        cutout.pm = pm;
        cutout.inGeodata = inCoverage;
        cutout.inMask = inMask;
        cutout.process();
        GridCoverage2D out = cutout.outGeodata;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutData, 0);
    }

    public void testCutoutInverse() throws Exception {
        CutOut cutout = new CutOut();
        cutout.pm = pm;
        cutout.inGeodata = inCoverage;
        cutout.inMask = inMask;
        cutout.doInverse = true;
        cutout.process();
        GridCoverage2D out = cutout.outGeodata;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutDataInverse, 0);
    }

    public void testCutoutInverseWithThresholds() throws Exception {
        CutOut cutout = new CutOut();
        cutout.pm = pm;
        cutout.inGeodata = inCoverage;
        cutout.inMask = inMask;
        cutout.doInverse = true;
        cutout.pMax = 1400.0;
        cutout.pMin = 800.0;
        cutout.process();
        GridCoverage2D out = cutout.outGeodata;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutDataMaxMinInverse800_1400, 0);
    }

}
