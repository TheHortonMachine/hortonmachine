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
package org.jgrasstools.hortonmachine.models.hm;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
import org.jgrasstools.hortonmachine.modules.network.strahler.Strahler;
import org.jgrasstools.hortonmachine.utils.HMTestCase;

/**
 * Test the {@link Strahler} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestStrahler extends HMTestCase {

    public void testStrahler() throws Exception {
        GridCoverage2D flowCoverage = RasterReader.readRaster("D:\\TMP\\TESTSTRAHLER\\a.cravinaie_drain.asc");
        GridCoverage2D netCoverage = RasterReader.readRaster("D:\\TMP\\TESTSTRAHLER\\a.cravinaie_net200.asc");

        Strahler strahler = new Strahler();
        strahler.inFlow = flowCoverage;
        strahler.inNet = netCoverage;
        strahler.pm = pm;
        strahler.process();
        GridCoverage2D outStrahler = strahler.outStrahler;

        RasterWriter.writeRaster("D:\\TMP\\TESTSTRAHLER\\a.cravinaie_strahler.asc", outStrahler);
    }
    // public void testStrahler() throws Exception {
    // HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
    // CoordinateReferenceSystem crs = HMTestMaps.crs;
    // double[][] flowData = HMTestMaps.mflowDataBorder;
    // GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
    // envelopeParams, crs, true);
    // double[][] netData = HMTestMaps.extractNet1Data;
    // GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams,
    // crs, true);
    //
    // Strahler strahler = new Strahler();
    // strahler.inFlow = flowCoverage;
    // strahler.inNet = netCoverage;
    // strahler.pm = pm;
    // strahler.process();
    // GridCoverage2D outStrahler = strahler.outStrahler;
    //
    // RenderedImage renderedImage = outStrahler.getRenderedImage();
    // // printImage(renderedImage);
    // checkMatrixEqual(renderedImage, HMTestMaps.strahlerData, 0.000001);
    // }

}
