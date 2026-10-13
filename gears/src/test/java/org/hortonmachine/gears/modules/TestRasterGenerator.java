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

import org.eclipse.imagen.iterator.RandomIter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.modules.r.rastergenerator.OmsRasterGenerator;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

/**
 * Test for {@link OmsRasterGenerator}
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestRasterGenerator extends HMTestCase {

    public void testConstant() throws Exception {
        // an extent that isn't a multiple of the cell size
        GridCoverage2D raster = generate(false, null);

        RegionMap region = RegionMap.fromGridGeometry(raster.getGridGeometry());
        assertEquals(4, region.getCols());
        assertEquals(3, region.getRows());
        assertEquals(10.0, region.getXres(), 1E-9);
        assertEquals(10.0, region.getYres(), 1E-9);
        // anchored at the north-west corner
        assertEquals(1000.0, region.getWest(), 1E-9);
        assertEquals(1040.0, region.getEast(), 1E-9);
        assertEquals(5000.0, region.getNorth(), 1E-9);
        assertEquals(4970.0, region.getSouth(), 1E-9);
        assertTrue(HMCrsRegistry.crsEquals(HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"), raster.getCoordinateReferenceSystem()));

        checkEqualsSinlgeValue(raster.getRenderedImage(), 7.0, 0);
    }

    public void testRandomWithSeed() throws Exception {
        GridCoverage2D raster1 = generate(true, 42L);
        GridCoverage2D raster2 = generate(true, 42L);

        RandomIter iter1 = CoverageUtilities.getRandomIterator(raster1);
        RandomIter iter2 = CoverageUtilities.getRandomIterator(raster2);
        for( int r = 0; r < 3; r++ ) {
            for( int c = 0; c < 4; c++ ) {
                double value = iter1.getSampleDouble(c, r, 0);
                // between the offset and the offset plus the scale
                assertTrue(value >= 100 && value < 150);
                assertEquals(value, iter2.getSampleDouble(c, r, 0), 0);
            }
        }
        iter1.done();
        iter2.done();
    }

    private GridCoverage2D generate( boolean doRandom, Long seed ) throws Exception {
        OmsRasterGenerator generator = new OmsRasterGenerator();
        generator.pm = pm;
        generator.pWest = 1000.0;
        generator.pEast = 1038.0;
        generator.pSouth = 4972.0;
        generator.pNorth = 5000.0;
        generator.pXres = 10.0;
        generator.pYres = 10.0;
        generator.pCode = "EPSG:32632";
        generator.pValue = 7;
        generator.doRandom = doRandom;
        generator.pOffset = 100;
        generator.pScale = 50;
        generator.pSeed = seed;
        generator.process();
        return generator.outRaster;
    }

}
