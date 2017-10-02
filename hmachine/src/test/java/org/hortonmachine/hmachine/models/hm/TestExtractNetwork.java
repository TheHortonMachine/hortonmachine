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
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.hortonmachine.hmachine.modules.network.networkattributes.NetworkChannel;
import org.hortonmachine.hmachine.modules.network.networkattributes.OmsNetworkAttributesBuilder;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * It test the {@link OmsExtractNetwork} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestExtractNetwork extends HMTestCase {
    
    /**
    * Test module with mode=0.
    */
    public void testExtractNetwork0() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);

        OmsExtractNetwork extractNetwork = new OmsExtractNetwork();
        extractNetwork.pm = pm;
        extractNetwork.inFlow = flowCoverage;
        extractNetwork.inTca = tcaCoverage;
        extractNetwork.pMode = Variables.TCA;
        extractNetwork.pThres = 5;
        extractNetwork.process();

        GridCoverage2D networkCoverage = extractNetwork.outNet;
        checkMatrixEqual(networkCoverage.getRenderedImage(), HMTestMaps.extractNet0Data, 0.01);
    }

    /**
    * Test module with mode=1.
    */
    public void testExtractNetwork1() throws Exception {

        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);
        double[][] slopeData = HMTestMaps.slopeData;
        GridCoverage2D slopeCoverage = CoverageUtilities.buildCoverage("slope", slopeData, envelopeParams, crs, true);

        OmsExtractNetwork extractNetwork = new OmsExtractNetwork();
        extractNetwork.pm = pm;
        extractNetwork.inFlow = flowCoverage;
        extractNetwork.inTca = tcaCoverage;
        extractNetwork.inSlope = slopeCoverage;
        extractNetwork.pMode = Variables.TCA_SLOPE;
        extractNetwork.pThres = 8;
        extractNetwork.process();

        GridCoverage2D networkCoverage = extractNetwork.outNet;

        checkMatrixEqual(networkCoverage.getRenderedImage(), HMTestMaps.extractNet1Data, 0.01);
    }

    public void testOmsNetworkAttributesBuilder() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet0Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        OmsNetworkAttributesBuilder extractNetwork = new OmsNetworkAttributesBuilder();
        extractNetwork.pm = pm;
        extractNetwork.inFlow = flowCoverage;
        extractNetwork.inTca = tcaCoverage;
        extractNetwork.inNet = netCoverage;
        extractNetwork.process();
        SimpleFeatureCollection networkFC = extractNetwork.outNet;

        List<FeatureMate> matesList = FeatureUtilities.featureCollectionToMatesList(networkFC);
        for( FeatureMate featureMate : matesList ) {
            if (featureMate.getAttribute(NetworkChannel.PFAFNAME, String.class).equals("1")) {
                assertEquals(1, featureMate.getAttribute(NetworkChannel.HACKNAME, Integer.class).intValue());
                assertEquals(2, featureMate.getAttribute(NetworkChannel.STRAHLERNAME, Integer.class).intValue());
                assertEquals(
                        "LINESTRING (1640845 5139885, 1640815 5139885, 1640785 5139885, 1640755 5139885, 1640725 5139885, 1640695 5139915)",
                        featureMate.getGeometry().toText());
            } else if (featureMate.getAttribute(NetworkChannel.PFAFNAME, String.class).equals("3")) {
                assertEquals(1, featureMate.getAttribute(NetworkChannel.HACKNAME, Integer.class).intValue());
                assertEquals(1, featureMate.getAttribute(NetworkChannel.STRAHLERNAME, Integer.class).intValue());
                assertEquals("LINESTRING (1640875 5139885, 1640845 5139885)", featureMate.getGeometry().toText());
            } else if (featureMate.getAttribute(NetworkChannel.PFAFNAME, String.class).equals("2.1")) {
                assertEquals(2, featureMate.getAttribute(NetworkChannel.HACKNAME, Integer.class).intValue());
                assertEquals(1, featureMate.getAttribute(NetworkChannel.STRAHLERNAME, Integer.class).intValue());
                assertEquals("LINESTRING (1640875 5139915, 1640845 5139885)", featureMate.getGeometry().toText());
            } else {
                throw new RuntimeException();
            }
        }
    }

}
