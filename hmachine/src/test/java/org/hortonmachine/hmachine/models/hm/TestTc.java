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

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.hillslopeanalyses.tc.OmsTc;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Tests the {@link OmsTc} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Daniele Andreis
 */
public class TestTc extends HMTestCase {

    @SuppressWarnings("nls")
    public void testTc() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] tanData = HMTestMaps.tanData;
        double[][] profData = HMTestMaps.profData;

        GridCoverage2D tanRaster = CoverageUtilities.buildCoverage("tan", tanData, envelopeParams, crs, true);
        GridCoverage2D profRaster = CoverageUtilities.buildCoverage("prof", profData, envelopeParams, crs, true);

        OmsTc tc = new OmsTc();
        tc.pTanthres = 0.02;
        tc.pProfthres = 0.0017;
        tc.inProf = profRaster;
        tc.inTan = tanRaster;
        tc.process();
        GridCoverage2D outTc3 = tc.outTc3;
        GridCoverage2D outTc9 = tc.outTc9;

        checkMatrixEqual(outTc3.getRenderedImage(), HMTestMaps.cp3Data);
        checkMatrixEqual(outTc9.getRenderedImage(), HMTestMaps.cp9Data);

    }

}
