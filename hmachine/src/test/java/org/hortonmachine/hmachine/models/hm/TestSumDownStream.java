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
import org.hortonmachine.hmachine.modules.statistics.sumdownstream.OmsSumDownStream;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * It test the {@link TestSumDownStream} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestSumDownStream extends HMTestCase {
    public void testSumDownstream() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.netFlowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true); //$NON-NLS-1$
        double[][] toSumData = HMTestMaps.netOneData;
        GridCoverage2D toSumCoverage = CoverageUtilities.buildCoverage("tosum", toSumData, envelopeParams, crs, true); //$NON-NLS-1$

        OmsSumDownStream sumDownstream = new OmsSumDownStream();
        sumDownstream.pm = pm;
        sumDownstream.inFlow = flowCoverage;
        sumDownstream.inToSum = toSumCoverage;
        sumDownstream.process();
        GridCoverage2D summedCoverage = sumDownstream.outSummed;
        checkMatrixEqual(summedCoverage.getRenderedImage(), HMTestMaps.sumDownstreamData, 0.01);

        // with threshold
        double[][] toSumThresData = HMTestMaps.netOneThresData;
        GridCoverage2D toSumThresCoverage = CoverageUtilities.buildCoverage(
                "tosumthres", toSumThresData, envelopeParams, crs, true); //$NON-NLS-1$
        sumDownstream = new OmsSumDownStream();
        sumDownstream.pm = pm;
        sumDownstream.inFlow = flowCoverage;
        sumDownstream.inToSum = toSumThresCoverage;
        sumDownstream.pUpperThres = 2.0;
        sumDownstream.pLowerThres = 0.0;
        sumDownstream.process();
        summedCoverage = sumDownstream.outSummed;

        printImage(summedCoverage.getRenderedImage());
        checkMatrixEqual(summedCoverage.getRenderedImage(), HMTestMaps.sumDownstreamThresData, 0.01);
    }

}
