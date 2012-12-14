/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.modules.r.rastervectorintersection.RasterVectorIntersector;
import org.jgrasstools.gears.ui.MapsViewer;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test for the {@link RasterVectorIntersector}
 * <
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterVectorIntersector extends HMTestCase {

    @SuppressWarnings("nls")
    public void testRasterVectorIntersector() throws Exception {
        RegionMap ep = HMTestMaps.envelopeParams;
        SimpleFeatureCollection testLeftFC = HMTestMaps.testLeftFC;

        double[][] elevationData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, ep, crs, true);

        double[][] test = new double[elevationData.length][elevationData[0].length];
        double[][] testInverse = new double[elevationData.length][elevationData[0].length];
        for( int i = 0; i < elevationData.length; i++ ) {
            for( int j = 0; j < elevationData[0].length; j++ ) {
                if (j < 5) {
                    // hirst half
                    test[i][j] = elevationData[i][j];
                    testInverse[i][j] = JGTConstants.doubleNovalue;
                } else {
                    test[i][j] = JGTConstants.doubleNovalue;
                    testInverse[i][j] = elevationData[i][j];
                }
            }
        }

        RasterVectorIntersector intersect = new RasterVectorIntersector();
        intersect.inRaster = elevationCoverage;
        intersect.inVector = testLeftFC;
        intersect.process();
        GridCoverage2D outRaster = intersect.outRaster;
        checkMatrixEqual(outRaster.getRenderedImage(), test);

        intersect = new RasterVectorIntersector();
        intersect.inRaster = elevationCoverage;
        intersect.inVector = testLeftFC;
        intersect.doInverse = true;
        intersect.process();
        outRaster = intersect.outRaster;
        checkMatrixEqual(outRaster.getRenderedImage(), testInverse);
    }

}
