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
package org.hortonmachine.gears.io.netcdf;

import java.awt.image.Raster;
import java.io.File;

import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.utils.HMTestCase;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * Test {@link OmsNetcdfTemporalAggregator}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestNetcdfTemporalAggregator extends HMTestCase {

    private static final double NV = -9999.0;
    private static final float FILL = -1f;

    private File ncFile;

    @Override
    protected void setUp() throws Exception {
        ncFile = File.createTempFile("hm_netcdf_aggregator_test", ".nc");
        writeGrid();
    }

    @Override
    protected void tearDown() throws Exception {
        ncFile.delete();
    }

    /**
     * Write a lat/long grid of 1 row and 3 columns, latitudes from south to north, and 4 days:
     * <ul>
     * <li>column 0: 1, 2, 3, 6</li>
     * <li>column 1: 4, fill, fill, 8 (half of the days missing)</li>
     * <li>column 2: always fill</li>
     * </ul>
     */
    private void writeGrid() throws Exception {
        float[] data = {//
                1, 4, FILL, //
                2, FILL, FILL, //
                3, FILL, FILL, //
                6, 8, FILL};
        NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(ncFile.getAbsolutePath());
        builder.addAttribute(new Attribute("Conventions", "CF-1.6"));
        builder.addDimension("time", 4);
        builder.addDimension("lat", 1);
        builder.addDimension("lon", 3);
        builder.addVariable("time", DataType.DOUBLE, "time").addAttribute(new Attribute("units", "days since 2024-01-01 00:00:00"))
                .addAttribute(new Attribute("standard_name", "time"));
        builder.addVariable("lat", DataType.DOUBLE, "lat").addAttribute(new Attribute("units", "degrees_north"));
        builder.addVariable("lon", DataType.DOUBLE, "lon").addAttribute(new Attribute("units", "degrees_east"));
        builder.addVariable("tas", DataType.FLOAT, "time lat lon").addAttribute(new Attribute("units", "K"))
                .addAttribute(new Attribute("_FillValue", FILL));
        try (NetcdfFormatWriter writer = builder.build()) {
            writer.write(writer.findVariable("time"), Array.factory(DataType.DOUBLE, new int[]{4}, new double[]{0, 1, 2, 3}));
            writer.write(writer.findVariable("lat"), Array.factory(DataType.DOUBLE, new int[]{1}, new double[]{46.0}));
            writer.write(writer.findVariable("lon"), Array.factory(DataType.DOUBLE, new int[]{3}, new double[]{10.0, 11.0, 12.0}));
            writer.write(writer.findVariable("tas"), Array.factory(DataType.FLOAT, new int[]{4, 1, 3}, data));
        }
    }

    private GridCoverage2D aggregate( String aggregation, Double minValidFraction, Integer from, Integer to ) throws Exception {
        OmsNetcdfTemporalAggregator aggregator = new OmsNetcdfTemporalAggregator();
        aggregator.pm = pm;
        aggregator.inPath = ncFile.getAbsolutePath();
        aggregator.pAggregation = aggregation;
        aggregator.pMinValidFraction = minValidFraction;
        if (from != null)
            aggregator.pFromTimestep = from;
        aggregator.pToTimestep = to;
        aggregator.process();
        return aggregator.outRaster;
    }

    public void testAggregations() throws Exception {
        // all the timesteps, a single valid value is enough
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.MEAN, 0.0, null, null).getRenderedImage(),
                new double[][]{{3, 6, NV}}, DELTA);
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.SUM, 0.0, null, null).getRenderedImage(),
                new double[][]{{12, 12, NV}}, DELTA);
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.MIN, 0.0, null, null).getRenderedImage(),
                new double[][]{{1, 4, NV}}, DELTA);
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.MAX, 0.0, null, null).getRenderedImage(),
                new double[][]{{6, 8, NV}}, DELTA);
    }

    public void testMinValidFraction() throws Exception {
        // half of the days are enough for column 1
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.MEAN, 0.5, null, null).getRenderedImage(),
                new double[][]{{3, 6, NV}}, DELTA);
        // 3 of 4 days, rounded up, are not
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.MEAN, 0.51, null, null).getRenderedImage(),
                new double[][]{{3, NV, NV}}, DELTA);
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.MEAN, 1.0, null, null).getRenderedImage(),
                new double[][]{{3, NV, NV}}, DELTA);
    }

    public void testTimestepRange() throws Exception {
        // the days 1 and 2, end excluded: column 1 has no valid value
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.MEAN, 0.0, 1, 3).getRenderedImage(),
                new double[][]{{2.5, NV, NV}}, DELTA);
        // from day 2 to the end
        checkMatrixEqual(aggregate(OmsNetcdfTemporalAggregator.SUM, 0.0, 2, null).getRenderedImage(),
                new double[][]{{9, 8, NV}}, DELTA);
    }

    public void testWrongParameters() throws Exception {
        try {
            aggregate("median", 0.0, null, null);
            fail("An unknown aggregation must fail.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
        try {
            aggregate(OmsNetcdfTemporalAggregator.MEAN, 1.5, null, null);
            fail("A fraction above 1 must fail.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
    }

    /**
     * The 10 days of the Daymet subset of the test data, the expected values computed from the values read by GDAL.
     */
    public void testDaymetMean() throws Exception {
        File daymetFile = new File(
                getClass().getClassLoader().getResource("formats/netcdf/daymet_v4r1_tmax_2020_boulder.nc4").toURI());
        OmsNetcdfTemporalAggregator aggregator = new OmsNetcdfTemporalAggregator();
        aggregator.pm = pm;
        aggregator.inPath = daymetFile.getAbsolutePath();
        aggregator.pGridName = "tmax";
        aggregator.pMinValidFraction = 1.0;
        aggregator.process();
        GridCoverage2D mean = aggregator.outRaster;

        // the native grid of the file
        assertTrue(mean.getCoordinateReferenceSystem() instanceof ProjectedCRS);
        ReferencedEnvelope envelope = mean.getEnvelope2D();
        assertEquals(-437750.0, envelope.getMinX(), DELTA);
        assertEquals(-240500.0, envelope.getMaxY(), DELTA);

        Raster data = mean.getRenderedImage().getData();
        assertEquals(27.561, data.getSampleDouble(0, 0, 0), 1E-4);
        assertEquals(30.452, data.getSampleDouble(5, 10, 0), 1E-4);
        assertEquals(33.716, data.getSampleDouble(19, 19, 0), 1E-4);

        aggregator = new OmsNetcdfTemporalAggregator();
        aggregator.pm = pm;
        aggregator.inPath = daymetFile.getAbsolutePath();
        aggregator.pAggregation = OmsNetcdfTemporalAggregator.MAX;
        aggregator.process();
        data = aggregator.outRaster.getRenderedImage().getData();
        assertEquals(30.79, data.getSampleDouble(0, 0, 0), 1E-4);
        assertEquals(37.18, data.getSampleDouble(19, 19, 0), 1E-4);
    }
}
