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

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.util.HashMap;

import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller.OmsDePitter;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsPitfiller} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDePitter extends HMTestCase {
    private static final double ND = JGTConstants.doubleNovalue;
    private static final int NI = JGTConstants.intNovalue;

    public void testPitfiller() throws Exception {
        GridCoverage2D dtm = OmsRasterReader
                .readRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/toblino/dtm_toblino.tiff");

        OmsDePitter pitfiller = new OmsDePitter();
        pitfiller.inElev = dtm;
        pitfiller.pm = pm;
        pitfiller.process();

        GridCoverage2D pitfillerCoverage = pitfiller.outPit;
//        printImage(pitfillerCoverage.getRenderedImage());
        OmsRasterWriter.writeRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/toblino/jgt_pit.tiff", pitfillerCoverage);
        OmsRasterWriter.writeRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/toblino/jgt_flow.tiff", pitfiller.outFlow);
        
    }
    // public void testPitfiller() throws Exception {
    // double[][] elevationData = new double[][]{//
    // {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
    // {600, ND, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
    // {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
    // {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
    // {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
    // {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
    // {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
    // {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500} //
    // };
    // HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
    // CoordinateReferenceSystem crs = HMTestMaps.getCrs();
    // GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
    // elevationData, envelopeParams, crs, true);
    //
    // OmsDePitter pitfiller = new OmsDePitter();
    // pitfiller.inElev = elevationCoverage;
    // pitfiller.pm = pm;
    // pitfiller.process();
    //
    // GridCoverage2D pitfillerCoverage = pitfiller.outPit;
    // printImage(pitfillerCoverage.getRenderedImage());
    // double[][] outNewPitData = new double[][]{ //
    // {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
    // {600, ND, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
    // {500, 550, 700, 750, 800.000004, 850, 900, 1000, 1100, 1500}, //
    // {400, 410, 650, 700, 750, 800.000002, 850, 800.000002, 800.000004, 1500}, //
    // {450, 550, 430, 500, 600, 700, 800, 800.000002, 800.000004, 1500}, //
    // {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
    // {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
    // {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500} //
    // };
    // checkMatrixEqual(pitfillerCoverage.getRenderedImage(), outNewPitData, 0);
    //
    // int[][] newIntFlowData = new int[][]{ //
    // {NI, NI, NI, NI, NI, NI, NI, NI, NI, NI}, //
    // {NI, NI, NI, 6, 6, 6, 6, 6, 6, NI}, //
    // {NI, NI, NI, 6, 6, 6, 6, 7, 7, NI}, //
    // {NI, 5, 5, 7, 6, 6, 6, 6, 5, NI}, //
    // {NI, 3, 4, 5, 5, 5, 5, 5, 5, NI}, //
    // {NI, 2, 3, 3, 4, 4, 4, 3, 3, NI}, //
    // {NI, 4, 4, 4, 4, 4, 5, 4, 4, NI}, //
    // {NI, NI, NI, NI, NI, NI, NI, NI, NI, NI} //
    // };
    //
    // GridCoverage2D flowCoverage = pitfiller.outFlow;
    // // printImage(flowCoverage.getRenderedImage());
    // checkMatrixEqual(flowCoverage.getRenderedImage(), newIntFlowData);
    //
    // }

    // public void testPitfiller2() throws Exception {
    // double[][] elevationData = new double[][]{
    // { 1651.0849609, 1653.8480225, 1655.6639404, 1655.8800049, 1655.3459473, 1655.5460205,
    // 1655.9090576, 1656.9279785, 1658.6870117, 1661.0300293 },
    // { 1652.1479492, 1654.8249512, 1655.6870117, 1655.3819580, 1655.0310059, 1654.9820557,
    // 1655.0069580, 1655.3750000, 1657.3979492, 1659.8929443 },
    // { 1653.2330322, 1655.2159424, 1655.8229980, 1655.2060547, 1655.0290527, 1655.0300293,
    // 1655.0310059, 1654.9060059, 1655.6650391, 1659.1159668 },
    // { 1653.0279541, 1657.0489502, 1656.6140137, 1655.0999756, 1655.1020508, 1655.1030273,
    // 1655.0999756, 1655.0810547, 1655.4739990, 1659.0260010 },
    // { 1653.5749512, 1656.3599854, 1655.6970215, 1655.3649902, 1655.2390137, 1655.1120605,
    // 1655.0109863, 1654.9749756, 1655.4969482, 1658.9169922 },
    // { 1652.8759766, 1655.1569824, 1655.5090332, 1655.1750488, 1655.1779785, 1655.1590576,
    // 1655.1219482, 1655.0119629, 1655.4820557, 1659.7419434 },
    // { 1650.8590088, 1654.1519775, 1655.1810303, 1655.1729736, 1655.1280518, 1655.0830078,
    // 1655.0379639, 1654.9110107, 1656.7679443, 1661.0780029 },
    // { 1649.5679932, 1652.1440430, 1655.2209473, 1655.3070068, 1655.4420166, 1655.3690186,
    // 1655.1810303, 1655.4969482, 1658.4449463, 1661.6729736 },
    // { 1648.3249512, 1651.2879639, 1653.4310303, 1654.6929932, 1655.3470459, 1655.3900146,
    // 1655.5109863, 1657.0830078, 1660.2889404, 1663.4310303 }
    // }; //
    //
    // HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
    // CoordinateReferenceSystem crs = HMTestMaps.getCrs();
    // GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
    // elevationData, envelopeParams, crs, true);
    //
    // OmsDePitter pitfiller = new OmsDePitter();
    // pitfiller.inElev = elevationCoverage;
    // pitfiller.pm = pm;
    // pitfiller.process();
    //
    // GridCoverage2D pitfillerCoverage = pitfiller.outPit;
    // printImage(pitfillerCoverage.getRenderedImage());
    // // double[][] outNewPitData = new double[][]{ //
    // // /* */{800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
    // // {600, -9999, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
    // // {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
    // // {400, 410, 650, 700, 750, 800, 850, 800.000002f, 800.000004f, 1500}, //
    // // {450, 550, 430, 500, 600, 700, 800, 800.000002f, 800.000004f, 1500}, //
    // // {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
    // // {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
    // // {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500} //
    // // };
    // // checkMatrixEqual(pitfillerCoverage.getRenderedImage(), outNewPitData, 0);
    //
    // GridCoverage2D flowCoverage = pitfiller.outFlow;
    // printImage(flowCoverage.getRenderedImage());
    // // checkMatrixEqual(flowCoverage.getRenderedImage(), HMTestMaps.newIntFlowData);
    //
    // }

    protected void checkMatrixEqualLimit( RenderedImage image, double[][] matrix, double delta ) {
        RectIter rectIter = RectIterFactory.create(image, null);
        int y = 0;
        do {
            int x = 0;
            do {
                double value = rectIter.getSampleDouble();
                double expectedResult = matrix[y][x];
                if (isNovalue(value)) {
                    assertTrue(x + " " + y, isNovalue(expectedResult));
                } else {
                    assertEquals(x + " " + y, expectedResult, value, delta);
                }
                x++;
            } while( !rectIter.nextPixelDone() );
            rectIter.startPixels();
            y++;
            if (y > 10) {
                break;
            }
        } while( !rectIter.nextLineDone() );
    }

}
