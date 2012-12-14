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

    // public void testGrassMosaicLegacy() throws Exception {
    //
    // FileIterator fiter = new FileIterator();
    // fiter.inFolder = "/home/moovida/DTM_TRENTINO/1x1/unzipped/";
    // fiter.pRegex = "asc";
    // fiter.pCode = "EPSG:32632";
    // fiter.process();
    // List<File> filesList = fiter.filesList;
    // for( File file : filesList ) {
    // System.out.println(file.getAbsolutePath());
    // }
    // PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
    // GrassMosaicLegacy mosaic = new GrassMosaicLegacy();
    // mosaic.inGeodataFiles = filesList;
    // mosaic.pm = pm;
    // mosaic.outFile =
    // "/home/moovida/DTM_TRENTINO/grassdb/trentino/dtm2009/cell/dsm_all_1x1_right_wgs";
    // mosaic.pRes = 2.0;
    // // left
    // // mosaic.pBounds = new double[]{5158010.0, 5059990.0, 611990.0, 672000.0}; // n,s,w,e
    // // right
    // mosaic.pBounds = new double[]{5158010.0, 5059990.0, 672000.0, 732010.0};
    //
    // // 1x1 upper left
    // // mosaic.pBounds = new double[]{5158010.0, 5109000.0, 611990.0, 672000.0};
    // // 1x1 upper right
    // // mosaic.pBounds = new double[]{5158010.0, 5109000.0, 672000.0, 732010.0};
    // // 1x1 lower left
    // // mosaic.pBounds = new double[]{5109000.0, 5059990.0, 611990.0, 672000.0};
    // // 1x1 upper right
    // // mosaic.pBounds = new double[]{5109000.0, 5059990.0, 672000.0, 732010.0};
    // mosaic.process();
    //
    // // CoverageViewer cv = new CoverageViewer();
    // // cv.coverage = RasterReader
    // //
    // .readCoverage("/home/moovida/data/hydrocareworkspace/grassdb/utm32n_etrf89/aidi/cell/dsm_all_wgs");
    // // cv.viewCoverage();
    // }

    // public static void main( String[] args ) throws Exception {
    // String in =
    // "/home/moovida/development/jgrasstools-hg/jgrassgears/src/test/resources/dtm_test.asc";
    // String left =
    // "/home/moovida/development/jgrasstools-hg/jgrassgears/src/test/resources/dtm_test_left.asc";
    // String right =
    // "/home/moovida/development/jgrasstools-hg/jgrassgears/src/test/resources/dtm_test_right.asc";
    //
    // double n = 5140020.0;
    // double s = 5139780.0;
    // double w = 1640650.0;
    // double e = 1640920.0;
    // double xres = 30.0;
    // double yres = 30.0;
    // RasterReader reader = new RasterReader();
    // reader.file = in;
    // reader.pNorth = n;
    // reader.pSouth = s;
    // reader.pWest = w;
    // reader.pEast = e;
    // reader.pXres = xres;
    // reader.pYres = yres;
    // reader.process();
    // GridCoverage2D readCoverage = reader.geodata;
    // RasterWriter.writeRaster(left, readCoverage);
    //
    // w = 1640920.0;
    // e = 1640950.0;
    // reader = new RasterReader();
    // reader.file = in;
    // reader.pNorth = n;
    // reader.pSouth = s;
    // reader.pWest = w;
    // reader.pEast = e;
    // reader.pXres = xres;
    // reader.pYres = yres;
    // reader.process();
    // readCoverage = reader.geodata;
    // RasterWriter.writeRaster(right, readCoverage);
    //
    // }

}
