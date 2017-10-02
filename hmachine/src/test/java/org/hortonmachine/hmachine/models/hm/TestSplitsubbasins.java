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
import org.hortonmachine.hmachine.modules.demmanipulation.splitsubbasin.OmsSplitSubbasins;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsSplitSubbasins} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestSplitsubbasins extends HMTestCase {
    @SuppressWarnings("nls")
    public void testSplitsubbasins() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        double[][] drainData = HMTestMaps.drainData1;
        GridCoverage2D drainCoverage = CoverageUtilities.buildCoverage("drain", drainData, envelopeParams, crs, true);
        double[][] hackstreamData = HMTestMaps.hackstream;
        GridCoverage2D hackstreamCoverage = CoverageUtilities.buildCoverage("drain", hackstreamData, envelopeParams, crs, true);

        OmsSplitSubbasins pitfiller = new OmsSplitSubbasins();
        pitfiller.inFlow = drainCoverage;
        pitfiller.inHack = hackstreamCoverage;
        pitfiller.pHackorder = 3.0;
        pitfiller.pm = pm;
        pitfiller.process();

        GridCoverage2D splitBasins = pitfiller.outSubbasins;
        // GridCoverage2D splitNet = pitfiller.outNetnum;

        checkMatrixEqual(splitBasins.getRenderedImage(), HMTestMaps.splitSubBasinData_withHackOrder3, 0.00001);

    }
}
