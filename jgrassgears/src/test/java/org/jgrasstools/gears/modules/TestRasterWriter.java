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

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link OmsRasterWriter}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterWriter extends HMTestCase {

    private String arcPath;
    private String grassPath;
    private GridCoverage2D coverage;

    protected void setUp() throws Exception {
        URL testUrl = this.getClass().getClassLoader().getResource("dtm_test.asc");
        arcPath = new File(testUrl.toURI()).getAbsolutePath();
        arcPath = arcPath.replaceFirst("dtm_test.asc", "dtm_testout.asc");
        testUrl = this.getClass().getClassLoader().getResource("gbovest/testcase/cell");
        grassPath = new File(testUrl.toURI()).getAbsolutePath() + File.separator + "testout";

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        coverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);
    }

    public void testRasterWriter() {

        try {
            OmsRasterWriter writer = new OmsRasterWriter();
            writer.inRaster = coverage;
            writer.file = arcPath;
            writer.process();

            OmsRasterReader reader = new OmsRasterReader();
            reader.file = arcPath;
            reader.fileNovalue = -9999.0;
            reader.geodataNovalue = Double.NaN;
            reader.process();
            GridCoverage2D readCoverage = reader.outRaster;
            checkMatrixEqual(readCoverage.getRenderedImage(), HMTestMaps.mapData);

            writer = new OmsRasterWriter();
            writer.inRaster = coverage;
            writer.file = grassPath;
            writer.process();

            reader = new OmsRasterReader();
            reader.file = grassPath;
            reader.fileNovalue = -9999.0;
            reader.geodataNovalue = Double.NaN;
            reader.process();
            readCoverage = reader.outRaster;
            checkMatrixEqual(readCoverage.getRenderedImage(), HMTestMaps.mapData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
