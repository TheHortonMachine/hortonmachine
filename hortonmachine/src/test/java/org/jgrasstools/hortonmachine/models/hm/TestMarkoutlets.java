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

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.demmanipulation.markoutlets.Markoutlets;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Markoutlets} module.
 * 
 * @author Giuseppe Formetta ()
 */
public class TestMarkoutlets extends HMTestCase {

    public static void main( String[] args ) throws Exception {

        String base = "D:/Dropbox/hydrologis/lavori/2012_03_27_finland_forestry/data/grassdata/tm35fin/lidar/cell/";
        String in = base + "flow";
        String out = base + "mflow";

        Markoutlets moutlet = new Markoutlets();

        moutlet.inFlow = RasterReader.readRaster(in);

        moutlet.process();

        GridCoverage2D markoutletCoverage = moutlet.outFlow;
        RasterWriter.writeRaster(out, markoutletCoverage);

    }
    // public void testTca() throws Exception {
    // HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
    // CoordinateReferenceSystem crs = HMTestMaps.crs;
    //
    // double[][] flowData = HMTestMaps.flowData;
    // GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
    // envelopeParams, crs, true);
    //
    // Markoutlets moutlet = new Markoutlets();
    //
    // moutlet.inFlow = flowCoverage;
    // moutlet.pm = pm;
    //
    // moutlet.process();
    //
    // GridCoverage2D markoutletCoverage = moutlet.outFlow;
    //
    // checkMatrixEqual(markoutletCoverage.getRenderedImage(), HMTestMaps.mflowData);
    //
    // }

}