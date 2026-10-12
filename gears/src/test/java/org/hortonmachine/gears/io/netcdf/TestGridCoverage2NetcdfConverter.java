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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

/**
 * Test {@link OmsGridCoverage2NetcdfConverter}, reading the files back with {@link OmsNetcdf2GridCoverageConverter}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestGridCoverage2NetcdfConverter extends HMTestCase {

    private static final double NV = -9999.0;

    private File ncFile;

    @Override
    protected void setUp() throws Exception {
        ncFile = File.createTempFile("hm_netcdf_writer_test", ".nc");
        ncFile.delete();
    }

    @Override
    protected void tearDown() throws Exception {
        ncFile.delete();
    }

    private OmsGridCoverage2NetcdfConverter writer() {
        OmsGridCoverage2NetcdfConverter writer = new OmsGridCoverage2NetcdfConverter();
        writer.pm = pm;
        writer.outPath = ncFile.getAbsolutePath();
        writer.pVariableName = "elev";
        writer.pUnits = "m";
        return writer;
    }

    private List<GridCoverage2D> readAll( boolean doLatLon, List<String> dates ) throws Exception {
        OmsNetcdf2GridCoverageConverter reader = new OmsNetcdf2GridCoverageConverter();
        reader.pm = pm;
        reader.inPath = ncFile.getAbsolutePath();
        reader.pGridName = "elev";
        reader.pToTimestep = 1000;
        reader.doLatLon = doLatLon;
        reader.initProcess();
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        List<GridCoverage2D> rasters = new ArrayList<>();
        while( reader.doProcess ) {
            reader.process();
            rasters.add(reader.outRaster);
            if (dates != null) {
                dates.add(reader.currentDate == null ? null : f.format(reader.currentDate));
            }
        }
        return rasters;
    }

    public void testProjectedRoundTrip() throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        RegionMap region = HMTestMaps.getEnvelopeparams();
        GridCoverage2D raster = CoverageUtilities.buildCoverage("elev", HMTestMaps.mapData, region, crs, true);

        OmsGridCoverage2NetcdfConverter writer = writer();
        writer.inRaster = raster;
        writer.process();
        writer.close();

        // the crs is written for GDAL and for the CF readers
        try (NetcdfDataset dataset = NetcdfDatasets.openDataset(ncFile.getAbsolutePath())) {
            Variable crsVariable = dataset.findVariable(OmsGridCoverage2NetcdfConverter.CRS_VARIABLE);
            assertEquals("EPSG:32632", crsVariable.findAttribute("epsg_code").getStringValue());
            assertEquals("transverse_mercator", crsVariable.findAttribute("grid_mapping_name").getStringValue());
            assertEquals(9.0, crsVariable.findAttribute("longitude_of_central_meridian").getNumericValue().doubleValue(), DELTA);
            assertEquals(500000.0, crsVariable.findAttribute("false_easting").getNumericValue().doubleValue(), DELTA);
            assertNotNull(crsVariable.findAttribute("spatial_ref"));
            Attribute fill = dataset.findVariable("elev").findAttribute("_FillValue");
            assertEquals(NV, fill.getNumericValue().doubleValue(), DELTA);
            // no time for a single raster without timestamp
            assertNull(dataset.findVariable("time"));
        }

        List<String> dates = new ArrayList<>();
        List<GridCoverage2D> rasters = readAll(false, dates);
        assertEquals(1, rasters.size());
        assertNull(dates.get(0));
        GridCoverage2D back = rasters.get(0);
        assertTrue(HMCrsRegistry.crsEquals(crs, back.getCoordinateReferenceSystem()));
        ReferencedEnvelope envelope = back.getEnvelope2D();
        assertEquals(region.getWest(), envelope.getMinX(), DELTA);
        assertEquals(region.getEast(), envelope.getMaxX(), DELTA);
        assertEquals(region.getSouth(), envelope.getMinY(), DELTA);
        assertEquals(region.getNorth(), envelope.getMaxY(), DELTA);
        checkMatrixEqual(back.getRenderedImage(), HMTestMaps.mapData, DELTA);

        // forced to lat/long, the grid is resampled to WGS84
        GridCoverage2D latLon = readAll(true, null).get(0);
        assertTrue(HMCrsRegistry.crsEquals(DefaultGeographicCRS.WGS84, latLon.getCoordinateReferenceSystem()));
    }

    public void testLatLonTimeSeriesRoundTrip() throws Exception {
        // 3x2 cells of half a degree, with a novalue
        RegionMap region = RegionMap.fromBoundsAndGrid(10.0, 11.5, 46.0, 47.0, 3, 2);
        String[] timestamps = {"2024-07-01 00:00", "2024-07-01 06:00", "2024-07-02 12:30"};

        OmsGridCoverage2NetcdfConverter writer = writer();
        for( int t = 0; t < timestamps.length; t++ ) {
            double[][] data = {//
                    {t * 10 + 1, t * 10 + 2, t * 10 + 3}, //
                    {t * 10 + 4, NV, t * 10 + 6}};
            writer.inRaster = CoverageUtilities.buildCoverage("elev", data, region, DefaultGeographicCRS.WGS84, true);
            writer.pTimestamp = timestamps[t];
            writer.process();
        }
        writer.close();

        List<String> dates = new ArrayList<>();
        List<GridCoverage2D> rasters = readAll(false, dates);
        assertEquals(List.of(timestamps), dates);
        ReferencedEnvelope envelope = rasters.get(0).getEnvelope2D();
        assertEquals(10.0, envelope.getMinX(), DELTA);
        assertEquals(11.5, envelope.getMaxX(), DELTA);
        assertEquals(46.0, envelope.getMinY(), DELTA);
        assertEquals(47.0, envelope.getMaxY(), DELTA);
        double[][] expected2 = {//
                {21, 22, 23}, //
                {24, NV, 26}};
        checkMatrixEqual(rasters.get(2).getRenderedImage(), expected2, DELTA);
    }

    public void testInvalidSeries() throws Exception {
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        RegionMap region = HMTestMaps.getEnvelopeparams();
        GridCoverage2D raster = CoverageUtilities.buildCoverage("elev", HMTestMaps.mapData, region, crs, true);

        // without timestamp the file holds a single raster
        OmsGridCoverage2NetcdfConverter writer = writer();
        writer.inRaster = raster;
        writer.process();
        try {
            writer.process();
            fail("A file without time can't hold two rasters.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        } finally {
            writer.close();
        }

        // the rasters of a series must share the grid
        ncFile.delete();
        writer = writer();
        writer.inRaster = raster;
        writer.pTimestamp = "2024-01-01 00:00";
        writer.process();
        RegionMap other = RegionMap.fromBoundsAndGrid(region.getWest(), region.getEast(), region.getSouth() + 30,
                region.getNorth() + 30, region.getCols(), region.getRows());
        writer.inRaster = CoverageUtilities.buildCoverage("elev", HMTestMaps.mapData, other, crs, true);
        writer.pTimestamp = "2024-01-02 00:00";
        try {
            writer.process();
            fail("Rasters on different grids can't be in the same series.");
        } catch (ModelsIllegalargumentException e) {
            // expected
        } finally {
            writer.close();
        }
    }
}
