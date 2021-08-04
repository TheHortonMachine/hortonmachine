/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.modules;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.modules.r.mosaic.OmsMosaic;
import org.hortonmachine.gears.modules.r.mosaic.OmsMosaic12;
import org.hortonmachine.gears.utils.HMTestCase;
/**
 * Test for the mosaic modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMosaic extends HMTestCase {
    static double[][] mapData = new double[][]{//
            {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, -9999.0, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
            {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
            {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
            {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
            {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
            {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};

    public void testMosaic() throws Exception {
        URL testUrl = this.getClass().getClassLoader().getResource("dtm_test_left.asc");
        File left = new File(testUrl.toURI());
        testUrl = this.getClass().getClassLoader().getResource("dtm_test_right.asc");
        File right = new File(testUrl.toURI());

        List<File> filesList = Arrays.asList(left, right);

        OmsMosaic mosaic = new OmsMosaic();
        mosaic.inFiles = filesList;
        mosaic.pm = pm;
        mosaic.process();
        GridCoverage2D readCoverage = mosaic.outRaster;
        checkMatrixEqual(readCoverage.getRenderedImage(), mapData);
    }

    public void testMosaic12() throws Exception {
        URL testUrl = this.getClass().getClassLoader().getResource("dtm_test_left.asc");
        File left = new File(testUrl.toURI());
        testUrl = this.getClass().getClassLoader().getResource("dtm_test_right.asc");
        File right = new File(testUrl.toURI());

        OmsMosaic12 mosaic = new OmsMosaic12();
        mosaic.inMap1 = left.getAbsolutePath();
        mosaic.inMap12 = right.getAbsolutePath();
        mosaic.pm = pm;
        mosaic.testmode = true;
        mosaic.process();
        GridCoverage2D readCoverage = mosaic.outRaster;
        checkMatrixEqual(readCoverage.getRenderedImage(), mapData);
    }

}
