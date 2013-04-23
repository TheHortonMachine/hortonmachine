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
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.io.rasterreader.OmsRasterReader;
import org.jgrasstools.gears.io.rasterwriter.OmsRasterWriter;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.Variables;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.hortonmachine.modules.network.extractnetwork.OmsExtractNetwork;
import org.jgrasstools.hortonmachine.modules.network.networkattributes.NetworkChannel;
import org.jgrasstools.hortonmachine.modules.network.networkattributes.OmsNetworkAttributesBuilder;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
/**
 * It test the {@link OmsExtractNetwork} module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestExtractNetwork extends HMTestCase {

    public static void main( String[] args ) throws Exception {

        String base = "/home/moovida/Dropbox/hydrologis/lavori/2013_04_idrologico_taggia/test_modelli/raster/";
        String inFlow = base + "basin_merelli_mdrain.asc";
        String inTca = base + "basin_merelli_tca.asc";
        String inNet = base + "basin_merelli_net5000.asc";
        String outHack = base + "basin_merelli_hack.asc";
        String out = base + "net.shp";

        OmsNetworkAttributesBuilder extract = new OmsNetworkAttributesBuilder();
        extract.inFlow = OmsRasterReader.readRaster(inFlow);
        extract.inTca = OmsRasterReader.readRaster(inTca);
        extract.inNet = OmsRasterReader.readRaster(inNet);
        extract.doHack = true;
        extract.process();

        SimpleFeatureCollection net = extract.outNet;
        OmsVectorWriter.writeVector(out, net);
        GridCoverage2D hack = extract.outHack;
        OmsRasterWriter.writeRaster(outHack, hack);

    }

    /**
    * Test module with mode=0.
    */
    public void testExtractNetwork0() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

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

        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

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
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

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
                        "LINESTRING (1640695 5139915, 1640725 5139885, 1640755 5139885, 1640785 5139885, 1640815 5139885, 1640845 5139885)",
                        featureMate.getGeometry().toText());
            } else if (featureMate.getAttribute(NetworkChannel.PFAFNAME, String.class).equals("3")) {
                assertEquals(1, featureMate.getAttribute(NetworkChannel.HACKNAME, Integer.class).intValue());
                assertEquals(1, featureMate.getAttribute(NetworkChannel.STRAHLERNAME, Integer.class).intValue());
                assertEquals("LINESTRING (1640845 5139885, 1640875 5139885)", featureMate.getGeometry().toText());
            } else if (featureMate.getAttribute(NetworkChannel.PFAFNAME, String.class).equals("2.1")) {
                assertEquals(2, featureMate.getAttribute(NetworkChannel.HACKNAME, Integer.class).intValue());
                assertEquals(1, featureMate.getAttribute(NetworkChannel.STRAHLERNAME, Integer.class).intValue());
                assertEquals("LINESTRING (1640845 5139885, 1640875 5139915)", featureMate.getGeometry().toText());
            } else {
                throw new RuntimeException();
            }
        }

        // FeatureIterator<SimpleFeature> featureIterator = networkFC.features();
        // while( featureIterator.hasNext() ) {
        // SimpleFeature feature = featureIterator.next();
        // Geometry geometry = (Geometry) feature.getDefaultGeometry();
        // if (geometry.getCoordinates().length > 2) {
        // assertTrue(geometry
        // .toText()
        // .equals("LINESTRING (1640695 5139915, 1640725 5139885, 1640755 5139885, 1640785 5139885, 1640815 5139885, 1640845 5139885)"));
        // } else {
        // assertTrue(geometry.toText().startsWith("LINESTRING (1640845 5139885, 1640875 "));
        // }
        // }
        // featureIterator.close();
    }

}
