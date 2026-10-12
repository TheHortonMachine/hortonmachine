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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * Test {@link RasterToNetcdf}, {@link NetcdfInfo} and {@link NetcdfGridDumper} together.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestNetcdfModules extends HMTestCase {

    private File workFolder;

    @Override
    protected void setUp() throws Exception {
        workFolder = FileUtilities.createTemporaryFolder("hm_netcdf_modules_test");
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(workFolder);
    }

    /**
     * Write a folder with 2 rasters, the second the first plus 100.
     */
    private File writeSeriesFolder() throws Exception {
        File folder = new File(workFolder, "series");
        folder.mkdirs();
        for( int t = 0; t < 2; t++ ) {
            double[][] data = new double[HMTestMaps.mapData.length][];
            for( int r = 0; r < data.length; r++ ) {
                data[r] = HMTestMaps.mapData[r].clone();
                for( int c = 0; c < data[r].length; c++ ) {
                    if (data[r][c] != -9999.0) {
                        data[r][c] += t * 100;
                    }
                }
            }
            GridCoverage2D raster = CoverageUtilities.buildCoverage("elev", data, HMTestMaps.envelopeParams, HMTestMaps.crs,
                    true);
            OmsRasterWriter.writeRaster(new File(folder, "elev_" + t + ".tif").getAbsolutePath(), raster);
        }
        return folder;
    }

    public void testSeriesToNetcdfAndBack() throws Exception {
        File ncFile = new File(workFolder, "series.nc");
        RasterToNetcdf toNetcdf = new RasterToNetcdf();
        toNetcdf.pm = pm;
        toNetcdf.inFolder = writeSeriesFolder().getAbsolutePath();
        toNetcdf.pTimestamps = "2024-07-01 00:00\n2024-07-02 00:00\n";
        toNetcdf.pVariableName = "elev";
        toNetcdf.pLongName = "elevation";
        toNetcdf.pUnits = "m";
        toNetcdf.outNetcdf = ncFile.getAbsolutePath();
        toNetcdf.process();
        assertTrue(ncFile.exists());

        // the numeric attributes are printed with their values
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NetcdfInfo info = new NetcdfInfo();
        info.pm = new PrintStreamProgressMonitor(new PrintStream(out, true, StandardCharsets.UTF_8), System.err);
        info.inPath = ncFile.getAbsolutePath();
        info.process();
        String infoText = out.toString(StandardCharsets.UTF_8);
        assertTrue(infoText, infoText.contains("_FillValue -> -9999.0"));
        assertTrue(infoText, infoText.contains("false_easting: [500.0]"));
        assertTrue(infoText, infoText.contains("Time Axis (time): 2024-07-01T00:00:00Z -> 2024-07-02T00:00:00Z   (2)"));

        File dumpFolder = new File(workFolder, "dump");
        dumpFolder.mkdirs();
        NetcdfGridDumper dumper = new NetcdfGridDumper();
        dumper.pm = pm;
        dumper.inPath = ncFile.getAbsolutePath();
        dumper.pIncludePattern = "ele";
        dumper.pFromTimestep = 0;
        dumper.pToTimestep = 2;
        dumper.outFolder = dumpFolder.getAbsolutePath();
        dumper.process();

        // the UTC timestamps and the grid found by the pattern name the files
        String[] names = dumpFolder.list();
        Arrays.sort(names);
        assertEquals("series__20240701_000000__elev.tif", names[0]);
        assertEquals("series__20240702_000000__elev.tif", names[1]);

        GridCoverage2D second = OmsRasterReader.readRaster(new File(dumpFolder, names[1]).getAbsolutePath());
        double[][] expected = new double[HMTestMaps.mapData.length][];
        for( int r = 0; r < expected.length; r++ ) {
            expected[r] = HMTestMaps.mapData[r].clone();
            for( int c = 0; c < expected[r].length; c++ ) {
                if (expected[r][c] != -9999.0) {
                    expected[r][c] += 100;
                }
            }
        }
        checkMatrixEqual(second.getRenderedImage(), expected, DELTA);
    }

    public void testTimestampsMustMatchRasters() throws Exception {
        RasterToNetcdf toNetcdf = new RasterToNetcdf();
        toNetcdf.pm = pm;
        toNetcdf.inFolder = writeSeriesFolder().getAbsolutePath();
        toNetcdf.pTimestamps = "2024-07-01 00:00";
        toNetcdf.outNetcdf = new File(workFolder, "wrong.nc").getAbsolutePath();
        try {
            toNetcdf.process();
            fail("Two rasters need two timestamps.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
    }
}
