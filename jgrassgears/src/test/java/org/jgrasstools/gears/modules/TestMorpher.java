/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules;

import static java.lang.Double.NaN;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.Variables;
import org.jgrasstools.gears.modules.r.morpher.Morpher;
import org.jgrasstools.gears.modules.r.rasterdiff.OmsRasterDiff;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.PrintUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link Morpher}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMorpher extends HMTestCase {

    private GridCoverage2D raster;

    protected void setUp() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        // double[][] map = new double[][]{//
        // /* */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
        // {NaN, NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, NaN}, //
        // {NaN, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0, 0.0, 0.0, NaN}, //
        // {NaN, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, NaN}, //
        // {NaN, 0.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, NaN}, //
        // {NaN, 0.0, 0.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, NaN}, //
        // {NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, NaN}, //
        // {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};
        double[][] map = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, 1.0, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}};
        raster = CoverageUtilities.buildCoverage("data", map, envelopeParams, crs, true);
    }

    public void testDilate() throws Exception {

        Morpher morpher = new Morpher();
        morpher.inMap = raster;
        morpher.pValid = 1.0;
        morpher.pMode = Variables.DILATE;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] dilated = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), dilated, DELTA);
    }

    public void testErode() throws Exception {
        Morpher morpher = new Morpher();
        morpher.inMap = raster;
        morpher.pValid = 1.0;
        morpher.pMode = Variables.ERODE;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] eroded = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), eroded, DELTA);
    }

    public void testSkeletonize() throws Exception {
        Morpher morpher = new Morpher();
        morpher.inMap = raster;
        morpher.pValid = 1.0;
        morpher.pMode = Variables.SKELETONIZE;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] skeletonized = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 1.0, NaN, NaN, NaN, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), skeletonized, DELTA);
    }

    public void testOpen() throws Exception {
        Morpher morpher = new Morpher();
        morpher.inMap = raster;
        morpher.pValid = 1.0;
        morpher.pMode = Variables.OPEN;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] opened = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), opened, DELTA);
    }

    public void testClose() throws Exception {
        Morpher morpher = new Morpher();
        morpher.inMap = raster;
        morpher.pValid = 1.0;
        morpher.pMode = Variables.CLOSE;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] closed = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), closed, DELTA);
    }

}
