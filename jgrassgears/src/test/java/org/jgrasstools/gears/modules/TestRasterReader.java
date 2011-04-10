package org.jgrasstools.gears.modules;

import java.io.File;
import java.net.URL;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
/**
 * Test Id2ValueReader.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterReader extends HMTestCase {

    public void testRasterReader() throws Exception {
        URL testUrl = this.getClass().getClassLoader().getResource("dtm_test.asc");
        String path = new File(testUrl.toURI()).getAbsolutePath();

        RasterReader reader = new RasterReader();
        reader.file = path;
        reader.fileNovalue = -9999.0;
        reader.geodataNovalue = Double.NaN;
        reader.process();
        GridCoverage2D readCoverage = reader.geodata;

        checkMatrixEqual(readCoverage.getRenderedImage(), HMTestMaps.mapData);

        double n = 5140020.0;
        double s = 5139840.0;
        double w = 1640710.0;
        double e = 1640920.0;
        double xres = 30.0;
        double yres = 30.0;
        reader = new RasterReader();
        reader.file = path;
        reader.pNorth = n;
        reader.pSouth = s;
        reader.pWest = w;
        reader.pEast = e;
        reader.pXres = xres;
        reader.pYres = yres;
        reader.process();
        readCoverage = reader.geodata;
        
        double[][] mapData = HMTestMaps.mapData;
        mapData[1][1] = -9999.0;
        printImage(readCoverage.getRenderedImage());
        checkMatrixEqual(readCoverage.getRenderedImage(), mapData);

    }
}
