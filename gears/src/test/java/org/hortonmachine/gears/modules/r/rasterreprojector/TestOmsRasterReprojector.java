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
package org.hortonmachine.gears.modules.r.rasterreprojector;

import java.awt.geom.Point2D;
import java.awt.image.Raster;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * Test {@link OmsRasterReprojector} with a given cell size.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestOmsRasterReprojector extends HMTestCase {

    /**
     * A UTM 32N raster of 20 x 16 cells of 30 m, with the value of every cell row * 100 + col.
     */
    private GridCoverage2D utmRaster() throws Exception {
        CoordinateReferenceSystem utm = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632");
        RegionMap region = RegionMap.fromBoundsAndGrid(650000, 650600, 5100000, 5100480, 20, 16);
        HMRaster raster = new HMRasterWritableBuilder().setRegion(region).setCrs(utm).setNoValue(-9999.0)
                .setInitialValue(-9999.0).build();
        for( int row = 0; row < 16; row++ ) {
            for( int col = 0; col < 20; col++ ) {
                raster.setValue(col, row, row * 100.0 + col);
            }
        }
        return raster.buildCoverage();
    }

    public void testGivenCellSizeIsExactAndAligned() throws Exception {
        GridCoverage2D source = utmRaster();
        OmsRasterReprojector reprojector = new OmsRasterReprojector();
        reprojector.pm = pm;
        reprojector.inRaster = source;
        reprojector.pCode = "EPSG:3035";
        reprojector.pXres = 50.0;
        reprojector.pYres = 40.0;
        reprojector.process();
        GridCoverage2D out = reprojector.outRaster;

        ReferencedEnvelope envelope = out.getEnvelope2D();
        int cols = out.getRenderedImage().getWidth();
        int rows = out.getRenderedImage().getHeight();
        // exactly the given cell size, the bounds on multiples of it
        assertEquals(50.0, envelope.getWidth() / cols, 1E-6);
        assertEquals(40.0, envelope.getHeight() / rows, 1E-6);
        assertEquals(0.0, Math.IEEEremainder(envelope.getMinX(), 50.0), 1E-6);
        assertEquals(0.0, Math.IEEEremainder(envelope.getMaxY(), 40.0), 1E-6);

        // the whole source is covered: every source cell center is inside the output
        CoordinateReferenceSystem laea = HMCrsRegistry.INSTANCE.getCrs("EPSG:3035");
        HMCrsTransformer toLaea = new HMCrsTransformer(source.getCoordinateReferenceSystem(), laea);
        for( int row = 0; row < 16; row++ ) {
            for( int col = 0; col < 20; col++ ) {
                Coordinate center = toLaea.transform(new Coordinate(650015 + col * 30, 5100465 - row * 30));
                assertTrue(envelope.contains(center.x, center.y));
            }
        }

        // nearest neighbour: the cells take the value of the source cell their center falls in
        HMCrsTransformer toUtm = new HMCrsTransformer(laea, source.getCoordinateReferenceSystem());
        Raster data = out.getRenderedImage().getData();
        Envelope sourceEnvelope = new Envelope(650000, 650600, 5100000, 5100480);
        int checked = 0;
        for( int row = 0; row < rows; row++ ) {
            for( int col = 0; col < cols; col++ ) {
                double x = envelope.getMinX() + (col + 0.5) * 50;
                double y = envelope.getMaxY() - (row + 0.5) * 40;
                Coordinate utmCenter = toUtm.transform(new Coordinate(x, y));
                // away from the source border, where the outside is novalue
                Envelope inner = new Envelope(sourceEnvelope);
                inner.expandBy(-31);
                if (!inner.contains(utmCenter)) {
                    continue;
                }
                double expected = source.evaluate(new Point2D.Double(utmCenter.x, utmCenter.y), new double[1])[0];
                assertEquals(expected, data.getSampleDouble(col, row, 0), DELTA);
                checked++;
            }
        }
        assertTrue(checked > 50);
    }

    public void testAlignedGridGeometry() throws Exception {
        CoordinateReferenceSystem utm = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632");
        // bounds already on the grid stay where they are
        GridGeometry2D grid = OmsRasterReprojector.alignedGridGeometry(new Envelope(650000, 651000, 5100000, 5100500), utm, utm,
                100, 50);
        ReferencedEnvelope envelope = grid.getEnvelope2D();
        assertEquals(650000.0, envelope.getMinX(), 1E-6);
        assertEquals(651000.0, envelope.getMaxX(), 1E-6);
        assertEquals(5100000.0, envelope.getMinY(), 1E-6);
        assertEquals(5100500.0, envelope.getMaxY(), 1E-6);
        assertEquals(10, grid.getGridRange2D().width);
        assertEquals(10, grid.getGridRange2D().height);

        // the others are enlarged to the next multiples
        grid = OmsRasterReprojector.alignedGridGeometry(new Envelope(650010, 650990, 5100001, 5100499), utm, utm, 100, 50);
        envelope = grid.getEnvelope2D();
        assertEquals(650000.0, envelope.getMinX(), 1E-6);
        assertEquals(651000.0, envelope.getMaxX(), 1E-6);
        assertEquals(5100000.0, envelope.getMinY(), 1E-6);
        assertEquals(5100500.0, envelope.getMaxY(), 1E-6);
    }
}
