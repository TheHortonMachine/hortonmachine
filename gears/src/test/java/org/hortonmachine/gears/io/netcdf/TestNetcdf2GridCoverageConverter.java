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

import java.awt.geom.Point2D;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.utils.HMTestCase;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.write.NetcdfFormatWriter;

/**
 * Test {@link OmsNetcdf2GridCoverageConverter} on small CF NetCDF files written by the test.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestNetcdf2GridCoverageConverter extends HMTestCase {

    private static final double NV = -9999.0;
    private static final float FILL = -1f;

    private static final double[] LATS = {46.0, 46.5, 47.0, 47.5};
    private static final double[] LONS = {10.0, 10.5, 11.0, 11.5, 12.0};

    private File ncFile;

    @Override
    protected void setUp() throws Exception {
        ncFile = File.createTempFile("hm_netcdf_test", ".nc");
    }

    @Override
    protected void tearDown() throws Exception {
        ncFile.delete();
    }

    /**
     * The value of a cell: 100 for every timestep, 10 for every latitude, 1 for every longitude.
     */
    private static float value( int t, int lat, int lon ) {
        return t * 100 + lat * 10 + lon;
    }

    /**
     * Write a lat/long grid with latitudes from south to north, as most CF files, and 3 days.
     * The variables are written in the given order, all with the same values.
     */
    private void writeTimeGrid( String... variables ) throws Exception {
        int nt = 3;
        float[] data = new float[nt * LATS.length * LONS.length];
        int k = 0;
        for( int t = 0; t < nt; t++ ) {
            for( int j = 0; j < LATS.length; j++ ) {
                for( int i = 0; i < LONS.length; i++ ) {
                    data[k++] = value(t, j, i);
                }
            }
        }
        // a missing value in the south west corner of the first day
        data[0] = FILL;

        NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(ncFile.getAbsolutePath());
        builder.addAttribute(new Attribute("Conventions", "CF-1.6"));
        builder.addDimension("time", nt);
        builder.addDimension("lat", LATS.length);
        builder.addDimension("lon", LONS.length);
        builder.addVariable("time", DataType.DOUBLE, "time").addAttribute(new Attribute("units", "days since 2024-01-01 00:00:00"))
                .addAttribute(new Attribute("standard_name", "time"));
        builder.addVariable("lat", DataType.DOUBLE, "lat").addAttribute(new Attribute("units", "degrees_north"));
        builder.addVariable("lon", DataType.DOUBLE, "lon").addAttribute(new Attribute("units", "degrees_east"));
        for( String variable : variables ) {
            builder.addVariable(variable, DataType.FLOAT, "time lat lon").addAttribute(new Attribute("units", "K"))
                    .addAttribute(new Attribute("_FillValue", FILL));
        }
        try (NetcdfFormatWriter writer = builder.build()) {
            writer.write(writer.findVariable("time"), Array.factory(DataType.DOUBLE, new int[]{nt}, new double[]{0, 1, 2}));
            writer.write(writer.findVariable("lat"), Array.factory(DataType.DOUBLE, new int[]{LATS.length}, LATS));
            writer.write(writer.findVariable("lon"), Array.factory(DataType.DOUBLE, new int[]{LONS.length}, LONS));
            for( String variable : variables ) {
                writer.write(writer.findVariable(variable),
                        Array.factory(DataType.FLOAT, new int[]{nt, LATS.length, LONS.length}, data));
            }
        }
    }

    private OmsNetcdf2GridCoverageConverter converter() {
        OmsNetcdf2GridCoverageConverter converter = new OmsNetcdf2GridCoverageConverter();
        converter.pm = pm;
        converter.inPath = ncFile.getAbsolutePath();
        return converter;
    }

    /**
     * Run the converter and collect the rasters of all the timesteps.
     */
    private List<GridCoverage2D> runAll( OmsNetcdf2GridCoverageConverter converter, List<String> dates ) throws Exception {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<GridCoverage2D> rasters = new ArrayList<>();
        converter.initProcess();
        while( converter.doProcess ) {
            converter.process();
            rasters.add(converter.outRaster);
            if (dates != null) {
                dates.add(converter.currentDate == null ? null : f.format(converter.currentDate));
            }
        }
        return rasters;
    }

    public void testTimestepsAndCellPositions() throws Exception {
        writeTimeGrid("tas");

        OmsNetcdf2GridCoverageConverter converter = converter();
        converter.pGridName = "tas";
        converter.pFromTimestep = 0;
        converter.pToTimestep = 3;
        List<String> dates = new ArrayList<>();
        List<GridCoverage2D> rasters = runAll(converter, dates);

        assertEquals(3, rasters.size());
        assertEquals(List.of("2024-01-01 00:00", "2024-01-02 00:00", "2024-01-03 00:00"), dates);

        // the bounds are those of the cells, half a cell out of the axis values
        ReferencedEnvelope envelope = rasters.get(0).getEnvelope2D();
        assertEquals(9.75, envelope.getMinX(), DELTA);
        assertEquals(12.25, envelope.getMaxX(), DELTA);
        assertEquals(45.75, envelope.getMinY(), DELTA);
        assertEquals(47.75, envelope.getMaxY(), DELTA);

        // the first row of the raster is the northernmost latitude, the fill value is novalue
        double[][] expected0 = {//
                {30, 31, 32, 33, 34}, //
                {20, 21, 22, 23, 24}, //
                {10, 11, 12, 13, 14}, //
                {NV, 1, 2, 3, 4}};
        checkMatrixEqual(rasters.get(0).getRenderedImage(), expected0, DELTA);

        double[][] expected2 = {//
                {230, 231, 232, 233, 234}, //
                {220, 221, 222, 223, 224}, //
                {210, 211, 212, 213, 214}, //
                {200, 201, 202, 203, 204}};
        checkMatrixEqual(rasters.get(2).getRenderedImage(), expected2, DELTA);
    }

    public void testSingleTimestepAndClippedEnd() throws Exception {
        writeTimeGrid("tas");

        // without end only the start timestep is dumped
        OmsNetcdf2GridCoverageConverter converter = converter();
        converter.pFromTimestep = 1;
        List<String> dates = new ArrayList<>();
        List<GridCoverage2D> rasters = runAll(converter, dates);
        assertEquals(1, rasters.size());
        assertEquals(List.of("2024-01-02 00:00"), dates);
        assertEquals(134.0, rasters.get(0).evaluate(new Point2D.Double(12.0, 47.5), new double[1])[0], DELTA);

        // an end beyond the available timesteps is clipped
        converter = converter();
        converter.pFromTimestep = 1;
        converter.pToTimestep = 10;
        rasters = runAll(converter, null);
        assertEquals(2, rasters.size());
    }

    public void testGridSelection() throws Exception {
        writeTimeGrid("tas_max", "tas", "pr");

        // no name nor pattern: the first grid
        OmsNetcdf2GridCoverageConverter converter = converter();
        converter.initProcess();
        assertEquals("tas_max", converter.selectedGridName);

        // the first grid that contains the inclusion and not the exclusion pattern
        converter = converter();
        converter.pIncludePattern = "tas";
        converter.pExcludePattern = "max";
        converter.initProcess();
        assertEquals("tas", converter.selectedGridName);

        converter = converter();
        converter.pGridName = "pr";
        converter.initProcess();
        assertEquals("pr", converter.selectedGridName);

        converter = converter();
        converter.pGridName = "missing";
        try {
            converter.initProcess();
            fail("A missing grid must not be silently ignored.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        }
    }

    public void testGridWithoutTimeAnd360Longitudes() throws Exception {
        // longitudes in [0, 360], west of Greenwich in [-180, 180], and no time axis
        double[] lons = {190.0, 195.0, 200.0};
        double[] lats = {10.0, 5.0};
        NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(ncFile.getAbsolutePath());
        builder.addAttribute(new Attribute("Conventions", "CF-1.6"));
        builder.addDimension("lat", lats.length);
        builder.addDimension("lon", lons.length);
        builder.addVariable("lat", DataType.DOUBLE, "lat").addAttribute(new Attribute("units", "degrees_north"));
        builder.addVariable("lon", DataType.DOUBLE, "lon").addAttribute(new Attribute("units", "degrees_east"));
        builder.addVariable("elev", DataType.DOUBLE, "lat lon").addAttribute(new Attribute("units", "m"));
        try (NetcdfFormatWriter writer = builder.build()) {
            writer.write(writer.findVariable("lat"), Array.factory(DataType.DOUBLE, new int[]{lats.length}, lats));
            writer.write(writer.findVariable("lon"), Array.factory(DataType.DOUBLE, new int[]{lons.length}, lons));
            writer.write(writer.findVariable("elev"),
                    Array.factory(DataType.DOUBLE, new int[]{lats.length, lons.length}, new double[]{1, 2, 3, 4, 5, 6}));
        }

        OmsNetcdf2GridCoverageConverter converter = converter();
        List<String> dates = new ArrayList<>();
        List<GridCoverage2D> rasters = runAll(converter, dates);
        assertEquals(1, rasters.size());
        assertNull(dates.get(0));

        ReferencedEnvelope envelope = rasters.get(0).getEnvelope2D();
        assertEquals(-172.5, envelope.getMinX(), DELTA);
        assertEquals(-157.5, envelope.getMaxX(), DELTA);
        // latitudes from north to south keep their order
        double[][] expected = {//
                {1, 2, 3}, //
                {4, 5, 6}};
        checkMatrixEqual(rasters.get(0).getRenderedImage(), expected, DELTA);
    }

    public void testNearestIndex() throws Exception {
        double[] ascending = {0, 1, 2, 3};
        assertEquals(0, OmsNetcdf2GridCoverageConverter.nearestIndex(ascending, -0.5));
        assertEquals(0, OmsNetcdf2GridCoverageConverter.nearestIndex(ascending, 0.4));
        assertEquals(1, OmsNetcdf2GridCoverageConverter.nearestIndex(ascending, 0.6));
        assertEquals(3, OmsNetcdf2GridCoverageConverter.nearestIndex(ascending, 3.5));
        assertEquals(-1, OmsNetcdf2GridCoverageConverter.nearestIndex(ascending, -0.51));
        assertEquals(-1, OmsNetcdf2GridCoverageConverter.nearestIndex(ascending, 3.51));

        double[] descending = {30, 20, 10};
        assertEquals(0, OmsNetcdf2GridCoverageConverter.nearestIndex(descending, 34));
        assertEquals(1, OmsNetcdf2GridCoverageConverter.nearestIndex(descending, 16));
        assertEquals(2, OmsNetcdf2GridCoverageConverter.nearestIndex(descending, 14));
        assertEquals(-1, OmsNetcdf2GridCoverageConverter.nearestIndex(descending, 4));
        assertEquals(-1, OmsNetcdf2GridCoverageConverter.nearestIndex(descending, 36));
    }

    public void testShiftToCellCenters() throws Exception {
        // the HDF-EOS axes of netcdf-java start at the upper left corner of the grid
        double[] x = {0, 2, 4};
        OmsNetcdf2GridCoverageConverter.shiftToCellCenters(x);
        assertEquals(1.0, x[0], DELTA);
        assertEquals(5.0, x[2], DELTA);

        double[] y = {100, 98, 96};
        OmsNetcdf2GridCoverageConverter.shiftToCellCenters(y);
        assertEquals(99.0, y[0], DELTA);
        assertEquals(95.0, y[2], DELTA);
    }
}
