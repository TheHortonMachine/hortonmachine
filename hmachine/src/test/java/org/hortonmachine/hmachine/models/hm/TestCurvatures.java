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
package org.hortonmachine.hmachine.models.hm;

import java.io.IOException;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Tests the {@link OmsCurvatures} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCurvatures extends HMTestCase {

    public void testCurvatures() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] pitfillerData = HMTestMaps.pitData;
        GridCoverage2D pitfillerCoverage = CoverageUtilities.buildCoverage("pitfiller", pitfillerData, envelopeParams, crs, true);

        OmsCurvatures curvatures = new OmsCurvatures();
        curvatures.inElev = pitfillerCoverage;
        curvatures.pm = pm;

        curvatures.process();

        GridCoverage2D profCoverage = curvatures.outProf;
        GridCoverage2D planCoverage = curvatures.outPlan;
        GridCoverage2D tangCoverage = curvatures.outTang;

        checkMatrixEqual(profCoverage.getRenderedImage(), HMTestMaps.profData, 0.0001);
        checkMatrixEqual(planCoverage.getRenderedImage(), HMTestMaps.planData, 0.0001);
        checkMatrixEqual(tangCoverage.getRenderedImage(), HMTestMaps.tanData, 0.0001);
    }
}
