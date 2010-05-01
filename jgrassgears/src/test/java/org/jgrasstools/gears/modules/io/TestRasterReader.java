package org.jgrasstools.gears.modules.io;

import java.io.File;
import java.net.URL;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterReader extends HMTestCase {

    public void testRasterReader() throws Exception {
        URL testUrl = this.getClass().getClassLoader().getResource("dtm_test.asc");
        String path = new File(testUrl.toURI()).getAbsolutePath();
        GridCoverage2D readCoverage = RasterReader.readCoverage(path);
        
        RasterReader reader = new RasterReader();
        reader.file = path;
        reader.fileNovalue = -9999.0;
        reader.geodataNovalue = Double.NaN;
        reader.process();
        readCoverage = reader.geodata;

        checkMatrixEqual(readCoverage.getRenderedImage(), HMTestMaps.mapData);

        double n = 5140020.0;
        double s = 5139840.0;
        double w = 1640710.0;
        double e = 1640920.0;
        double xres = 30.0;
        double yres = 30.0;
        reader = new RasterReader();
        reader.file = path;
        reader.pBounds = new double[]{n, s, w, e};
        reader.pRes = new double[]{xres, yres};
        reader.process();
        readCoverage = reader.geodata;
        
        double[][] mapData = new double[][]{
            {1000, 1000, 1200, 1250, 1300, 1350, 1450},
            {750, 850, 860, 900, 1000, 1200, 1250},
            {700, 750, 800, 850, 900, 1000, 1100},
            {650, 700, 750, 800, 850, 490, 450},
            {430, 500, 600, 700, 800, 500, 450},
            {700, 750, 760, 770, 850, 1000, 1150}};
        checkMatrixEqual(readCoverage.getRenderedImage(), mapData);

    }
}
