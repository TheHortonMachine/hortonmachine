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

import java.io.File;
import java.nio.file.Files;

import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetRasterHandler;
import org.hortonmachine.gears.io.stac.assets.handlers.AsciiGridHandler;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests of the handler of the ESRI ASCII grid assets.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestStacAsciiGridHandler extends HMTestCase {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static HMStacAsset asset( String href, String type, Integer epsg ) {
        ObjectNode node = MAPPER.createObjectNode().put("href", href);
        if (type != null) {
            node.put("type", type);
        }
        if (epsg != null) {
            node.put("proj:code", "EPSG:" + epsg);
        }
        return new HMStacAsset("data", node);
    }

    public void testRecognized() throws Exception {
        // by the extension of the href, also signed
        assertTrue(asset("https://host/api/data/files/a/dem.asc?expires=1&signature=x", "text/plain", null)
                .getHandler() instanceof AsciiGridHandler);
        assertTrue(asset("https://host/dem.ASC", null, null).getHandler() instanceof AsciiGridHandler);
        assertFalse(asset("https://host/notes.txt", "text/plain", null).isValid());
        assertFalse(asset("https://host/dem.asc", "image/png", null).isValid());
    }

    public void testRead() throws Exception {
        File folder = Files.createTempDirectory("hm-asc-test").toFile();
        try {
            File grid = new File(folder, "dem.asc");
            RegionMap region = HMTestMaps.getEnvelopeparams();
            OmsRasterWriter.writeRaster(grid.getAbsolutePath(),
                    CoverageUtilities.buildCoverage("dem", HMTestMaps.mapData, region, HMTestMaps.getCrs(), true));
            // the projection comes from the asset, as when the grid alone is downloaded
            new File(folder, "dem.prj").delete();

            HMStacAsset asset = asset(grid.toURI().toString(), "text/plain", 32632);
            assertTrue(asset.getNonValidReason(), asset.isValid());
            IHMStacAssetRasterHandler handler = (IHMStacAssetRasterHandler) asset.getHandler();

            GridCoverage2D coverage = handler.readRaster(null);
            RegionMap read = RegionMap.fromGridGeometry(coverage.getGridGeometry());
            assertEquals(region.getCols(), read.getCols());
            assertEquals(region.getRows(), read.getRows());
            assertEquals(region.getWest(), read.getWest(), 1e-6);
            assertEquals("EPSG:32632", HMCrsRegistry.getCodeFromCrs(coverage.getCoordinateReferenceSystem()));
            double[] value = coverage.evaluate((Position) new Position2D(coverage.getCoordinateReferenceSystem(),
                    region.getWest() + region.getXres() / 2, region.getNorth() - region.getYres() / 2), new double[1]);
            assertEquals(HMTestMaps.mapData[0][0], value[0], 1e-6);

            // a part of it
            RegionMap half = HMTestMaps.getEnvelopeparamsLeftHalf();
            RegionMap readHalf = RegionMap.fromGridGeometry(handler.readRaster(half).getGridGeometry());
            assertEquals(half.getCols(), readHalf.getCols());
            assertEquals(half.getEast(), readHalf.getEast(), 1e-6);

            assertTrue(handler.read(File.class, null).isFile());
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }
}
