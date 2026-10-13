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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.tmsgenerator.TmsWms2Geotiff;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Envelope;

/**
 * Test for {@link TmsWms2Geotiff}, on local tiles read through file urls.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestTmsWms2Geotiff extends HMTestCase {

    // the four tiles of zoom level 1, by their Google column and row (row 0 is the north)
    private static final Color[][] COLORS = {//
            {Color.RED, Color.GREEN}, // north-west, north-east
            {Color.BLUE, Color.WHITE}}; // south-west, south-east

    public void testGoogleAndTmsSchemas() throws Exception {
        File folder = Files.createTempDirectory("hm-tms").toFile();
        try {
            // the same tiles, numbered from the north and from the south
            for( int x = 0; x < 2; x++ ) {
                for( int y = 0; y < 2; y++ ) {
                    writeTile(new File(folder, "google/1/" + x + "/" + y + ".png"), COLORS[y][x]);
                    writeTile(new File(folder, "tms/1/" + x + "/" + (1 - y) + ".png"), COLORS[y][x]);
                }
            }
            // a region that touches the four tiles
            String roiPath = new File(folder, "roi.shp").getAbsolutePath();
            OmsVectorWriter.writeVector(roiPath, FeatureUtilities.featureCollectionFromGeometry(DefaultGeographicCRS.WGS84,
                    GeometryUtilities.gf().toGeometry(new Envelope(-170, 170, -80, 80))));

            checkMosaic(folder, roiPath, "google", Variables.GOOGLE);
            checkMosaic(folder, roiPath, "tms", Variables.OGC_TMS);
        } finally {
            FileUtilities.deleteFileOrDir(folder);
        }
    }

    private void checkMosaic( File folder, String roiPath, String tilesFolder, String schema ) throws Exception {
        String outPath = new File(folder, tilesFolder + ".tif").getAbsolutePath();
        TmsWms2Geotiff tms = new TmsWms2Geotiff();
        tms.pm = pm;
        tms.inRoiPath = roiPath;
        tms.inServiceUrl = new File(folder, tilesFolder).toURI() + "ZZZ/XXX/YYY.png";
        tms.pSchemaType = schema;
        tms.pZoomlevel = 1;
        tms.outRaster = outPath;
        tms.process();

        GridCoverage2D mosaic = OmsRasterReader.readRaster(outPath);
        RegionMap region = RegionMap.fromGridGeometry(mosaic.getGridGeometry());
        double worldHalf = 20037508.342789244;
        assertEquals(-worldHalf, region.getWest(), 1E-3);
        assertEquals(worldHalf, region.getNorth(), 1E-3);
        assertEquals(512, region.getCols());
        assertEquals(512, region.getRows());

        // the north-west tile at the top left, and so on
        Raster data = mosaic.getRenderedImage().getData();
        assertColor(data, 100, 100, Color.RED);
        assertColor(data, 400, 100, Color.GREEN);
        assertColor(data, 100, 400, Color.BLUE);
        assertColor(data, 400, 400, Color.WHITE);
    }

    private void assertColor( Raster data, int x, int y, Color color ) {
        assertEquals(color.getRed(), data.getSample(x, y, 0));
        assertEquals(color.getGreen(), data.getSample(x, y, 1));
        assertEquals(color.getBlue(), data.getSample(x, y, 2));
    }

    private void writeTile( File file, Color color ) throws Exception {
        file.getParentFile().mkdirs();
        BufferedImage tile = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = tile.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 256, 256);
        g.dispose();
        ImageIO.write(tile, "png", file);
    }

}
