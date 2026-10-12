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

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.modules.r.imagemosaic.OmsImageMosaicCreator;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * Test {@link OmsImageMosaicCreator}.
 */
public class TestImageMosaicCreator extends HMTestCase {

    /**
     * The test map split in a west and an east tile is read back whole from the mosaic.
     */
    public void testMosaicOfTwoTiles() throws Exception {
        double[][] data = HMTestMaps.mapData;
        RegionMap region = HMTestMaps.getEnvelopeparams();
        int rows = data.length;
        int half = data[0].length / 2;
        double[][] west = new double[rows][];
        double[][] east = new double[rows][];
        for( int r = 0; r < rows; r++ ) {
            west[r] = Arrays.copyOfRange(data[r], 0, half);
            east[r] = Arrays.copyOfRange(data[r], half, data[0].length);
        }
        double middle = region.getWest() + half * region.getXres();
        RegionMap westRegion = RegionMap.fromBoundsAndGrid(region.getWest(), middle, region.getSouth(), region.getNorth(), half,
                rows);
        RegionMap eastRegion = RegionMap.fromBoundsAndGrid(middle, region.getEast(), region.getSouth(), region.getNorth(), half,
                rows);

        File folder = Files.createTempDirectory("hm-imagemosaic").toFile();
        try {
            OmsRasterWriter.writeRaster(new File(folder, "west.tif").getAbsolutePath(),
                    CoverageUtilities.buildCoverage("west", west, westRegion, HMTestMaps.getCrs(), true));
            OmsRasterWriter.writeRaster(new File(folder, "east.tif").getAbsolutePath(),
                    CoverageUtilities.buildCoverage("east", east, eastRegion, HMTestMaps.getCrs(), true));

            OmsImageMosaicCreator creator = new OmsImageMosaicCreator();
            creator.pm = pm;
            creator.inFolder = folder.getAbsolutePath();
            creator.process();

            String name = folder.getName();
            assertTrue(new File(folder, name + ".shp").exists());
            assertTrue(new File(folder, name + ".properties").exists());

            ImageMosaicReader reader = new ImageMosaicFormat().getReader(folder);
            try {
                GridCoverage2D mosaic = reader.read();
                RegionMap mosaicRegion = RegionMap.fromGridGeometry(mosaic.getGridGeometry());
                assertEquals(region.getCols(), mosaicRegion.getCols());
                assertEquals(region.getRows(), mosaicRegion.getRows());
                assertEquals(region.getWest(), mosaicRegion.getWest(), DELTA);
                assertEquals(region.getNorth(), mosaicRegion.getNorth(), DELTA);
                checkMatrixEqual(mosaic.getRenderedImage(), data, DELTA);
            } finally {
                reader.dispose();
            }

            // an image added later is not in the existing index
            OmsRasterWriter.writeRaster(new File(folder, "west2.tif").getAbsolutePath(),
                    CoverageUtilities.buildCoverage("west2", west, westRegion, HMTestMaps.getCrs(), true));
            creator = new OmsImageMosaicCreator();
            creator.pm = pm;
            creator.inFolder = folder.getAbsolutePath();
            creator.process();
            assertEquals(2, OmsVectorReader.readVector(new File(folder, name + ".shp").getAbsolutePath()).size());
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    public void testFolderWithoutImages() throws Exception {
        File folder = Files.createTempDirectory("hm-imagemosaic").toFile();
        try {
            OmsImageMosaicCreator creator = new OmsImageMosaicCreator();
            creator.pm = pm;
            creator.inFolder = folder.getAbsolutePath();
            try {
                creator.process();
                fail("A folder without images gives no mosaic.");
            } catch (Exception e) {
                // expected
            }
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }
}
