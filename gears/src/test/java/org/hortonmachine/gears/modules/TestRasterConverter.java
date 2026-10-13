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

import java.awt.image.DataBuffer;
import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.rasterconverter.OmsRasterConverter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

/**
 * Test for {@link OmsRasterConverter}
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestRasterConverter extends HMTestCase {
    private static final double NV = HMConstants.doubleNovalue;

    private GridCoverage2D raster;

    protected void setUp() throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 3, 0, 2, 3, 2);
        // a novalue that isn't the one of the HortonMachine
        raster = CoverageUtilities.buildCoverageWithNovalue("raster", new double[][]{//
                {1.4, 1.6, -32768}, //
                {-2.5, 3E10, 1.123456789}}, region, HMTestMaps.getCrs(), true, -32768);
    }

    public void testInteger() throws Exception {
        GridCoverage2D out = convert(Variables.TYPE_INT);

        assertEquals(DataBuffer.TYPE_INT, out.getRenderedImage().getSampleModel().getDataType());
        assertEquals(NV, HMConstants.getNovalue(out), 0);
        // rounded to the nearest, the novalue and the too large value become novalue
        double[][] expected = new double[][]{//
                {1, 2, NV}, //
                {-2, NV, 1}};
        checkMatrixEqual(out.getRenderedImage(), expected, 0);
    }

    public void testByte() throws Exception {
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 3, 0, 2, 3, 2);
        GridCoverage2D classes = CoverageUtilities.buildCoverage("classes", new double[][]{//
                {0, 3.6, NV}, //
                {254, 255, -1}}, region, HMTestMaps.getCrs(), true);
        OmsRasterConverter converter = new OmsRasterConverter();
        converter.pm = pm;
        converter.inRaster = classes;
        converter.pOutType = Variables.TYPE_BYTE;
        converter.process();
        GridCoverage2D out = converter.outRaster;

        assertEquals(DataBuffer.TYPE_BYTE, out.getRenderedImage().getSampleModel().getDataType());
        assertEquals(255, HMConstants.getNovalue(out), 0);
        // 255 and the negatives don't fit in 0-254 and become novalue
        double[][] expected = new double[][]{//
                {0, 4, 255}, //
                {254, 255, 255}};
        checkMatrixEqual(out.getRenderedImage(), expected, 0);

        // the novalue survives a GeoTIFF
        File tiff = File.createTempFile("hm-byte", ".tif");
        try {
            OmsRasterWriter.writeRaster(tiff.getAbsolutePath(), out);
            GridCoverage2D read = OmsRasterReader.readRaster(tiff.getAbsolutePath());
            assertEquals(255, HMConstants.getNovalue(read), 0);
            checkMatrixEqual(read.getRenderedImage(), expected, 0);
        } finally {
            tiff.delete();
        }
    }

    public void testShort() throws Exception {
        GridCoverage2D out = convert(Variables.TYPE_SHORT);

        assertEquals(DataBuffer.TYPE_SHORT, out.getRenderedImage().getSampleModel().getDataType());
        assertEquals(NV, HMConstants.getNovalue(out), 0);
        double[][] expected = new double[][]{//
                {1, 2, NV}, //
                {-2, NV, 1}};
        checkMatrixEqual(out.getRenderedImage(), expected, 0);
    }

    public void testFloat() throws Exception {
        GridCoverage2D out = convert(Variables.TYPE_FLOAT);

        assertEquals(DataBuffer.TYPE_FLOAT, out.getRenderedImage().getSampleModel().getDataType());
        // the values keep the float precision, about 7 significant digits
        double[][] expected = new double[][]{//
                {1.4f, 1.6f, NV}, //
                {-2.5f, 3E10f, 1.123456789f}};
        checkMatrixEqual(out.getRenderedImage(), expected, 0);
    }

    public void testNoType() throws Exception {
        GridCoverage2D out = convert(null);
        assertSame(raster, out);
    }

    private GridCoverage2D convert( String type ) throws Exception {
        OmsRasterConverter converter = new OmsRasterConverter();
        converter.pm = pm;
        converter.inRaster = raster;
        converter.pOutType = type;
        converter.process();
        return converter.outRaster;
    }

}
