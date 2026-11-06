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

import java.awt.image.Raster;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;

/**
 * Tests the {@link OmsCurvaturesBivariate} module on surfaces with known derivatives.
 *
 * <p>The cells are not square, 10 m along x and 5 m along y, to check that the fit is in map units.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestCurvaturesBivariate extends HMTestCase {

    private static final int SIZE = 11;
    private static final double XRES = 10;
    private static final double YRES = 5;
    private static final int CENTER = SIZE / 2;

    private interface Surface {
        double z( double x, double y );
    }

    /**
     * Build the elevation from a surface of the offsets in meters from the centre cell, x towards
     * east and y towards north.
     */
    private GridCoverage2D elevation( Surface surface ) {
        double[][] data = new double[SIZE][SIZE];
        for( int r = 0; r < SIZE; r++ ) {
            for( int c = 0; c < SIZE; c++ ) {
                data[r][c] = 1000 + surface.z((c - CENTER) * XRES, (CENTER - r) * YRES);
            }
        }
        RegionMap region = RegionMap.fromBoundsAndGrid(0, SIZE * XRES, 0, SIZE * YRES, SIZE, SIZE);
        return CoverageUtilities.buildCoverage("elev", data, region, HMTestMaps.getCrs(), true);
    }

    private OmsCurvaturesBivariate run( GridCoverage2D elev, int cells ) throws Exception {
        OmsCurvaturesBivariate curvatures = new OmsCurvaturesBivariate();
        curvatures.inElev = elev;
        curvatures.pCells = cells;
        curvatures.pm = pm;
        curvatures.process();
        return curvatures;
    }

    private double value( GridCoverage2D coverage, int col, int row ) {
        Raster raster = coverage.getRenderedImage().getData();
        return raster.getSampleDouble(col, row, 0);
    }

    public void testPlane() throws Exception {
        // rising 0.1 towards east and 0.05 towards north: the slope faces south-west
        OmsCurvaturesBivariate result = run(elevation(( x, y ) -> 0.1 * x + 0.05 * y), 3);

        double expectedSlope = Math.toDegrees(Math.atan(Math.sqrt(0.1 * 0.1 + 0.05 * 0.05)));
        double expectedAspect = 360 + Math.toDegrees(Math.atan2(-0.1, -0.05));
        assertEquals(expectedSlope, value(result.outSlope, CENTER, CENTER), 1E-9);
        assertEquals(expectedAspect, value(result.outAspect, CENTER, CENTER), 1E-9);
        assertEquals(0.0, value(result.outProf, CENTER, CENTER), 1E-12);
        assertEquals(0.0, value(result.outPlan, CENTER, CENTER), 1E-12);

        // the border cells have an incomplete window
        assertTrue(HMConstants.isNovalue(value(result.outSlope, 0, CENTER)));
        assertTrue(HMConstants.isNovalue(value(result.outProf, CENTER, 0)));
        assertFalse(HMConstants.isNovalue(value(result.outSlope, 1, 1)));
    }

    public void testBowl() throws Exception {
        // a bowl around the centre: z = (x² + y²) / 200, so zxx = zyy = 0.01
        GridCoverage2D elev = elevation(( x, y ) -> (x * x + y * y) / 200.0);
        // the fit of a quadratic surface is exact for any window
        for( int cells : new int[]{3, 5} ) {
            OmsCurvaturesBivariate result = run(elev, cells);

            // two cells east of the centre, x0 = 20 m: the slope faces west, towards the centre
            double x0 = 2 * XRES;
            double gradient = x0 / 100.0;
            int col = CENTER + 2;
            assertEquals(Math.toDegrees(Math.atan(gradient)), value(result.outSlope, col, CENTER), 1E-9);
            assertEquals(270.0, value(result.outAspect, col, CENTER), 1E-9);
            // concave profile: the slope decreases going down towards the centre
            assertEquals(0.01 / Math.pow(1 + gradient * gradient, 1.5), value(result.outProf, col, CENTER), 1E-9);
            // converging contour lines, circles of radius x0
            assertEquals(1.0 / x0, value(result.outPlan, col, CENTER), 1E-9);

            // two cells north of the centre, y0 = 10 m: the slope faces south
            int row = CENTER - 2;
            assertEquals(180.0, value(result.outAspect, CENTER, row), 1E-9);
            assertEquals(1.0 / (2 * YRES), value(result.outPlan, CENTER, row), 1E-9);
        }
    }

    public void testRidge() throws Exception {
        // a ridge along y, falling towards east and west and down towards north
        OmsCurvaturesBivariate result = run(elevation(( x, y ) -> -x * x / 200.0 - 0.2 * y), 3);
        // beside the ridge line the flow diverges
        assertTrue(value(result.outPlan, CENTER + 1, CENTER) < 0);
        // on the ridge line the steepest slope runs along y, where the surface is straight
        assertEquals(0.0, value(result.outProf, CENTER, CENTER), 1E-12);
        // facing north: 0 or, by rounding, just below 360
        double aspect = value(result.outAspect, CENTER, CENTER);
        assertEquals(0.0, Math.min(aspect, 360 - aspect), 1E-9);
    }

    public void testFlatAndNovalue() throws Exception {
        double[][] data = new double[SIZE][SIZE];
        for( double[] row : data ) {
            java.util.Arrays.fill(row, 500.0);
        }
        data[3][3] = HMConstants.doubleNovalue;
        RegionMap region = RegionMap.fromBoundsAndGrid(0, SIZE * XRES, 0, SIZE * YRES, SIZE, SIZE);
        GridCoverage2D elev = CoverageUtilities.buildCoverage("elev", data, region, HMTestMaps.getCrs(), true);

        OmsCurvaturesBivariate result = run(elev, 5);
        // flat: no slope nor curvature, undefined aspect
        assertEquals(0.0, value(result.outSlope, 8, 8), 0);
        assertEquals(0.0, value(result.outProf, 8, 8), 0);
        assertEquals(0.0, value(result.outPlan, 8, 8), 0);
        assertTrue(HMConstants.isNovalue(value(result.outAspect, 8, 8)));
        // the no-data cell and the cells with it in their 5x5 window
        assertTrue(HMConstants.isNovalue(value(result.outSlope, 3, 3)));
        assertTrue(HMConstants.isNovalue(value(result.outSlope, 5, 5)));
        assertFalse(HMConstants.isNovalue(value(result.outSlope, 6, 6)));
    }

    public void testWindowSize() throws Exception {
        GridCoverage2D elev = elevation(( x, y ) -> 0.1 * x);
        try {
            run(elev, 4);
            fail("An even window has no centre cell.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
    }
}
