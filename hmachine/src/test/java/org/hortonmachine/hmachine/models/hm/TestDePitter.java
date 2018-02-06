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
package org.hortonmachine.hmachine.models.hm;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.util.HashMap;

import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.time.EggClock;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsDePitter;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsPitfiller} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestDePitter extends HMTestCase {
    private static final double ND = HMConstants.doubleNovalue;
    private static final int NI = HMConstants.intNovalue;

//    public void testPitfiller() throws Exception {
//
//        EggClock egg = new EggClock("", "");
//        egg.startAndPrint(System.out);
//
//        GridCoverage2D dtm = OmsRasterReader
//                // .readRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/DTM_calvello/dtm_all.tiff");
//                .readRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/toblino/dtm_toblino_filled.tiff");
//
//        OmsDePitter pitfiller = new OmsDePitter();
//        pitfiller.inElev = dtm;
//        pitfiller.pm = pm;
//        pitfiller.process();
//
//        GridCoverage2D pitfillerCoverage = pitfiller.outPit;
//        // printImage(pitfillerCoverage.getRenderedImage());
//        // OmsRasterWriter.writeRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/DTM_calvello/jgt_pit_parallel.tiff",
//        // pitfillerCoverage);
//        // OmsRasterWriter.writeRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/DTM_calvello/jgt_flow_parallel.tiff",
//        // pitfiller.outFlow);
//        OmsRasterWriter.writeRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/toblino/jgt_pit_filled.tiff",
//                pitfillerCoverage);
//        OmsRasterWriter.writeRaster("/home/hydrologis/Dropbox/hydrologis/lavori/2017_06_mapzone/test/toblino/jgt_flow_filled.tiff",
//                pitfiller.outFlow);
//
//        egg.printTimePassedInMinutes(System.out);
//
//    }
//     public void testPitfiller() throws Exception {
//     double[][] elevationData = new double[][]{//
//     {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
//     {600, ND, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
//     {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
//     {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
//     {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
//     {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
//     {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
//     {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500} //
//     };
//     HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//     CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//     GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
//     elevationData, envelopeParams, crs, true);
//    
//     OmsDePitter pitfiller = new OmsDePitter();
//     pitfiller.inElev = elevationCoverage;
//     pitfiller.pm = pm;
//     pitfiller.process();
//    
//     GridCoverage2D pitfillerCoverage = pitfiller.outPit;
//     // printImage(pitfillerCoverage.getRenderedImage());
//     double[][] outNewPitData = new double[][]{ //
//     {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
//     {600, ND, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
//     {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
//     {400, 410, 650, 700, 750, 800, 850, 800.000002, 800.000004, 1500}, //
//     {450, 550, 430, 500, 600, 700, 800, 800.000002, 800.000004, 1500}, //
//     {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
//     {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
//     {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500} //
//     };
//     checkMatrixEqual(pitfillerCoverage.getRenderedImage(), outNewPitData, 0);
//    
//     int[][] newIntFlowData = new int[][]{ //
//     {NI, NI, NI, NI, NI, NI, NI, NI, NI, NI}, //
//     {NI, NI, NI, 6, 6, 6, 6, 6, 6, NI}, //
//     {NI, NI, NI, 6, 6, 6, 6, 7, 7, NI}, //
//     {NI, 5, 5, 7, 6, 6, 6, 6, 5, NI}, //
//     {NI, 3, 4, 5, 5, 5, 5, 5, 5, NI}, //
//     {NI, 2, 3, 3, 4, 4, 4, 3, 3, NI}, //
//     {NI, 4, 4, 4, 4, 4, 5, 4, 4, NI}, //
//     {NI, NI, NI, NI, NI, NI, NI, NI, NI, NI} //
//     };
//    
//     GridCoverage2D flowCoverage = pitfiller.outFlow;
//     // printImage(flowCoverage.getRenderedImage());
//     checkMatrixEqual(flowCoverage.getRenderedImage(), newIntFlowData);
//    
//     }

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
