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
import org.jgrasstools.hortonmachine.modules.geomorphology.aspect.Aspect;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Aspect} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestAspect extends HMTestCase {
    public void testAspectDegrees() throws Exception {
        double[][] pitData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs, true);

        Aspect aspect = new Aspect();
        aspect.inDem = pitCoverage;
        aspect.doRound = true;
        aspect.pm = pm;

        aspect.process();

        GridCoverage2D aspectCoverage = aspect.outAspect;

        checkMatrixEqual(aspectCoverage.getRenderedImage(), HMTestMaps.aspectDataDegrees, 0.01);
    }

    public void testAspectRadiants() throws Exception {
        double[][] pitData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D pitCoverage = CoverageUtilities.buildCoverage("pit", pitData, envelopeParams, crs, true);

        Aspect aspect = new Aspect();
        aspect.inDem = pitCoverage;
        aspect.doRadiants = true;
        aspect.pm = pm;

        aspect.process();

        GridCoverage2D aspectCoverage = aspect.outAspect;

        checkMatrixEqual(aspectCoverage.getRenderedImage(), HMTestMaps.aspectDataRadiants, 0.01);
    }

}
