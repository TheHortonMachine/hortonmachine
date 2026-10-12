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
package org.hortonmachine.modules;

import java.io.File;
import java.nio.file.Files;

import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * Test {@link RasterCompare}.
 */
public class TestRasterCompare extends HMTestCase {

    public void testCompare() throws Exception {
        double[][] data = HMTestMaps.mapData;
        double[][] changed = new double[data.length][];
        for( int r = 0; r < data.length; r++ ) {
            changed[r] = data[r].clone();
        }
        changed[2][3] += 0.005; // within a tolerance of 0.01
        changed[4][5] += 2.0;
        changed[6][7] = HMConstants.doubleNovalue;

        File folder = Files.createTempDirectory("hm-rcompare").toFile();
        try {
            String path1 = write(folder, "r1", data, HMTestMaps.envelopeParams);
            String path2 = write(folder, "r2", changed, HMTestMaps.envelopeParams);

            RasterCompare compare = compare(path1, path1, 0);
            assertTrue(compare.outSameGrid);
            assertEquals(0L, compare.outDifferent.longValue());

            compare = compare(path1, path2, 0);
            assertEquals(3L, compare.outDifferent.longValue());
            assertEquals(2.0, compare.outMaxDifference, 1E-9);

            compare = compare(path1, path2, 0.01);
            assertEquals(2L, compare.outDifferent.longValue());

            // a grid shifted by one cell is not compared
            RegionMap e = HMTestMaps.envelopeParams;
            RegionMap shifted = RegionMap.fromBoundsAndGrid(e.getWest() + e.getXres(), e.getEast() + e.getXres(), e.getSouth(),
                    e.getNorth(), e.getCols(), e.getRows());
            String path3 = write(folder, "r3", data, shifted);
            compare = compare(path1, path3, 0);
            assertFalse(compare.outSameGrid);
            assertNull(compare.outDifferent);
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    private RasterCompare compare( String path1, String path2, double tolerance ) throws Exception {
        RasterCompare compare = new RasterCompare();
        compare.inRaster1 = path1;
        compare.inRaster2 = path2;
        compare.pTolerance = tolerance;
        compare.process();
        return compare;
    }

    private String write( File folder, String name, double[][] data, RegionMap region ) throws Exception {
        String path = new File(folder, name + ".tif").getAbsolutePath();
        OmsRasterWriter.writeRaster(path, CoverageUtilities.buildCoverage(name, data, region, HMTestMaps.crs, true));
        return path;
    }
}
