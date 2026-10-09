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

import java.awt.image.Raster;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.modules.r.carver.OmsCarver;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Test for {@link OmsCarver}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestCarver extends HMTestCase {

    private static final GeometryFactory GF = new GeometryFactory();

    private RegionMap region;
    private CoordinateReferenceSystem crs;
    private GridCoverage2D flatRaster;

    protected void setUp() throws Exception {
        region = HMTestMaps.getEnvelopeparams();
        crs = HMTestMaps.getCrs();
        double[][] data = new double[region.rows][region.cols];
        for( double[] row : data ) {
            java.util.Arrays.fill(row, 100.0);
        }
        flatRaster = CoverageUtilities.buildCoverage("elev", data, region, crs, true);
    }

    /**
     * @return a horizontal line through the centers of the cells of a row.
     */
    private Geometry rowLine( int row ) {
        double y = region.north - (row + 0.5) * region.yres;
        return GF.createLineString(new Coordinate[]{new Coordinate(region.west + 1, y), new Coordinate(region.east - 1, y)});
    }

    /**
     * @return a polygon over the cells of the given rows and columns.
     */
    private Geometry cellsPolygon( int fromRow, int toRow, int fromCol, int toCol ) {
        double west = region.west + fromCol * region.xres + 1;
        double east = region.west + (toCol + 1) * region.xres - 1;
        double north = region.north - fromRow * region.yres - 1;
        double south = region.north - (toRow + 1) * region.yres + 1;
        return GF.createPolygon(new Coordinate[]{new Coordinate(west, south), new Coordinate(west, north),
                new Coordinate(east, north), new Coordinate(east, south), new Coordinate(west, south)});
    }

    public void testCarveLinesAndPolygons() throws Exception {
        OmsCarver carver = new OmsCarver();
        carver.pm = pm;
        carver.inRaster = flatRaster;
        carver.inCarverLines = FeatureUtilities.featureCollectionFromGeometry(crs, rowLine(1), rowLine(5));
        carver.pDepthLines = 5.0;
        // the polygon covers part of the line of row 5
        carver.inCarverPolygons = FeatureUtilities.featureCollectionFromGeometry(crs, cellsPolygon(4, 6, 2, 4));
        carver.pDepthPolygons = 2.0;
        carver.process();
        Raster out = carver.outRaster.getRenderedImage().getData();

        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double value = out.getSampleDouble(c, r, 0);
                boolean inPolygon = r >= 4 && r <= 6 && c >= 2 && c <= 4;
                if (inPolygon) {
                    // the depth of the polygons, also where the line crosses it
                    assertEquals(98.0, value, DELTA);
                } else if (r == 1 || r == 5) {
                    assertEquals(95.0, value, DELTA);
                } else {
                    assertEquals(100.0, value, DELTA);
                }
            }
        }
    }
}
