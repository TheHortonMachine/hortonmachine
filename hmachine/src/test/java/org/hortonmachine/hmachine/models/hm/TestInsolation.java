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
import org.hortonmachine.hmachine.modules.hydrogeomorphology.insolation.OmsInsolation;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsInsolation} module.
 * 
 * @author Daniele Andreis
 */
public class TestInsolation extends HMTestCase {

    private final static String START_DATE = "2010-01-01";
    private final static String END_DATE = "2010-01-02";

    public void testInsolation() throws Exception {

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs3004();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);

        OmsInsolation insolation = new OmsInsolation();
        insolation.inElev = elevationCoverage;
        insolation.tStartDate = START_DATE;
        insolation.tEndDate = END_DATE;
        // insolation.defaultLapse=-.0065;
        // insolation.defaultRH=0.4;
        // insolation.defaultVisibility=60;

        insolation.pm = pm;

        insolation.process();

        GridCoverage2D insolationCoverage = insolation.outIns;

        checkMatrixEqual(insolationCoverage.getRenderedImage(), HMTestMaps.outInsolation, 0.1);
    }

}
