/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears;

import java.awt.image.BufferedImage;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.modules.r.summary.OmsRasterSummary;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.images.ImageGenerator;

/**
 * Test {@link ImageGenerator} with in memory coverages and features.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestImageGenerator extends HMTestCase {

    private GridCoverage2D elevation() {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        return CoverageUtilities.buildCoverage("elevation", HMTestMaps.mapData, envelopeParams, crs, true);
    }

    public void testCoverageFullExtent() throws Exception {
        ImageGenerator imgGen = new ImageGenerator(null, null);
        imgGen.addCoverage(elevation(), null);
        BufferedImage image = imgGen.drawFullExtent(200, 160, 0);
        imgGen.dispose();

        assertEquals(200, image.getWidth());
        assertEquals(160, image.getHeight());
        assertTrue(countPainted(image) > 0);
    }

    public void testHMRasterFullExtent() throws Exception {
        ImageGenerator imgGen = new ImageGenerator(null, null);
        imgGen.addRaster(HMRaster.fromGridCoverage(elevation()), null);
        BufferedImage image = imgGen.drawFullExtent(100, 80, 0);
        imgGen.dispose();

        assertTrue(countPainted(image) > 0);
    }

    public void testFeaturesFullExtent() throws Exception {
        ImageGenerator imgGen = new ImageGenerator(null, null);
        imgGen.addFeatures(HMTestMaps.getTestFC(), null);
        BufferedImage image = imgGen.drawFullExtent(200, 160, 20);
        imgGen.dispose();

        assertTrue(countPainted(image) > 0);
    }

    public void testNoLayersFails() throws Exception {
        ImageGenerator imgGen = new ImageGenerator(null, null);
        try {
            imgGen.drawFullExtent(100, 100, 0);
            fail("an empty generator can't be drawn");
        } catch (IllegalStateException e) {
            // expected
        }
    }

    public void testSetLayersTwiceDoesNotDuplicate() throws Exception {
        GridCoverage2D coverage = elevation();
        double[] minMax = OmsRasterSummary.getMinMax(coverage);
        // half transparent, so a duplicated layer would change the alpha
        Style style = RasterStyleUtilities.createStyleForColortable(ImageGenerator.DEFAULT_COLORTABLE, minMax[0], minMax[1],
                0.5);

        ImageGenerator imgGen = new ImageGenerator(null, null);
        imgGen.addCoverage(coverage, style);
        BufferedImage first = imgGen.drawFullExtent(100, 80, 0);
        BufferedImage second = imgGen.drawFullExtent(100, 80, 0);
        ReferencedEnvelope extent = imgGen.setLayers();
        BufferedImage third = imgGen.drawImage(extent, 100, 80, 0);
        BufferedImage fourth = imgGen.drawImage(extent, 100, 80, 0);
        imgGen.dispose();

        int x = 50;
        int y = 40;
        assertEquals(first.getRGB(x, y), second.getRGB(x, y));
        assertEquals(third.getRGB(x, y), fourth.getRGB(x, y));
    }

    public void testEnvelopeIsNotModified() throws Exception {
        ImageGenerator imgGen = new ImageGenerator(null, null);
        imgGen.addCoverage(elevation(), null);
        ReferencedEnvelope extent = imgGen.setLayers();
        ReferencedEnvelope original = new ReferencedEnvelope(extent);

        imgGen.drawImage(extent, 100, 100, 50);
        assertEquals(original, extent);
        // not square, so the aspect ratio gets fixed internally
        imgGen.getImageWithCheck(extent, 300, 100, 0, null);
        assertEquals(original, extent);
        imgGen.getImageWithCheck(extent, 100, 300, 50, null);
        assertEquals(original, extent);
        imgGen.dispose();
    }

    private int countPainted( BufferedImage image ) {
        int count = 0;
        for( int x = 0; x < image.getWidth(); x++ ) {
            for( int y = 0; y < image.getHeight(); y++ ) {
                int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 0) {
                    count++;
                }
            }
        }
        return count;
    }

}
