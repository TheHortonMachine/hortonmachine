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
package org.hortonmachine.hmachine.models.hm;

import java.awt.image.RenderedImage;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.nabla.OmsNabla;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;

/**
 * Test {@link OmsNabla} on paraboloids, whose laplacian is known.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestNabla extends HMTestCase {

    /**
     * z = a * (x² + y²) has a laplacian of 4a everywhere.
     */
    private static double[][] paraboloid( RegionMap region, double a ) {
        double[][] data = new double[region.rows][region.cols];
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double x = (c - region.cols / 2.0) * region.xres;
                double y = (r - region.rows / 2.0) * region.yres;
                data[r][c] = 1000 + a * (x * x + y * y);
            }
        }
        return data;
    }

    private GridCoverage2D nabla( double[][] data, Double threshold ) throws Exception {
        RegionMap region = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        OmsNabla nabla = new OmsNabla();
        nabla.inElev = CoverageUtilities.buildCoverage("elevation", data, region, crs, true);
        nabla.pThreshold = threshold;
        nabla.pm = pm;
        nabla.process();
        return nabla.outNabla;
    }

    public void testNablaOfParaboloid() throws Exception {
        RegionMap region = HMTestMaps.getEnvelopeparams();
        double a = 0.001;
        RenderedImage nablaImage = nabla(paraboloid(region, a), null).getRenderedImage();

        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double value = nablaImage.getData().getSampleDouble(c, r, 0);
                if (r == 0 || c == 0 || r == region.rows - 1 || c == region.cols - 1) {
                    assertTrue(HMConstants.isNovalue(value));
                } else {
                    assertEquals(4 * a, value, 1E-9);
                }
            }
        }
    }

    public void testNablaNextToNovalues() throws Exception {
        RegionMap region = HMTestMaps.getEnvelopeparams();
        double a = 0.001;
        double[][] data = paraboloid(region, a);
        data[4][5] = HMConstants.doubleNovalue;
        RenderedImage nablaImage = nabla(data, null).getRenderedImage();

        assertTrue(HMConstants.isNovalue(nablaImage.getData().getSampleDouble(5, 4, 0)));
        // the cells around the novalue get the mean of their neighbours
        for( int r = 3; r <= 5; r++ ) {
            for( int c = 4; c <= 6; c++ ) {
                if (r != 4 || c != 5) {
                    assertEquals(4 * a, nablaImage.getData().getSampleDouble(c, r, 0), 1E-9);
                }
            }
        }
    }

    public void testNablaMask() throws Exception {
        RegionMap region = HMTestMaps.getEnvelopeparams();
        double threshold = 0.001;

        // concave: nabla 0.004 is not below -threshold
        RenderedImage concave = nabla(paraboloid(region, 0.001), threshold).getRenderedImage();
        assertEquals(0.0, concave.getData().getSampleDouble(5, 4, 0), 0.0);

        // convex: nabla -0.004 is below -threshold
        RenderedImage convex = nabla(paraboloid(region, -0.001), threshold).getRenderedImage();
        assertEquals(1.0, convex.getData().getSampleDouble(5, 4, 0), 0.0);
    }
}
