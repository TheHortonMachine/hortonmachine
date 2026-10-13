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

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer;
import org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/**
 * Test for {@link OmsPointsRasterizer} and {@link OmsLinesRasterizer}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestPointsLinesRasterizer extends HMTestCase {
    private static final double NV = HMConstants.doubleNovalue;

    private CoordinateReferenceSystem crs = HMTestMaps.getCrs();
    // 5 columns and 4 rows of 1 m
    private RegionMap region = RegionMap.fromBoundsAndGrid(0, 5, 0, 4, 5, 4);

    public void testLines() throws Exception {
        DefaultFeatureCollection lines = new DefaultFeatureCollection();
        // a diagonal from the north-west cell to the south-east one
        addFeature(lines, LineString.class, 2, new Coordinate(0.5, 3.5), new Coordinate(4.5, 0.5));
        // a vertical one on the first column
        addFeature(lines, LineString.class, 5, new Coordinate(0.5, 0.5), new Coordinate(0.5, 1.5));
        // a line coming from outside of the grid on the first row, larger than the diagonal where they cross
        addFeature(lines, LineString.class, 7, new Coordinate(-3, 3.5), new Coordinate(1.5, 3.5));

        OmsLinesRasterizer rasterizer = new OmsLinesRasterizer();
        rasterizer.pm = pm;
        rasterizer.inVector = lines;
        rasterizer.fCat = "CAT";
        rasterizer.pNorth = region.getNorth();
        rasterizer.pSouth = region.getSouth();
        rasterizer.pWest = region.getWest();
        rasterizer.pEast = region.getEast();
        rasterizer.pRows = region.getRows();
        rasterizer.pCols = region.getCols();
        rasterizer.process();

        double[][] expected = new double[][]{//
                {7, 7, NV, NV, NV}, //
                {NV, 2, NV, NV, NV}, //
                {5, NV, 2, 2, NV}, //
                {5, NV, NV, NV, 2}};
        checkMatrixEqual(rasterizer.outRaster.getRenderedImage(), expected, 0);
    }

    public void testLinesOnRasterWithConstant() throws Exception {
        DefaultFeatureCollection lines = new DefaultFeatureCollection();
        addFeature(lines, LineString.class, 2, new Coordinate(0.5, 0.5), new Coordinate(4.5, 0.5));

        OmsLinesRasterizer rasterizer = new OmsLinesRasterizer();
        rasterizer.pm = pm;
        rasterizer.inVector = lines;
        rasterizer.pCat = 3;
        rasterizer.inRaster = template();
        rasterizer.process();

        double[][] expected = new double[][]{//
                {NV, NV, NV, NV, NV}, //
                {NV, NV, NV, NV, NV}, //
                {NV, NV, NV, NV, NV}, //
                {3, 3, 3, 3, 3}};
        checkMatrixEqual(rasterizer.outRaster.getRenderedImage(), expected, 0);
    }

    public void testPoints() throws Exception {
        DefaultFeatureCollection points = new DefaultFeatureCollection();
        addFeature(points, Point.class, 3, new Coordinate(0.5, 0.5));
        // on the corner of four cells: it goes to the south-east one
        addFeature(points, Point.class, 4, new Coordinate(2.0, 2.0));
        // on the east and south borders of the grid: in the last column and row
        addFeature(points, Point.class, 8, new Coordinate(5.0, 2.5));
        addFeature(points, Point.class, 6, new Coordinate(3.5, 0.0));
        // outside of the grid: left out
        addFeature(points, Point.class, 8, new Coordinate(10, 10));
        // two points in the same cell: the largest wins, whatever the order
        addFeature(points, Point.class, 9, new Coordinate(1.8, 3.1));
        addFeature(points, Point.class, 1, new Coordinate(1.2, 3.7));

        OmsPointsRasterizer rasterizer = new OmsPointsRasterizer();
        rasterizer.pm = pm;
        rasterizer.inVector = points;
        rasterizer.fCat = "cat";
        rasterizer.inGrid = CoverageUtilities.gridGeometryFromRegionParams(region, crs);
        rasterizer.process();

        double[][] expected = new double[][]{//
                {NV, 9, NV, NV, NV}, //
                {NV, NV, NV, NV, 8}, //
                {NV, NV, 4, NV, NV}, //
                {3, NV, NV, 6, NV}};
        checkMatrixEqual(rasterizer.outRaster.getRenderedImage(), expected, 0);
    }

    public void testPointsOnRasterWithConstant() throws Exception {
        DefaultFeatureCollection points = new DefaultFeatureCollection();
        addFeature(points, Point.class, 3, new Coordinate(4.5, 3.5));

        OmsPointsRasterizer rasterizer = new OmsPointsRasterizer();
        rasterizer.pm = pm;
        rasterizer.inVector = points;
        rasterizer.inRaster = template();
        rasterizer.process();

        double[][] expected = new double[][]{//
                {NV, NV, NV, NV, 1}, //
                {NV, NV, NV, NV, NV}, //
                {NV, NV, NV, NV, NV}, //
                {NV, NV, NV, NV, NV}};
        checkMatrixEqual(rasterizer.outRaster.getRenderedImage(), expected, 0);
    }

    private GridCoverage2D template() {
        return CoverageUtilities.buildCoverage("template", new double[4][5], region, crs, true);
    }

    private void addFeature( DefaultFeatureCollection collection, Class< ? extends Geometry> geometryClass, double cat,
            Coordinate... coordinates ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(crs);
        b.add("the_geom", geometryClass);
        b.add("cat", Double.class);
        SimpleFeatureType type = b.buildFeatureType();

        Geometry geometry;
        if (geometryClass == Point.class) {
            geometry = GeometryUtilities.gf().createPoint(coordinates[0]);
        } else {
            geometry = GeometryUtilities.gf().createLineString(coordinates);
        }
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        builder.addAll(new Object[]{geometry, cat});
        collection.add(builder.buildFeature(null));
    }

}
