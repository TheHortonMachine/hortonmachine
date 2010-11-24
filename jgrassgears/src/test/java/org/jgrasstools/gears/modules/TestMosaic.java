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
import java.util.Arrays;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.arcgrid.ArcgridCoverageWriter;
import org.jgrasstools.gears.io.grasslegacy.modules.GrassMosaicLegacy;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.modules.r.mosaic.Mosaic;
import org.jgrasstools.gears.modules.utils.fileiterator.FileIterator;
import org.jgrasstools.gears.ui.CoverageViewer;
import org.jgrasstools.gears.utils.HMTestCase;
/**
 * Test for the mosaic modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMosaic extends HMTestCase {
    // public void testMosaic() throws Exception {
    //
    // FileIterator fiter = new FileIterator();
    // fiter.inFolder = "/home/moovida/TMP/geologico_dtm_test/";
    // fiter.pRegex = "asc";
    // fiter.pCode = "EPSG:32632";
    // fiter.process();
    // List<File> filesList = fiter.filesList;
    // for( File file : filesList ) {
    // System.out.println(file.getAbsolutePath());
    // }
    // PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
    // Mosaic mosaic = new Mosaic();
    // mosaic.inGeodataFiles = filesList;
    // mosaic.pm = pm;
    // mosaic.process();
    // GridCoverage2D outMap = mosaic.outGeodata;
    //
    // ArcgridCoverageWriter.writeArcgrid("/home/moovida/TMP/geologico_dtm_test/dsm00000ALL_wgs.asc",
    // outMap);
    //
    // CoverageViewer cv = new CoverageViewer();
    // cv.coverage =
    // RasterReader.readCoverage("/home/moovida/TMP/geologico_dtm_test/dsm00000ALL_wgs.asc");
    // cv.viewCoverage();
    // }

    public void testGrassMosaicLegacy() throws Exception {

        FileIterator fiter = new FileIterator();
        fiter.inFolder = "/home/moovida/DTM_TRENTINO/1x1/unzipped/";
        fiter.pRegex = "asc";
        fiter.pCode = "EPSG:32632";
        fiter.process();
        List<File> filesList = fiter.filesList;
        for( File file : filesList ) {
            System.out.println(file.getAbsolutePath());
        }
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
        GrassMosaicLegacy mosaic = new GrassMosaicLegacy();
        mosaic.inGeodataFiles = filesList;
        mosaic.pm = pm;
        mosaic.outFile = "/home/moovida/DTM_TRENTINO/grassdb/trentino/dtm2009/cell/dsm_all_1x1_right_wgs";
        mosaic.pRes = 2.0;
        // left
//        mosaic.pBounds = new double[]{5158010.0, 5059990.0, 611990.0, 672000.0}; // n,s,w,e
        // right
        mosaic.pBounds = new double[]{5158010.0, 5059990.0, 672000.0, 732010.0};

        // 1x1 upper left
        // mosaic.pBounds = new double[]{5158010.0, 5109000.0, 611990.0, 672000.0};
        // 1x1 upper right
        // mosaic.pBounds = new double[]{5158010.0, 5109000.0, 672000.0, 732010.0};
        // 1x1 lower left
        // mosaic.pBounds = new double[]{5109000.0, 5059990.0, 611990.0, 672000.0};
        // 1x1 upper right
        // mosaic.pBounds = new double[]{5109000.0, 5059990.0, 672000.0, 732010.0};
        mosaic.process();

        // CoverageViewer cv = new CoverageViewer();
        // cv.coverage = RasterReader
        // .readCoverage("/home/moovida/data/hydrocareworkspace/grassdb/utm32n_etrf89/aidi/cell/dsm_all_wgs");
        // cv.viewCoverage();
    }

    public static void main( String[] args ) {
        try {
            new TestMosaic().testGrassMosaicLegacy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
