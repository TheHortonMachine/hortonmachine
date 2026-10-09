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
package org.hortonmachine.gears.modules;

import java.awt.image.Raster;
import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.raster2xyz.OmsRaster2Xyz;
import org.hortonmachine.gears.modules.r.raster4xyz.OmsXyz2Raster;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * Test for {@link OmsRaster2Xyz} and {@link OmsXyz2Raster}: a raster written as XYZ triplets and
 * read back is the same.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestRaster2Xyz extends HMTestCase {

    private void checkRoundTrip( double[][] data, RegionMap region ) throws Exception {
        GridCoverage2D raster = CoverageUtilities.buildCoverage("elev", data, region,
                HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"), true);

        File xyzFile = File.createTempFile("hm_raster2xyz", ".xyz");
        xyzFile.deleteOnExit();
        OmsRaster2Xyz raster2Xyz = new OmsRaster2Xyz();
        raster2Xyz.pm = pm;
        raster2Xyz.inRaster = raster;
        raster2Xyz.outFile = xyzFile.getAbsolutePath();
        raster2Xyz.process();

        OmsXyz2Raster xyz2Raster = new OmsXyz2Raster();
        xyz2Raster.pm = pm;
        xyz2Raster.inFile = xyzFile.getAbsolutePath();
        xyz2Raster.pRes = region.xres;
        xyz2Raster.pCode = "EPSG:32632";
        xyz2Raster.process();

        RegionMap outRegion = CoverageUtilities.getRegionParamsFromGridCoverage(xyz2Raster.outRaster);
        assertEquals(region.rows, outRegion.rows);
        assertEquals(region.cols, outRegion.cols);
        assertEquals(region.west, outRegion.west, 1E-6);
        assertEquals(region.north, outRegion.north, 1E-6);

        Raster out = xyz2Raster.outRaster.getRenderedImage().getData();
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double expected = data[r][c];
                double value = out.getSampleDouble(c, r, 0);
                if (HMConstants.isNovalue(expected)) {
                    assertTrue(HMConstants.isNovalue(value));
                } else {
                    assertEquals(expected, value, 1E-6);
                }
            }
        }
    }

    public void testHeaderIsSkipped() throws Exception {
        File xyzFile = File.createTempFile("hm_xyz2raster", ".xyz");
        xyzFile.deleteOnExit();
        FileUtilities.writeFile("x y z\n0.5 1.5 1\n1.5 1.5 2\n0.5 0.5 3\n1.5 0.5 4\n", xyzFile);

        OmsXyz2Raster xyz2Raster = new OmsXyz2Raster();
        xyz2Raster.pm = pm;
        xyz2Raster.inFile = xyzFile.getAbsolutePath();
        xyz2Raster.pRes = 1.0;
        xyz2Raster.pCode = "EPSG:32632";
        xyz2Raster.process();

        Raster out = xyz2Raster.outRaster.getRenderedImage().getData();
        assertEquals(2, out.getWidth());
        assertEquals(2, out.getHeight());
        assertEquals(1.0, out.getSampleDouble(0, 0, 0), 1E-6);
        assertEquals(4.0, out.getSampleDouble(1, 1, 0), 1E-6);
    }

    public void testRoundTrip() throws Exception {
        double[][] data = HMTestMaps.mapData;
        checkRoundTrip(data, HMTestMaps.getEnvelopeparams());
    }

    public void testRoundTripFineResolution() throws Exception {
        // with 0.1 m cells the bounds divided by the resolution are not exact integers
        int rows = 37;
        int cols = 53;
        RegionMap region = RegionMap.fromBoundsAndGrid(650000.0, 650000.0 + cols * 0.1, 5100000.0, 5100000.0 + rows * 0.1,
                cols, rows);
        double[][] data = new double[rows][cols];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                data[r][c] = r * 100 + c;
            }
        }
        data[3][4] = HMConstants.doubleNovalue;
        checkRoundTrip(data, region);
    }
}
