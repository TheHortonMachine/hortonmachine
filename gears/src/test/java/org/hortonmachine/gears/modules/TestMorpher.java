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
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.morpher.OmsMorpher;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsMorpher}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMorpher extends HMTestCase {
    private double NaN = HMConstants.doubleNovalue;

    private GridCoverage2D raster;
    private GridCoverage2D skeleton;

    protected void setUp() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

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
        double[][] skeletonized = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 1.0, NaN, NaN, NaN, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        skeleton = CoverageUtilities.buildCoverage("data", skeletonized, envelopeParams, crs, true);
    }

    public void testDilate() throws Exception {

        OmsMorpher morpher = new OmsMorpher();
        morpher.inMap = raster;
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

        morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pKernel = new int[]{//
        /*    */0, 1, 0, //
                1, 1, 1, //
                0, 1, 0, //
        };
        morpher.pMode = Variables.DILATE;
        morpher.process();
        morphed = morpher.outMap;
        dilated = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, 1.0, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), dilated, DELTA);

        morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pKernel = new int[]{//
        /*    */0, 1, 1, //
                1, 1, 1, //
                1, 1, 0, //
        };
        morpher.pMode = Variables.DILATE;
        morpher.process();
        morphed = morpher.outMap;
        dilated = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN}, //
                {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };

        checkMatrixEqual(morphed.getRenderedImage(), dilated, DELTA);

        morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pKernel = new int[]{//
        /* */0, 0, 1, 0, 0,//
                0, 0, 1, 0, 0,//
                1, 1, 1, 1, 1, //
                0, 0, 1, 0, 0, //
                0, 0, 1, 0, 0 //
        };
        morpher.pMode = Variables.DILATE;
        morpher.process();
        morphed = morpher.outMap;
        dilated = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
                {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
                {NaN, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
                {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, NaN}, //
                {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, 1.0, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), dilated, DELTA);
    }

    public void testErode() throws Exception {
        OmsMorpher morpher = new OmsMorpher();
        morpher.inMap = raster;
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

        morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pMode = Variables.ERODE;
        morpher.pKernel = new int[]{//
        /*    */0, 1, 0, //
                1, 1, 1, //
                0, 1, 0, //
        };
        morpher.process();
        morphed = morpher.outMap;
        eroded = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };

        checkMatrixEqual(morphed.getRenderedImage(), eroded, DELTA);

    }

    public void testSkeletonize() throws Exception {
        OmsMorpher morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pMode = Variables.SKELETONIZE2;
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
        OmsMorpher morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pMode = Variables.OPEN;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] opened = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), opened, DELTA);

        morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pMode = Variables.OPEN;
        morpher.pKernel = new int[]{//
        /*    */0, 1, 0, //
                1, 1, 1, //
                0, 1, 0, //
        };
        morpher.process();
        morphed = morpher.outMap;
        opened = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), opened, DELTA);
    }

    public void testClose() throws Exception {
        OmsMorpher morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pMode = Variables.CLOSE;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] closed = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, 1.0, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), closed, DELTA);

        morpher = new OmsMorpher();
        morpher.inMap = raster;
        morpher.pMode = Variables.CLOSE;
        morpher.pKernel = new int[]{//
        /*    */0, 1, 0, //
                1, 1, 1, //
                0, 1, 0, //
        };
        morpher.process();
        morphed = morpher.outMap;

        closed = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, 1.0, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, 1.0, NaN, 1.0, 1.0, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), closed, DELTA);
    }

    public void testPrune() throws Exception {
        OmsMorpher morpher = new OmsMorpher();
        morpher.inMap = skeleton;
        morpher.pMode = Variables.PRUNE;
        morpher.pIterations = 1;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] pruned = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, 1.0, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), pruned, DELTA);
    }

    public void testLineendings() throws Exception {
        OmsMorpher morpher = new OmsMorpher();
        morpher.inMap = skeleton;
        morpher.pMode = Variables.LINEENDINGS;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] pruned = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, 1.0, NaN, NaN, NaN, 1.0, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), pruned, DELTA);
    }

    public void testJunctions() throws Exception {
        OmsMorpher morpher = new OmsMorpher();
        morpher.inMap = skeleton;
        morpher.pMode = Variables.LINEJUNCTIONS;
        morpher.process();
        GridCoverage2D morphed = morpher.outMap;
        double[][] pruned = new double[][]{//
        /*    */{NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, 1.0, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}, //
                {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN} //
        };
        checkMatrixEqual(morphed.getRenderedImage(), pruned, DELTA);
    }
}
