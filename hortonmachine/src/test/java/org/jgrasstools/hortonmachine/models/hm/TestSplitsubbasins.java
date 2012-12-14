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
import org.jgrasstools.hortonmachine.modules.demmanipulation.splitsubbasin.SplitSubbasins;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link SplitSubbasins} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestSplitsubbasins extends HMTestCase {
    @SuppressWarnings("nls")
    public void testSplitsubbasins() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        double[][] drainData = HMTestMaps.drainData1;
        GridCoverage2D drainCoverage = CoverageUtilities.buildCoverage("drain", drainData, envelopeParams, crs, true);
        double[][] hackstreamData = HMTestMaps.hackstream;
        GridCoverage2D hackstreamCoverage = CoverageUtilities.buildCoverage("drain", hackstreamData, envelopeParams, crs, true);

        SplitSubbasins pitfiller = new SplitSubbasins();
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
