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
package org.hortonmachine.gears.io.vrt;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMImageMosaic;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.locationtech.jts.geom.Envelope;

/**
 * Test {@link VrtDataset} and {@link VrtImageMosaic}.
 */
public class TestVrtImageMosaic extends HMTestCase {

    private File folder;
    private RegionMap region;
    private int half;

    @Override
    protected void setUp() throws Exception {
        folder = FileUtilities.createTemporaryFolder("hm-vrt");
        region = HMTestMaps.getEnvelopeparams();
        double[][] data = HMTestMaps.mapData;
        int rows = data.length;
        half = data[0].length / 2;
        double[][] west = new double[rows][];
        double[][] east = new double[rows][];
        for( int r = 0; r < rows; r++ ) {
            west[r] = Arrays.copyOfRange(data[r], 0, half);
            east[r] = Arrays.copyOfRange(data[r], half, data[0].length);
        }
        double middle = region.getWest() + half * region.getXres();
        RegionMap westRegion = RegionMap.fromBoundsAndGrid(region.getWest(), middle, region.getSouth(), region.getNorth(), half,
                rows);
        RegionMap eastRegion = RegionMap.fromBoundsAndGrid(middle, region.getEast(), region.getSouth(), region.getNorth(),
                data[0].length - half, rows);
        File tiles = new File(folder, "tiles");
        tiles.mkdirs();
        OmsRasterWriter.writeRaster(new File(tiles, "west.tif").getAbsolutePath(),
                CoverageUtilities.buildCoverage("west", west, westRegion, HMTestMaps.getCrs(), true));
        OmsRasterWriter.writeRaster(new File(tiles, "east.tif").getAbsolutePath(),
                CoverageUtilities.buildCoverage("east", east, eastRegion, HMTestMaps.getCrs(), true));
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(folder);
    }

    /**
     * A VRT as gdalbuildvrt writes it, with the SRS as WKT and the sources relative to the VRT.
     */
    private String mosaicVrt() {
        int cols = region.getCols();
        int rows = region.getRows();
        int eastCols = cols - half;
        return """
                <VRTDataset rasterXSize="%d" rasterYSize="%d">
                  <SRS dataAxisToSRSAxisMapping="1,2">%s</SRS>
                  <GeoTransform>%s, %s, 0.0, %s, 0.0, -%s</GeoTransform>
                  <VRTRasterBand dataType="Float64" band="1">
                    <NoDataValue>-9999</NoDataValue>
                    <ColorInterp>Gray</ColorInterp>
                    <SimpleSource>
                      <SourceFilename relativeToVRT="1">tiles/west.tif</SourceFilename>
                      <SourceBand>1</SourceBand>
                      <SourceProperties RasterXSize="%d" RasterYSize="%d" DataType="Float64" />
                      <SrcRect xOff="0" yOff="0" xSize="%d" ySize="%d" />
                      <DstRect xOff="0" yOff="0" xSize="%d" ySize="%d" />
                    </SimpleSource>
                    <ComplexSource>
                      <SourceFilename relativeToVRT="1">tiles/east.tif</SourceFilename>
                      <SourceBand>1</SourceBand>
                      <SourceProperties RasterXSize="%d" RasterYSize="%d" DataType="Float64" />
                      <SrcRect xOff="0" yOff="0" xSize="%d" ySize="%d" />
                      <DstRect xOff="%d" yOff="0" xSize="%d" ySize="%d" />
                      <ScaleRatio>1</ScaleRatio>
                      <NODATA>-9999</NODATA>
                    </ComplexSource>
                  </VRTRasterBand>
                </VRTDataset>
                """.formatted(cols, rows, HMTestMaps.getCrs().toWKT(), region.getWest(), region.getXres(), region.getNorth(),
                region.getYres(), //
                half, rows, half, rows, half, rows, //
                eastCols, rows, eastCols, rows, half, eastCols, rows);
    }

    private File writeVrt( String name, String xml ) throws Exception {
        File vrtFile = new File(folder, name);
        FileUtilities.writeFile(xml, vrtFile);
        return vrtFile;
    }

    public void testParse() throws Exception {
        File vrtFile = writeVrt("mosaic.vrt", mosaicVrt());
        VrtDataset vrt = VrtDataset.read(vrtFile);
        assertNull(vrt.subClass);
        assertEquals(region.getCols(), vrt.width);
        assertEquals(region.getRows(), vrt.height);
        assertTrue(vrt.isNorthUp());
        assertEquals(region.getWest(), vrt.geoTransform[0], DELTA);
        assertEquals(1, vrt.bands.size());
        VrtDataset.Band band = vrt.bands.get(0);
        assertEquals(-9999.0, band.noData(), DELTA);
        assertEquals(2, band.sources().size());
        VrtDataset.Source east = band.sources().get(1);
        assertEquals("ComplexSource", east.kind());
        assertEquals(new File(folder, "tiles/east.tif").getAbsolutePath(), east.path());
        assertEquals(1, east.sourceBand());
        assertEquals(-9999.0, east.noData(), DELTA);
        // a ratio of 1 changes nothing
        assertFalse(east.transformsValues());

        Envelope footprint = VrtImageMosaic.footprint(vrt, east);
        assertEquals(region.getWest() + half * region.getXres(), footprint.getMinX(), DELTA);
        assertEquals(region.getEast(), footprint.getMaxX(), DELTA);
        assertEquals(region.getSouth(), footprint.getMinY(), DELTA);
        assertEquals(region.getNorth(), footprint.getMaxY(), DELTA);

        assertTrue(VrtImageMosaic.checkSupported(vrt).isEmpty());
    }

    /**
     * The VRT is read whole as a grid coverage through the image mosaic reader, as any mosaic.
     */
    public void testReadAsImageMosaic() throws Exception {
        File vrtFile = writeVrt("mosaic.vrt", mosaicVrt());
        File shpFile = VrtImageMosaic.getIndex(vrtFile);
        assertEquals(new File(folder, "mosaic" + VrtImageMosaic.INDEX_FOLDER_SUFFIX + "/mosaic.shp"), shpFile);

        ImageMosaicReader reader = new ImageMosaicFormat().getReader(shpFile);
        try {
            GridCoverage2D mosaic = reader.read(null);
            RegionMap mosaicRegion = RegionMap.fromGridGeometry(mosaic.getGridGeometry());
            assertEquals(region.getCols(), mosaicRegion.getCols());
            assertEquals(region.getRows(), mosaicRegion.getRows());
            assertEquals(region.getWest(), mosaicRegion.getWest(), DELTA);
            assertEquals(region.getNorth(), mosaicRegion.getNorth(), DELTA);
            checkMatrixEqual(mosaic.getRenderedImage(), HMTestMaps.mapData, DELTA);
        } finally {
            reader.dispose();
        }

        // made once, used again while the VRT doesn't change
        long made = shpFile.lastModified();
        assertEquals(shpFile, VrtImageMosaic.getIndex(vrtFile));
        assertEquals(made, shpFile.lastModified());
    }

    /**
     * The VRT path is given straight to HMImageMosaic, which reads windows of it.
     */
    public void testHMImageMosaicOnVrt() throws Exception {
        File vrtFile = writeVrt("mosaic.vrt", mosaicVrt());
        double[][] data = HMTestMaps.mapData;
        try (HMImageMosaic mosaic = new HMImageMosaic(vrtFile.getAbsolutePath())) {
            // a window across the two tiles
            int startCol = half - 2;
            int cols = 4;
            int rows = 3;
            Envelope window = new Envelope(region.getWest() + startCol * region.getXres(),
                    region.getWest() + (startCol + cols) * region.getXres(), region.getNorth() - rows * region.getYres(),
                    region.getNorth());
            HMRaster raster = mosaic.read(window);
            assertEquals(cols, raster.getCols());
            assertEquals(rows, raster.getRows());
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double expected = data[r][startCol + c];
                    double value = raster.getValue(c, r);
                    if (Double.isNaN(expected)) {
                        assertTrue(raster.isNovalue(value));
                    } else {
                        assertEquals(expected, value, DELTA);
                    }
                }
            }
        }
    }

    /**
     * The bands of gdalbuildvrt -separate come from different files: refused with the reason.
     */
    public void testSeparateBandsAreRefused() throws Exception {
        String xml = mosaicVrt();
        String secondBand = """
                  <VRTRasterBand dataType="Float64" band="2">
                    <SimpleSource>
                      <SourceFilename relativeToVRT="1">tiles/east.tif</SourceFilename>
                      <SourceBand>1</SourceBand>
                    </SimpleSource>
                  </VRTRasterBand>
                </VRTDataset>
                """;
        xml = xml.replace("</VRTDataset>\n", secondBand);
        File vrtFile = writeVrt("separate.vrt", xml);
        VrtDataset vrt = VrtDataset.read(vrtFile);
        List<String> problems = VrtImageMosaic.checkSupported(vrt);
        assertFalse(problems.isEmpty());
        try {
            VrtImageMosaic.getIndex(vrtFile);
            fail("A VRT with bands from different files is not a mosaic.");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("band 2"));
        }
        assertFalse(new File(folder, "separate" + VrtImageMosaic.INDEX_FOLDER_SUFFIX).exists());
    }

    public void testWarpedAndScaledAreRefused() throws Exception {
        String xml = mosaicVrt().replace("<VRTDataset ", "<VRTDataset subClass=\"VRTWarpedDataset\" ")
                .replace("<ScaleRatio>1</ScaleRatio>", "<ScaleRatio>0.5</ScaleRatio>");
        VrtDataset vrt = VrtDataset.parse(xml, new File(folder, "warped.vrt"));
        List<String> problems = VrtImageMosaic.checkSupported(vrt);
        assertEquals(problems.toString(), 2, problems.size());
        assertTrue(problems.get(0).contains("VRTWarpedDataset"));
        assertTrue(problems.get(1).contains("transformed"));
    }
}
