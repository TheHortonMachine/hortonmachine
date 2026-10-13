/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com
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

import org.eclipse.imagen.iterator.RandomIter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Geometry;

/**
 * Test for {@link OmsRasterTransformer}.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterTransformer extends HMTestCase {
    private static final double NV = HMConstants.doubleNovalue;

    // 3 columns and 2 rows of 1 m, from 0,0 to 3,2
    private GridCoverage2D raster = CoverageUtilities.buildCoverage("raster", new double[][]{//
            {1, 2, 3}, //
            {4, 5, NV}}, RegionMap.fromBoundsAndGrid(0, 3, 0, 2, 3, 2), HMTestMaps.getCrs(), true);

    public void testFlips() throws Exception {
        OmsRasterTransformer transformer = transformer();
        transformer.doFlipHorizontal = true;
        transformer.process();
        checkMatrixEqual(transformer.outRaster.getRenderedImage(), new double[][]{{3, 2, 1}, {NV, 5, 4}}, 0);
        assertRegion(transformer.outRaster, 0, 3, 0, 2);

        transformer = transformer();
        transformer.doFlipVertical = true;
        transformer.process();
        checkMatrixEqual(transformer.outRaster.getRenderedImage(), new double[][]{{4, 5, NV}, {1, 2, 3}}, 0);
    }

    public void testRotation90() throws Exception {
        OmsRasterTransformer transformer = transformer();
        transformer.pAngle = 90.0;
        transformer.process();

        // counterclockwise: the north row becomes the west column, read from south to north
        double[][] expected = new double[][]{//
                {3, NV}, //
                {2, 5}, //
                {1, 4}};
        checkMatrixEqual(transformer.outRaster.getRenderedImage(), expected, 0);
        // turned around the center 1.5,1
        assertRegion(transformer.outRaster, 0.5, 2.5, -0.5, 2.5);
        Geometry outline = FeatureUtilities.featureCollectionToGeometriesList(transformer.outBounds, false, null).get(0);
        assertEquals(6.0, outline.getArea(), 1E-9);
        assertEquals(0.5, outline.getEnvelopeInternal().getMinX(), 1E-9);
    }

    public void testScaleAndTranslation() throws Exception {
        OmsRasterTransformer transformer = transformer();
        transformer.pScaleX = 2.0;
        transformer.pScaleY = 2.0;
        transformer.pTransX = 10.0;
        transformer.pTransY = 20.0;
        transformer.process();

        // the values don't change, the cells double around the center 1.5,1 and then move
        checkMatrixEqual(transformer.outRaster.getRenderedImage(), new double[][]{{1, 2, 3}, {4, 5, NV}}, 0);
        assertRegion(transformer.outRaster, 8.5, 14.5, 19.0, 23.0);
        Geometry outline = FeatureUtilities.featureCollectionToGeometriesList(transformer.outBounds, false, null).get(0);
        assertEquals(24.0, outline.getArea(), 1E-9);
    }

    public void testBilinearRotation() throws Exception {
        // a ramp along the columns: the bilinear interpolation of a plane is exact
        int size = 9;
        double[][] ramp = new double[size][size];
        for( int r = 0; r < size; r++ ) {
            for( int c = 0; c < size; c++ ) {
                ramp[r][c] = c;
            }
        }
        RegionMap region = RegionMap.fromBoundsAndGrid(0, size, 0, size, size, size);
        OmsRasterTransformer transformer = new OmsRasterTransformer();
        transformer.pm = pm;
        transformer.inRaster = CoverageUtilities.buildCoverage("ramp", ramp, region, HMTestMaps.getCrs(), true);
        transformer.pAngle = 30.0;
        transformer.pInterpolation = Variables.BILINEAR;
        transformer.process();

        RegionMap outRegion = RegionMap.fromGridGeometry(transformer.outRaster.getGridGeometry());
        double cos = Math.cos(Math.toRadians(30));
        double sin = Math.sin(Math.toRadians(30));
        double pivot = size / 2.0;
        RandomIter iter = CoverageUtilities.getRandomIterator(transformer.outRaster);
        int checked = 0;
        for( int r = 0; r < outRegion.getRows(); r++ ) {
            for( int c = 0; c < outRegion.getCols(); c++ ) {
                double x = outRegion.getWest() + (c + 0.5) * outRegion.getXres() - pivot;
                double y = outRegion.getNorth() - (r + 0.5) * outRegion.getYres() - pivot;
                // the source point, rotated back
                double sourceX = pivot + x * cos + y * sin;
                double sourceY = pivot - x * sin + y * cos;
                if (sourceX > 1 && sourceX < size - 1 && sourceY > 1 && sourceY < size - 1) {
                    assertEquals(sourceX - 0.5, iter.getSampleDouble(c, r, 0), 1E-9);
                    checked++;
                } else if (sourceX < 0 || sourceX > size || sourceY < 0 || sourceY > size) {
                    assertEquals(NV, iter.getSampleDouble(c, r, 0), 0);
                }
            }
        }
        iter.done();
        assertTrue(checked > 20);
    }

    private OmsRasterTransformer transformer() {
        OmsRasterTransformer transformer = new OmsRasterTransformer();
        transformer.pm = pm;
        transformer.inRaster = raster;
        return transformer;
    }

    private void assertRegion( GridCoverage2D coverage, double west, double east, double south, double north ) {
        RegionMap region = RegionMap.fromGridGeometry(coverage.getGridGeometry());
        assertEquals(west, region.getWest(), 1E-9);
        assertEquals(east, region.getEast(), 1E-9);
        assertEquals(south, region.getSouth(), 1E-9);
        assertEquals(north, region.getNorth(), 1E-9);
    }
}
