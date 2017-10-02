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
import org.hortonmachine.hmachine.modules.hydrogeomorphology.shalstab.OmsShalstab;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsShalstab} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestShalstab extends HMTestCase {
    public void testShalstab() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] slopeData = HMTestMaps.slopeData;
        GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("slope", slopeData, envelopeParams, crs, true);
        double[][] abData = HMTestMaps.abData;
        GridCoverage2D abCoverage = CoverageUtilities.buildCoverage("ab", abData, envelopeParams, crs, true);
        
        OmsShalstab shalstab = new OmsShalstab();
        shalstab.inSlope = slopeCoverage;
        shalstab.inTca = abCoverage;
        shalstab.pTrasmissivity = 0.001;
        shalstab.pCohesion = 0.0;
        shalstab.pSdepth = 2.0;
        shalstab.pRho = 1.6;
        shalstab.pTgphi = 0.7;
        shalstab.pQ = 0.05;
        shalstab.pm = pm;

        shalstab.process();

        GridCoverage2D qcritCoverage = shalstab.outQcrit;
        GridCoverage2D classiCoverage = shalstab.outShalstab;

        checkMatrixEqual(qcritCoverage.getRenderedImage(), HMTestMaps.qcritmapData, 0);
        checkMatrixEqual(classiCoverage.getRenderedImage(), HMTestMaps.classimapData, 0);
    }

}
