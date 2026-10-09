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
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.modules.r.bobthebuilder.OmsBobTheBuilder;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * Test for {@link OmsBobTheBuilder}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestBobTheBuilder extends HMTestCase {

    private static final GeometryFactory GF = new GeometryFactory();

    private RegionMap region;
    private CoordinateReferenceSystem crs;
    private GridCoverage2D flatRaster;
    private SimpleFeatureCollection area;

    protected void setUp() throws Exception {
        region = HMTestMaps.getEnvelopeparams();
        crs = HMTestMaps.getCrs();

        // a flat raster at 100 m, higher in the first column inside the polygon
        double[][] data = new double[region.rows][region.cols];
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                data[r][c] = c == 2 ? 120 : 100;
            }
        }
        flatRaster = CoverageUtilities.buildCoverage("elev", data, region, crs, true);

        // a polygon over columns 2 to 5 and rows 2 to 4
        double west = region.west + 2 * region.xres + 1;
        double east = region.west + 6 * region.xres - 1;
        double north = region.north - 2 * region.yres - 1;
        double south = region.north - 5 * region.yres + 1;
        Geometry polygon = GF.createPolygon(new Coordinate[]{new Coordinate(west, south), new Coordinate(west, north),
                new Coordinate(east, north), new Coordinate(east, south), new Coordinate(west, south)});
        area = FeatureUtilities.featureCollectionFromGeometry(crs, polygon);
    }

    /**
     * @return points at the corners of the raster with the given elevation, in the field userdata.
     */
    private SimpleFeatureCollection points( double elevation ) {
        Point[] points = {//
                GF.createPoint(new Coordinate(region.west + 1, region.north - 1)), //
                GF.createPoint(new Coordinate(region.east - 1, region.north - 1)), //
                GF.createPoint(new Coordinate(region.east - 1, region.south + 1)), //
                GF.createPoint(new Coordinate(region.west + 1, region.south + 1))};
        for( Point point : points ) {
            point.setUserData(elevation);
        }
        return FeatureUtilities.featureCollectionFromGeometry(crs, points);
    }

    private Raster build( double elevation, boolean doErode ) throws Exception {
        OmsBobTheBuilder bob = new OmsBobTheBuilder();
        bob.pm = pm;
        bob.inRaster = flatRaster;
        bob.inArea = area;
        bob.inElevations = points(elevation);
        bob.fElevation = "userdata";
        // all the points are used for every cell
        bob.pMaxbuffer = 1000;
        bob.doErode = doErode;
        bob.process();
        return bob.outRaster.getRenderedImage().getData();
    }

    public void testBuildOnlyRaises() throws Exception {
        Raster out = build(110, false);
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double value = out.getSampleDouble(c, r, 0);
                boolean inside = r >= 2 && r <= 4 && c >= 2 && c <= 5;
                if (c == 2) {
                    // higher than the artifact: kept
                    assertEquals(120.0, value, DELTA);
                } else if (inside) {
                    assertEquals(110.0, value, DELTA);
                } else {
                    assertEquals(100.0, value, DELTA);
                }
            }
        }
    }

    public void testBuildWithErosion() throws Exception {
        Raster out = build(90, true);
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                double value = out.getSampleDouble(c, r, 0);
                boolean inside = r >= 2 && r <= 4 && c >= 2 && c <= 5;
                if (inside) {
                    assertEquals(90.0, value, DELTA);
                } else {
                    assertEquals(c == 2 ? 120.0 : 100.0, value, DELTA);
                }
            }
        }
    }

    public void testWithoutErosionLowerArtifactChangesNothing() throws Exception {
        Raster out = build(90, false);
        for( int r = 0; r < region.rows; r++ ) {
            for( int c = 0; c < region.cols; c++ ) {
                assertEquals(c == 2 ? 120.0 : 100.0, out.getSampleDouble(c, r, 0), DELTA);
            }
        }
    }
}
