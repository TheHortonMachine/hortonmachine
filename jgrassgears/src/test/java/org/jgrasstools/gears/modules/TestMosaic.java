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
package org.jgrasstools.gears.modules;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.modules.r.mosaic.Mosaic;
import org.jgrasstools.gears.modules.r.mosaic.Mosaic12;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
/**
 * Test for the mosaic modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMosaic extends HMTestCase {
    public void testMosaic() throws Exception {
        URL testUrl = this.getClass().getClassLoader().getResource("dtm_test_left.asc");
        File left = new File(testUrl.toURI());
        testUrl = this.getClass().getClassLoader().getResource("dtm_test_right.asc");
        File right = new File(testUrl.toURI());

        List<File> filesList = Arrays.asList(left, right);

        Mosaic mosaic = new Mosaic();
        mosaic.inFiles = filesList;
        mosaic.pm = pm;
        mosaic.process();
        GridCoverage2D readCoverage = mosaic.outRaster;
        checkMatrixEqual(readCoverage.getRenderedImage(), HMTestMaps.mapData);
    }

    public void testMosaic12() throws Exception {
        URL testUrl = this.getClass().getClassLoader().getResource("dtm_test_left.asc");
        File left = new File(testUrl.toURI());
        testUrl = this.getClass().getClassLoader().getResource("dtm_test_right.asc");
        File right = new File(testUrl.toURI());

        Mosaic12 mosaic = new Mosaic12();
        mosaic.inMap1 = left.getAbsolutePath();
        mosaic.inMap12 = right.getAbsolutePath();
        mosaic.pm = pm;
        mosaic.process();
        GridCoverage2D readCoverage = mosaic.outRaster;
        checkMatrixEqual(readCoverage.getRenderedImage(), HMTestMaps.mapData);
    }

}
