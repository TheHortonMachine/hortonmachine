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
package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.shalstab.Shalstab;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Shalstab} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestShalstab extends HMTestCase {
    public void testShalstab() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] slopeData = HMTestMaps.slopeData;
        GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("slope", slopeData, envelopeParams, crs, true);
        double[][] abData = HMTestMaps.abData;
        GridCoverage2D abCoverage = CoverageUtilities.buildCoverage("ab", abData, envelopeParams, crs, true);
        
        Shalstab shalstab = new Shalstab();
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
