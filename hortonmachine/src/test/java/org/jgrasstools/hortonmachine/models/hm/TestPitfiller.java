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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.libs.modules.GridNode;
import org.jgrasstools.gears.modules.r.rasterdiff.OmsRasterDiff;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.time.EggClock;
import org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller.OmsPitfiller2;
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.OmsFlowDirections;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsPitfiller} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestPitfiller extends HMTestCase {
    public void testPitfiller() throws Exception {
        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);

        OmsPitfiller2 pitfiller = new OmsPitfiller2();
        pitfiller.inElev = elevationCoverage;
        pitfiller.pm = pm;
        pitfiller.process();

        GridCoverage2D pitfillerCoverage = pitfiller.outPit;
         printImage(pitfillerCoverage.getRenderedImage());
        checkMatrixEqual(pitfillerCoverage.getRenderedImage(), HMTestMaps.outNewPitData, 0);

        GridCoverage2D flowCoverage = pitfiller.outFlow;
         printImage(flowCoverage.getRenderedImage());
        checkMatrixEqual(flowCoverage.getRenderedImage(), HMTestMaps.newIntFlowData);
    }

//    public void testPitfillerReal() throws Exception {
//        EggClock egg = new EggClock("pitflow", "");
//        egg.startAndPrint(System.out);
//
//        // GridCoverage2D elevationCoverage =
//        // OmsRasterReader.readRaster("/home/hydrologis/TMP/PITFILLE/dtm_flanginec.asc");
//        // GridCoverage2D elevationCoverage =
//        // OmsRasterReader.readRaster("/home/hydrologis/TMP/PITFILLE/dtm_flanginec_pitted.asc");
//        // GridCoverage2D elevationCoverage =
//        // OmsRasterReader.readRaster("/home/hydrologis/TMP/PITFILLE/DTM_calvello/pit_all.asc");
//        GridCoverage2D elevationCoverage = OmsRasterReader.readRaster("/home/hydrologis/TMP/PITFILLE/DTM_calvello/dtm_all.asc");
//        // GridCoverage2D elevationCoverage =
//        // OmsRasterReader.readRaster("/home/hydrologis/TMP/PITFILLE/CALVSMALL/dtm_small_01_pitted.asc");
//        // GridCoverage2D elevationCoverage =
//        // OmsRasterReader.readRaster("/home/hydrologis/TMP/PITFILLE/CALVSMALL/dtm_small_01.tif");
//
//        OmsPitfiller2 pitfiller = new OmsPitfiller2();
//        pitfiller.inElev = elevationCoverage;
//        pitfiller.pm = pm;
//        pitfiller.process();
//
//        GridCoverage2D pitfillerCoverage = pitfiller.outPit;
//        GridCoverage2D outFlow = pitfiller.outFlow;
//        
//        egg.printSubTimePassedInSeconds(System.out);
//        
//        //
//        // OmsRasterWriter.writeRaster("/home/hydrologis/TMP/PITFILLE/flow.asc", outFlow);
//        // OmsRasterWriter.writeRaster("/home/hydrologis/TMP/PITFILLE/dtm_flanginec_pitted2.asc",
//        // pitfillerCoverage);
//        // OmsRasterWriter.writeRaster("/home/hydrologis/TMP/PITFILLE/CALVSMALL/dtm_small_01_pitted2.asc",
//        // pitfillerCoverage);
//        OmsRasterWriter.writeRaster("/home/hydrologis/TMP/PITFILLE/DTM_calvello/pit_all.asc", pitfillerCoverage);
//        OmsRasterWriter.writeRaster("/home/hydrologis/TMP/PITFILLE/DTM_calvello/flow_all.asc", outFlow);
//
//        // OmsRasterDiff diff = new OmsRasterDiff();
//        // diff.inRaster1 = OmsRasterReader.readRaster("/home/hydrologis/TMP/PITFILLE/pit.asc");
//        // diff.inRaster2 = pitfillerCoverage;
//        // diff.pThreshold = 0.1;
//        // diff.process();
//        //
//        // OmsRasterWriter.writeRaster("/home/hydrologis/TMP/PITFILLE/dtm_flanginec_pit_diffs.asc",
//        // diff.outRaster);
//
//        // checkMatrixEqualLimit(pitfillerCoverage.getRenderedImage(), testMatrix, 0.01);
//    }

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
