/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.modules;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
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
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
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
            reader.process();
            GridCoverage2D readCoverage = reader.outRaster;
            double[][] mapData = new double[][]{//
                    {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
                    {600, -9999.0, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
                    {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
                    {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
                    {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
                    {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
                    {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
                    {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};
            checkMatrixEqual(readCoverage.getRenderedImage(), mapData);

            writer = new OmsRasterWriter();
            writer.inRaster = coverage;
            writer.file = grassPath;
            writer.process();

            reader = new OmsRasterReader();
            reader.file = grassPath;
            reader.process();
            readCoverage = reader.outRaster;
            checkMatrixEqual(readCoverage.getRenderedImage(), HMTestMaps.mapData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
