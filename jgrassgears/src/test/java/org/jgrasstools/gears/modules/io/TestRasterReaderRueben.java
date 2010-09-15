package org.jgrasstools.gears.modules.io;

import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.DirectPosition2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.ui.CoverageViewer;
import org.jgrasstools.gears.utils.HMTestCase;
import org.opengis.geometry.DirectPosition;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterReaderRueben extends HMTestCase {

    public void testRasterReader() throws Exception {
        String path = new File("C:\\TMP\\ruben\\Tofino_dem.tif").getAbsolutePath();
        GridCoverage2D readCoverage = RasterReader.readCoverage(path);

        RasterReader reader = new RasterReader();
        reader.file = path;
        // reader.fileNovalue = -9999.0;
        reader.geodataNovalue = Double.NaN;
        reader.process();
        readCoverage = reader.geodata;

        DirectPosition pt = new DirectPosition2D(readCoverage.getCoordinateReferenceSystem(), 288500.0, 5447900.0);
        double[] elev = null;
        elev = readCoverage.evaluate(pt, elev);
        System.out.println("Elev for first point. Length:  " + elev.length);
        for( double data : elev ) {
            System.out.println("    " + data);
        }

        CoverageViewer viewer = new CoverageViewer();
        viewer.coverage = readCoverage;
        viewer.viewCoverage();

    }
}
