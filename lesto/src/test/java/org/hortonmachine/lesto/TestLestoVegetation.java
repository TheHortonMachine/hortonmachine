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
package org.hortonmachine.lesto;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.io.las.core.ALasWriter;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.lesto.modules.vegetation.OmsGeomorphonMaximaFinder;
import org.hortonmachine.lesto.modules.vegetation.PointCloudMaximaFinder;
import org.hortonmachine.lesto.modules.vegetation.WatershedAlgorithm;
import org.hortonmachine.lesto.modules.vegetation.rastermaxima.OmsRasterMaximaFinder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import junit.framework.TestCase;

/**
 * Tests the lidar vegetation modules on a synthetic canopy of two cones.
 */
public class TestLestoVegetation extends TestCase {

    static {
        Locale.setDefault(Locale.ENGLISH);
    }

    private static final double GROUND = 100.0;
    /** The tops of the two cones, as col/row of the cells of 1 m. */
    private static final int[][] TOPS = {{8, 10}, {21, 10}};

    private CoordinateReferenceSystem crs;
    private File folder;

    @Override
    protected void setUp() throws Exception {
        crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632", true);
        folder = File.createTempFile("hm_lesto_vegetation_", "");
        folder.delete();
        folder.mkdirs();
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(folder);
    }

    /**
     * The two cones, inverted, are two basins, each around its top, and the cells with no value
     * stay without value. Not inverted, the basins are around the minima instead.
     */
    public void testWatershedTwoCrowns() throws Exception {
        double[][] canopy = twoCones(0.0);
        // a row with no value, as a masked ground, that is no basin and no line
        for( int c = 0; c < 30; c++ ) {
            canopy[0][c] = HMConstants.doubleNovalue;
        }
        File chm = writeRaster("chm.tif", canopy);

        WatershedAlgorithm watershed = new WatershedAlgorithm();
        watershed.inRaster = chm.getAbsolutePath();
        watershed.doInvert = true;
        watershed.doBorders = false;
        watershed.outRaster = new File(folder, "crowns.tif").getAbsolutePath();
        watershed.process();

        try (HMRaster crowns = HMRaster.fromFile(watershed.outRaster)) {
            double west = crowns.getValue(TOPS[0][0], TOPS[0][1]);
            double east = crowns.getValue(TOPS[1][0], TOPS[1][1]);
            assertFalse(crowns.isNovalue(west));
            assertFalse(crowns.isNovalue(east));
            assertTrue("two crowns", west != east);
            for( int c = 0; c < 30; c++ ) {
                assertTrue(crowns.isNovalue(crowns.getValue(c, 0)));
                double value = crowns.getValue(c, 10);
                if (c < 13) {
                    assertEquals(west, value, 0.0);
                } else if (c > 16) {
                    assertEquals(east, value, 0.0);
                }
            }
        }

        watershed = new WatershedAlgorithm();
        watershed.inRaster = chm.getAbsolutePath();
        watershed.doInvert = true;
        watershed.doBorders = true;
        watershed.outRaster = new File(folder, "borders.tif").getAbsolutePath();
        watershed.process();
        try (HMRaster borders = HMRaster.fromFile(watershed.outRaster)) {
            int lines = 0;
            for( int c = 0; c < 30; c++ ) {
                if (!borders.isNovalue(borders.getValue(c, 10))) {
                    assertEquals(1.0, borders.getValue(c, 10), 0.0);
                    assertTrue("the line is between the crowns: " + c, c > 12 && c < 17);
                    lines++;
                }
            }
            assertTrue("a line divides the crowns", lines > 0);
        }
    }

    /**
     * The cast to byte made values over 255 wrap around: an elevation model over 1000 m works
     * as one starting from 0.
     */
    public void testWatershedHighValues() throws Exception {
        File dsm = writeRaster("dsm.tif", twoCones(1900.0));
        WatershedAlgorithm watershed = new WatershedAlgorithm();
        watershed.inRaster = dsm.getAbsolutePath();
        watershed.doInvert = true;
        watershed.doBorders = false;
        watershed.outRaster = new File(folder, "crowns.tif").getAbsolutePath();
        watershed.process();
        try (HMRaster crowns = HMRaster.fromFile(watershed.outRaster)) {
            double max = 0;
            for( int r = 0; r < crowns.getRows(); r++ ) {
                for( int c = 0; c < crowns.getCols(); c++ ) {
                    double value = crowns.getValue(c, r);
                    if (!crowns.isNovalue(value)) {
                        max = Math.max(max, value);
                    }
                }
            }
            assertEquals("two crowns", 2.0, max, 0.0);
        }
    }

    /**
     * The tops of the cones are the only maxima over the threshold, also with the window of the
     * crown widths of Popescu and Wynne.
     */
    public void testRasterMaximaFinder() throws Exception {
        GridCoverage2D chm = coverage(twoCones(0.0));
        for( String mode : new String[]{"custom", "mixed_pines_and_deciduous_trees"} ) {
            OmsRasterMaximaFinder finder = new OmsRasterMaximaFinder();
            finder.inDsmDtmDiff = chm;
            finder.pMode = mode;
            finder.pThreshold = 5.0;
            finder.pMaxRadius = 3.0;
            finder.process();
            assertTops(finder.outMaxima, mode);
            assertEquals(2, finder.outCircles.size());
        }
    }

    /**
     * The tops of the cones of the surface are the peaks of its geomorphons.
     */
    public void testGeomorphonMaximaFinder() throws Exception {
        OmsGeomorphonMaximaFinder finder = new OmsGeomorphonMaximaFinder();
        finder.inDSM = coverage(twoCones(GROUND));
        finder.inDTM = coverage(twoCones(GROUND, true));
        finder.pRadius = 4.0;
        finder.pThreshold = 1.0;
        finder.pElevDiffThres = 10.0;
        finder.process();
        assertTops(finder.outMaxima, "geomorphon");
    }

    /**
     * The tops of the cones are the only points higher than all the others around them, with the
     * dynamic radius also with no upper limit for it: that used to make every point a top.
     */
    public void testPointCloudMaximaFinder() throws Exception {
        // points every 0.5 m, closer than the smallest dynamic radius, about 1 m at 5 m
        List<LasRecord> points = new ArrayList<>();
        for( int i = 1; i < 60; i++ ) {
            for( int j = 1; j < 40; j++ ) {
                double x = i * 0.5;
                double y = j * 0.5;
                double d1 = Math.hypot(x - (TOPS[0][0] + 0.5), y - (20 - TOPS[0][1] - 0.5));
                double d2 = Math.hypot(x - (TOPS[1][0] + 0.5), y - (20 - TOPS[1][1] - 0.5));
                LasRecord point = new LasRecord();
                point.x = x;
                point.y = y;
                point.z = GROUND + Math.max(0, 20.0 - 2.5 * Math.min(d1, d2));
                point.intensity = 100;
                point.returnNumber = 1;
                point.numberOfReturns = 1;
                point.classification = 1;
                points.add(point);
            }
        }
        File las = new File(folder, "cones.las");
        try (ALasWriter writer = ALasWriter.getWriter(las, crs)) {
            writer.setBounds(0, 30, 0, 20, GROUND, GROUND + 20);
            writer.open();
            for( LasRecord point : points ) {
                writer.addPoint(point);
            }
        }
        File dtm = writeRaster("dtm.tif", twoCones(GROUND, true));

        for( double maxRadius : new double[]{-1.0, 3.0} ) {
            PointCloudMaximaFinder finder = new PointCloudMaximaFinder();
            finder.inLas = las.getAbsolutePath();
            finder.inDtm = dtm.getAbsolutePath();
            finder.pThreshold = 5.0;
            finder.doDynamicRadius = true;
            finder.pMaxRadius = maxRadius;
            finder.outTops = new File(folder, "tops.shp").getAbsolutePath();
            finder.process();
            assertTops(OmsVectorReader.readVector(finder.outTops), "radius " + maxRadius);
        }
    }

    /**
     * Asserts that the maxima are the two tops of the cones, on the centers of their cells.
     */
    private static void assertTops( SimpleFeatureCollection maxima, String message ) {
        assertEquals(message, 2, maxima.size());
        try (SimpleFeatureIterator it = maxima.features()) {
            while( it.hasNext() ) {
                SimpleFeature feature = it.next();
                Coordinate top = ((Geometry) feature.getDefaultGeometry()).getCoordinate();
                boolean isTop = false;
                for( int[] t : TOPS ) {
                    if (Math.abs(top.x - (t[0] + 0.5)) < 0.01 && Math.abs(top.y - (20 - t[1] - 0.5)) < 0.01) {
                        isTop = true;
                    }
                }
                assertTrue(message + ": " + top, isTop);
            }
        }
    }

    private static double[][] twoCones( double base ) {
        return twoCones(base, false);
    }

    /**
     * @return 30 x 20 cells of 1 m with two cones 20 m high over the base, with tops at the cells
     *         of TOPS, or, if flat, the base only.
     */
    private static double[][] twoCones( double base, boolean flat ) {
        double[][] data = new double[20][30];
        for( int r = 0; r < 20; r++ ) {
            for( int c = 0; c < 30; c++ ) {
                double d1 = Math.hypot(c - TOPS[0][0], r - TOPS[0][1]);
                double d2 = Math.hypot(c - TOPS[1][0], r - TOPS[1][1]);
                data[r][c] = base + (flat ? 0 : Math.max(0, 20.0 - 2.5 * Math.min(d1, d2)));
            }
        }
        return data;
    }

    private GridCoverage2D coverage( double[][] data ) {
        int rows = data.length;
        int cols = data[0].length;
        RegionMap region = RegionMap.fromBoundsAndGrid(0, cols, 0, rows, cols, rows);
        return CoverageUtilities.buildCoverage("raster", data, region, crs, true);
    }

    /**
     * Writes the data, by rows from north, on cells of 1 m from the origin.
     */
    private File writeRaster( String name, double[][] data ) throws Exception {
        File file = new File(folder, name);
        OmsRasterWriter.writeRaster(file.getAbsolutePath(), coverage(data));
        assertNotNull(OmsRasterReader.readRaster(file.getAbsolutePath()));
        return file;
    }
}
