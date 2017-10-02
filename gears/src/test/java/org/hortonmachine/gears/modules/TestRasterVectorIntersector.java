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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.rastervectorintersection.OmsRasterVectorIntersector;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test for the {@link OmsRasterVectorIntersector}
 * <
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterVectorIntersector extends HMTestCase {

    @SuppressWarnings("nls")
    public void testRasterVectorIntersector() throws Exception {
        RegionMap ep = HMTestMaps.getEnvelopeparams();
        SimpleFeatureCollection testLeftFC = HMTestMaps.getTestLeftFC();

        double[][] elevationData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, ep, crs, true);

        double[][] test = new double[elevationData.length][elevationData[0].length];
        double[][] testInverse = new double[elevationData.length][elevationData[0].length];
        for( int i = 0; i < elevationData.length; i++ ) {
            for( int j = 0; j < elevationData[0].length; j++ ) {
                if (j < 5) {
                    // hirst half
                    test[i][j] = elevationData[i][j];
                    testInverse[i][j] = HMConstants.doubleNovalue;
                } else {
                    test[i][j] = HMConstants.doubleNovalue;
                    testInverse[i][j] = elevationData[i][j];
                }
            }
        }

        OmsRasterVectorIntersector intersect = new OmsRasterVectorIntersector();
        intersect.inRaster = elevationCoverage;
        intersect.inVector = testLeftFC;
        intersect.process();
        GridCoverage2D outRaster = intersect.outRaster;
        checkMatrixEqual(outRaster.getRenderedImage(), test);

        intersect = new OmsRasterVectorIntersector();
        intersect.inRaster = elevationCoverage;
        intersect.inVector = testLeftFC;
        intersect.doInverse = true;
        intersect.process();
        outRaster = intersect.outRaster;
        checkMatrixEqual(outRaster.getRenderedImage(), testInverse);
    }

}
