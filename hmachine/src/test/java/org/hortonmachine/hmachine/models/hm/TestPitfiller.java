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
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
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

        OmsPitfiller pitfiller = new OmsPitfiller();
        pitfiller.inElev = elevationCoverage;
        pitfiller.pm = pm;
        pitfiller.process();

        GridCoverage2D pitfillerCoverage = pitfiller.outPit;
        // printImage(pitfillerCoverage.getRenderedImage());
        checkMatrixEqual(pitfillerCoverage.getRenderedImage(), HMTestMaps.outPitData, 0);

    }

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
