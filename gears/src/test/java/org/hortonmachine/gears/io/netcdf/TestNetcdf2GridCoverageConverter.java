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
import java.awt.image.Raster;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.geotools.api.referencing.crs.ProjectedCRS;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.locationtech.jts.geom.Coordinate;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
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

    /**
     * Write a projected grid without time, with x and y in meters and a CF grid mapping variable without crs
     * attributes. The value of a cell is 10 for every y index and 1 for every x index.
     */
    private void writeProjectedGrid( double[] x, double[] y, String mappingName, Attribute... mappingAttributes )
            throws Exception {
        double[] data = new double[y.length * x.length];
        int k = 0;
        for( int j = 0; j < y.length; j++ ) {
            for( int i = 0; i < x.length; i++ ) {
                data[k++] = j * 10 + i;
            }
        }
        NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(ncFile.getAbsolutePath());
        builder.addAttribute(new Attribute("Conventions", "CF-1.6"));
        builder.addDimension("y", y.length);
        builder.addDimension("x", x.length);
        builder.addVariable("y", DataType.DOUBLE, "y").addAttribute(new Attribute("units", "m"))
                .addAttribute(new Attribute("standard_name", "projection_y_coordinate"));
        builder.addVariable("x", DataType.DOUBLE, "x").addAttribute(new Attribute("units", "m"))
                .addAttribute(new Attribute("standard_name", "projection_x_coordinate"));
        Variable.Builder< ? > crsVariable = builder.addVariable("crs", DataType.SHORT, "")
                .addAttribute(new Attribute("grid_mapping_name", mappingName));
        for( Attribute attribute : mappingAttributes ) {
            crsVariable.addAttribute(attribute);
        }
        builder.addVariable("data", DataType.DOUBLE, "y x").addAttribute(new Attribute("grid_mapping", "crs"));
        try (NetcdfFormatWriter writer = builder.build()) {
            writer.write(writer.findVariable("y"), Array.factory(DataType.DOUBLE, new int[]{y.length}, y));
            writer.write(writer.findVariable("x"), Array.factory(DataType.DOUBLE, new int[]{x.length}, x));
            writer.write(writer.findVariable("data"), Array.factory(DataType.DOUBLE, new int[]{y.length, x.length}, data));
        }
    }

    private static Attribute doubles( String name, double... values ) {
        return Attribute.builder(name).setValues(Array.factory(DataType.DOUBLE, new int[]{values.length}, values)).build();
    }

    /**
     * Convert the projected grid and check that it keeps its grid in a projected crs.
     */
    private GridCoverage2D checkNativeGrid( double[] x, double[] y ) throws Exception {
        List<GridCoverage2D> rasters = runAll(converter(), null);
        GridCoverage2D raster = rasters.get(0);
        assertTrue(raster.getCoordinateReferenceSystem() instanceof ProjectedCRS);

        double cellX = Math.abs(x[1] - x[0]);
        double cellY = Math.abs(y[1] - y[0]);
        ReferencedEnvelope envelope = raster.getEnvelope2D();
        assertEquals(Math.min(x[0], x[x.length - 1]) - cellX / 2, envelope.getMinX(), DELTA);
        assertEquals(Math.max(x[0], x[x.length - 1]) + cellX / 2, envelope.getMaxX(), DELTA);
        assertEquals(Math.min(y[0], y[y.length - 1]) - cellY / 2, envelope.getMinY(), DELTA);
        assertEquals(Math.max(y[0], y[y.length - 1]) + cellY / 2, envelope.getMaxY(), DELTA);
        assertEquals(x.length, raster.getRenderedImage().getWidth());
        assertEquals(y.length, raster.getRenderedImage().getHeight());
        return raster;
    }

    public void testCfLambertConformalAsDaymet() throws Exception {
        // the grid mapping of Daymet V4, with y from north to south
        double[] x = {-450250, -449250, -448250, -447250, -446250};
        double[] y = {-226000, -227000, -228000, -229000};
        writeProjectedGrid(x, y, "lambert_conformal_conic", //
                new Attribute("longitude_of_central_meridian", -100.0), //
                new Attribute("latitude_of_projection_origin", 42.5), //
                new Attribute("false_easting", 0.0), //
                new Attribute("false_northing", 0.0), //
                doubles("standard_parallel", 25.0, 60.0), //
                new Attribute("semi_major_axis", 6378137.0), //
                new Attribute("inverse_flattening", 298.257223563));

        GridCoverage2D raster = checkNativeGrid(x, y);
        // the cells are copied, the first row is the first y, the northernmost
        double[][] expected = {//
                {0, 1, 2, 3, 4}, //
                {10, 11, 12, 13, 14}, //
                {20, 21, 22, 23, 24}, //
                {30, 31, 32, 33, 34}};
        checkMatrixEqual(raster.getRenderedImage(), expected, DELTA);

        // lon -105.5 lat 40.2 as projected by PROJ with
        // +proj=lcc +lat_1=25 +lat_2=60 +lat_0=42.5 +lon_0=-100 +x_0=0 +y_0=0 +ellps=WGS84
        HMCrsTransformer transformer = new HMCrsTransformer(DefaultGeographicCRS.WGS84, raster.getCoordinateReferenceSystem());
        Coordinate projected = transformer.transform(new Coordinate(-105.5, 40.2));
        assertEquals(-446844.550, projected.x, 0.01);
        assertEquals(-228978.209, projected.y, 0.01);
    }

    /**
     * A real Daymet V4R1 subset as served by the Earthdata OPeNDAP server, see the README of the data folder. The
     * expected values are those read by GDAL.
     */
    public void testDaymetSubset() throws Exception {
        File daymetFile = new File(getClass().getClassLoader().getResource("formats/netcdf/daymet_v4r1_tmax_2020_boulder.nc4").toURI());
        OmsNetcdf2GridCoverageConverter converter = new OmsNetcdf2GridCoverageConverter();
        converter.pm = pm;
        converter.inPath = daymetFile.getAbsolutePath();
        converter.pGridName = "tmax";
        converter.pFromTimestep = 0;
        converter.pToTimestep = 10;
        List<String> dates = new ArrayList<>();
        List<GridCoverage2D> rasters = runAll(converter, dates);

        assertEquals(10, rasters.size());
        assertEquals("2020-07-01 12:00", dates.get(0));
        assertEquals("2020-07-10 12:00", dates.get(9));

        GridCoverage2D first = rasters.get(0);
        assertTrue(first.getCoordinateReferenceSystem() instanceof ProjectedCRS);
        ReferencedEnvelope envelope = first.getEnvelope2D();
        assertEquals(-437750.0, envelope.getMinX(), DELTA);
        assertEquals(-417750.0, envelope.getMaxX(), DELTA);
        assertEquals(-260500.0, envelope.getMinY(), DELTA);
        assertEquals(-240500.0, envelope.getMaxY(), DELTA);

        Raster data = first.getRenderedImage().getData();
        assertEquals(20, data.getWidth());
        assertEquals(20, data.getHeight());
        // col, row, value
        double[][] expectedFirst = {{0, 0, 24.43}, {19, 0, 30.78}, {5, 10, 27.18}, {0, 19, 24.32}, {19, 19, 30.45}};
        for( double[] e : expectedFirst ) {
            assertEquals(e[2], data.getSampleDouble((int) e[0], (int) e[1], 0), 1E-4);
        }
        data = rasters.get(9).getRenderedImage().getData();
        double[][] expectedLast = {{0, 0, 30.79}, {19, 0, 38.10}, {5, 10, 34.20}, {0, 19, 31.09}, {19, 19, 37.18}};
        for( double[] e : expectedLast ) {
            assertEquals(e[2], data.getSampleDouble((int) e[0], (int) e[1], 0), 1E-4);
        }

        // the crs maps the cell centers to the lat/long of the file
        HMCrsTransformer transformer = new HMCrsTransformer(first.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84);
        try (NetcdfFile nc = NetcdfFiles.open(daymetFile.getAbsolutePath())) {
            Array lats = nc.findVariable("lat").read();
            Array lons = nc.findVariable("lon").read();
            Index index = lats.getIndex();
            for( int[] cell : new int[][]{{0, 0}, {5, 10}, {19, 19}} ) {
                int col = cell[0];
                int row = cell[1];
                Coordinate center = new Coordinate(envelope.getMinX() + (col + 0.5) * 1000, envelope.getMaxY() - (row + 0.5) * 1000);
                Coordinate lonLat = transformer.transform(center);
                index.set(row, col);
                assertEquals(lons.getDouble(index), lonLat.x, 1E-4);
                assertEquals(lats.getDouble(index), lonLat.y, 1E-4);
            }
        }

        // the shortwave radiation of the last day
        File sradFile = new File(getClass().getClassLoader().getResource("formats/netcdf/daymet_v4r1_srad_2020_boulder.nc4").toURI());
        converter = new OmsNetcdf2GridCoverageConverter();
        converter.pm = pm;
        converter.inPath = sradFile.getAbsolutePath();
        converter.pGridName = "srad";
        converter.pFromTimestep = 9;
        rasters = runAll(converter, null);
        assertEquals(1, rasters.size());
        data = rasters.get(0).getRenderedImage().getData();
        double[][] expectedSrad = {{0, 0, 560.11}, {19, 0, 532.64}, {5, 10, 530.17}, {0, 19, 557.18}, {19, 19, 534.71}};
        for( double[] e : expectedSrad ) {
            assertEquals(e[2], data.getSampleDouble((int) e[0], (int) e[1], 0), 1E-4);
        }
    }

    public void testCfLambertConformalWithoutEarthShape() throws Exception {
        // without ellipsoid the sphere of netcdf-java is assumed, y from south to north
        double[] x = {-10000, 0, 10000};
        double[] y = {-5000, 5000};
        writeProjectedGrid(x, y, "lambert_conformal_conic", //
                new Attribute("longitude_of_central_meridian", 11.0), //
                new Attribute("latitude_of_projection_origin", 46.0), //
                new Attribute("standard_parallel", 46.0));

        GridCoverage2D raster = checkNativeGrid(x, y);
        double[][] expected = {//
                {10, 11, 12}, //
                {0, 1, 2}};
        checkMatrixEqual(raster.getRenderedImage(), expected, DELTA);
    }

    public void testCfOtherProjections() throws Exception {
        // UTM 32N, with the ellipsoid as semi minor axis
        double[] x = {650000, 660000, 670000};
        double[] y = {5100000, 5110000};
        writeProjectedGrid(x, y, "transverse_mercator", //
                new Attribute("longitude_of_central_meridian", 9.0), //
                new Attribute("latitude_of_projection_origin", 0.0), //
                new Attribute("scale_factor_at_central_meridian", 0.9996), //
                new Attribute("false_easting", 500000.0), //
                new Attribute("false_northing", 0.0), //
                new Attribute("semi_major_axis", 6378137.0), //
                new Attribute("semi_minor_axis", 6356752.314245));
        checkNativeGrid(x, y);

        // CONUS albers
        x = new double[]{0, 30000, 60000};
        y = new double[]{1500000, 1530000};
        writeProjectedGrid(x, y, "albers_conical_equal_area", //
                new Attribute("longitude_of_central_meridian", -96.0), //
                new Attribute("latitude_of_projection_origin", 23.0), //
                doubles("standard_parallel", 29.5, 45.5), //
                new Attribute("semi_major_axis", 6378137.0), //
                new Attribute("inverse_flattening", 298.257222101));
        checkNativeGrid(x, y);

        // the european LAEA
        x = new double[]{4400000, 4410000, 4420000};
        y = new double[]{2700000, 2710000};
        writeProjectedGrid(x, y, "lambert_azimuthal_equal_area", //
                new Attribute("longitude_of_projection_origin", 10.0), //
                new Attribute("latitude_of_projection_origin", 52.0), //
                new Attribute("false_easting", 4321000.0), //
                new Attribute("false_northing", 3210000.0), //
                new Attribute("semi_major_axis", 6378137.0), //
                new Attribute("inverse_flattening", 298.257222101));
        checkNativeGrid(x, y);

        // a north polar sea ice grid, with standard parallel
        x = new double[]{-100000, -75000, -50000};
        y = new double[]{100000, 75000};
        writeProjectedGrid(x, y, "polar_stereographic", //
                new Attribute("straight_vertical_longitude_from_pole", -45.0), //
                new Attribute("latitude_of_projection_origin", 90.0), //
                new Attribute("standard_parallel", 70.0), //
                new Attribute("semi_major_axis", 6378273.0), //
                new Attribute("inverse_flattening", 298.279411123064));
        checkNativeGrid(x, y);
    }

    public void testCfProjectionResampledOnRequest() throws Exception {
        double[] x = {-10000, 0, 10000};
        double[] y = {-5000, 5000};
        writeProjectedGrid(x, y, "lambert_conformal_conic", //
                new Attribute("longitude_of_central_meridian", 11.0), //
                new Attribute("latitude_of_projection_origin", 46.0), //
                new Attribute("standard_parallel", 46.0));
        OmsNetcdf2GridCoverageConverter converter = converter();
        converter.doLatLon = true;
        GridCoverage2D raster = runAll(converter, null).get(0);
        assertSame(DefaultGeographicCRS.WGS84, raster.getCoordinateReferenceSystem());
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
