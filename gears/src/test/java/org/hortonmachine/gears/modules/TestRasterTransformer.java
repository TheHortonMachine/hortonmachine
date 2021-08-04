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

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Geometry;
/**
 * Test {@link OmsRasterTransformer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterTransformer extends HMTestCase {

    public void testRasterTransformer() throws Exception {

        int[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        OmsRasterTransformer transformer = new OmsRasterTransformer();
        transformer.inRaster = flowCoverage;
        transformer.pInterpolation = Variables.BICUBIC;
        transformer.pAngle = 90.0;
        transformer.pTransX = 100.0;
        transformer.pTransY = 100.0;
        transformer.process();
        // GridCoverage2D outCoverage = transformer.outRaster;
        SimpleFeatureCollection outBounds = transformer.outBounds;
        Geometry bound = FeatureUtilities.featureCollectionToGeometriesList(outBounds, false, null).get(0);

        String expected = "POLYGON ((1640780 5140150, 1641020 5140150, 1641020 5139850, 1640780 5139850, 1640780 5140150))";
        assertEquals(expected, bound.toText());

    }
}
